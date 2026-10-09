package batallanaval.excepciones;

/**
 * Se lanza cuando un despliegue viola las reglas.
 *
 * <p>Casos: casilla fuera de zona, ocupada u obstáculo; confirmar sin tener los 6 barcos ubicados.</p>
 */
public class DespliegueInvalidoException extends AccionInvalidaException {

    private static final long serialVersionUID = 1L;

    /**
     * @param mensaje texto para mostrar al jugador
     */
    public DespliegueInvalidoException(String mensaje) {
        super(mensaje);
    }
}