package batallanaval.vista;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * Prueba solo la validación de nombres, que es lógica pura: no se crea ninguna
 * ventana Swing.
 */
class PanelNuevaPartidaTest {

    @Test
    void nombresValidosNoDevuelvenError() {
        assertTrue(PanelNuevaPartida.validarNombres("Bota", "Santi").isEmpty());
    }

    @Test
    void nombreVacioDelJugador1EsError() {
        Optional<String> error = PanelNuevaPartida.validarNombres("", "Santi");
        assertEquals("El nombre del Jugador 1 no puede estar vacío", error.orElseThrow());
    }

    @Test
    void nombreSoloConEspaciosEsVacio() {
        Optional<String> error = PanelNuevaPartida.validarNombres("Bota", "   ");
        assertEquals("El nombre del Jugador 2 no puede estar vacío", error.orElseThrow());
    }

    @Test
    void nombreNuloSeTrataComoVacio() {
        assertTrue(PanelNuevaPartida.validarNombres(null, "Santi").isPresent());
        assertTrue(PanelNuevaPartida.validarNombres("Bota", null).isPresent());
    }

    @Test
    void puntoYComaEsError() {
        assertTrue(PanelNuevaPartida.validarNombres("Bo;ta", "Santi").isPresent());
        assertTrue(PanelNuevaPartida.validarNombres("Bota", "San;ti").isPresent());
    }

    @Test
    void nombresIgualesSonErrorSinImportarMayusculasNiEspacios() {
        assertTrue(PanelNuevaPartida.validarNombres("Bota", "Bota").isPresent());
        assertTrue(PanelNuevaPartida.validarNombres("Bota", "  bOTA ").isPresent());
    }

    @Test
    void nombresParecidosPeroDistintosSonValidos() {
        assertTrue(PanelNuevaPartida.validarNombres("Bota", "Bota2").isEmpty());
    }
}
