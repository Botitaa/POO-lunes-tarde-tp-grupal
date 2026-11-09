package batallanaval.controlador;

import batallanaval.excepciones.AccionInvalidaException;
import batallanaval.historial.EventoPartida;
import batallanaval.historial.HistorialPartida;
import batallanaval.historial.TipoEvento;
import java.time.LocalDateTime;
import batallanaval.modelo.Acorazado;
import batallanaval.modelo.Barco;
import batallanaval.modelo.Casilla;
import batallanaval.modelo.Crucero;
import batallanaval.modelo.Destructor;
import batallanaval.modelo.Jugador;
import batallanaval.modelo.Posicion;
import batallanaval.modelo.Tablero;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Controlador principal de las reglas del juego y coordinador del estado de la partida (patrón Controlador GRASP).
 */
public class GestorPartida implements Partida {

    public static final int TURNOS_MAX = 40;
    public static final int PUNTOS_MOVIMIENTO_TURNO = 6;
    public static final int PUNTOS_DOMINACION_VICTORIA = 6;

    private final Jugador jugador1;
    private final Jugador jugador2;
    private final Tablero tablero;
    private final List<ObservadorPartida> observadores;
    private final HistorialPartida historial;
    private String mapa = "estandar";

    private Jugador jugadorActivo;
    private FasePartida fase;
    private int ronda;
    private Barco barcoSeleccionado;
    private List<Posicion> rutaPlanificada;
    private ResultadoPartida resultado;
    private boolean accionRealizadaEnTurno;

    /**
     * Construye una nueva partida con los dos jugadores y el tablero de juego.
     *
     * @param jugador1 primer jugador de la partida.
     * @param jugador2 segundo jugador de la partida.
     * @param tablero grilla y mapa de juego.
     */
    public GestorPartida(Jugador jugador1, Jugador jugador2, Tablero tablero) {
        this.jugador1 = Objects.requireNonNull(jugador1, "El jugador 1 no puede ser nulo.");
        this.jugador2 = Objects.requireNonNull(jugador2, "El jugador 2 no puede ser nulo.");
        this.tablero = Objects.requireNonNull(tablero, "El tablero no puede ser nulo.");
        this.observadores = new ArrayList<>();
        this.rutaPlanificada = new ArrayList<>();
        this.jugadorActivo = jugador1;
        this.fase = FasePartida.DESPLIEGUE_J1;
        this.ronda = 1;
        this.historial = new HistorialPartida();
        this.mapa = "estandar";
    }

    private GestorPartida(Jugador jugador1, Jugador jugador2, Tablero tablero, String mapa,
                          int jugadorActivoNum, int ronda, FasePartida fase, HistorialPartida historial) {
        this.jugador1 = jugador1;
        this.jugador2 = jugador2;
        this.tablero = tablero;
        this.mapa = (mapa != null && !mapa.isBlank()) ? mapa : "estandar";
        this.observadores = new ArrayList<>();
        this.rutaPlanificada = new ArrayList<>();
        this.barcoSeleccionado = null;
        this.accionRealizadaEnTurno = false;
        this.resultado = null;
        this.ronda = ronda;
        this.fase = fase;
        this.historial = (historial != null) ? historial : new HistorialPartida();
        this.jugadorActivo = (jugadorActivoNum == 1) ? jugador1 : jugador2;
    }

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

    private EstadoJugador aEstadoJugador(Jugador jugador) {

        return new EstadoJugador(jugador.getNombre(), jugador.getNumero(), jugador.getPuntosMovimiento(), jugador.getPuntosDominacion(), jugador.getBarcosVivos().size(), jugador.getVidaTotal());

    }

    private Jugador rival() {

        return (jugadorActivo == jugador1) ? jugador2 : jugador1;
    }

