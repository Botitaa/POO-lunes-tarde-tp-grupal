package batallanaval.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class EstadisticasBarcoTest {

    @Test
    void testCrearEstadisticasValidas() {
        EstadisticasBarco stats = new EstadisticasBarco(5, 3, 2, 4, 3, 0, 2, 3);

        assertEquals(5, stats.vidaMaxima());
        assertEquals(3, stats.movimientoMaxPorTurno());
        assertEquals(2, stats.costoMovimiento());
        assertEquals(4, stats.rangoDisparo());
        assertEquals(3, stats.vision());
        assertEquals(0, stats.cooldownDisparoMax());
        assertEquals(2, stats.danio());
        assertEquals(3, stats.cooldownHabilidadMax());
    }

    @Test
    void testValoresNegativosLanzanExcepcion() {
        assertThrows(IllegalArgumentException.class, () ->
                new EstadisticasBarco(-1, 3, 2, 4, 3, 0, 2, 3));
        assertThrows(IllegalArgumentException.class, () ->
                new EstadisticasBarco(5, -1, 2, 4, 3, 0, 2, 3));
        assertThrows(IllegalArgumentException.class, () ->
                new EstadisticasBarco(5, 3, -1, 4, 3, 0, 2, 3));
        assertThrows(IllegalArgumentException.class, () ->
                new EstadisticasBarco(5, 3, 2, -1, 3, 0, 2, 3));
        assertThrows(IllegalArgumentException.class, () ->
                new EstadisticasBarco(5, 3, 2, 4, -1, 0, 2, 3));
        assertThrows(IllegalArgumentException.class, () ->
                new EstadisticasBarco(5, 3, 2, 4, 3, -1, 2, 3));
        assertThrows(IllegalArgumentException.class, () ->
                new EstadisticasBarco(5, 3, 2, 4, 3, 0, -1, 3));
        assertThrows(IllegalArgumentException.class, () ->
                new EstadisticasBarco(5, 3, 2, 4, 3, 0, 2, -1));
    }
}
