package batallanaval.excepciones;

/**
 * Se lanza cuando un disparo viola las reglas.
 *
 * <p>Casos: objetivo no visible, fuera de rango, sin línea de visión, o tirador en cooldown.</p>
 */
public class DisparoInvalidoException extends AccionInvalidaException {

  private static final long serialVersionUID = 1L;

  /**
   * @param mensaje texto para mostrar al jugador
   */
  public DisparoInvalidoException(String mensaje) {
    super(mensaje);
  }
}