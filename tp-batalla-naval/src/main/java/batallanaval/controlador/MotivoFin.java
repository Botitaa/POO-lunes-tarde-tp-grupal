package batallanaval.controlador;

/**
 * Motivos posibles por los cuales finaliza una partida.
 */
public enum MotivoFin {

    /**
     * Victoria al hundir todos los barcos del rival.
     */
    HUNDIMIENTO,

    /**
     * Victoria al acumular los puntos necesarios en zona central.
     */
    DOMINACION,

    /**
     * Fin por alcanzar el maximo de turnos permitidos.
     */
    LIMITE_TURNOS,

    /**
     * Fin de partida con igualdad de condiciones.
     */
    EMPATE,

    /**
     * Fin de partida por retiro voluntario de un jugador.
     */
    ABANDONO

}
