package batallanaval.vista;

import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

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
 *   <li>Si ya hay un guardado en curso, avisa y no lanza otro.</li>
 *   <li>Si todo está bien, <b>lanza una {@link TareaGuardar} en un hilo propio</b> (Unidad 6) y
 *       avisa con {@code alEmpezar} para que quien la armó bloquee los botones. Cuando la tarea
 *       termina, el hilo de eventos (EDT) ejecuta {@code alTerminar} (siempre, salga bien o mal)
 *       y, según el caso:
 *       <ul>
 *         <li>si el repositorio lanza {@link PersistenciaException} (nombre inválido, error de
 *             escritura...), el mensaje de la excepción va a {@code alError} (en la aplicación,
 *             {@code ventana::mostrarError}) y el juego sigue;</li>
 *         <li>si guarda bien, avisa con {@code alAviso} (en la aplicación,
 *             {@code ventana::mostrarMensaje}).</li>
 *       </ul></li>
 * </ul>
 * <b>No decide reglas ni sabe escribir archivos</b>: valida el nombre y escribe el
 * {@link RepositorioPartidas}, y qué partidas se pueden guardar lo dice la partida.
 * <p>
 * <b>Qué corre en cada hilo.</b> Esta acción corre en el EDT (desde un botón o un menú): ahí se
 * piden el nombre y la confirmación, porque los diálogos de Swing no se pueden abrir desde otro
 * hilo. Lo único que sale del EDT es escribir el archivo, que hace la {@link TareaGuardar}.
 */
public final class AccionGuardarPartida implements Runnable {

    /** Nombre que aparece escrito de entrada en el diálogo. */
    static final String NOMBRE_SUGERIDO = "partida1";

    static final String PEDIDO_NOMBRE =
            "Nombre de la partida (letras, números, - y _; hasta 40 caracteres):";
    static final String MENSAJE_SIN_PARTIDA = "No hay una partida para guardar.";
    static final String MENSAJE_NO_SE_PUEDE_GUARDAR =
            "La partida solo se puede guardar al inicio de un turno o en la pantalla de blackout.";
    static final String MENSAJE_YA_SE_ESTA_GUARDANDO = "Ya se está guardando la partida.";

    private final RepositorioPartidas repositorio;
    private final Supplier<GestorPartida> partidaActual;
    private final Consumer<String> alError;
    private final Consumer<String> alAviso;
    private final Supplier<String> pedirNombre;
    private final Predicate<String> confirmarSobrescribir;
    private final Runnable alEmpezar;
    private final Runnable alTerminar;
    private final Executor lanzador;
    private final Executor enElEdt;

    // Solo se toca desde el EDT: evita que dos guardados corran a la vez.
    private boolean guardando = false;

    /**
     * Crea la acción con diálogos reales ({@link JOptionPane}) y sin avisos de inicio y fin del
     * guardado. El guardado corre en un hilo propio.
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
        this(repositorio, partidaActual, alError, alAviso, () -> { }, () -> { });
    }

    /**
     * Crea la acción con diálogos reales ({@link JOptionPane}). El guardado corre en un hilo propio
     * ({@code new Thread(tarea).start()}) y los avisos vuelven al EDT con
     * {@link SwingUtilities#invokeLater(Runnable)}.
     *
     * @param repositorio   donde se guarda la partida
     * @param partidaActual devuelve la partida en curso, o {@code null} si no hay ninguna (se
     *                      pregunta en cada guardado, porque la partida cambia al cargar otra)
     * @param alError       qué hacer con el mensaje cuando falla el guardado
     * @param alAviso       qué hacer con el mensaje cuando se guarda bien
     * @param alEmpezar     se ejecuta en el EDT justo antes de lanzar el hilo (bloquear botones)
     * @param alTerminar    se ejecuta en el EDT cuando el guardado terminó, salga bien o mal
     *                      (volver a habilitar los botones)
     * @throws NullPointerException si algún argumento es {@code null}
     */
    public AccionGuardarPartida(RepositorioPartidas repositorio, Supplier<GestorPartida> partidaActual,
                                Consumer<String> alError, Consumer<String> alAviso,
                                Runnable alEmpezar, Runnable alTerminar) {
        this(repositorio, partidaActual, alError, alAviso,
                AccionGuardarPartida::pedirNombreConDialogo, AccionGuardarPartida::confirmarConDialogo,
                alEmpezar, alTerminar,
                tarea -> new Thread(tarea, "hilo-guardar").start(), SwingUtilities::invokeLater);
    }

