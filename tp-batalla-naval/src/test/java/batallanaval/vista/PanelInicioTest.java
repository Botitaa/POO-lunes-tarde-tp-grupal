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

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PanelInicioTest {

    private VentanaPrincipal ventana;
    private PanelInicio panel;

    @BeforeEach
    void armarElPanel() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Hace falta un entorno con pantalla para crear una ventana");
        SwingUtilities.invokeAndWait(() -> {
            ventana = new VentanaPrincipal();
            panel = new PanelInicio(ventana);
        });
    }

    @AfterEach
    void cerrar() throws Exception {
        if (panel != null) {
            panel.detener();
        }
        if (ventana != null) {
            SwingUtilities.invokeAndWait(() -> ventana.dispose());
        }
    }

    // Franja de mar (no necesita pantalla, pero se prueba junto con el resto)

    @Test
    void elMarTieneSiempreElMismoAnchoYUnSoloBarco() {
        for (int posicion = 0; posicion <= PanelInicio.ANCHO_MAR - PanelInicio.BARCO.length(); posicion++) {
            String mar = PanelInicio.armarMar(posicion);
            assertEquals(PanelInicio.ANCHO_MAR, mar.length());
            assertEquals(posicion, mar.indexOf(PanelInicio.BARCO));
        }
    }

    // Hilo propio (PanelConHilo)

    @Test
    void esUnPanelConHiloQueNoArrancaSolo() {
        assertInstanceOf(PanelConHilo.class, panel);
        assertFalse(panel.estaCorriendo(), "el hilo lo inicia la ventana al mostrar la pantalla, no el constructor");
    }

    @Test
    void elIntervaloDeActualizacionEsPositivo() {
        assertTrue(panel.getIntervaloMs() > 0);
    }

    @Test
    void actualizarMueveElBarcoUnaColumna() throws Exception {
        String antes = texto(panel, "etiquetaMar");

        SwingUtilities.invokeAndWait(() -> panel.actualizar());

        String despues = texto(panel, "etiquetaMar");
        assertNotEquals(antes, despues);
        assertEquals(antes.indexOf(PanelInicio.BARCO) + 1, despues.indexOf(PanelInicio.BARCO));
    }

    @Test
    void alLlegarAlFinalElBarcoVuelveAEmpezar() throws Exception {
        int pasos = PanelInicio.ANCHO_MAR - PanelInicio.BARCO.length() + 1;
        SwingUtilities.invokeAndWait(() -> {
            for (int i = 0; i < pasos; i++) {
                panel.actualizar();
            }
        });

        assertEquals(0, texto(panel, "etiquetaMar").indexOf(PanelInicio.BARCO));
    }

    @Test
    void actualizarMuestraLaHoraConHorasMinutosYSegundos() throws Exception {
        SwingUtilities.invokeAndWait(() -> panel.actualizar());

        assertTrue(texto(panel, "etiquetaReloj").matches("\\d{2}:\\d{2}:\\d{2}"));
    }

    @Test
    void elHiloMueveElBarcoSinQueNadieLlameAActualizar() throws Exception {
        String antes = texto(panel, "etiquetaMar");

        panel.iniciar();
        try {
            long limite = System.currentTimeMillis() + 2000;
            while (antes.equals(texto(panel, "etiquetaMar")) && System.currentTimeMillis() < limite) {
                Thread.sleep(20);
            }
        } finally {
            panel.detener();
        }

        assertNotEquals(antes, texto(panel, "etiquetaMar"));
    }

    @Test
    void detenerFrenaElHiloDelPanel() {
        panel.iniciar();
        assertTrue(panel.estaCorriendo());

        panel.detener();

        assertFalse(panel.estaCorriendo());
    }

    // Ayudas

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