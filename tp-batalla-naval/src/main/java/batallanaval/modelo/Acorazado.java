package batallanaval.modelo;

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

    public Acorazado(int id) {
        super(id, ESTADISTICAS);
    }

    public Acorazado(int id, Posicion posicion) {
        super(id, ESTADISTICAS, posicion);
    }

    @Override
    protected void aplicarHabilidad() {
    }

    @Override
    public int getRangoDisparo() {
        if (estaHabilidadActiva()) {
            return 8;
        }
        return 6;
    }

    @Override
    public char getSimbolo() {
        return 'A';
    }
}
