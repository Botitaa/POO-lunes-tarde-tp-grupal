package batallanaval.controlador;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Registra los datos del resultado final de una partida concluida.
 *
 * @param fecha fecha y hora de conclusion.
 * @param jugador1 nombre del jugador 1.
 * @param jugador2 nombre del jugador 2.
 * @param ganador nombre del jugador ganador, o null en caso de empate.
 * @param rondas cantidad total de rondas disputadas.
 * @param motivo razon de finalizacion de la partida.
 */
public record ResultadoPartida(LocalDateTime fecha, String jugador1, String jugador2,
                               String ganador, int rondas, MotivoFin motivo) {

    public ResultadoPartida {
        Objects.requireNonNull(fecha, "La fecha no puede ser nula.");
        Objects.requireNonNull(jugador1, "El jugador 1 no puede ser nulo.");
        Objects.requireNonNull(jugador2, "El jugador 2 no puede ser nulo.");
        Objects.requireNonNull(motivo, "El motivo no puede ser nulo.");
        if (rondas < 0) {
            throw new IllegalArgumentException("Las rondas no pueden ser negativas.");
        }
    }

    /**
     * Indica si la partida termino en empate.
     *
     * @return true si no hubo ganador.
     */
    public boolean esEmpate() {
        return ganador == null;
    }
}
