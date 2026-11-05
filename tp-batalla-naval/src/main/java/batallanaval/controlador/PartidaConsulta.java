package batallanaval.controlador;

import batallanaval.historial.EventoPartida;
import java.util.List;

/**
 * Consulta de solo lectura sobre el estado general de la partida para las pantallas de la vista.
 */
public interface PartidaConsulta {

    /**
     * Devuelve el estado de solo lectura del jugador activo en este turno.
     *
     * @return estado del jugador en turno.
     */
    EstadoJugador getJugadorActivo();

    /**
     * Devuelve el estado de solo lectura del jugador rival.
     *
     * @return estado del rival.
     */
    EstadoJugador getRival();

    /**
     * Devuelve el numero de ronda actual de la partida.
     *
     * @return ronda en curso.
     */
    int getRonda();

    /**
     * Devuelve la fase actual en la que se encuentra el juego.
     *
     * @return fase de la partida.
     */
    FasePartida getFase();

    /**
     * Indica si la partida ha finalizado.
     *
     * @return true si la fase es TERMINADA.
     */
    boolean estaTerminada();

    /**
     * Devuelve el resultado final de la partida o null si aun no ha concluido.
     *
     * @return resultado o null.
     */
    ResultadoPartida getResultado();

    /**
     * Devuelve la lista de mensajes ocurridos durante el turno inactivo del jugador.
     *
     * @return lista inmodificable de mensajes del resumen.
     */
    List<String> getMensajesResumen();

    /**
     * Devuelve los eventos del historial que son visibles respetando niebla de guerra.
     *
     * @return lista de eventos visibles.
     */
    List<EventoPartida> getHistorialVisible();

    /**
     * Comprueba si la partida se encuentra en condiciones validas para ser guardada en archivo.
     *
     * @return true si puede guardarse en este momento.
     */
    boolean puedeGuardarse();

}
