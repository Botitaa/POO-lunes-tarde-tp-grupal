package batallanaval.persistencia;

import batallanaval.controlador.FasePartida;
import java.time.LocalDateTime;

public record CabeceraGuardado(
        int versionFormato,
        String nombre,
        LocalDateTime fecha,
        String jugador1,
        String jugador2,
        int ronda,
        FasePartida fase
) {
}
