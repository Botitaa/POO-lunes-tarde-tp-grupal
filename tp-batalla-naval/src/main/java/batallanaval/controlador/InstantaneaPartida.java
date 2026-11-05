package batallanaval.controlador;

import batallanaval.historial.EventoPartida;
import batallanaval.modelo.Posicion;
import java.util.List;

/**
 * Fotografía completa e inmutable del estado de una partida para ser persistida o restaurada (Memento).
 *
 * @param mapa nombre identificador del mapa de juego.
 * @param jugadorActivo numero del jugador que tiene el turno actual (1 o 2).
 * @param ronda numero de ronda actual.
 * @param fase fase en la que se encontraba la partida al guardarse.
 * @param jugadores datos inmutables de los dos jugadores.
 * @param barcos datos inmutables de la totalidad de barcos de la partida.
 * @param restos coordenadas de casillas convertidas en restos de barcos hundidos.
 * @param eventos lista ordenada de eventos acontecidos en la partida.
 */
public record InstantaneaPartida(
        String mapa,
        int jugadorActivo,
        int ronda,
        FasePartida fase,
        List<DatosJugador> jugadores,
        List<DatosBarco> barcos,
        List<Posicion> restos,
        List<EventoPartida> eventos
) {
}
