package batallanaval.controlador;

import batallanaval.excepciones.AccionInvalidaException;
import batallanaval.excepciones.HabilidadNoDisponibleException;
import batallanaval.historial.TipoEvento;
import batallanaval.modelo.Posicion;
import batallanaval.modelo.Tablero;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests de habilidades en GestorPartida (PAR-11, #56): destructor daño doble, crucero se repara 2,
 * acorazado rango 8; una vez, espera de 3 turnos, dura hasta el fin del turno, no cuesta puntos.
 * <p>
 * Siempre juega Ana (J1). Flota base de Ana en la fila 1: acorazado 1 en (1,0), cruceros 2 y 3 en (1,1)
 * y (1,2), destructores 4, 5 y 6 en (1,3), (1,4) y (1,5). La de Beto (ids 7 a 12) en la fila 9.
 */
class GestorPartidaHabilidadTest {

    // ---------- armado ----------

    private static GestorPartida partida(List<DatosBarco> barcos) {
        List<DatosJugador> jugadores = List.of(
                new DatosJugador(1, "Ana", GestorPartida.PUNTOS_MOVIMIENTO_TURNO, 0, List.of()),
                new DatosJugador(2, "Beto", GestorPartida.PUNTOS_MOVIMIENTO_TURNO, 0, List.of()));
        InstantaneaPartida instantanea = new InstantaneaPartida("estandar", 1, 1, FasePartida.EN_JUEGO,
                jugadores, barcos, List.of(), List.of());
        return GestorPartida.restaurar(instantanea, Tablero.crearMapaEstandar());
    }

    private static DatosBarco barco(int id, char tipo, int jugador, int fila, int columna, int vida) {
        int casillas = tipo == 'A' ? 2 : tipo == 'C' ? 3 : 4;
        return new DatosBarco(id, tipo, jugador, new Posicion(fila, columna), vida, 0, 0, casillas, false, false);
    }

    private static DatosBarco sano(int id, char tipo, int jugador, int fila, int columna) {
        int vida = tipo == 'A' ? 8 : tipo == 'C' ? 5 : 3;
        return barco(id, tipo, jugador, fila, columna, vida);
    }

    private static List<DatosBarco> ana() {
        return List.of(
                sano(1, 'A', 1, 1, 0), sano(2, 'C', 1, 1, 1), sano(3, 'C', 1, 1, 2),
                sano(4, 'D', 1, 1, 3), sano(5, 'D', 1, 1, 4), sano(6, 'D', 1, 1, 5));
    }

    private static List<DatosBarco> beto() {
        return List.of(
                sano(7, 'A', 2, 9, 0), sano(8, 'C', 2, 9, 1), sano(9, 'C', 2, 9, 2),
                sano(10, 'D', 2, 9, 3), sano(11, 'D', 2, 9, 4), sano(12, 'D', 2, 9, 5));
    }

    /** Las dos flotas base, con los barcos que pase el test reemplazados (por id). */
    private static List<DatosBarco> con(DatosBarco... cambios) {
        List<DatosBarco> barcos = new ArrayList<>(ana());
        barcos.addAll(beto());
        for (DatosBarco cambio : cambios) {
            barcos.replaceAll(b -> b.id() == cambio.id() ? cambio : b);
        }
        return barcos;
    }

    private static DatosBarco datosDe(GestorPartida partida, int idBarco) {
        for (DatosBarco barco : partida.crearInstantanea().barcos()) {
            if (barco.id() == idBarco) {
                return barco;
            }
        }
        throw new AssertionError("No existe el barco " + idBarco);
    }

    private static boolean esObjetivo(GestorPartida partida, int idBarco) {
        return partida.getObjetivosValidos().stream().anyMatch(b -> b.id() == idBarco);
    }

    /** Termina el turno de Ana y el de Beto: vuelve a jugar Ana. */
    private static void pasarRonda(GestorPartida partida) {
        partida.terminarTurno();
        partida.terminarTurno();
    }

    // ---------- reglas generales ----------

    @Test
    void sinBarcoSeleccionadoNoSePuedeUsar() {
        GestorPartida partida = partida(con());

        assertThrows(HabilidadNoDisponibleException.class, partida::usarHabilidad);
    }

    @Test
    void laHabilidadQuedaActivaYEnEspera() {
        GestorPartida partida = partida(con());
        partida.seleccionarBarco(4);

        partida.usarHabilidad();

        assertTrue(datosDe(partida, 4).habilidadActiva());
        assertEquals(3, datosDe(partida, 4).cooldownHabilidad());
    }

    @Test
    void seUsaUnaSolaVezPorTurno() {
        GestorPartida partida = partida(con());
        partida.seleccionarBarco(4);
        partida.usarHabilidad();

        assertThrows(HabilidadNoDisponibleException.class, partida::usarHabilidad);
    }

    @Test
    void otroBarcoPuedeUsarLaSuyaEnElMismoTurno() {
        GestorPartida partida = partida(con());
        partida.seleccionarBarco(4);
        partida.usarHabilidad();

        partida.seleccionarBarco(5);

        assertDoesNotThrow(partida::usarHabilidad);
    }

    @Test
    void duraHastaElFinDelTurno() {
        GestorPartida partida = partida(con());
        partida.seleccionarBarco(4);
        partida.usarHabilidad();

        pasarRonda(partida);

        assertFalse(datosDe(partida, 4).habilidadActiva());
        assertEquals(2, datosDe(partida, 4).cooldownHabilidad());
    }

    @Test
    void enEsperaNoSePuedeUsar() {
        GestorPartida partida = partida(con());
        partida.seleccionarBarco(4);
        partida.usarHabilidad();
        pasarRonda(partida);
        partida.seleccionarBarco(4);

        assertThrows(HabilidadNoDisponibleException.class, partida::usarHabilidad);
    }

    @Test
    void despuesDeTresTurnosSePuedeVolverAUsar() {
        GestorPartida partida = partida(con());
        partida.seleccionarBarco(4);
        partida.usarHabilidad();
        pasarRonda(partida);
        pasarRonda(partida);
        pasarRonda(partida);
        partida.seleccionarBarco(4);

        assertDoesNotThrow(partida::usarHabilidad);
    }

    @Test
    void noCuestaPuntosNiImpideMoverse() {
        GestorPartida partida = partida(con());
        partida.seleccionarBarco(4);
        partida.usarHabilidad();

        assertEquals(GestorPartida.PUNTOS_MOVIMIENTO_TURNO, partida.getJugadorActivo().puntosMovimiento());
        assertDoesNotThrow(() -> partida.agregarPaso(new Posicion(2, 3)));
    }

    @Test
    void noRevelaAlBarco() {
        GestorPartida partida = partida(con());
        partida.seleccionarBarco(4);
        partida.usarHabilidad();

        partida.terminarTurno(); // juega Beto

        assertFalse(datosDe(partida, 4).revelado());
    }

    @Test
    void despuesDeUsarlaYaNoSePuedeGuardarEnEseTurno() {
        GestorPartida partida = partida(con());
        partida.seleccionarBarco(4);
        partida.usarHabilidad();
        partida.cancelarSeleccion();

        assertFalse(partida.puedeGuardarse());
    }

    @Test
    void fueraDelCombateNoSePuedeUsar() {
        List<DatosJugador> jugadores = List.of(
                new DatosJugador(1, "Ana", GestorPartida.PUNTOS_MOVIMIENTO_TURNO, 0, List.of()),
                new DatosJugador(2, "Beto", GestorPartida.PUNTOS_MOVIMIENTO_TURNO, 0, List.of()));
        GestorPartida partida = GestorPartida.restaurar(new InstantaneaPartida("estandar", 1, 1,
                FasePartida.TERMINADA, jugadores, con(), List.of(), List.of()), Tablero.crearMapaEstandar());

        assertThrows(AccionInvalidaException.class, partida::usarHabilidad);
    }

    // ---------- historial ----------

    @Test
    void elUsoQuedaEnElHistorialDelQueLaUsa() {
        GestorPartida partida = partida(con());
        partida.seleccionarBarco(4);
        partida.usarHabilidad();

        assertTrue(partida.getHistorialVisible().stream().anyMatch(e -> e.tipo() == TipoEvento.HABILIDAD));
    }

    @Test
    void elRivalNoSeEnteraDeLaHabilidad() {
        GestorPartida partida = partida(con());
        partida.seleccionarBarco(4);
        partida.usarHabilidad();

        partida.terminarTurno(); // juega Beto

        assertTrue(partida.getHistorialVisible().stream().noneMatch(e -> e.tipo() == TipoEvento.HABILIDAD));
    }

    // ---------- efectos ----------

    @Test
    void elDestructorHaceDanioDoble() {
        // Acorazado 7 de Beto en (3,4), con 8 de vida: el destructor 4 de Ana lo ve y le llega.
        GestorPartida partida = partida(con(sano(7, 'A', 2, 3, 4)));
        partida.seleccionarBarco(4);
        partida.usarHabilidad();

        partida.disparar(7);

        assertEquals(8 - 6, datosDe(partida, 7).vida());
    }

    @Test
    void elCruceroSeReparaDos() {
        GestorPartida partida = partida(con(barco(2, 'C', 1, 1, 1, 2)));
        partida.seleccionarBarco(2);

        partida.usarHabilidad();

        assertEquals(4, datosDe(partida, 2).vida());
    }

    @Test
    void laReparacionNoPasaDeLaVidaMaxima() {
        GestorPartida partida = partida(con(barco(2, 'C', 1, 1, 1, 4)));
        partida.seleccionarBarco(2);

        partida.usarHabilidad();

        assertEquals(5, datosDe(partida, 2).vida());
    }

    /** Crucero 8 de Beto en (8,0), a 7 del acorazado de Ana; lo ve el destructor 4 de Ana, puesto en (6,0). */
    private static GestorPartida partidaConObjetivoLejano() {
        return partida(con(sano(4, 'D', 1, 6, 0), sano(8, 'C', 2, 8, 0)));
    }

    @Test
    void sinHabilidadElAcorazadoNoLlegaASiete() {
        GestorPartida partida = partidaConObjetivoLejano();
        partida.seleccionarBarco(1);

        assertTrue(partida.getObjetivosValidos().stream().noneMatch(b -> b.id() == 8));
    }

    @Test
    void conHabilidadElAcorazadoTiraARangoOcho() {
        GestorPartida partida = partidaConObjetivoLejano();
        partida.seleccionarBarco(1);
        partida.usarHabilidad();

        partida.disparar(8);

        assertEquals(5 - 4, datosDe(partida, 8).vida());
    }

    @Test
    void elRangoDelAcorazadoVuelveASeisEnElTurnoSiguiente() {
        GestorPartida partida = partidaConObjetivoLejano();
        partida.seleccionarBarco(1);
        partida.usarHabilidad();
        assertEquals(8, partida.getBarcoSeleccionado().rangoDisparo());

        pasarRonda(partida);
        partida.seleccionarBarco(1);

        assertEquals(6, partida.getBarcoSeleccionado().rangoDisparo());
    }
}