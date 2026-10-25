package batallanaval.controlador;

import batallanaval.modelo.Posicion;

import java.util.List;

public interface PartidaDespliegue {

    void ubicarBarco(int idBarco, Posicion destino);
    void retirarBarco(int idBarco);
    List<EstadoBarco> getBarcosSinUbicar();
    void confirmarDespliegue();

}
