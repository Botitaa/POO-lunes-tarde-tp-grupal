package batallanaval.modelo;

/**
 * Contiene los atributos estáticos y características fijas de un tipo de barco.
 *
 * @param vidaMaxima vida total máxima que puede alcanzar el barco.
 * @param movimientoMaxPorTurno casillas máximas que puede recorrer en un turno.
 * @param costoMovimiento costo en puntos de movimiento por cada casilla avanzada.
 * @param rangoDisparo distancia máxima en casillas para efectuar un disparo.
 * @param vision radio de visión del barco para revelar niebla de guerra.
 * @param cooldownDisparoMax turnos de espera requeridos tras registrar un disparo.
 * @param danio cantidad de puntos de vida que resta al impactar un objetivo.
 * @param cooldownHabilidadMax turnos de recarga tras activar su habilidad especial.
 */
public record EstadisticasBarco(
        int vidaMaxima,
        int movimientoMaxPorTurno,
        int costoMovimiento,
        int rangoDisparo,
        int vision,
        int cooldownDisparoMax,
        int danio,
        int cooldownHabilidadMax) {

    public EstadisticasBarco {
        if (vidaMaxima < 0 || movimientoMaxPorTurno < 0 || costoMovimiento < 0
                || rangoDisparo < 0 || vision < 0 || cooldownDisparoMax < 0
                || danio < 0 || cooldownHabilidadMax < 0) {
            throw new IllegalArgumentException("Las estadisticas no pueden ser negativas.");
        }
    }
}
