package batallanaval.vista;

import batallanaval.controlador.DatosBarco;
import batallanaval.controlador.DatosJugador;
import batallanaval.controlador.FasePartida;
import batallanaval.controlador.GestorPartida;
import batallanaval.controlador.InstantaneaPartida;
import batallanaval.historial.EventoPartida;
import batallanaval.historial.TipoEvento;
import batallanaval.modelo.Casilla;
import batallanaval.modelo.Posicion;
import batallanaval.modelo.Tablero;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Los textos de "Qué podés hacer" y "Último aviso" del panel lateral, con partidas reales. */
class GuiaJugadorTest {

    private static Tablero mapaDeAgua() {
        Casilla[][] grilla = new Casilla[10][10];
        for (Casilla[] fila : grilla) {
            Arrays.fill(fila, Casilla.AGUA);
        }
        return new Tablero(grilla);
    }

    private static DatosBarco barco(int id, char tipo, int jugador, int fila, int columna, int vida, int casillas) {
        return new DatosBarco(id, tipo, jugador, new Posicion(fila, columna), vida, 0, 0, casillas, false, false);
    }

    /** Turno de J1. J1 abajo a la izquierda de su zona, J2 lejos; los cambios reemplazan por id. */
    private static GestorPartida partida(DatosBarco... cambios) {
        Map<Integer, DatosBarco> barcos = new LinkedHashMap<>();
        char[] tipos = {'A', 'C', 'C', 'D', 'D', 'D'};
        int[] vidas = {8, 5, 5, 3, 3, 3};
        int[] casillas = {2, 3, 3, 4, 4, 4};
        for (int i = 0; i < 6; i++) {
            barcos.put(i + 1, barco(i + 1, tipos[i], 1, 0, i, vidas[i], casillas[i]));
            barcos.put(i + 7, barco(i + 7, tipos[i], 2, 9, 9 - i, vidas[i], casillas[i]));
        }
        for (DatosBarco cambio : cambios) {
            barcos.put(cambio.id(), cambio);
        }
        InstantaneaPartida instantanea = new InstantaneaPartida("estandar", 1, 1, FasePartida.EN_JUEGO,
                List.of(new DatosJugador(1, "Ana", 6, 0, List.of()), new DatosJugador(2, "Beto", 6, 0, List.of())),
                new ArrayList<>(barcos.values()), List.of(), List.of());
        return GestorPartida.restaurar(instantanea, mapaDeAgua());
    }

    @Test
    void sinBarcoElegidoPideElegirUno() {
        assertTrue(PanelLateral.textoAyuda(partida()).startsWith("Hacé clic en uno de tus barcos"));
    }

    @Test
    void conRutaArmadaDiceComoConfirmar() {
        GestorPartida partida = partida();
        partida.seleccionarBarco(4);
        partida.agregarPaso(new Posicion(1, 3));
        assertTrue(PanelLateral.textoAyuda(partida).startsWith("Apretá Enter"));
    }

    @Test
    void conEnemigoATiroYCasillasLibresOfreceLasDosCosas() {
        GestorPartida partida = partida(barco(4, 'D', 1, 2, 3, 3, 4), barco(10, 'D', 2, 4, 3, 3, 4));
        partida.seleccionarBarco(4);
        assertTrue(PanelLateral.textoAyuda(partida).contains("enemigo marcado en rojo"));
        assertTrue(PanelLateral.textoAyuda(partida).contains("casilla sombreada"));
    }

    @Test
    void sinEnemigosALaVistaLoExplica() {
        GestorPartida partida = partida();
        partida.seleccionarBarco(4);
        assertTrue(PanelLateral.textoAyuda(partida).startsWith("No ves ningún enemigo."));
    }

    @Test
    void enemigoVistoPeroLejosDiceQueEstaFueraDeRango() {
        // El acorazado (visión 5) ve al enemigo; el destructor (rango 2) no llega
        GestorPartida partida = partida(barco(1, 'A', 1, 2, 0, 8, 2), barco(10, 'D', 2, 6, 0, 3, 4));
        partida.seleccionarBarco(4);
        assertTrue(PanelLateral.textoAyuda(partida).startsWith("Los enemigos que ves están fuera de su rango"));
    }

    @Test
    void barcoQueYaDisparoYNoPuedeMoverseLoDice() {
        GestorPartida partida = partida(barco(4, 'D', 1, 2, 3, 3, 4), barco(10, 'A', 2, 4, 3, 8, 2));
        partida.seleccionarBarco(4);
        partida.disparar(10);
        assertTrue(PanelLateral.textoAyuda(partida).startsWith("Este barco ya no puede moverse ni disparar"));
    }

    @Test
    void sinPuntosDeMovimientoLoAvisa() {
        GestorPartida partida = partida();
        partida.seleccionarBarco(1);
        partida.agregarPaso(new Posicion(1, 0));
        partida.agregarPaso(new Posicion(2, 0));
        partida.confirmarMovimiento();
        partida.cancelarSeleccion();
        assertTrue(PanelLateral.textoAyuda(partida).startsWith("Ya no te quedan puntos de movimiento"));
    }

    @Test
    void elUltimoAvisoEsElUltimoEventoQueSeVe() {
        assertEquals("Todavía no pasó nada.", PanelLateral.textoUltimoAviso(List.of()));
        EventoPartida evento = new EventoPartida(1, 1, TipoEvento.DISPARO, "Disparo de prueba", LocalDateTime.now());
        assertEquals("Disparo de prueba", PanelLateral.textoUltimoAviso(List.of(evento)));
    }

    @Test
    void losTextosDelHistorialSeEntienden() {
        GestorPartida partida = partida(barco(4, 'D', 1, 2, 3, 3, 4), barco(10, 'A', 2, 4, 3, 8, 2));
        partida.seleccionarBarco(4);
        partida.disparar(10);
        assertEquals("Destructor de Ana en (2,3) disparó al Acorazado de Beto en (4,3): -3 de vida (le queda 5)",
                PanelLateral.textoUltimoAviso(partida.getHistorialVisible()));
    }
}
