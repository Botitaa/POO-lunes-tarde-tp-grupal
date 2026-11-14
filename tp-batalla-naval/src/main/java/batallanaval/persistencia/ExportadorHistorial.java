package batallanaval.persistencia;
import batallanaval.controlador.ResultadoPartida;
import batallanaval.excepciones.PersistenciaException;
import batallanaval.historial.EventoPartida;
import batallanaval.historial.HistorialPartida;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public final class ExportadorHistorial {
    private static final String EXTENSION = ".txt";
    private ExportadorHistorial() {
    }

    /// Exportación de historial de partida
    public static Path exportar(HistorialPartida historial, ResultadoPartida resultado, Path carpeta) throws PersistenciaException{
        if(historial == null || resultado == null || carpeta == null) {
            throw new IllegalArgumentException("Los parámetros no pueden ser nulos");
        }
        try {
            Files.createDirectories(carpeta);
            Path archivo = elegirArchivoLibre(carpeta, nombreBase(resultado));
            try (BufferedWriter escritor = new BufferedWriter(new FileWriter(archivo.toFile()))) {
                escribirEncabezado(escritor, resultado, historial.cantidad());
                List<EventoPartida> eventos = historial.getEventos();
                for (EventoPartida evento : eventos) {
                    escritor.write(evento.aLinea());
                    escritor.newLine();
                }
            }
            return archivo;
        } catch (IOException e) {
            throw new PersistenciaException("No se pudo exportar el historial en " + carpeta, e);
        }
    }

    static String nombreBase(ResultadoPartida resultado) {
        LocalDateTime fecha = resultado.fecha();
        String fechaYhora = fecha.getYear() + "-" + dosDigitos(fecha.getMonthValue()) + "-" + dosDigitos(fecha.getDayOfMonth()) + "_" + dosDigitos(fecha.getHour()) +  dosDigitos(fecha.getMinute());
        return fechaYhora + "_" + limpiarNombre(resultado.jugador1()) + "-vs-" + limpiarNombre(resultado.jugador2());
    }
    ///elige un nombre de archivo libre en la carpeta, agregando sufijos si es necesario
    private static Path elegirArchivoLibre(Path carpeta, String nombreBase) {
        Path archivo = carpeta.resolve(nombreBase + EXTENSION);
        int contador = 2;
        while (Files.exists(archivo)) {
            archivo = carpeta.resolve(nombreBase + "_" + contador + EXTENSION);
            contador++;
        }
        return archivo;
    }

    private static String limpiarNombre(String nombre) {
        String limpio = "";
        for (int i = 0 ; i < nombre.length(); i++) {
            char letra = nombre.charAt(i);
            if (Character.isLetterOrDigit(letra))  {
                limpio = limpio + letra;
            } else {
                limpio = limpio + "_";
            }
        }
        return limpio;
    }
    //necesario para respetar el formato de fecha agrega un 0 a la izq
    private static String dosDigitos(int numero) {
        if (numero < 10) {
            return "0" + numero;
        }
            return String.valueOf(numero);
    }

    private static void escribirEncabezado(BufferedWriter escritor, ResultadoPartida resultado, int cantidadEventos) throws IOException{
        escritor.write("BATALLA NAVAL TACTICA - HISTORIAL DE PARTIDA");
        escritor.newLine();
        LocalDateTime fecha = resultado.fecha();
        escritor.write("Fecha: " + dosDigitos(fecha.getDayOfMonth()) + "/" + dosDigitos(fecha.getMonthValue()) + "/" + fecha.getYear() + " " + dosDigitos(fecha.getHour()) + ":" + dosDigitos(fecha.getMinute()));
        escritor.newLine();
        escritor.write("Jugadores: " + resultado.jugador1() + " vs " + resultado.jugador2());
        escritor.newLine();
        escritor.write("Resultado: " + textoResultado(resultado));
        escritor.newLine();
        escritor.write("Rondas: " + resultado.rondas());
        escritor.newLine();
        escritor.write("Cantidad de eventos: " + cantidadEventos);
        escritor.newLine();
        escritor.newLine();
    }

    private static String textoResultado(ResultadoPartida resultado) {
        if(resultado.esEmpate()){
            return "Empate (" + resultado.motivo() + ")";
        }
        return "Gana: " + resultado.ganador() + " (" + resultado.motivo() + ")";
    }
}

