package batallanaval.persistencia;

import batallanaval.controlador.EstadoBarco;
import batallanaval.controlador.FasePartida;
import batallanaval.controlador.GestorPartida;
import batallanaval.controlador.InstantaneaPartida;
import batallanaval.controlador.MotivoFin;
import batallanaval.controlador.ResultadoPartida;
import batallanaval.excepciones.PersistenciaException;
import batallanaval.historial.EventoPartida;
import batallanaval.modelo.Casilla;
import batallanaval.modelo.FabricaFlota;
import batallanaval.modelo.Jugador;
import batallanaval.modelo.Posicion;
import batallanaval.modelo.Tablero;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test de integración (QA-01, #110): una partida completa que se guarda en un archivo de verdad
 * a mitad de camino, se carga y se termina de jugar. La partida cargada tiene que seguir exactamente
 * igual que la original: mismo estado al cargar y mismo resultado al final.
 * <p>
 * Despliegue fijo. Ana (J1, ids 1 a 6) en la fila 2: acorazado 1 en (2,4), cruceros 2 y 3 en (2,5) y
 * (2,8), destructores 4, 5 y 6 en (2,0), (2,1) y (2,9). Beto (J2, ids 7 a 12) en la fila 7, mismas
 * columnas. Beto no hace nada en sus turnos.
 */
class PartidaConGuardadoIntegracionTest {

    private static final String NOMBRE = "integracion";

    // ---------- armado ----------

    private static Posicion p(int fila, int columna) {
        return new Posicion(fila, columna);
    }

    private static RepositorioPartidasArchivo repositorioTemporal() throws IOException {
        return new RepositorioPartidasArchivo(Files.createTempDirectory("partidas-integracion"));
    }

    private static GestorPartida partidaDesplegada() {
        GestorPartida partida = new GestorPartida(
                new Jugador(1, "Ana", FabricaFlota.crearFlotaEstandar(1)),
                new Jugador(2, "Beto", FabricaFlota.crearFlotaEstandar(7)),
                Tablero.crearMapaEstandar());
        int[][] ana = {{1, 2, 4}, {2, 2, 5}, {3, 2, 8}, {4, 2, 0}, {5, 2, 1}, {6, 2, 9}};
        int[][] beto = {{7, 7, 4}, {8, 7, 5}, {9, 7, 8}, {10, 7, 0}, {11, 7, 1}, {12, 7, 9}};
        for (int[] barco : ana) {
            partida.ubicarBarco(barco[0], p(barco[1], barco[2]));
        }
        partida.confirmarDespliegue();
        for (int[] barco : beto) {
            partida.ubicarBarco(barco[0], p(barco[1], barco[2]));
        }
        partida.confirmarDespliegue();
        return partida;
    }

    private static void mover(GestorPartida partida, int idBarco, Posicion... pasos) {
        partida.seleccionarBarco(idBarco);
        for (Posicion paso : pasos) {
            partida.agregarPaso(paso);
        }
        partida.confirmarMovimiento();
    }

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

    private static void pasarRonda(GestorPartida partida) {
        partida.terminarTurno();
        partida.terminarTurno();
    }

    /**
     * Primera mitad: en la ronda 1 bajan los destructores 4 y 5, en la 2 baja el 6 y dispara toda la
     * flota (Beto pierde dos destructores). Queda al inicio del turno de Ana en la ronda 3, que es
     * cuando se puede guardar.
     */
    private static GestorPartida jugarPrimeraMitad() {
        GestorPartida partida = partidaDesplegada();
        mover(partida, 4, p(3, 0), p(4, 0), p(5, 0));
        mover(partida, 5, p(3, 1), p(4, 1), p(5, 1));
        pasarRonda(partida);
        mover(partida, 6, p(3, 9), p(4, 9), p(5, 9));
        dispararConTodo(partida);
        pasarRonda(partida);
        return partida;
    }

    /** Segunda mitad: Ana dispara con todo en cada ronda hasta hundir la flota de Beto. */
    private static void jugarHastaElFin(GestorPartida partida) {
        while (!partida.estaTerminada()) {
            dispararConTodo(partida);
            if (!partida.estaTerminada()) {
                pasarRonda(partida);
            }
        }
    }

    private static GestorPartida guardarYCargar(GestorPartida partida) throws IOException, PersistenciaException {
        RepositorioPartidasArchivo repositorio = repositorioTemporal();
        repositorio.guardar(partida, NOMBRE);
        return repositorio.cargar(NOMBRE);
    }

    // ---------- estado al cargar ----------

    @Test
    void laPartidaCargadaQuedaEnElMismoTurno() throws Exception {
        GestorPartida original = jugarPrimeraMitad();

        GestorPartida cargada = guardarYCargar(original);

        assertEquals(FasePartida.EN_JUEGO, cargada.getFase());
        assertEquals(3, cargada.getRonda());
        assertEquals(original.getJugadorActivo(), cargada.getJugadorActivo());
        assertEquals(original.getRival(), cargada.getRival());
    }

    @Test
    void losBarcosSeCarganConPosicionVidaYCooldowns() throws Exception {
        GestorPartida original = jugarPrimeraMitad();

        GestorPartida cargada = guardarYCargar(original);

        InstantaneaPartida antes = original.crearInstantanea();
        InstantaneaPartida despues = cargada.crearInstantanea();
        assertEquals(antes.barcos(), despues.barcos());
        assertEquals(antes.jugadores(), despues.jugadores());
    }

    @Test
    void losRestosDeLosHundidosSiguenEnElTablero() throws Exception {
        GestorPartida original = jugarPrimeraMitad();
        assertEquals(4, original.getRival().barcosVivos()); // Beto perdió los destructores de (7,0) y (7,1)

        GestorPartida cargada = guardarYCargar(original);

        assertEquals(Casilla.RESTOS, cargada.getCasilla(p(7, 0)));
        assertEquals(Casilla.RESTOS, cargada.getCasilla(p(7, 1)));
    }

    @Test
    void elHistorialSeCargaCompletoYEnOrden() throws Exception {
        GestorPartida original = jugarPrimeraMitad();

        GestorPartida cargada = guardarYCargar(original);

        List<EventoPartida> antes = original.crearInstantanea().eventos();
        List<EventoPartida> despues = cargada.crearInstantanea().eventos();
        assertEquals(antes.size(), despues.size());
        for (int i = 0; i < antes.size(); i++) {
            assertEquals(antes.get(i).tipo(), despues.get(i).tipo());
            assertEquals(antes.get(i).descripcion(), despues.get(i).descripcion());
            assertEquals(antes.get(i).numeroJugador(), despues.get(i).numeroJugador());
        }
    }

    @Test
    void laNieblaSeRespetaDespuesDeCargar() throws Exception {
        GestorPartida original = jugarPrimeraMitad();

        GestorPartida cargada = guardarYCargar(original);

        assertEquals(ids(original.getBarcosEnemigosVisibles()), ids(cargada.getBarcosEnemigosVisibles()));
    }

    // ---------- seguir jugando ----------

    @Test
    void laPartidaCargadaTerminaIgualQueLaOriginal() throws Exception {
        GestorPartida original = jugarPrimeraMitad();
        GestorPartida cargada = guardarYCargar(original);

        jugarHastaElFin(original);
        jugarHastaElFin(cargada);

        ResultadoPartida esperado = original.getResultado();
        ResultadoPartida obtenido = cargada.getResultado();
        assertEquals("Ana", obtenido.ganador());
        assertEquals(MotivoFin.HUNDIMIENTO, obtenido.motivo());
        assertEquals(esperado.ganador(), obtenido.ganador());
        assertEquals(esperado.rondas(), obtenido.rondas());
    }

    @Test
    void sePuedeGuardarYCargarVariasVecesEnLaMismaPartida() throws Exception {
        RepositorioPartidasArchivo repositorio = repositorioTemporal();
        GestorPartida partida = jugarPrimeraMitad();

        repositorio.guardar(partida, NOMBRE);
        partida = repositorio.cargar(NOMBRE);
        dispararConTodo(partida);
        pasarRonda(partida);
        repositorio.guardar(partida, NOMBRE); // pisa el guardado anterior
        partida = repositorio.cargar(NOMBRE);

        assertEquals(4, partida.getRonda());
        jugarHastaElFin(partida);
        assertTrue(partida.estaTerminada());
        assertEquals(MotivoFin.HUNDIMIENTO, partida.getResultado().motivo());
    }

    // ---------- cuándo no se puede guardar ----------

    @Test
    void aMitadDeTurnoNoSeGuarda() throws Exception {
        RepositorioPartidasArchivo repositorio = repositorioTemporal();
        GestorPartida partida = partidaDesplegada();
        mover(partida, 4, p(3, 0));

        assertThrows(PersistenciaException.class, () -> repositorio.guardar(partida, NOMBRE));
        assertFalse(repositorio.existe(NOMBRE));
    }

    @Test
    void unaPartidaTerminadaNoSeGuarda() throws Exception {
        RepositorioPartidasArchivo repositorio = repositorioTemporal();
        GestorPartida partida = jugarPrimeraMitad();
        jugarHastaElFin(partida);

        assertThrows(PersistenciaException.class, () -> repositorio.guardar(partida, NOMBRE));
    }

    private static List<Integer> ids(List<EstadoBarco> barcos) {
        return barcos.stream().map(EstadoBarco::id).toList();
    }
}