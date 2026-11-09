package batallanaval.historial;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Historial de la partida: guarda en orden los eventos confirmados que ve el jugador.
 * Lo crea GestorPartida y viaja dentro del guardado.
 */
public class HistorialPartida {

    // Eventos en el orden en que se registraron (composición: el historial es dueño de su lista)
    private final List<EventoPartida> eventos = new ArrayList<>();

    /**
     * Agrega un evento al final del historial.
     *
     * @param evento evento a registrar.
     * @throws NullPointerException si el evento es nulo.
     */
    public void registrar(EventoPartida evento) {
        eventos.add(Objects.requireNonNull(evento, "El evento no puede ser nulo."));
    }

    /**
     * Devuelve todos los eventos en orden, sin filtrar.
     *
     * @return copia no modificable de la lista de eventos.
     */
    public List<EventoPartida> getEventos() {
        return List.copyOf(eventos);
    }

    /**
     * Devuelve los eventos de una ronda, en orden.
     *
     * @param ronda número de ronda.
     * @return copia no modificable con los eventos de esa ronda (vacía si no hay).
     */
    public List<EventoPartida> getEventosDeRonda(int ronda) {
        List<EventoPartida> resultado = new ArrayList<>();
        for (EventoPartida evento : eventos) {
            if (evento.ronda() == ronda) {
                resultado.add(evento);
            }
        }
        return List.copyOf(resultado);
    }

    /**
     * Devuelve los eventos que puede ver un jugador, respetando la niebla de guerra:
     * los públicos y los privados que son suyos.
     *
     * @param numeroJugador número del jugador que consulta (1 o 2).
     * @return copia no modificable con los eventos visibles para ese jugador.
     */
    public List<EventoPartida> getEventosVisiblesPara(int numeroJugador) {
        List<EventoPartida> resultado = new ArrayList<>();
        for (EventoPartida evento : eventos) {
            if (evento.esVisiblePara(numeroJugador)) {
                resultado.add(evento);
            }
        }
        return List.copyOf(resultado);
    }

    /**
     * Devuelve cuántos eventos hay registrados.
     *
     * @return cantidad de eventos.
     */
    public int cantidad() {
        return eventos.size();
    }
}
