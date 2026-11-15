package batallanaval.controlador;

public interface ObservadorPartida {

    void onEstadoCambiado();
    void onFaseCambiada(FasePartida fasePartida);
    void onTurnoIniciado(String nombreJugador);
    void onPartidaTerminada(ResultadoPartida resultadoPartida);
}
