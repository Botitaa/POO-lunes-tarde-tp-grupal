package batallanaval.controlador;

import batallanaval.modelo.Barco;
import batallanaval.modelo.Jugador;
import batallanaval.modelo.Posicion;
import batallanaval.modelo.Tablero;

import java.util.List;

public class GestorPartida {

    public static final int TURNOS_MAX = 40;
    public static final int PUNTOS_MOVIMIENTO_TURNO = 6;
    public static final int PUNTOS_DOMINACION_VICTORIA = 6;

    private final Jugador jugador1;
    private final Jugador jugador2;
    private final List<ObservadorPartida> observadores;
    private final Tablero tablero;

    private Jugador jugadorActivo;
    private FasePartida fase;
    private int ronda;
    private Barco barcoSeleccionado;
    private List<Posicion> rutaPlanificada;
    private HistorialPartida historial;
    private ResultadoPartida resultado;
    private boolean accionRealizadaEnTurno;


    public GestorPartida(Tablero tablero, Jugador jugador2, Jugador jugador1) {
        this.tablero = tablero;
        this.jugador2 = jugador2;
        this.jugador1 = jugador1;
    }
}
