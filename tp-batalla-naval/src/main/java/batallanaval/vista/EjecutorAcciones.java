package batallanaval.vista;

import java.awt.event.ActionListener;
import java.util.Objects;
import java.util.function.Consumer;

import batallanaval.excepciones.AccionInvalidaException;

/**
 * Ejecuta las acciones de los botones de la vista sin que una excepción corte el juego.
 * <p>
 * Todos los botones (y atajos) de las pantallas pasan por acá, así el manejo de errores
 * está escrito una sola vez y es igual en toda la interfaz:
 * <ul>
 *   <li>Si la acción termina bien, no se muestra nada (la pantalla se refresca sola cuando
 *       la partida avisa el cambio).</li>
 *   <li>Si la partida la rechaza con una {@link AccionInvalidaException} (o cualquiera de sus
 *       subclases: despliegue, movimiento, disparo, habilidad), se muestra el mensaje de la
 *       excepción, que ya está escrito para el jugador.</li>
 *   <li>Si salta cualquier otra {@link RuntimeException}, es un error de programación: al
 *       jugador no le sirve el mensaje técnico, así que se muestra
 *       {@value #MENSAJE_ERROR_INESPERADO}.</li>
 * </ul>
 * En los dos casos de error el juego sigue: la excepción no sale del listener, el EDT sigue
 * procesando eventos y el jugador puede volver a intentar u otra cosa.
 * <p>
 * Los {@link Error} (por ejemplo, {@code OutOfMemoryError}) no se atrapan a propósito: son
 * problemas de la máquina virtual de los que el juego no se puede recuperar.
 * <p>
 * Es una clase utilitaria (constructor privado, métodos {@code static}) y de paquete: solo
 * la usan las pantallas de {@code vista}.
 */
final class EjecutorAcciones {

    /** Mensaje que ve el jugador cuando salta un error que no es una regla del juego. */
    static final String MENSAJE_ERROR_INESPERADO = "Ocurrió un error inesperado";

    /** Mensaje de respaldo si la partida rechazó la acción con una excepción sin mensaje. */
    static final String MENSAJE_ACCION_INVALIDA = "La acción no es válida";

    private EjecutorAcciones() {
        // Clase utilitaria: no se instancia.
    }

    /**
     * Ejecuta la acción y, si lanza una excepción, le pasa a {@code alError} el mensaje
     * que tiene que ver el jugador en lugar de dejar que la excepción corte el juego.
     *
     * @param accion  lo que tiene que hacer el botón (por ejemplo, {@code partida::terminarTurno})
     * @param alError dónde mostrar el mensaje de error (en la aplicación,
     *                {@code ventana::mostrarError}, que lo escribe en la barra de estado)
     * @return {@code true} si la acción terminó bien, {@code false} si hubo un error
     * @throws NullPointerException si algún argumento es {@code null}
     */
    static boolean ejecutar(Runnable accion, Consumer<String> alError) {
        Objects.requireNonNull(accion, "accion");
        Objects.requireNonNull(alError, "alError");
        try {
            accion.run();
            return true;
        } catch (AccionInvalidaException e) {
            // Va primero: es subclase de RuntimeException. Si estuviera después,
            // el catch de RuntimeException la atraparía antes y el código no compilaría.
            alError.accept(mensajeParaJugador(e));
        } catch (RuntimeException e) {
            alError.accept(MENSAJE_ERROR_INESPERADO);
        }
        return false;
    }

    /**
     * Arma el listener de un botón que ejecuta la acción con {@link #ejecutar(Runnable, Consumer)}.
     * Sirve para escribir {@code boton.addActionListener(EjecutorAcciones.alTocar(accion, alError))}
     * en lugar de repetir el {@code try/catch} en cada botón.
     *
     * @param accion  lo que tiene que hacer el botón
     * @param alError dónde mostrar el mensaje de error
     * @return listener listo para agregarle a un botón o a un campo de texto
     * @throws NullPointerException si algún argumento es {@code null}
     */
    static ActionListener alTocar(Runnable accion, Consumer<String> alError) {
        // Se valida acá y no recién al tocar el botón: un null se detecta al armar la pantalla.
        Objects.requireNonNull(accion, "accion");
        Objects.requireNonNull(alError, "alError");
        return evento -> ejecutar(accion, alError);
    }

    /** El mensaje de la excepción, o uno genérico si vino vacío. */
    private static String mensajeParaJugador(AccionInvalidaException e) {
        String mensaje = e.getMessage();
        if (mensaje == null || mensaje.isBlank()) {
            return MENSAJE_ACCION_INVALIDA;
        }
        return mensaje;
    }
}
