package batallanaval.vista;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

import batallanaval.controlador.InstantaneaPartida;
import batallanaval.excepciones.PersistenciaException;
import batallanaval.persistencia.RepositorioPartidas;

/**
 * Tarea que <b>guarda la partida en un hilo propio</b>, para que escribir el archivo no congele
 * la ventana. Es un {@link Runnable} (Unidad 6: clase {@link Thread} + interfaz
 * {@link Runnable}): quien la crea la lanza con {@code new Thread(tarea).start()}.
 * <p>
 * El cuerpo de {@link #run()} sigue el esquema de la guía del TP:
 * <pre>
 * try     { repositorio.guardar(instantanea, nombre);  → avisa que se guardó }
 * catch   (PersistenciaException e) { → muestra el mensaje del error }
 * finally { → vuelve a habilitar los botones }
 * </pre>
 * El {@code finally} corre siempre: si el guardado sale bien, si falla con una
 * {@link PersistenciaException} o si salta cualquier otra excepción inesperada. Así los botones
 * nunca quedan bloqueados para siempre.
 * <p>
 * <b>El hilo propio no toca la interfaz.</b> Swing no es <i>thread-safe</i>: solo el hilo de
 * despacho de eventos (EDT) puede cambiar componentes. Por eso lo único que hace el hilo propio
 * es escribir el archivo; el aviso, el error y el re-habilitado de los botones se le entregan
 * al EDT con {@link SwingUtilities#invokeLater(Runnable)}.
 * <p>
 * <b>Qué se comparte entre hilos.</b> Nada que cambie. La tarea no recibe la partida sino una
 * {@link InstantaneaPartida} que {@link AccionGuardarPartida} armó en el EDT antes de lanzarla:
 * una copia inmutable (records y listas que nadie más modifica). Si mientras se escribe el
 * archivo el jugador sigue tocando el tablero, cambia la partida, no la copia, y el archivo
 * queda con el estado del momento en que se tocó "Guardar".
 * <p>
 * No sabe pedir nombres ni confirmar nada: eso lo hace {@link AccionGuardarPartida} en el EDT
 * antes de lanzar esta tarea.
 */
public final class TareaGuardar implements Runnable {

    private final RepositorioPartidas repositorio;
    private final InstantaneaPartida instantanea;
    private final String nombre;
    private final Consumer<String> alGuardar;
    private final Consumer<String> alError;
    private final Runnable alTerminar;
    private final Executor enElEdt;

    /**
     * Crea la tarea. Los tres últimos argumentos se ejecutan siempre en el EDT.
     *
     * @param repositorio donde se escribe la partida
     * @param instantanea foto de la partida a guardar, armada en el EDT con
     *                    {@code partida.crearInstantanea()}
     * @param nombre      nombre del guardado, ya sin espacios sobrantes
     * @param alGuardar   recibe el nombre guardado cuando el guardado salió bien
     * @param alError     recibe el mensaje para el jugador cuando el guardado falló
     * @param alTerminar  se ejecuta siempre al final, salga bien o mal (volver a habilitar botones)
     * @throws NullPointerException si algún argumento es {@code null}
     */
    public TareaGuardar(RepositorioPartidas repositorio, InstantaneaPartida instantanea, String nombre,
                        Consumer<String> alGuardar, Consumer<String> alError, Runnable alTerminar) {
        this(repositorio, instantanea, nombre, alGuardar, alError, alTerminar, SwingUtilities::invokeLater);
    }

    /**
     * Igual que el constructor público, pero recibe quién entrega los resultados al EDT. Es de
     * paquete a propósito: los tests pasan un {@link Executor} que ejecuta en el momento
     * ({@code Runnable::run}) para no depender del reloj ni del hilo de eventos.
     */
    TareaGuardar(RepositorioPartidas repositorio, InstantaneaPartida instantanea, String nombre,
                 Consumer<String> alGuardar, Consumer<String> alError, Runnable alTerminar,
                 Executor enElEdt) {
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio");
        this.instantanea = Objects.requireNonNull(instantanea, "instantanea");
        this.nombre = Objects.requireNonNull(nombre, "nombre");
        this.alGuardar = Objects.requireNonNull(alGuardar, "alGuardar");
        this.alError = Objects.requireNonNull(alError, "alError");
        this.alTerminar = Objects.requireNonNull(alTerminar, "alTerminar");
        this.enElEdt = Objects.requireNonNull(enElEdt, "enElEdt");
    }

    /**
     * Escribe la instantánea y le entrega el resultado al EDT. No hay que llamarlo directamente:
     * se ejecuta dentro del hilo que lanza {@code new Thread(tarea).start()}.
     */
    @Override
    public void run() {
        try {
            repositorio.guardar(instantanea, nombre);
            enElEdt.execute(() -> alGuardar.accept(nombre));
        } catch (PersistenciaException e) {
            // Archivo imposible de escribir, nombre inválido, partida que no está en juego...
            // El mensaje de la excepción ya está escrito para el jugador.
            String mensaje = e.getMessage();
            enElEdt.execute(() -> alError.accept(mensaje));
        } catch (RuntimeException e) {
            // Error de programación: sin este catch el hilo moriría sin avisar a nadie.
            // Mismo mensaje genérico que el resto de la vista (y que TareaCargar).
            enElEdt.execute(() -> alError.accept(EjecutorAcciones.MENSAJE_ERROR_INESPERADO));
        } finally {
            enElEdt.execute(alTerminar);
        }
    }
}
