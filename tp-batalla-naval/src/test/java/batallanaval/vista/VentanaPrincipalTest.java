package batallanaval.vista;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.GraphicsEnvironment;
import java.awt.event.WindowEvent;
import java.io.Serial;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Prueba que la {@link VentanaPrincipal} inicia y detiene el hilo de las pantallas que son
 * {@link PanelConHilo}. Necesita crear una ventana real, así que si el entorno no tiene pantalla
 * (por ejemplo, un servidor sin interfaz gráfica) los tests se saltean en lugar de fallar.
 */
class VentanaPrincipalTest {

    private VentanaPrincipal ventana;
    private PanelDePrueba panelConHilo1;
    private PanelDePrueba panelConHilo2;

    @BeforeEach
    void armarLaVentana() throws InterruptedException, InvocationTargetException {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Hace falta un entorno con pantalla para crear una ventana");

        panelConHilo1 = new PanelDePrueba();
        panelConHilo2 = new PanelDePrueba();
        enElEdt(() -> {
            ventana = new VentanaPrincipal();
            // Sin esto, cerrar la ventana en un test terminaría todo el programa (EXIT_ON_CLOSE).
            ventana.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            ventana.agregarPantalla(Pantalla.INICIO, new JPanel());
            ventana.agregarPantalla(Pantalla.JUEGO, panelConHilo1);
            ventana.agregarPantalla(Pantalla.RESUMEN, panelConHilo2);
        });
    }

    @AfterEach
    void cerrarLaVentana() throws InterruptedException, InvocationTargetException {
        if (ventana != null) {
            enElEdt(ventana::dispose);
        }
    }

    @Test
    void entrarAUnaPantallaConHiloLeIniciaElHilo() throws InterruptedException, InvocationTargetException {
        assertFalse(panelConHilo1.estaCorriendo(), "antes de mostrarla no tiene que estar corriendo");

        enElEdt(() -> ventana.mostrar(Pantalla.JUEGO));

        assertTrue(panelConHilo1.estaCorriendo());
    }

    @Test
    void salirDeUnaPantallaConHiloLeDetieneElHilo() throws InterruptedException, InvocationTargetException {
        enElEdt(() -> ventana.mostrar(Pantalla.JUEGO));
        Thread hilo = panelConHilo1.getHilo();

        enElEdt(() -> ventana.mostrar(Pantalla.INICIO));
        hilo.join(1000);

        assertFalse(hilo.isAlive(), "el hilo de la pantalla que sale tendría que haber terminado");
        assertFalse(panelConHilo1.estaCorriendo());
    }

    @Test
    void pasarDeUnaPantallaConHiloAOtraDetieneUnaEIniciaLaOtra() throws InterruptedException, InvocationTargetException {
        enElEdt(() -> ventana.mostrar(Pantalla.JUEGO));
        Thread hiloDeLaQueSale = panelConHilo1.getHilo();

        enElEdt(() -> ventana.mostrar(Pantalla.RESUMEN));
        hiloDeLaQueSale.join(1000);

        assertFalse(hiloDeLaQueSale.isAlive(), "la pantalla que sale tendría que haberse detenido");
        assertFalse(panelConHilo1.estaCorriendo());
        assertTrue(panelConHilo2.estaCorriendo(), "la pantalla que entra tendría que haberse iniciado");
    }

    @Test
    void unaPantallaSinHiloNoIniciaNiDetieneNada() {
        assertDoesNotThrow(() -> enElEdt(() -> ventana.mostrar(Pantalla.INICIO)));

        assertFalse(panelConHilo1.estaCorriendo());
        assertFalse(panelConHilo2.estaCorriendo());
    }

    @Test
    void volverAEntrarAUnaPantallaConHiloLeCreaUnHiloNuevo() throws InterruptedException, InvocationTargetException {
        enElEdt(() -> ventana.mostrar(Pantalla.JUEGO));
        Thread primero = panelConHilo1.getHilo();
        enElEdt(() -> ventana.mostrar(Pantalla.INICIO));
        primero.join(1000);

        enElEdt(() -> ventana.mostrar(Pantalla.JUEGO));

        assertTrue(panelConHilo1.estaCorriendo());
        assertNotSame(primero, panelConHilo1.getHilo());
    }

    @Test
    void cerrarLaVentanaLeDetieneElHiloALaPantallaActual() throws InterruptedException, InvocationTargetException {
        enElEdt(() -> ventana.mostrar(Pantalla.JUEGO));
        Thread hilo = panelConHilo1.getHilo();

        enElEdt(() -> ventana.dispatchEvent(new WindowEvent(ventana, WindowEvent.WINDOW_CLOSING)));
        hilo.join(1000);

        assertFalse(hilo.isAlive(), "al cerrar la ventana el hilo tendría que haber terminado");
        assertFalse(panelConHilo1.estaCorriendo());
    }

    @Test
    void liberarLaVentanaConDisposeLeDetieneElHiloALaPantallaActual() throws InterruptedException, InvocationTargetException {
        enElEdt(() -> ventana.mostrar(Pantalla.RESUMEN));
        Thread hilo = panelConHilo2.getHilo();

        enElEdt(ventana::dispose);
        hilo.join(1000);

        assertFalse(hilo.isAlive(), "dispose() (lo que hace el botón Salir) tendría que detener el hilo");
        assertFalse(panelConHilo2.estaCorriendo());
    }

    /** Todo lo que toca una ventana se hace en el hilo de eventos de Swing. */
    private static void enElEdt(Runnable accion) throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(accion);
    }

    /** Pantalla de prueba que hereda de {@link PanelConHilo} y no hace nada en cada actualización. */
    private static final class PanelDePrueba extends PanelConHilo {

        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        protected void actualizar() {
            // no hace falta nada: lo que se prueba es cuándo se inicia y se detiene el hilo
        }

        @Override
        protected int getIntervaloMs() {
            return 10;
        }
    }
}
