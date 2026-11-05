package batallanaval.controlador;

/**
 * Representación inmutable de solo lectura del estado de un jugador para la vista.
 *
 * @param nombre nombre del jugador.
 * @param numero 1 o 2.
 * @param puntosMovimiento puntos disponibles en el turno actual.
 * @param puntosDominacion puntos de victoria acumulados.
 * @param barcosVivos cantidad de navios que siguen a flote.
 * @param vidaTotal suma de salud de toda su flota.
 */
public record EstadoJugador(String nombre, int numero, int puntosMovimiento, int puntosDominacion, int barcosVivos, int vidaTotal) {
}
