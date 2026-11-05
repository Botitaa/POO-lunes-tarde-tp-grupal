package batallanaval.modelo;


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
