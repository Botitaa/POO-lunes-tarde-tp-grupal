package batallanaval.app;

import batallanaval.controlador.GestorPartida;
import batallanaval.excepciones.PersistenciaException;
import batallanaval.modelo.Barco;
import batallanaval.modelo.FabricaFlota;
import batallanaval.modelo.Jugador;
import batallanaval.modelo.Posicion;
import batallanaval.modelo.Tablero;
import batallanaval.persistencia.CabeceraGuardado;
import batallanaval.persistencia.ConfiguracionJuego;
import batallanaval.persistencia.RepositorioPartidas;
import batallanaval.persistencia.RepositorioPartidasArchivo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoGuardadoTest {

    private GestorPartida crearPartidaEnJuego() {
        List<Barco> flota1 = FabricaFlota.crearFlotaEstandar(1);
        List<Barco> flota2 = FabricaFlota.crearFlotaEstandar(7);
        Jugador j1 = new Jugador(1, "Bota", flota1);
        Jugador j2 = new Jugador(2, "Santi", flota2);
        GestorPartida partida = new GestorPartida(j1, j2, Tablero.crearMapaEstandar());

        for (int i = 0; i < 6; i++) {
            partida.ubicarBarco(flota1.get(i).getId(), new Posicion(0, i));
        }
        partida.confirmarDespliegue();

        for (int i = 0; i < 6; i++) {
            partida.ubicarBarco(flota2.get(i).getId(), new Posicion(9, i));
        }
        partida.confirmarDespliegue();

        return partida;
    }

    @Test
    void guardaAutoguardadoEnCadaTurnoIniciadoSiConfigLoHabilita(@TempDir Path tempDir) {
        GestorPartida partida = crearPartidaEnJuego();
        RepositorioPartidasArchivo repositorio = new RepositorioPartidasArchivo(tempDir);
        ConfiguracionJuego config = ConfiguracionJuego.cargar(tempDir.resolve("config.properties"));
        config.setAutoguardado(true);
        List<String> errores = new ArrayList<>();

        AutoGuardado autoGuardado = new AutoGuardado(partida, repositorio, config, errores::add);
        partida.agregarObservador(autoGuardado);

        partida.terminarTurno();

        assertTrue(Files.exists(tempDir.resolve("autoguardado.sav")));
        assertTrue(repositorio.existe(AutoGuardado.NOMBRE_ARCHIVO));
        assertTrue(errores.isEmpty());
    }

    @Test
    void noGuardaSiConfigLoDeshabilita(@TempDir Path tempDir) {
        GestorPartida partida = crearPartidaEnJuego();
        RepositorioPartidasArchivo repositorio = new RepositorioPartidasArchivo(tempDir);
        ConfiguracionJuego config = ConfiguracionJuego.cargar(tempDir.resolve("config.properties"));
        config.setAutoguardado(false);
        List<String> errores = new ArrayList<>();

        AutoGuardado autoGuardado = new AutoGuardado(partida, repositorio, config, errores::add);
        partida.agregarObservador(autoGuardado);

        partida.terminarTurno();

        assertFalse(Files.exists(tempDir.resolve("autoguardado.sav")));
        assertTrue(errores.isEmpty());
    }

    @Test
    void siFallaElGuardadoAvisaEnBarraDeEstadoYLaPartidaSigue(@TempDir Path tempDir) {
        GestorPartida partida = crearPartidaEnJuego();
        ConfiguracionJuego config = ConfiguracionJuego.cargar(tempDir.resolve("config.properties"));
        config.setAutoguardado(true);
        List<String> errores = new ArrayList<>();

        RepositorioPartidas repositorioQueFalla = new RepositorioPartidas() {
            @Override
            public void guardar(GestorPartida p, String nombre) throws PersistenciaException {
                throw new PersistenciaException("Error al escribir autoguardado.sav");
            }

            @Override
            public GestorPartida cargar(String nombre) {
                return null;
            }

            @Override
            public List<CabeceraGuardado> listar() {
                return List.of();
            }

            @Override
            public void borrar(String nombre) {
            }

            @Override
            public boolean existe(String nombre) {
                return false;
            }
        };

        AutoGuardado autoGuardado = new AutoGuardado(partida, repositorioQueFalla, config, errores::add);
        partida.agregarObservador(autoGuardado);

        partida.terminarTurno();

        assertEquals(1, errores.size());
        assertEquals("Error al escribir autoguardado.sav", errores.get(0));
        assertFalse(partida.estaTerminada());
        assertEquals("Santi", partida.getJugadorActivo().nombre());
    }

    @Test
    void noGuardaMientrasEstaEnDespliegue(@TempDir Path tempDir) {
        List<Barco> flota1 = FabricaFlota.crearFlotaEstandar(1);
        List<Barco> flota2 = FabricaFlota.crearFlotaEstandar(7);
        GestorPartida partida = new GestorPartida(
                new Jugador(1, "Bota", flota1),
                new Jugador(2, "Santi", flota2),
                Tablero.crearMapaEstandar()
        );
        RepositorioPartidasArchivo repositorio = new RepositorioPartidasArchivo(tempDir);
        ConfiguracionJuego config = ConfiguracionJuego.cargar(tempDir.resolve("config.properties"));
        List<String> errores = new ArrayList<>();

        AutoGuardado autoGuardado = new AutoGuardado(partida, repositorio, config, errores::add);
        partida.agregarObservador(autoGuardado);

        for (int i = 0; i < 6; i++) {
            partida.ubicarBarco(flota1.get(i).getId(), new Posicion(0, i));
        }
        partida.confirmarDespliegue();

        assertFalse(Files.exists(tempDir.resolve("autoguardado.sav")));
        assertTrue(errores.isEmpty());
    }
}
