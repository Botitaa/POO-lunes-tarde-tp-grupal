package batallanaval.persistencia;

import batallanaval.controlador.DatosBarco;
import batallanaval.controlador.DatosJugador;
import batallanaval.controlador.FasePartida;
import batallanaval.controlador.GestorPartida;
import batallanaval.controlador.InstantaneaPartida;
import batallanaval.excepciones.PersistenciaException;
import batallanaval.historial.EventoPartida;
import batallanaval.historial.TipoEvento;
import batallanaval.modelo.Posicion;
import batallanaval.modelo.Tablero;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

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
        if (nombre == null || !nombre.matches("^[a-zA-Z0-9_-]{1,40}$")) {
            throw new PersistenciaException("Nombre de partida invalido: " + nombre);
        }

        Path archivo = carpeta.resolve(nombre + ".sav");
        if (!Files.exists(archivo)) {
            throw new PersistenciaException("El archivo de partida no existe: " + archivo);
        }

        String mapa = "estandar";
        int jugadorActivo = 1;
        int ronda = 1;
        FasePartida fase = null;

        List<DatosJugadorAux> jugadoresAux = new ArrayList<>();
        Map<Integer, List<String>> resumenes = new HashMap<>();
        List<DatosBarco> barcos = new ArrayList<>();
        List<Posicion> restos = new ArrayList<>();
        List<EventoPartida> eventos = new ArrayList<>();

        int numeroLinea = 0;

        try (BufferedReader reader = Files.newBufferedReader(archivo)) {
            String linea = reader.readLine();
            numeroLinea++;

            if (linea == null) {
                throw new PersistenciaException("Línea 1 del guardado inválida: archivo vacío");
            }

            String[] partesMarca = linea.split(";");
            if (partesMarca.length != 2 || !partesMarca[0].equals("BATALLA-NAVAL")) {
                throw new PersistenciaException("Línea " + numeroLinea + " del guardado inválida: marca incorrecta");
            }

            int version;
            try {
                version = Integer.parseInt(partesMarca[1]);
            } catch (NumberFormatException e) {
                throw new PersistenciaException("Línea " + numeroLinea + " del guardado inválida: versión no numérica", e);
            }

            if (version != VERSION_FORMATO) {
                throw new PersistenciaException("Línea " + numeroLinea + " del guardado inválida: versión no soportada " + version);
            }

            while ((linea = reader.readLine()) != null) {
                numeroLinea++;
                if (linea.isBlank()) {
                    continue;
                }

                try {
                    if (linea.startsWith("CABECERA;")) {
                        String[] partes = linea.split(";");
                        if (partes.length != 7) {
                            throw new PersistenciaException("Línea " + numeroLinea + " del guardado inválida: campos insuficientes en CABECERA");
                        }
                        ronda = Integer.parseInt(partes[5]);
                        fase = FasePartida.valueOf(partes[6]);
                    } else if (linea.startsWith("PARTIDA;")) {
                        String[] partes = linea.split(";");
                        if (partes.length != 3) {
                            throw new PersistenciaException("Línea " + numeroLinea + " del guardado inválida: campos insuficientes en PARTIDA");
                        }
                        mapa = partes[1];
                        jugadorActivo = Integer.parseInt(partes[2]);
                    } else if (linea.startsWith("JUGADOR;")) {
                        String[] partes = linea.split(";");
                        if (partes.length != 5) {
                            throw new PersistenciaException("Línea " + numeroLinea + " del guardado inválida: campos insuficientes en JUGADOR");
                        }
                        int numero = Integer.parseInt(partes[1]);
                        String nom = partes[2];
                        int puntosMov = Integer.parseInt(partes[3]);
                        int puntosDom = Integer.parseInt(partes[4]);
                        jugadoresAux.add(new DatosJugadorAux(numero, nom, puntosMov, puntosDom));
                        resumenes.putIfAbsent(numero, new ArrayList<>());
                    } else if (linea.startsWith("RESUMEN;")) {
                        String[] partes = linea.split(";", 3);
                        if (partes.length != 3) {
                            throw new PersistenciaException("Línea " + numeroLinea + " del guardado inválida: campos insuficientes en RESUMEN");
                        }
                        int numero = Integer.parseInt(partes[1]);
                        String mensaje = partes[2];
                        resumenes.putIfAbsent(numero, new ArrayList<>());
                        resumenes.get(numero).add(mensaje);
                    } else if (linea.startsWith("BARCO;")) {
                        String[] partes = linea.split(";");
                        if (partes.length != 12) {
                            throw new PersistenciaException("Línea " + numeroLinea + " del guardado inválida: campos insuficientes en BARCO");
                        }
                        int id = Integer.parseInt(partes[1]);
                        char tipo = partes[2].charAt(0);
                        int jugador = Integer.parseInt(partes[3]);
                        int fila = Integer.parseInt(partes[4]);
                        int columna = Integer.parseInt(partes[5]);
                        int vida = Integer.parseInt(partes[6]);
                        int cdDisparo = Integer.parseInt(partes[7]);
                        int cdHabilidad = Integer.parseInt(partes[8]);
                        int casillasRestantes = Integer.parseInt(partes[9]);
                        boolean habActiva = Boolean.parseBoolean(partes[10]);
                        boolean revelado = Boolean.parseBoolean(partes[11]);

                        Posicion pos = (fila >= 0 && columna >= 0) ? new Posicion(fila, columna) : null;
                        barcos.add(new DatosBarco(id, tipo, jugador, pos, vida, cdDisparo, cdHabilidad, casillasRestantes, habActiva, revelado));
                    } else if (linea.startsWith("RESTOS;")) {
                        String[] partes = linea.split(";");
                        if (partes.length != 3) {
                            throw new PersistenciaException("Línea " + numeroLinea + " del guardado inválida: campos insuficientes en RESTOS");
                        }
                        int fila = Integer.parseInt(partes[1]);
                        int columna = Integer.parseInt(partes[2]);
                        restos.add(new Posicion(fila, columna));
                    } else if (linea.startsWith("EVENTO;")) {
                        String[] partes = linea.split(";", 6);
                        if (partes.length != 6) {
                            throw new PersistenciaException("Línea " + numeroLinea + " del guardado inválida: campos insuficientes en EVENTO");
                        }
                        int r = Integer.parseInt(partes[1]);
                        int j = Integer.parseInt(partes[2]);
                        TipoEvento tipo = TipoEvento.valueOf(partes[3]);
                        LocalDateTime momento = LocalDateTime.parse(partes[4]);
                        String desc = partes[5];
                        eventos.add(new EventoPartida(r, j, tipo, desc, momento));
                    } else {
                        throw new PersistenciaException("Línea " + numeroLinea + " del guardado inválida: tipo de registro desconocido");
                    }
                } catch (NumberFormatException e) {
                    throw new PersistenciaException("Línea " + numeroLinea + " del guardado inválida: formato numérico incorrecto", e);
                } catch (PersistenciaException e) {
                    throw e;
                } catch (Exception e) {
                    throw new PersistenciaException("Línea " + numeroLinea + " del guardado inválida", e);
                }
            }
        } catch (IOException e) {
            throw new PersistenciaException("Error al leer el archivo de guardado: " + archivo, e);
        }

        if (fase == null) {
            throw new PersistenciaException("El archivo de guardado no contiene cabecera válida.");
        }

        List<DatosJugador> jugadores = new ArrayList<>();
        for (DatosJugadorAux aux : jugadoresAux) {
            jugadores.add(new DatosJugador(
                    aux.numero,
                    aux.nombre,
                    aux.puntosMovimiento,
                    aux.puntosDominacion,
                    resumenes.getOrDefault(aux.numero, List.of())
            ));
        }

        InstantaneaPartida instantanea = new InstantaneaPartida(
                mapa,
                jugadorActivo,
                ronda,
                fase,
                jugadores,
                barcos,
                restos,
                eventos
        );

        Tablero tablero = Tablero.crearMapaEstandar();

        try {
            return GestorPartida.restaurar(instantanea, tablero);
        } catch (IllegalStateException e) {
            throw new PersistenciaException("Error al restaurar partida guardada: " + e.getMessage(), e);
        }
    }

    private record DatosJugadorAux(int numero, String nombre, int puntosMovimiento, int puntosDominacion) {
    }

    @Override
    public List<CabeceraGuardado> listar() throws PersistenciaException {
        if (!Files.exists(carpeta)) {
            return new ArrayList<>();
        }

        List<CabeceraGuardado> cabeceras = new ArrayList<>();

        try (Stream<Path> stream = Files.list(carpeta)) {
            List<Path> archivos = stream
                    .filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".sav"))
                    .toList();

            for (Path archivo : archivos) {
                try (BufferedReader reader = Files.newBufferedReader(archivo)) {
                    String linea1 = reader.readLine();
                    String linea2 = reader.readLine();

                    if (linea1 == null || linea2 == null) {
                        continue;
                    }

                    String[] partes1 = linea1.split(";");
                    if (partes1.length != 2 || !partes1[0].equals("BATALLA-NAVAL")) {
                        continue;
                    }

                    int version = Integer.parseInt(partes1[1]);
                    if (version != VERSION_FORMATO) {
                        continue;
                    }

                    String[] partes2 = linea2.split(";");
                    if (partes2.length != 7 || !partes2[0].equals("CABECERA")) {
                        continue;
                    }

                    String nombre = partes2[1];
                    LocalDateTime fecha = LocalDateTime.parse(partes2[2]);
                    String j1 = partes2[3];
                    String j2 = partes2[4];
                    int ronda = Integer.parseInt(partes2[5]);
                    FasePartida fase = FasePartida.valueOf(partes2[6]);

                    cabeceras.add(new CabeceraGuardado(version, nombre, fecha, j1, j2, ronda, fase));
                } catch (Exception e) {
                    continue;
                }
            }
        } catch (IOException e) {
            throw new PersistenciaException("Error al listar las partidas en " + carpeta, e);
        }

        cabeceras.sort((c1, c2) -> c2.fecha().compareTo(c1.fecha()));
        return cabeceras;
    }

    @Override
    public void borrar(String nombre) throws PersistenciaException {
        if (nombre == null || !nombre.matches("^[a-zA-Z0-9_-]{1,40}$")) {
            throw new PersistenciaException("Nombre de partida invalido: " + nombre);
        }

        Path archivo = carpeta.resolve(nombre + ".sav");
        if (!Files.exists(archivo)) {
            throw new PersistenciaException("El archivo a borrar no existe: " + archivo);
        }

        try {
            Files.delete(archivo);
        } catch (IOException e) {
            throw new PersistenciaException("Error al borrar el archivo: " + archivo, e);
        }
    }

    @Override
    public boolean existe(String nombre) {
        if (nombre == null || !nombre.matches("^[a-zA-Z0-9_-]{1,40}$")) {
            return false;
        }
        return Files.exists(carpeta.resolve(nombre + ".sav"));
    }
}
