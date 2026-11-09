package batallanaval.historial;

/**
 * Representa los distintos tipos de eventos que pueden registrarse en el historial
 * de la partida, indicando si su visibilidad es publica para ambos jugadores o privada.
 */
public enum TipoEvento {
    
    DESPLIEGUE(false),
    MOVIMIENTO(false),
    HABILIDAD(false),
    DISPARO(true),
    HUNDIMIENTO(true),
    DOMINACION(true),
    FIN_TURNO(true),
    FIN_PARTIDA(true);

    private final boolean publico;

    TipoEvento(boolean publico) {
        this.publico = publico;
    }

    /**
     * Indica si el evento es publico y visible para el rival.
     * @return true si es publico, false si es privado del jugador.
     */
    public boolean esPublico() {
        return publico;
    }
}