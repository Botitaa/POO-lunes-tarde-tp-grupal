package batallanaval.excepciones;

/**
 * Se lanza cuando se intenta usar una habilidad que no está disponible.
 *
 * <p>Casos: habilidad en espera (cooldown) o ya usada este turno.</p>
 */
public class HabilidadNoDisponibleException extends AccionInvalidaException {

    private static final long serialVersionUID = 1L;

    /**
     * @param mensaje texto para mostrar al jugador
     */
    public HabilidadNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}