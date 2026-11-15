package batallanaval.app;

import batallanaval.controlador.MotivoFin;
import batallanaval.controlador.ResultadoPartida;
import batallanaval.excepciones.PersistenciaException;
import batallanaval.historial.EventoPartida;
import batallanaval.historial.TipoEvento;
import batallanaval.controlador.GestorPartida;
import batallanaval.modelo.FabricaFlota;
import batallanaval.modelo.Jugador;
import batallanaval.modelo.Posicion;
import batallanaval.modelo.Tablero;
import batallanaval.persistencia.ConfiguracionJuego;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Tests del controlador de la aplicación: el cierre de la partida (exportar el historial, registrar
 * las estadísticas y pasar a la pantalla de fin), el autoguardado y los nombres de la configuración. El resto del flujo entre pantallas
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
                    new RepositorioPartidasArchivo(carpeta.resolve("partidas")), estadisticas, historiales,
                    ConfiguracionJuego.cargar(carpeta.resolve("config.properties")));
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

    // ---------- configuración y autoguardado ----------

    @Test
    void precargaLosUltimosNombresYLosRecuerdaAlComenzar() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Hace falta un entorno con pantalla para crear una ventana");
        Path archivoConfig = carpeta.resolve("config.properties");
        ConfiguracionJuego config = ConfiguracionJuego.cargar(archivoConfig);
        config.setUltimoJugador1("Ana");
        config.setUltimoJugador2("Beto");
        config.guardar();

        enAplicacion(ConfiguracionJuego.cargar(archivoConfig), controlador -> {
            controlador.comenzarPartidaNueva();

            assertEquals("Ana", controlador.getPartida().getJugadorActivo().nombre());
            ConfiguracionJuego releida = ConfiguracionJuego.cargar(archivoConfig);
            assertEquals("Ana", releida.getUltimoJugador1());
            assertEquals("Beto", releida.getUltimoJugador2());
        });
    }

    @Test
    void conAutoguardadoActivoGuardaAlEmpezarCadaTurno() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Hace falta un entorno con pantalla para crear una ventana");
        ConfiguracionJuego config = ConfiguracionJuego.cargar(carpeta.resolve("config.properties"));
        config.setAutoguardado(true);

        enAplicacion(config, controlador -> {
            GestorPartida partida = partidaEnJuego();
            controlador.alCargarPartida(partida);

            partida.terminarTurno();

            assertTrue(Files.exists(carpeta.resolve("partidas").resolve(AutoGuardado.NOMBRE_ARCHIVO + ".sav")),
                    "se creó el autoguardado");
        });
    }

    @Test
    void conAutoguardadoApagadoNoGuarda() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Hace falta un entorno con pantalla para crear una ventana");
        ConfiguracionJuego config = ConfiguracionJuego.cargar(carpeta.resolve("config.properties"));
        config.setAutoguardado(false);

        enAplicacion(config, controlador -> {
            GestorPartida partida = partidaEnJuego();
            controlador.alCargarPartida(partida);

            partida.terminarTurno();

            assertFalse(Files.exists(carpeta.resolve("partidas").resolve(AutoGuardado.NOMBRE_ARCHIVO + ".sav")));
        });
    }

    /** Arma la aplicación con archivos en la carpeta temporal, corre {@code prueba} en el EDT y cierra la ventana. */
    private void enAplicacion(ConfiguracionJuego config, PruebaEnEdt prueba) throws Exception {
        VentanaPrincipal[] ventana = new VentanaPrincipal[1];
        Throwable[] error = new Throwable[1];
        SwingUtilities.invokeAndWait(() -> {
            ventana[0] = new VentanaPrincipal();
            ControladorAplicacion controlador = new ControladorAplicacion(ventana[0],
                    new RepositorioPartidasArchivo(carpeta.resolve("partidas")),
                    new EstadisticasEnMemoria(new ArrayList<>()), carpeta.resolve("historiales"), config);
            controlador.iniciar();
            try {
                prueba.ejecutar(controlador);
            } catch (Throwable t) {
                error[0] = t;
            }
        });
        SwingUtilities.invokeAndWait(() -> ventana[0].dispose());
        if (error[0] instanceof Exception e) {
            throw e;
        }
        if (error[0] instanceof Error e) {
            throw e;
        }
    }

    @FunctionalInterface
    private interface PruebaEnEdt {
        void ejecutar(ControladorAplicacion controlador) throws Exception;
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