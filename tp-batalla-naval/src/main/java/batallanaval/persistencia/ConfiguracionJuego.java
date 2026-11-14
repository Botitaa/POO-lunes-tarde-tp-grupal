package batallanaval.persistencia;

import batallanaval.excepciones.PersistenciaException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Properties;

public class ConfiguracionJuego {
    /// Claves de las propiedades
    private static final String CLAVE_JUGADOR1 = "jugador1.ultimo";
    private static final String CLAVE_JUGADOR2 = "jugador2.ultimo";
    private static final String CLAVE_AUTOGUARDADO = "autoguardado";
    private static final String CLAVE_CARPETA_PARTIDAS = "carpeta.partidas";
    /// Valores por defecto
    private static final String DEFAULT_JUGADOR1 = "Jugador 1";
    private static final String DEFAULT_JUGADOR2 = "Jugador 2";
    private static final boolean DEFAULT_AUTOGUARDADO = true;
    private static final String DEFAULT_CARPETA_PARTIDAS = RutasApp.PARTIDAS.toString();
    private static final String COMENTARIO = "Configuración del juego";

    private final Path archivo;
    private final Properties propiedades;

    private ConfiguracionJuego(Path archivo, Properties propiedades) {
        this.archivo = archivo;
        this.propiedades = propiedades;
    }

    public static ConfiguracionJuego cargar() {
        return cargar(RutasApp.CONFIGURACION);
    }

    /// carga la configuración desde un archivo, creando uno nuevo con valores por defecto si no existe o está corrupto.
    public static ConfiguracionJuego cargar(Path archivo) {
        Properties propiedades = new Properties();
        boolean hayQueReescribir = false;
        if (Files.exists(archivo)) {
            try (BufferedReader lector = new BufferedReader(new FileReader(archivo.toFile()))) {
                propiedades.load(lector);
            } catch (IOException | IllegalArgumentException e) {
                propiedades = new Properties();
                hayQueReescribir = true;
            }
        } else {
            hayQueReescribir = true;
        }
        ConfiguracionJuego configuracion = new ConfiguracionJuego(archivo, propiedades);
        if (configuracion.completarValoresFaltantes()) {
            hayQueReescribir = true;
        }
        if (hayQueReescribir) {
            try {
                configuracion.guardar();
            } catch (PersistenciaException e) {
                // no se puede guardar la configuración, pero no se lanza excepción para no interrumpir el flujo del juego
            }
        }
        return configuracion;
    }
//guarda la conf del archivo correspondiente y crea las carpetas si no existen
    public void guardar() throws PersistenciaException {
        try {
            Path carpeta = archivo.getParent();
            if (carpeta != null) {
                Files.createDirectories(carpeta);
            }
            try (BufferedWriter escritor = new BufferedWriter(new FileWriter(archivo.toFile()))) {
                propiedades.store(escritor, COMENTARIO);
            }
        } catch (IOException e) {
            throw new PersistenciaException("No se pudo guardar la configuración en " + archivo, e);
        }
    }

    public String getUltimoJugador1() {
        return propiedades.getProperty(CLAVE_JUGADOR1);
    }

    public void setUltimoJugador1(String nombre) {
        propiedades.setProperty(CLAVE_JUGADOR1, validarNombre(nombre));
    }

    public String getUltimoJugador2() {
        return propiedades.getProperty(CLAVE_JUGADOR2);
    }

    public void setUltimoJugador2(String nombre) {
        propiedades.setProperty(CLAVE_JUGADOR2, validarNombre(nombre));
    }

    public boolean isAutoguardado() {
        return Boolean.parseBoolean(propiedades.getProperty(CLAVE_AUTOGUARDADO));
    }

    public void setAutoguardado(boolean autoguardado) {
        propiedades.setProperty(CLAVE_AUTOGUARDADO, String.valueOf(autoguardado));
    }

    public Path getCarpetaPartidas() {
        return Path.of(propiedades.getProperty(CLAVE_CARPETA_PARTIDAS));
    }

    //completa los valores faltantes con los valores default
    private boolean completarValoresFaltantes() {
        boolean cambio = false;
        if (estaVacio(propiedades.getProperty(CLAVE_JUGADOR1))) {
            propiedades.setProperty(CLAVE_JUGADOR1, DEFAULT_JUGADOR1);
            cambio = true;
        }
        if (estaVacio(propiedades.getProperty(CLAVE_JUGADOR2))) {
            propiedades.setProperty(CLAVE_JUGADOR2, DEFAULT_JUGADOR2);
            cambio = true;
        }
        String autoguardado = propiedades.getProperty(CLAVE_AUTOGUARDADO);
        if (!"true".equals(autoguardado) && !"false".equals(autoguardado)) {
            propiedades.setProperty(CLAVE_AUTOGUARDADO, String.valueOf(DEFAULT_AUTOGUARDADO));
            cambio = true;
        }
        if (estaVacio(propiedades.getProperty(CLAVE_CARPETA_PARTIDAS))) {
            propiedades.setProperty(CLAVE_CARPETA_PARTIDAS, DEFAULT_CARPETA_PARTIDAS);
            cambio = true;
        }
        return cambio;
    }

    private static boolean estaVacio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }

    private static String validarNombre(String nombre) {
        if (estaVacio(nombre)) {
            throw new IllegalArgumentException("El nombre del jugador no puede estar vacío");
        }
        return nombre.trim();
    }
}