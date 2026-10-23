package batallanaval.controlador;

import batallanaval.modelo.Casilla;
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

}
