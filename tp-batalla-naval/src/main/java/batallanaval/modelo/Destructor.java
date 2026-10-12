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

    public Destructor(int id) {
        super(id, ESTADISTICAS);
    }

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
