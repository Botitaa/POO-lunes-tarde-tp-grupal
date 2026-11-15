package batallanaval.controlador;

import batallanaval.excepciones.MovimientoInvalidoException;
import batallanaval.modelo.FabricaFlota;
import batallanaval.modelo.Jugador;
import batallanaval.modelo.Posicion;
import batallanaval.modelo.Tablero;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests de visibilidad (niebla de guerra) en GestorPartida (PAR-10, #55).
 * <p>
 * Visiones: destructor 2, crucero 3, acorazado 5. Mapa estándar: islas en (3,2), (3,3), (4,2), (5,7),
 * (6,6) y (6,7); zona de dominación en filas 4-5 y columnas 4-5. Siempre juega Ana (J1).
 */
class GestorPartidaVisibilidadTest {

    // ---------- armado ----------

    private static GestorPartida partida(List<DatosBarco> barcos) {
        List<DatosJugador> jugadores = List.of(
                new DatosJugador(1, "Ana", GestorPartida.PUNTOS_MOVIMIENTO_TURNO, 0, List.of()),
                new DatosJugador(2, "Beto", GestorPartida.PUNTOS_MOVIMIENTO_TURNO, 0, List.of()));
        InstantaneaPartida instantanea = new InstantaneaPartida("estandar", 1, 1, FasePartida.EN_JUEGO,
                jugadores, barcos, List.of(), List.of());
        return GestorPartida.restaurar(instantanea, Tablero.crearMapaEstandar());
    }

    private static DatosBarco barco(int id, char tipo, int jugador, int fila, int columna) {
        int vida = tipo == 'A' ? 8 : tipo == 'C' ? 5 : 3;
        int casillas = tipo == 'A' ? 2 : tipo == 'C' ? 3 : 4;
        return new DatosBarco(id, tipo, jugador, new Posicion(fila, columna), vida, 0, 0, casillas, false, false);
    }

    private static DatosBarco hundido(int id, char tipo, int jugador, int fila, int columna) {
        int casillas = tipo == 'A' ? 2 : tipo == 'C' ? 3 : 4;
        return new DatosBarco(id, tipo, jugador, new Posicion(fila, columna), 0, 0, 0, casillas, false, false);
    }

    /** Flota de Ana en la fila 1 (acorazado en (1,0), destructor 4 en (1,3)). */
    private static List<DatosBarco> anaFila1() {
        return List.of(
                barco(1, 'A', 1, 1, 0), barco(2, 'C', 1, 1, 1), barco(3, 'C', 1, 1, 2),
                barco(4, 'D', 1, 1, 3), barco(5, 'D', 1, 1, 4), barco(6, 'D', 1, 1, 5));
    }

    /**
     * Flota de Ana pegada a la fila 0 y con el acorazado hundido, así nadie ve más allá de la fila 3.
     * El crucero 2 se puede ubicar donde pida el test.
     */
    private static List<DatosBarco> anaCorta(int filaCrucero, int columnaCrucero) {
        return List.of(
                hundido(1, 'A', 1, 0, 0), barco(2, 'C', 1, filaCrucero, columnaCrucero), barco(3, 'C', 1, 0, 1),
                barco(4, 'D', 1, 0, 3), barco(5, 'D', 1, 0, 4), barco(6, 'D', 1, 0, 5));
    }

    /** Flota de Beto en la fila 9; el destructor 10 va donde pida el test. */
    private static List<DatosBarco> beto(DatosBarco destructor10) {
        return List.of(
                barco(7, 'A', 2, 9, 0), barco(8, 'C', 2, 9, 1), barco(9, 'C', 2, 9, 2),
                destructor10, barco(11, 'D', 2, 9, 4), barco(12, 'D', 2, 9, 5));
    }

    private static List<DatosBarco> juntar(List<DatosBarco> a, List<DatosBarco> b) {
        List<DatosBarco> todos = new ArrayList<>(a);
        todos.addAll(b);
        return todos;
    }

    private static boolean seVeElBarco(GestorPartida partida, int id) {
        return partida.getBarcosEnemigosVisibles().stream().anyMatch(b -> b.id() == id);
    }

    // ---------- casillas ----------

    @Test
    void lasCasillasDeTusBarcosSeVen() {
        GestorPartida partida = partida(juntar(anaFila1(), beto(barco(10, 'D', 2, 9, 3))));

        assertTrue(partida.esVisible(new Posicion(1, 3)));
    }

    @Test
    void unaCasillaLejosDeTusBarcosNoSeVe() {
        GestorPartida partida = partida(juntar(anaFila1(), beto(barco(10, 'D', 2, 9, 3))));

        assertFalse(partida.esVisible(new Posicion(9, 9)));
    }

    @Test
    void laZonaDeDominacionSeVeSiempre() {
        GestorPartida partida = partida(juntar(anaCorta(0, 2), beto(barco(10, 'D', 2, 9, 3))));

        assertTrue(partida.esVisible(new Posicion(4, 4)));
        assertFalse(partida.esVisible(new Posicion(4, 3))); // al lado, pero fuera de la zona
    }

    @Test
    void unaIslaCortaLaVision() {
        // El crucero 2 en (2,2) llega a (5,2) por distancia (3), pero en el medio están las islas (3,2) y (4,2).
        GestorPartida partida = partida(juntar(anaCorta(2, 2), beto(barco(10, 'D', 2, 9, 3))));

        assertFalse(partida.esVisible(new Posicion(5, 2)));
        assertTrue(partida.esVisible(new Posicion(2, 5))); // misma distancia, sin islas en el medio
    }

    @Test
    void conBarcosSinUbicarDuranteElDespliegueNoRevienta() {
        GestorPartida enDespliegue = new GestorPartida(
                new Jugador(1, "Ana", FabricaFlota.crearFlotaEstandar(1)),
                new Jugador(2, "Beto", FabricaFlota.crearFlotaEstandar(7)),
                Tablero.crearMapaEstandar());

        assertFalse(enDespliegue.esVisible(new Posicion(0, 0)));
        assertTrue(enDespliegue.esVisible(new Posicion(4, 4)));
    }

    // ---------- barcos enemigos ----------

    @Test
    void unEnemigoLejosNoAparece() {
        GestorPartida partida = partida(juntar(anaFila1(), beto(barco(10, 'D', 2, 9, 3))));

        assertTrue(partida.getBarcosEnemigosVisibles().isEmpty());
    }

    @Test
    void unEnemigoDentroDeLaVisionAparece() {
        GestorPartida partida = partida(juntar(anaFila1(), beto(barco(10, 'D', 2, 3, 4))));

        assertTrue(seVeElBarco(partida, 10));
    }

    @Test
    void elEnemigoVisibleVieneComoDelRivalYSinSusCooldowns() {
        List<DatosBarco> barcos = juntar(anaFila1(),
                beto(new DatosBarco(10, 'D', 2, new Posicion(3, 4), 3, 2, 3, 4, false, false)));
        GestorPartida partida = partida(barcos);

        EstadoBarco enemigo = partida.getBarcosEnemigosVisibles().get(0);
        assertEquals(2, enemigo.numeroJugador());
        assertFalse(enemigo.esPropio());
        assertEquals(0, enemigo.cooldownDisparo());
        assertEquals(0, enemigo.cooldownHabilidad());
    }

    @Test
    void unEnemigoDetrasDeUnaIslaNoAparece() {
        GestorPartida partida = partida(juntar(anaCorta(2, 2), beto(barco(10, 'D', 2, 5, 2))));

        assertFalse(seVeElBarco(partida, 10));
    }

    @Test
    void unEnemigoEnLaZonaDeDominacionSeVeAunqueEsteLejos() {
        GestorPartida partida = partida(juntar(anaCorta(0, 2), beto(barco(10, 'D', 2, 5, 5))));

        assertTrue(seVeElBarco(partida, 10));
    }

    @Test
    void unEnemigoReveladoSeVeAunqueEsteLejos() {
        List<DatosBarco> barcos = juntar(anaFila1(),
                beto(new DatosBarco(10, 'D', 2, new Posicion(9, 3), 3, 0, 0, 4, false, true)));
        GestorPartida partida = partida(barcos);

        assertTrue(seVeElBarco(partida, 10));
    }

    @Test
    void unEnemigoHundidoNoAparece() {
        GestorPartida partida = partida(juntar(anaFila1(), beto(hundido(10, 'D', 2, 3, 4))));

        assertFalse(seVeElBarco(partida, 10));
    }

    // ---------- relación con la ruta ----------

    @Test
    void noSePuedePlanificarPasandoPorUnEnemigoVisible() {
        GestorPartida partida = partida(juntar(anaFila1(), beto(barco(10, 'D', 2, 2, 3))));
        partida.seleccionarBarco(4);

        assertThrows(MovimientoInvalidoException.class, () -> partida.agregarPaso(new Posicion(2, 3)));
    }

    @Test
    void lasAlcanzablesNoIncluyenEnemigosVisibles() {
        GestorPartida partida = partida(juntar(anaFila1(), beto(barco(10, 'D', 2, 2, 3))));
        partida.seleccionarBarco(4);

        assertFalse(partida.getCasillasAlcanzables().contains(new Posicion(2, 3)));
        assertTrue(partida.getCasillasAlcanzables().contains(new Posicion(2, 4)));
    }
}