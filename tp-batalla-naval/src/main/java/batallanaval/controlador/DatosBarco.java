package batallanaval.controlador;

import batallanaval.modelo.Posicion;

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
