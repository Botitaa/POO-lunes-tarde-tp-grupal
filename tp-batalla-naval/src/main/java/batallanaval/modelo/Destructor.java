package batallanaval.modelo;


public class Destructor extends Barco {

    private static final EstadisticasBarco ESTADISTICAS = new EstadisticasBarco(
            3,
            4,
            1,
            2,
            2,
            0,
            3,
            3
    );

    /**
     * Crea un destructor sin posición inicial asignada.
     *
     * @param id identificador único del barco.
     */
    public Destructor(int id) {
        super(id, ESTADISTICAS);
    }

    /**
     * Crea un destructor con posición inicial asignada.
     *
     * @param id       identificador único del barco.
     * @param posicion posición inicial en el mapa.
     */
    public Destructor(int id, Posicion posicion) {
        super(id, ESTADISTICAS, posicion);
    }

    @Override
    protected void aplicarHabilidad() {
    }

    @Override
    public int getDanio() {
        if (estaHabilidadActiva()) {
            return 6;
        }
        return 3;
    }

    @Override
    public char getSimbolo() {
        return 'D';
    }
}
