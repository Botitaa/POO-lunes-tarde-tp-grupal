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
    
    // ---------- partida simulada ----------

    @Test
    void elOrdenSeMantieneEnUnaPartidaDeVariasRondas() {
        HistorialPartida historial = new HistorialPartida();
        EventoPartida r1j1 = crearEvento(1, 1, TipoEvento.MOVIMIENTO);
        EventoPartida r1j2 = crearEvento(1, 2, TipoEvento.DISPARO);
        EventoPartida r2j1 = crearEvento(2, 1, TipoEvento.HABILIDAD);
        EventoPartida r2j1b = crearEvento(2, 1, TipoEvento.DISPARO);
        EventoPartida r2j2 = crearEvento(2, 2, TipoEvento.FIN_TURNO);
        EventoPartida r3j1 = crearEvento(3, 1, TipoEvento.HUNDIMIENTO);
        historial.registrar(r1j1);
        historial.registrar(r1j2);
        historial.registrar(r2j1);
        historial.registrar(r2j1b);
        historial.registrar(r2j2);
        historial.registrar(r3j1);

        assertEquals(List.of(r1j1, r1j2, r2j1, r2j1b, r2j2, r3j1), historial.getEventos());
        assertEquals(List.of(r2j1, r2j1b, r2j2), historial.getEventosDeRonda(2));
        assertEquals(List.of(r1j2, r2j1b, r2j2, r3j1), historial.getEventosVisiblesPara(2));
    }

    @Test
    void elRivalNuncaVeLosEventosPrivadosAjenos() {
        HistorialPartida historial = new HistorialPartida();
        // El jugador 1 hace un evento de cada tipo
        for (TipoEvento tipo : TipoEvento.values()) {
            historial.registrar(crearEvento(1, 1, tipo));
        }

        List<EventoPartida> visiblesParaRival = historial.getEventosVisiblesPara(2);
        for (EventoPartida evento : visiblesParaRival) {
            assertTrue(evento.tipo().esPublico(), "El rival vio un evento privado: " + evento.tipo());
        }
        assertEquals(5, visiblesParaRival.size()); // DISPARO, HUNDIMIENTO, DOMINACION, FIN_TURNO, FIN_PARTIDA
    }

    @Test
    void elRivalNoVeMovimientoNiHabilidadAjenosPeroElDuenioSi() {
        HistorialPartida historial = new HistorialPartida();
        EventoPartida movimientoJ2 = crearEvento(4, 2, TipoEvento.MOVIMIENTO);
        EventoPartida habilidadJ2 = crearEvento(4, 2, TipoEvento.HABILIDAD);
        historial.registrar(movimientoJ2);
        historial.registrar(habilidadJ2);

        assertTrue(historial.getEventosVisiblesPara(1).isEmpty());
        assertEquals(List.of(movimientoJ2, habilidadJ2), historial.getEventosVisiblesPara(2));
    }

    @Test
    void cadaJugadorVeTodosSusEventosAunqueSeanPrivados() {
        HistorialPartida historial = new HistorialPartida();
        for (TipoEvento tipo : TipoEvento.values()) {
            historial.registrar(crearEvento(1, 2, tipo));
        }

        assertEquals(TipoEvento.values().length, historial.getEventosVisiblesPara(2).size());
    }

    // ---------- aLinea ----------

    @Test
    void aLineaDeLosEventosDelHistorialSaleEnOrden() {
        HistorialPartida historial = new HistorialPartida();
        historial.registrar(new EventoPartida(12, 2, TipoEvento.DISPARO,
                "Acorazado (7,4) -> Crucero (4,4), -4", MOMENTO));
        historial.registrar(new EventoPartida(12, 2, TipoEvento.FIN_TURNO, "Termina el turno", MOMENTO));

        List<EventoPartida> eventos = historial.getEventos();
        assertEquals("[R12] J2 DISPARO: Acorazado (7,4) -> Crucero (4,4), -4", eventos.get(0).aLinea());
        assertEquals("[R12] J2 FIN_TURNO: Termina el turno", eventos.get(1).aLinea());
    }

    @Test
    void aLineaTambienFuncionaConEventosPrivados() {
        EventoPartida evento = new EventoPartida(3, 1, TipoEvento.MOVIMIENTO, "Crucero (2,5) -> (3,5)", MOMENTO);
        assertEquals("[R3] J1 MOVIMIENTO: Crucero (2,5) -> (3,5)", evento.aLinea());
    }
}