package batallanaval.modelo;

import batallanaval.excepciones.MapaInvalidoException;

public class Tablero {
    public static final int FILAS = 10;
    public static final int COLUMNAS = 10;
    private final Casilla[][] grilla;

    private static final int ULTIMA_FILA_DESPLIEGUE_J1 = 2;
    private static final int PRIMERA_FILA_DESPLIEGUE_J2 = 7;

    private static final String[] MAPA_ESTANDAR = {
            "~~~~~~~~~~",//0
            "~~~~~~~~~~",//1
            "~~~~~~~~~~",//2 ultima fila de despliegue jugador 1
            "~~II~~~P~~",//3
            "~~I~~~P~~~",//4
            "~~~P~~~I~~",//5
            "~~P~~~II~~",//6
            "~~~~~~~~~~",//7 primera fila de despliegue jugador 2
            "~~~~~~~~~~",//8
            "~~~~~~~~~~"//9
    };
    public Tablero(Casilla[][] grilla) {//constructir
        validarMapa(grilla);//valido el mapa
        this.grilla = new Casilla[FILAS][COLUMNAS];//copia defensiva botovich
        for (int fila = 0; fila < FILAS; fila++) {
            for (int columna = 0; columna < COLUMNAS; columna++) {
                this.grilla[fila][columna] = grilla[fila][columna];
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

    private void validarMapa(Casilla[][] grilla) {
        if (grilla == null) {
            throw new MapaInvalidoException("La grilla no puede ser nula");
        }
        if (grilla.length != FILAS) {
            throw new MapaInvalidoException("La grilla debe tener " + FILAS + " filas");
        }
        for (int fila = 0; fila < FILAS; fila++) {//recorro las filas de la grillla
            if (grilla[fila] == null || grilla[fila].length != COLUMNAS) {
                throw new MapaInvalidoException("La fila " + fila + "debe tener " + COLUMNAS + " columnas");
            }
            for (int columna = 0; columna < COLUMNAS; columna++) {//recorro las columnas de la grilla
                Casilla casilla = grilla[fila][columna];
                if (casilla == null) {
                    throw new MapaInvalidoException("La casilla en la posición (" + fila + "," + columna + ") no puede ser nula");
                }
                if (casilla.bloqueaPaso() && esFilaDespliegue(fila)) {
                    throw new MapaInvalidoException("Hay un obstáculo en la zona de despliegue. Posicion: " + fila + "," + columna);
                }
            }
        }
    }

    private boolean esFilaDespliegue(int fila) {
        return fila <= ULTIMA_FILA_DESPLIEGUE_J1 || fila >= PRIMERA_FILA_DESPLIEGUE_J2;
    }
//devuelve un tablero con el mapa estandar
    public static Tablero crearMapaEstandar() {
        Casilla[][] grilla = new Casilla[FILAS][COLUMNAS];
        for (int fila = 0; fila < FILAS; fila++) {
            String linea = MAPA_ESTANDAR[fila];//obtengo la linea correspondiente a la fila
            for (int columna = 0; columna < COLUMNAS; columna++) {
                grilla[fila][columna] = Casilla.desdeSimbolo(linea.charAt(columna));//charAt devuelve el caracter en la posicion columna de la linea,
                                                                                    // y desdeSimbolo devuelve la casilla correspondiente al simbolo
            }
        }
        return new Tablero(grilla);
    }
}