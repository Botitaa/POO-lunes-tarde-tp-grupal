package batallanaval.vista;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JButton;
import javax.swing.JLabel;

import org.junit.jupiter.api.Test;

/** Prueba que los botones de {@link PanelBlackout} ejecutan lo que corresponde y que el cartel cambia. */
class PanelBlackoutTest {

    private final AtomicInteger vecesListo = new AtomicInteger();
    private final AtomicInteger vecesGuardar = new AtomicInteger();
    private final PanelBlackout panel = new PanelBlackout(vecesListo::incrementAndGet, vecesGuardar::incrementAndGet);

    @Test
    void elFondoEsNegro() {
        assertEquals(Color.BLACK, panel.getBackground());
    }

    @Test
    void mostrarTurnoDeEscribeElNombreEnElCartel() {
        panel.mostrarTurnoDe("Bota");

        assertEquals("Turno de Bota", buscarCartel().getText());
    }

    @Test
    void cambiarDeTurnoReemplazaElCartel() {
        panel.mostrarTurnoDe("Bota");
        panel.mostrarTurnoDe("Santi");

        assertEquals("Turno de Santi", buscarCartel().getText());
    }

    @Test
    void listoEjecutaSoloLaAccionDeListo() {
        buscarBoton("Listo").doClick();

        assertEquals(1, vecesListo.get());
        assertEquals(0, vecesGuardar.get());
    }

    @Test
    void guardarPartidaEjecutaSoloLaAccionDeGuardar() {
        buscarBoton("Guardar partida").doClick();

        assertEquals(1, vecesGuardar.get());
        assertEquals(0, vecesListo.get());
    }

    @Test
    void conLosBotonesDeshabilitadosNoSeEjecutaNada() {
        panel.setBotonesHabilitados(false);

        buscarBoton("Listo").doClick();
        buscarBoton("Guardar partida").doClick();

        assertFalse(buscarBoton("Listo").isEnabled());
        assertFalse(buscarBoton("Guardar partida").isEnabled());
        assertEquals(0, vecesListo.get());
        assertEquals(0, vecesGuardar.get());
    }

    @Test
    void alVolverAHabilitarLosBotonesFuncionanDeNuevo() {
        panel.setBotonesHabilitados(false);
        panel.setBotonesHabilitados(true);

        buscarBoton("Listo").doClick();
        buscarBoton("Guardar partida").doClick();

        assertTrue(buscarBoton("Listo").isEnabled());
        assertEquals(1, vecesListo.get());
        assertEquals(1, vecesGuardar.get());
    }

    @Test
    void sinAccionesOSinNombreLanzaNullPointerException() {
        assertThrows(NullPointerException.class, () -> new PanelBlackout(null, () -> { }));
        assertThrows(NullPointerException.class, () -> new PanelBlackout(() -> { }, null));
        assertThrows(NullPointerException.class, () -> panel.mostrarTurnoDe(null));
    }

    private JButton buscarBoton(String texto) {
        return buscarBoton(panel, texto);
    }

    private JLabel buscarCartel() {
        return buscarEtiqueta(panel);
    }

    /** Recorre el contenedor y devuelve el botón con ese texto, o {@code null} si no está. */
    private static JButton buscarBoton(Container contenedor, String texto) {
        for (Component componente : contenedor.getComponents()) {
            if (componente instanceof JButton && ((JButton) componente).getText().equals(texto)) {
                return (JButton) componente;
            }
            if (componente instanceof Container) {
                JButton hallado = buscarBoton((Container) componente, texto);
                if (hallado != null) {
                    return hallado;
                }
            }
        }
        return null;
    }

    /** Recorre el contenedor y devuelve la primera etiqueta, o {@code null} si no hay. */
    private static JLabel buscarEtiqueta(Container contenedor) {
        for (Component componente : contenedor.getComponents()) {
            if (componente instanceof JLabel) {
                return (JLabel) componente;
            }
            if (componente instanceof Container) {
                JLabel hallada = buscarEtiqueta((Container) componente);
                if (hallada != null) {
                    return hallada;
                }
            }
        }
        return null;
    }
}
