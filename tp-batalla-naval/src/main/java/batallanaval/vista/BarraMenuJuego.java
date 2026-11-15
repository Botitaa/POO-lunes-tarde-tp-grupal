package batallanaval.vista;

import java.awt.KeyboardFocusManager;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.Serial;
import java.util.Objects;
import java.util.function.BooleanSupplier;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import batallanaval.controlador.FasePartida;
import batallanaval.controlador.ObservadorPartida;
import batallanaval.controlador.PartidaConsulta;
import batallanaval.controlador.ResultadoPartida;

/**
 * Barra de menú de la ventana principal:
 * <ul>
 *   <li><b>Partida</b> → Guardar (Ctrl+S), Cargar (Ctrl+O), Abandonar.</li>
 *   <li><b>Ayuda</b> → Reglas (F1).</li>
 * </ul>
 * <p>
 * <b>La barra no decide nada.</b> Como el resto de las pantallas, cuando se elige una opción
 * ejecuta la acción que recibió por constructor; quien la arma (en la aplicación,
 * {@code ControladorAplicacion}) es el que guarda en un hilo propio, muestra la pantalla de
 * carga, termina la partida o muestra las reglas.
 * <p>
 * Lo único que hace por su cuenta es <b>pedir confirmación antes de abandonar</b>, porque
 * abandonar no se puede deshacer: si el jugador dice que no, la acción no se ejecuta.
 * <p>
 * Qué opciones están habilitadas se lo pregunta a la partida, nunca lo calcula:
 * <ul>
 *   <li>Guardar, solo si {@link PartidaConsulta#puedeGuardarse()} es {@code true}
 *       (partida en juego, al inicio de un turno o en el blackout).</li>
 *   <li>Abandonar, solo si hay una partida que todavía no terminó.</li>
 *   <li>Cargar y Reglas, siempre.</li>
 * </ul>
 * Implementa {@link ObservadorPartida}: si se la registra en la partida, cada aviso vuelve a
 * leer esos dos datos (en el EDT). Cuando se crea o se carga otra partida, hay que pasársela
 * con {@link #mostrarPartida(PartidaConsulta)}.
 */
public final class BarraMenuJuego extends JMenuBar implements ObservadorPartida {

    @Serial
    private static final long serialVersionUID = 1L;

    static final String PREGUNTA_ABANDONAR =
            "¿Seguro que querés abandonar la partida? Lo que no hayas guardado se pierde.";

    private final transient Runnable alAbandonar;
    private final transient BooleanSupplier confirmarAbandono;

    private final JMenuItem itemGuardar = new JMenuItem("Guardar partida");
    private final JMenuItem itemCargar = new JMenuItem("Cargar partida");
    private final JMenuItem itemAbandonar = new JMenuItem("Abandonar partida");
    private final JMenuItem itemReglas = new JMenuItem("Reglas");

    // Partida que se muestra; null mientras no hay ninguna (por ejemplo, en el menú de inicio).
    private transient PartidaConsulta partida;

    /**
     * Crea la barra. Al abandonar, la confirmación se pregunta con un diálogo
     * ({@link JOptionPane}). Arranca sin partida: Guardar y Abandonar deshabilitados.
     *
     * @param alGuardar   se ejecuta al elegir Partida → Guardar
     * @param alCargar    se ejecuta al elegir Partida → Cargar
     * @param alAbandonar se ejecuta al elegir Partida → Abandonar, solo si el jugador lo confirma
     * @param alVerReglas se ejecuta al elegir Ayuda → Reglas
     * @throws NullPointerException si alguna acción es {@code null}
     */
    public BarraMenuJuego(Runnable alGuardar, Runnable alCargar, Runnable alAbandonar, Runnable alVerReglas) {
        this(alGuardar, alCargar, alAbandonar, alVerReglas, BarraMenuJuego::preguntarSiAbandona);
    }

    /**
     * Igual que el constructor público, pero recibe cómo se confirma el abandono. Es de paquete
     * a propósito: lo usan los tests para no abrir un diálogo (un {@link JOptionPane} se queda
     * esperando un clic y, sin pantalla, ni siquiera se puede crear).
     *
     * @param confirmarAbandono devuelve {@code true} si el jugador confirma que quiere abandonar
     */
    BarraMenuJuego(Runnable alGuardar, Runnable alCargar, Runnable alAbandonar, Runnable alVerReglas,
                   BooleanSupplier confirmarAbandono) {
        Objects.requireNonNull(alGuardar, "alGuardar");
        Objects.requireNonNull(alCargar, "alCargar");
        Objects.requireNonNull(alVerReglas, "alVerReglas");
        this.alAbandonar = Objects.requireNonNull(alAbandonar, "alAbandonar");
        this.confirmarAbandono = Objects.requireNonNull(confirmarAbandono, "confirmarAbandono");

        add(armarMenuPartida(alGuardar, alCargar));
        add(armarMenuAyuda(alVerReglas));
        actualizar();
    }

