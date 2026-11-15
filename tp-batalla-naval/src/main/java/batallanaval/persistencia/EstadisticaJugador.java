package batallanaval.persistencia;

import java.util.Objects;

/**
 * Registro inmutable que almacena las estadísticas acumuladas de un jugador:
 * total de partidas jugadas, ganadas, perdidas y empatadas.
 *
 * @param nombre nombre identificatorio del jugador.
 * @param jugadas cantidad total de partidas jugadas.
 * @param ganadas cantidad de partidas ganadas.
 * @param perdidas cantidad de partidas perdidas.
 * @param empatadas cantidad de partidas empatadas.
 */
public record EstadisticaJugador(String nombre, int jugadas, int ganadas,
                                 int perdidas, int empatadas) {

    /**
     * Valida que el nombre no sea nulo y que los contadores no sean negativos.
     *
     * @param nombre nombre identificatorio del jugador.
     * @param jugadas cantidad total de partidas jugadas.
     * @param ganadas cantidad de partidas ganadas.
     * @param perdidas cantidad de partidas perdidas.
     * @param empatadas cantidad de partidas empatadas.
     */
    public EstadisticaJugador {
        Objects.requireNonNull(nombre, "El nombre del jugador no puede ser nulo.");
        if (jugadas < 0 || ganadas < 0 || perdidas < 0 || empatadas < 0) {
            throw new IllegalArgumentException("Los valores estadísticos no pueden ser negativos.");
        }
    }

    /**
     * Calcula el porcentaje de victorias sobre el total de partidas jugadas.
     * Devuelve 0.0 si el jugador no disputó partidas.
     *
     * @return porcentaje de victorias en el rango de 0.0 a 100.0.
     */
    public double porcentajeVictorias() {
        if (jugadas == 0) {
            return 0.0;
        }
        return (double) ganadas / jugadas * 100;
    }
}
