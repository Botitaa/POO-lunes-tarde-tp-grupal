package batallanaval.persistencia;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import batallanaval.excepciones.MapaInvalidoException;
import batallanaval.excepciones.PersistenciaException;
import batallanaval.modelo.Casilla;
import batallanaval.modelo.Tablero;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import org.junit.jupiter.api.Test;

class LectorMapasTest {

    private static final String FILA_AGUA = "~ ~ ~ ~ ~ ~ ~ ~ ~ ~";

    /** Arma un mapa de texto con 10 filas de agua, cambiando una fila por otra. */
    private String mapaConFila(int numeroFila, String filaDistinta) {
        String texto = "";
        for (int fila = 0; fila < Tablero.FILAS; fila++) {
            if (fila == numeroFila) {
                texto = texto + filaDistinta + "\n";
            } else {
                texto = texto + FILA_AGUA + "\n";
            }
        }
        return texto;
    }

    /** Lee un mapa desde un texto en vez de un archivo. */
    private Tablero leerTexto(String texto) throws IOException, PersistenciaException {
        BufferedReader lector = new BufferedReader(new StringReader(texto));
        return LectorMapas.leer(lector, "prueba");
    }

    // ---------- mapa estándar ----------

    @Test
    void cargaElMapaEstandarDelArchivo() throws PersistenciaException {
        Tablero tablero = LectorMapas.cargar(LectorMapas.MAPA_ESTANDAR);
        assertSame(Casilla.ISLA, tablero.getCasilla(3, 2));
        assertSame(Casilla.PIEDRA, tablero.getCasilla(3, 7));
        assertSame(Casilla.AGUA, tablero.getCasilla(0, 0));
    }

    @Test
    void elArchivoEstandarEsIgualAlMapaProvisorio() throws PersistenciaException {
        Tablero delArchivo = LectorMapas.cargar(LectorMapas.MAPA_ESTANDAR);
        Tablero provisorio = Tablero.crearMapaEstandar();
        for (int fila = 0; fila < Tablero.FILAS; fila++) {
            for (int columna = 0; columna < Tablero.COLUMNAS; columna++) {
                assertSame(provisorio.getCasilla(fila, columna), delArchivo.getCasilla(fila, columna));
            }
        }
    }

    @Test
    void unMapaQueNoExisteLanzaPersistenciaException() {
        try {
            LectorMapas.cargar("no-existe");
            fail("Debería haber lanzado PersistenciaException");
        } catch (PersistenciaException e) {
            assertTrue(e.getMessage().contains("no-existe"));
        }
    }

    // ---------- formato válido ----------

    @Test
    void leeUnMapaValidoDesdeTexto() throws IOException, PersistenciaException {
        Tablero tablero = leerTexto(mapaConFila(4, "~ ~ ~ ~ ~ I ~ ~ ~ ~"));
        assertSame(Casilla.ISLA, tablero.getCasilla(4, 5));
        assertSame(Casilla.AGUA, tablero.getCasilla(4, 4));
    }

    @Test
    void ignoraLasLineasVacias() throws IOException, PersistenciaException {
        Tablero tablero = leerTexto("\n" + mapaConFila(4, "~ ~ ~ ~ ~ P ~ ~ ~ ~") + "\n\n");
        assertSame(Casilla.PIEDRA, tablero.getCasilla(4, 5));
    }

    // ---------- errores de formato ----------

    @Test
    void unaFilaConMenosSimbolosIndicaLaLinea() throws IOException {
        try {
            leerTexto(mapaConFila(3, "~ ~ ~ ~ ~ ~ ~ ~ ~"));
            fail("Debería haber lanzado PersistenciaException");
        } catch (PersistenciaException e) {
            assertTrue(e.getMessage().contains("linea 4")); // la fila 3 es la línea 4 del archivo
        }
    }

    @Test
    void unSimboloDesconocidoIndicaLineaYColumna() throws IOException {
        try {
            leerTexto(mapaConFila(5, "~ ~ Z ~ ~ ~ ~ ~ ~ ~"));
            fail("Debería haber lanzado PersistenciaException");
        } catch (PersistenciaException e) {
            assertTrue(e.getMessage().contains("linea 6"));
            assertTrue(e.getMessage().contains("columna 2"));
            assertTrue(e.getCause() instanceof IllegalArgumentException);
        }
    }

    @Test
    void simbolosPegadosSinEspaciosLanzanExcepcion() throws IOException {
        try {
            leerTexto(mapaConFila(0, "~~~~~~~~~~"));
            fail("Debería haber lanzado PersistenciaException");
        } catch (PersistenciaException e) {
            assertTrue(e.getMessage().contains("linea 1"));
        }
    }

    @Test
    void menosDeDiezFilasLanzaExcepcion() throws IOException {
        try {
            leerTexto(FILA_AGUA + "\n" + FILA_AGUA + "\n");
            fail("Debería haber lanzado PersistenciaException");
        } catch (PersistenciaException e) {
            assertTrue(e.getMessage().contains("2 filas"));
        }
    }

    @Test
    void masDeDiezFilasIndicaLaLinea() throws IOException {
        try {
            leerTexto(mapaConFila(0, FILA_AGUA) + FILA_AGUA + "\n");
            fail("Debería haber lanzado PersistenciaException");
        } catch (PersistenciaException e) {
            assertTrue(e.getMessage().contains("linea 11"));
        }
    }

    @Test
    void unArchivoVacioLanzaExcepcion() throws IOException {
        try {
            leerTexto("");
            fail("Debería haber lanzado PersistenciaException");
        } catch (PersistenciaException e) {
            assertTrue(e.getMessage().contains("0 filas"));
        }
    }

    // ---------- reglas del tablero ----------

    @Test
    void unObstaculoEnZonaDeDespliegueLanzaPersistenciaException() throws IOException {
        try {
            leerTexto(mapaConFila(1, "~ ~ I ~ ~ ~ ~ ~ ~ ~"));
            fail("Debería haber lanzado PersistenciaException");
        } catch (PersistenciaException e) {
            assertTrue(e.getCause() instanceof MapaInvalidoException);
        }
    }
}