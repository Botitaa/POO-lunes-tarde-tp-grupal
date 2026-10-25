package batallanaval.controlador;

import batallanaval.modelo.Posicion;

public interface PartidaCombate {

    void seleccionarBarco(int id);
    void agregarPaso(Posicion destino);
    void deshacerPaso();
    void confirmarMovimiento();
    void usarHabilidad();
    void disparar(int idObjetivo);
    void terminarTurno();

}
