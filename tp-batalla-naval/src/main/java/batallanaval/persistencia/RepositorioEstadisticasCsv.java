package batallanaval.persistencia;

import batallanaval.controlador.MotivoFin;
import batallanaval.controlador.ResultadoPartida;
import batallanaval.excepciones.PersistenciaException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Implementación de {@link RepositorioEstadisticas} que persiste y recupera resultados
 * de partidas en un archivo de formato CSV con separador punto y coma (;).
 */
public class RepositorioEstadisticasCsv implements RepositorioEstadisticas {

    /**
     * Separador utilizado para delimitar los campos del CSV.
     */
    public static final String SEPARADOR = ";";

    /**
     * Línea de encabezado estándar que identifica las columnas del archivo CSV.
     */
    public static final String ENCABEZADO = "fecha;jugador1;jugador2;ganador;rondas;motivo";

    private final Path archivo;

    /**
     * Crea una instancia que utiliza la ruta predeterminada de estadísticas (datos/estadisticas.csv).
     */
    public RepositorioEstadisticasCsv() {
        this(RutasApp.ESTADISTICAS);
    }

    /**
     * Crea una instancia configurando una ruta de archivo personalizada.
     *
     * @param archivo ruta del archivo CSV a utilizar.
     */
    public RepositorioEstadisticasCsv(Path archivo) {
        this.archivo = Objects.requireNonNull(archivo, "El archivo no puede ser nulo.");
    }

    /**
     * Registra el resultado de una partida agregándolo al final del archivo CSV (append).
     * Si el archivo no existe o se encuentra vacío, escribe la línea de encabezado antes del registro.
     *
     * @param resultado resultado de la partida finalizada.
     * @throws PersistenciaException si el resultado es nulo o si ocurre un fallo al escribir en el archivo.
     */
    @Override
    public void registrar(ResultadoPartida resultado) throws PersistenciaException {
        if (resultado == null) {
            throw new PersistenciaException("El resultado de la partida no puede ser nulo.");
        }

        Path padre = archivo.getParent();
        if (padre != null && !Files.exists(padre)) {
            try {
                Files.createDirectories(padre);
            } catch (IOException e) {
                throw new PersistenciaException("No se pudo crear la carpeta para estadísticas: " + padre, e);
            }
        }

        try {
            boolean escribirEncabezado = !Files.exists(archivo) || Files.size(archivo) == 0;

            try (BufferedWriter writer = Files.newBufferedWriter(archivo,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {

                if (escribirEncabezado) {
                    writer.write(ENCABEZADO);
                    writer.newLine();
                }

                String ganadorStr = resultado.ganador() != null ? resultado.ganador() : "";
                String linea = String.join(SEPARADOR,
                        resultado.fecha().toString(),
                        resultado.jugador1(),
                        resultado.jugador2(),
                        ganadorStr,
                        String.valueOf(resultado.rondas()),
                        resultado.motivo().name()
                );

                writer.write(linea);
                writer.newLine();
            }
        } catch (IOException e) {
            throw new PersistenciaException("Error al registrar resultado en el archivo de estadísticas: " + archivo, e);
        }
    }

    /**
     * Recupera todos los resultados de partidas almacenados en el archivo CSV.
     * Si el archivo no existe, devuelve una lista vacía.
     * Si se detectan líneas rotas, corruptas o con formato inválido, las saltea silenciosamente
     * sin interrumpir la lectura del resto de las entradas válidas.
     *
     * @return lista inmutable con los resultados válidos leídos, en orden de registro.
     * @throws PersistenciaException si ocurre un error de lectura de bajo nivel al acceder al archivo.
     */
    @Override
    public List<ResultadoPartida> obtenerTodos() throws PersistenciaException {
        if (!Files.exists(archivo)) {
            return List.of();
        }

        List<ResultadoPartida> resultados = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(archivo)) {
            String linea;
            boolean primeraLinea = true;

            while ((linea = reader.readLine()) != null) {
                String lineaLimpia = linea.trim();
                if (lineaLimpia.isEmpty()) {
                    continue;
                }

                if (primeraLinea) {
                    primeraLinea = false;
                    if (lineaLimpia.equalsIgnoreCase(ENCABEZADO)) {
                        continue;
                    }
                }

                try {
                    String[] partes = lineaLimpia.split(SEPARADOR, -1);
                    if (partes.length != 6) {
                        continue;
                    }

                    LocalDateTime fecha = LocalDateTime.parse(partes[0].trim());
                    String j1 = partes[1].trim();
                    String j2 = partes[2].trim();
                    if (j1.isEmpty() || j2.isEmpty()) {
                        continue;
                    }

                    String campoGanador = partes[3].trim();
                    String ganador = (campoGanador.isEmpty() || campoGanador.equalsIgnoreCase("null")) ? null : campoGanador;

                    int rondas = Integer.parseInt(partes[4].trim());
                    if (rondas < 0) {
                        continue;
                    }

                    MotivoFin motivo = MotivoFin.valueOf(partes[5].trim());

                    resultados.add(new ResultadoPartida(fecha, j1, j2, ganador, rondas, motivo));
                } catch (Exception e) {
                    // Saltea líneas corruptas, campos incompletos o tipos no parseables
                    continue;
                }
            }
        } catch (IOException e) {
            throw new PersistenciaException("Error al leer el archivo de estadísticas: " + archivo, e);
        }

        return List.copyOf(resultados);
    }
}
