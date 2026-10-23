package batallanaval.historial;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Record inmutable que almacena la informacion detallada de un evento ocurrido
 * durante la partida (ronda, jugador, tipo, descripcion y marca temporal).
 */
public record EventoPartida(int ronda, int numeroJugador, TipoEvento tipo,
                            String descripcion, LocalDateTime momento) {
    /**
     * Crea un evento validando que tipo, descripcion y momento no sean nulos.
     *
     * @throws NullPointerException si tipo, descripcion o momento son nulos.
     */
    public EventoPartida {
        Objects.requireNonNull(tipo, "El tipo de evento no puede ser nulo.");
        Objects.requireNonNull(descripcion, "La descripcion no puede ser nula.");
        Objects.requireNonNull(momento, "El momento no puede ser nulo.");
    }

    /**
     * Determina si este evento puede ser visto por un jugador determinado,
     * respetando las reglas de visibilidad y niebla de guerra.
     * 
     * @param numeroJugadorConsulta Numero del jugador que intenta ver el evento (1 o 2).
     * @return true si el evento es publico o si pertenece al jugador consultado.
     */
    public boolean esVisiblePara(int numeroJugadorConsulta) {
        return tipo.esPublico() || this.numeroJugador == numeroJugadorConsulta;
    }

    /**
     * Devuelve una representacion formateada del evento en una sola linea de texto.
     * Ejemplo: "[R12] J2 DISPARO: Acorazado (7,4) -> Crucero (4,4), -4"
     * 
     * @return Linea de texto con el formato estandar del historial.
     */
    public String aLinea() {
        return String.format("[R%d] J%d %s: %s", ronda, numeroJugador, tipo, descripcion);
    }
}
