package batallanaval.controlador;

import batallanaval.excepciones.AccionInvalidaException;
import batallanaval.historial.EventoPartida;
import batallanaval.modelo.Barco;
import batallanaval.modelo.Casilla;
import batallanaval.modelo.Jugador;
import batallanaval.modelo.Posicion;
import batallanaval.modelo.Tablero;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;


public class GestorPartida implements Partida {

    public static final int TURNOS_MAX = 40;
    public static final int PUNTOS_MOVIMIENTO_TURNO = 6;
    public static final int PUNTOS_DOMINACION_VICTORIA = 6;

    private final Jugador jugador1;
    private final Jugador jugador2;
    private final Tablero tablero;
    private final List<ObservadorPartida> observadores;
    // private final HistorialPartida historial;   // pendiente: issue #68 (Bruno)

    private Jugador jugadorActivo;
    private FasePartida fase;
    private int ronda;
    private Barco barcoSeleccionado;
    private List<Posicion> rutaPlanificada;
    private ResultadoPartida resultado;
    private boolean accionRealizadaEnTurno;

    public GestorPartida(Jugador jugador1, Jugador jugador2, Tablero tablero) {
        this.jugador1 = Objects.requireNonNull(jugador1, "El jugador 1 no puede ser nulo.");
        this.jugador2 = Objects.requireNonNull(jugador2, "El jugador 2 no puede ser nulo.");
        this.tablero = Objects.requireNonNull(tablero, "El tablero no puede ser nulo.");
        this.observadores = new ArrayList<>();
        this.rutaPlanificada = new ArrayList<>();
        this.jugadorActivo = jugador1;
        this.fase = FasePartida.DESPLIEGUE_J1;
        this.ronda = 1;
    }

    // ---------- chequeo de fase ----------

    private void exigirDespliegue() {
        if (fase != FasePartida.DESPLIEGUE_J1 && fase != FasePartida.DESPLIEGUE_J2) {
            throw new AccionInvalidaException("La acción solo se puede hacer durante el despliegue.");
        }
    }

    private void exigirEnJuego() {
        if (fase != FasePartida.EN_JUEGO) {
            throw new AccionInvalidaException("La acción solo se puede hacer mientras se juega.");
        }
    }

    // ---------- observadores ----------

    @Override
    public void agregarObservador(ObservadorPartida observador) {
        observadores.add(Objects.requireNonNull(observador, "El observador no puede ser nulo."));
    }

    @Override
    public void quitarObservador(ObservadorPartida observador) {
        observadores.remove(observador);
    }

    // ---------- PartidaDespliegue ----------

    @Override
    public void ubicarBarco(int idBarco, Posicion destino) {
        exigirDespliegue();
        throw new UnsupportedOperationException("Pendiente: PAR-05 (#50)");
    }

    @Override
    public void retirarBarco(int idBarco) {
        exigirDespliegue();
        throw new UnsupportedOperationException("Pendiente: PAR-05 (#50)");
    }

    @Override
    public List<EstadoBarco> getBarcosSinUbicar() {
        throw new UnsupportedOperationException("Pendiente: PAR-05 (#50)");
    }

    @Override
    public void confirmarDespliegue() {
        exigirDespliegue();
        throw new UnsupportedOperationException("Pendiente: PAR-05 (#50)");
    }

    // ---------- PartidaCombate ----------

    @Override
    public void seleccionarBarco(int id) {
        exigirEnJuego();
        throw new UnsupportedOperationException("Pendiente: PAR-07 (#52)");
    }

    @Override
    public void agregarPaso(Posicion destino) {
        exigirEnJuego();
        throw new UnsupportedOperationException("Pendiente: PAR-07 (#52)");
    }

    @Override
    public void deshacerPaso() {
        exigirEnJuego();
        throw new UnsupportedOperationException("Pendiente: PAR-07 (#52)");
    }

    @Override
    public void confirmarMovimiento() {
        exigirEnJuego();
        throw new UnsupportedOperationException("Pendiente: PAR-08 (#53)");
    }

    @Override
    public void usarHabilidad() {
        exigirEnJuego();
        throw new UnsupportedOperationException("Pendiente: PAR-11 (#56)");
    }

    @Override
    public void disparar(int idObjetivo) {
        exigirEnJuego();
        throw new UnsupportedOperationException("Pendiente: PAR-09 (#54)");
    }

    @Override
    public void terminarTurno() {
        exigirEnJuego();
        throw new UnsupportedOperationException("Pendiente: PAR-06 (#51)");
    }

    // ---------- PartidaConsulta ----------

    @Override
    public FasePartida getFase() {
        return fase;
    }

    @Override
    public int getRonda() {
        return ronda;
    }

    @Override
    public boolean estaTerminada() {
        return fase == FasePartida.TERMINADA;
    }

    @Override
    public ResultadoPartida getResultado() {
        return resultado;
    }

    @Override
    public boolean puedeGuardarse() {
        throw new UnsupportedOperationException("Pendiente: guardado");
    }

    @Override
    public EstadoJugador getJugadorActivo() {
        throw new UnsupportedOperationException("Pendiente: PAR-15 (#60)");
    }

    @Override
    public EstadoJugador getRival() {
        throw new UnsupportedOperationException("Pendiente: PAR-15 (#60)");
    }

    @Override
    public List<String> getMensajesResumen() {
        throw new UnsupportedOperationException("Pendiente: PAR-15 (#60)");
    }

    @Override
    public List<EventoPartida> getHistorialVisible() {
        throw new UnsupportedOperationException("Pendiente: LOG-08 (#69)");
    }

    // ---------- PartidaTablero ----------

    @Override
    public Casilla getCasilla(Posicion posicion) {
        return tablero.getCasilla(posicion);
    }

    @Override
    public boolean esVisible(Posicion posicion) {
        throw new UnsupportedOperationException("Pendiente: PAR-10 (#55)");
    }

    @Override
    public List<EstadoBarco> getBarcosPropios() {
        throw new UnsupportedOperationException("Pendiente: PAR-15 (#60)");
    }

    @Override
    public List<EstadoBarco> getBarcosEnemigosVisibles() {
        throw new UnsupportedOperationException("Pendiente: PAR-15 (#60)");
    }

    @Override
    public EstadoBarco getBarcoSeleccionado() {
        throw new UnsupportedOperationException("Pendiente: PAR-07 (#52)");
    }

    @Override
    public List<Posicion> getRutaPlanificada() {
        throw new UnsupportedOperationException("Pendiente: PAR-07 (#52)");
    }

    @Override
    public int getCostoRuta() {
        throw new UnsupportedOperationException("Pendiente: PAR-07 (#52)");
    }

    @Override
    public Set<Posicion> getCasillasAlcanzables() {
        throw new UnsupportedOperationException("Pendiente: PAR-07 (#52)");
    }

    @Override
    public List<EstadoBarco> getObjetivosValidos() {
        throw new UnsupportedOperationException("Pendiente: PAR-09 (#54)");
    }
}