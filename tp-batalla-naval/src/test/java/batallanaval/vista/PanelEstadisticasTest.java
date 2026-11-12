package batallanaval.vista;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import batallanaval.persistencia.EstadisticaJugador;

/**
 * Prueba que {@link PanelEstadisticas} muestra el ranking en la tabla, en el orden recibido, y que
 * Volver lleva al inicio. Necesita crear una {@link VentanaPrincipal}, así que si el entorno no
 * tiene pantalla los tests se saltean en lugar de fallar.
 */
class PanelEstadisticasTest {

    private VentanaPrincipal ventana;
    private PanelEstadisticas panel;

    @BeforeEach
    void armarLaPantalla() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Hace falta un entorno con pantalla para crear una ventana");
        ventana = new VentanaPrincipal();
        panel = new PanelEstadisticas(ventana);
        ventana.agregarPantalla(Pantalla.INICIO, new JPanel());
        ventana.agregarPantalla(Pantalla.ESTADISTICAS, panel);
    }

    @AfterEach
    void cerrarLaVentana() {
        if (ventana != null) {
            ventana.dispose();
        }
    }

    @Test
    void alCrearseLaTablaTieneLasColumnasDelRankingYNingunaFila() {
        JTable tabla = buscarTabla(panel);

        assertEquals(7, tabla.getColumnCount());
        assertEquals("#", tabla.getColumnName(0));
        assertEquals("Jugador", tabla.getColumnName(1));
        assertEquals("Jugadas", tabla.getColumnName(2));
        assertEquals("Ganadas", tabla.getColumnName(3));
        assertEquals("Perdidas", tabla.getColumnName(4));
        assertEquals("Empatadas", tabla.getColumnName(5));
        assertEquals("% Victorias", tabla.getColumnName(6));
        assertEquals(0, tabla.getRowCount());
    }

    @Test
    void cargarRankingMuestraUnaFilaPorJugadorEnElOrdenRecibido() {
        panel.cargarRanking(List.of(
                new EstadisticaJugador("Bota", 10, 7, 2, 1),
                new EstadisticaJugador("Santi", 8, 3, 5, 0)));

        JTable tabla = buscarTabla(panel);
        assertEquals(2, tabla.getRowCount());
        assertEquals(1, tabla.getValueAt(0, 0));
        assertEquals("Bota", tabla.getValueAt(0, 1));
        assertEquals(10, tabla.getValueAt(0, 2));
        assertEquals(7, tabla.getValueAt(0, 3));
        assertEquals(2, tabla.getValueAt(0, 4));
        assertEquals(1, tabla.getValueAt(0, 5));
        assertEquals(2, tabla.getValueAt(1, 0));
        assertEquals("Santi", tabla.getValueAt(1, 1));
    }

    @Test
    void elPorcentajeSeMuestraConUnDecimalYComa() {
        panel.cargarRanking(List.of(
                new EstadisticaJugador("Bota", 10, 7, 2, 1),
                new EstadisticaJugador("Santi", 3, 2, 1, 0)));

        JTable tabla = buscarTabla(panel);
        assertEquals("70,0 %", tabla.getValueAt(0, 6));
        assertEquals("66,7 %", tabla.getValueAt(1, 6));
    }

    @Test
    void unJugadorSinPartidasMuestraCeroPorCiento() {
        panel.cargarRanking(List.of(new EstadisticaJugador("Nuevo", 0, 0, 0, 0)));

        assertEquals("0,0 %", buscarTabla(panel).getValueAt(0, 6));
    }

    @Test
    void cargarDeNuevoReemplazaLasFilasEnLugarDeAcumularlas() {
        panel.cargarRanking(List.of(
                new EstadisticaJugador("Bota", 10, 7, 2, 1),
                new EstadisticaJugador("Santi", 8, 3, 5, 0)));

        panel.cargarRanking(List.of(new EstadisticaJugador("Ana", 1, 1, 0, 0)));

        JTable tabla = buscarTabla(panel);
        assertEquals(1, tabla.getRowCount());
        assertEquals("Ana", tabla.getValueAt(0, 1));
        assertEquals(1, tabla.getValueAt(0, 0));
    }

    @Test
    void sinPartidasSeMuestraElAvisoYLaTablaQuedaVacia() {
        panel.cargarRanking(List.of());

        assertEquals(0, buscarTabla(panel).getRowCount());
        assertTrue(buscarAviso(panel).isVisible());
    }

    @Test
    void alCrearseTodaviaSeMuestraElAviso() {
        assertTrue(buscarAviso(panel).isVisible());
    }

    @Test
    void conJugadoresElAvisoSeOculta() {
        panel.cargarRanking(List.of());

        panel.cargarRanking(List.of(new EstadisticaJugador("Bota", 1, 1, 0, 0)));

        assertFalse(buscarAviso(panel).isVisible());
    }

    @Test
    void lasCeldasNoSePuedenEditar() {
        panel.cargarRanking(List.of(new EstadisticaJugador("Bota", 1, 1, 0, 0)));

        JTable tabla = buscarTabla(panel);
        assertFalse(tabla.isCellEditable(0, 1));
        assertFalse(tabla.isCellEditable(0, 3));
    }

    @Test
    void volverMuestraElInicio() {
        ventana.mostrar(Pantalla.ESTADISTICAS);
        JButton botonVolver = buscarBotonVolver(panel);
        assertNotNull(botonVolver, "el panel tiene que tener un botón Volver");

        botonVolver.doClick();

        assertEquals(Pantalla.INICIO, ventana.getPantallaActual());
    }

    @Test
    void noSeAceptanArgumentosNulos() {
        assertThrows(NullPointerException.class, () -> new PanelEstadisticas(null));
        assertThrows(NullPointerException.class, () -> panel.cargarRanking(null));
    }

    /** Recorre el contenedor y devuelve la primera tabla, o {@code null} si no hay. */
    private static JTable buscarTabla(Container contenedor) {
        for (Component componente : contenedor.getComponents()) {
            if (componente instanceof JTable) {
                return (JTable) componente;
            }
            if (componente instanceof Container) {
                JTable hallada = buscarTabla((Container) componente);
                if (hallada != null) {
                    return hallada;
                }
            }
        }
        return null;
    }

    /** Recorre el contenedor y devuelve la etiqueta con el aviso de "sin partidas", o {@code null}. */
    private static JLabel buscarAviso(Container contenedor) {
        for (Component componente : contenedor.getComponents()) {
            if (componente instanceof JLabel
                    && PanelEstadisticas.MENSAJE_SIN_DATOS.equals(((JLabel) componente).getText())) {
                return (JLabel) componente;
            }
            if (componente instanceof Container) {
                JLabel hallada = buscarAviso((Container) componente);
                if (hallada != null) {
                    return hallada;
                }
            }
        }
        return null;
    }

    /** Recorre el contenedor y devuelve el botón "Volver", o {@code null} si no está. */
    private static JButton buscarBotonVolver(Container contenedor) {
        for (Component componente : contenedor.getComponents()) {
            if (componente instanceof JButton && "Volver".equals(((JButton) componente).getText())) {
                return (JButton) componente;
            }
            if (componente instanceof Container) {
                JButton hallado = buscarBotonVolver((Container) componente);
                if (hallado != null) {
                    return hallado;
                }
            }
        }
        return null;
    }
}
