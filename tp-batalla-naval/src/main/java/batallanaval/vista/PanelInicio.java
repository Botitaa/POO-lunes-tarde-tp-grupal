package batallanaval.vista;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.Serial;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;

/**
 * Menú principal: Nueva partida, Cargar partida, Estadísticas, Reglas y Salir.
 * <p>
 * Los botones no deciden nada de la partida: solo le piden a la
 * {@link VentanaPrincipal} que cambie de pantalla.
 * <p>
 * Tiene su propio hilo ({@link PanelConHilo}): cada {@value #INTERVALO_MS} ms avanza un barquito
 * por el mar debajo del título y actualiza la hora. El hilo lo inicia y lo detiene la
 * {@link VentanaPrincipal} cuando muestra u oculta esta pantalla.
 */
public class PanelInicio extends PanelConHilo {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Cada cuántos milisegundos se mueve el barco y se actualiza la hora. */
    static final int INTERVALO_MS = 300;

    /** Cantidad de caracteres de la franja de mar. */
    static final int ANCHO_MAR = 30;

    /** Dibujo del barco que cruza el mar. */
    static final String BARCO = "<=>";

    private static final Dimension TAMANIO_BOTON = new Dimension(260, 44);
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final VentanaPrincipal ventana;

    // Franja de mar animada y reloj: los cambia actualizar(), que corre en el EDT
    private final JLabel etiquetaMar = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel etiquetaReloj = new JLabel(" ", SwingConstants.CENTER);

    // Columna del mar en la que está el barco
    private int posicionBarco = 0;

    /**
     * @param ventana ventana desde la que se navega a las otras pantallas
     */
    public PanelInicio(VentanaPrincipal ventana) {
        this.ventana = ventana;
        setLayout(new GridBagLayout());
        armarContenido();
    }

    private void armarContenido() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = GridBagConstraints.RELATIVE;
        c.insets = new Insets(6, 0, 6, 0);

        JLabel titulo = new JLabel("Batalla Naval Táctica", SwingConstants.CENTER);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 36f));
        c.insets = new Insets(0, 0, 4, 0);
        add(titulo, c);

        // Mar con letra de ancho fijo, para que el barco avance parejo y la franja no "baile"
        etiquetaMar.setName("etiquetaMar");
        etiquetaMar.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
        etiquetaMar.setText(armarMar(posicionBarco));
        c.insets = new Insets(0, 0, 4, 0);
        add(etiquetaMar, c);

        JLabel subtitulo = new JLabel("Dos jugadores, una computadora, mucha niebla", SwingConstants.CENTER);
        subtitulo.setFont(subtitulo.getFont().deriveFont(Font.ITALIC, 14f));
        c.insets = new Insets(0, 0, 6, 0);
        add(subtitulo, c);

        etiquetaReloj.setName("etiquetaReloj");
        etiquetaReloj.setText(LocalTime.now().format(FORMATO_HORA));
        c.insets = new Insets(0, 0, 24, 0);
        add(etiquetaReloj, c);

        c.insets = new Insets(6, 0, 6, 0);
        add(crearBoton("Nueva partida", () -> ventana.mostrar(Pantalla.NUEVA_PARTIDA)), c);
        add(crearBoton("Cargar partida", () -> ventana.mostrar(Pantalla.CARGAR)), c);
        add(crearBoton("Estadísticas", () -> ventana.mostrar(Pantalla.ESTADISTICAS)), c);
        add(crearBoton("Reglas", () -> ventana.mostrar(Pantalla.REGLAS)), c);
        add(crearBoton("Salir", this::confirmarSalida), c);
    }

    private JButton crearBoton(String texto, Runnable accion) {
        JButton boton = new JButton(texto);
        boton.setPreferredSize(TAMANIO_BOTON);
        // Si la acción falla (por ejemplo, una pantalla que todavía no está registrada),
        // se avisa en la barra de estado y el menú sigue andando.
        boton.addActionListener(EjecutorAcciones.alTocar(accion, ventana::mostrarError));
        return boton;
    }

    private void confirmarSalida() {
        int opcion = JOptionPane.showConfirmDialog(
                ventana,
                "¿Seguro que querés salir?",
                "Salir",
                JOptionPane.YES_NO_OPTION);
        if (opcion == JOptionPane.YES_OPTION) {
            ventana.dispose();
        }
    }

    // Hilo propio (PanelConHilo)

    /**
     * Paso del hilo propio: avanza el barco una columna (al llegar al final vuelve a empezar)
     * y actualiza la hora. Lo ejecuta el EDT cada {@value #INTERVALO_MS} ms, por eso puede
     * cambiar las etiquetas sin problemas.
     */
    @Override
    protected void actualizar() {
        posicionBarco = (posicionBarco + 1) % (ANCHO_MAR - BARCO.length() + 1);
        etiquetaMar.setText(armarMar(posicionBarco));
        etiquetaReloj.setText(LocalTime.now().format(FORMATO_HORA));
    }

    /** @return {@value #INTERVALO_MS}: el barco avanza unas tres veces por segundo */
    @Override
    protected int getIntervaloMs() {
        return INTERVALO_MS;
    }

    /**
     * Arma la franja de mar con el barco en la columna indicada: olas ({@code ~}) antes,
     * el barco y olas después, siempre de {@value #ANCHO_MAR} caracteres.
     *
     * @param posicion columna donde empieza el barco (de 0 a ANCHO_MAR - largo del barco)
     * @return la franja de mar como texto
     */
    static String armarMar(int posicion) {
        String mar = "";
        for (int i = 0; i < posicion; i++) {
            mar += "~";
        }
        mar += BARCO;
        while (mar.length() < ANCHO_MAR) {
            mar += "~";
        }
        return mar;
    }
}