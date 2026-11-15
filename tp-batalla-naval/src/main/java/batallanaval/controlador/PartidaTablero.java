package batallanaval.controlador;

import batallanaval.modelo.Casilla;
import batallanaval.modelo.Disparo;
import batallanaval.modelo.Posicion;

import java.util.List;
import java.util.Set;

public interface PartidaTablero {

    Casilla getCasilla(Posicion posicion);
    boolean esVisible(Posicion posicion);
    List<EstadoBarco> getBarcosPropios();
    List<EstadoBarco> getBarcosEnemigosVisibles();
    EstadoBarco getBarcoSeleccionado();
    List<Posicion> getRutaPlanificada();
    int getCostoRuta();
    Set<Posicion> getCasillasAlcanzables();
    List<EstadoBarco> getObjetivosValidos();
    boolean esZonaDespliegue(Posicion posicion, int numeroJugador);
    boolean esZonaDominacion(Posicion posicion);

    /**
     * Los disparos que recibió el jugador activo durante el último turno del rival, con sus
     * casillas, para que el tablero los marque. Por defecto, ninguno.
     *
     * @return lista inmutable (vacía si no le pegaron o si la partida se acaba de cargar)
     */
    default List<Disparo> getDisparosRecibidos() {
        return List.of();
    }

    /**
     * El último disparo hecho en este turno, para animarlo. Por defecto, ninguno.
     *
     * @return el disparo, o {@code null} si todavía no se disparó en este turno
     */
    default Disparo getUltimoDisparo() {
        return null;
    }

    /**
     * Si un barco propio todavía puede moverse o disparar en este turno. El tablero apaga los
     * que ya no pueden hacer nada. Por defecto, sí.
     *
     * @param idBarco id de un barco del jugador activo
     * @return {@code true} si puede moverse o disparar
     */
    default boolean puedeActuar(int idBarco) {
        return true;
    }

}
