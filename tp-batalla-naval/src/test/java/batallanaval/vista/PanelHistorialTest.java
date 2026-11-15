package batallanaval.vista;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;

import org.junit.jupiter.api.Test;

import batallanaval.historial.EventoPartida;
import batallanaval.historial.TipoEvento;

/**
 * Prueba que {@link PanelHistorial} muestra lo que devuelve la partida, en orden y con el último
 * evento abajo, que se rearma con cada aviso (también cuando cambia el jugador activo) y que se
 * puede plegar. No necesita pantalla: los componentes se crean pero no se muestran.
 */
class PanelHistorialTest {

    private static final LocalDateTime MOMENTO = LocalDateTime.of(2026, 10, 5, 14, 31, 50);

    private final PartidaFalsa partida = new PartidaFalsa();

    private static EventoPartida evento(int ronda, int jugador, TipoEvento tipo, String descripcion) {
        return new EventoPartida(ronda, jugador, tipo, descripcion, MOMENTO);
    }

    // Contenido

    @Test
    void sinEventosLaListaEstaVaciaYElTituloLoAvisa() {
        PanelHistorial panel = new PanelHistorial(partida);

        assertEquals(0, panel.getCantidadEventos());
        assertEquals("Historial: " + PanelHistorial.SIN_EVENTOS, titulo(panel));
    }

    @Test
    void muestraLosEventosEnOrdenConElUltimoAbajo() {
        EventoPartida primero = evento(1, 1, TipoEvento.DISPARO, "Acorazado (2,4) -> Crucero (5,4), -4");
        EventoPartida segundo = evento(1, 1, TipoEvento.FIN_TURNO, "Termina el turno de Bota");
        EventoPartida ultimo = evento(1, 2, TipoEvento.HUNDIMIENTO, "Se hundió el Destructor (6,6)");
        partida.historial.addAll(List.of(primero, segundo, ultimo));

        PanelHistorial panel = new PanelHistorial(partida);

        ListModel<EventoPartida> modelo = lista(panel).getModel();
        assertEquals(3, modelo.getSize());
        assertEquals(primero, modelo.getElementAt(0));
        assertEquals(ultimo, modelo.getElementAt(2), "el último evento va abajo");
        assertEquals("Historial (3)", titulo(panel));
    }

    @Test
    void cadaFilaSeMuestraConElFormatoDelHistorialYLaHoraEnElTooltip() {
        EventoPartida disparo = evento(12, 2, TipoEvento.DISPARO, "Acorazado (7,4) -> Crucero (4,4), -4");
        partida.historial.add(disparo);
        PanelHistorial panel = new PanelHistorial(partida);
        JList<EventoPartida> lista = lista(panel);

        ListCellRenderer<? super EventoPartida> renderer = lista.getCellRenderer();
        Component fila = renderer.getListCellRendererComponent(lista, disparo, 0, false, false);

        JLabel etiqueta = (JLabel) fila;
        assertEquals("[R12] J2 DISPARO: Acorazado (7,4) -> Crucero (4,4), -4", etiqueta.getText());
        assertEquals("14:31:50", etiqueta.getToolTipText());
    }

    // Actualización

    @Test
    void cuandoLaPartidaAvisaUnCambioAparecenLosEventosNuevos()
            throws InterruptedException, InvocationTargetException {
        PanelHistorial panel = new PanelHistorial(partida);
        partida.agregarObservador(panel);

        partida.historial.add(evento(1, 1, TipoEvento.DISPARO, "Crucero (2,2) -> Destructor (4,3), -2"));
        panel.onEstadoCambiado();
        PanelLateralTest.esperarAlEdt();

        assertEquals(1, panel.getCantidadEventos());
        assertEquals("Historial (1)", titulo(panel));
    }

    @Test
    void alCambiarDeJugadorSeMuestraSoloLoQueVeElNuevoNoSeAcumula()
            throws InterruptedException, InvocationTargetException {
        // Turno de J1: ve su movimiento (privado) y su disparo (público).
        EventoPartida movimientoJ1 = evento(3, 1, TipoEvento.MOVIMIENTO, "Destructor (2,1) -> (3,1)");
        EventoPartida disparoJ1 = evento(3, 1, TipoEvento.DISPARO, "Destructor (3,1) -> Crucero (5,1), -3");
        partida.historial.addAll(List.of(movimientoJ1, disparoJ1));
        PanelHistorial panel = new PanelHistorial(partida);
        assertEquals(2, panel.getCantidadEventos());

        // Empieza el turno de J2: la partida ya le devuelve solo el disparo (el movimiento era privado).
        partida.historial.clear();
        partida.historial.add(disparoJ1);
        panel.onTurnoIniciado("Santi");
        PanelLateralTest.esperarAlEdt();

        assertEquals(1, panel.getCantidadEventos(), "no quedan eventos que el nuevo jugador no puede ver");
        assertEquals(disparoJ1, lista(panel).getModel().getElementAt(0));
    }

