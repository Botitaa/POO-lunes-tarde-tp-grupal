package batallanaval.vista;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Única ventana del juego. Contiene un {@link CardLayout} donde se apilan las
 * pantallas (una por valor de {@link Pantalla}) y una barra de estado abajo para
 * avisos y errores.
 * <p>
 * La ventana no sabe nada de las reglas ni de la partida: solo muestra la pantalla
 * que se le pide. Quien arma las pantallas y las registra es
 * {@code ControladorAplicacion}.
 * <p>
 * Todos los métodos se tienen que llamar desde el EDT
 * ({@code SwingUtilities.invokeLater}).
 */
public class VentanaPrincipal extends JFrame {

    private static final String TITULO = "Batalla Naval Táctica";
    private static final Color COLOR_ERROR = new Color(180, 30, 30);

    private final CardLayout disposicion = new CardLayout();
    private final JPanel contenedor = new JPanel(disposicion);
    private final Map<Pantalla, JPanel> pantallas = new EnumMap<>(Pantalla.class);
    private final JLabel barraEstado = new JLabel(" ");
    private final Color colorMensaje;
    private Pantalla pantallaActual;

    /** Crea la ventana vacía: hay que registrar las pantallas y después llamar a {@code setVisible(true)}. */
    public VentanaPrincipal() {
        super(TITULO);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 650));
        setSize(1100, 750);
        setLocationRelativeTo(null);

        barraEstado.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        colorMensaje = barraEstado.getForeground();

        setLayout(new BorderLayout());
        add(contenedor, BorderLayout.CENTER);
        add(barraEstado, BorderLayout.SOUTH);
    }

    /**
     * Registra el panel que se va a mostrar para una pantalla.
     *
     * @param pantalla pantalla a la que corresponde el panel
     * @param panel    panel a mostrar
     * @throws NullPointerException si algún argumento es {@code null}
     * @throws IllegalStateException si esa pantalla ya tenía un panel registrado
     */
    public void agregarPantalla(Pantalla pantalla, JPanel panel) {
        Objects.requireNonNull(pantalla, "pantalla");
        Objects.requireNonNull(panel, "panel");
        if (pantallas.containsKey(pantalla)) {
            throw new IllegalStateException("La pantalla " + pantalla + " ya está registrada");
        }
        pantallas.put(pantalla, panel);
        contenedor.add(panel, pantalla.name());
    }

    /**
     * Muestra una pantalla y limpia la barra de estado.
     *
     * @param pantalla pantalla a mostrar
     * @throws NullPointerException  si {@code pantalla} es {@code null}
     * @throws IllegalStateException si todavía no se registró un panel para esa pantalla
     */
    public void mostrar(Pantalla pantalla) {
        Objects.requireNonNull(pantalla, "pantalla");
        if (!pantallas.containsKey(pantalla)) {
            throw new IllegalStateException("No hay un panel registrado para " + pantalla);
        }
        disposicion.show(contenedor, pantalla.name());
        pantallaActual = pantalla;
        mostrarMensaje(" ");
    }

    /** @return la pantalla que se está mostrando, o {@code null} si todavía no se mostró ninguna */
    public Pantalla getPantallaActual() {
        return pantallaActual;
    }

    /**
     * Muestra un error para el jugador en la barra de estado (en rojo).
     *
     * @param texto mensaje a mostrar
     */
    public void mostrarError(String texto) {
        barraEstado.setForeground(COLOR_ERROR);
        barraEstado.setText(texto);
    }

    /**
     * Muestra un aviso informativo en la barra de estado.
     *
     * @param texto mensaje a mostrar
     */
    public void mostrarMensaje(String texto) {
        barraEstado.setForeground(colorMensaje);
        barraEstado.setText(texto);
    }
}