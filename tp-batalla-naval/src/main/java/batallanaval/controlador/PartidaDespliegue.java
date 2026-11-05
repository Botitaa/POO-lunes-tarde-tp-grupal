package batallanaval.controlador;

import batallanaval.modelo.Posicion;

import java.util.List;

/**
 * Contrato de acciones permitidas durante las fases de despliegue inicial de barcos.
 */
public interface PartidaDespliegue {

    /**
     * Posiciona un barco en una casilla valida del tablero.
     *
     * @param idBarco identificador unico del barco.
     * @param destino coordenada elegida en el tablero.
     */
    void ubicarBarco(int idBarco, Posicion destino);

    /**
     * Retira un barco previamente posicionado para volverlo al estado no ubicado.
     *
     * @param idBarco identificador unico del barco.
     */
    void retirarBarco(int idBarco);

    /**
     * Devuelve la lista de barcos que todavia no fueron posicionados en el tablero.
     *
     * @return lista de estados de barcos sin ubicar.
     */
    List<EstadoBarco> getBarcosSinUbicar();

    /**
     * Confirma la distribucion de la flota si todos los barcos han sido ubicados.
     */
    void confirmarDespliegue();

}
