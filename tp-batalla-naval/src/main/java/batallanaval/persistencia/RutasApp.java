package batallanaval.persistencia;

import batallanaval.excepciones.PersistenciaException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Rutas fijas de los archivos del juego, todas dentro de la carpeta datos.
 */
public final class RutasApp {

    private static final String CARPETA_PARTIDAS = "partidas";
    private static final String CARPETA_HISTORIALES = "historiales";

    public static final Path BASE = Path.of("datos");
    public static final Path PARTIDAS = BASE.resolve(CARPETA_PARTIDAS);
    public static final Path HISTORIALES = BASE.resolve(CARPETA_HISTORIALES);
    public static final Path ESTADISTICAS = BASE.resolve("estadisticas.csv");
    public static final Path CONFIGURACION = BASE.resolve("config.properties");

    private RutasApp() {
    }

    /**
     * Crea las carpetas de datos si no existen.
     *
     * @throws PersistenciaException si no se pueden crear
     */
    public static void crearCarpetas() throws PersistenciaException {
        crearCarpetas(BASE);
    }

    static void crearCarpetas(Path base) throws PersistenciaException {
        try {
            Files.createDirectories(base);
            Files.createDirectories(base.resolve(CARPETA_PARTIDAS));
            Files.createDirectories(base.resolve(CARPETA_HISTORIALES));
        } catch (IOException e) {
            throw new PersistenciaException(
                    "No se pudieron crear las carpetas de datos en " + base.toAbsolutePath(), e);
        }
    }
}
