package batallanaval.modelo;

public enum Casilla {
    AGUA('~', false, false), //no bloquea nada
    PIEDRA('P', true, false), //bloque paso, no vision
    ISLA('I', true, true), // bloque todo
    RESTOS('X', false, false); // no bloquea nada, solo estetico

    private final char simbolo;
    private final boolean bloqueaPaso;
    private final boolean bloqueaVision;

    Casilla(char simbolo, boolean bloqueaPaso, boolean bloqueaVision) {
        this.simbolo = simbolo;
        this.bloqueaPaso = bloqueaPaso;
        this.bloqueaVision = bloqueaVision;
    }

    public boolean bloqueaPaso() {
        return bloqueaPaso;
    }
    public boolean bloqueaVision() {
        return bloqueaVision;
    }
    public char getSimbolo() {
        return simbolo;
    }

    public static Casilla desdeSimbolo(char simbolo) { //devuelve la casilla correspondiente al simbolo
        Casilla[] todas = values();
        for (int i = 0; i < todas.length; i++) {
            if (todas[i].simbolo == simbolo) {
                return todas[i];
            }
        }
        throw new IllegalArgumentException("Símbolo de casilla desconocido: " + simbolo); //si el simbolo no corresponde a ninguna casilla, lanza una excepcion
    }
}
