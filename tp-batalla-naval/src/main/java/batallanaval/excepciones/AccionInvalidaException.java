package batallanaval.excepciones;

/**
 * Base de las excepciones por violar una regla del juego.
 *
 * <p>Es unchecked: la vista ya deshabilita las acciones inválidas, así que si
 * una llega hasta acá se muestra el mensaje en la GUI sin cortar la partida.</p>
 */
public class AccionInvalidaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param mensaje texto para mostrar al jugador
     */
    public AccionInvalidaException(String mensaje) {
        super(mensaje);
    }
}