package batallanaval.modelo;

/** Coordenada inmutable (fila, columna) en el tablero. */
public final class Posicion {
    private final int fila;
    private final int columna;

    public Posicion(int fila, int columna) {
        this.fila = fila;
        this.columna = columna;
    }

    public int getFila() {
        return fila;
    }

    public int getColumna() {
        return columna;
    }

    /** Distancia Chebyshev: max(|fila1 - fila2|, |col1 - col2|). Usada para rango de tiro y visión. */
    public int distanciaChebyshev(Posicion otra) {
        int diferenciaFilas = this.fila - otra.fila;
        if (diferenciaFilas < 0) {
            diferenciaFilas = -diferenciaFilas;
        }
        int diferenciaColumnas = this.columna - otra.columna;
        if (diferenciaColumnas < 0) {
            diferenciaColumnas = -diferenciaColumnas;
        }
        if (diferenciaFilas > diferenciaColumnas) {
            return diferenciaFilas;
        }
        return diferenciaColumnas;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Posicion)) {
            return false;
        }
        Posicion otra = (Posicion) obj;
        return this.fila == otra.fila && this.columna == otra.columna;
    }

    @Override
    public int hashCode() {
        return 31 * fila + columna;
    }

    @Override
    public String toString() {
        return "(" + fila + "," + columna + ")";
    }
}
