package batallanaval.controlador;

import batallanaval.excepciones.DisparoInvalidoException;
import batallanaval.excepciones.MovimientoInvalidoException;
import batallanaval.historial.TipoEvento;
import batallanaval.modelo.Casilla;
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
 * Tests de disparo en GestorPartida (PAR-09, #54): objetivos válidos, daño, revelado del tirador,
 * resumen del rival, hundimiento y victoria por hundimiento.
 * <p>
 * Siempre juega Ana (J1). Su flota base está en la fila 1: acorazado 1 en (1,0) (rango 6, daño 4,
 * visión 5), cruceros 2 y 3 en (1,1) y (1,2) (rango 4, daño 2, visión 3), destructores 4, 5 y 6 en
 * (1,3), (1,4) y (1,5) (rango 2, daño 3, visión 2). La flota de Beto (J2, ids 7 a 12) está en la fila 9,
 * salvo los barcos que cada test acerca.
 */
class GestorPartidaDisparoTest {

    // ---------- armado ----------

    private static GestorPartida partida(List<DatosBarco> barcos) {
        List<DatosJugador> jugadores = List.of(
                new DatosJugador(1, "Ana", GestorPartida.PUNTOS_MOVIMIENTO_TURNO, 0, List.of()),
                new DatosJugador(2, "Beto", GestorPartida.PUNTOS_MOVIMIENTO_TURNO, 0, List.of()));
        InstantaneaPartida instantanea = new InstantaneaPartida("estandar", 1, 1, FasePartida.EN_JUEGO,
                jugadores, barcos, List.of(), List.of());
        return GestorPartida.restaurar(instantanea, Tablero.crearMapaEstandar());
    }

    private static DatosBarco barco(int id, char tipo, int jugador, int fila, int columna, int vida) {
        int casillas = tipo == 'A' ? 2 : tipo == 'C' ? 3 : 4;
        return new DatosBarco(id, tipo, jugador, new Posicion(fila, columna), vida, 0, 0, casillas, false, false);
    }

    private static DatosBarco sano(int id, char tipo, int jugador, int fila, int columna) {
        int vida = tipo == 'A' ? 8 : tipo == 'C' ? 5 : 3;
        return barco(id, tipo, jugador, fila, columna, vida);
    }

    private static List<DatosBarco> ana() {
        return List.of(
                sano(1, 'A', 1, 1, 0), sano(2, 'C', 1, 1, 1), sano(3, 'C', 1, 1, 2),
                sano(4, 'D', 1, 1, 3), sano(5, 'D', 1, 1, 4), sano(6, 'D', 1, 1, 5));
    }

    private static List<DatosBarco> beto() {
        return List.of(
                sano(7, 'A', 2, 9, 0), sano(8, 'C', 2, 9, 1), sano(9, 'C', 2, 9, 2),
                sano(10, 'D', 2, 9, 3), sano(11, 'D', 2, 9, 4), sano(12, 'D', 2, 9, 5));
    }

    /** Las dos flotas base, con los barcos que pase el test reemplazados (por id). */
    private static List<DatosBarco> con(DatosBarco... cambios) {
        List<DatosBarco> barcos = new ArrayList<>(ana());
        barcos.addAll(beto());
        for (DatosBarco cambio : cambios) {
            barcos.replaceAll(b -> b.id() == cambio.id() ? cambio : b);
        }
        return barcos;
    }

    private static DatosBarco datosDe(GestorPartida partida, int idBarco) {
        for (DatosBarco barco : partida.crearInstantanea().barcos()) {
            if (barco.id() == idBarco) {
                return barco;
            }
        }
        throw new AssertionError("No existe el barco " + idBarco);
    }

    private static boolean esObjetivo(GestorPartida partida, int idBarco) {
        return partida.getObjetivosValidos().stream().anyMatch(b -> b.id() == idBarco);
    }

    /** Crucero 8 de Beto en (3,4): a 2 del destructor 4 de Ana, que lo ve y le llega. */
    private static GestorPartida partidaConCruceroCerca() {
        return partida(con(sano(8, 'C', 2, 3, 4)));
    }

    // ---------- objetivos válidos ----------

    @Test
    void sinBarcoSeleccionadoNoHayObjetivos() {
        GestorPartida partida = partidaConCruceroCerca();

        assertTrue(partida.getObjetivosValidos().isEmpty());
    }

    @Test
    void unEnemigoVisibleYEnRangoEsObjetivo() {
        GestorPartida partida = partidaConCruceroCerca();
        partida.seleccionarBarco(4);

        assertTrue(esObjetivo(partida, 8));
    }

    @Test
    void unEnemigoFueraDeRangoNoEsObjetivo() {
        // El crucero 8 en (4,4) se ve (zona de dominación), pero está a 3 del destructor 4, que tira a 2.
        GestorPartida partida = partida(con(sano(8, 'C', 2, 4, 4)));
        partida.seleccionarBarco(4);

        assertFalse(esObjetivo(partida, 8));
        assertThrows(DisparoInvalidoException.class, () -> partida.disparar(8));
    }

    @Test
    void sinVisionNoSePuedeDispararAunqueEsteEnRango() {
        // El crucero 8 en (7,0) está a 6 del acorazado (rango 6), pero el acorazado ve solo 5 y nadie más lo ve.
        GestorPartida partida = partida(con(sano(8, 'C', 2, 7, 0)));
        partida.seleccionarBarco(1);

        assertFalse(esObjetivo(partida, 8));
        assertThrows(DisparoInvalidoException.class, () -> partida.disparar(8));
    }

    @Test
    void conVisionCompartidaSePuedeDispararAUnEnemigoQueOtroBarcoVe() {
        // El crucero 8 de Beto en (5,1): el crucero 2 de Ana le llega (distancia 4, rango 4) pero no lo ve
        // (visión 3). Lo ve el acorazado de Ana (visión 5), y con eso alcanza.
        GestorPartida partida = partida(con(sano(8, 'C', 2, 5, 1)));
        partida.seleccionarBarco(2);

        assertTrue(esObjetivo(partida, 8));
    }

    @Test
    void unaIslaEnElMedioImpideElDisparo() {
        // El crucero 8 de Beto en (4,3) se ve gracias al destructor 6 de Ana en (3,4), pero del crucero 3
        // de Ana en (1,2) hasta (4,3) hay una isla en el medio.
        GestorPartida partida = partida(con(sano(6, 'D', 1, 3, 4), sano(8, 'C', 2, 4, 3)));
        partida.seleccionarBarco(3);

        assertFalse(esObjetivo(partida, 8));
        assertThrows(DisparoInvalidoException.class, () -> partida.disparar(8));
    }

    @Test
    void losObjetivosVienenComoBarcosDelRival() {
        GestorPartida partida = partidaConCruceroCerca();
        partida.seleccionarBarco(4);

        EstadoBarco objetivo = partida.getObjetivosValidos().get(0);
        assertEquals(2, objetivo.numeroJugador());
        assertFalse(objetivo.esPropio());
    }

    // ---------- disparar ----------

    @Test
    void elDisparoSiempreAciertaYRestaElDanioDelTirador() {
        GestorPartida partida = partidaConCruceroCerca();
        partida.seleccionarBarco(4);

        partida.disparar(8);

        assertEquals(5 - 3, datosDe(partida, 8).vida()); // destructor: daño 3
    }

    @Test
    void sinBarcoSeleccionadoNoSePuedeDisparar() {
        GestorPartida partida = partidaConCruceroCerca();

        assertThrows(DisparoInvalidoException.class, () -> partida.disparar(8));
    }

    @Test
    void noSePuedeDispararleAUnBarcoPropio() {
        GestorPartida partida = partidaConCruceroCerca();
        partida.seleccionarBarco(4);

        assertThrows(DisparoInvalidoException.class, () -> partida.disparar(5));
    }

    @Test
    void unBarcoDisparaUnaSolaVezPorTurno() {
        GestorPartida partida = partidaConCruceroCerca();
        partida.seleccionarBarco(4);
        partida.disparar(8);

        assertThrows(DisparoInvalidoException.class, () -> partida.disparar(8));
        assertTrue(partida.getObjetivosValidos().isEmpty());
    }

    @Test
    void unBarcoQueDisparoYaNoSeMueve() {
        GestorPartida partida = partidaConCruceroCerca();
        partida.seleccionarBarco(4);
        partida.disparar(8);

        assertThrows(MovimientoInvalidoException.class, () -> partida.agregarPaso(new Posicion(2, 3)));
    }

    @Test
    void otroBarcoPuedeDispararEnElMismoTurno() {
        GestorPartida partida = partidaConCruceroCerca();
        partida.seleccionarBarco(4);
        partida.disparar(8);

        partida.seleccionarBarco(5); // destructor 5 en (1,4), a 2 del crucero
        partida.disparar(8);

        assertEquals(0, datosDe(partida, 8).vida()); // 5 - 3 - 3: hundido
    }

    @Test
    void elAcorazadoQuedaRecargandoDespuesDeDisparar() {
        // El crucero 8 en (3,1) está a 2 del acorazado (1,0): lo ve y le llega.
        GestorPartida partida = partida(con(sano(8, 'C', 2, 3, 1)));
        partida.seleccionarBarco(1);

        partida.disparar(8);

        assertEquals(1, datosDe(partida, 1).cooldownDisparo());
    }

    @Test
    void despuesDeDispararYaNoSePuedeGuardarEnEseTurno() {
        GestorPartida partida = partidaConCruceroCerca();
        partida.seleccionarBarco(4);
        partida.disparar(8);
        partida.cancelarSeleccion();

        assertFalse(partida.puedeGuardarse());
    }

    // ---------- revelado, resumen e historial ----------

    @Test
    void elTiradorQuedaReveladoDuranteElTurnoDelRival() {
        GestorPartida partida = partidaConCruceroCerca();
        partida.seleccionarBarco(4);
        partida.disparar(8);

        partida.terminarTurno(); // juega Beto

        assertTrue(datosDe(partida, 4).revelado());
    }

    @Test
    void elReveladoDelTiradorTerminaCuandoVuelveSuTurno() {
        GestorPartida partida = partidaConCruceroCerca();
        partida.seleccionarBarco(4);
        partida.disparar(8);
        partida.terminarTurno(); // juega Beto

        partida.terminarTurno(); // vuelve Ana

        assertFalse(datosDe(partida, 4).revelado());
    }

    @Test
    void elRivalRecibeElDisparoEnSuResumen() {
        GestorPartida partida = partidaConCruceroCerca();
        partida.seleccionarBarco(4);
        partida.disparar(8);

        partida.terminarTurno(); // juega Beto: ve el resumen

        assertEquals(1, partida.getMensajesResumen().size());
    }

    @Test
    void elDisparoQuedaEnElHistorialYLoVenLosDos() {
        GestorPartida partida = partidaConCruceroCerca();
        partida.seleccionarBarco(4);
        partida.disparar(8);

        partida.terminarTurno(); // juega Beto

        assertTrue(partida.getHistorialVisible().stream()
                .anyMatch(e -> e.tipo() == TipoEvento.DISPARO && e.numeroJugador() == 1));
    }

    // ---------- hundimiento y victoria ----------

    /** Destructor 10 de Beto en (3,4) con 3 de vida: un disparo del destructor 4 de Ana lo hunde. */
    private static GestorPartida partidaConDestructorAHundir() {
        return partida(con(sano(10, 'D', 2, 3, 4)));
    }

    @Test
    void conVidaCeroElBarcoSeHundeYDejaRestos() {
        GestorPartida partida = partidaConDestructorAHundir();
        partida.seleccionarBarco(4);

        partida.disparar(10);

        assertEquals(0, datosDe(partida, 10).vida());
        assertEquals(Casilla.RESTOS, partida.getCasilla(new Posicion(3, 4)));
        assertTrue(partida.getBarcosEnemigosVisibles().stream().noneMatch(b -> b.id() == 10));
    }

    @Test
    void elHundimientoLlegaAlResumenDelRivalYAlHistorial() {
        GestorPartida partida = partidaConDestructorAHundir();
        partida.seleccionarBarco(4);
        partida.disparar(10);

        partida.terminarTurno(); // juega Beto

        assertEquals(2, partida.getMensajesResumen().size()); // el disparo y el hundimiento
        assertTrue(partida.getHistorialVisible().stream().anyMatch(e -> e.tipo() == TipoEvento.HUNDIMIENTO));
    }

    @Test
    void hundirElUltimoBarcoGanaLaPartida() {
        // A Beto le queda solo el destructor 10, en (3,4).
        GestorPartida partida = partida(con(
                barco(7, 'A', 2, 9, 0, 0), barco(8, 'C', 2, 9, 1, 0), barco(9, 'C', 2, 9, 2, 0),
                sano(10, 'D', 2, 3, 4), barco(11, 'D', 2, 9, 4, 0), barco(12, 'D', 2, 9, 5, 0)));
        partida.seleccionarBarco(4);

        partida.disparar(10);

        assertTrue(partida.estaTerminada());
        assertEquals("Ana", partida.getResultado().ganador());
        assertEquals(MotivoFin.HUNDIMIENTO, partida.getResultado().motivo());
    }

    @Test
    void siQuedanBarcosLaPartidaSigue() {
        GestorPartida partida = partidaConDestructorAHundir();
        partida.seleccionarBarco(4);

        partida.disparar(10);

        assertFalse(partida.estaTerminada());
    }
}