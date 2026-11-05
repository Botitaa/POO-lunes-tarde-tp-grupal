package batallanaval.persistencia;

import batallanaval.controlador.FasePartida;
import java.time.LocalDateTime;

/**
 * Metadatos esenciales de una partida guardada para permitir su listado e identificación rápida.
 *
 * @param versionFormato versión del formato de persistencia utilizado.
 * @param nombre nombre identificador del archivo de guardado.
 * @param fecha fecha y hora en la que se generó el guardado.
 * @param jugador1 nombre del primer jugador.
 * @param jugador2 nombre del segundo jugador.
 * @param ronda número de ronda en el momento del guardado.
 * @param fase fase en la que se encontraba la partida.
 */
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
