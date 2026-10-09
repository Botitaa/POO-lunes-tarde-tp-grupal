package batallanaval.excepciones;

/**
 * Se lanza cuando un movimiento viola las reglas.
 *
 * <p>Casos: paso no adyacente, bloqueado, sin puntos de movimiento, o barco que ya disparó.</p>
 */
public class MovimientoInvalidoException extends AccionInvalidaException {

    private static final long serialVersionUID = 1L;

    /**
     * @param mensaje texto para mostrar al jugador
     */
    public MovimientoInvalidoException(String mensaje) {
        super(mensaje);
    }
}