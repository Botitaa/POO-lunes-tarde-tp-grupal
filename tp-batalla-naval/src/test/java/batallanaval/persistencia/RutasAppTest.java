package batallanaval.persistencia;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import batallanaval.excepciones.PersistenciaException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RutasAppTest {

    @Test
    void creaLasCarpetasDePartidasEHistoriales(@TempDir Path temp) throws PersistenciaException {
        Path base = temp.resolve("datos");

        RutasApp.crearCarpetas(base);

        assertTrue(Files.isDirectory(base.resolve("partidas")));
        assertTrue(Files.isDirectory(base.resolve("historiales")));
    }

    @Test
    void llamarloDosVecesNoFalla(@TempDir Path temp) throws PersistenciaException {
        Path base = temp.resolve("datos");

        RutasApp.crearCarpetas(base);

        assertDoesNotThrow(() -> RutasApp.crearCarpetas(base));
    }

    @Test
    void siLaBaseEsUnArchivoLanzaPersistenciaConLaCausa(@TempDir Path temp) throws IOException {
        Path archivo = Files.createFile(temp.resolve("datos"));

        PersistenciaException e = assertThrows(PersistenciaException.class,
                () -> RutasApp.crearCarpetas(archivo));

        assertInstanceOf(IOException.class, e.getCause());
    }

    @Test
    void lasRutasCuelganDeBase() {
        assertTrue(RutasApp.PARTIDAS.startsWith(RutasApp.BASE));
        assertTrue(RutasApp.HISTORIALES.startsWith(RutasApp.BASE));
        assertTrue(RutasApp.ESTADISTICAS.startsWith(RutasApp.BASE));
        assertTrue(RutasApp.CONFIGURACION.startsWith(RutasApp.BASE));
    }
}