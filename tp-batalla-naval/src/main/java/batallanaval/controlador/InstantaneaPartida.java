package batallanaval.controlador;

import batallanaval.historial.EventoPartida;
import batallanaval.modelo.Posicion;
import java.util.List;

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
