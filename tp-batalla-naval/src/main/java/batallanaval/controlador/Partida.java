package batallanaval.controlador;

public interface Partida extends PartidaCombate, PartidaConsulta, PartidaDespliegue, PartidaTablero{

    void agregarObservador(ObservadorPartida observadorPartida);
    void quitarObservador(ObservadorPartida observadorPartida);

}
