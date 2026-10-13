package batallanaval.modelo;


public class Crucero extends Barco {

    private static final EstadisticasBarco ESTADISTICAS = new EstadisticasBarco(
            5,
            3,
            2,
            4,
            3,
            0,
            2,
            3
    );

    /**
     * Crea un crucero sin posición inicial asignada.
     *
     * @param id identificador único del barco.
     */
    public Crucero(int id) {
        super(id, ESTADISTICAS);
    }

    /**
     * Crea un crucero con posición inicial asignada.
     *
     * @param id       identificador único del barco.
     * @param posicion posición inicial en el mapa.
     */
    public Crucero(int id, Posicion posicion) {
        super(id, ESTADISTICAS, posicion);
    }

    @Override
    protected void aplicarHabilidad() {
        curar(2);
    }

    @Override
    public char getSimbolo() {
        return 'C';
    }
}
