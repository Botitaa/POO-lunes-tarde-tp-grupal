package batallanaval.modelo;

/**
 * Representa un navío de tipo Destructor, con gran movilidad y habilidad de danio doble.
 */
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

    /**
     * Devuelve el danio del destructor, aumentado al doble (6) si su habilidad especial esta activa.
     *
     * @return poder de disparo efectivo.
     */
    @Override
    public int getDanio() {
        if (estaHabilidadActiva()) {
            return 6;
        }
        return 3;
    }

    /**
     * Devuelve el simbolo 'D' representativo del destructor.
     *
     * @return caracter 'D'.
     */
    @Override
    public char getSimbolo() {
        return 'D';
    }
}
