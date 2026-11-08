package batallanaval.persistencia;

import batallanaval.controlador.MotivoFin;
import batallanaval.controlador.ResultadoPartida;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RepositorioEstadisticasCsvTest {

    @Test
    void archivoInexistenteRetornaListaVacia(@TempDir Path tempDir) throws PersistenciaException {
        Path archivo = tempDir.resolve("inexistente.csv");
        RepositorioEstadisticasCsv repo = new RepositorioEstadisticasCsv(archivo);

        List<ResultadoPartida> resultados = repo.obtenerTodos();

        assertTrue(resultados.isEmpty());
    }

    @Test
    void registrarPrimeraVezCreaArchivoConEncabezadoYDatos(@TempDir Path tempDir) throws PersistenciaException, IOException {
        Path archivo = tempDir.resolve("estadisticas.csv");
        RepositorioEstadisticasCsv repo = new RepositorioEstadisticasCsv(archivo);

        LocalDateTime fecha = LocalDateTime.parse("2026-10-05T14:32:10");
        ResultadoPartida res = new ResultadoPartida(fecha, "Bota", "Santi", "Bota", 12, MotivoFin.HUNDIMIENTO);

        repo.registrar(res);

        assertTrue(Files.exists(archivo));
        List<String> lineas = Files.readAllLines(archivo);
        assertEquals(2, lineas.size());
        assertEquals("fecha;jugador1;jugador2;ganador;rondas;motivo", lineas.get(0));
        assertEquals("2026-10-05T14:32:10;Bota;Santi;Bota;12;HUNDIMIENTO", lineas.get(1));

        List<ResultadoPartida> recuperados = repo.obtenerTodos();
        assertEquals(1, recuperados.size());
        ResultadoPartida obtenido = recuperados.get(0);
        assertEquals(fecha, obtenido.fecha());
        assertEquals("Bota", obtenido.jugador1());
        assertEquals("Santi", obtenido.jugador2());
        assertEquals("Bota", obtenido.ganador());
        assertEquals(12, obtenido.rondas());
        assertEquals(MotivoFin.HUNDIMIENTO, obtenido.motivo());
        assertFalse(obtenido.esEmpate());
    }

    @Test
    void registrarSegundoResultadoHaceAppendSinDuplicarEncabezado(@TempDir Path tempDir) throws PersistenciaException, IOException {
        Path archivo = tempDir.resolve("estadisticas.csv");
        RepositorioEstadisticasCsv repo = new RepositorioEstadisticasCsv(archivo);

        ResultadoPartida r1 = new ResultadoPartida(
                LocalDateTime.parse("2026-10-05T10:00:15"), "J1", "J2", "J1", 8, MotivoFin.DOMINACION
        );
        ResultadoPartida r2 = new ResultadoPartida(
                LocalDateTime.parse("2026-10-05T11:30:25"), "J1", "J2", "J2", 15, MotivoFin.HUNDIMIENTO
        );

        repo.registrar(r1);
        repo.registrar(r2);

        List<String> lineas = Files.readAllLines(archivo);
        assertEquals(3, lineas.size());
        assertEquals("fecha;jugador1;jugador2;ganador;rondas;motivo", lineas.get(0));
        assertEquals("2026-10-05T10:00:15;J1;J2;J1;8;DOMINACION", lineas.get(1));
        assertEquals("2026-10-05T11:30:25;J1;J2;J2;15;HUNDIMIENTO", lineas.get(2));

        List<ResultadoPartida> recuperados = repo.obtenerTodos();
        assertEquals(2, recuperados.size());
        assertEquals("J1", recuperados.get(0).ganador());
        assertEquals(MotivoFin.DOMINACION, recuperados.get(0).motivo());
        assertEquals("J2", recuperados.get(1).ganador());
        assertEquals(MotivoFin.HUNDIMIENTO, recuperados.get(1).motivo());
    }

    @Test
    void registrarEmpateGuardaGanadorVacioYRecuperaNull(@TempDir Path tempDir) throws PersistenciaException, IOException {
        Path archivo = tempDir.resolve("estadisticas.csv");
        RepositorioEstadisticasCsv repo = new RepositorioEstadisticasCsv(archivo);

        ResultadoPartida empate = new ResultadoPartida(
                LocalDateTime.parse("2026-10-05T12:00:40"), "Bota", "Santi", null, 40, MotivoFin.EMPATE
        );

        repo.registrar(empate);

        List<String> lineas = Files.readAllLines(archivo);
        assertEquals(2, lineas.size());
        assertEquals("2026-10-05T12:00:40;Bota;Santi;;40;EMPATE", lineas.get(1));

        List<ResultadoPartida> recuperados = repo.obtenerTodos();
        assertEquals(1, recuperados.size());
        ResultadoPartida obtenido = recuperados.get(0);
        assertNull(obtenido.ganador());
        assertTrue(obtenido.esEmpate());
        assertEquals(MotivoFin.EMPATE, obtenido.motivo());
        assertEquals(40, obtenido.rondas());
    }

    @Test
    void obtenerTodosSalteaLineasRotasYConservaLineasValidas(@TempDir Path tempDir) throws IOException, PersistenciaException {
        Path archivo = tempDir.resolve("estadisticas.csv");

        List<String> contenido = List.of(
                "fecha;jugador1;jugador2;ganador;rondas;motivo",
                "2026-10-05T10:00:00;Jugador1;Jugador2;Jugador1;10;HUNDIMIENTO",
                "linea_con_pocos_campos;campo2",
                "2026-10-05T10:00:00;J1;J2;J1;10;HUNDIMIENTO;demasiados;campos",
                "FECHA_INVALIDA;J1;J2;J1;10;HUNDIMIENTO",
                "2026-10-05T10:00:00;;J2;J1;10;HUNDIMIENTO",
                "2026-10-05T10:00:00;J1;;J1;10;HUNDIMIENTO",
                "2026-10-05T10:00:00;J1;J2;J1;no_es_numero;HUNDIMIENTO",
                "2026-10-05T10:00:00;J1;J2;J1;-5;HUNDIMIENTO",
                "2026-10-05T10:00:00;J1;J2;J1;10;MOTIVO_INEXISTENTE",
                "",
                "   ",
                "2026-10-06T15:30:00;Alfa;Beta;;20;LIMITE_TURNOS"
        );
        Files.write(archivo, contenido);

        RepositorioEstadisticasCsv repo = new RepositorioEstadisticasCsv(archivo);
        List<ResultadoPartida> resultados = repo.obtenerTodos();

        assertEquals(2, resultados.size());

        ResultadoPartida r1 = resultados.get(0);
        assertEquals("Jugador1", r1.jugador1());
        assertEquals("Jugador2", r1.jugador2());
        assertEquals("Jugador1", r1.ganador());
        assertEquals(10, r1.rondas());
        assertEquals(MotivoFin.HUNDIMIENTO, r1.motivo());

        ResultadoPartida r2 = resultados.get(1);
        assertEquals("Alfa", r2.jugador1());
        assertEquals("Beta", r2.jugador2());
        assertNull(r2.ganador());
        assertTrue(r2.esEmpate());
        assertEquals(20, r2.rondas());
        assertEquals(MotivoFin.LIMITE_TURNOS, r2.motivo());
    }

    @Test
    void registrarLanzaPersistenciaExceptionSiResultadoEsNulo(@TempDir Path tempDir) {
        RepositorioEstadisticasCsv repo = new RepositorioEstadisticasCsv(tempDir.resolve("estadisticas.csv"));
        assertThrows(PersistenciaException.class, () -> repo.registrar(null));
    }

    @Test
    void constructorLanzaNullPointerExceptionSiArchivoEsNulo() {
        assertThrows(NullPointerException.class, () -> new RepositorioEstadisticasCsv(null));
    }

    @Test
    void constructorPorDefectoInstanciaCorrectamente() {
        RepositorioEstadisticasCsv repo = new RepositorioEstadisticasCsv();
        assertTrue(repo instanceof RepositorioEstadisticas);
    }

    @Test
    void obtenerTodosDevuelveListaInmutable(@TempDir Path tempDir) throws PersistenciaException {
        Path archivo = tempDir.resolve("estadisticas.csv");
        RepositorioEstadisticasCsv repo = new RepositorioEstadisticasCsv(archivo);
        repo.registrar(new ResultadoPartida(LocalDateTime.now(), "A", "B", "A", 5, MotivoFin.ABANDONO));

        List<ResultadoPartida> resultados = repo.obtenerTodos();
        assertThrows(UnsupportedOperationException.class, () -> resultados.add(
                new ResultadoPartida(LocalDateTime.now(), "C", "D", "C", 2, MotivoFin.HUNDIMIENTO)
        ));
    }

    @Test
    void registrarCreaDirectorioPadreSiNoExiste(@TempDir Path tempDir) throws PersistenciaException {
        Path archivoSubdir = tempDir.resolve("subcarpeta").resolve("anidada").resolve("estadisticas.csv");
        RepositorioEstadisticasCsv repo = new RepositorioEstadisticasCsv(archivoSubdir);

        repo.registrar(new ResultadoPartida(LocalDateTime.now(), "A", "B", "A", 7, MotivoFin.DOMINACION));

        assertTrue(Files.exists(archivoSubdir));
        assertEquals(1, repo.obtenerTodos().size());
    }
}
