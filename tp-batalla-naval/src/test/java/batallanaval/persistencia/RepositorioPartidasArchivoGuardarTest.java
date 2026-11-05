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
import batallanaval.modelo.Jugador;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RepositorioPartidasArchivoGuardarTest {

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
    void guardarPartidaValidaEscribeArchivoCorrectamente(@TempDir Path tempDir) throws PersistenciaException, IOException {
        RepositorioPartidasArchivo repo = new RepositorioPartidasArchivo(tempDir);
        GestorPartida partida = crearPartidaEnJuegoValida();

        repo.guardar(partida, "partida1");

        Path archivo = tempDir.resolve("partida1.sav");
        assertTrue(Files.exists(archivo));
        assertTrue(repo.existe("partida1"));

        List<String> lineas = Files.readAllLines(archivo);
        assertFalse(lineas.isEmpty());
        assertTrue(lineas.get(0).equals("BATALLA-NAVAL;1"));
        assertTrue(lineas.get(1).startsWith("CABECERA;partida1;"));
        assertTrue(lineas.get(1).contains(";Bota;Santi;12;EN_JUEGO"));
        assertTrue(lineas.contains("PARTIDA;estandar;2"));
        assertTrue(lineas.contains("JUGADOR;1;Bota;6;1"));
        assertTrue(lineas.contains("JUGADOR;2;Santi;6;0"));
        assertTrue(lineas.contains("RESUMEN;2;Tu Crucero (4,4) recibió 4 de daño desde (7,4)"));
        assertTrue(lineas.contains("RESTOS;5;3"));
    }

    @Test
    void guardarFallaSiNombreEsInvalido(@TempDir Path tempDir) {
        RepositorioPartidasArchivo repo = new RepositorioPartidasArchivo(tempDir);
        GestorPartida partida = crearPartidaEnJuegoValida();

        assertThrows(PersistenciaException.class, () -> repo.guardar(partida, null));
        assertThrows(PersistenciaException.class, () -> repo.guardar(partida, ""));
        assertThrows(PersistenciaException.class, () -> repo.guardar(partida, "partida con espacios"));
        assertThrows(PersistenciaException.class, () -> repo.guardar(partida, "partida;con;puntoycoma"));
        assertThrows(PersistenciaException.class, () -> repo.guardar(partida, "a".repeat(41)));
    }

    @Test
    void guardarFallaSiNoPuedeGuardarse(@TempDir Path tempDir) {
        RepositorioPartidasArchivo repo = new RepositorioPartidasArchivo(tempDir);
        List<Barco> f1 = FabricaFlota.crearFlotaEstandar(1);
        List<Barco> f2 = FabricaFlota.crearFlotaEstandar(7);
        Jugador j1 = new Jugador(1, "J1", f1);
        Jugador j2 = new Jugador(2, "J2", f2);
        GestorPartida partidaDespliegue = new GestorPartida(j1, j2, Tablero.crearMapaEstandar());

        assertThrows(PersistenciaException.class, () -> repo.guardar(partidaDespliegue, "partida1"));
    }

    @Test
    void guardarYRecuperarConSaltosDeLineaEnResumenYEvento(@TempDir Path tempDir) throws PersistenciaException {
        RepositorioPartidasArchivo repo = new RepositorioPartidasArchivo(tempDir);
        List<Barco> f1 = FabricaFlota.crearFlotaEstandar(1);
        f1.get(0).ubicarEn(new Posicion(0, 0));
        f1.get(1).ubicarEn(new Posicion(0, 1));
        f1.get(2).ubicarEn(new Posicion(0, 2));
        f1.get(3).ubicarEn(new Posicion(0, 3));
        f1.get(4).ubicarEn(new Posicion(0, 4));
        f1.get(5).ubicarEn(new Posicion(0, 5));

        List<Barco> f2 = FabricaFlota.crearFlotaEstandar(7);
        f2.get(0).ubicarEn(new Posicion(9, 0));
        f2.get(1).ubicarEn(new Posicion(9, 1));
        f2.get(2).ubicarEn(new Posicion(9, 2));
        f2.get(3).ubicarEn(new Posicion(9, 3));
        f2.get(4).ubicarEn(new Posicion(9, 4));
        f2.get(5).ubicarEn(new Posicion(9, 5));

        Tablero tablero = Tablero.crearMapaEstandar();

        List<DatosJugador> jugadores = List.of(
                new DatosJugador(1, "JugadorUno", 6, 1, List.of("Mensaje con\nsalto de linea")),
                new DatosJugador(2, "JugadorDos", 6, 0, List.of())
        );

        List<DatosBarco> barcos = new ArrayList<>();
        for (Barco b : f1) {
            barcos.add(new DatosBarco(b.getId(), b.getSimbolo(), 1, b.getPosicion(), b.getVida(),
                    b.getCooldownDisparo(), b.getCooldownHabilidad(), b.getCasillasRestantes(),
                    b.estaHabilidadActiva(), b.estaRevelado()));
        }
        for (Barco b : f2) {
            barcos.add(new DatosBarco(b.getId(), b.getSimbolo(), 2, b.getPosicion(), b.getVida(),
                    b.getCooldownDisparo(), b.getCooldownHabilidad(), b.getCasillasRestantes(),
                    b.estaHabilidadActiva(), b.estaRevelado()));
        }

        EventoPartida evento = new EventoPartida(1, 1, TipoEvento.FIN_TURNO, "Evento con\nsalto", LocalDateTime.now());
        InstantaneaPartida instantanea = new InstantaneaPartida(
                "estandar", 1, 1, FasePartida.EN_JUEGO, jugadores, barcos, List.of(), List.of(evento)
        );

        GestorPartida partida = GestorPartida.restaurar(instantanea, tablero);
        repo.guardar(partida, "partidaConSaltos");

        GestorPartida cargada = repo.cargar("partidaConSaltos");
        InstantaneaPartida instCargada = cargada.crearInstantanea();

        assertEquals(1, instCargada.jugadores().get(0).resumenPendiente().size());
        assertEquals("Mensaje con\nsalto de linea", instCargada.jugadores().get(0).resumenPendiente().get(0));
        assertEquals("Evento con\nsalto", instCargada.eventos().get(0).descripcion());
    }
}
