package batallanaval.vista;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.Serial;
import java.util.Objects;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JLabel;
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
 * El panel no crea la partida: cuando los nombres son válidos y el jugador toca
 * "Comenzar", ejecuta el {@link Runnable} que recibió; quien lo creó (en la aplicación,
 * {@code ControladorAplicacion}) lee entonces los nombres con
 * {@link #getNombreJugador1()} y {@link #getNombreJugador2()}. Los errores se muestran
 * en la barra de estado de la {@link VentanaPrincipal}, no por consola.
 * <p>
 * Tiene su propio hilo ({@link PanelConHilo}): cada {@value #INTERVALO_MS} ms muestra una vista
 * previa con los nombres escritos ("Bota vs Santi") o el motivo por el que todavía no se puede
 * comenzar. El hilo lo inicia y lo detiene la {@link VentanaPrincipal}.
 */
public final class PanelNuevaPartida extends PanelConHilo {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final int COLUMNAS_CAMPO = 20;
    private static final Dimension TAMANIO_BOTON = new Dimension(200, 40);

    /** Cada cuántos milisegundos se actualiza la vista previa. */
    static final int INTERVALO_MS = 300;

    private final VentanaPrincipal ventana;
    private final transient Runnable alComenzar;
    private final JTextField campoJugador1 = new JTextField(COLUMNAS_CAMPO);
    private final JTextField campoJugador2 = new JTextField(COLUMNAS_CAMPO);
    private final Random azar = new Random();
    private final JLabel vistaPrevia = new JLabel(" ", SwingConstants.CENTER);

    /**
     * @param ventana        ventana desde la que se navega y donde se muestran los errores
     * @param nombreJugador1 nombre con el que se precarga el Jugador 1 (si es {@code null}, queda vacío)
     * @param nombreJugador2 nombre con el que se precarga el Jugador 2 (si es {@code null}, queda vacío)
     * @param alComenzar     se ejecuta cuando el jugador toca "Comenzar" y los nombres son válidos
     * @throws NullPointerException si {@code ventana} o {@code alComenzar} son {@code null}
     */
    public PanelNuevaPartida(VentanaPrincipal ventana, String nombreJugador1, String nombreJugador2,
                             Runnable alComenzar) {
        this.ventana = Objects.requireNonNull(ventana, "ventana");
        this.alComenzar = Objects.requireNonNull(alComenzar, "alComenzar");
        setLayout(new GridBagLayout());
        armarContenido();
        campoJugador1.setText(Objects.toString(nombreJugador1, ""));
        campoJugador2.setText(Objects.toString(nombreJugador2, ""));
    }

    /** @return el nombre del Jugador 1, sin espacios en los bordes */
    public String getNombreJugador1() {
        return campoJugador1.getText().trim();
    }

    /** @return el nombre del Jugador 2, sin espacios en los bordes */
    public String getNombreJugador2() {
        return campoJugador2.getText().trim();
    }

    /**
     * Valida los nombres de los dos jugadores. Es una función pura (no toca la interfaz),
     * así se puede testear sin abrir ninguna ventana.
     *
     * @param nombre1 nombre del Jugador 1
     * @param nombre2 nombre del Jugador 2
     * @return el mensaje de error para el jugador, o {@code null} si los nombres son válidos
     */
    static String validarNombres(String nombre1, String nombre2) {
        String n1 = nombre1 == null ? "" : nombre1.trim();
        String n2 = nombre2 == null ? "" : nombre2.trim();

        if (n1.isEmpty()) {
            return "El nombre del Jugador 1 no puede estar vacío";
        }
        if (n2.isEmpty()) {
            return "El nombre del Jugador 2 no puede estar vacío";
        }
        if (n1.contains(";")) {
            return "El nombre del Jugador 1 no puede tener el carácter ';'";
        }
        if (n2.contains(";")) {
            return "El nombre del Jugador 2 no puede tener el carácter ';'";
        }
        if (n1.equalsIgnoreCase(n2)) {
            return "Los dos jugadores tienen que tener nombres distintos";
        }
        return null;
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
        campoJugador1.addActionListener(EjecutorAcciones.alTocar(this::comenzar, ventana::mostrarError));
        campoJugador2.addActionListener(EjecutorAcciones.alTocar(this::comenzar, ventana::mostrarError));

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

        // Vista previa de los nombres: la va cambiando el hilo propio (actualizar)
        c.gridy = 6;
        c.insets = new Insets(18, 0, 6, 0);
        vistaPrevia.setName("vistaPrevia");
        vistaPrevia.setFont(vistaPrevia.getFont().deriveFont(Font.ITALIC, 15f));
        add(vistaPrevia, c);
    }

    private JButton crearBoton(String texto, Runnable accion) {
        JButton boton = new JButton(texto);
        boton.setPreferredSize(TAMANIO_BOTON);
        // Si la acción falla (por ejemplo, al crear la partida en alComenzar), se avisa en la
        // barra de estado y la pantalla sigue andando para que el jugador lo intente de nuevo.
        boton.addActionListener(EjecutorAcciones.alTocar(accion, ventana::mostrarError));
        return boton;
    }

    /** Valida los nombres y, si están bien, ejecuta la acción que recibió el panel. */
    private void comenzar() {
        String error = validarNombres(getNombreJugador1(), getNombreJugador2());
        if (error != null) {
            ventana.mostrarError(error);
            return;
        }
        alComenzar.run();
    }

    // Hilo propio (PanelConHilo)

    /**
     * Paso del hilo propio: muestra "Jugador 1 vs Jugador 2" si los nombres son válidos o, si no,
     * el motivo. Lo ejecuta el EDT cada {@value #INTERVALO_MS} ms.
     */
    @Override
    protected void actualizar() {
        String error = validarNombres(getNombreJugador1(), getNombreJugador2());
        if (error == null) {
            vistaPrevia.setText(getNombreJugador1() + " vs " + getNombreJugador2());
        } else {
            vistaPrevia.setText(error);
        }
    }

    /** @return {@value #INTERVALO_MS} */
    @Override
    protected int getIntervaloMs() {
        return INTERVALO_MS;
    }

    /** Con 50% de probabilidad intercambia los nombres y avisa quién quedó como Jugador 1. */
    private void sortear() {
        String error = validarNombres(getNombreJugador1(), getNombreJugador2());
        if (error != null) {
            ventana.mostrarError(error);
            return;
        }
        if (azar.nextBoolean()) {
            String primero = campoJugador1.getText();
            campoJugador1.setText(campoJugador2.getText());
            campoJugador2.setText(primero);
        }
        ventana.mostrarMensaje("Sorteo: " + getNombreJugador1() + " es el Jugador 1");
    }
}