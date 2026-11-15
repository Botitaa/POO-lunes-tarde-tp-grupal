package batallanaval.controlador;

import batallanaval.modelo.Disparo;
import batallanaval.modelo.Posicion;
import org.junit.jupiter.api.Test;

import static batallanaval.controlador.ReglasDeJuegoTest.b;
import static batallanaval.controlador.ReglasDeJuegoTest.mapa;
import static batallanaval.controlador.ReglasDeJuegoTest.partida;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de lo que la partida le da al tablero para las ayudas visuales: el último disparo
 * (para animarlo), los disparos recibidos en el turno del rival y qué barcos pueden actuar.
 */
class AyudasVisualesPartidaTest {

    @Test
    void alDispararQuedaElUltimoDisparoYElRivalLoVeAlEmpezarSuTurno() {
        // Crucero J1 en (2,2) le dispara al acorazado J2 en (4,2)
        GestorPartida p = partida(mapa(), b(2, 'C', 1, 2, 2), b(7, 'A', 2, 4, 2));
        assertNull(p.getUltimoDisparo());
        assertTrue(p.puedeActuar(2));
        p.seleccionarBarco(2);
        p.disparar(7);

        Disparo ultimo = p.getUltimoDisparo();
        assertEquals(new Posicion(2, 2), ultimo.origen());
        assertEquals(new Posicion(4, 2), ultimo.destino());
        assertTrue(ultimo.danio() > 0);
        assertFalse(ultimo.hundimiento());
        assertFalse(p.puedeActuar(2), "un barco que disparó ya no se mueve ni vuelve a disparar");

        p.terminarTurno();
        assertNull(p.getUltimoDisparo(), "el turno nuevo empieza sin disparo");
        assertEquals(1, p.getDisparosRecibidos().size());
        assertEquals(new Posicion(4, 2), p.getDisparosRecibidos().get(0).destino());
        assertEquals(new Posicion(2, 2), p.getDisparosRecibidos().get(0).origen());
    }

    @Test
    void unHundimientoQuedaMarcadoEnElDisparoYEnLoQueRecibeElRival() {
        GestorPartida p = partida(mapa(), b(4, 'D', 1, 2, 2), b(10, 'D', 2, 4, 2));
        p.seleccionarBarco(4);
        p.disparar(10);
        assertTrue(p.getUltimoDisparo().hundimiento());
        p.terminarTurno();
        assertTrue(p.getDisparosRecibidos().get(0).hundimiento());
    }

    @Test
    void unBarcoDeOtroJugadorNoPuedeActuar() {
        GestorPartida p = partida(mapa());
        assertFalse(p.puedeActuar(7));
    }
}
