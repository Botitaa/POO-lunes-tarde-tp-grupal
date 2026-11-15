package batallanaval.persistencia;

import batallanaval.controlador.GestorPartida;
import batallanaval.excepciones.PersistenciaException;
import batallanaval.modelo.FabricaFlota;
import batallanaval.modelo.Jugador;
import batallanaval.modelo.Posicion;
import batallanaval.modelo.Tablero;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests de persistencia con archivos rotos a propósito (#83): el error tiene que decir en qué línea
 * está el problema, para que el jugador (o quien lo revise) lo pueda encontrar. Parte de un guardado
 * real y le rompe una línea del medio.
 */
class RepositorioPartidasArchivoCorruptosTest {

    @TempDir
    Path carpeta;

    @Test
    void unArchivoVacioIndicaLaLinea1() throws Exception {
        RepositorioPartidasArchivo repo = new RepositorioPartidasArchivo(carpeta);
        Files.writeString(carpeta.resolve("vacia.sav"), "");

        PersistenciaException e = assertThrows(PersistenciaException.class, () -> repo.cargar("vacia"));

        assertTrue(e.getMessage().contains("Línea 1"), e.getMessage());
    }

    @Test
    void unBarcoRotoEnElMedioIndicaSuNumeroDeLinea() throws Exception {
        int linea = romperPrimeraLineaQueEmpiezaCon("BARCO;", "BARCO;1;D");

        PersistenciaException e = assertThrows(PersistenciaException.class,
                () -> new RepositorioPartidasArchivo(carpeta).cargar("partida"));

        assertTrue(e.getMessage().contains("Línea " + linea), e.getMessage());
    }

    @Test
    void unNumeroInvalidoEnUnJugadorIndicaSuNumeroDeLinea() throws Exception {
        int linea = romperPrimeraLineaQueEmpiezaCon("JUGADOR;", "JUGADOR;1;Ana;seis;0");

        PersistenciaException e = assertThrows(PersistenciaException.class,
                () -> new RepositorioPartidasArchivo(carpeta).cargar("partida"));

        assertTrue(e.getMessage().contains("Línea " + linea), e.getMessage());
    }

    @Test
    void unEventoConTipoDesconocidoIndicaSuNumeroDeLinea() throws Exception {
        int linea = romperPrimeraLineaQueEmpiezaCon("EVENTO;", "EVENTO;1;1;EXPLOSION;2026-11-15T10:00;algo");

        PersistenciaException e = assertThrows(PersistenciaException.class,
                () -> new RepositorioPartidasArchivo(carpeta).cargar("partida"));

        assertTrue(e.getMessage().contains("Línea " + linea), e.getMessage());
    }

    @Test
    void unRegistroDesconocidoIndicaSuNumeroDeLinea() throws Exception {
        int linea = romperPrimeraLineaQueEmpiezaCon("PARTIDA;", "TESORO;3;3");

        PersistenciaException e = assertThrows(PersistenciaException.class,
                () -> new RepositorioPartidasArchivo(carpeta).cargar("partida"));

        assertTrue(e.getMessage().contains("Línea " + linea), e.getMessage());
        assertTrue(e.getMessage().contains("desconocido"), e.getMessage());
    }

    @Test
    void unArchivoRotoNoApareceEnLaListaPeroLosSanosSi() throws Exception {
        RepositorioPartidasArchivo repo = new RepositorioPartidasArchivo(carpeta);
        repo.guardar(partidaEnJuego().crearInstantanea(), "sana");
        Files.writeString(carpeta.resolve("rota.sav"), "esto no es un guardado\n");

        List<CabeceraGuardado> lista = repo.listar();

        assertEquals(1, lista.size());
        assertEquals("sana", lista.get(0).nombre());
    }

    @Test
    void guardarDosVecesConElMismoNombreDejaLaUltimaVersion() throws Exception {
        RepositorioPartidasArchivo repo = new RepositorioPartidasArchivo(carpeta);
        GestorPartida partida = partidaEnJuego();
        repo.guardar(partida.crearInstantanea(), "partida");
        partida.terminarTurno();

        repo.guardar(partida.crearInstantanea(), "partida");

        assertEquals(2, repo.cargar("partida").getJugadorActivo().numero());
        assertEquals(1, repo.listar().size(), "no se duplica en la lista");
    }

    // ---------- auxiliares ----------

    /**
     * Guarda una partida real como {@code partida.sav} y reemplaza la primera línea que empieza
     * con {@code prefijo} por {@code reemplazo}.
     *
     * @return el número (desde 1) de la línea que se rompió
     */
    private int romperPrimeraLineaQueEmpiezaCon(String prefijo, String reemplazo) throws Exception {
        new RepositorioPartidasArchivo(carpeta).guardar(partidaEnJuego().crearInstantanea(), "partida");
        Path archivo = carpeta.resolve("partida.sav");
        List<String> lineas = new ArrayList<>(Files.readAllLines(archivo));
        for (int i = 0; i < lineas.size(); i++) {
            if (lineas.get(i).startsWith(prefijo)) {
                lineas.set(i, reemplazo);
                Files.write(archivo, lineas);
                return i + 1;
            }
        }
        throw new AssertionError("El guardado no tiene ninguna línea " + prefijo);
    }

    /** Despliegue fijo de las dos flotas: la partida queda EN_JUEGO, al inicio del turno del jugador 1. */
    private static GestorPartida partidaEnJuego() {
        GestorPartida partida = new GestorPartida(
                new Jugador(1, "Ana", FabricaFlota.crearFlotaEstandar(1)),
                new Jugador(2, "Beto", FabricaFlota.crearFlotaEstandar(7)),
                Tablero.crearMapaEstandar());
        for (int id = 1; id <= 6; id++) {
            partida.ubicarBarco(id, new Posicion(0, id - 1));
        }
        partida.confirmarDespliegue();
        for (int id = 7; id <= 12; id++) {
            partida.ubicarBarco(id, new Posicion(9, id - 7));
        }
        partida.confirmarDespliegue();
        return partida;
    }
}