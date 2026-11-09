package batallanaval.persistencia;

import batallanaval.controlador.ResultadoPartida;
import batallanaval.excepciones.PersistenciaException;
import java.util.List;

/**
 * Contrato del repositorio para persistir y recuperar el historial de resultados
 * y estadísticas de las partidas finalizadas.
 */
public interface RepositorioEstadisticas {

    /**
     * Registra el resultado final de una partida en el almacenamiento de estadísticas.
     *
     * @param resultado resultado de la partida finalizada a almacenar.
     * @throws PersistenciaException si ocurre un error al escribir o acceder al almacenamiento.
     */
    void registrar(ResultadoPartida resultado) throws PersistenciaException;

    /**
     * Obtiene el listado completo de todos los resultados de partidas registradas.
     *
     * @return lista inmutable con los resultados de las partidas, preservando el orden de registro.
     * @throws PersistenciaException si ocurre un error de lectura al acceder al almacenamiento.
     */
    List<ResultadoPartida> obtenerTodos() throws PersistenciaException;
}
