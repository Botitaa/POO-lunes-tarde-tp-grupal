package batallanaval.persistencia;

import batallanaval.controlador.GestorPartida;
import batallanaval.controlador.MotivoFin;
import batallanaval.controlador.ResultadoPartida;
import batallanaval.excepciones.PersistenciaException;
import batallanaval.modelo.Casilla;
import batallanaval.modelo.Posicion;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica las partidas guardadas que usa la prueba manual guiada ({@code docs/prueba-manual.md}).
 * Si alguien cambia el formato del guardado o una regla y estas partidas dejan de servir, este test
 * falla y avisa que hay que regenerarlas, en vez de que se descubra en medio de la prueba manual.
 * <p>
 * Solo lee los archivos: nunca los modifica.
 */
class PartidasDePruebaManualTest {

    private final RepositorioPartidasArchivo repositorio = new RepositorioPartidasArchivo(carpetaPartidas());

    @Test
    void hundimientoSeGanaConDosDisparos() throws Exception {
        GestorPartida partida = repositorio.cargar("prueba-hundimiento");
        assertEquals("Ana", partida.getJugadorActivo().nombre());

        partida.seleccionarBarco(1); // acorazado en (2,4)
        partida.disparar(10);        // destructor enemigo en (6,4), vida 2
        assertEquals(Casilla.RESTOS, partida.getCasilla(new Posicion(6, 4)));
        assertFalse(partida.estaTerminada(), "todavía queda un barco enemigo");

        partida.seleccionarBarco(4); // destructor en (5,5)
        partida.disparar(11);        // destructor enemigo en (7,5), vida 2

        assertResultado(partida.getResultado(), "Ana", 8, MotivoFin.HUNDIMIENTO);
    }

    @Test
    void dominacionSeGanaAlTerminarElTurno() throws Exception {
        GestorPartida partida = repositorio.cargar("prueba-dominacion");
        assertEquals(5, partida.getJugadorActivo().puntosDominacion());

        partida.terminarTurno();

        assertResultado(partida.getResultado(), "Ana", 10, MotivoFin.DOMINACION);
    }

    @Test
    void limiteDeTurnosLoGanaQuienTieneMasDominacion() throws Exception {
        GestorPartida partida = repositorio.cargar("prueba-limite");
        assertEquals("Beto", partida.getJugadorActivo().nombre());

        partida.terminarTurno();

        assertResultado(partida.getResultado(), "Ana", 40, MotivoFin.LIMITE_TURNOS);
    }

    @Test
    void limiteDeTurnosConTodoIgualadoEsEmpate() throws Exception {
        GestorPartida partida = repositorio.cargar("prueba-limite-empate");

        partida.terminarTurno();

        assertTrue(partida.getResultado().esEmpate());
        assertNull(partida.getResultado().ganador());
        assertEquals(MotivoFin.EMPATE, partida.getResultado().motivo());
    }

    @Test
    void lasPartidasRotasDanUnMensajeParaElJugador() {
        PersistenciaException rota = assertThrows(PersistenciaException.class,
                () -> repositorio.cargar("prueba-rota"));
        assertEquals("Línea 6 del guardado inválida: formato numérico incorrecto", rota.getMessage());

        PersistenciaException imposible = assertThrows(PersistenciaException.class,
                () -> repositorio.cargar("prueba-imposible"));
        assertTrue(imposible.getMessage().contains("dos barcos vivos en la misma posicion: (2,4)"),
                imposible.getMessage());

        PersistenciaException version = assertThrows(PersistenciaException.class,
                () -> repositorio.cargar("prueba-otra-version"));
        assertTrue(version.getMessage().contains("versión no soportada"), version.getMessage());
    }

    @Test
    void laDeOtraVersionNoApareceEnLaLista() throws Exception {
        List<String> nombres = repositorio.listar().stream().map(CabeceraGuardado::nombre).toList();

        assertFalse(nombres.contains("prueba-otra-version"));
        assertTrue(nombres.containsAll(List.of("prueba-hundimiento", "prueba-dominacion", "prueba-limite",
                "prueba-limite-empate", "prueba-rota", "prueba-imposible")), nombres.toString());
    }

    // Ayudas

    /**
     * Carpeta de las partidas de prueba. Maven corre los tests desde {@code tp-batalla-naval/}; si
     * alguien los corre desde la raíz del repo, se busca un nivel más adentro.
     */
    private static Path carpetaPartidas() {
        Path desdeElModulo = Path.of("docs", "prueba-manual", "partidas");
        if (Files.isDirectory(desdeElModulo)) {
            return desdeElModulo;
        }
        return Path.of("tp-batalla-naval").resolve(desdeElModulo);
    }

    private static void assertResultado(ResultadoPartida resultado, String ganador, int rondas, MotivoFin motivo) {
        assertEquals(ganador, resultado.ganador());
        assertEquals(rondas, resultado.rondas());
        assertEquals(motivo, resultado.motivo());
    }
}
