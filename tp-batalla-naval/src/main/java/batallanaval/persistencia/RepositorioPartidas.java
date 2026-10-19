package batallanaval.persistencia;

import batallanaval.controlador.GestorPartida;
import batallanaval.excepciones.PersistenciaException;
import java.util.List;

public interface RepositorioPartidas {

    void guardar(GestorPartida partida, String nombre) throws PersistenciaException;

    GestorPartida cargar(String nombre) throws PersistenciaException;

    List<CabeceraGuardado> listar() throws PersistenciaException;

    void borrar(String nombre) throws PersistenciaException;

    boolean existe(String nombre);
}
    