    private EstadoBarco aEstadoBarco(Barco barco, int numeroJugador, boolean esPropio) {

        return  new EstadoBarco(barco.getId(),
                numeroJugador,
                barco.getSimbolo(),
                barco.getPosicion(),
                barco.getVida(),
                barco.getVidaMaxima(),
                barco.getCasillasRestantes(),
                esPropio ? barco.getCooldownDisparo() : 0,
                esPropio ? barco.getCooldownHabilidad() : 0,
                barco.estaHabilidadActiva(),
                barco.getRangoDisparo(),
                barco.getEstadisticas().vision(),
                esPropio);

    }

    private void registrarEvento(TipoEvento tipo,String descripcion){

        EventoPartida evento = new EventoPartida(
                ronda,
                jugadorActivo.getNumero(),
                tipo,
                descripcion,
                LocalDateTime.now());

        historial.registrar(evento);

    }

    @Override
    public void agregarObservador(ObservadorPartida observador) {
        observadores.add(Objects.requireNonNull(observador, "El observador no puede ser nulo."));
    }

    @Override
    public void quitarObservador(ObservadorPartida observador) {
        observadores.remove(observador);
    }

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
        return fase == FasePartida.EN_JUEGO
                && !accionRealizadaEnTurno
                && (rutaPlanificada == null || rutaPlanificada.isEmpty())
                && barcoSeleccionado == null;
    }

    @Override
    public EstadoJugador getJugadorActivo() {
        return aEstadoJugador(jugadorActivo);
    }

    @Override
    public EstadoJugador getRival() {
        return aEstadoJugador(rival());
    }

    @Override
    public List<String> getMensajesResumen() {
        return jugadorActivo.getResumen().getMensajes();
    }

    @Override
    public List<EventoPartida> getHistorialVisible() {
        return historial.getEventosVisiblesPara(jugadorActivo.getNumero());
    }

    /**
     * Construye una instantánea completa (Memento) con el estado actual de la partida.
     *
     * @return instantanea inmutable de la partida.
     */
    public InstantaneaPartida crearInstantanea() {
        List<DatosJugador> datosJugadores = new ArrayList<>();
        datosJugadores.add(new DatosJugador(
                jugador1.getNumero(),
                jugador1.getNombre(),
                jugador1.getPuntosMovimiento(),
                jugador1.getPuntosDominacion(),
                jugador1.getResumen().getMensajes()
        ));
        datosJugadores.add(new DatosJugador(
                jugador2.getNumero(),
                jugador2.getNombre(),
                jugador2.getPuntosMovimiento(),
                jugador2.getPuntosDominacion(),
                jugador2.getResumen().getMensajes()
        ));

        List<DatosBarco> datosBarcos = new ArrayList<>();
        for (Barco barco : jugador1.getFlota()) {
            datosBarcos.add(new DatosBarco(
                    barco.getId(),
                    barco.getSimbolo(),
                    jugador1.getNumero(),
                    barco.getPosicion(),
                    barco.getVida(),
                    barco.getCooldownDisparo(),
                    barco.getCooldownHabilidad(),
                    barco.getCasillasRestantes(),
                    barco.estaHabilidadActiva(),
                    barco.estaRevelado()
            ));
        }
        for (Barco barco : jugador2.getFlota()) {
            datosBarcos.add(new DatosBarco(
                    barco.getId(),
                    barco.getSimbolo(),
                    jugador2.getNumero(),
                    barco.getPosicion(),
                    barco.getVida(),
                    barco.getCooldownDisparo(),
                    barco.getCooldownHabilidad(),
                    barco.getCasillasRestantes(),
                    barco.estaHabilidadActiva(),
                    barco.estaRevelado()
            ));
        }

        List<Posicion> restos = tablero.getRestos();
        List<EventoPartida> eventos = historial.getEventos();

        return new InstantaneaPartida(
                mapa,
                jugadorActivo.getNumero(),
                ronda,
                fase,
                datosJugadores,
                datosBarcos,
                restos,
                eventos
        );
    }

    void validarEstado() {
        if (jugador1 == null || jugador2 == null || tablero == null) {
            throw new IllegalArgumentException("Estado invalido: jugadores o tablero nulos");
        }
        if (jugador1.getNumero() != 1 || jugador2.getNumero() != 2) {
            throw new IllegalArgumentException("Estado invalido: numeros de jugador incorrectos");
        }
        if (jugadorActivo != jugador1 && jugadorActivo != jugador2) {
            throw new IllegalArgumentException("Estado invalido: jugador activo no es ni jugador1 ni jugador2");
        }
        if (ronda < 1 || ronda > TURNOS_MAX) {
            throw new IllegalArgumentException("Estado invalido: ronda fuera de rango: " + ronda);
        }
        if (fase != FasePartida.EN_JUEGO && fase != FasePartida.TERMINADA) {
            throw new IllegalArgumentException("Estado invalido: la partida no puede guardarse ni restaurarse en despliegue");
        }
        if (jugador1.getFlota().size() != 6 || jugador2.getFlota().size() != 6) {
            throw new IllegalArgumentException("Estado invalido: flota incompleta");
        }
        if (jugador1.getPuntosMovimiento() < 0 || jugador2.getPuntosMovimiento() < 0) {
            throw new IllegalArgumentException("Estado invalido: puntos de movimiento negativos");
        }
        if (jugador1.getPuntosDominacion() < 0 || jugador2.getPuntosDominacion() < 0) {
            throw new IllegalArgumentException("Estado invalido: puntos de dominacion negativos");
        }
        if (jugador1.getPuntosDominacion() > PUNTOS_DOMINACION_VICTORIA || jugador2.getPuntosDominacion() > PUNTOS_DOMINACION_VICTORIA) {
            throw new IllegalArgumentException("Estado invalido: puntos de dominacion excesivos");
        }

        Set<Posicion> posicionesOcupadas = new HashSet<>();
        List<Barco> todosLosBarcos = new ArrayList<>(jugador1.getFlota());
        todosLosBarcos.addAll(jugador2.getFlota());

        for (Barco b : todosLosBarcos) {
            if (b.getVida() < 0 || b.getVida() > b.getVidaMaxima()) {
                throw new IllegalArgumentException("Estado invalido: vida de barco fuera de rango");
            }
            if (b.getCooldownDisparo() < 0 || b.getCooldownHabilidad() < 0) {
                throw new IllegalArgumentException("Estado invalido: cooldown negativo");
            }
            if (b.getCasillasRestantes() < 0 || b.getCasillasRestantes() > b.getEstadisticas().movimientoMaxPorTurno()) {
                throw new IllegalArgumentException("Estado invalido: casillas restantes fuera de rango");
            }
            if (!b.estaHundido()) {
                Posicion pos = b.getPosicion();
                if (pos == null || !tablero.estaDentro(pos)) {
                    throw new IllegalArgumentException("Estado invalido: barco vivo sin posicion valida dentro del tablero");
                }
                if (!tablero.esTransitable(pos)) {
                    throw new IllegalArgumentException("Estado invalido: barco vivo sobre obstaculo");
                }
                if (!posicionesOcupadas.add(pos)) {
                    throw new IllegalArgumentException("Estado invalido: dos barcos vivos en la misma posicion: " + pos);
                }
            }
        }
    }

    /**
     * Reconstruye una instancia de GestorPartida a partir de una instantanea previa y un tablero.
     *
     * @param instantanea foto completa de la partida guardada.
     * @param tablero tablero de juego base a configurar.
     * @return nueva partida restaurada en el estado exacto.
     * @throws IllegalArgumentException si la instantanea contiene un estado invalido o inconsistente.
     */
    public static GestorPartida restaurar(InstantaneaPartida instantanea, Tablero tablero) {
        if (instantanea == null || tablero == null) {
            throw new IllegalArgumentException("La instantanea y el tablero no pueden ser nulos");
        }
        if (instantanea.jugadores() == null || instantanea.jugadores().size() != 2) {
            throw new IllegalArgumentException("Debe haber exactamente dos jugadores");
        }
        DatosJugador dj1 = null;
        DatosJugador dj2 = null;
        for (DatosJugador dj : instantanea.jugadores()) {
            if (dj == null) {
                throw new IllegalArgumentException("Datos de jugador nulos");
            }
            if (dj.numero() == 1 && dj1 == null) {
                dj1 = dj;
            } else if (dj.numero() == 2 && dj2 == null) {
                dj2 = dj;
            } else {
                throw new IllegalArgumentException("Jugadores invalidos en instantanea");
            }
        }
        if (dj1 == null || dj2 == null) {
            throw new IllegalArgumentException("Faltan datos de jugador 1 o 2");
        }
        if (instantanea.barcos() == null) {
            throw new IllegalArgumentException("Lista de barcos nula");
        }

        List<Barco> flotaJ1 = new ArrayList<>();
        List<Barco> flotaJ2 = new ArrayList<>();

        for (DatosBarco db : instantanea.barcos()) {
            if (db == null) {
                throw new IllegalArgumentException("Datos de barco nulos");
            }
            Barco barco;
            char tipo = db.tipo();
            if (tipo == 'A') {
                barco = new Acorazado(db.id());
            } else if (tipo == 'C') {
                barco = new Crucero(db.id());
            } else if (tipo == 'D') {
                barco = new Destructor(db.id());
            } else {
                throw new IllegalArgumentException("Tipo de barco desconocido: " + tipo);
            }
            barco.restaurar(
                    db.vida(),
                    db.posicion(),
                    db.cooldownDisparo(),
                    db.cooldownHabilidad(),
                    db.casillasRestantes(),
                    db.habilidadActiva(),
                    db.revelado()
            );

            if (db.jugador() == 1) {
                flotaJ1.add(barco);
            } else if (db.jugador() == 2) {
                flotaJ2.add(barco);
            } else {
                throw new IllegalArgumentException("Numero de jugador invalido en barco: " + db.jugador());
            }
        }

        Jugador j1 = new Jugador(dj1.numero(), dj1.nombre(), flotaJ1, dj1.puntosMovimiento(), dj1.puntosDominacion(), dj1.resumenPendiente());
        Jugador j2 = new Jugador(dj2.numero(), dj2.nombre(), flotaJ2, dj2.puntosMovimiento(), dj2.puntosDominacion(), dj2.resumenPendiente());

        if (instantanea.restos() != null) {
            for (Posicion resto : instantanea.restos()) {
                if (resto == null || !tablero.estaDentro(resto)) {
                    throw new IllegalArgumentException("Resto fuera del tablero");
                }
                tablero.convertirEnRestos(resto);
            }
        }

        HistorialPartida hist = new HistorialPartida();
        if (instantanea.eventos() != null) {
            for (EventoPartida evento : instantanea.eventos()) {
                if (evento != null) {
                    hist.registrar(evento);
                }
            }
        }

        GestorPartida partida = new GestorPartida(
                j1,
                j2,
                tablero,
                instantanea.mapa(),
                instantanea.jugadorActivo(),
                instantanea.ronda(),
                instantanea.fase(),
                hist
        );

        partida.validarEstado();
        return partida;
    }

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

        List<EstadoBarco> barcosPropios = new ArrayList<>();

        for (Barco barco: jugadorActivo.getBarcosVivos()){
            barcosPropios.add(aEstadoBarco(barco, jugadorActivo.getNumero(), true));
        }

        return List.copyOf(barcosPropios);

    }

    @Override
    public List<EstadoBarco> getBarcosEnemigosVisibles() {

        List<EstadoBarco> barcosEnemigosVisibles = new ArrayList<>();

//        for (Barco barco: rival().getBarcosVivos()){
//            barcosEnemigosVisibles.add(aEstadoBarco(barco, jugadorActivo.getNumero(), false));
//        }
// cuando marcso termine sigo me falta una depen

        return List.copyOf(barcosEnemigosVisibles);
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

    @Override
    public boolean esZonaDespliegue(Posicion posicion, int numeroJugador) {
        return tablero.esZonaDespliegue(posicion, numeroJugador);
    }

    @Override
    public boolean esZonaDominacion(Posicion posicion) {
        return tablero.esZonaDominacion(posicion);
    }
}
