package batallanaval.vista;

import batallanaval.modelo.Disparo;
import batallanaval.modelo.Posicion;
import batallanaval.modelo.ResumenTurno;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de los datos que usa el tablero para las ayudas visuales: los disparos recibidos,
 * y la descripción de las habilidades.
 */
class AyudasVisualesTest {

    @Test
    void elDisparoNoAceptaDanioNegativoNiPosicionesNulas() {
        assertThrows(IllegalArgumentException.class, () -> new Disparo(new Posicion(0, 0), new Posicion(1, 1), -1, false));
        assertThrows(NullPointerException.class, () -> new Disparo(null, new Posicion(1, 1), 1, false));
    }

    @Test
    void elResumenNuevoNoTieneDisparos() {
        assertTrue(new ResumenTurno().getDisparosRecibidos().isEmpty());
    }

    @Test
    void descripcionDeCadaHabilidad() {
        assertTrue(PanelTablero.descripcionHabilidad('A').contains("6"));
        assertTrue(PanelTablero.descripcionHabilidad('C').contains("repara"));
        assertTrue(PanelTablero.descripcionHabilidad('D').contains("doble"));
    }
}
