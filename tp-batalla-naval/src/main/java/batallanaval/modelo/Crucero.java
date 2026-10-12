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

    public Crucero(int id) {
        super(id, ESTADISTICAS);
    }

    public Crucero(int id, Posicion posicion) {
        super(id, ESTADISTICAS, posicion);
    }

    @Override
    protected void aplicarHabilidad() {
        curar(2);
    }

    //movi este metodo aca por que es exclusiva de este barco
    public void curar(int cantidad) {
        if (!estaHundido()) {
            int nuevaVida = getVida() + cantidad;
            if (nuevaVida > getEstadisticas().vidaMaxima()) {
                setVida(getEstadisticas().vidaMaxima());
            } else {
                setVida(nuevaVida);
            }
        }
    }

    @Override
    public char getSimbolo() {
        return 'C';
    }
}
