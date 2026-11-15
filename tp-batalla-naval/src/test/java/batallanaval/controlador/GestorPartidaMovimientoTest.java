package batallanaval.controlador;

import batallanaval.excepciones.MovimientoInvalidoException;
import batallanaval.historial.EventoPartida;
import batallanaval.historial.TipoEvento;
import batallanaval.modelo.Posicion;
import batallanaval.modelo.Tablero;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests de confirmarMovimiento en GestorPartida (PAR-08, #53): mover, gastar puntos y casillas,
 * y frenar al chocar con un barco enemigo que no se veía.
 * <p>
 * Turno de Ana (J1). Flota de Ana en la fila 1 (destructor 4 en (1,3), 1 punto por casilla, 4 casillas)
 * y la de Beto (J2, ids 7 a 12) en la fila 8, salvo que el test mueva alguno.
 */
class GestorPartidaMovimientoTest {

    // ---------- armado ----------

    private static GestorPartida partida(List<DatosBarco> barcos) {
        List<DatosJugador> jugadores = List.of(
                new DatosJugador(1, "Ana", GestorPartida.PUNTOS_MOVIMIENTO_TURNO, 0, List.of()),
                new DatosJugador(2, "Beto", GestorPartida.PUNTOS_MOVIMIENTO_TURNO, 0, List.of()));
        InstantaneaPartida instantanea = new InstantaneaPartida("estandar", 1, 1, FasePartida.EN_JUEGO,
                jugadores, barcos, List.of(), List.of());
        return GestorPartida.restaurar(instantanea, Tablero.crearMapaEstandar());
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

    /**
     * Partida para los choques: Ana con el acorazado hundido y los destructores en la esquina de arriba a la
     * derecha (el 4 en (0,9), que ve solo 2 casillas), y el destructor 10 de Beto escondido en (3,9),
     * a 3 casillas: ningún barco de Ana lo ve.
     */
    private static GestorPartida partidaConEnemigoOculto() {
        List<DatosBarco> barcos = new ArrayList<>(List.of(
                new DatosBarco(1, 'A', 1, new Posicion(0, 0), 0, 0, 0, 2, false, false),
                new DatosBarco(2, 'C', 1, new Posicion(0, 1), 5, 0, 0, 3, false, false),
                new DatosBarco(3, 'C', 1, new Posicion(0, 2), 5, 0, 0, 3, false, false),
                new DatosBarco(4, 'D', 1, new Posicion(0, 9), 3, 0, 0, 4, false, false),
                new DatosBarco(5, 'D', 1, new Posicion(0, 8), 3, 0, 0, 4, false, false),
                new DatosBarco(6, 'D', 1, new Posicion(0, 7), 3, 0, 0, 4, false, false)));
        for (DatosBarco barco : flota(2, 7, 8)) {
            barcos.add(barco.id() == 10
                    ? new DatosBarco(10, 'D', 2, new Posicion(3, 9), 3, 0, 0, 4, false, false)
                    : barco);
        }
        return partida(barcos);
    }

    /** Destructor 4 de Ana bajando por la columna 9: el tercer paso cae sobre el enemigo oculto. */
    private static void planificarHaciaElEnemigo(GestorPartida partida) {
        planificar(partida, new Posicion(1, 9), new Posicion(2, 9), new Posicion(3, 9), new Posicion(4, 9));
    }

    private static DatosBarco datosDe(GestorPartida partida, int idBarco) {
        for (DatosBarco barco : partida.crearInstantanea().barcos()) {
            if (barco.id() == idBarco) {
                return barco;
            }
        }
        throw new AssertionError("No existe el barco " + idBarco);
    }

    /** Selecciona el destructor 4 de Ana y le arma una ruta con los pasos indicados. */
    private static void planificar(GestorPartida partida, Posicion... pasos) {
        partida.seleccionarBarco(4);
        for (Posicion paso : pasos) {
            partida.agregarPaso(paso);
        }
    }

    // ---------- movimiento normal ----------

    @Test
    void elBarcoTerminaEnElUltimoPasoDeLaRuta() {
        GestorPartida partida = partida(flotas());
        planificar(partida, new Posicion(2, 3), new Posicion(2, 4));

        partida.confirmarMovimiento();

        assertEquals(new Posicion(2, 4), datosDe(partida, 4).posicion());
    }

    @Test
    void seGastanLosPuntosDeLaRuta() {
        GestorPartida partida = partida(flotas());
        planificar(partida, new Posicion(2, 3), new Posicion(2, 4));

        partida.confirmarMovimiento();

        assertEquals(GestorPartida.PUNTOS_MOVIMIENTO_TURNO - 2, partida.getJugadorActivo().puntosMovimiento());
    }

    @Test
    void seDescuentanLasCasillasDelBarco() {
        GestorPartida partida = partida(flotas());
        planificar(partida, new Posicion(2, 3), new Posicion(2, 4));

        partida.confirmarMovimiento();

        assertEquals(2, datosDe(partida, 4).casillasRestantes());
    }

    @Test
    void laRutaSeVaciaPeroElBarcoSigueSeleccionado() {
        // Sigue seleccionado para poder disparar o usar la habilidad después de moverse.
        GestorPartida partida = partida(flotas());
        planificar(partida, new Posicion(2, 3));

        partida.confirmarMovimiento();

        assertTrue(partida.getRutaPlanificada().isEmpty());
        assertEquals(4, partida.getBarcoSeleccionado().id());
    }

    @Test
    void sePuedeSeguirMoviendoConLasCasillasQueLeQuedan() {
        GestorPartida partida = partida(flotas());
        planificar(partida, new Posicion(2, 3), new Posicion(2, 4));
        partida.confirmarMovimiento();

        partida.agregarPaso(new Posicion(2, 5));
        partida.agregarPaso(new Posicion(2, 6));
        partida.confirmarMovimiento();

        assertEquals(new Posicion(2, 6), datosDe(partida, 4).posicion());
        assertEquals(0, datosDe(partida, 4).casillasRestantes());
    }

    @Test
    void sinRutaNoHayNadaQueConfirmar() {
        GestorPartida partida = partida(flotas());
        partida.seleccionarBarco(4);

        assertThrows(MovimientoInvalidoException.class, partida::confirmarMovimiento);
    }

    @Test
    void sinBarcoSeleccionadoNoHayNadaQueConfirmar() {
        GestorPartida partida = partida(flotas());

        assertThrows(MovimientoInvalidoException.class, partida::confirmarMovimiento);
    }

    @Test
    void despuesDeMoverseYaNoSePuedeGuardarEnEseTurno() {
        GestorPartida partida = partida(flotas());
        planificar(partida, new Posicion(2, 3));
        partida.confirmarMovimiento();
        partida.cancelarSeleccion();

        assertFalse(partida.puedeGuardarse());
    }

    @Test
    void elMovimientoNoLoVeElRival() {
        GestorPartida partida = partida(flotas());
        planificar(partida, new Posicion(2, 3));
        partida.confirmarMovimiento();

        partida.terminarTurno(); // ahora juega Beto

        for (EventoPartida evento : partida.getHistorialVisible()) {
            assertFalse(evento.tipo() == TipoEvento.MOVIMIENTO && evento.numeroJugador() == 1);
        }
    }

    // ---------- choque con un enemigo oculto ----------

    @Test
    void unEnemigoOcultoNoImpidePlanificarLaRuta() {
        GestorPartida partida = partidaConEnemigoOculto();

        planificarHaciaElEnemigo(partida);

        assertEquals(4, partida.getRutaPlanificada().size());
    }

    @Test
    void alChocarConUnEnemigoOcultoFrenaEnElPasoAnterior() {
        GestorPartida partida = partidaConEnemigoOculto();
        planificarHaciaElEnemigo(partida);

        partida.confirmarMovimiento();

        assertEquals(new Posicion(2, 9), datosDe(partida, 4).posicion());
    }

    @Test
    void alChocarSoloSeCobranLosPasosHechos() {
        GestorPartida partida = partidaConEnemigoOculto();
        planificarHaciaElEnemigo(partida);

        partida.confirmarMovimiento();

        assertEquals(GestorPartida.PUNTOS_MOVIMIENTO_TURNO - 2, partida.getJugadorActivo().puntosMovimiento());
        assertEquals(2, datosDe(partida, 4).casillasRestantes());
    }

    @Test
    void elEnemigoConElQueChocaQuedaRevelado() {
        GestorPartida partida = partidaConEnemigoOculto();
        planificarHaciaElEnemigo(partida);

        partida.confirmarMovimiento();

        assertTrue(datosDe(partida, 10).revelado());
    }

    @Test
    void elRevelePorChoqueTerminaCuandoEmpiezaElTurnoDelRival() {
        GestorPartida partida = partidaConEnemigoOculto();
        planificarHaciaElEnemigo(partida);
        partida.confirmarMovimiento();

        partida.terminarTurno();

        assertFalse(datosDe(partida, 10).revelado());
    }
}