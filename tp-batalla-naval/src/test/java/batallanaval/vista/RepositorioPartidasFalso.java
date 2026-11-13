package batallanaval.vista;

import java.util.ArrayList;
import java.util.List;

import batallanaval.controlador.GestorPartida;
import batallanaval.excepciones.PersistenciaException;
import batallanaval.persistencia.CabeceraGuardado;
import batallanaval.persistencia.RepositorioPartidas;

/**
 * {@link RepositorioPartidas} de prueba para los tests de la vista: no toca el disco. Devuelve lo que
 * el test le carga, anota qué se le pidió y, si el test cargó {@link #error}, lanza esa excepción.
 */
class RepositorioPartidasFalso implements RepositorioPartidas {

    List<CabeceraGuardado> guardados = new ArrayList<>();
    GestorPartida partidaACargar;
    PersistenciaException error = null;
    boolean falla = false;

    final List<String> nombresGuardados = new ArrayList<>();
    final List<String> nombresCargados = new ArrayList<>();
    final List<String> nombresBorrados = new ArrayList<>();
    GestorPartida ultimaPartidaGuardada;

    private void fallarSiCorresponde() throws PersistenciaException {
        if (error != null) {
            throw error;
        }
    }

    @Override
    public void guardar(GestorPartida partida, String nombre) throws PersistenciaException {
        fallarSiCorresponde();
        ultimaPartidaGuardada = partida;
        nombresGuardados.add(nombre);
    }

    @Override
    public GestorPartida cargar(String nombre) throws PersistenciaException {
        fallarSiCorresponde();
        nombresCargados.add(nombre);
        return partidaACargar;
    }

    @Override
    public List<CabeceraGuardado> listar() throws PersistenciaException {
        fallarSiCorresponde();
        return List.copyOf(guardados);
    }

    @Override
    public void borrar(String nombre) throws PersistenciaException {
        fallarSiCorresponde();
        nombresBorrados.add(nombre);
        guardados.removeIf(c -> c.nombre().equals(nombre));
    }

    @Override
    public boolean existe(String nombre) {
        return guardados.stream().anyMatch(c -> c.nombre().equals(nombre));
    }
}
