package batallanaval.persistencia;

import batallanaval.controlador.FasePartida;
import batallanaval.excepciones.PersistenciaException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RepositorioPartidasArchivoListarBorrarTest {

    @Test
    void listarDevuelvePartidasOrdenadasPorMasNuevoPrimeroYSalteaRotas(@TempDir Path tempDir) throws IOException, PersistenciaException {
        RepositorioPartidasArchivo repo = new RepositorioPartidasArchivo(tempDir);

        Path partidaAntigua = tempDir.resolve("antigua.sav");
        Files.write(partidaAntigua, List.of(
                "BATALLA-NAVAL;1",
                "CABECERA;antigua;2026-10-01T10:00:00;J1;J2;5;EN_JUEGO"
        ));

        Path partidaNueva = tempDir.resolve("nueva.sav");
        Files.write(partidaNueva, List.of(
                "BATALLA-NAVAL;1",
                "CABECERA;nueva;2026-10-05T12:00:00;J1;J2;10;EN_JUEGO"
        ));

        Path partidaRotaCorta = tempDir.resolve("rota_corta.sav");
        Files.write(partidaRotaCorta, List.of("BATALLA-NAVAL;1"));

        Path partidaRotaVersion = tempDir.resolve("rota_version.sav");
        Files.write(partidaRotaVersion, List.of(
                "BATALLA-NAVAL;99",
                "CABECERA;rota_version;2026-10-06T12:00:00;J1;J2;10;EN_JUEGO"
        ));

        Path partidaRotaCabecera = tempDir.resolve("rota_cabecera.sav");
        Files.write(partidaRotaCabecera, List.of(
                "BATALLA-NAVAL;1",
                "CABECERA;incompleta"
        ));

        List<CabeceraGuardado> listado = repo.listar();

        assertEquals(2, listado.size());
        assertEquals("nueva", listado.get(0).nombre());
        assertEquals(LocalDateTime.parse("2026-10-05T12:00:00"), listado.get(0).fecha());
        assertEquals("antigua", listado.get(1).nombre());
        assertEquals(LocalDateTime.parse("2026-10-01T10:00:00"), listado.get(1).fecha());
        assertEquals(FasePartida.EN_JUEGO, listado.get(0).fase());
    }

    @Test
    void borrarEliminaArchivoExistente(@TempDir Path tempDir) throws IOException, PersistenciaException {
        RepositorioPartidasArchivo repo = new RepositorioPartidasArchivo(tempDir);
        Path archivo = tempDir.resolve("partida1.sav");
        Files.write(archivo, List.of("BATALLA-NAVAL;1", "CABECERA;partida1;2026-10-01T10:00:00;J1;J2;5;EN_JUEGO"));

        assertTrue(repo.existe("partida1"));
        repo.borrar("partida1");
        assertFalse(repo.existe("partida1"));
        assertFalse(Files.exists(archivo));
    }

    @Test
    void borrarLanzaPersistenciaExceptionSiArchivoNoExiste(@TempDir Path tempDir) {
        RepositorioPartidasArchivo repo = new RepositorioPartidasArchivo(tempDir);
        assertThrows(PersistenciaException.class, () -> repo.borrar("no_existe"));
    }

    @Test
    void borrarLanzaPersistenciaExceptionSiNombreInvalido(@TempDir Path tempDir) {
        RepositorioPartidasArchivo repo = new RepositorioPartidasArchivo(tempDir);
        assertThrows(PersistenciaException.class, () -> repo.borrar(null));
        assertThrows(PersistenciaException.class, () -> repo.borrar(""));
        assertThrows(PersistenciaException.class, () -> repo.borrar("partida con espacio"));
    }

    @Test
    void existeDevuelveFalseParaNombreInvalidoONoExistente(@TempDir Path tempDir) {
        RepositorioPartidasArchivo repo = new RepositorioPartidasArchivo(tempDir);
        assertFalse(repo.existe(null));
        assertFalse(repo.existe(""));
        assertFalse(repo.existe("no_existe"));
    }
}
