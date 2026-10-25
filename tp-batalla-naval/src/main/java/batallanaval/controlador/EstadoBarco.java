package batallanaval.controlador;

import batallanaval.modelo.Posicion;

public record EstadoBarco(int id, int numeroJugador, char simbolo, Posicion posicion,
                          int vida, int vidaMaxima, int casillasRestantes,
                          int cooldownDisparo, int cooldownHabilidad,
                          boolean habilidadActiva, int rangoDisparo, int vision,
                          boolean esPropio){
}
