package batallanaval.historial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class EventoPartidaTest {

    private static final LocalDateTime MOMENTO = LocalDateTime.of(2026, 10, 5, 14, 31, 50);

    // ---------- visibilidad ----------

    @Test
    void unEventoPublicoLoVenLosDosJugadores() {
        EventoPartida evento = new EventoPartida(3, 1, TipoEvento.DISPARO, "Disparo", MOMENTO);
        assertTrue(evento.esVisiblePara(1));
        assertTrue(evento.esVisiblePara(2));
    }

    @Test
    void unEventoPrivadoLoVeSoloSuDuenio() {
        EventoPartida evento = new EventoPartida(3, 1, TipoEvento.MOVIMIENTO, "Movimiento", MOMENTO);
        assertTrue(evento.esVisiblePara(1));
        assertFalse(evento.esVisiblePara(2));
    }

    // ---------- aLinea ----------

    @Test
    void aLineaTieneRondaJugadorTipoYDescripcion() {
        EventoPartida evento = new EventoPartida(12, 2, TipoEvento.DISPARO,
                "Acorazado (7,4) -> Crucero (4,4), -4", MOMENTO);
        assertEquals("[R12] J2 DISPARO: Acorazado (7,4) -> Crucero (4,4), -4", evento.aLinea());
    }

    // ---------- nulls ----------

    @Test
    void tipoNuloLanzaExcepcion() {
        assertThrows(NullPointerException.class,
                () -> new EventoPartida(1, 1, null, "Descripcion", MOMENTO));
    }

    @Test
    void descripcionNulaLanzaExcepcion() {
        assertThrows(NullPointerException.class,
                () -> new EventoPartida(1, 1, TipoEvento.DISPARO, null, MOMENTO));
    }

    @Test
    void momentoNuloLanzaExcepcion() {
        assertThrows(NullPointerException.class,
                () -> new EventoPartida(1, 1, TipoEvento.DISPARO, "Descripcion", null));
    }
}