package batallanaval.vista;

import java.io.Serial;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Panel que tiene su propio hilo de ejecución.
 * <p>
 * Es una clase abstracta que aplica el patrón <b>Template Method</b>: esta clase fija
 * el algoritmo (arrancar el hilo, repetir {@code actualizar()} cada cierto tiempo y
 * frenarlo) con métodos {@code final}, y las subclases solo completan los dos pasos
 * abstractos: {@link #actualizar()} y {@link #getIntervaloMs()}.
 * <p>
 * <b>Por qué el hilo no toca la interfaz.</b> Swing no es <i>thread-safe</i>: sus
 * componentes están pensados para usarse desde un solo hilo, el de despacho de
 * eventos (EDT). Si el hilo propio de este panel modificara un {@code JLabel} o
 * cualquier otro componente directamente, podría hacerlo justo mientras el EDT lo
 * está dibujando, y la pantalla quedaría rota o inconsistente. Por eso el hilo propio
 * solo espera y, en cada vuelta, le pide al EDT que ejecute {@link #actualizar()}
 * con {@link SwingUtilities#invokeLater(Runnable)}. Cualquier cambio en componentes Swing
 * tiene que hacerse dentro de {@code actualizar()}, y nunca en otro lugar de la subclase
 * que corra en el hilo propio.
 * <p>
 * El hilo es de tipo <i>daemon</i>: si el usuario cierra la ventana, no impide que
 * el programa termine.
 */
public abstract class PanelConHilo extends JPanel implements Runnable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient Object cerrojo = new Object();
    private transient Thread hilo;

    /**
     * Crea el hilo y lo arranca. Si ya hay un hilo corriendo, no hace nada, así que
     * llamar dos veces seguidas no crea dos hilos.
     *
     * @throws IllegalStateException si {@link #getIntervaloMs()} no es mayor a 0
     */
    public final void iniciar() {
        synchronized (cerrojo) {
            if (estaCorriendo()) {
                return;
            }
            if (getIntervaloMs() <= 0) {
                throw new IllegalStateException("El intervalo del panel tiene que ser mayor a 0 ms");
            }
            hilo = new Thread(this, "hilo-" + getClass().getSimpleName());
            hilo.setDaemon(true);
            hilo.start();
        }
    }

    /**
     * Le pide al hilo que termine (con {@link Thread#interrupt()}). Si nunca se
     * inició, o ya se detuvo, no hace nada.
     */
    public final void detener() {
        synchronized (cerrojo) {
            if (hilo != null) {
                hilo.interrupt();
                hilo = null;
            }
        }
    }

    /**
     * @return {@code true} si el hilo del panel está vivo (se inició y no se detuvo)
     */
    public final boolean estaCorriendo() {
        synchronized (cerrojo) {
            return hilo != null && hilo.isAlive();
        }
    }

    /**
     * Cuerpo del hilo: mientras no lo interrumpan y siga siendo el hilo del panel, le pide
     * al EDT que ejecute {@link #actualizar()} y duerme {@link #getIntervaloMs()} milisegundos.
     * <p>
     * No hay que llamarlo directamente: lo usa {@link #iniciar()}.
     */
    @Override
    public final void run() {
        while (!Thread.currentThread().isInterrupted() && esElHiloVigente()) {
            SwingUtilities.invokeLater(this::actualizar);
            try {
                Thread.sleep(getIntervaloMs());
            } catch (InterruptedException e) {
                // sleep() borró la marca de interrupción al lanzar la excepción: se vuelve
                // a poner para que la condición del while corte el bucle.
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Indica si el hilo que está ejecutando {@link #run()} sigue siendo el del panel. Deja de
     * serlo cuando se llama a {@link #detener()}. Se revisa además de la interrupción porque
     * algunas clases de Swing pueden "tragarse" una interrupción si llega justo cuando
     * arrancan por primera vez; así {@code detener()} funciona igual.
     */
    private boolean esElHiloVigente() {
        synchronized (cerrojo) {
            return hilo == Thread.currentThread();
        }
    }

    /**
     * Hilo actual del panel, o {@code null} si no hay. Solo para los tests.
     */
    final Thread getHilo() {
        synchronized (cerrojo) {
            return hilo;
        }
    }

    /**
     * Paso que se repite en cada vuelta. Se ejecuta en el EDT, por lo que acá sí se
     * pueden modificar componentes Swing.
     */
    protected abstract void actualizar();

    /**
     * @return milisegundos de espera entre una actualización y la siguiente; tiene que ser mayor a 0
     */
    protected abstract int getIntervaloMs();
}
