package batallanaval.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

class TableroTest {

    /** Crea una grilla de 10x10 llena de agua para usar en los tests. */
    private Casilla[][] crearGrillaDeAgua() {
        Casilla[][] grilla = new Casilla[Tablero.FILAS][Tablero.COLUMNAS];
        for (int fila = 0; fila < Tablero.FILAS; fila++) {
            for (int columna = 0; columna < Tablero.COLUMNAS; columna++) {
                grilla[fila][columna] = Casilla.AGUA;
            }
        }
        return grilla;
    }

    // ---------- constantes ----------

    @Test
    void elTableroEsDe10x10() {
        assertEquals(10, Tablero.FILAS);
        assertEquals(10, Tablero.COLUMNAS);
    }

    // ---------- constructor y copia defensiva ----------

    @Test
    void guardaLasCasillasDeLaGrilla() {
        Casilla[][] grilla = crearGrillaDeAgua();
        grilla[3][4] = Casilla.ISLA;
        grilla[6][1] = Casilla.PIEDRA;

        Tablero tablero = new Tablero(grilla);

        assertSame(Casilla.ISLA, tablero.getCasilla(3, 4));
        assertSame(Casilla.PIEDRA, tablero.getCasilla(6, 1));
        assertSame(Casilla.AGUA, tablero.getCasilla(0, 0));
    }

    @Test
    void cambiarLaGrillaOriginalNoCambiaElTablero() {
        Casilla[][] grilla = crearGrillaDeAgua();
        Tablero tablero = new Tablero(grilla);

        grilla[5][5] = Casilla.ISLA;

        assertSame(Casilla.AGUA, tablero.getCasilla(5, 5));
    }

    @Test
    void grillaNullLanzaExcepcion() {
        try {
            new Tablero(null);
            fail("Debería haber lanzado IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // esperado
        }
    }

    @Test
    void grillaConMenosFilasLanzaExcepcion() {
        try {
            new Tablero(new Casilla[9][10]);
            fail("Debería haber lanzado IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // esperado
        }
    }

    @Test
    void filaConMasColumnasLanzaExcepcion() {
        Casilla[][] grilla = crearGrillaDeAgua();
        grilla[2] = new Casilla[11];
        try {
            new Tablero(grilla);
            fail("Debería haber lanzado IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // esperado
        }
    }

    @Test
    void casillaNullLanzaExcepcion() {
        Casilla[][] grilla = crearGrillaDeAgua();
        grilla[7][8] = null;
        try {
            new Tablero(grilla);
            fail("Debería haber lanzado IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("(7,8)"));
        }
    }

    // ---------- estaDentro (sobrecarga) ----------

    @Test
    void lasEsquinasEstanDentro() {
        Tablero tablero = new Tablero(crearGrillaDeAgua());
        assertTrue(tablero.estaDentro(0, 0));
        assertTrue(tablero.estaDentro(0, 9));
        assertTrue(tablero.estaDentro(9, 0));
        assertTrue(tablero.estaDentro(9, 9));
    }

    @Test
    void justoAfueraDelBordeNoEstaDentro() {
        Tablero tablero = new Tablero(crearGrillaDeAgua());
        assertFalse(tablero.estaDentro(-1, 5));
        assertFalse(tablero.estaDentro(10, 5));
        assertFalse(tablero.estaDentro(5, -1));
        assertFalse(tablero.estaDentro(5, 10));
    }

    @Test
    void estaDentroConPosicionDaLoMismoQueConFilaYColumna() {
        Tablero tablero = new Tablero(crearGrillaDeAgua());
        assertTrue(tablero.estaDentro(new Posicion(4, 5)));
        assertFalse(tablero.estaDentro(new Posicion(-1, 5)));
        assertFalse(tablero.estaDentro(new Posicion(4, 10)));
    }

    // ---------- getCasilla (sobrecarga) ----------

    @Test
    void getCasillaConPosicionDaLoMismoQueConFilaYColumna() {
        Casilla[][] grilla = crearGrillaDeAgua();
        grilla[4][6] = Casilla.PIEDRA;
        Tablero tablero = new Tablero(grilla);

        assertSame(tablero.getCasilla(4, 6), tablero.getCasilla(new Posicion(4, 6)));
    }

    @Test
    void getCasillaFueraDelTableroLanzaExcepcion() {
        Tablero tablero = new Tablero(crearGrillaDeAgua());
        try {
            tablero.getCasilla(10, 0);
            fail("Debería haber lanzado IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // esperado
        }
    }

    @Test
    void getCasillaConPosicionFueraDelTableroLanzaExcepcion() {
        Tablero tablero = new Tablero(crearGrillaDeAgua());
        try {
            tablero.getCasilla(new Posicion(0, -1));
            fail("Debería haber lanzado IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // esperado
        }
    }

    // ---------- esTransitable ----------

    @Test
    void aguaYRestosSonTransitables() {
        Casilla[][] grilla = crearGrillaDeAgua();
        grilla[3][3] = Casilla.RESTOS;
        Tablero tablero = new Tablero(grilla);

        assertTrue(tablero.esTransitable(new Posicion(0, 0)));
        assertTrue(tablero.esTransitable(new Posicion(3, 3)));
    }

    @Test
    void piedraEIslaNoSonTransitables() {
        Casilla[][] grilla = crearGrillaDeAgua();
        grilla[4][2] = Casilla.PIEDRA;
        grilla[5][7] = Casilla.ISLA;
        Tablero tablero = new Tablero(grilla);

        assertFalse(tablero.esTransitable(new Posicion(4, 2)));
        assertFalse(tablero.esTransitable(new Posicion(5, 7)));
    }

    @Test
    void fueraDelTableroNoEsTransitable() {
        Tablero tablero = new Tablero(crearGrillaDeAgua());
        assertFalse(tablero.esTransitable(new Posicion(-1, 0)));
        assertFalse(tablero.esTransitable(new Posicion(0, 10)));
    }
}