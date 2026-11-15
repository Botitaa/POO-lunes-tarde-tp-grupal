package batallanaval.modelo;

import java.util.Objects;

/**
 * Un disparo ya hecho: desde dónde salió, a qué casilla fue, cuánto daño hizo y si hundió al
 * blanco. Es inmutable (record). La vista lo usa para dibujar la línea del disparo y el daño.
 *
 * @param origen      casilla del barco que disparó
 * @param destino     casilla del barco que recibió el disparo
 * @param danio       daño que hizo
 * @param hundimiento {@code true} si el blanco se hundió
 */
public record Disparo(Posicion origen, Posicion destino, int danio, boolean hundimiento) {

    /**
     * @throws NullPointerException     si origen o destino son {@code null}
     * @throws IllegalArgumentException si el daño es negativo
     */
    public Disparo {
        Objects.requireNonNull(origen, "origen");
        Objects.requireNonNull(destino, "destino");
        if (danio < 0) {
            throw new IllegalArgumentException("El daño no puede ser negativo.");
        }
    }
}
