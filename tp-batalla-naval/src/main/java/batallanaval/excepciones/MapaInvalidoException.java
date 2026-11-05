package batallanaval.excepciones;

/**
 * Se lanza cuando el mapa no es válido.
 *
 * <p>Casos: mapa mal formado u obstáculos fuera de la zona neutral. No extiende {@link AccionInvalidaException} porque no es una acción del jugador.</p>
 */
public class MapaInvalidoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param mensaje texto para mostrar al jugador
     */
    public MapaInvalidoException(String mensaje) {
        super(mensaje);
    }
}