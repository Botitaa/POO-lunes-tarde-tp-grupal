package batallanaval.controlador;

import batallanaval.historial.EventoPartida;
import batallanaval.historial.TipoEvento;
import batallanaval.modelo.Barco;
import batallanaval.modelo.Casilla;
import batallanaval.modelo.FabricaFlota;
import batallanaval.modelo.Jugador;
import batallanaval.modelo.Posicion;
import batallanaval.modelo.Tablero;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GestorPartidaSerializacionTest {

    private Tablero crearTableroEstandar() {
        return Tablero.crearMapaEstandar();
    }

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

        Jugador j1 = new Jugador(1, "Jugador 1", flota1, 6, 1, List.of("Mensaje J1"));
        Jugador j2 = new Jugador(2, "Jugador 2", flota2, 6, 0, List.of("Mensaje J2"));

        Tablero tablero = crearTableroEstandar();
        tablero.convertirEnRestos(new Posicion(1, 1));

        List<DatosJugador> jugadores = List.of(
                new DatosJugador(1, "Jugador 1", 6, 1, List.of("Mensaje J1")),
                new DatosJugador(2, "Jugador 2", 6, 0, List.of("Mensaje J2"))
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

        EventoPartida evento = new EventoPartida(1, 1, TipoEvento.FIN_TURNO, "Turno finalizado", LocalDateTime.now());
        InstantaneaPartida instantanea = new InstantaneaPartida(
                "estandar",
                1,
                1,
                FasePartida.EN_JUEGO,
                jugadores,
                barcos,
                tablero.getRestos(),
                List.of(evento)
        );

        return GestorPartida.restaurar(instantanea, tablero);
    }

    @Test
    void restaurarCrearInstantaneaDaPartidaEquivalente() {
        GestorPartida partidaOriginal = crearPartidaEnJuegoValida();
        InstantaneaPartida instantanea1 = partidaOriginal.crearInstantanea();

        Tablero nuevoTablero = crearTableroEstandar();
        GestorPartida partidaRestaurada = GestorPartida.restaurar(instantanea1, nuevoTablero);
        InstantaneaPartida instantanea2 = partidaRestaurada.crearInstantanea();

        assertEquals(instantanea1, instantanea2);
    }

    @Test
    void restaurarLanzaIllegalStateExceptionSiFaseEsDespliegue() {
        GestorPartida partida = crearPartidaEnJuegoValida();
        InstantaneaPartida inst = partida.crearInstantanea();
        InstantaneaPartida instInvalida = new InstantaneaPartida(
                inst.mapa(),
                inst.jugadorActivo(),
                inst.ronda(),
                FasePartida.DESPLIEGUE_J1,
                inst.jugadores(),
                inst.barcos(),
                inst.restos(),
                inst.eventos()
        );

        assertThrows(IllegalStateException.class, () -> GestorPartida.restaurar(instInvalida, crearTableroEstandar()));
    }

    @Test
    void restaurarLanzaIllegalStateExceptionSiBarcosEnMismaPosicion() {
        GestorPartida partida = crearPartidaEnJuegoValida();
        InstantaneaPartida inst = partida.crearInstantanea();

        List<DatosBarco> barcosModificados = new ArrayList<>(inst.barcos());
        DatosBarco primerBarco = barcosModificados.get(0);
        DatosBarco segundoBarco = barcosModificados.get(1);
        barcosModificados.set(1, new DatosBarco(
                segundoBarco.id(),
                segundoBarco.tipo(),
                segundoBarco.jugador(),
                primerBarco.posicion(),
                segundoBarco.vida(),
                segundoBarco.cooldownDisparo(),
                segundoBarco.cooldownHabilidad(),
                segundoBarco.casillasRestantes(),
                segundoBarco.habilidadActiva(),
                segundoBarco.revelado()
        ));

        InstantaneaPartida instInvalida = new InstantaneaPartida(
                inst.mapa(),
                inst.jugadorActivo(),
                inst.ronda(),
                inst.fase(),
                inst.jugadores(),
                barcosModificados,
                inst.restos(),
                inst.eventos()
        );

        assertThrows(IllegalStateException.class, () -> GestorPartida.restaurar(instInvalida, crearTableroEstandar()));
    }

    @Test
    void restaurarLanzaIllegalStateExceptionSiBarcoSobreObstaculo() {
        GestorPartida partida = crearPartidaEnJuegoValida();
        InstantaneaPartida inst = partida.crearInstantanea();

        List<DatosBarco> barcosModificados = new ArrayList<>(inst.barcos());
        DatosBarco primerBarco = barcosModificados.get(0);
        barcosModificados.set(0, new DatosBarco(
                primerBarco.id(),
                primerBarco.tipo(),
                primerBarco.jugador(),
                new Posicion(3, 2),
                primerBarco.vida(),
                primerBarco.cooldownDisparo(),
                primerBarco.cooldownHabilidad(),
                primerBarco.casillasRestantes(),
                primerBarco.habilidadActiva(),
                primerBarco.revelado()
        ));

        InstantaneaPartida instInvalida = new InstantaneaPartida(
                inst.mapa(),
                inst.jugadorActivo(),
                inst.ronda(),
                inst.fase(),
                inst.jugadores(),
                barcosModificados,
                inst.restos(),
                inst.eventos()
        );

        assertThrows(IllegalStateException.class, () -> GestorPartida.restaurar(instInvalida, crearTableroEstandar()));
    }

    @Test
    void restaurarLanzaIllegalStateExceptionSiVidaEsNegativa() {
        GestorPartida partida = crearPartidaEnJuegoValida();
        InstantaneaPartida inst = partida.crearInstantanea();

        List<DatosBarco> barcosModificados = new ArrayList<>(inst.barcos());
        DatosBarco primerBarco = barcosModificados.get(0);
        barcosModificados.set(0, new DatosBarco(
                primerBarco.id(),
                primerBarco.tipo(),
                primerBarco.jugador(),
                primerBarco.posicion(),
                -1,
                primerBarco.cooldownDisparo(),
                primerBarco.cooldownHabilidad(),
                primerBarco.casillasRestantes(),
                primerBarco.habilidadActiva(),
                primerBarco.revelado()
        ));

        InstantaneaPartida instInvalida = new InstantaneaPartida(
                inst.mapa(),
                inst.jugadorActivo(),
                inst.ronda(),
                inst.fase(),
                inst.jugadores(),
                barcosModificados,
                inst.restos(),
                inst.eventos()
        );

        assertThrows(IllegalStateException.class, () -> GestorPartida.restaurar(instInvalida, crearTableroEstandar()));
    }

    @Test
    void puedeGuardarseSoloEnJuegoSinAcciones() {
        List<Barco> flota1 = FabricaFlota.crearFlotaEstandar(1);
        List<Barco> flota2 = FabricaFlota.crearFlotaEstandar(7);
        Jugador j1 = new Jugador(1, "J1", flota1);
        Jugador j2 = new Jugador(2, "J2", flota2);
        GestorPartida partidaDespliegue = new GestorPartida(j1, j2, crearTableroEstandar());

        assertFalse(partidaDespliegue.puedeGuardarse());

        GestorPartida partidaEnJuego = crearPartidaEnJuegoValida();
        assertTrue(partidaEnJuego.puedeGuardarse());
    }

    @Test
    void tableroExponeGetRestosYConvertirEnRestos() {
        Tablero tablero = crearTableroEstandar();
        assertTrue(tablero.getRestos().isEmpty());

        Posicion pos = new Posicion(2, 3);
        tablero.convertirEnRestos(pos);

        assertEquals(1, tablero.getRestos().size());
        assertEquals(pos, tablero.getRestos().get(0));
        assertEquals(Casilla.RESTOS, tablero.getCasilla(pos));
    }
}
