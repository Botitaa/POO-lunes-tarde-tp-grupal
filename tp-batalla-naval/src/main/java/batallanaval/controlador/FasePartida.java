package batallanaval.controlador;

/**
 * Fases posibles del flujo de una partida de batalla naval.
 */
public enum FasePartida {

    /**
     * Fase inicial donde el Jugador 1 ubica sus navíos.
     */
    DESPLIEGUE_J1,

    /**
     * Fase donde el Jugador 2 ubica sus navíos en su sector.
     */
    DESPLIEGUE_J2,

    /**
     * Fase de combate activo por turnos alternados.
     */
    EN_JUEGO,

    /**
     * Partida concluida por victoria, derrota, empate o abandono.
     */
    TERMINADA

}
