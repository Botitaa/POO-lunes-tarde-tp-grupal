package batallanaval.app;

import batallanaval.controlador.MotivoFin;
import batallanaval.controlador.ResultadoPartida;
import batallanaval.excepciones.PersistenciaException;
import batallanaval.historial.EventoPartida;
import batallanaval.historial.TipoEvento;
import batallanaval.persistencia.RepositorioEstadisticas;
import batallanaval.persistencia.RepositorioPartidasArchivo;
import batallanaval.vista.Pantalla;
import batallanaval.vista.VentanaPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Tests del cierre de la partida en el controlador de la aplicación: exportar el historial,
 * registrar las estadísticas y pasar a la pantalla de fin. El resto del flujo entre pantallas
 * se prueba a mano (ver {@code docs/prueba-manual.md}).
 */
class ControladorAplicacionTest {

    private static final ResultadoPartida RESULTADO = new ResultadoPartida(
            LocalDateTime.of(2026, 11, 14, 20, 0), "Ana", "Beto", "Ana", 5, MotivoFin.HUNDIMIENTO);

    @TempDir
    Path carpeta;

    // ---------- exportar el historial ----------

    @Test
    void exportarHistorialEscribeUnaLineaPorEvento() throws Exception {
        List<EventoPartida> eventos = List.of(
                new EventoPartida(1, 1, TipoEvento.DESPLIEGUE, "Despliegue confirmado", RESULTADO.fecha()),
                new EventoPartida(5, 1, TipoEvento.HUNDIMIENTO, "Destructor (4,4) hundido", RESULTADO.fecha()));

        Path archivo = ControladorAplicacion.exportarHistorial(eventos, RESULTADO, carpeta);

        assertNotNull(archivo);
        List<String> lineas = Files.readAllLines(archivo);
        assertTrue(lineas.contains("[R1] J1 DESPLIEGUE: Despliegue confirmado"));
        assertTrue(lineas.contains("[R5] J1 HUNDIMIENTO: Destructor (4,4) hundido"));
    }

    @Test
    void siNoSePuedeExportarDevuelveNullSinCortarElJuego() throws Exception {
        // Un archivo donde tendría que ir la carpeta: no se pueden crear directorios adentro
        Path noEsCarpeta = Files.createFile(carpeta.resolve("ocupado"));

        assertNull(ControladorAplicacion.exportarHistorial(List.of(), RESULTADO, noEsCarpeta));
    }

    // ---------- fin de la partida ----------

    @Test
    void alTerminarRegistraElResultadoExportaElHistorialYMuestraLaPantallaDeFin() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Hace falta un entorno con pantalla para crear una ventana");
        List<ResultadoPartida> registrados = new ArrayList<>();
        RepositorioEstadisticas estadisticas = new EstadisticasEnMemoria(registrados);
        Path historiales = carpeta.resolve("historiales");
        VentanaPrincipal[] ventana = new VentanaPrincipal[1];
        ControladorAplicacion[] controlador = new ControladorAplicacion[1];

        SwingUtilities.invokeAndWait(() -> {
            ventana[0] = new VentanaPrincipal();
            controlador[0] = new ControladorAplicacion(ventana[0],
                    new RepositorioPartidasArchivo(carpeta.resolve("partidas")), estadisticas, historiales);
            controlador[0].iniciar();
        });
        try {
            // onPartidaTerminada pasa el trabajo al EDT con invokeLater; el invokeAndWait que
            // sigue espera a que ese trabajo termine
            controlador[0].onPartidaTerminada(RESULTADO);
            SwingUtilities.invokeAndWait(() -> { });

            assertEquals(List.of(RESULTADO), registrados);
            try (var archivos = Files.list(historiales)) {
                assertEquals(1, archivos.count(), "se exportó el historial");
            }
            assertEquals(Pantalla.FIN_PARTIDA, ventana[0].getPantallaActual());
        } finally {
            SwingUtilities.invokeAndWait(() -> ventana[0].dispose());
        }
    }

    /** Repositorio de estadísticas que anota lo que recibe, sin archivos. */
    private record EstadisticasEnMemoria(List<ResultadoPartida> registrados) implements RepositorioEstadisticas {
        @Override
        public void registrar(ResultadoPartida resultado) {
            registrados.add(resultado);
        }

        @Override
        public List<ResultadoPartida> obtenerTodos() throws PersistenciaException {
            return List.copyOf(registrados);
        }
    }
}
