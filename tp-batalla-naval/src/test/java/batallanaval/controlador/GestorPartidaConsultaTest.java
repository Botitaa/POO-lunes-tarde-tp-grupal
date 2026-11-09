package batallanaval.controlador;

import batallanaval.modelo.Barco;
import batallanaval.modelo.FabricaFlota;
import batallanaval.modelo.Posicion;
import batallanaval.modelo.Tablero;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests de los getters de consulta de GestorPartida (PAR-15, #60).
 */
class GestorPartidaConsultaTest {

    /**
     * Arma una partida EN_JUEGO para probar los getters.
     * J1 "Ana": 6 pts de movimiento, 1 de dominación, el barco 0 hundido (5 vivos), un mensaje en el resumen.
     * J2 "Beto": 4 pts de movimiento, 0 de dominación, 6 vivos, sin mensajes.
     * Todos los barcos tienen cooldownDisparo 2 y cooldownHabilidad 3.
     * Los números son distintos a propósito: si un getter cruza dos campos, el test lo agarra.
     */
    private GestorPartida crearPartida(int jugadorActivo) {
        List<DatosBarco> barcos = new ArrayList<>();
        List<Barco> flota1 = FabricaFlota.crearFlotaEstandar(1);
        List<Barco> flota2 = FabricaFlota.crearFlotaEstandar(7);
        for (int i = 0; i < 6; i++) {
            Barco b = flota1.get(i);
            int vida = (i == 0) ? 0 : b.getVidaMaxima();
            barcos.add(new DatosBarco(b.getId(), b.getSimbolo(), 1, new Posicion(0, i), vida, 2, 3, 0, false, false));
        }
        for (int i = 0; i < 6; i++) {
            Barco b = flota2.get(i);
            barcos.add(new DatosBarco(b.getId(), b.getSimbolo(), 2, new Posicion(9, i), b.getVidaMaxima(), 2, 3, 0, false, false));
        }
        List<DatosJugador> jugadores = List.of(
                new DatosJugador(1, "Ana", 6, 1, List.of("Te dispararon")),
                new DatosJugador(2, "Beto", 4, 0, List.of()));
        InstantaneaPartida instantanea = new InstantaneaPartida(
                "estandar", jugadorActivo, 1, FasePartida.EN_JUEGO, jugadores, barcos, List.of(), List.of());
        return GestorPartida.restaurar(instantanea, Tablero.crearMapaEstandar());
    }

    @Test
    void getJugadorActivoDevuelveAlQueTieneElTurno() {
        GestorPartida partida = crearPartida(1);

        EstadoJugador activo = partida.getJugadorActivo();

        assertEquals("Ana", activo.nombre());
        assertEquals(1, activo.numero());
        assertEquals(6, activo.puntosMovimiento());
        assertEquals(1, activo.puntosDominacion());
        assertEquals(5, activo.barcosVivos());
    }

    @Test
    void getRivalDevuelveAlOtroJugador() {
        GestorPartida partida = crearPartida(1);

        EstadoJugador rival = partida.getRival();

        assertEquals("Beto", rival.nombre());
        assertEquals(2, rival.numero());
        assertEquals(4, rival.puntosMovimiento());
        assertEquals(0, rival.puntosDominacion());
        assertEquals(6, rival.barcosVivos());
    }

    @Test
    void activoYRivalSeInviertenSiJuegaElJugador2() {
        GestorPartida partida = crearPartida(2);

        assertEquals("Beto", partida.getJugadorActivo().nombre());
        assertEquals("Ana", partida.getRival().nombre());
    }

    @Test
    void getBarcosPropiosNoIncluyeHundidos() {
        GestorPartida partida = crearPartida(1);

        List<EstadoBarco> propios = partida.getBarcosPropios();

        assertEquals(5, propios.size());
        for (EstadoBarco barco : propios) {
            assertTrue(barco.esPropio());
            assertEquals(1, barco.numeroJugador());
            assertTrue(barco.vida() > 0);
        }
    }

    @Test
    void getBarcosPropiosMuestraLosCooldownsReales() {
        GestorPartida partida = crearPartida(1);

        EstadoBarco barco = partida.getBarcosPropios().get(0);

        assertEquals(2, barco.cooldownDisparo());
        assertEquals(3, barco.cooldownHabilidad());
    }

    @Test
    void getBarcosPropiosNoSePuedeModificar() {
        GestorPartida partida = crearPartida(1);

        assertThrows(UnsupportedOperationException.class, () -> partida.getBarcosPropios().clear());
    }

    @Test
    void getMensajesResumenSonDelJugadorActivo() {
        assertEquals(List.of("Te dispararon"), crearPartida(1).getMensajesResumen());
        assertTrue(crearPartida(2).getMensajesResumen().isEmpty());
    }

    @Test
    void getBarcosEnemigosVisiblesVacioHastaPAR10() {
        GestorPartida partida = crearPartida(1);

        assertTrue(partida.getBarcosEnemigosVisibles().isEmpty());
    }
}