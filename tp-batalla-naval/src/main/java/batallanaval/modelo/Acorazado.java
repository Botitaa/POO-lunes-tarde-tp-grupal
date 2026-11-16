package batallanaval.modelo;


public class Acorazado extends Barco {

    private static final EstadisticasBarco ESTADISTICAS = new EstadisticasBarco(
            8,
            2,
            3,
            4,
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

    @Override
    public int getRangoDisparo() {
        if (estaHabilidadActiva()) {
            return 6;
        }
        return 4;
    }

    @Override
    public char getSimbolo() {
        return 'A';
    }
}
