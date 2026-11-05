package batallanaval.modelo;

public final class Posicion{
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

    public int distanciaChebyshev(Posicion otra) {
        int diferenciaFilas = this.fila - otra.fila;
        if (diferenciaFilas < 0) { //si la diferencia es negativa, la hago positiva para obtener el valor absoluto
            diferenciaFilas = -diferenciaFilas;
        }
        int diferenciaColumnas = this.columna - otra.columna;
        if (diferenciaColumnas < 0) {//si la diferencia es negativa, la hago positiva para obtener el valor absoluto
            diferenciaColumnas = -diferenciaColumnas;
        }
        if (diferenciaFilas > diferenciaColumnas) {//si la diferencia de filas es mayor que la diferencia de columnas, devuelvo la diferencia de filas
            return diferenciaFilas;
        }
        return diferenciaColumnas;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {//si el objeto es el mismo, devuelvo true
            return true;
        }
        if ((!(obj instanceof Posicion))) {//si el objeto no es una instancia de Posicion, devuelvo false
            return false;
        }
        Posicion otra = (Posicion) obj;//si el objeto es una instancia de Posicion, lo casteo a Posicion
        return this.fila == otra.fila && this.columna == otra.columna;//si la fila y la columna son iguales, devuelvo true, sino devuelvo false
    }

    @Override
    public int hashCode() {
        return 31 * fila + columna; //uso 31 porque es un numero primo e imparr y ayuda a distribuir mejor los hashcodes
    }

    @Override
    public String toString() {
        return "(" + fila + "," + columna + ")"; //devuelve la posicion en formato (fila,columna)
    }
}
