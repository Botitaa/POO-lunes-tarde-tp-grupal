package batallanaval.persistencia;

import batallanaval.controlador.DatosBarco;
import batallanaval.controlador.DatosJugador;
import batallanaval.controlador.GestorPartida;
import batallanaval.controlador.InstantaneaPartida;
import batallanaval.excepciones.PersistenciaException;
import batallanaval.historial.EventoPartida;
import batallanaval.modelo.Posicion;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class RepositorioPartidasArchivo implements RepositorioPartidas {

    public static final int VERSION_FORMATO = 1;

    private final Path carpeta;

    public RepositorioPartidasArchivo() {
        this(RutasApp.PARTIDAS);
    }

    public RepositorioPartidasArchivo(Path carpeta) {
        this.carpeta = carpeta;
    }

    @Override
    public void guardar(GestorPartida partida, String nombre) throws PersistenciaException {
        if (nombre == null || !nombre.matches("^[a-zA-Z0-9_-]{1,40}$")) {
            throw new PersistenciaException("Nombre de partida invalido: " + nombre);
        }

        if (partida == null || !partida.puedeGuardarse()) {
            throw new PersistenciaException("La partida no se puede guardar en su estado actual.");
        }

        InstantaneaPartida instantanea = partida.crearInstantanea();

        try {
            Files.createDirectories(carpeta);
        } catch (IOException e) {
            throw new PersistenciaException("No se pudo crear la carpeta de guardado: " + carpeta, e);
        }

        Path archivo = carpeta.resolve(nombre + ".sav");

        try (BufferedWriter writer = Files.newBufferedWriter(archivo)) {
            writer.write("BATALLA-NAVAL;" + VERSION_FORMATO);
            writer.newLine();

            String j1 = instantanea.jugadores().size() > 0 ? instantanea.jugadores().get(0).nombre() : "";
            String j2 = instantanea.jugadores().size() > 1 ? instantanea.jugadores().get(1).nombre() : "";
            LocalDateTime fecha = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

            writer.write("CABECERA;" + nombre + ";" + fecha + ";" + j1 + ";" + j2 + ";" + instantanea.ronda() + ";" + instantanea.fase().name());
            writer.newLine();

            writer.write("PARTIDA;" + instantanea.mapa() + ";" + instantanea.jugadorActivo());
            writer.newLine();

            for (DatosJugador dj : instantanea.jugadores()) {
                writer.write("JUGADOR;" + dj.numero() + ";" + dj.nombre() + ";" + dj.puntosMovimiento() + ";" + dj.puntosDominacion());
                writer.newLine();
            }

            for (DatosJugador dj : instantanea.jugadores()) {
                if (dj.resumenPendiente() != null) {
                    for (String mensaje : dj.resumenPendiente()) {
                        writer.write("RESUMEN;" + dj.numero() + ";" + mensaje);
                        writer.newLine();
                    }
                }
            }

            for (DatosBarco db : instantanea.barcos()) {
                int fila = db.posicion() != null ? db.posicion().getFila() : -1;
                int columna = db.posicion() != null ? db.posicion().getColumna() : -1;
                writer.write("BARCO;" + db.id() + ";" + db.tipo() + ";" + db.jugador() + ";" + fila + ";" + columna + ";" + db.vida() + ";" + db.cooldownDisparo() + ";" + db.cooldownHabilidad() + ";" + db.casillasRestantes() + ";" + db.habilidadActiva() + ";" + db.revelado());
                writer.newLine();
            }

            if (instantanea.restos() != null) {
                for (Posicion resto : instantanea.restos()) {
                    writer.write("RESTOS;" + resto.getFila() + ";" + resto.getColumna());
                    writer.newLine();
                }
            }

            if (instantanea.eventos() != null) {
                for (EventoPartida ev : instantanea.eventos()) {
                    writer.write("EVENTO;" + ev.ronda() + ";" + ev.numeroJugador() + ";" + ev.tipo().name() + ";" + ev.momento() + ";" + ev.descripcion());
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            throw new PersistenciaException("Error al guardar el archivo: " + archivo, e);
        }
    }

    @Override
    public GestorPartida cargar(String nombre) throws PersistenciaException {
        throw new UnsupportedOperationException("Pendiente");
    }

    @Override
    public List<CabeceraGuardado> listar() throws PersistenciaException {
        throw new UnsupportedOperationException("Pendiente");
    }

    @Override
    public void borrar(String nombre) throws PersistenciaException {
        throw new UnsupportedOperationException("Pendiente");
    }

    @Override
    public boolean existe(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return false;
        }
        return Files.exists(carpeta.resolve(nombre + ".sav"));
    }
}
