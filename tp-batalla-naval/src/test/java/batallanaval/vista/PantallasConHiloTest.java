package batallanaval.vista;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Verifica que cada pantalla tenga su propio hilo y que actualizar() cambie algo visible. */
class PantallasConHiloTest {

    private VentanaPrincipal ventana;
    private PanelNuevaPartida nuevaPartida;
    private PanelEstadisticas estadisticas;
    private PanelReglas reglas;
    private PanelBlackout blackout;
    private PanelResumen resumen;

    @BeforeEach
    void armarPantallas() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Hace falta un entorno con pantalla para crear una ventana");
        SwingUtilities.invokeAndWait(() -> {
            ventana = new VentanaPrincipal();
            nuevaPartida = new PanelNuevaPartida(ventana, "Bota", "Santi", () -> { });
            estadisticas = new PanelEstadisticas(ventana);
            reglas = new PanelReglas(ventana);
            blackout = new PanelBlackout(() -> { }, () -> { });
            resumen = new PanelResumen(new PartidaFalsa(), () -> { });
        });
    }

    @AfterEach
    void cerrar() throws Exception {
        if (ventana == null) {
            return;
        }
        for (PanelConHilo panel : pantallas()) {
            panel.detener();
        }
        SwingUtilities.invokeAndWait(() -> ventana.dispose());
    }

    @Test
    void todasSonPanelConHiloYNoArrancanSolas() {
        for (PanelConHilo panel : pantallas()) {
            assertInstanceOf(PanelConHilo.class, panel);
            assertFalse(panel.estaCorriendo(), panel.getClass().getSimpleName() + " arrancó su hilo sola");
            assertTrue(panel.getIntervaloMs() > 0);
        }
    }

    @Test
    void nuevaPartidaMuestraLaVistaPreviaOElMotivo() throws Exception {
        SwingUtilities.invokeAndWait(() -> nuevaPartida.actualizar());
        assertEquals("Bota vs Santi", texto(nuevaPartida, "vistaPrevia"));
    }

    @Test
    void estadisticasMuestraElContadorYLaHora() throws Exception {
        SwingUtilities.invokeAndWait(() -> estadisticas.actualizar());
        assertTrue(texto(estadisticas, "etiquetaEstado").matches("0 jugadores en el ranking · \\d{2}:\\d{2}:\\d{2}"));
    }

    @Test
    void reglasMuestraLaHora() throws Exception {
        SwingUtilities.invokeAndWait(() -> reglas.actualizar());
        assertTrue(texto(reglas, "etiquetaReloj").matches("\\d{2}:\\d{2}:\\d{2}"));
    }

    @Test
    void blackoutYResumenHacenParpadearElAviso() throws Exception {
        String blackoutAntes = texto(blackout, "aviso");
        String resumenAntes = texto(resumen, "aviso");

        SwingUtilities.invokeAndWait(() -> {
            blackout.actualizar();
            resumen.actualizar();
        });
        assertNotEquals(blackoutAntes, texto(blackout, "aviso"));
        assertNotEquals(resumenAntes, texto(resumen, "aviso"));

        SwingUtilities.invokeAndWait(() -> {
            blackout.actualizar();
            resumen.actualizar();
        });
        assertEquals(blackoutAntes, texto(blackout, "aviso"));
        assertEquals(resumenAntes, texto(resumen, "aviso"));
    }

    @Test
    void elResumenIniciaSuHiloAlMostrarseYLoDetieneAlOcultarse() throws Exception {
        JFrame[] marco = new JFrame[1];
        SwingUtilities.invokeAndWait(() -> {
            marco[0] = new JFrame();
            marco[0].add(resumen);
            marco[0].setSize(400, 300);
            marco[0].setVisible(true);
        });
        try {
            esperar(() -> resumen.estaCorriendo());
            assertTrue(resumen.estaCorriendo());

            SwingUtilities.invokeAndWait(() -> resumen.setVisible(false));
            esperar(() -> !resumen.estaCorriendo());
            assertFalse(resumen.estaCorriendo());
        } finally {
            SwingUtilities.invokeAndWait(() -> marco[0].dispose());
        }
    }

    // Ayudas

    private List<PanelConHilo> pantallas() {
        return List.of(nuevaPartida, estadisticas, reglas, blackout, resumen);
    }

    /** Espera, con un límite de 2 segundos, a que se cumpla la condición. */
    private static void esperar(java.util.function.BooleanSupplier condicion) throws InterruptedException {
        long limite = System.currentTimeMillis() + 2000;
        while (!condicion.getAsBoolean() && System.currentTimeMillis() < limite) {
            Thread.sleep(20);
        }
    }

    private static String texto(Container contenedor, String nombre) {
        Component componente = buscarPorNombre(contenedor, nombre);
        assertNotNull(componente, "no está la etiqueta " + nombre);
        return ((JLabel) componente).getText();
    }

    private static Component buscarPorNombre(Container contenedor, String nombre) {
        for (Component hijo : contenedor.getComponents()) {
            if (nombre.equals(hijo.getName())) {
                return hijo;
            }
            if (hijo instanceof Container) {
                Component encontrado = buscarPorNombre((Container) hijo, nombre);
                if (encontrado != null) {
                    return encontrado;
                }
            }
        }
        return null;
    }
}