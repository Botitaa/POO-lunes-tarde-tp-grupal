package batallanaval.excepciones;

/**
 * Se lanza cuando falla la lectura o escritura de un archivo del juego.
 */
public class PersistenciaException extends Exception {

    private static final long serialVersionUID = 1L;

    public PersistenciaException(String mensaje) {
        super(mensaje);
    }

    public PersistenciaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
