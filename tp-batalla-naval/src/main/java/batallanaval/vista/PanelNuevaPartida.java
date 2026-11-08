package batallanaval.vista;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.Serial;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.BiConsumer;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * Pantalla donde los dos jugadores escriben sus nombres antes de empezar una partida.
 * <p>
 * Los nombres tienen que ser distintos, no estar vacíos y no tener {@code ;} (ese
 * carácter es el separador del archivo de guardado y del CSV de estadísticas). Vienen
 * precargados con los últimos nombres usados y hay un botón para sortear quién es el
 * Jugador 1.
 * <p>
 * El panel no crea la partida ni lee la configuración: recibe los nombres iniciales por
 * constructor y, al confirmar, se los entrega a quien lo creó mediante {@code alIniciar}
 * (en la aplicación, {@code ControladorAplicacion}). Los errores se muestran en la barra
 * de estado de la {@link VentanaPrincipal}, no por consola.
 */
public final class PanelNuevaPartida extends JPanel {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final int COLUMNAS_CAMPO = 20;
    private static final Dimension TAMANIO_BOTON = new Dimension(200, 40);

    private final VentanaPrincipal ventana;
    private final transient BiConsumer<String, String> alIniciar;
    private final JTextField campoJugador1 = new JTextField(COLUMNAS_CAMPO);
    private final JTextField campoJugador2 = new JTextField(COLUMNAS_CAMPO);
    private final Random azar = new Random();

    /**
     * @param ventana          ventana desde la que se navega y donde se muestran los errores
     * @param nombreJugador1   nombre con el que se precarga el Jugador 1 (si es {@code null}, queda vacío)
     * @param nombreJugador2   nombre con el que se precarga el Jugador 2 (si es {@code null}, queda vacío)
     * @param alIniciar        acción que recibe (nombre del J1, nombre del J2), ya validados y sin
     *                         espacios en los bordes, cuando el jugador toca "Comenzar"
     * @throws NullPointerException si {@code ventana} o {@code alIniciar} son {@code null}
     */
    public PanelNuevaPartida(VentanaPrincipal ventana, String nombreJugador1, String nombreJugador2,
                             BiConsumer<String, String> alIniciar) {
        this.ventana = Objects.requireNonNull(ventana, "ventana");
        this.alIniciar = Objects.requireNonNull(alIniciar, "alIniciar");
        setLayout(new GridBagLayout());
        armarContenido();
        precargarNombres(nombreJugador1, nombreJugador2);
    }

    /**
     * Reemplaza el contenido de los dos campos. Sirve para volver a precargar los últimos
     * nombres cuando se vuelve a entrar a esta pantalla.
     *
     * @param nombreJugador1 nombre del Jugador 1 (si es {@code null}, el campo queda vacío)
     * @param nombreJugador2 nombre del Jugador 2 (si es {@code null}, el campo queda vacío)
     */
    public void precargarNombres(String nombreJugador1, String nombreJugador2) {
        campoJugador1.setText(Objects.toString(nombreJugador1, ""));
        campoJugador2.setText(Objects.toString(nombreJugador2, ""));
    }

    /**
     * Valida los nombres de los dos jugadores. Es una función pura (no toca la interfaz),
     * así se puede testear sin abrir ninguna ventana.
     *
     * @param nombre1 nombre del Jugador 1
     * @param nombre2 nombre del Jugador 2
     * @return el mensaje de error para el jugador, o vacío si los nombres son válidos
     */
    static Optional<String> validarNombres(String nombre1, String nombre2) {
        String n1 = nombre1 == null ? "" : nombre1.trim();
        String n2 = nombre2 == null ? "" : nombre2.trim();

        if (n1.isEmpty()) {
            return Optional.of("El nombre del Jugador 1 no puede estar vacío");
        }
        if (n2.isEmpty()) {
            return Optional.of("El nombre del Jugador 2 no puede estar vacío");
        }
        if (n1.contains(";")) {
            return Optional.of("El nombre del Jugador 1 no puede tener el carácter ';'");
        }
        if (n2.contains(";")) {
            return Optional.of("El nombre del Jugador 2 no puede tener el carácter ';'");
        }
        if (n1.equalsIgnoreCase(n2)) {
            return Optional.of("Los dos jugadores tienen que tener nombres distintos");
        }
        return Optional.empty();
    }

    private void armarContenido() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = GridBagConstraints.RELATIVE;
        c.gridwidth = 2;
        c.insets = new Insets(0, 0, 24, 0);

        JLabel titulo = new JLabel("Nueva partida", SwingConstants.CENTER);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 32f));
        add(titulo, c);

        c.gridwidth = 1;
        c.insets = new Insets(6, 0, 6, 10);
        c.anchor = GridBagConstraints.EAST;
        add(new JLabel("Jugador 1:"), c);
        c.gridx = 1;
        c.gridy = 1;
        c.insets = new Insets(6, 0, 6, 0);
        c.anchor = GridBagConstraints.WEST;
        add(campoJugador1, c);

        c.gridx = 0;
        c.gridy = 2;
        c.insets = new Insets(6, 0, 6, 10);
        c.anchor = GridBagConstraints.EAST;
        add(new JLabel("Jugador 2:"), c);
        c.gridx = 1;
        c.insets = new Insets(6, 0, 6, 0);
        c.anchor = GridBagConstraints.WEST;
        add(campoJugador2, c);

        // Enter en cualquiera de los dos campos equivale a tocar "Comenzar".
        campoJugador1.addActionListener(e -> comenzar());
        campoJugador2.addActionListener(e -> comenzar());

        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(24, 0, 6, 0);
        add(crearBoton("Sortear quién es J1", this::sortear), c);

        c.gridy = 4;
        c.insets = new Insets(6, 0, 6, 0);
        add(crearBoton("Comenzar", this::comenzar), c);

        c.gridy = 5;
        add(crearBoton("Volver", () -> ventana.mostrar(Pantalla.INICIO)), c);
    }

    private JButton crearBoton(String texto, Runnable accion) {
        JButton boton = new JButton(texto);
        boton.setPreferredSize(TAMANIO_BOTON);
        boton.addActionListener(e -> accion.run());
        return boton;
    }

    /** Valida los nombres y, si están bien, se los entrega a quien arma la partida. */
    private void comenzar() {
        String n1 = campoJugador1.getText().trim();
        String n2 = campoJugador2.getText().trim();
        Optional<String> error = validarNombres(n1, n2);
        if (error.isPresent()) {
            ventana.mostrarError(error.get());
            return;
        }
        alIniciar.accept(n1, n2);
    }

    /** Con 50% de probabilidad intercambia los nombres y avisa quién quedó como Jugador 1. */
    private void sortear() {
        Optional<String> error = validarNombres(campoJugador1.getText(), campoJugador2.getText());
        if (error.isPresent()) {
            ventana.mostrarError(error.get());
            return;
        }
        if (azar.nextBoolean()) {
            String primero = campoJugador1.getText();
            campoJugador1.setText(campoJugador2.getText());
            campoJugador2.setText(primero);
        }
        ventana.mostrarMensaje("Sorteo: " + campoJugador1.getText().trim() + " es el Jugador 1");
    }
}
