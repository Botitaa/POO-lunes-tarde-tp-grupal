package batallanaval.vista;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import batallanaval.controlador.EstadoBarco;
import batallanaval.controlador.EstadoJugador;
import batallanaval.controlador.FasePartida;
import batallanaval.controlador.ObservadorPartida;
import batallanaval.controlador.Partida;
import batallanaval.controlador.ResultadoPartida;
import batallanaval.excepciones.AccionInvalidaException;
import batallanaval.historial.EventoPartida;
import batallanaval.modelo.Casilla;
import batallanaval.modelo.Posicion;

/**
 * {@link Partida} de prueba para los tests de la pantalla de juego. No aplica reglas: devuelve
 * el estado que el test le carga en sus atributos y cuenta cuántas veces se llamó cada acción.
 * <p>
 * Como la partida real, cuando una acción se ejecuta avisa {@code onEstadoCambiado()} a sus
 * observadores; si el test cargó {@link #rechazo}, la acción lanza esa excepción y no avisa nada.
 */
class PartidaFalsa implements Partida {

    // Estado que lee la vista (lo cambia cada test)
    FasePartida fase = FasePartida.EN_JUEGO;
    boolean terminada = false;
    int ronda = 1;
    EstadoJugador activo = new EstadoJugador("Bota", 1, 6, 0, 6, 34);
    EstadoJugador rival = new EstadoJugador("Santi", 2, 6, 0, 6, 34);
    EstadoBarco seleccionado = null;
    List<Posicion> ruta = new ArrayList<>();
    int costoRuta = 0;
    List<EstadoBarco> objetivos = new ArrayList<>();

    /**
     * Si no es {@code null}, cualquier acción del turno la lanza. Una {@link AccionInvalidaException}
     * simula que la partida rechazó la acción; cualquier otra {@code RuntimeException} simula un
     * error de programación (por ejemplo, un {@code IllegalStateException}).
     */
    RuntimeException rechazo = null;

    // Contadores de las acciones pedidas
    int vecesConfirmarMovimiento;
    int vecesDeshacerPaso;
    int vecesUsarHabilidad;
    int vecesTerminarTurno;
    final List<Integer> disparosA = new ArrayList<>();

    final List<ObservadorPartida> observadores = new ArrayList<>();

    /** Barco propio de ejemplo: un crucero sano con la habilidad disponible. */
    static EstadoBarco crucero(int id, Posicion posicion) {
        return new EstadoBarco(id, 1, 'C', posicion, 5, 5, 3, 0, 0, false, 4, 3, true);
    }

    /** Barco enemigo de ejemplo: un destructor con la vida que se pida. */
    static EstadoBarco destructorEnemigo(int id, Posicion posicion, int vida) {
        return new EstadoBarco(id, 2, 'D', posicion, vida, 3, 4, 0, 0, false, 2, 2, false);
    }

    private void aplicar(Runnable cambio) {
        if (rechazo != null) {
            throw rechazo;
        }
        cambio.run();
        for (ObservadorPartida observador : observadores) {
            observador.onEstadoCambiado();
        }
    }

    // PartidaCombate

    @Override
    public void seleccionarBarco(int id) {
        aplicar(() -> { });
    }

    @Override
    public void agregarPaso(Posicion destino) {
        aplicar(() -> ruta.add(destino));
    }

    @Override
    public void deshacerPaso() {
        aplicar(() -> vecesDeshacerPaso++);
    }

    @Override
    public void confirmarMovimiento() {
        aplicar(() -> vecesConfirmarMovimiento++);
    }

    @Override
    public void usarHabilidad() {
        aplicar(() -> vecesUsarHabilidad++);
    }

    @Override
    public void disparar(int idObjetivo) {
        aplicar(() -> disparosA.add(idObjetivo));
    }

    @Override
    public void terminarTurno() {
        aplicar(() -> vecesTerminarTurno++);
    }

    @Override
    public void cancelarSeleccion() {
        aplicar(() -> seleccionado = null);
    }

    // PartidaConsulta

    @Override
    public EstadoJugador getJugadorActivo() {
        return activo;
    }

    @Override
    public EstadoJugador getRival() {
        return rival;
    }

    @Override
    public int getRonda() {
        return ronda;
    }

    @Override
    public FasePartida getFase() {
        return fase;
    }

    @Override
    public boolean estaTerminada() {
        return terminada;
    }

    @Override
    public ResultadoPartida getResultado() {
        return null;
    }

    @Override
    public List<String> getMensajesResumen() {
        return List.of();
    }

    @Override
    public List<EventoPartida> getHistorialVisible() {
        return List.of();
    }

    @Override
    public boolean puedeGuardarse() {
        return false;
    }

    // PartidaTablero

    @Override
    public Casilla getCasilla(Posicion posicion) {
        return Casilla.AGUA;
    }

    @Override
    public boolean esVisible(Posicion posicion) {
        return true;
    }

    @Override
    public List<EstadoBarco> getBarcosPropios() {
        return seleccionado == null ? List.of() : List.of(seleccionado);
    }

    @Override
    public List<EstadoBarco> getBarcosEnemigosVisibles() {
        return List.copyOf(objetivos);
    }

    @Override
    public EstadoBarco getBarcoSeleccionado() {
        return seleccionado;
    }

    @Override
    public List<Posicion> getRutaPlanificada() {
        return List.copyOf(ruta);
    }

    @Override
    public int getCostoRuta() {
        return costoRuta;
    }

    @Override
    public Set<Posicion> getCasillasAlcanzables() {
        return Set.of();
    }

    @Override
    public List<EstadoBarco> getObjetivosValidos() {
        return List.copyOf(objetivos);
    }

    @Override
    public boolean esZonaDespliegue(Posicion posicion, int numeroJugador) {
        return false;
    }

    @Override
    public boolean esZonaDominacion(Posicion posicion) {
        return false;
    }

    // PartidaDespliegue (no se usa en la pantalla de juego)

    @Override
    public void ubicarBarco(int idBarco, Posicion destino) {
        throw new UnsupportedOperationException("No se usa en estos tests");
    }

    @Override
    public void retirarBarco(int idBarco) {
        throw new UnsupportedOperationException("No se usa en estos tests");
    }

    @Override
    public List<EstadoBarco> getBarcosSinUbicar() {
        return List.of();
    }

    @Override
    public void confirmarDespliegue() {
        throw new UnsupportedOperationException("No se usa en estos tests");
    }

    // Partida

    @Override
    public void agregarObservador(ObservadorPartida observador) {
        observadores.add(observador);
    }

    @Override
    public void quitarObservador(ObservadorPartida observador) {
        observadores.remove(observador);
    }
}
