package batallanaval.modelo;

import batallanaval.excepciones.MapaInvalidoException;

public class Tablero {
    public static final int FILAS = 10;
    public static final int COLUMNAS = 10;
    private final Casilla[][] grilla;


    public Tablero(Casilla[][] grilla) {//constructir
        if(grilla == null) {
            throw new MapaInvalidoException("La grilla no puede ser nula");
        }
        if(grilla.length != FILAS) {
            throw new MapaInvalidoException("La grilla debe tener " + FILAS + " filas");
        }
        this.grilla = new Casilla[FILAS][COLUMNAS];//copia defensiva botovich

        for (int fila = 0; fila < FILAS; fila++) {//recorro las filas de la grillla
            if(grilla[fila] == null||grilla[fila].length != COLUMNAS ) {
                throw new MapaInvalidoException("La fila " + fila + "debe tener " + COLUMNAS + " columnas");
            }
            for (int columna = 0; columna < COLUMNAS; columna++) {//recorro las columnas de la grilla
                if(grilla[fila][columna] == null) {
                    throw new MapaInvalidoException("La casilla en la posición (" + fila + "," + columna + ") no puede ser nula");
                }
                this.grilla[fila][columna] = grilla[fila][columna];//asigno la casilla a la grilla
            }
        }
    }

    public boolean estaDentro(int fila, int columna) {//verifica si la fila y columna estan dentro del tablero
        return fila >= 0 && fila < FILAS && columna >= 0 && columna < COLUMNAS;
    }

    public boolean estaDentro(Posicion posicion) {//verifica si la posicion esta dentro del tablero
        return estaDentro(posicion.getFila(), posicion.getColumna());
    }
    //devuelve la casilla en la posicion (fila, columna) si esta dentro del tablero, sino lanza una excepcion
    public Casilla getCasilla(int fila, int columna) {
        if (!estaDentro(fila, columna)) {
            throw new IllegalArgumentException("La posición (" + fila + "," + columna + ") está fuera del tablero");
        }
        return grilla[fila][columna];
    }

    public Casilla getCasilla(Posicion pos) {//devuelve la casilla en la posicion pos
        return getCasilla(pos.getFila(), pos.getColumna());
    }

    public boolean esTransitable(Posicion pos) {//verifica si la posicion pos es transitable
        if (!estaDentro(pos)) {
            return false;
        }
        return !getCasilla(pos).bloqueaPaso();
    }
}
