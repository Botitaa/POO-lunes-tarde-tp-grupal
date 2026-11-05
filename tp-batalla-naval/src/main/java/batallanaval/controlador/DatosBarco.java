package batallanaval.controlador;

import batallanaval.modelo.Posicion;

/**
 * Datos inmutables requeridos para persistir o restaurar el estado completo de un barco.
 *
 * @param id identificador unico del navio.
 * @param tipo caracter del tipo de barco ('A', 'C', 'D').
 * @param jugador numero de jugador propietario (1 o 2).
 * @param posicion coordenada en el tablero (o null si no fue ubicado).
 * @param vida salud actual restante.
 * @param cooldownDisparo turnos restantes de recarga de disparo.
 * @param cooldownHabilidad turnos restantes de recarga de habilidad.
 * @param casillasRestantes casillas restantes de movimiento en el turno.
 * @param habilidadActiva true si tenia activa su habilidad especial.
 * @param revelado true si estaba revelado por disparar.
 */
public record DatosBarco(
        int id,
        char tipo,
        int jugador,
        Posicion posicion,
        int vida,
        int cooldownDisparo,
        int cooldownHabilidad,
        int casillasRestantes,
        boolean habilidadActiva,
        boolean revelado
) {
}
