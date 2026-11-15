package batallanaval.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import batallanaval.excepciones.PersistenciaException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfiguracionJuegoTest {

    /** Escribe un archivo de configuración con el texto dado y lo devuelve. */
    private Path escribirArchivo(Path carpeta, String contenido) throws IOException {
        Path archivo = carpeta.resolve("config.properties");
        Files.writeString(archivo, contenido);
        return archivo;
    }

    // ---------- valores por defecto ----------

    @Test
    void sinArchivoUsaLosValoresPorDefecto(@TempDir Path temp) {
        ConfiguracionJuego config = ConfiguracionJuego.cargar(temp.resolve("config.properties"));

        assertEquals("Jugador 1", config.getUltimoJugador1());
        assertEquals("Jugador 2", config.getUltimoJugador2());
        assertTrue(config.isAutoguardado());
        assertEquals(RutasApp.PARTIDAS, config.getCarpetaPartidas());
    }

    @Test
    void sinArchivoLoCreaConLosValoresPorDefecto(@TempDir Path temp) {
        Path archivo = temp.resolve("config.properties");

        ConfiguracionJuego.cargar(archivo);

        assertTrue(Files.exists(archivo));
    }

    @Test
    void creaLaCarpetaSiNoExiste(@TempDir Path temp) {
        Path archivo = temp.resolve("datos").resolve("config.properties");

        ConfiguracionJuego.cargar(archivo);

        assertTrue(Files.exists(archivo));
    }

    // ---------- guardar y cargar ----------

    @Test
    void guardarYVolverACargarConservaLosValores(@TempDir Path temp) throws PersistenciaException {
        Path archivo = temp.resolve("config.properties");
        ConfiguracionJuego config = ConfiguracionJuego.cargar(archivo);
        config.setUltimoJugador1("Bota");
        config.setUltimoJugador2("Santi");
        config.setAutoguardado(false);
        config.guardar();

        ConfiguracionJuego cargada = ConfiguracionJuego.cargar(archivo);

        assertEquals("Bota", cargada.getUltimoJugador1());
        assertEquals("Santi", cargada.getUltimoJugador2());
        assertFalse(cargada.isAutoguardado());
    }

    @Test
    void leeUnArchivoEscritoAMano(@TempDir Path temp) throws IOException {
        Path archivo = escribirArchivo(temp,
                "jugador1.ultimo=Marcos\njugador2.ultimo=Bruno\nautoguardado=false\ncarpeta.partidas=otra/carpeta\n");

        ConfiguracionJuego config = ConfiguracionJuego.cargar(archivo);

        assertEquals("Marcos", config.getUltimoJugador1());
        assertEquals("Bruno", config.getUltimoJugador2());
        assertFalse(config.isAutoguardado());
        assertEquals(Path.of("otra/carpeta"), config.getCarpetaPartidas());
    }

    @Test
    void losNombresConTildesSeGuardanBien(@TempDir Path temp) throws PersistenciaException {
        Path archivo = temp.resolve("config.properties");
        ConfiguracionJuego config = ConfiguracionJuego.cargar(archivo);
        config.setUltimoJugador1("Agustín");
        config.guardar();

        assertEquals("Agustín", ConfiguracionJuego.cargar(archivo).getUltimoJugador1());
    }

    // ---------- archivo roto o incompleto ----------

    @Test
    void unaClaveFaltanteTomaSuValorPorDefecto(@TempDir Path temp) throws IOException {
        Path archivo = escribirArchivo(temp, "jugador1.ultimo=Marcos\n");

        ConfiguracionJuego config = ConfiguracionJuego.cargar(archivo);

        assertEquals("Marcos", config.getUltimoJugador1());     // la que estaba se respeta
        assertEquals("Jugador 2", config.getUltimoJugador2()); // las que faltan, por defecto
        assertTrue(config.isAutoguardado());
    }

    @Test
    void unAutoguardadoInvalidoQuedaEnTrue(@TempDir Path temp) throws IOException {
        Path archivo = escribirArchivo(temp, "autoguardado=quizas\n");

        assertTrue(ConfiguracionJuego.cargar(archivo).isAutoguardado());
    }

    @Test
    void unNombreVacioTomaElValorPorDefecto(@TempDir Path temp) throws IOException {
        Path archivo = escribirArchivo(temp, "jugador1.ultimo=   \n");

        assertEquals("Jugador 1", ConfiguracionJuego.cargar(archivo).getUltimoJugador1());
    }

    @Test
    void unArchivoRotoUsaLosValoresPorDefectoSinLanzarExcepcion(@TempDir Path temp) throws IOException {
        Path archivo = escribirArchivo(temp, "jugador1.ultimo=Marcos\\uZZZZ\n"); // escape \\u inválido

        ConfiguracionJuego config = ConfiguracionJuego.cargar(archivo);

        assertEquals("Jugador 1", config.getUltimoJugador1());
        assertTrue(config.isAutoguardado());
    }

    @Test
    void unArchivoRotoSeReescribeArreglado(@TempDir Path temp) throws IOException {
        Path archivo = escribirArchivo(temp, "autoguardado=quizas\n");

        ConfiguracionJuego.cargar(archivo);

        String contenido = Files.readString(archivo);
        assertTrue(contenido.contains("autoguardado=true"));
    }

    @Test
    void siNoSePuedeLeerNiEscribirIgualDevuelveDefaults(@TempDir Path temp) {
        ConfiguracionJuego config = ConfiguracionJuego.cargar(temp); // temp es una carpeta, no un archivo

        assertEquals("Jugador 1", config.getUltimoJugador1());
    }

    // ---------- errores ----------

    @Test
    void guardarSobreUnaCarpetaLanzaPersistenciaException(@TempDir Path temp) {
        ConfiguracionJuego config = ConfiguracionJuego.cargar(temp);
        try {
            config.guardar();
            fail("Debería haber lanzado PersistenciaException");
        } catch (PersistenciaException e) {
            assertTrue(e.getCause() instanceof IOException);
        }
    }

    @Test
    void unNombreVacioEnElSetterLanzaExcepcion(@TempDir Path temp) {
        ConfiguracionJuego config = ConfiguracionJuego.cargar(temp.resolve("config.properties"));
        try {
            config.setUltimoJugador1("   ");
            fail("Debería haber lanzado IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // esperado
        }
    }

    @Test
    void elSetterSacaLosEspaciosDeLosCostados(@TempDir Path temp) {
        ConfiguracionJuego config = ConfiguracionJuego.cargar(temp.resolve("config.properties"));
        config.setUltimoJugador2("  Guille  ");
        assertEquals("Guille", config.getUltimoJugador2());
    }
}