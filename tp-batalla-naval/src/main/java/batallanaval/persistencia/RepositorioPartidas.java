package batallanaval.persistencia;

import batallanaval.controlador.GestorPartida;
import batallanaval.excepciones.PersistenciaException;
import java.util.List;

/**
 * Contrato del repositorio para almacenar, cargar, listar y eliminar partidas guardadas.
 */
public interface RepositorioPartidas {

    /**
     * Guarda el estado actual de una partida en disco asignándole un nombre.
     *
     * @param partida instancia de la partida a persistir.
     * @param nombre nombre identificatorio del archivo.
     * @throws PersistenciaException si el nombre es inválido, la partida no puede guardarse o falla la escritura.
     */
    void guardar(GestorPartida partida, String nombre) throws PersistenciaException;

    /**
     * Carga y reconstruye una partida previamente guardada.
     *
     * @param nombre nombre del guardado a recuperar.
     * @return partida restaurada y lista para continuar.
     * @throws PersistenciaException si el archivo no existe, está corrupto o tiene formato inválido.
     */
    GestorPartida cargar(String nombre) throws PersistenciaException;

    /**
     * Obtiene el listado de cabeceras de todas las partidas guardadas, ordenadas de más reciente a más antigua.
     *
     * @return lista de cabeceras disponibles.
     * @throws PersistenciaException si ocurre un error al acceder a la carpeta de guardados.
     */
    List<CabeceraGuardado> listar() throws PersistenciaException;

    /**
     * Elimina permanentemente un archivo de partida guardada.
     *
     * @param nombre nombre de la partida a borrar.
     * @throws PersistenciaException si el archivo no existe o no pudo eliminarse.
     */
    void borrar(String nombre) throws PersistenciaException;

    /**
     * Comprueba si existe un archivo de partida guardado con el nombre especificado.
     *
     * @param nombre nombre a verificar.
     * @return true si el archivo existe.
     */
    boolean existe(String nombre);
}
