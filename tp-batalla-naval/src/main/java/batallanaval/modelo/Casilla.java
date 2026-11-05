package batallanaval.modelo;

/**
 * Representa el tipo de casilla de un tablero con sus propiedades físicas fijas.
 */
public enum Casilla {
    AGUA('~', false, false),
    PIEDRA('P', true, false),
    ISLA('I', true, true),
    RESTOS('X', false, false);

    private final char simbolo;
    private final boolean bloqueaPaso;
    private final boolean bloqueaVision;

    Casilla(char simbolo, boolean bloqueaPaso, boolean bloqueaVision) {
        this.simbolo = simbolo;
        this.bloqueaPaso = bloqueaPaso;
        this.bloqueaVision = bloqueaVision;
    }

    /**
     * Indica si la casilla bloquea el movimiento de los barcos.
     *
     * @return true si impide el paso de barcos.
     */
    public boolean bloqueaPaso() {
        return bloqueaPaso;
    }

    /**
     * Indica si la casilla bloquea la linea de vision.
     *
     * @return true si impide la vision a traves de ella.
     */
    public boolean bloqueaVision() {
        return bloqueaVision;
    }

    /**
     * Devuelve el caracter asociado al tipo de casilla.
     *
     * @return simbolo representativo de la casilla.
     */
    public char getSimbolo() {
        return simbolo;
    }

    /**
     * Obtiene el tipo de casilla correspondiente a un simbolo dado.
     *
     * @param simbolo caracter del tipo de casilla.
     * @return casilla correspondiente al simbolo.
     * @throws IllegalArgumentException si el simbolo no corresponde a ninguna casilla conocida.
     */
    public static Casilla desdeSimbolo(char simbolo) {
        Casilla[] todas = values();
        for (int i = 0; i < todas.length; i++) {
            if (todas[i].simbolo == simbolo) {
                return todas[i];
            }
        }
        throw new IllegalArgumentException("Símbolo de casilla desconocido: " + simbolo);
    }
}
