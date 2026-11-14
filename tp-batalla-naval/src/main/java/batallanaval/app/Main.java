package batallanaval.app;

import batallanaval.excepciones.PersistenciaException;
import batallanaval.persistencia.RutasApp;

import javax.swing.SwingUtilities;

/**
 * Punto de entrada de Batalla Naval Táctica: crea las carpetas de datos y le delega todo a
 * {@link ControladorAplicacion}.
 */
public final class Main {

    /** Título de la ventana principal. */
    public static final String TITULO = "Batalla Naval Táctica";

    private Main() {
        // Clase de arranque: no se instancia.
    }

    /**
     * Arranca la aplicación. Swing no es thread-safe, así que todo lo de la interfaz se arma en el
     * hilo de eventos (EDT) con {@link SwingUtilities#invokeLater(Runnable)}.
     *
     * @param args argumentos de línea de comandos (no se usan)
     */
    public static void main(String[] args) {
        try {
            RutasApp.crearCarpetas();
        } catch (PersistenciaException e) {
            // Sin carpetas no se puede guardar, pero se puede jugar igual: el error aparece al guardar.
            System.err.println("No se pudieron crear las carpetas de datos: " + e.getMessage());
        }
        SwingUtilities.invokeLater(() -> new ControladorAplicacion().iniciar());
    }
}