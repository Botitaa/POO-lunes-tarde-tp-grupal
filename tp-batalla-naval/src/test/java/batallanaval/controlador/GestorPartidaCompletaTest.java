package batallanaval.controlador;

import batallanaval.excepciones.AccionInvalidaException;
import batallanaval.excepciones.DespliegueInvalidoException;
import batallanaval.excepciones.DisparoInvalidoException;
import batallanaval.excepciones.HabilidadNoDisponibleException;
import batallanaval.excepciones.MovimientoInvalidoException;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Partidas completas simuladas por código (PAR-16, #61). A diferencia de los otros tests de
 * GestorPartida, acá no se usa {@code restaurar}: la partida arranca con el constructor, se despliega
 * con {@code ubicarBarco} y se juega solo con los métodos públicos, como lo haría la vista.
 * <p>
 * Despliegue fijo. Ana (J1, ids 1 a 6) en la fila 2: acorazado 1 en (2,4), cruceros 2 y 3 en (2,5) y
 * (2,8), destructores 4, 5 y 6 en (2,0), (2,1) y (2,9). Beto (J2, ids 7 a 12) en la fila 7: acorazado 7
 * en (7,4), cruceros 8 y 9 en (7,5) y (7,8), destructores 10, 11 y 12 en (7,0), (7,1) y (7,9).
 * Beto no hace nada en sus turnos salvo que el test diga lo contrario.
 */
class GestorPartidaCompletaTest {

    // ---------- armado ----------

    private static Posicion p(int fila, int columna) {
        return new Posicion(fila, columna);
    }

    private static GestorPartida partidaNueva() {
        return new GestorPartida(
                new Jugador(1, "Ana", FabricaFlota.crearFlotaEstandar(1)),
                new Jugador(2, "Beto", FabricaFlota.crearFlotaEstandar(7)),
                Tablero.crearMapaEstandar());
    }

    private static void desplegarAna(GestorPartida partida) {
        partida.ubicarBarco(1, p(2, 4));
        partida.ubicarBarco(2, p(2, 5));
        partida.ubicarBarco(3, p(2, 8));
        partida.ubicarBarco(4, p(2, 0));
        partida.ubicarBarco(5, p(2, 1));
        partida.ubicarBarco(6, p(2, 9));
        partida.confirmarDespliegue();
    }

    private static void desplegarBeto(GestorPartida partida) {
        partida.ubicarBarco(7, p(7, 4));
        partida.ubicarBarco(8, p(7, 5));
        partida.ubicarBarco(9, p(7, 8));
        partida.ubicarBarco(10, p(7, 0));
        partida.ubicarBarco(11, p(7, 1));
        partida.ubicarBarco(12, p(7, 9));
        partida.confirmarDespliegue();
    }

    /** Partida recién desplegada: empieza el turno de Ana en la ronda 1. */
    private static GestorPartida partidaDesplegada() {
        GestorPartida partida = partidaNueva();
        desplegarAna(partida);
        desplegarBeto(partida);
        return partida;
    }

    private static void mover(GestorPartida partida, int idBarco, Posicion... pasos) {
        partida.seleccionarBarco(idBarco);
        for (Posicion paso : pasos) {
            partida.agregarPaso(paso);
        }
        partida.confirmarMovimiento();
    }

    /** Cada barco vivo del jugador activo le tira al primer objetivo válido que tenga. */
    private static void dispararConTodo(GestorPartida partida) {
        for (EstadoBarco propio : partida.getBarcosPropios()) {
            partida.seleccionarBarco(propio.id());
            List<EstadoBarco> objetivos = partida.getObjetivosValidos();
            if (!objetivos.isEmpty()) {
                partida.disparar(objetivos.get(0).id());
                if (partida.estaTerminada()) {
                    return;
                }
            }
        }
        partida.cancelarSeleccion();
    }

    /** Termina el turno de Ana y el de Beto (que no hace nada): vuelve a jugar Ana. */
    private static void pasarRonda(GestorPartida partida) {
        partida.terminarTurno();
        partida.terminarTurno();
    }

    private static void jugarHastaElFin(GestorPartida partida) {
        while (!partida.estaTerminada()) {
            partida.terminarTurno();
        }
    }

    /** Observador que cuenta los avisos que le llegan. */
    private static final class ObservadorContador implements ObservadorPartida {
        private final List<ResultadoPartida> resultados = new ArrayList<>();
        private final List<String> turnos = new ArrayList<>();

        @Override
        public void onEstadoCambiado() {
        }

        @Override
        public void onFaseCambiada(FasePartida fase) {
        }

        @Override
        public void onTurnoIniciado(String nombreJugador) {
            turnos.add(nombreJugador);
        }

        @Override
        public void onPartidaTerminada(ResultadoPartida resultado) {
            resultados.add(resultado);
        }
    }

    // ---------- despliegue ----------

    @Test
    void conElDespliegueFijoArrancaElCombate() {
        GestorPartida partida = partidaDesplegada();

        assertEquals(FasePartida.EN_JUEGO, partida.getFase());
        assertEquals(1, partida.getRonda());
        assertEquals("Ana", partida.getJugadorActivo().nombre());
        assertEquals(GestorPartida.PUNTOS_MOVIMIENTO_TURNO, partida.getJugadorActivo().puntosMovimiento());
        assertEquals(6, partida.getBarcosPropios().size());
    }

    @Test
    void desdeElDespliegueElAcorazadoVeLoQueTieneEnFrente() {
        // El acorazado de Ana en (2,4), con visión 5, ve al acorazado (7,4) y al crucero (7,5) de Beto.
        GestorPartida partida = partidaDesplegada();

        assertEquals(List.of(7, 8), partida.getBarcosEnemigosVisibles().stream().map(EstadoBarco::id).toList());
    }

    // ---------- victoria por hundimiento ----------

    /**
     * Ronda 1: los destructores 4 y 5 bajan por las columnas 0 y 1 hasta la fila 5 (6 puntos).
     * Ronda 2: el destructor 6 baja por la columna 9 y todos disparan. De la ronda 2 en adelante
     * Ana dispara con todo lo que tiene; Beto no se mueve.
     */
    private static void jugarHastaHundirTodo(GestorPartida partida) {
        mover(partida, 4, p(3, 0), p(4, 0), p(5, 0));
        mover(partida, 5, p(3, 1), p(4, 1), p(5, 1));
        pasarRonda(partida);
        mover(partida, 6, p(3, 9), p(4, 9), p(5, 9));
        while (!partida.estaTerminada()) {
            dispararConTodo(partida);
            if (!partida.estaTerminada()) {
                pasarRonda(partida);
            }
        }
    }

    @Test
    void hundirLosSeisBarcosGanaLaPartida() {
        GestorPartida partida = partidaDesplegada();

        jugarHastaHundirTodo(partida);

        assertEquals(FasePartida.TERMINADA, partida.getFase());
        assertEquals("Ana", partida.getResultado().ganador());
        assertEquals(MotivoFin.HUNDIMIENTO, partida.getResultado().motivo());
        assertEquals(5, partida.getResultado().rondas());
        assertEquals(0, partida.getRival().barcosVivos());
    }

    @Test
    void cadaHundimientoQuedaEnElHistorial() {
        GestorPartida partida = partidaDesplegada();

        jugarHastaHundirTodo(partida);

        long hundimientos = partida.getHistorialVisible().stream()
                .filter(e -> e.tipo() == TipoEvento.HUNDIMIENTO).count();
        assertEquals(6L, hundimientos);
    }

    @Test
    void alHundirTodoSeAvisaUnaSolaVezElResultado() {
        GestorPartida partida = partidaDesplegada();
        ObservadorContador observador = new ObservadorContador();
        partida.agregarObservador(observador);

        jugarHastaHundirTodo(partida);

        assertEquals(1, observador.resultados.size());
        assertEquals(MotivoFin.HUNDIMIENTO, observador.resultados.get(0).motivo());
    }

    @Test
    void lasRondasAlternanEntreLosDosJugadores() {
        GestorPartida partida = partidaDesplegada();
        ObservadorContador observador = new ObservadorContador();
        partida.agregarObservador(observador);

        pasarRonda(partida);
        pasarRonda(partida);

        assertEquals(List.of("Beto", "Ana", "Beto", "Ana"), observador.turnos);
        assertEquals(3, partida.getRonda());
    }

    // ---------- victoria por dominación ----------

    /** El crucero 2 de Ana baja de (2,5) a la zona de dominación, en (4,5). Cuesta 4 puntos. */
    private static void cruceroAnaALaZona(GestorPartida partida) {
        mover(partida, 2, p(3, 5), p(4, 5));
    }

    @Test
    void seisTurnosSolaEnLaZonaGanaPorDominacion() {
        GestorPartida partida = partidaDesplegada();
        cruceroAnaALaZona(partida);

        jugarHastaElFin(partida);

        assertEquals("Ana", partida.getResultado().ganador());
        assertEquals(MotivoFin.DOMINACION, partida.getResultado().motivo());
        assertEquals(6, partida.getResultado().rondas());
        assertEquals(0, partida.getRival().puntosDominacion());
    }

    @Test
    void enZonaDisputadaNadieSumaYLosPuntosNoSePierden() {
        GestorPartida partida = partidaDesplegada();
        cruceroAnaALaZona(partida);
        partida.terminarTurno(); // Ana sola en la zona: 1 punto

        mover(partida, 7, p(6, 4), p(5, 4)); // el acorazado de Beto entra a la zona
        partida.terminarTurno(); // zona disputada: Beto no suma
        partida.terminarTurno(); // zona disputada: Ana no suma, pero conserva el que tenía

        assertEquals("Beto", partida.getJugadorActivo().nombre());
        assertEquals(0, partida.getJugadorActivo().puntosDominacion());
        assertEquals(1, partida.getRival().puntosDominacion());
    }

    // ---------- límite de turnos ----------

    @Test
    void sinVictoriaAntesLaPartidaTerminaAlFinDeLaRonda40() {
        GestorPartida partida = partidaDesplegada();

        jugarHastaElFin(partida);

        assertEquals(GestorPartida.TURNOS_MAX, partida.getResultado().rondas());
    }

    @Test
    void enElLimiteGanaElQueTieneMasDominacion() {
        GestorPartida partida = partidaDesplegada();
        cruceroAnaALaZona(partida);
        pasarRonda(partida); // Ana suma 1 punto
        mover(partida, 2, p(3, 5)); // y se va de la zona

        jugarHastaElFin(partida);

        assertEquals("Ana", partida.getResultado().ganador());
        assertEquals(MotivoFin.LIMITE_TURNOS, partida.getResultado().motivo());
    }

    @Test
    void siEnElLimiteEstanIgualesEsEmpate() {
        GestorPartida partida = partidaDesplegada();

        jugarHastaElFin(partida);

        assertNull(partida.getResultado().ganador());
        assertEquals(MotivoFin.EMPATE, partida.getResultado().motivo());
    }

    // ---------- una excepción por cada acción inválida ----------

    @Test
    void ubicarFueraDeLaZonaDeDespliegueTiraDespliegueInvalido() {
        GestorPartida partida = partidaNueva();

        assertThrows(DespliegueInvalidoException.class, () -> partida.ubicarBarco(1, p(5, 0)));
    }

    @Test
    void confirmarConBarcosSinUbicarTiraDespliegueInvalido() {
        GestorPartida partida = partidaNueva();
        partida.ubicarBarco(1, p(2, 4));

        assertThrows(DespliegueInvalidoException.class, partida::confirmarDespliegue);
    }

    @Test
    void jugarDuranteElDespliegueTiraAccionInvalida() {
        GestorPartida partida = partidaNueva();

        assertThrows(AccionInvalidaException.class, () -> partida.seleccionarBarco(1));
        assertThrows(AccionInvalidaException.class, partida::terminarTurno);
    }

    @Test
    void desplegarDuranteElCombateTiraAccionInvalida() {
        GestorPartida partida = partidaDesplegada();

        assertThrows(AccionInvalidaException.class, () -> partida.ubicarBarco(1, p(1, 4)));
    }

    @Test
    void moverAUnaIslaTiraMovimientoInvalido() {
        GestorPartida partida = partidaDesplegada();
        partida.seleccionarBarco(5); // destructor en (2,1); (3,2) es isla

        assertThrows(MovimientoInvalidoException.class, () -> partida.agregarPaso(p(3, 2)));
    }

    @Test
    void dispararSinObjetivoAlAlcanceTiraDisparoInvalido() {
        GestorPartida partida = partidaDesplegada();
        partida.seleccionarBarco(4); // destructor en (2,0): rango 2, el acorazado de Beto está a 5

        assertThrows(DisparoInvalidoException.class, () -> partida.disparar(7));
    }

    @Test
    void usarLaHabilidadEnEsperaTiraHabilidadNoDisponible() {
        GestorPartida partida = partidaDesplegada();
        partida.seleccionarBarco(4);
        partida.usarHabilidad();
        pasarRonda(partida);
        partida.seleccionarBarco(4);

        assertThrows(HabilidadNoDisponibleException.class, partida::usarHabilidad);
    }

    @Test
    void conLaPartidaTerminadaNingunaAccionSePuedeHacer() {
        GestorPartida partida = partidaDesplegada();
        jugarHastaElFin(partida);

        assertTrue(partida.estaTerminada());
        assertThrows(AccionInvalidaException.class, () -> partida.seleccionarBarco(1));
        assertThrows(AccionInvalidaException.class, partida::usarHabilidad);
        assertThrows(AccionInvalidaException.class, partida::terminarTurno);
        assertFalse(partida.puedeGuardarse());
    }
}