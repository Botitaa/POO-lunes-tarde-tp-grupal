package batallanaval.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import batallanaval.controlador.MotivoFin;
import batallanaval.controlador.ResultadoPartida;
import batallanaval.excepciones.PersistenciaException;
import batallanaval.historial.EventoPartida;
import batallanaval.historial.HistorialPartida;
import batallanaval.historial.TipoEvento;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExportadorHistorialTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 10, 5, 14, 32);

    private ResultadoPartida resultadoGanaBota() {
        return new ResultadoPartida(FECHA, "Bota", "Santi", "Bota", 12, MotivoFin.DOMINACION);
    }

    /** Historial con tres eventos, uno privado de cada jugador y uno público. */
    private HistorialPartida historialDePrueba() {
        HistorialPartida historial = new HistorialPartida();
        historial.registrar(new EventoPartida(1, 1, TipoEvento.MOVIMIENTO, "Crucero a (3,4)", FECHA));
        historial.registrar(new EventoPartida(1, 2, TipoEvento.HABILIDAD, "Acorazado rango 8", FECHA));
        historial.registrar(new EventoPartida(2, 2, TipoEvento.DISPARO, "Acorazado (7,4) -> Crucero (3,4), -4", FECHA));
        return historial;
    }

    // ---------- archivo ----------

    @Test
    void creaElArchivoConElNombreFechaYJugadores(@TempDir Path temp) throws PersistenciaException {
        Path archivo = ExportadorHistorial.exportar(historialDePrueba(), resultadoGanaBota(), temp);

        assertTrue(Files.exists(archivo));
        assertEquals("2026-10-05_1432_Bota-vs-Santi.txt", archivo.getFileName().toString());
        assertEquals(temp, archivo.getParent());
    }

    @Test
    void creaLaCarpetaSiNoExiste(@TempDir Path temp) throws PersistenciaException {
        Path carpeta = temp.resolve("datos").resolve("historiales");

        Path archivo = ExportadorHistorial.exportar(historialDePrueba(), resultadoGanaBota(), carpeta);

        assertTrue(Files.exists(archivo));
    }

    @Test
    void dosExportacionesIgualesNoSePisan(@TempDir Path temp) throws PersistenciaException {
        Path primero = ExportadorHistorial.exportar(historialDePrueba(), resultadoGanaBota(), temp);
        Path segundo = ExportadorHistorial.exportar(historialDePrueba(), resultadoGanaBota(), temp);

        assertFalse(primero.equals(segundo));
        assertEquals("2026-10-05_1432_Bota-vs-Santi_2.txt", segundo.getFileName().toString());
    }

    @Test
    void losCaracteresRarosDelNombreSeReemplazan(@TempDir Path temp) throws PersistenciaException {
        ResultadoPartida resultado = new ResultadoPartida(FECHA, "Jugador 1", "a/b:c", null, 40, MotivoFin.EMPATE);

        Path archivo = ExportadorHistorial.exportar(new HistorialPartida(), resultado, temp);

        assertEquals("2026-10-05_1432_Jugador_1-vs-a_b_c.txt", archivo.getFileName().toString());
    }

    // ---------- contenido ----------

    @Test
    void elEncabezadoTieneJugadoresResultadoYRondas(@TempDir Path temp) throws PersistenciaException, IOException {
        Path archivo = ExportadorHistorial.exportar(historialDePrueba(), resultadoGanaBota(), temp);
        List<String> lineas = Files.readAllLines(archivo);

        assertEquals("BATALLA NAVAL TACTICA - HISTORIAL DE PARTIDA", lineas.get(0));
        assertEquals("Fecha: 05/10/2026 14:32", lineas.get(1));
        assertEquals("Jugadores: Bota vs Santi", lineas.get(2));
        assertEquals("Resultado: Gana: Bota (DOMINACION)", lineas.get(3));
        assertEquals("Rondas: 12", lineas.get(4));
        assertEquals("Cantidad de eventos: 3", lineas.get(5));
        assertEquals("", lineas.get(6));
    }

    @Test
    void escribeUnaLineaPorEventoEnOrdenYSinNiebla(@TempDir Path temp) throws PersistenciaException, IOException {
        HistorialPartida historial = historialDePrueba();
        Path archivo = ExportadorHistorial.exportar(historial, resultadoGanaBota(), temp);
        List<String> lineas = Files.readAllLines(archivo);
        List<EventoPartida> eventos = historial.getEventos();

        assertEquals(7 + eventos.size(), lineas.size()); // 7 de encabezado + 1 por evento
        for (int i = 0; i < eventos.size(); i++) {
            assertEquals(eventos.get(i).aLinea(), lineas.get(7 + i));
        }
    }

    @Test
    void unEmpateSeEscribeComoEmpate(@TempDir Path temp) throws PersistenciaException, IOException {
        ResultadoPartida empate = new ResultadoPartida(FECHA, "Bota", "Santi", null, 40, MotivoFin.LIMITE_TURNOS);

        Path archivo = ExportadorHistorial.exportar(historialDePrueba(), empate, temp);

        assertEquals("Resultado: Empate (LIMITE_TURNOS)", Files.readAllLines(archivo).get(3));
    }

    @Test
    void unHistorialVacioSoloTieneElEncabezado(@TempDir Path temp) throws PersistenciaException, IOException {
        Path archivo = ExportadorHistorial.exportar(new HistorialPartida(), resultadoGanaBota(), temp);
        List<String> lineas = Files.readAllLines(archivo);

        assertEquals(7, lineas.size());
        assertEquals("Cantidad de eventos: 0", lineas.get(5));
    }

    // ---------- errores ----------

    @Test
    void siLaCarpetaEsUnArchivoLanzaPersistenciaException(@TempDir Path temp) throws IOException {
        Path noEsCarpeta = Files.createFile(temp.resolve("soy-un-archivo.txt"));
        try {
            ExportadorHistorial.exportar(historialDePrueba(), resultadoGanaBota(), noEsCarpeta);
            fail("Debería haber lanzado PersistenciaException");
        } catch (PersistenciaException e) {
            assertTrue(e.getCause() instanceof IOException);
        }
    }

    @Test
    void parametrosNullLanzanExcepcion(@TempDir Path temp) throws PersistenciaException {
        try {
            ExportadorHistorial.exportar(null, resultadoGanaBota(), temp);
            fail("Debería haber lanzado IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // esperado
        }
    }
}