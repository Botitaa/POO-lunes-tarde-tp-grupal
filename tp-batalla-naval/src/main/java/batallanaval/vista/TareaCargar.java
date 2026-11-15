package batallanaval.vista;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

import batallanaval.controlador.GestorPartida;
import batallanaval.excepciones.PersistenciaException;
import batallanaval.persistencia.RepositorioPartidas;

/**
 * Tarea que <b>carga una partida guardada en un hilo propio</b>, para que leer y validar el
 * archivo no congele la ventana. Es la pareja de {@link TareaGuardar}: un {@link Runnable} que
 * se lanza con {@code new Thread(tarea).start()}.
 * <p>
 * El cuerpo de {@link #run()} es:
 * <pre>
 * try     { partida = repositorio.cargar(nombre);  → entrega la partida }
 * catch   (PersistenciaException e) { → muestra el mensaje del error }
 * finally { → vuelve a habilitar los botones }
 * </pre>
 * <b>El hilo propio no toca la interfaz.</b> Solo lee el archivo y arma la partida; con
 * {@link SwingUtilities#invokeLater(Runnable)} le entrega al hilo de despacho de eventos (EDT) la
 * partida cargada (en la aplicación, a {@code ControladorAplicacion}), el mensaje de error o el
 * aviso de que terminó. La partida recién armada todavía no la conoce nadie, así que pasarla de
 * un hilo al otro es seguro.
 * <p>
 * Si la entrega de la partida lanza una excepción en el EDT (un error de programación en quien la
 * recibe), no se pierde: se pasa por {@link EjecutorAcciones} y el jugador ve un mensaje
 * genérico en lugar de que la ventana quede a medias.
 */
public final class TareaCargar implements Runnable {

    private final RepositorioPartidas repositorio;
    private final String nombre;
    private final Consumer<? super GestorPartida> alCargar;
    private final Consumer<String> alError;
    private final Runnable alTerminar;
    private final Executor enElEdt;

    /**
     * Crea la tarea. Los tres últimos argumentos se ejecutan siempre en el EDT.
     *
     * @param repositorio de donde se carga la partida
     * @param nombre      nombre del guardado a cargar
     * @param alCargar    recibe la partida cuando la carga salió bien
     * @param alError     recibe el mensaje para el jugador cuando la carga falló
     * @param alTerminar  se ejecuta siempre al final, salga bien o mal (volver a habilitar botones)
     * @throws NullPointerException si algún argumento es {@code null}
     */
    public TareaCargar(RepositorioPartidas repositorio, String nombre, Consumer<? super GestorPartida> alCargar,
                       Consumer<String> alError, Runnable alTerminar) {
        this(repositorio, nombre, alCargar, alError, alTerminar, SwingUtilities::invokeLater);
    }

    /**
     * Igual que el constructor público, pero recibe quién entrega los resultados al EDT. Es de
     * paquete a propósito: los tests pasan un {@link Executor} que ejecuta en el momento
     * ({@code Runnable::run}).
     */
    TareaCargar(RepositorioPartidas repositorio, String nombre, Consumer<? super GestorPartida> alCargar,
                Consumer<String> alError, Runnable alTerminar, Executor enElEdt) {
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio");
        this.nombre = Objects.requireNonNull(nombre, "nombre");
        this.alCargar = Objects.requireNonNull(alCargar, "alCargar");
        this.alError = Objects.requireNonNull(alError, "alError");
        this.alTerminar = Objects.requireNonNull(alTerminar, "alTerminar");
        this.enElEdt = Objects.requireNonNull(enElEdt, "enElEdt");
    }

    /**
     * Carga la partida y le entrega el resultado al EDT. No hay que llamarlo directamente:
     * se ejecuta dentro del hilo que lanza {@code new Thread(tarea).start()}.
     */
    @Override
    public void run() {
        try {
            GestorPartida partida = repositorio.cargar(nombre);
            enElEdt.execute(() -> EjecutorAcciones.ejecutar(() -> alCargar.accept(partida), alError));
        } catch (PersistenciaException e) {
            // Archivo borrado a mano, corrupto, de otra versión... El mensaje ya es para el jugador.
            String mensaje = e.getMessage();
            enElEdt.execute(() -> alError.accept(mensaje));
        } catch (RuntimeException e) {
            enElEdt.execute(() -> alError.accept(EjecutorAcciones.MENSAJE_ERROR_INESPERADO));
        } finally {
            enElEdt.execute(alTerminar);
        }
    }
}
