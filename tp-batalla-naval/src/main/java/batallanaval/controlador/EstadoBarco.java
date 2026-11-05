package batallanaval.controlador;

import batallanaval.modelo.Posicion;

/**
 * Representación inmutable de solo lectura de un barco para ser consumida por la vista.
 *
 * @param id identificador unico del navio.
 * @param numeroJugador 1 o 2 segun el propietario.
 * @param simbolo caracter identificatorio ('A', 'C', 'D').
 * @param posicion coordenada en el mapa (o null si no fue ubicado).
 * @param vida puntos de salud actuales.
 * @param vidaMaxima salud maxima del modelo.
 * @param casillasRestantes pasos de movimiento pendientes este turno.
 * @param cooldownDisparo turnos restantes de recarga de disparo.
 * @param cooldownHabilidad turnos restantes de recarga de habilidad.
 * @param habilidadActiva true si tiene activa la habilidad temporal.
 * @param rangoDisparo distancia maxima efectiva de tiro.
 * @param vision alcance de visibilidad en niebla.
 * @param esPropio true si pertenece al jugador que esta observando.
 */
public record EstadoBarco(int id, int numeroJugador, char simbolo, Posicion posicion,
                          int vida, int vidaMaxima, int casillasRestantes,
                          int cooldownDisparo, int cooldownHabilidad,
                          boolean habilidadActiva, int rangoDisparo, int vision,
                          boolean esPropio) {
}
