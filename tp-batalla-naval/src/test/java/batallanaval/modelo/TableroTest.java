package batallanaval.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import batallanaval.excepciones.MapaInvalidoException;
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
            fail("Debería haber lanzado MapaInvalidoException");
        } catch (MapaInvalidoException e) {
            // esperado
        }
    }

    @Test
    void grillaConMenosFilasLanzaExcepcion() {
        try {
            new Tablero(new Casilla[9][10]);
            fail("Debería haber lanzado MapaInvalidoException");
        } catch (MapaInvalidoException e) {
            // esperado
        }
    }

    @Test
    void filaConMasColumnasLanzaExcepcion() {
        Casilla[][] grilla = crearGrillaDeAgua();
        grilla[2] = new Casilla[11];
        try {
            new Tablero(grilla);
            fail("Debería haber lanzado MapaInvalidoException");
        } catch (MapaInvalidoException e) {
            // esperado
        }
    }

    @Test
    void casillaNullLanzaExcepcion() {
        Casilla[][] grilla = crearGrillaDeAgua();
        grilla[7][8] = null;
        try {
            new Tablero(grilla);
            fail("Debería haber lanzado MapaInvalidoException");
        } catch (MapaInvalidoException e) {
            assertTrue(e.getMessage().contains("(7,8)"));
        }

    }  @Test
    void filaNullLanzaExcepcion() {
        Casilla[][] grilla = crearGrillaDeAgua();
        grilla[2] = null;
        try {
            new Tablero(grilla);
            fail("Debería haber lanzado MapaInvalidoException");
        } catch (MapaInvalidoException e) {
            // esperado
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
    // ---------- validarMapa: zonas ----------

    @Test
    void obstaculosEnLaZonaNeutralSonValidos() {
        Casilla[][] grilla = crearGrillaDeAgua();
        grilla[3][0] = Casilla.ISLA;   // primera fila neutral
        grilla[6][9] = Casilla.PIEDRA; // última fila neutral
        Tablero tablero = new Tablero(grilla);

        assertSame(Casilla.ISLA, tablero.getCasilla(3, 0));
        assertSame(Casilla.PIEDRA, tablero.getCasilla(6, 9));
    }

    @Test
    void islaEnLaZonaDelJugador1LanzaExcepcion() {
        Casilla[][] grilla = crearGrillaDeAgua();
        grilla[2][5] = Casilla.ISLA; // última fila de despliegue de J1
        try {
            new Tablero(grilla);
            fail("Debería haber lanzado MapaInvalidoException");
        } catch (MapaInvalidoException e) {
            // esperado
        }
    }

    @Test
    void piedraEnLaZonaDelJugador1LanzaExcepcion() {
        Casilla[][] grilla = crearGrillaDeAgua();
        grilla[0][0] = Casilla.PIEDRA;
        try {
            new Tablero(grilla);
            fail("Debería haber lanzado MapaInvalidoException");
        } catch (MapaInvalidoException e) {
            // esperado
        }
    }

    @Test
    void obstaculoEnLaZonaDelJugador2LanzaExcepcion() {
        Casilla[][] grilla = crearGrillaDeAgua();
        grilla[7][3] = Casilla.PIEDRA; // primera fila de despliegue de J2
        try {
            new Tablero(grilla);
            fail("Debería haber lanzado MapaInvalidoException");
        } catch (MapaInvalidoException e) {
            // esperado
        }
    }

    @Test
    void restosEnLaZonaDeDespliegueNoSonObstaculo() {
        Casilla[][] grilla = crearGrillaDeAgua();
        grilla[1][1] = Casilla.RESTOS;
        grilla[8][8] = Casilla.RESTOS;
        Tablero tablero = new Tablero(grilla);

        assertSame(Casilla.RESTOS, tablero.getCasilla(1, 1));
    }
    // ---------- crearMapaEstandar ----------

    @Test
    void elMapaEstandarSeCreaSinErrores() {
        Tablero tablero = Tablero.crearMapaEstandar();
        assertSame(Casilla.AGUA, tablero.getCasilla(0, 0));
    }

    @Test
    void elMapaEstandarTieneLosObstaculosDelMvp() {
        Tablero tablero = Tablero.crearMapaEstandar();
        assertSame(Casilla.ISLA, tablero.getCasilla(3, 2));
        assertSame(Casilla.ISLA, tablero.getCasilla(3, 3));
        assertSame(Casilla.PIEDRA, tablero.getCasilla(3, 7));
        assertSame(Casilla.ISLA, tablero.getCasilla(4, 2));
        assertSame(Casilla.PIEDRA, tablero.getCasilla(4, 6));
        assertSame(Casilla.PIEDRA, tablero.getCasilla(5, 3));
        assertSame(Casilla.ISLA, tablero.getCasilla(5, 7));
        assertSame(Casilla.PIEDRA, tablero.getCasilla(6, 2));
        assertSame(Casilla.ISLA, tablero.getCasilla(6, 6));
        assertSame(Casilla.ISLA, tablero.getCasilla(6, 7));
    }

    @Test
    void elMapaEstandarTieneDiezObstaculos() {
        Tablero tablero = Tablero.crearMapaEstandar();
        int obstaculos = 0;
        for (int fila = 0; fila < Tablero.FILAS; fila++) {
            for (int columna = 0; columna < Tablero.COLUMNAS; columna++) {
                if (tablero.getCasilla(fila, columna).bloqueaPaso()) {
                    obstaculos++;
                }
            }
        }
        assertEquals(10, obstaculos);
    }

    @Test
    void laZonaDeDominacionDelMapaEstandarEsAgua() {
        Tablero tablero = Tablero.crearMapaEstandar();
        assertSame(Casilla.AGUA, tablero.getCasilla(4, 4));
        assertSame(Casilla.AGUA, tablero.getCasilla(4, 5));
        assertSame(Casilla.AGUA, tablero.getCasilla(5, 4));
        assertSame(Casilla.AGUA, tablero.getCasilla(5, 5));
    }

    @Test
    void elMapaEstandarEsSimetricoRespectoDelCentro() {
        Tablero tablero = Tablero.crearMapaEstandar();
        for (int fila = 0; fila < Tablero.FILAS; fila++) {
            for (int columna = 0; columna < Tablero.COLUMNAS; columna++) {
                int filaEspejo = Tablero.FILAS - 1 - fila;
                int columnaEspejo = Tablero.COLUMNAS - 1 - columna;
                assertSame(tablero.getCasilla(fila, columna),
                        tablero.getCasilla(filaEspejo, columnaEspejo));
            }
        }
    }

    @Test
    void cadaLlamadaDevuelveUnTableroNuevo() {
        Tablero primero = Tablero.crearMapaEstandar();
        Tablero segundo = Tablero.crearMapaEstandar();
        assertFalse(primero == segundo);
    }
    // ---------- esZonaDespliegue ----------

    @Test
    void lasFilas0a2SonZonaDelJugador1() {
        Tablero tablero = new Tablero(crearGrillaDeAgua());
        assertTrue(tablero.esZonaDespliegue(new Posicion(0, 0), 1));
        assertTrue(tablero.esZonaDespliegue(new Posicion(2, 9), 1));
    }

    @Test
    void laFila3NoEsZonaDelJugador1() {
        Tablero tablero = new Tablero(crearGrillaDeAgua());
        assertFalse(tablero.esZonaDespliegue(new Posicion(3, 0), 1));
    }

    @Test
    void lasFilas7a9SonZonaDelJugador2() {
        Tablero tablero = new Tablero(crearGrillaDeAgua());
        assertTrue(tablero.esZonaDespliegue(new Posicion(7, 0), 2));
        assertTrue(tablero.esZonaDespliegue(new Posicion(9, 9), 2));
    }

    @Test
    void laFila6NoEsZonaDelJugador2() {
        Tablero tablero = new Tablero(crearGrillaDeAgua());
        assertFalse(tablero.esZonaDespliegue(new Posicion(6, 0), 2));
    }

    @Test
    void laZonaDeUnJugadorNoEsLaDelOtro() {
        Tablero tablero = new Tablero(crearGrillaDeAgua());
        assertFalse(tablero.esZonaDespliegue(new Posicion(0, 0), 2));
        assertFalse(tablero.esZonaDespliegue(new Posicion(9, 9), 1));
    }

    @Test
    void fueraDelTableroNoEsZonaDeDespliegue() {
        Tablero tablero = new Tablero(crearGrillaDeAgua());
        assertFalse(tablero.esZonaDespliegue(new Posicion(-1, 0), 1));
        assertFalse(tablero.esZonaDespliegue(new Posicion(0, 10), 1));
        assertFalse(tablero.esZonaDespliegue(new Posicion(10, 0), 2));
        assertFalse(tablero.esZonaDespliegue(new Posicion(9, -1), 2));
    }

    @Test
    void numeroDeJugadorInvalidoLanzaExcepcion() {
        Tablero tablero = new Tablero(crearGrillaDeAgua());
        try {
            tablero.esZonaDespliegue(new Posicion(0, 0), 3);
            fail("Debería haber lanzado IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // esperado
        }
    }

    @Test
    void jugadorCeroTambienEsInvalido() {
        Tablero tablero = new Tablero(crearGrillaDeAgua());
        try {
            tablero.esZonaDespliegue(new Posicion(0, 0), 0);
            fail("Debería haber lanzado IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // esperado
        }
    }

    // ---------- esZonaDominacion ----------

    @Test
    void lasCuatroCasillasCentralesSonZonaDeDominacion() {
        Tablero tablero = new Tablero(crearGrillaDeAgua());
        assertTrue(tablero.esZonaDominacion(new Posicion(4, 4)));
        assertTrue(tablero.esZonaDominacion(new Posicion(4, 5)));
        assertTrue(tablero.esZonaDominacion(new Posicion(5, 4)));
        assertTrue(tablero.esZonaDominacion(new Posicion(5, 5)));
    }

    @Test
    void lasVecinasDeLaZonaCentralNoSonDominacion() {
        Tablero tablero = new Tablero(crearGrillaDeAgua());
        assertFalse(tablero.esZonaDominacion(new Posicion(3, 4))); // arriba
        assertFalse(tablero.esZonaDominacion(new Posicion(6, 5))); // abajo
        assertFalse(tablero.esZonaDominacion(new Posicion(4, 3))); // izquierda
        assertFalse(tablero.esZonaDominacion(new Posicion(5, 6))); // derecha
        assertFalse(tablero.esZonaDominacion(new Posicion(3, 3))); // diagonal
    }

    @Test
    void unaCasillaNeutralCualquieraNoEsDominacion() {
        Tablero tablero = new Tablero(crearGrillaDeAgua());
        assertFalse(tablero.esZonaDominacion(new Posicion(3, 0)));
        assertFalse(tablero.esZonaDominacion(new Posicion(4, 9)));
    }

    @Test
    void fueraDelTableroNoEsZonaDeDominacion() {
        Tablero tablero = new Tablero(crearGrillaDeAgua());
        assertFalse(tablero.esZonaDominacion(new Posicion(-1, 4)));
        assertFalse(tablero.esZonaDominacion(new Posicion(4, 10)));
    }
}