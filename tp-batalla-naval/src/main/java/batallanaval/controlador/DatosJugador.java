package batallanaval.controlador;

import java.util.List;

/**
 * Datos inmutables requeridos para persistir o restaurar el estado de un jugador.
 *
 * @param numero numero asignado al jugador (1 o 2).
 * @param nombre nombre del jugador.
 * @param puntosMovimiento puntos de movimiento restantes.
 * @param puntosDominacion puntos de dominacion acumulados.
 * @param resumenPendiente lista de mensajes pendientes del resumen de turno.
 */
public record DatosJugador(
        int numero,
        String nombre,
        int puntosMovimiento,
        int puntosDominacion,
        List<String> resumenPendiente
) {
}
