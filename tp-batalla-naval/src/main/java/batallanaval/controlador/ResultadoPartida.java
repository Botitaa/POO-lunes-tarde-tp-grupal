package batallanaval.controlador;

import java.time.LocalDateTime;
import java.util.Objects;

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

    public boolean esEmpate() {
        return ganador == null;
    }
}