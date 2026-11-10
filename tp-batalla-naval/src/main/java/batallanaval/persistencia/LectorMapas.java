package batallanaval.persistencia;

import batallanaval.excepciones.PersistenciaException;
import batallanaval.excepciones.MapaInvalidoException;
import batallanaval.modelo.Casilla;
import batallanaval.modelo.Tablero;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;

public class LectorMapas {

    public static final String MAPA_ESTANDAR = "estandar";
    private static final String CARPETA_MAPAS = "/mapas/";
    private static final String EXTENSION  = ".txt";
    private static final String SEPARADOR = " ";

    private LectorMapas() {

    }
    public static Tablero cargar(String nombreMapa) throws PersistenciaException {
        String ruta = CARPETA_MAPAS + nombreMapa + EXTENSION;
        InputStream entrada = LectorMapas.class.getResourceAsStream(ruta);
        if (entrada == null) {
            throw new PersistenciaException("No se pudo encontrar el mapa " + ruta);
        }
        try (BufferedReader lector = new BufferedReader(new InputStreamReader(entrada))) {
            return leer(lector, nombreMapa);
        } catch (IOException e) {
            throw new PersistenciaException("Error al leer el mapa " + ruta, e);
        }
    }
     static Tablero leer(BufferedReader lector, String nombreMapa) throws IOException, PersistenciaException {
        Casilla[][] grilla = new Casilla[Tablero.FILAS][Tablero.COLUMNAS];
        int fila = 0;
        int numeroLinea = 0;
        String linea = lector.readLine();

        while (linea != null) {
            numeroLinea++;
            linea = linea.trim();
            if (!linea.isEmpty()) {
                if(fila == Tablero.FILAS) {
                    throw new PersistenciaException("Mapa " + nombreMapa + ", linea " + numeroLinea + ": el mapa tiene más de " + Tablero.FILAS + " filas");
                }
                leerFila(linea, grilla[fila], nombreMapa, numeroLinea);
                fila++;
            }
            linea = lector.readLine();
        }
        if(fila != Tablero.FILAS) {
            throw new PersistenciaException("El mapa " + nombreMapa + " tiene : " + fila + " filas. Se esperaban " + Tablero.FILAS);
        }
        try {
            return new Tablero(grilla);
        } catch (MapaInvalidoException e) {
            throw new PersistenciaException("El mapa " + nombreMapa + " es inválido: " + e.getMessage(),e);
        }
    }

    private static void leerFila(String linea, Casilla[] filaDestino, String nombreMapa, int numeroLinea) throws PersistenciaException {
        String[] simbolos = linea.split(SEPARADOR);
        if(simbolos.length != Tablero.COLUMNAS) {
            throw new PersistenciaException("El mapa " + nombreMapa + ", linea " + numeroLinea + ": tiene " + simbolos.length + " columnas. Se esperaban " + Tablero.COLUMNAS);
        }
        for (int columna = 0; columna < Tablero.COLUMNAS; columna++) {
            String simbolo = simbolos[columna];
            if(simbolo.length() != 1) {
                throw new PersistenciaException("el mapa " + nombreMapa +", linea " + numeroLinea + ", columna " + columna + " : simbolo invalido '" + simbolo + "'");
            }
            try {
                filaDestino[columna] = Casilla.desdeSimbolo(simbolo.charAt(0));
            } catch (IllegalArgumentException e) {
                throw new PersistenciaException("el mapa " + nombreMapa +", linea " + numeroLinea + ", columna " + columna + " : simbolo invalido '" + simbolo + "'", e);
            }
        }
    }
}

