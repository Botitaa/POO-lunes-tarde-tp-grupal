package batallanaval.modelo;

import batallanaval.excepciones.MovimientoInvalidoException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Modela un jugador: administra su flota de 6 barcos, puntos de movimiento y de dominación.
 */
public class Jugador {

    private final int numero;
    private final String nombre;
    private final List<Barco> flota;
    private final Map<Integer, Barco> barcosPorId;
    private int puntosMovimiento;
    private int puntosDominacion;
    private final ResumenTurno resumen;

    /** Constructor inicial con flota estándar (6 barcos). */
    public Jugador(int numero, String nombre, List<Barco> flota) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("el nombre no puede ser vacio o null");
        }
        if (numero != 1 && numero != 2) {
            throw new IllegalArgumentException("el numero del jugador tiene que ser 1 o 2");
        }
        if (flota == null || flota.isEmpty()) {
            throw new IllegalArgumentException("la flota no puede estar vacia");
        }

        this.numero = numero;
        this.nombre = nombre;
        this.flota = List.copyOf(flota);
        this.resumen = new ResumenTurno();
        this.barcosPorId = new HashMap<>();

        for (Barco barco : this.flota) {
            if (barcosPorId.put(barco.getId(), barco) != null) {
                throw new IllegalArgumentException("hay dos barcos con el mismo id: " + barco.getId());
            }
        }
    }

    /** Constructor para restaurar jugador desde guardado con puntos y mensajes. */
    public Jugador(int numero, String nombre, List<Barco> flota, int puntosMovimiento, int puntosDominacion, List<String> resumenPendiente) {
        this(numero, nombre, flota);
        this.puntosMovimiento = puntosMovimiento;
        this.puntosDominacion = puntosDominacion;
        if (resumenPendiente != null) {
            for (String mensaje : resumenPendiente) {
                this.resumen.agregarMensaje(mensaje);
            }
        }
    }

    /** Constructor para restaurar jugador desde guardado sin mensajes pendientes. */
    public Jugador(int numero, String nombre, List<Barco> flota, int puntosMovimiento, int puntosDominacion) {
        this(numero, nombre, flota, puntosMovimiento, puntosDominacion, List.of());
    }

    public ResumenTurno getResumen() {
        return resumen;
    }

    public int getPuntosDominacion() {
        return puntosDominacion;
    }

    public int getPuntosMovimiento() {
        return puntosMovimiento;
    }

    /** Busca un barco por ID en O(1) usando el mapa interno. */
    public Barco getBarcoPorId(int id) {
        return barcosPorId.get(id);
    }

    public List<Barco> getFlota() {
        return List.copyOf(flota);
    }

    public String getNombre() {
        return nombre;
    }

    public int getNumero() {
        return numero;
    }

    /** Lista de barcos con vida mayor a cero. */
    public List<Barco> getBarcosVivos() {
        List<Barco> barcosVivos = new ArrayList<>();
        for (Barco barco : flota) {
            if (!barco.estaHundido()) {
                barcosVivos.add(barco);
            }
        }
        return barcosVivos;
    }

    public boolean tieneBarcosVivos() {
        for (Barco barco : flota) {
            if (!barco.estaHundido()) {
                return true;
            }
        }
        return false;
    }

    public int getVidaTotal() {
        int vidaTotal = 0;
        for (Barco barco : flota) {
            vidaTotal += barco.getVida();
        }
        return vidaTotal;
    }

    public boolean tienePuntos(int n) {
        return puntosMovimiento >= n;
    }

    /** Descuenta puntos de movimiento. Lanza MovimientoInvalidoException si no alcanzan. */
    public void gastarPuntos(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("tu movimiento no puede ser negativo");
        } else if (puntosMovimiento < n) {
            throw new MovimientoInvalidoException("no tenes puntos suficientes");
        } else {
            puntosMovimiento -= n;
        }
    }

    /** Fija los puntos de movimiento al inicio del turno (típicamente 6). */
    public void reponerPuntos(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("tu movimiento no puede ser negativo");
        } else {
            puntosMovimiento = n;
        }
    }

    /** Suma 1 punto de dominación por controlar la zona central. */
    public void sumarPuntoDominacion() {
        puntosDominacion += 1;
    }
}
