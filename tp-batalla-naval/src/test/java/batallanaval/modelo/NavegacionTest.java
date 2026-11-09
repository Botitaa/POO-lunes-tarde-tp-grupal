package batallanaval.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class NavegacionTest {

    /** Crea una grilla de 10x10 llena de agua. */
    private Casilla[][] crearGrillaDeAgua() {
        Casilla[][] grilla = new Casilla[Tablero.FILAS][Tablero.COLUMNAS];
        for (int fila = 0; fila < Tablero.FILAS; fila++) {
            for (int columna = 0; columna < Tablero.COLUMNAS; columna++) {
                grilla[fila][columna] = Casilla.AGUA;
            }
        }
        return grilla;
    }

    /** Conjunto vacío de casillas ocupadas, para los tests sin barcos. */
    private Set<Posicion> sinOcupadas() {
        return new HashSet<>();
    }

    // ---------- en agua abierta ----------

    @Test
    void conUnPasoLlegaALas8Vecinas() {
        Tablero tablero = new Tablero(crearGrillaDeAgua());
        Set<Posicion> alcanzables = Navegacion.casillasAlcanzables(tablero, new Posicion(4, 4), 1, sinOcupadas());
        assertEquals(8, alcanzables.size());
    }

    @Test
    void conDosPasosLlegaAUnCuadradoDe5x5() {
        Tablero tablero = new Tablero(crearGrillaDeAgua());
        Set<Posicion> alcanzables = Navegacion.casillasAlcanzables(tablero, new Posicion(4, 4), 2, sinOcupadas());
        assertEquals(24, alcanzables.size()); // 5x5 menos el origen
    }

    @Test
    void enAguaAbiertaLlegaJustoHastaLaDistanciaPermitida() {
        Tablero tablero = new Tablero(crearGrillaDeAgua());
        Posicion origen = new Posicion(4, 4);
        Set<Posicion> alcanzables = Navegacion.casillasAlcanzables(tablero, origen, 3, sinOcupadas());

        for (int fila = 0; fila < Tablero.FILAS; fila++) {
            for (int columna = 0; columna < Tablero.COLUMNAS; columna++) {
                Posicion pos = new Posicion(fila, columna);
                int distancia = origen.distanciaChebyshev(pos);
                boolean deberiaLlegar = distancia >= 1 && distancia <= 3;
                assertEquals(deberiaLlegar, alcanzables.contains(pos));
            }
        }
    }

    @Test
    void noIncluyeElOrigen() {
        Tablero tablero = new Tablero(crearGrillaDeAgua());
        Set<Posicion> alcanzables = Navegacion.casillasAlcanzables(tablero, new Posicion(4, 4), 2, sinOcupadas());
        assertFalse(alcanzables.contains(new Posicion(4, 4)));
    }

    @Test
    void conCeroPasosNoLlegaANingunLado() {
        Tablero tablero = new Tablero(crearGrillaDeAgua());
        Set<Posicion> alcanzables = Navegacion.casillasAlcanzables(tablero, new Posicion(4, 4), 0, sinOcupadas());
        assertTrue(alcanzables.isEmpty());
    }

    @Test
    void enLaEsquinaNoSeSaleDelTablero() {
        Tablero tablero = new Tablero(crearGrillaDeAgua());
        Set<Posicion> alcanzables = Navegacion.casillasAlcanzables(tablero, new Posicion(0, 0), 1, sinOcupadas());
        assertEquals(3, alcanzables.size());
    }

    // ---------- obstáculos ----------

    @Test
    void noEntraEnIslasNiPiedras() {
        Casilla[][] grilla = crearGrillaDeAgua();
        grilla[4][5] = Casilla.ISLA;
        grilla[5][3] = Casilla.PIEDRA;
        Tablero tablero = new Tablero(grilla);
        Set<Posicion> alcanzables = Navegacion.casillasAlcanzables(tablero, new Posicion(4, 4), 1, sinOcupadas());

        assertEquals(6, alcanzables.size());
        assertFalse(alcanzables.contains(new Posicion(4, 5)));
        assertFalse(alcanzables.contains(new Posicion(5, 3)));
    }

    @Test
    void rodeaUnaParedDeIslasSiLeAlcanzanLosPasos() {
        Casilla[][] grilla = crearGrillaDeAgua();
        grilla[3][1] = Casilla.ISLA; // pared en la columna 1, filas 3 a 5
        grilla[4][1] = Casilla.ISLA;
        grilla[5][1] = Casilla.ISLA;
        Tablero tablero = new Tablero(grilla);
        Posicion origen = new Posicion(4, 0);

        Set<Posicion> conDos = Navegacion.casillasAlcanzables(tablero, origen, 2, sinOcupadas());
        assertFalse(conDos.contains(new Posicion(4, 2))); // detrás de la pared: no llega

        Set<Posicion> conCuatro = Navegacion.casillasAlcanzables(tablero, origen, 4, sinOcupadas());
        assertTrue(conCuatro.contains(new Posicion(4, 2))); // rodeando la pared sí llega
    }

    @Test
    void losRestosSeAtraviesan() {
        Casilla[][] grilla = crearGrillaDeAgua();
        grilla[4][5] = Casilla.RESTOS;
        Tablero tablero = new Tablero(grilla);
        Set<Posicion> alcanzables = Navegacion.casillasAlcanzables(tablero, new Posicion(4, 4), 1, sinOcupadas());

        assertEquals(8, alcanzables.size());
        assertTrue(alcanzables.contains(new Posicion(4, 5)));
    }

    // ---------- diagonal bloqueada ----------

    @Test
    void noCortaEnDiagonalEntreDosObstaculos() {
        Casilla[][] grilla = crearGrillaDeAgua();
        grilla[4][5] = Casilla.ISLA;
        grilla[5][4] = Casilla.ISLA;
        Tablero tablero = new Tablero(grilla);
        Set<Posicion> alcanzables = Navegacion.casillasAlcanzables(tablero, new Posicion(4, 4), 1, sinOcupadas());

        assertFalse(alcanzables.contains(new Posicion(5, 5)));
        assertEquals(5, alcanzables.size()); // 8 menos las 2 islas menos la diagonal
    }

    @Test
    void llegaALaDiagonalBloqueadaDandoLaVuelta() {
        Casilla[][] grilla = crearGrillaDeAgua();
        grilla[4][5] = Casilla.ISLA;
        grilla[5][4] = Casilla.ISLA;
        Tablero tablero = new Tablero(grilla);
        Set<Posicion> alcanzables = Navegacion.casillasAlcanzables(tablero, new Posicion(4, 4), 3, sinOcupadas());

        assertTrue(alcanzables.contains(new Posicion(5, 5))); // (4,4) -> (3,5) -> (4,6) -> (5,5)
    }

    // ---------- barcos ----------

    @Test
    void noEntraEnUnaCasillaOcupada() {
        Tablero tablero = new Tablero(crearGrillaDeAgua());
        Set<Posicion> ocupadas = new HashSet<>();
        ocupadas.add(new Posicion(4, 5));
        Set<Posicion> alcanzables = Navegacion.casillasAlcanzables(tablero, new Posicion(4, 4), 1, ocupadas);

        assertEquals(7, alcanzables.size());
        assertFalse(alcanzables.contains(new Posicion(4, 5)));
    }

    @Test
    void puedeRodearUnBarco() {
        Tablero tablero = new Tablero(crearGrillaDeAgua());
        Set<Posicion> ocupadas = new HashSet<>();
        ocupadas.add(new Posicion(4, 5));
        Set<Posicion> alcanzables = Navegacion.casillasAlcanzables(tablero, new Posicion(4, 4), 2, ocupadas);

        assertTrue(alcanzables.contains(new Posicion(4, 6))); // pasando por (3,5) o (5,5)
        assertFalse(alcanzables.contains(new Posicion(4, 5)));
    }

    @Test
    void elPropioBarcoEnOcupadasNoMolesta() {
        Tablero tablero = new Tablero(crearGrillaDeAgua());
        Set<Posicion> ocupadas = new HashSet<>();
        ocupadas.add(new Posicion(4, 4)); // el barco que se mueve
        Set<Posicion> alcanzables = Navegacion.casillasAlcanzables(tablero, new Posicion(4, 4), 1, ocupadas);

        assertEquals(8, alcanzables.size());
    }

    // ---------- mapa estándar ----------

    @Test
    void enElMapaEstandarNoAtraviesaLasIslas() {
        Tablero tablero = Tablero.crearMapaEstandar();
        Set<Posicion> alcanzables = Navegacion.casillasAlcanzables(tablero, new Posicion(2, 2), 2, sinOcupadas());

        assertFalse(alcanzables.contains(new Posicion(3, 2))); // isla
        assertFalse(alcanzables.contains(new Posicion(3, 3))); // isla
        assertTrue(alcanzables.contains(new Posicion(4, 1)));  // rodeando por la izquierda
    }

    // ---------- errores ----------

    @Test
    void pasosNegativosLanzanExcepcion() {
        Tablero tablero = new Tablero(crearGrillaDeAgua());
        try {
            Navegacion.casillasAlcanzables(tablero, new Posicion(4, 4), -1, sinOcupadas());
            fail("Debería haber lanzado IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // esperado
        }
    }

    @Test
    void origenFueraDelTableroLanzaExcepcion() {
        Tablero tablero = new Tablero(crearGrillaDeAgua());
        try {
            Navegacion.casillasAlcanzables(tablero, new Posicion(10, 0), 2, sinOcupadas());
            fail("Debería haber lanzado IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // esperado
        }
    }
}