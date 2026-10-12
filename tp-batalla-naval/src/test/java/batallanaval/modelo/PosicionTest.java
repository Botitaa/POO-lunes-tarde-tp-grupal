package batallanaval.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;

import org.junit.jupiter.api.Test;

class PosicionTest {

    // ---------- getters ----------

    @Test
    void guardaFilaYColumna() {
        Posicion p = new Posicion(4, 7);
        assertEquals(4, p.getFila());
        assertEquals(7, p.getColumna());
    }

    // ---------- equals ----------

    @Test
    void dosPosicionesConMismosValoresSonIguales() {
        Posicion a = new Posicion(2, 3);
        Posicion b = new Posicion(2, 3);
        assertEquals(a, b);
    }

    @Test
    void unaPosicionEsIgualASiMisma() {
        Posicion a = new Posicion(2, 3);
        assertEquals(a, a);
    }

    @Test
    void distintaFilaNoEsIgual() {
        assertNotEquals(new Posicion(2, 3), new Posicion(5, 3));
    }

    @Test
    void distintaColumnaNoEsIgual() {
        assertNotEquals(new Posicion(2, 3), new Posicion(2, 8));
    }

    @Test
    void filaYColumnaInvertidasNoSonIguales() {
        assertNotEquals(new Posicion(2, 3), new Posicion(3, 2));
    }

    @Test
    void noEsIgualANull() {
        assertFalse(new Posicion(2, 3).equals(null));
    }

    @Test
    void noEsIgualAOtroTipoDeObjeto() {
        assertFalse(new Posicion(2, 3).equals("(2,3)"));
    }

    // ---------- hashCode ----------

    @Test
    void posicionesIgualesTienenElMismoHash() {
        Posicion a = new Posicion(6, 1);
        Posicion b = new Posicion(6, 1);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void hashSetNoGuardaPosicionesRepetidas() {
        HashSet<Posicion> conjunto = new HashSet<Posicion>();
        conjunto.add(new Posicion(1, 1));
        conjunto.add(new Posicion(1, 1));
        conjunto.add(new Posicion(1, 2));
        assertEquals(2, conjunto.size());
    }

    @Test
    void hashSetEncuentraUnaPosicionCreadaAparte() {
        HashSet<Posicion> conjunto = new HashSet<Posicion>();
        conjunto.add(new Posicion(4, 5));
        assertTrue(conjunto.contains(new Posicion(4, 5)));
        assertFalse(conjunto.contains(new Posicion(5, 4)));
    }

    // ---------- distanciaChebyshev ----------

    @Test
    void distanciaAMismaPosicionEsCero() {
        Posicion p = new Posicion(3, 3);
        assertEquals(0, p.distanciaChebyshev(new Posicion(3, 3)));
    }

    @Test
    void distanciaEnLineaRecta() {
        Posicion p = new Posicion(0, 0);
        assertEquals(4, p.distanciaChebyshev(new Posicion(0, 4)));
        assertEquals(4, p.distanciaChebyshev(new Posicion(4, 0)));
    }

    @Test
    void distanciaEnDiagonalCuentaUnPasoPorCasilla() {
        Posicion p = new Posicion(0, 0);
        assertEquals(3, p.distanciaChebyshev(new Posicion(3, 3)));
    }

    @Test
    void distanciaTomaLaMayorDiferencia() {
        Posicion p = new Posicion(2, 3);
        assertEquals(3, p.distanciaChebyshev(new Posicion(5, 4)));
    }

    @Test
    void distanciaEsLaMismaEnAmbosSentidos() {
        Posicion a = new Posicion(1, 8);
        Posicion b = new Posicion(7, 2);
        assertEquals(a.distanciaChebyshev(b), b.distanciaChebyshev(a));
        assertEquals(6, a.distanciaChebyshev(b));
    }

    // ---------- toString ----------

    @Test
    void toStringMuestraFilaYColumna() {
        assertEquals("(4,5)", new Posicion(4, 5).toString());
    }
}