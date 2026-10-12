package batallanaval.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

class CasillaTest {

    // ---------- bloqueos (reglas del juego) ----------

    @Test
    void aguaNoBloqueaNada() {
        assertFalse(Casilla.AGUA.bloqueaPaso());
        assertFalse(Casilla.AGUA.bloqueaVision());
    }

    @Test
    void piedraBloqueaSoloElPaso() {
        assertTrue(Casilla.PIEDRA.bloqueaPaso());
        assertFalse(Casilla.PIEDRA.bloqueaVision());
    }

    @Test
    void islaBloqueaPasoYVision() {
        assertTrue(Casilla.ISLA.bloqueaPaso());
        assertTrue(Casilla.ISLA.bloqueaVision());
    }

    @Test
    void restosNoBloqueanNada() {
        assertFalse(Casilla.RESTOS.bloqueaPaso());
        assertFalse(Casilla.RESTOS.bloqueaVision());
    }

    // ---------- símbolos ----------

    @Test
    void cadaCasillaTieneSuSimbolo() {
        assertEquals('~', Casilla.AGUA.getSimbolo());
        assertEquals('P', Casilla.PIEDRA.getSimbolo());
        assertEquals('I', Casilla.ISLA.getSimbolo());
        assertEquals('X', Casilla.RESTOS.getSimbolo());
    }

    // ---------- desdeSimbolo ----------

    @Test
    void desdeSimboloDevuelveLaCasillaCorrecta() {
        assertSame(Casilla.AGUA, Casilla.desdeSimbolo('~'));
        assertSame(Casilla.PIEDRA, Casilla.desdeSimbolo('P'));
        assertSame(Casilla.ISLA, Casilla.desdeSimbolo('I'));
        assertSame(Casilla.RESTOS, Casilla.desdeSimbolo('X'));
    }

    @Test
    void desdeSimboloEsLaInversaDeGetSimbolo() {
        Casilla[] todas = Casilla.values();
        for (int i = 0; i < todas.length; i++) {
            assertSame(todas[i], Casilla.desdeSimbolo(todas[i].getSimbolo()));
        }
    }

    @Test
    void desdeSimboloDesconocidoLanzaExcepcion() {
        try {
            Casilla.desdeSimbolo('Z');
            fail("Debería haber lanzado IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Z"));
        }
    }

    @Test
    void desdeSimboloDistingueMayusculas() {
        try {
            Casilla.desdeSimbolo('i');
            fail("Debería haber lanzado IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // esperado: el mapa usa 'I' mayúscula
        }
    }

    @Test
    void hayExactamenteCuatroCasillas() {
        assertEquals(4, Casilla.values().length);
    }
}