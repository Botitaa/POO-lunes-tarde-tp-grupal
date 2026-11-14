package batallanaval.persistencia;

import batallanaval.controlador.GestorPartida;
import batallanaval.controlador.InstantaneaPartida;
import batallanaval.excepciones.PersistenciaException;
import java.util.List;

/**
 * Contrato del repositorio para almacenar, cargar, listar y eliminar partidas guardadas.
 */
public interface RepositorioPartidas {

    /**
     * Guarda en disco una foto de la partida asignándole un nombre.
     * <p>
     * Recibe la {@link InstantaneaPartida} y no el {@link GestorPartida} para poder escribir el
     * archivo en un hilo aparte sin leer la partida mientras alguien la modifica: quien guarda arma
     * la instantánea con {@code partida.crearInstantanea()} en el hilo de Swing (después de chequear
     * {@code partida.puedeGuardarse()}) y al repositorio le llega solo esa copia.
     *
     * @param instantanea foto de la partida a persistir (tiene que estar en {@code EN_JUEGO}).
     * @param nombre nombre identificatorio del archivo.
     * @throws PersistenciaException si el nombre es inválido, la instantánea no es de una partida en
     *                               juego o falla la escritura.
     */
    void guardar(InstantaneaPartida instantanea, String nombre) throws PersistenciaException;

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
