package batallanaval.vista;

import java.awt.BorderLayout;
import java.awt.event.HierarchyEvent;
import java.io.Serial;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import javax.swing.JPanel;

import batallanaval.controlador.FasePartida;
import batallanaval.controlador.ObservadorPartida;
import batallanaval.controlador.Partida;
import batallanaval.controlador.ResultadoPartida;

/**
 * Pantalla de juego ({@link Pantalla#JUEGO}): el {@link PanelTablero} en el centro y el
 * {@link PanelLateral} (HUD con ronda, puntos, dominación, ficha del barco y botones) a la
 * derecha y el {@link PanelHistorial} (plegable) abajo.
 * <p>
 * Arma sus tres paneles con la misma {@link Partida} y les reenvía los avisos de la partida:
 * así quien la crea (en la aplicación, {@code ControladorAplicacion}) registra un solo
 * observador, {@code partida.agregarObservador(panelJuego)}, y no tiene que conocer los
 * paneles de adentro. Ninguno decide reglas: solo leen la partida y le piden acciones.
 */
public final class PanelJuego extends JPanel implements ObservadorPartida {

    @Serial
    private static final long serialVersionUID = 1L;

    private final PanelTablero panelTablero;
    private final PanelLateral panelLateral;
    private final PanelHistorial panelHistorial;

    /**
     * Crea la pantalla de juego con su tablero, su panel lateral y su historial.
     *
     * @param partida partida que se muestra y a la que se le piden las acciones
     * @param alError qué hacer con el mensaje cuando la partida rechaza una acción de un botón
     *                (en la aplicación, {@code ventana::mostrarError})
     * @throws NullPointerException si algún argumento es {@code null}
     */
    public PanelJuego(Partida partida, Consumer<String> alError) {
        Objects.requireNonNull(partida, "partida");
        Objects.requireNonNull(alError, "alError");

        // PanelTablero pide por separado lo que dibuja y lo que ejecutan sus atajos (ISP):
        // las dos interfaces las implementa la misma Partida.
        panelTablero = new PanelTablero(partida, partida);
        panelLateral = new PanelLateral(partida, alError);
        panelHistorial = new PanelHistorial(partida);

        setLayout(new BorderLayout());
        add(panelTablero, BorderLayout.CENTER);
        add(panelLateral, BorderLayout.EAST);
        add(panelHistorial, BorderLayout.SOUTH);

        // VentanaPrincipal solo le inicia el hilo a la pantalla registrada (esta), no a los paneles de
        // adentro. Por eso el hilo del PanelLateral corre solo mientras esta pantalla se está viendo:
        // el CardLayout oculta las pantallas que no están al frente, y también se oculta al cerrar la ventana.
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (isShowing()) {
                    panelLateral.iniciar();
                } else {
                    panelLateral.detener();
                }
            }
        });
    }

    /** Reenvía el aviso a los tres paneles (cada uno se redibuja en el EDT). */
    @Override
    public void onEstadoCambiado() {
        for (ObservadorPartida panel : paneles()) {
            panel.onEstadoCambiado();
        }
    }

    /**
     * Reenvía el cambio de fase a los tres paneles.
     *
     * @param fasePartida fase nueva
     */
    @Override
    public void onFaseCambiada(FasePartida fasePartida) {
        for (ObservadorPartida panel : paneles()) {
            panel.onFaseCambiada(fasePartida);
        }
    }

    /**
     * Reenvía el inicio de turno a los tres paneles.
     *
     * @param nombreJugador jugador que empieza el turno
     */
    @Override
    public void onTurnoIniciado(String nombreJugador) {
        for (ObservadorPartida panel : paneles()) {
            panel.onTurnoIniciado(nombreJugador);
        }
    }

    /**
     * Reenvía el fin de la partida a los tres paneles.
     *
     * @param resultadoPartida cómo terminó
     */
    @Override
    public void onPartidaTerminada(ResultadoPartida resultadoPartida) {
        for (ObservadorPartida panel : paneles()) {
            panel.onPartidaTerminada(resultadoPartida);
        }
    }

    /**
     * Tablero de la pantalla (por ejemplo, para que la aplicación le dé el foco al mostrarla).
     *
     * @return el panel del tablero
     */
    public PanelTablero getPanelTablero() {
        return panelTablero;
    }

    /**
     * Panel lateral de la pantalla (HUD y botones).
     *
     * @return el panel lateral
     */
    public PanelLateral getPanelLateral() {
        return panelLateral;
    }

    /**
     * Historial de la partida (abajo, plegable).
     *
     * @return el panel del historial
     */
    public PanelHistorial getPanelHistorial() {
        return panelHistorial;
    }

    /** Los tres paneles que reciben los avisos, como {@code ObservadorPartida} (polimorfismo). */
    private List<ObservadorPartida> paneles() {
        return List.of(panelTablero, panelLateral, panelHistorial);
    }
}
