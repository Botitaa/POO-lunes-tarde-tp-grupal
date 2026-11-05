package batallanaval.controlador;

import batallanaval.modelo.Posicion;

/**
 * Contrato de operaciones disponibles para el jugador activo durante su turno de combate.
 */
public interface PartidaCombate {

    /**
     * Selecciona un barco propio para iniciar acciones de movimiento, disparo o habilidad.
     *
     * @param id identificador unico del barco.
     */
    void seleccionarBarco(int id);

    /**
     * Agrega un paso a la ruta planificada de desplazamiento.
     *
     * @param destino coordenada vecina transitable.
     */
    void agregarPaso(Posicion destino);

    /**
     * Deshace el ultimo paso agregado a la ruta planificada.
     */
    void deshacerPaso();

    /**
     * Ejecuta el movimiento planificado consumiendo puntos de movimiento.
     */
    void confirmarMovimiento();

    /**
     * Activa la habilidad especial del barco actualmente seleccionado.
     */
    void usarHabilidad();

    /**
     * Efectua un disparo sobre un barco enemigo identificado.
     *
     * @param idObjetivo identificador unico del barco blanco.
     */
    void disparar(int idObjetivo);

    /**
     * Da por finalizado el turno del jugador activo y pasa el control al rival.
     */
    void terminarTurno();

}
