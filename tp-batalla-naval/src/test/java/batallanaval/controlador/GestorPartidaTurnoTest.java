package batallanaval.controlador;

import batallanaval.excepciones.AccionInvalidaException;
import batallanaval.historial.EventoPartida;
import batallanaval.historial.TipoEvento;
import batallanaval.modelo.FabricaFlota;
import batallanaval.modelo.Jugador;
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
 * Tests de terminarTurno en GestorPartida (PAR-06, #51): cambio de jugador, ronda, inicio del turno
 * (puntos, barcos, revelado), resumen y dominación.
 * <p>
 * Las partidas se arman con {@link GestorPartida#restaurar} para poder ubicar barcos donde haga falta
 * (por ejemplo en la zona de dominación, filas 4-5 y columnas 4-5) sin depender del movimiento.
 * Por defecto la flota de Ana (J1, ids 1 a 6) está en la fila 1 y la de Beto (J2, ids 7 a 12) en la fila 8.
 */
class GestorPartidaTurnoTest {

    private static final Posicion ZONA_A = new Posicion(4, 4);
    private static final Posicion ZONA_B = new Posicion(5, 5);

    // ---------- armado de partidas ----------

    /** Arma una partida EN_JUEGO a partir de los datos que le pasa cada test. */
    private static GestorPartida partida(int jugadorActivo, int ronda, List<DatosBarco> barcos,
                                         int puntosMovimientoJ2, List<String> resumenJ1) {
        return partida(jugadorActivo, ronda, barcos, puntosMovimientoJ2, resumenJ1, 0, 0);
    }

    /** Igual que la anterior, pero con los puntos de dominación de cada uno. */
    private static GestorPartida partida(int jugadorActivo, int ronda, List<DatosBarco> barcos,
                                         int puntosMovimientoJ2, List<String> resumenJ1,
                                         int dominacionJ1, int dominacionJ2) {
        List<DatosJugador> jugadores = List.of(
                new DatosJugador(1, "Ana", GestorPartida.PUNTOS_MOVIMIENTO_TURNO, dominacionJ1, resumenJ1),
                new DatosJugador(2, "Beto", puntosMovimientoJ2, dominacionJ2, List.of()));
        InstantaneaPartida instantanea = new InstantaneaPartida("estandar", jugadorActivo, ronda,
                FasePartida.EN_JUEGO, jugadores, barcos, List.of(), List.of());
        return GestorPartida.restaurar(instantanea, Tablero.crearMapaEstandar());
    }

    /** Partida de la ronda 1, turno de Ana, con las dos flotas en su fila de siempre. */
    private static GestorPartida partidaNormal() {
        return partida(1, 1, flotas(null, null), GestorPartida.PUNTOS_MOVIMIENTO_TURNO, List.of());
    }

    /**
     * Las dos flotas completas. Si se pasa una posición, el destructor 4 (de Ana) o el 10 (de Beto)
     * se ubica ahí en vez de en su fila.
     */
    private static List<DatosBarco> flotas(Posicion destructorAna, Posicion destructorBeto) {
        List<DatosBarco> barcos = new ArrayList<>();
        barcos.addAll(flota(1, 1, 1, destructorAna));
        barcos.addAll(flota(2, 7, 8, destructorBeto));
        return barcos;
    }

    /** 1 acorazado, 2 cruceros y 3 destructores sanos, en la fila indicada. */
    private static List<DatosBarco> flota(int jugador, int primerId, int fila, Posicion primerDestructor) {
        Posicion posDestructor = primerDestructor != null ? primerDestructor : new Posicion(fila, 3);
        return List.of(
                new DatosBarco(primerId, 'A', jugador, new Posicion(fila, 0), 8, 0, 0, 2, false, false),
                new DatosBarco(primerId + 1, 'C', jugador, new Posicion(fila, 1), 5, 0, 0, 3, false, false),
                new DatosBarco(primerId + 2, 'C', jugador, new Posicion(fila, 2), 5, 0, 0, 3, false, false),
                new DatosBarco(primerId + 3, 'D', jugador, posDestructor, 3, 0, 0, 4, false, false),
                new DatosBarco(primerId + 4, 'D', jugador, new Posicion(fila, 4), 3, 0, 0, 4, false, false),
                new DatosBarco(primerId + 5, 'D', jugador, new Posicion(fila, 5), 3, 0, 0, 4, false, false));
    }

    /** Devuelve los mismos barcos pero con el barco {@code id} reemplazado. */
    private static List<DatosBarco> reemplazar(List<DatosBarco> barcos, DatosBarco nuevo) {
        List<DatosBarco> resultado = new ArrayList<>();
        for (DatosBarco barco : barcos) {
            resultado.add(barco.id() == nuevo.id() ? nuevo : barco);
        }
        return resultado;
    }

    private static DatosBarco datosDe(GestorPartida partida, int idBarco) {
        for (DatosBarco barco : partida.crearInstantanea().barcos()) {
            if (barco.id() == idBarco) {
                return barco;
            }
        }
        throw new AssertionError("No existe el barco " + idBarco);
    }

    private static int dominacionDe(GestorPartida partida, int numeroJugador) {
        EstadoJugador activo = partida.getJugadorActivo();
        return activo.numero() == numeroJugador ? activo.puntosDominacion() : partida.getRival().puntosDominacion();
    }

    // ---------- cambio de jugador y ronda ----------

    @Test
    void cuandoTerminaJ1LeTocaAJ2YLaRondaNoCambia() {
        GestorPartida partida = partidaNormal();

        partida.terminarTurno();

        assertEquals("Beto", partida.getJugadorActivo().nombre());
        assertEquals(1, partida.getRonda());
    }

    @Test
    void cuandoTerminaJ2VuelveJ1YEmpiezaLaRondaSiguiente() {
        GestorPartida partida = partidaNormal();

        partida.terminarTurno();
        partida.terminarTurno();

        assertEquals("Ana", partida.getJugadorActivo().nombre());
        assertEquals(2, partida.getRonda());
    }

    @Test
    void noSePuedeTerminarTurnoDuranteElDespliegue() {
        GestorPartida enDespliegue = new GestorPartida(
                new Jugador(1, "Ana", FabricaFlota.crearFlotaEstandar(1)),
                new Jugador(2, "Beto", FabricaFlota.crearFlotaEstandar(7)),
                Tablero.crearMapaEstandar());

        assertThrows(AccionInvalidaException.class, enDespliegue::terminarTurno);
    }


    // ---------- inicio del turno del que arranca ----------

    @Test
    void elQueArrancaRecibeSusPuntosDeMovimiento() {
        GestorPartida partida = partida(1, 1, flotas(null, null), 0, List.of());

        partida.terminarTurno();

        assertEquals(GestorPartida.PUNTOS_MOVIMIENTO_TURNO, partida.getJugadorActivo().puntosMovimiento());
    }

    @Test
    void alQueArrancaLeBajaElCooldownDeDisparo() {
        // El acorazado de Beto (id 7) arranca con 1 turno de espera.
        List<DatosBarco> barcos = reemplazar(flotas(null, null),
                new DatosBarco(7, 'A', 2, new Posicion(8, 0), 8, 1, 0, 2, false, false));
        GestorPartida partida = partida(1, 1, barcos, GestorPartida.PUNTOS_MOVIMIENTO_TURNO, List.of());

        partida.terminarTurno();

        assertEquals(0, datosDe(partida, 7).cooldownDisparo());
    }

    @Test
    void alQueArrancaSeLeApagaElReveladoDeSuDisparoAnterior() {
        // Beto disparó con el crucero 8 en su turno: quedó revelado durante el turno de Ana.
        List<DatosBarco> barcos = reemplazar(flotas(null, null),
                new DatosBarco(8, 'C', 2, new Posicion(8, 1), 5, 0, 0, 3, false, true));
        GestorPartida partida = partida(1, 1, barcos, GestorPartida.PUNTOS_MOVIMIENTO_TURNO, List.of());

        partida.terminarTurno();

        assertFalse(datosDe(partida, 8).revelado());
    }

    @Test
    void elResumenDelQueTerminaSeLimpia() {
        GestorPartida partida = partida(1, 1, flotas(null, null), GestorPartida.PUNTOS_MOVIMIENTO_TURNO,
                List.of("Tu Crucero (1,1) recibió 2 de daño desde (5,1)"));

        partida.terminarTurno();

        DatosJugador ana = partida.crearInstantanea().jugadores().get(0);
        assertTrue(ana.resumenPendiente().isEmpty());
    }

    // ---------- avisos e historial ----------

    @Test
    void avisaQueEmpiezaElTurnoDelOtro() {
        GestorPartida partida = partidaNormal();
        List<String> turnos = new ArrayList<>();
        partida.agregarObservador(new ObservadorPartida() {
            @Override public void onEstadoCambiado() { }
            @Override public void onFaseCambiada(FasePartida fase) { }
            @Override public void onTurnoIniciado(String nombreJugador) { turnos.add(nombreJugador); }
            @Override public void onPartidaTerminada(ResultadoPartida resultado) { }
        });

        partida.terminarTurno();

        assertEquals(List.of("Beto"), turnos);
    }

    @Test
    void elFinDeTurnoQuedaANombreDelQueTermino() {
        GestorPartida partida = partidaNormal();

        partida.terminarTurno();

        EventoPartida finTurno = null;
        for (EventoPartida evento : partida.crearInstantanea().eventos()) {
            if (evento.tipo() == TipoEvento.FIN_TURNO) {
                finTurno = evento;
            }
        }
        assertEquals(1, finTurno.numeroJugador());
    }

    // ---------- dominación ----------

    @Test
    void soloEnLaZonaSumaUnPuntoAlTerminarSuTurno() {
        GestorPartida partida = partida(1, 1, flotas(ZONA_A, null), GestorPartida.PUNTOS_MOVIMIENTO_TURNO, List.of());

        partida.terminarTurno();

        assertEquals(1, dominacionDe(partida, 1));
    }

    @Test
    void noSumaDeNuevoCuandoTerminaElTurnoDelRival() {
        GestorPartida partida = partida(1, 1, flotas(ZONA_A, null), GestorPartida.PUNTOS_MOVIMIENTO_TURNO, List.of());

        partida.terminarTurno(); // termina Ana: suma 1
        partida.terminarTurno(); // termina Beto: Ana no vuelve a sumar

        assertEquals(1, dominacionDe(partida, 1));
    }

    @Test
    void siLosDosEstanEnLaZonaNadieSuma() {
        GestorPartida partida = partida(1, 1, flotas(ZONA_A, ZONA_B), GestorPartida.PUNTOS_MOVIMIENTO_TURNO, List.of());

        partida.terminarTurno();
        partida.terminarTurno();

        assertEquals(0, dominacionDe(partida, 1));
        assertEquals(0, dominacionDe(partida, 2));
    }

    @Test
    void unBarcoHundidoEnLaZonaNoCuenta() {
        List<DatosBarco> barcos = reemplazar(flotas(null, null),
                new DatosBarco(4, 'D', 1, ZONA_A, 0, 0, 0, 4, false, false));
        GestorPartida partida = partida(1, 1, barcos, GestorPartida.PUNTOS_MOVIMIENTO_TURNO, List.of());

        partida.terminarTurno();

        assertEquals(0, dominacionDe(partida, 1));
    }

    @Test
    void elPuntoDeDominacionQuedaEnElHistorial() {
        GestorPartida partida = partida(1, 1, flotas(ZONA_A, null), GestorPartida.PUNTOS_MOVIMIENTO_TURNO, List.of());

        partida.terminarTurno();

        assertTrue(partida.crearInstantanea().eventos().stream()
                .anyMatch(e -> e.tipo() == TipoEvento.DOMINACION && e.numeroJugador() == 1));
    }

    // ---------- fin de partida ----------

    /** Partida en la última ronda, turno de Beto, con la dominación y los barcos que pide cada test. */
    private static GestorPartida ultimaRonda(List<DatosBarco> barcos, int dominacionJ1, int dominacionJ2) {
        return partida(2, GestorPartida.TURNOS_MAX, barcos, GestorPartida.PUNTOS_MOVIMIENTO_TURNO, List.of(),
                dominacionJ1, dominacionJ2);
    }

    @Test
    void alTerminarLaUltimaRondaLaPartidaTermina() {
        GestorPartida partida = ultimaRonda(flotas(null, null), 0, 0);

        partida.terminarTurno();

        assertTrue(partida.estaTerminada());
        assertEquals(GestorPartida.TURNOS_MAX, partida.getRonda());
    }

    @Test
    void enElLimiteGanaElDeMasDominacion() {
        // Ana tiene más dominación aunque Beto tenga más barcos vivos: decide la dominación.
        List<DatosBarco> barcos = reemplazar(flotas(null, null),
                new DatosBarco(4, 'D', 1, new Posicion(1, 3), 0, 0, 0, 4, false, false));
        GestorPartida partida = ultimaRonda(barcos, 3, 1);

        partida.terminarTurno();

        assertEquals("Ana", partida.getResultado().ganador());
        assertEquals(MotivoFin.LIMITE_TURNOS, partida.getResultado().motivo());
    }

    @Test
    void conDominacionEmpatadaGanaElDeMasBarcosVivos() {
        // Ana perdió un destructor (hundido): Beto tiene más barcos vivos.
        List<DatosBarco> barcos = reemplazar(flotas(null, null),
                new DatosBarco(4, 'D', 1, new Posicion(1, 3), 0, 0, 0, 4, false, false));
        GestorPartida partida = ultimaRonda(barcos, 2, 2);

        partida.terminarTurno();

        assertEquals("Beto", partida.getResultado().ganador());
    }

    @Test
    void conBarcosEmpatadosGanaElDeMasVidaTotal() {
        // Mismos barcos vivos, pero el crucero 2 de Ana está dañado.
        List<DatosBarco> barcos = reemplazar(flotas(null, null),
                new DatosBarco(2, 'C', 1, new Posicion(1, 1), 2, 0, 0, 3, false, false));
        GestorPartida partida = ultimaRonda(barcos, 0, 0);

        partida.terminarTurno();

        assertEquals("Beto", partida.getResultado().ganador());
    }

    @Test
    void siEmpatanEnTodoEsEmpate() {
        GestorPartida partida = ultimaRonda(flotas(null, null), 1, 1);

        partida.terminarTurno();

        assertTrue(partida.getResultado().esEmpate());
        assertEquals(MotivoFin.EMPATE, partida.getResultado().motivo());
    }

    @Test
    void alLlegarALosPuntosDeDominacionGanaEnElActo() {
        GestorPartida partida = partida(1, 5, flotas(ZONA_A, null), GestorPartida.PUNTOS_MOVIMIENTO_TURNO, List.of(),
                GestorPartida.PUNTOS_DOMINACION_VICTORIA - 1, 0);

        partida.terminarTurno();

        assertTrue(partida.estaTerminada());
        assertEquals("Ana", partida.getResultado().ganador());
        assertEquals(MotivoFin.DOMINACION, partida.getResultado().motivo());
    }

    @Test
    void alTerminarAvisaLaFaseYElResultado() {
        GestorPartida partida = ultimaRonda(flotas(null, null), 3, 1);
        List<FasePartida> fases = new ArrayList<>();
        List<ResultadoPartida> resultados = new ArrayList<>();
        partida.agregarObservador(new ObservadorPartida() {
            @Override public void onEstadoCambiado() { }
            @Override public void onFaseCambiada(FasePartida fase) { fases.add(fase); }
            @Override public void onTurnoIniciado(String nombreJugador) { }
            @Override public void onPartidaTerminada(ResultadoPartida resultado) { resultados.add(resultado); }
        });

        partida.terminarTurno();

        assertEquals(List.of(FasePartida.TERMINADA), fases);
        assertEquals(1, resultados.size());
    }

    @Test
    void conLaPartidaTerminadaNoSePuedeSeguirJugando() {
        GestorPartida partida = ultimaRonda(flotas(null, null), 0, 0);
        partida.terminarTurno();

        assertThrows(AccionInvalidaException.class, partida::terminarTurno);
    }
}