    /**
     * Igual que el constructor público, pero recibe cómo se pide el nombre y cómo se confirma
     * que se pisa un guardado, y guarda <b>sin hilo</b>: todo corre en el momento y en el hilo que
     * llama. Es de paquete a propósito: lo usan los tests para no abrir diálogos (un
     * {@link JOptionPane} se queda esperando un clic) ni depender de otro hilo.
     *
     * @param pedirNombre           devuelve el nombre escrito, o {@code null} si se cancela
     * @param confirmarSobrescribir recibe el nombre y devuelve {@code true} si se puede pisar
     */
    AccionGuardarPartida(RepositorioPartidas repositorio, Supplier<GestorPartida> partidaActual,
                         Consumer<String> alError, Consumer<String> alAviso,
                         Supplier<String> pedirNombre, Predicate<String> confirmarSobrescribir) {
        this(repositorio, partidaActual, alError, alAviso, pedirNombre, confirmarSobrescribir,
                () -> { }, () -> { }, Runnable::run, Runnable::run);
    }

    /**
     * Constructor completo, de paquete: además de lo anterior recibe quién lanza la tarea
     * ({@code lanzador}) y quién entrega los resultados al EDT ({@code enElEdt}). Los tests pasan
     * {@code Runnable::run} en los dos para que todo sea sincrónico.
     */
    AccionGuardarPartida(RepositorioPartidas repositorio, Supplier<GestorPartida> partidaActual,
                         Consumer<String> alError, Consumer<String> alAviso,
                         Supplier<String> pedirNombre, Predicate<String> confirmarSobrescribir,
                         Runnable alEmpezar, Runnable alTerminar, Executor lanzador, Executor enElEdt) {
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio");
        this.partidaActual = Objects.requireNonNull(partidaActual, "partidaActual");
        this.alError = Objects.requireNonNull(alError, "alError");
        this.alAviso = Objects.requireNonNull(alAviso, "alAviso");
        this.pedirNombre = Objects.requireNonNull(pedirNombre, "pedirNombre");
        this.confirmarSobrescribir = Objects.requireNonNull(confirmarSobrescribir, "confirmarSobrescribir");
        this.alEmpezar = Objects.requireNonNull(alEmpezar, "alEmpezar");
        this.alTerminar = Objects.requireNonNull(alTerminar, "alTerminar");
        this.lanzador = Objects.requireNonNull(lanzador, "lanzador");
        this.enElEdt = Objects.requireNonNull(enElEdt, "enElEdt");
    }

    /** Guarda la partida como se explica en la descripción de la clase. Llamar desde el EDT. */
    @Override
    public void run() {
        // Si salta algo que no es PersistenciaException (error de programación), el jugador ve un
        // mensaje genérico y la ventana sigue andando.
        EjecutorAcciones.ejecutar(this::guardar, alError);
    }

    private void guardar() {
        if (guardando) {
            alError.accept(MENSAJE_YA_SE_ESTA_GUARDANDO);
            return;
        }
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

        lanzarTarea(partida, nombre);
    }

    /**
     * Bloquea (alEmpezar) y lanza la {@link TareaGuardar}. Desde acá el trabajo sigue en otro hilo
     * y el resultado vuelve al EDT.
     */
    private void lanzarTarea(GestorPartida partida, String nombre) {
        TareaGuardar tarea = new TareaGuardar(repositorio, partida, nombre,
                guardado -> alAviso.accept("Partida guardada como \"" + guardado + "\"."),
                alError, this::terminarGuardado, enElEdt);
        guardando = true;
        try {
            alEmpezar.run();
            lanzador.execute(tarea);
        } catch (RuntimeException e) {
            // Si no se pudo ni lanzar el hilo, la tarea nunca va a correr su finally.
            terminarGuardado();
            throw e;
        }
    }

    /** Se ejecuta en el EDT cuando la tarea terminó: se puede volver a guardar y a usar los botones. */
    private void terminarGuardado() {
        guardando = false;
        alTerminar.run();
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
