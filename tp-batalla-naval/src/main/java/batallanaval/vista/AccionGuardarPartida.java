package batallanaval.vista;

import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.swing.JOptionPane;

import batallanaval.controlador.GestorPartida;
import batallanaval.excepciones.PersistenciaException;
import batallanaval.persistencia.RepositorioPartidas;

/**
 * Acción de <b>guardar la partida desde la interfaz</b>: le pide un nombre al jugador, avisa si
 * ya existe un guardado con ese nombre (para no pisarlo sin querer) y le pide al repositorio que
 * guarde. Es un {@link Runnable} para poder conectarla a donde se guarda: el ítem
 * Partida → Guardar de la {@link BarraMenuJuego} y el botón "Guardar partida" del
 * {@link PanelBlackout}.
 * <p>
 * Lo que pasa en cada caso:
 * <ul>
 *   <li>Si no hay partida, o la partida no se puede guardar ahora
 *       ({@code puedeGuardarse()} es {@code false}), muestra el error y ni siquiera pide nombre.</li>
 *   <li>Si el jugador cancela el diálogo, no pasa nada.</li>
 *   <li>Si el nombre ya existe, pregunta si se pisa; si dice que no, no se guarda.</li>
 *   <li>Si el repositorio lanza {@link PersistenciaException} (nombre inválido, error de
 *       escritura...), el mensaje de la excepción va a {@code alError} (en la aplicación,
 *       {@code ventana::mostrarError}) y el juego sigue.</li>
 *   <li>Si guarda bien, avisa con {@code alAviso} (en la aplicación,
 *       {@code ventana::mostrarMensaje}).</li>
 * </ul>
 * <b>No decide reglas ni sabe escribir archivos</b>: valida el nombre y escribe el
 * {@link RepositorioPartidas}, y qué partidas se pueden guardar lo dice la partida.
 * Corre en el hilo que la llame (el EDT, desde un botón o menú); como el archivo es chico no
 * congela la ventana, pero se puede envolver en un hilo propio sin cambiarla.
 */
public final class AccionGuardarPartida implements Runnable {

    /** Nombre que aparece escrito de entrada en el diálogo. */
    static final String NOMBRE_SUGERIDO = "partida1";

    static final String PEDIDO_NOMBRE =
            "Nombre de la partida (letras, números, - y _; hasta 40 caracteres):";
    static final String MENSAJE_SIN_PARTIDA = "No hay una partida para guardar.";
    static final String MENSAJE_NO_SE_PUEDE_GUARDAR =
            "La partida solo se puede guardar al inicio de un turno o en la pantalla de blackout.";

    private final RepositorioPartidas repositorio;
    private final Supplier<GestorPartida> partidaActual;
    private final Consumer<String> alError;
    private final Consumer<String> alAviso;
    private final Supplier<String> pedirNombre;
    private final Predicate<String> confirmarSobrescribir;

    /**
     * Crea la acción con diálogos reales ({@link JOptionPane}).
     *
     * @param repositorio   donde se guarda la partida
     * @param partidaActual devuelve la partida en curso, o {@code null} si no hay ninguna (se
     *                      pregunta en cada guardado, porque la partida cambia al cargar otra)
     * @param alError       qué hacer con el mensaje cuando falla el guardado
     * @param alAviso       qué hacer con el mensaje cuando se guarda bien
     * @throws NullPointerException si algún argumento es {@code null}
     */
    public AccionGuardarPartida(RepositorioPartidas repositorio, Supplier<GestorPartida> partidaActual,
                                Consumer<String> alError, Consumer<String> alAviso) {
        this(repositorio, partidaActual, alError, alAviso,
                AccionGuardarPartida::pedirNombreConDialogo, AccionGuardarPartida::confirmarConDialogo);
    }

    /**
     * Igual que el constructor público, pero recibe cómo se pide el nombre y cómo se confirma
     * que se pisa un guardado. Es de paquete a propósito: lo usan los tests para no abrir
     * diálogos (un {@link JOptionPane} se queda esperando un clic).
     *
     * @param pedirNombre           devuelve el nombre escrito, o {@code null} si se cancela
     * @param confirmarSobrescribir recibe el nombre y devuelve {@code true} si se puede pisar
     */
    AccionGuardarPartida(RepositorioPartidas repositorio, Supplier<GestorPartida> partidaActual,
                         Consumer<String> alError, Consumer<String> alAviso,
                         Supplier<String> pedirNombre, Predicate<String> confirmarSobrescribir) {
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio");
        this.partidaActual = Objects.requireNonNull(partidaActual, "partidaActual");
        this.alError = Objects.requireNonNull(alError, "alError");
        this.alAviso = Objects.requireNonNull(alAviso, "alAviso");
        this.pedirNombre = Objects.requireNonNull(pedirNombre, "pedirNombre");
        this.confirmarSobrescribir = Objects.requireNonNull(confirmarSobrescribir, "confirmarSobrescribir");
    }

    /** Guarda la partida como se explica en la descripción de la clase. Llamar desde el EDT. */
    @Override
    public void run() {
        // Si salta algo que no es PersistenciaException (error de programación), el jugador ve un
        // mensaje genérico y la ventana sigue andando.
        EjecutorAcciones.ejecutar(this::guardar, alError);
    }

    private void guardar() {
        GestorPartida partida = partidaActual.get();
        if (partida == null) {
            alError.accept(MENSAJE_SIN_PARTIDA);
            return;
        }
        if (!partida.puedeGuardarse()) {
            alError.accept(MENSAJE_NO_SE_PUEDE_GUARDAR);
            return;
        }

        String escrito = pedirNombre.get();
        if (escrito == null) {
            return; // el jugador canceló el diálogo
        }
        String nombre = escrito.trim();
        if (repositorio.existe(nombre) && !confirmarSobrescribir.test(nombre)) {
            return;
        }

        try {
            repositorio.guardar(partida, nombre);
            alAviso.accept("Partida guardada como \"" + nombre + "\".");
        } catch (PersistenciaException e) {
            alError.accept(e.getMessage());
        }
    }

    // Diálogos reales

    private static String pedirNombreConDialogo() {
        Object escrito = JOptionPane.showInputDialog(ventanaActiva(), PEDIDO_NOMBRE, "Guardar partida",
                JOptionPane.PLAIN_MESSAGE, null, null, NOMBRE_SUGERIDO);
        return escrito == null ? null : escrito.toString();
    }

    /** Solo el "Sí" confirma: "No" o cerrar el diálogo con la X no pisan el guardado. */
    private static boolean confirmarConDialogo(String nombre) {
        int opcion = JOptionPane.showConfirmDialog(ventanaActiva(),
                "Ya existe una partida guardada como \"" + nombre + "\". ¿Querés reemplazarla?",
                "Reemplazar partida", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return opcion == JOptionPane.YES_OPTION;
    }

    private static Window ventanaActiva() {
        return KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
    }
}