    @Test
    void todosLosAvisosRearmanLaLista() throws InterruptedException, InvocationTargetException {
        PanelHistorial panel = new PanelHistorial(partida);

        partida.historial.add(evento(1, 1, TipoEvento.DESPLIEGUE, "Bota desplegó su flota"));
        panel.onFaseCambiada(null);
        PanelLateralTest.esperarAlEdt();
        assertEquals(1, panel.getCantidadEventos());

        partida.historial.add(evento(40, 2, TipoEvento.FIN_PARTIDA, "Gana Bota por dominación"));
        panel.onPartidaTerminada(null);
        PanelLateralTest.esperarAlEdt();
        assertEquals(2, panel.getCantidadEventos());
    }

    @Test
    void noModificaLaListaQueDevuelveLaPartida() {
        partida.historial.add(evento(1, 1, TipoEvento.DISPARO, "x"));
        PanelHistorial panel = new PanelHistorial(partida);

        panel.actualizar();
        panel.actualizar();

        assertEquals(1, panel.getCantidadEventos(), "actualizar dos veces no duplica eventos");
        assertEquals(1, partida.historial.size());
    }

    // Plegable

    @Test
    void arrancaDesplegado() {
        PanelHistorial panel = new PanelHistorial(partida);

        assertFalse(panel.estaPlegado());
        assertTrue(desplazamiento(panel).isVisible());
        assertEquals(PanelHistorial.TEXTO_PLEGAR, botonPlegar(panel).getText());
    }

    @Test
    void elBotonPliegaYDespliegaLaLista() {
        PanelHistorial panel = new PanelHistorial(partida);

        botonPlegar(panel).doClick();
        assertTrue(panel.estaPlegado());
        assertFalse(desplazamiento(panel).isVisible());
        assertEquals(PanelHistorial.TEXTO_DESPLEGAR, botonPlegar(panel).getText());
        assertTrue(botonPlegar(panel).isVisible(), "el botón queda a la vista para volver a abrirlo");

        botonPlegar(panel).doClick();
        assertFalse(panel.estaPlegado());
        assertTrue(desplazamiento(panel).isVisible());
    }

    @Test
    void plegadoSeSigueActualizando() throws InterruptedException, InvocationTargetException {
        PanelHistorial panel = new PanelHistorial(partida);
        panel.setPlegado(true);

        partida.historial.add(evento(2, 2, TipoEvento.DOMINACION, "Santi suma 1 punto de dominación"));
        panel.onEstadoCambiado();
        PanelLateralTest.esperarAlEdt();

        assertEquals(1, panel.getCantidadEventos());
        assertEquals("Historial (1)", titulo(panel));
    }

    @Test
    void noSeAceptaUnaPartidaNula() {
        assertThrows(NullPointerException.class, () -> new PanelHistorial(null));
    }

    // Ayudas

    @SuppressWarnings("unchecked")
    private static JList<EventoPartida> lista(PanelHistorial panel) {
        Component componente = PanelLateralTest.buscarPorNombre(panel, "listaHistorial");
        assertNotNull(componente, "no está la lista");
        return (JList<EventoPartida>) componente;
    }

    private static String titulo(PanelHistorial panel) {
        return ((JLabel) PanelLateralTest.buscarPorNombre(panel, "etiquetaTituloHistorial")).getText();
    }

    private static JButton botonPlegar(PanelHistorial panel) {
        return (JButton) PanelLateralTest.buscarPorNombre(panel, "botonPlegarHistorial");
    }

    /** El JScrollPane que envuelve la lista (lo que se esconde al plegar). */
    private static JScrollPane desplazamiento(PanelHistorial panel) {
        for (Component componente : panel.getComponents()) {
            if (componente instanceof JScrollPane scroll) {
                return scroll;
            }
        }
        throw new AssertionError("no está el JScrollPane");
    }
}
