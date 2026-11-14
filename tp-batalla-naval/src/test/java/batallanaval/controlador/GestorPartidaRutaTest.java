package batallanaval.controlador;

import batallanaval.excepciones.MovimientoInvalidoException;
import batallanaval.modelo.Posicion;
import batallanaval.modelo.Tablero;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests de selección de barco y planificación de ruta en GestorPartida (PAR-07, #52).
 * <p>
 * Las partidas se arman con {@link GestorPartida#restaurar}: turno de Ana (J1), flota de Ana en la fila 1
 * (acorazado 1 en (1,0), cruceros 2 y 3 en (1,1) y (1,2), destructores 4, 5 y 6 en (1,3), (1,4) y (1,5))
 * y la de Beto (J2, ids 7 a 12) en la fila 8.
 * Costos por casilla: destructor 1, crucero 2, acorazado 3. Casillas por turno: 4, 3 y 2.
 */
class GestorPartidaRutaTest {

    // ---------- armado de partidas ----------

    private static GestorPartida partida(List<DatosBarco> barcos, int puntosAna) {
        List<DatosJugador> jugadores = List.of(
                new DatosJugador(1, "Ana", puntosAna, 0, List.of()),
                new DatosJugador(2, "Beto", GestorPartida.PUNTOS_MOVIMIENTO_TURNO, 0, List.of()));
        InstantaneaPartida instantanea = new InstantaneaPartida("estandar", 1, 1, FasePartida.EN_JUEGO,
                jugadores, barcos, List.of(), List.of());
        return GestorPartida.restaurar(instantanea, Tablero.crearMapaEstandar());
    }

    private static GestorPartida partidaNormal() {
        return partida(flotas(), GestorPartida.PUNTOS_MOVIMIENTO_TURNO);
    }

    private static List<DatosBarco> flotas() {
        List<DatosBarco> barcos = new ArrayList<>();
        barcos.addAll(flota(1, 1, 1));
        barcos.addAll(flota(2, 7, 8));
        return barcos;
    }

    private static List<DatosBarco> flota(int jugador, int primerId, int fila) {
        return List.of(
                new DatosBarco(primerId, 'A', jugador, new Posicion(fila, 0), 8, 0, 0, 2, false, false),
                new DatosBarco(primerId + 1, 'C', jugador, new Posicion(fila, 1), 5, 0, 0, 3, false, false),
                new DatosBarco(primerId + 2, 'C', jugador, new Posicion(fila, 2), 5, 0, 0, 3, false, false),
                new DatosBarco(primerId + 3, 'D', jugador, new Posicion(fila, 3), 3, 0, 0, 4, false, false),
                new DatosBarco(primerId + 4, 'D', jugador, new Posicion(fila, 4), 3, 0, 0, 4, false, false),
                new DatosBarco(primerId + 5, 'D', jugador, new Posicion(fila, 5), 3, 0, 0, 4, false, false));
    }

    /** Devuelve las flotas de siempre con el barco {@code nuevo.id()} reemplazado. */
    private static List<DatosBarco> con(DatosBarco nuevo) {
        List<DatosBarco> resultado = new ArrayList<>();
        for (DatosBarco barco : flotas()) {
            resultado.add(barco.id() == nuevo.id() ? nuevo : barco);
        }
        return resultado;
    }

    /** El destructor 4 de Ana, sano, en la posición indicada. */
    private static DatosBarco destructorEn(Posicion posicion) {
        return new DatosBarco(4, 'D', 1, posicion, 3, 0, 0, 4, false, false);
    }

    // ---------- seleccionarBarco ----------

    @Test
    void sinSeleccionNoHayBarcoNiRutaNiCosto() {
        GestorPartida partida = partidaNormal();

        assertNull(partida.getBarcoSeleccionado());
        assertTrue(partida.getRutaPlanificada().isEmpty());
        assertEquals(0, partida.getCostoRuta());
        assertTrue(partida.getCasillasAlcanzables().isEmpty());
    }

    @Test
    void seleccionarUnBarcoPropioLoDejaSeleccionado() {
        GestorPartida partida = partidaNormal();

        partida.seleccionarBarco(4);

        assertEquals(4, partida.getBarcoSeleccionado().id());
    }

    @Test
    void noSePuedeSeleccionarUnBarcoDelRival() {
        GestorPartida partida = partidaNormal();

        assertThrows(MovimientoInvalidoException.class, () -> partida.seleccionarBarco(7));
    }

    @Test
    void noSePuedeSeleccionarUnBarcoHundido() {
        GestorPartida partida = partida(con(new DatosBarco(4, 'D', 1, new Posicion(1, 3), 0, 0, 0, 4, false, false)),
                GestorPartida.PUNTOS_MOVIMIENTO_TURNO);

        assertThrows(MovimientoInvalidoException.class, () -> partida.seleccionarBarco(4));
    }

    @Test
    void unBarcoSinCasillasSePuedeSeleccionarPeroNoMover() {
        // Sirve para disparar o usar la habilidad aunque ya no pueda moverse.
        GestorPartida partida = partida(con(new DatosBarco(4, 'D', 1, new Posicion(1, 3), 3, 0, 0, 0, false, false)),
                GestorPartida.PUNTOS_MOVIMIENTO_TURNO);

        partida.seleccionarBarco(4);

        assertEquals(4, partida.getBarcoSeleccionado().id());
        assertTrue(partida.getCasillasAlcanzables().isEmpty());
        assertThrows(MovimientoInvalidoException.class, () -> partida.agregarPaso(new Posicion(2, 3)));
    }

    @Test
    void cambiarDeBarcoDescartaLaRutaAnterior() {
        GestorPartida partida = partidaNormal();
        partida.seleccionarBarco(4);
        partida.agregarPaso(new Posicion(2, 3));

        partida.seleccionarBarco(5);

        assertTrue(partida.getRutaPlanificada().isEmpty());
    }

    // ---------- agregarPaso ----------

    @Test
    void agregarUnPasoVecinoLoSumaALaRutaConSuCosto() {
        GestorPartida partida = partidaNormal();
        partida.seleccionarBarco(4);

        partida.agregarPaso(new Posicion(2, 3));
        partida.agregarPaso(new Posicion(2, 4));

        assertEquals(List.of(new Posicion(2, 3), new Posicion(2, 4)), partida.getRutaPlanificada());
        assertEquals(2, partida.getCostoRuta()); // destructor: 1 punto por casilla
    }

    @Test
    void sinBarcoSeleccionadoNoSePuedeAgregarPaso() {
        GestorPartida partida = partidaNormal();

        assertThrows(MovimientoInvalidoException.class, () -> partida.agregarPaso(new Posicion(2, 3)));
    }

    @Test
    void noSePuedeSaltarAUnaCasillaQueNoEsVecina() {
        GestorPartida partida = partidaNormal();
        partida.seleccionarBarco(4);

        assertThrows(MovimientoInvalidoException.class, () -> partida.agregarPaso(new Posicion(3, 3)));
    }

    @Test
    void elPasoSiguienteTieneQueSerVecinoDelUltimoDeLaRuta() {
        GestorPartida partida = partidaNormal();
        partida.seleccionarBarco(4);
        partida.agregarPaso(new Posicion(2, 3));

        // (2,5) es vecina... del barco no, y del último paso tampoco (distancia 2).
        assertThrows(MovimientoInvalidoException.class, () -> partida.agregarPaso(new Posicion(2, 5)));
    }

    @Test
    void noSePuedePasarPorUnaIsla() {
        // (3,3) es isla en el mapa estándar.
        GestorPartida partida = partida(con(destructorEn(new Posicion(2, 3))), GestorPartida.PUNTOS_MOVIMIENTO_TURNO);
        partida.seleccionarBarco(4);

        assertThrows(MovimientoInvalidoException.class, () -> partida.agregarPaso(new Posicion(3, 3)));
    }

    @Test
    void noSePuedeCruzarUnaDiagonalBloqueada() {
        // De (5,2) a (6,3): las esquinas (5,3) y (6,2) son piedras, así que la diagonal está bloqueada.
        GestorPartida partida = partida(con(destructorEn(new Posicion(5, 2))), GestorPartida.PUNTOS_MOVIMIENTO_TURNO);
        partida.seleccionarBarco(4);

        assertThrows(MovimientoInvalidoException.class, () -> partida.agregarPaso(new Posicion(6, 3)));
    }

    @Test
    void noSePuedePasarPorUnaCasillaConOtroBarcoPropio() {
        GestorPartida partida = partidaNormal();
        partida.seleccionarBarco(4);

        assertThrows(MovimientoInvalidoException.class, () -> partida.agregarPaso(new Posicion(1, 4)));
    }

    @Test
    void noSePuedeAvanzarMasCasillasDeLasQueLeQuedan() {
        // El acorazado avanza 2 casillas por turno.
        GestorPartida partida = partidaNormal();
        partida.seleccionarBarco(1);
        partida.agregarPaso(new Posicion(2, 0));
        partida.agregarPaso(new Posicion(3, 0));

        assertThrows(MovimientoInvalidoException.class, () -> partida.agregarPaso(new Posicion(4, 0)));
    }

    @Test
    void noSePuedeAgregarUnPasoQueNoAlcanzaConLosPuntos() {
        // Crucero: 2 puntos por casilla. Con 4 puntos llega a 2 casillas aunque podría hacer 3.
        GestorPartida partida = partida(flotas(), 4);
        partida.seleccionarBarco(2);
        partida.agregarPaso(new Posicion(2, 1));
        partida.agregarPaso(new Posicion(3, 1));

        assertThrows(MovimientoInvalidoException.class, () -> partida.agregarPaso(new Posicion(4, 1)));
    }

    // ---------- deshacerPaso y cancelarSeleccion ----------

    @Test
    void deshacerPasoSacaElUltimo() {
        GestorPartida partida = partidaNormal();
        partida.seleccionarBarco(4);
        partida.agregarPaso(new Posicion(2, 3));
        partida.agregarPaso(new Posicion(2, 4));

        partida.deshacerPaso();

        assertEquals(List.of(new Posicion(2, 3)), partida.getRutaPlanificada());
        assertEquals(1, partida.getCostoRuta());
    }

    @Test
    void deshacerConLaRutaVaciaNoHaceNada() {
        GestorPartida partida = partidaNormal();
        partida.seleccionarBarco(4);

        partida.deshacerPaso();

        assertTrue(partida.getRutaPlanificada().isEmpty());
        assertEquals(4, partida.getBarcoSeleccionado().id());
    }

    @Test
    void cancelarSeleccionDescartaBarcoYRuta() {
        GestorPartida partida = partidaNormal();
        partida.seleccionarBarco(4);
        partida.agregarPaso(new Posicion(2, 3));

        partida.cancelarSeleccion();

        assertNull(partida.getBarcoSeleccionado());
        assertTrue(partida.getRutaPlanificada().isEmpty());
    }

    @Test
    void conUnaRutaAMediasNoSePuedeGuardarYAlCancelarSi() {
        GestorPartida partida = partidaNormal();
        partida.seleccionarBarco(4);
        partida.agregarPaso(new Posicion(2, 3));

        assertFalse(partida.puedeGuardarse());
        partida.cancelarSeleccion();
        assertTrue(partida.puedeGuardarse());
    }

    // ---------- getCasillasAlcanzables ----------

    @Test
    void lasAlcanzablesNoIncluyenCasillasConBarcosPropios() {
        GestorPartida partida = partidaNormal();
        partida.seleccionarBarco(4);

        Set<Posicion> alcanzables = partida.getCasillasAlcanzables();

        assertFalse(alcanzables.contains(new Posicion(1, 4)));
        assertFalse(alcanzables.contains(new Posicion(1, 2)));
        assertTrue(alcanzables.contains(new Posicion(2, 3)));
    }

    @Test
    void lasAlcanzablesRespetanLosPuntos() {
        // Acorazado (3 puntos por casilla) con 3 puntos: solo llega a casillas vecinas.
        GestorPartida partida = partida(flotas(), 3);
        partida.seleccionarBarco(1);

        Set<Posicion> alcanzables = partida.getCasillasAlcanzables();

        assertFalse(alcanzables.isEmpty());
        for (Posicion posicion : alcanzables) {
            assertEquals(1, new Posicion(1, 0).distanciaChebyshev(posicion));
        }
    }

    @Test
    void lasAlcanzablesSeCalculanDesdeElFinDeLaRuta() {
        // Acorazado: 2 casillas. Después de 1 paso a (2,0) le queda 1, así que todo queda a 1 de (2,0).
        GestorPartida partida = partidaNormal();
        partida.seleccionarBarco(1);
        partida.agregarPaso(new Posicion(2, 0));

        Set<Posicion> alcanzables = partida.getCasillasAlcanzables();

        assertTrue(alcanzables.contains(new Posicion(3, 0)));
        for (Posicion posicion : alcanzables) {
            assertEquals(1, new Posicion(2, 0).distanciaChebyshev(posicion));
        }
    }

    // ---------- relación con el turno ----------

    @Test
    void alTerminarElTurnoSeDescartaLaSeleccion() {
        GestorPartida partida = partidaNormal();
        partida.seleccionarBarco(4);
        partida.agregarPaso(new Posicion(2, 3));

        partida.terminarTurno();

        assertNull(partida.getBarcoSeleccionado());
        assertTrue(partida.getRutaPlanificada().isEmpty());
    }
}