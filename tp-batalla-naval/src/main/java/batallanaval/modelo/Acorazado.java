package batallanaval.modelo;

/**
 * Representa un navío de tipo Acorazado, con alto blindaje, gran alcance y habilidad de disparo extendido.
 */
public class Acorazado extends Barco {

    private static final EstadisticasBarco ESTADISTICAS = new EstadisticasBarco(
            8,
            2,
            3,
            6,
            5,
            1,
            4,
            3
    );

    /**
     * Crea un acorazado sin posición inicial asignada.
     *
     * @param id identificador único del barco.
     */
    public Acorazado(int id) {
        super(id, ESTADISTICAS);
    }

    /**
     * Crea un acorazado con posición inicial asignada.
     *
     * @param id       identificador único del barco.
     * @param posicion posición inicial en el mapa.
     */
    public Acorazado(int id, Posicion posicion) {
        super(id, ESTADISTICAS, posicion);
    }

    @Override
    protected void aplicarHabilidad() {
    }

    /**
     * Devuelve el rango de disparo, aumentado a 8 si la habilidad especial esta activa.
     *
     * @return rango efectivo de tiro.
     */
    @Override
    public int getRangoDisparo() {
        if (estaHabilidadActiva()) {
            return 8;
        }
        return 6;
    }

    /**
     * Devuelve el simbolo 'A' representativo del acorazado.
     *
     * @return caracter 'A'.
     */
    @Override
    public char getSimbolo() {
        return 'A';
    }
}
