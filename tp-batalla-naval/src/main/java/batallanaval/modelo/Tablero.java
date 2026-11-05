package batallanaval.modelo;

import batallanaval.excepciones.MapaInvalidoException;

import java.util.ArrayList;
import java.util.List;

/**
 * Grilla 10x10 de Casilla. Administra zonas de despliegue, dominación central,
 * obstáculos fijos, línea de visión y casillas de restos.
 */
public class Tablero {
    public static final int FILAS = 10;
    public static final int COLUMNAS = 10;
    private final Casilla[][] grilla;

    private static final int ULTIMA_FILA_DESPLIEGUE_J1 = 2;
    private static final int PRIMERA_FILA_DESPLIEGUE_J2 = 7;
    private static final int PRIMERA_FILA_DOMINACION = 4;
    private static final int ULTIMA_FILA_DOMINACION = 5;
    private static final int PRIMERA_COLUMNA_DOMINACION = 4;
    private static final int ULTIMA_COLUMNA_DOMINACION = 5;

    private static final String[] MAPA_ESTANDAR = {
            "~~~~~~~~~~",
            "~~~~~~~~~~",
            "~~~~~~~~~~",
            "~~II~~~P~~",
            "~~I~~~P~~~",
            "~~~P~~~I~~",
            "~~P~~~II~~",
            "~~~~~~~~~~",
            "~~~~~~~~~~",
            "~~~~~~~~~~"
    };

    /** Crea el tablero con copia defensiva. Valida dimensiones 10x10 y que no haya obstáculos en zonas de despliegue. */
    public Tablero(Casilla[][] grilla) {
        validarMapa(grilla);
        this.grilla = new Casilla[FILAS][COLUMNAS];
        for (int fila = 0; fila < FILAS; fila++) {
            for (int columna = 0; columna < COLUMNAS; columna++) {
                this.grilla[fila][columna] = grilla[fila][columna];
            }
        }
    }

    /** Verifica si la fila y columna están dentro de los límites [0, 9]. */
    public boolean estaDentro(int fila, int columna) {
        return fila >= 0 && fila < FILAS && columna >= 0 && columna < COLUMNAS;
    }

    /** Verifica si la posición está dentro de los límites del tablero. */
    public boolean estaDentro(Posicion posicion) {
        return estaDentro(posicion.getFila(), posicion.getColumna());
    }

    /** Casilla en (fila, columna). Lanza IllegalArgumentException si está fuera. */
    public Casilla getCasilla(int fila, int columna) {
        if (!estaDentro(fila, columna)) {
            throw new IllegalArgumentException("La posición (" + fila + "," + columna + ") está fuera del tablero");
        }
        return grilla[fila][columna];
    }

    /** Casilla en la posición indicada. */
    public Casilla getCasilla(Posicion pos) {
        return getCasilla(pos.getFila(), pos.getColumna());
    }

    /** Transitable si está dentro y no bloquea paso (AGUA o RESTOS son transitables; PIEDRA o ISLA no). */
    public boolean esTransitable(Posicion pos) {
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
        for (int fila = 0; fila < FILAS; fila++) {
            if (grilla[fila] == null || grilla[fila].length != COLUMNAS) {
                throw new MapaInvalidoException("La fila " + fila + "debe tener " + COLUMNAS + " columnas");
            }
            for (int columna = 0; columna < COLUMNAS; columna++) {
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

    /** Genera el mapa estándar de 10 obstáculos (islas y piedras) en zona neutral. */
    public static Tablero crearMapaEstandar() {
        Casilla[][] grilla = new Casilla[FILAS][COLUMNAS];
        for (int fila = 0; fila < FILAS; fila++) {
            String linea = MAPA_ESTANDAR[fila];
            for (int columna = 0; columna < COLUMNAS; columna++) {
                grilla[fila][columna] = Casilla.desdeSimbolo(linea.charAt(columna));
            }
        }
        return new Tablero(grilla);
    }

    /** Zona de despliegue: Jugador 1 filas 0 a 2; Jugador 2 filas 7 a 9. */
    public boolean esZonaDespliegue(Posicion pos, int numeroJugador) {
        if (numeroJugador != 1 && numeroJugador != 2) {
            throw new IllegalArgumentException("El número de jugador debe ser 1 o 2");
        }
        if (!estaDentro(pos)) {
            return false;
        }
        if (numeroJugador == 1) {
            return pos.getFila() <= ULTIMA_FILA_DESPLIEGUE_J1;
        } else {
            return pos.getFila() >= PRIMERA_FILA_DESPLIEGUE_J2;
        }
    }

    /** Zona de dominación: las 4 casillas centrales (filas 4-5, columnas 4-5). */
    public boolean esZonaDominacion(Posicion pos) {
        return pos.getFila() >= PRIMERA_FILA_DOMINACION && pos.getFila() <= ULTIMA_FILA_DOMINACION &&
               pos.getColumna() >= PRIMERA_COLUMNA_DOMINACION && pos.getColumna() <= ULTIMA_COLUMNA_DOMINACION;
    }

    /** Devuelve vecinos transitables adyacentes en las 8 direcciones (no incluye obstáculos). */
    public List<Posicion> getVecinos(Posicion pos) {
        if (!estaDentro(pos)) {
            throw new IllegalArgumentException("La posición " + pos + " está fuera del tablero");
        }
        List<Posicion> vecinos = new ArrayList<>();
        for (int fila = pos.getFila() - 1; fila <= pos.getFila() + 1; fila++) {
            for (int columna = pos.getColumna() - 1; columna <= pos.getColumna() + 1; columna++) {
                Posicion vecina = new Posicion(fila, columna);
                boolean esLaMismaPosicion = fila == pos.getFila() && columna == pos.getColumna();
                if (!esLaMismaPosicion && esTransitable(vecina)) {
                    vecinos.add(vecina);
                }
            }
        }
        return vecinos;
    }

    /** Bloqueada si las dos esquinas intermedias entre desde y hasta bloquean el paso. */
    public boolean esDiagonalBloqueada(Posicion desde, Posicion hasta) {
        if (!estaDentro(desde) || !estaDentro(hasta)) {
            throw new IllegalArgumentException("Una de las posiciones está fuera del tablero");
        }
        if (desde.distanciaChebyshev(hasta) != 1) {
            throw new IllegalArgumentException("Las posiciones" + desde + " y " + hasta + " no son adyacentes");
        }
        boolean esDiagonal = desde.getFila() != hasta.getFila() && desde.getColumna() != hasta.getColumna();
        if (!esDiagonal) {
            return false;
        }
        Casilla esquina1 = getCasilla(desde.getFila(), hasta.getColumna());
        Casilla esquina2 = getCasilla(hasta.getFila(), desde.getColumna());
        return esquina1.bloqueaPaso() && esquina2.bloqueaPaso();
    }

    /** Convierte una casilla en RESTOS al hundirse un barco en ella. */
    public void convertirEnRestos(Posicion pos) {
        if (!estaDentro(pos)) {
            throw new IllegalArgumentException("La posición está fuera del tablero");
        }
        grilla[pos.getFila()][pos.getColumna()] = Casilla.RESTOS;
    }

    /** Lista de casillas convertidas en RESTOS para guardar en la instantánea. */
    public List<Posicion> getRestos() {
        List<Posicion> restos = new ArrayList<>();
        for (int fila = 0; fila < FILAS; fila++) {
            for (int columna = 0; columna < COLUMNAS; columna++) {
                if (grilla[fila][columna] == Casilla.RESTOS) {
                    restos.add(new Posicion(fila, columna));
                }
            }
        }
        return List.copyOf(restos);
    }
}
