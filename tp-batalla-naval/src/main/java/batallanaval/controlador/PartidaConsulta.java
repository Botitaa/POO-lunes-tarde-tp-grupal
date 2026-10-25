package batallanaval.controlador;

import java.util.List;
import batallanaval.historial.EventoPartida;

public interface PartidaConsulta {

    EstadoJugador getJugadorActivo();
    EstadoJugador getRival();
    int getRonda();
    FasePartida getFase();
    boolean estaTerminada();
    ResultadoPartida getResultado();
    List<String> getMensajesResumen();
    List<EventoPartida> getHistorialVisible();
    boolean puedeGuardarse();

}
