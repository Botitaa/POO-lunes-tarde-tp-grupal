package batallanaval.controlador;

import batallanaval.modelo.Casilla;
import batallanaval.modelo.Posicion;

import java.util.List;
import java.util.Set;

/**
 * Operaciones y consultas necesarias para que el panel de la vista grafique el tablero.
 */
public interface PartidaTablero {

    /**
     * Devuelve el tipo de casilla ubicada en la coordenada indicada.
     *
     * @param posicion coordenada a consultar.
     * @return tipo de casilla.
     */
    Casilla getCasilla(Posicion posicion);

    /**
     * Indica si una posicion es visible para el jugador activo considerando la niebla de guerra.
     *
     * @param posicion coordenada a consultar.
     * @return true si la casilla esta descubierta.
     */
    boolean esVisible(Posicion posicion);

    /**
     * Devuelve la lista de barcos propios del jugador activo.
     *
     * @return lista de estados de barcos propios.
     */
    List<EstadoBarco> getBarcosPropios();

    /**
     * Devuelve los barcos del jugador rival que resultan visibles en este turno.
     *
     * @return lista de estados de barcos enemigos visibles.
     */
    List<EstadoBarco> getBarcosEnemigosVisibles();

    /**
     * Devuelve el barco propio que se encuentra actualmente seleccionado, o null si no hay ninguno.
     *
     * @return estado del barco seleccionado o null.
     */
    EstadoBarco getBarcoSeleccionado();

    /**
     * Devuelve las posiciones que componen la ruta de movimiento planificada.
     *
     * @return lista ordenada de coordenadas del camino.
     */
    List<Posicion> getRutaPlanificada();

    /**
     * Devuelve el costo total en puntos de movimiento que insumira la ruta planificada.
     *
     * @return costo en puntos de movimiento.
     */
    int getCostoRuta();

    /**
     * Devuelve el conjunto de casillas a las cuales el barco seleccionado podria desplazarse.
     *
     * @return conjunto de posiciones alcanzables.
     */
    Set<Posicion> getCasillasAlcanzables();

    /**
     * Devuelve los barcos enemigos que estan al alcance de tiro y con linea de vision libre.
     *
     * @return lista de objetivos disponibles para disparar.
     */
    List<EstadoBarco> getObjetivosValidos();

    /**
     * Indica si una posicion pertenece a la zona de despliegue del jugador indicado.
     *
     * @param posicion coordenada en el tablero.
     * @param numeroJugador 1 o 2.
     * @return true si corresponde a su zona de despliegue.
     */
    boolean esZonaDespliegue(Posicion posicion, int numeroJugador);

    /**
     * Indica si la casilla forma parte de las cuatro casillas de dominacion central.
     *
     * @param posicion coordenada en el tablero.
     * @return true si esta en la zona de dominacion.
     */
    boolean esZonaDominacion(Posicion posicion);

}
