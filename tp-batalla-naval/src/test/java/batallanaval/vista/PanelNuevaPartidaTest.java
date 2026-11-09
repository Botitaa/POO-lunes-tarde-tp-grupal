package batallanaval.vista;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Prueba solo la validación de nombres, que es lógica pura: no se crea ninguna
 * ventana Swing.
 */
class PanelNuevaPartidaTest {

    @Test
    void nombresValidosNoDevuelvenError() {
        assertNull(PanelNuevaPartida.validarNombres("Bota", "Santi"));
    }

    @Test
    void nombreVacioDelJugador1EsError() {
        assertEquals("El nombre del Jugador 1 no puede estar vacío",
                PanelNuevaPartida.validarNombres("", "Santi"));
    }

    @Test
    void nombreSoloConEspaciosEsVacio() {
        assertEquals("El nombre del Jugador 2 no puede estar vacío",
                PanelNuevaPartida.validarNombres("Bota", "   "));
    }

    @Test
    void nombreNuloSeTrataComoVacio() {
        assertNotNull(PanelNuevaPartida.validarNombres(null, "Santi"));
        assertNotNull(PanelNuevaPartida.validarNombres("Bota", null));
    }

    @Test
    void puntoYComaEsError() {
        assertNotNull(PanelNuevaPartida.validarNombres("Bo;ta", "Santi"));
        assertNotNull(PanelNuevaPartida.validarNombres("Bota", "San;ti"));
    }

    @Test
    void nombresIgualesSonErrorSinImportarMayusculasNiEspacios() {
        assertNotNull(PanelNuevaPartida.validarNombres("Bota", "Bota"));
        assertNotNull(PanelNuevaPartida.validarNombres("Bota", "  bOTA "));
    }

    @Test
    void nombresParecidosPeroDistintosSonValidos() {
        assertNull(PanelNuevaPartida.validarNombres("Bota", "Bota2"));
    }
}
