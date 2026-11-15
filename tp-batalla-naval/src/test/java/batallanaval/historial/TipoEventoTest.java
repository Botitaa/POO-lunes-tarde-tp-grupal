package batallanaval.historial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TipoEventoTest {

    @Test
    void despliegueMovimientoYHabilidadSonPrivados() {
        assertFalse(TipoEvento.DESPLIEGUE.esPublico());
        assertFalse(TipoEvento.MOVIMIENTO.esPublico());
        assertFalse(TipoEvento.HABILIDAD.esPublico());
    }

    @Test
    void losDemasTiposSonPublicos() {
        assertTrue(TipoEvento.DISPARO.esPublico());
        assertTrue(TipoEvento.HUNDIMIENTO.esPublico());
        assertTrue(TipoEvento.DOMINACION.esPublico());
        assertTrue(TipoEvento.FIN_TURNO.esPublico());
        assertTrue(TipoEvento.FIN_PARTIDA.esPublico());
    }

    @Test
    void hayOchoTiposDeEvento() {
        assertEquals(8, TipoEvento.values().length);
    }
}