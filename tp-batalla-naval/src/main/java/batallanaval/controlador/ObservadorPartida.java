package batallanaval.controlador;

/**
 * Interfaz que implementan la vista y otros observadores para reaccionar a cambios en la partida.
 */
public interface ObservadorPartida {

    /**
     * Notifica que ocurrio un cambio en el estado del tablero o de las acciones en juego.
     */
    void onEstadoCambiado();

    /**
     * Notifica un cambio de fase general (por ejemplo de despliegue a combate o finalizacion).
     *
     * @param fasePartida nueva fase de la partida.
     */
    void onFaseCambiada(FasePartida fasePartida);

    /**
     * Notifica que se inicio el turno de un jugador.
     *
     * @param nombreJugador nombre del jugador cuyo turno comienza.
     */
    void onTurnoIniciado(String nombreJugador);

    /**
     * Notifica la finalizacion de la partida junto con su resultado.
     *
     * @param resultadoPartida informacion final de la partida concluida.
     */
    void onPartidaTerminada(ResultadoPartida resultadoPartida);
}
