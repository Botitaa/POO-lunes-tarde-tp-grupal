package batallanaval.historial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class HistorialPartidaTest {

    private static final LocalDateTime MOMENTO = LocalDateTime.of(2026, 10, 5, 14, 31, 50);

    private EventoPartida crearEvento(int ronda, int numeroJugador, TipoEvento tipo) {
        return new EventoPartida(ronda, numeroJugador, tipo, "Evento", MOMENTO);
    }

    // ---------- registrar y cantidad ----------

    @Test
    void unHistorialNuevoEstaVacio() {
        HistorialPartida historial = new HistorialPartida();
        assertEquals(0, historial.cantidad());
        assertTrue(historial.getEventos().isEmpty());
    }

    @Test
    void registrarGuardaLosEventosEnOrden() {
        HistorialPartida historial = new HistorialPartida();
        EventoPartida primero = crearEvento(1, 1, TipoEvento.MOVIMIENTO);
        EventoPartida segundo = crearEvento(1, 1, TipoEvento.DISPARO);
        historial.registrar(primero);
        historial.registrar(segundo);

        assertEquals(2, historial.cantidad());
        assertEquals(List.of(primero, segundo), historial.getEventos());
    }

    @Test
    void registrarNuloLanzaExcepcion() {
        HistorialPartida historial = new HistorialPartida();
        assertThrows(NullPointerException.class, () -> historial.registrar(null));
    }

    // ---------- encapsulamiento ----------

    @Test
    void laListaDevueltaNoSePuedeModificar() {
        HistorialPartida historial = new HistorialPartida();
        historial.registrar(crearEvento(1, 1, TipoEvento.DISPARO));
        List<EventoPartida> eventos = historial.getEventos();

        assertThrows(UnsupportedOperationException.class,
                () -> eventos.add(crearEvento(1, 2, TipoEvento.DISPARO)));
        assertEquals(1, historial.cantidad());
    }

    @Test
    void laListaDevueltaNoCambiaSiDespuesSeRegistraOtroEvento() {
        HistorialPartida historial = new HistorialPartida();
        historial.registrar(crearEvento(1, 1, TipoEvento.DISPARO));
        List<EventoPartida> antes = historial.getEventos();

        historial.registrar(crearEvento(1, 2, TipoEvento.DISPARO));

        assertEquals(1, antes.size());
        assertEquals(2, historial.cantidad());
    }

    // ---------- getEventosDeRonda ----------

    @Test
    void getEventosDeRondaDevuelveSoloLosDeEsaRonda() {
        HistorialPartida historial = new HistorialPartida();
        EventoPartida ronda1 = crearEvento(1, 1, TipoEvento.DISPARO);
        EventoPartida ronda2a = crearEvento(2, 1, TipoEvento.DISPARO);
        EventoPartida ronda2b = crearEvento(2, 2, TipoEvento.HUNDIMIENTO);
        historial.registrar(ronda1);
        historial.registrar(ronda2a);
        historial.registrar(ronda2b);

        assertEquals(List.of(ronda2a, ronda2b), historial.getEventosDeRonda(2));
        assertEquals(List.of(ronda1), historial.getEventosDeRonda(1));
        assertTrue(historial.getEventosDeRonda(5).isEmpty());
    }

    // ---------- getEventosVisiblesPara ----------

    @Test
    void cadaJugadorVeLosPublicosYSusPrivadosPeroNoLosPrivadosDelRival() {
        HistorialPartida historial = new HistorialPartida();
        EventoPartida movimientoJ1 = crearEvento(1, 1, TipoEvento.MOVIMIENTO); // privado de J1
        EventoPartida disparoJ1 = crearEvento(1, 1, TipoEvento.DISPARO);       // público
        EventoPartida habilidadJ2 = crearEvento(1, 2, TipoEvento.HABILIDAD);   // privado de J2
        historial.registrar(movimientoJ1);
        historial.registrar(disparoJ1);
        historial.registrar(habilidadJ2);

        assertEquals(List.of(movimientoJ1, disparoJ1), historial.getEventosVisiblesPara(1));
        assertEquals(List.of(disparoJ1, habilidadJ2), historial.getEventosVisiblesPara(2));
    }
}