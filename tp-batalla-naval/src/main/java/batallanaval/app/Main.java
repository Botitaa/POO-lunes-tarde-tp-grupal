package batallanaval.app;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * Punto de entrada de Batalla Naval Táctica.
 *
 * <p>Por ahora solo abre la ventana principal vacía. Más adelante, en este orden:
 * crea las carpetas de datos, configura el log, instala el manejador de errores
 * y le delega todo a {@code ControladorAplicacion}.</p>
 */
public final class Main {

    /** Título de la ventana principal. */
    public static final String TITULO = "Batalla Naval Táctica";

    private static final int ANCHO = 1000;
    private static final int ALTO = 700;

    private Main() {
        // Clase de arranque: no se instancia.
    }

    /**
     * Arranca la aplicación. Swing no es thread-safe, así que la ventana se crea
     * en el hilo de eventos (EDT) con {@link SwingUtilities#invokeLater(Runnable)}.
     *
     * @param args argumentos de línea de comandos (no se usan)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::crearVentana);
    }

    private static void crearVentana() {
        JFrame ventana = new JFrame(TITULO);
        ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ventana.setSize(ANCHO, ALTO);
        ventana.setLocationRelativeTo(null);
        ventana.add(new JLabel(TITULO, SwingConstants.CENTER));
        ventana.setVisible(true);
    }
}