    /**
     * Cambia la partida de la que se leen las opciones habilitadas y refresca la barra.
     * Se llama al crear o cargar una partida, y con {@code null} al volver al inicio.
     * No registra la barra como observador: eso lo hace quien la arma.
     *
     * @param partida partida actual, o {@code null} si no hay ninguna
     */
    public void mostrarPartida(PartidaConsulta partida) {
        this.partida = partida;
        actualizar();
    }

    /**
     * Vuelve a preguntarle a la partida qué se puede hacer y habilita o deshabilita
     * Guardar y Abandonar. Se tiene que llamar desde el EDT.
     */
    public void actualizar() {
        boolean hayPartidaEnCurso = partida != null && !partida.estaTerminada();
        itemGuardar.setEnabled(hayPartidaEnCurso && partida.puedeGuardarse());
        itemAbandonar.setEnabled(hayPartidaEnCurso);
    }

    // Avisos de la partida (ObservadorPartida). Pueden llegar desde otro hilo: se pasa al EDT.

    /** Cambió algo de la partida (por ejemplo, se empezó a planificar una ruta): se refresca. */
    @Override
    public void onEstadoCambiado() {
        actualizarEnEdt();
    }

    /**
     * Cambió la fase (por ejemplo, terminó el despliegue): se refresca.
     *
     * @param fasePartida fase nueva
     */
    @Override
    public void onFaseCambiada(FasePartida fasePartida) {
        actualizarEnEdt();
    }

    /**
     * Empezó un turno, que es cuando se vuelve a poder guardar: se refresca.
     *
     * @param nombreJugador jugador que empieza el turno
     */
    @Override
    public void onTurnoIniciado(String nombreJugador) {
        actualizarEnEdt();
    }

    /**
     * La partida terminó: ya no se puede guardar ni abandonar.
     *
     * @param resultadoPartida cómo terminó
     */
    @Override
    public void onPartidaTerminada(ResultadoPartida resultadoPartida) {
        actualizarEnEdt();
    }

    private void actualizarEnEdt() {
        SwingUtilities.invokeLater(this::actualizar);
    }

    // Armado de los menús (una sola vez, en el constructor)

    private JMenu armarMenuPartida(Runnable alGuardar, Runnable alCargar) {
        JMenu menu = new JMenu("Partida");
        menu.setMnemonic(KeyEvent.VK_P);
        configurar(itemGuardar, "itemGuardar", KeyEvent.VK_G,
                KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), alGuardar);
        configurar(itemCargar, "itemCargar", KeyEvent.VK_C,
                KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK), alCargar);
        configurar(itemAbandonar, "itemAbandonar", KeyEvent.VK_A, null, this::abandonarSiConfirma);
        menu.add(itemGuardar);
        menu.add(itemCargar);
        menu.addSeparator(); // Abandonar queda separado: es la única opción que no se puede deshacer
        menu.add(itemAbandonar);
        return menu;
    }

    private JMenu armarMenuAyuda(Runnable alVerReglas) {
        JMenu menu = new JMenu("Ayuda");
        menu.setMnemonic(KeyEvent.VK_Y);
        configurar(itemReglas, "itemReglas", KeyEvent.VK_R,
                KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), alVerReglas);
        menu.add(itemReglas);
        return menu;
    }

    /**
     * Le pone nombre (para encontrarlo en los tests), letra subrayada, atajo de teclado
     * (si tiene) y la acción a un ítem del menú.
     */
    private static void configurar(JMenuItem item, String nombre, int mnemonico, KeyStroke atajo,
                                   Runnable accion) {
        item.setName(nombre);
        item.setMnemonic(mnemonico);
        if (atajo != null) {
            item.setAccelerator(atajo);
        }
        item.addActionListener(e -> accion.run());
    }

    // Abandonar

    /** Pide confirmación y, solo si el jugador dice que sí, ejecuta la acción de abandonar. */
    private void abandonarSiConfirma() {
        if (confirmarAbandono.getAsBoolean()) {
            alAbandonar.run();
        }
    }

    /**
     * Confirmación real: un diálogo Sí/No centrado en la ventana activa. Solo cuenta como
     * confirmado el "Sí": si se toca "No" o se cierra el diálogo con la X
     * ({@code CLOSED_OPTION}), no se abandona.
     */
    private static boolean preguntarSiAbandona() {
        int opcion = JOptionPane.showConfirmDialog(
                KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow(),
                PREGUNTA_ABANDONAR,
                "Abandonar partida",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        return opcion == JOptionPane.YES_OPTION;
    }
}
