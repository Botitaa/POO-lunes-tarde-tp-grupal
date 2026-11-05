package batallanaval.controlador;

/**
 * Interfaz integral de la partida que unifica los contratos de combate, consulta,
 * despliegue y tablero, gestionando ademas el registro de observadores (patron Observer).
 */
public interface Partida extends PartidaCombate, PartidaConsulta, PartidaDespliegue, PartidaTablero {

    /**
     * Registra un nuevo observador para recibir eventos y cambios de estado en la partida.
     *
     * @param observadorPartida instancia del observador a suscribir.
     */
    void agregarObservador(ObservadorPartida observadorPartida);

    /**
     * Da de baja un observador previamente registrado.
     *
     * @param observadorPartida instancia del observador a desuscribir.
     */
    void quitarObservador(ObservadorPartida observadorPartida);

}
