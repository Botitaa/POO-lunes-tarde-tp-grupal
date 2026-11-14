package batallanaval.app;

import batallanaval.controlador.FasePartida;
import batallanaval.controlador.GestorPartida;
import batallanaval.controlador.ObservadorPartida;
import batallanaval.controlador.ResultadoPartida;
import batallanaval.excepciones.PersistenciaException;
import batallanaval.persistencia.ConfiguracionJuego;
import batallanaval.persistencia.RepositorioPartidas;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Observador de la partida que realiza el guardado automático en {@code autoguardado.sav}
 * al iniciar cada turno si la configuración lo tiene habilitado.
 */
public class AutoGuardado implements ObservadorPartida {

    /**
     * Nombre del archivo de autoguardado (sin la extensión .sav).
     */
    public static final String NOMBRE_ARCHIVO = "autoguardado";

    private final GestorPartida partida;
    private final RepositorioPartidas repositorio;
    private final ConfiguracionJuego configuracion;
    private final Consumer<String> alError;

    /**
     * Crea el observador de autoguardado para una partida.
     *
     * @param partida partida a observar y guardar.
     * @param repositorio repositorio donde se almacena el archivo de autoguardado.
     * @param configuracion configuración del juego que indica si el autoguardado está activo.
     * @param alError acción para mostrar un aviso en la barra de estado si falla el guardado.
     */
    public AutoGuardado(GestorPartida partida, RepositorioPartidas repositorio,
                        ConfiguracionJuego configuracion, Consumer<String> alError) {
        this.partida = Objects.requireNonNull(partida, "La partida no puede ser nula.");
        this.repositorio = Objects.requireNonNull(repositorio, "El repositorio no puede ser nulo.");
        this.configuracion = Objects.requireNonNull(configuracion, "La configuracion no puede ser nula.");
        this.alError = Objects.requireNonNull(alError, "El consumidor de error no puede ser nulo.");
    }

    /**
     * Se invoca cuando cambia el estado general de la partida. No realiza acciones.
     */
    @Override
    public void onEstadoCambiado() {
    }

    /**
     * Se invoca cuando cambia la fase de la partida. No realiza acciones.
     *
     * @param fasePartida nueva fase de la partida.
     */
    @Override
    public void onFaseCambiada(FasePartida fasePartida) {
    }

    /**
     * Guarda la partida en {@code autoguardado.sav} al comenzar cada turno si el autoguardado
     * está habilitado en la configuración y la partida se encuentra en estado guardable.
     * Si ocurre un error de persistencia, avisa en la barra de estado y la partida continúa.
     *
     * @param nombreJugador nombre del jugador que inicia su turno.
     */
    @Override
    public void onTurnoIniciado(String nombreJugador) {
        if (!configuracion.isAutoguardado() || !partida.puedeGuardarse()) {
            return;
        }

        try {
            repositorio.guardar(partida, NOMBRE_ARCHIVO);
        } catch (PersistenciaException e) {
            alError.accept(e.getMessage());
        }
    }

    /**
     * Se invoca cuando termina la partida. No realiza acciones.
     *
     * @param resultadoPartida resultado final de la partida.
     */
    @Override
    public void onPartidaTerminada(ResultadoPartida resultadoPartida) {
    }
}
