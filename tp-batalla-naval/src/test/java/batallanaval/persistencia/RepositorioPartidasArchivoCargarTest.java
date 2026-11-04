package batallanaval.persistencia;

import batallanaval.controlador.DatosBarco;
import batallanaval.controlador.DatosJugador;
import batallanaval.controlador.FasePartida;
import batallanaval.controlador.GestorPartida;
import batallanaval.controlador.InstantaneaPartida;
import batallanaval.excepciones.PersistenciaException;
import batallanaval.historial.EventoPartida;
import batallanaval.historial.TipoEvento;
import batallanaval.modelo.Barco;
import batallanaval.modelo.FabricaFlota;
import batallanaval.modelo.Posicion;
import batallanaval.modelo.Tablero;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RepositorioPartidasArchivoCargarTest {

    private GestorPartida crearPartidaEnJuegoValida() {
        List<Barco> flota1 = FabricaFlota.crearFlotaEstandar(1);
        flota1.get(0).ubicarEn(new Posicion(0, 0));
        flota1.get(1).ubicarEn(new Posicion(0, 1));
        flota1.get(2).ubicarEn(new Posicion(0, 2));
        flota1.get(3).ubicarEn(new Posicion(0, 3));
        flota1.get(4).ubicarEn(new Posicion(0, 4));
        flota1.get(5).ubicarEn(new Posicion(0, 5));

        List<Barco> flota2 = FabricaFlota.crearFlotaEstandar(7);
        flota2.get(0).ubicarEn(new Posicion(9, 0));
        flota2.get(1).ubicarEn(new Posicion(9, 1));
        flota2.get(2).ubicarEn(new Posicion(9, 2));
        flota2.get(3).ubicarEn(new Posicion(9, 3));
        flota2.get(4).ubicarEn(new Posicion(9, 4));
        flota2.get(5).ubicarEn(new Posicion(9, 5));

        Tablero tablero = Tablero.crearMapaEstandar();
        tablero.convertirEnRestos(new Posicion(5, 3));

        List<DatosJugador> jugadores = List.of(
                new DatosJugador(1, "Bota", 6, 1, List.of()),
                new DatosJugador(2, "Santi", 6, 0, List.of("Tu Crucero (4,4) recibió 4 de daño desde (7,4)"))
        );

        List<DatosBarco> barcos = new ArrayList<>();
        for (Barco b : flota1) {
            barcos.add(new DatosBarco(b.getId(), b.getSimbolo(), 1, b.getPosicion(), b.getVida(),
                    b.getCooldownDisparo(), b.getCooldownHabilidad(), b.getCasillasRestantes(),
                    b.estaHabilidadActiva(), b.estaRevelado()));
        }
        for (Barco b : flota2) {
            barcos.add(new DatosBarco(b.getId(), b.getSimbolo(), 2, b.getPosicion(), b.getVida(),
                    b.getCooldownDisparo(), b.getCooldownHabilidad(), b.getCasillasRestantes(),
                    b.estaHabilidadActiva(), b.estaRevelado()));
        }

        EventoPartida evento = new EventoPartida(12, 2, TipoEvento.DISPARO, "Acorazado (7,4) -> Crucero (4,4), -4", LocalDateTime.now());
        InstantaneaPartida instantanea = new InstantaneaPartida(
                "estandar",
                2,
                12,
                FasePartida.EN_JUEGO,
                jugadores,
                barcos,
                tablero.getRestos(),
                List.of(evento)
        );

        return GestorPartida.restaurar(instantanea, tablero);
    }

    @Test
    void guardarYCargarPartidaDevuelvePartidaEquivalente(@TempDir Path tempDir) throws PersistenciaException {
        RepositorioPartidasArchivo repo = new RepositorioPartidasArchivo(tempDir);
        GestorPartida partidaOriginal = crearPartidaEnJuegoValida();

        repo.guardar(partidaOriginal, "partida1");
        GestorPartida partidaCargada = repo.cargar("partida1");

        assertEquals(partidaOriginal.crearInstantanea(), partidaCargada.crearInstantanea());
    }

    @Test
    void cargarLanzaPersistenciaExceptionSiArchivoNoExiste(@TempDir Path tempDir) {
        RepositorioPartidasArchivo repo = new RepositorioPartidasArchivo(tempDir);
        assertThrows(PersistenciaException.class, () -> repo.cargar("no_existe"));
    }

    @Test
    void cargarLanzaPersistenciaExceptionSiMarcaInvalida(@TempDir Path tempDir) throws IOException {
        RepositorioPartidasArchivo repo = new RepositorioPartidasArchivo(tempDir);
        Path archivo = tempDir.resolve("invalida.sav");
        Files.write(archivo, List.of("OTRA-MARCA;1", "CABECERA;invalida;2026-11-04T18:00:00;J1;J2;1;EN_JUEGO"));

        PersistenciaException e = assertThrows(PersistenciaException.class, () -> repo.cargar("invalida"));
        assertTrue(e.getMessage().contains("Línea 1"));
    }

    @Test
    void cargarLanzaPersistenciaExceptionSiVersionInvalida(@TempDir Path tempDir) throws IOException {
        RepositorioPartidasArchivo repo = new RepositorioPartidasArchivo(tempDir);
        Path archivo = tempDir.resolve("version.sav");
        Files.write(archivo, List.of("BATALLA-NAVAL;2", "CABECERA;version;2026-11-04T18:00:00;J1;J2;1;EN_JUEGO"));

        PersistenciaException e = assertThrows(PersistenciaException.class, () -> repo.cargar("version"));
        assertTrue(e.getMessage().contains("Línea 1"));
    }

    @Test
    void cargarLanzaPersistenciaExceptionSiNumeroInvalido(@TempDir Path tempDir) throws IOException {
        RepositorioPartidasArchivo repo = new RepositorioPartidasArchivo(tempDir);
        Path archivo = tempDir.resolve("numero.sav");
        Files.write(archivo, List.of(
                "BATALLA-NAVAL;1",
                "CABECERA;numero;2026-11-04T18:00:00;J1;J2;NO_NUMERO;EN_JUEGO"
        ));

        PersistenciaException e = assertThrows(PersistenciaException.class, () -> repo.cargar("numero"));
        assertTrue(e.getMessage().contains("Línea 2"));
    }

    @Test
    void cargarLanzaPersistenciaExceptionSiLineaMalFormada(@TempDir Path tempDir) throws IOException {
        RepositorioPartidasArchivo repo = new RepositorioPartidasArchivo(tempDir);
        Path archivo = tempDir.resolve("corta.sav");
        Files.write(archivo, List.of(
                "BATALLA-NAVAL;1",
                "CABECERA;corta;2026-11-04T18:00:00"
        ));

        PersistenciaException e = assertThrows(PersistenciaException.class, () -> repo.cargar("corta"));
        assertTrue(e.getMessage().contains("Línea 2"));
    }
}
