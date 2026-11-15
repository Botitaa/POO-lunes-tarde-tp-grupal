package batallanaval.controlador;

import batallanaval.excepciones.AccionInvalidaException;
import batallanaval.excepciones.DespliegueInvalidoException;
import batallanaval.historial.EventoPartida;
import batallanaval.modelo.FabricaFlota;
import batallanaval.modelo.Jugador;
import batallanaval.modelo.Posicion;
import batallanaval.modelo.Tablero;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests del despliegue de GestorPartida (PAR-05, #50): ubicar, retirar, barcos sin ubicar y confirmar.
 * <p>
 * Mapa estándar: J1 despliega en las filas 0 a 2 y J2 en las filas 7 a 9 (todas de agua).
 * Flota de J1 con ids 1 a 6 y de J2 con ids 7 a 12.
 */
class GestorPartidaDespliegueTest {

    private GestorPartida partida;
    private ObservadorEspia espia;

    @BeforeEach
    void crearPartida() {
        Jugador ana = new Jugador(1, "Ana", FabricaFlota.crearFlotaEstandar(1));
        Jugador beto = new Jugador(2, "Beto", FabricaFlota.crearFlotaEstandar(7));
        partida = new GestorPartida(ana, beto, Tablero.crearMapaEstandar());
        espia = new ObservadorEspia();
        partida.agregarObservador(espia);
    }

    /** Ubica los 6 barcos del jugador activo en una fila de su zona, uno por columna. */
    private void ubicarFlotaCompleta(int primerId, int fila) {
        for (int i = 0; i < 6; i++) {
            partida.ubicarBarco(primerId + i, new Posicion(fila, i));
        }
    }

    // ---------- visibilidad durante el despliegue ----------

    @Test
    void sinBarcosUbicadosElRivalNoTieneEnemigosVisiblesNiFallaLaConsulta() {
        // El rival todavía no ubicó ninguno: sus barcos no tienen posición y no hay nada que ver.
        assertDoesNotThrow(() -> partida.getBarcosEnemigosVisibles());
        assertTrue(partida.getBarcosEnemigosVisibles().isEmpty());
    }

    @Test
    void enElDespliegueDeJ2LosBarcosDeJ1NoSeVenDesdeLaZonaDeJ2() {
        ubicarFlotaCompleta(1, 0);
        partida.confirmarDespliegue();
        partida.ubicarBarco(7, new Posicion(9, 0));

        assertTrue(partida.getBarcosEnemigosVisibles().isEmpty());
    }

    // ---------- ubicarBarco ----------

    @Test
    void alEmpezarTodosLosBarcosEstanSinUbicar() {
        assertEquals(FasePartida.DESPLIEGUE_J1, partida.getFase());
        assertEquals(6, partida.getBarcosSinUbicar().size());
    }

    @Test
    void ubicarUnBarcoLoSacaDeLosSinUbicar() {
        partida.ubicarBarco(1, new Posicion(0, 0));

        assertEquals(5, partida.getBarcosSinUbicar().size());
        assertTrue(partida.getBarcosSinUbicar().stream().noneMatch(b -> b.id() == 1));
    }

    @Test
    void ubicarFueraDeLaZonaDeDespliegueTiraExcepcion() {
        // La fila 5 es zona neutral: J1 solo puede desplegar en las filas 0 a 2.
        assertThrows(DespliegueInvalidoException.class,
                () -> partida.ubicarBarco(1, new Posicion(5, 0)));
    }

    @Test
    void ubicarEnLaZonaDelRivalTiraExcepcion() {
        assertThrows(DespliegueInvalidoException.class,
                () -> partida.ubicarBarco(1, new Posicion(8, 0)));
    }

    @Test
    void ubicarDosBarcosEnLaMismaCasillaTiraExcepcion() {
        partida.ubicarBarco(1, new Posicion(0, 0));

        assertThrows(DespliegueInvalidoException.class,
                () -> partida.ubicarBarco(2, new Posicion(0, 0)));
    }

    @Test
    void ubicarUnBarcoDelRivalTiraExcepcion() {
        // El barco 7 es de Beto y el turno de despliegue es de Ana.
        assertThrows(DespliegueInvalidoException.class,
                () -> partida.ubicarBarco(7, new Posicion(0, 0)));
    }

    @Test
    void ubicarDeNuevoElMismoBarcoLoMueve() {
        partida.ubicarBarco(1, new Posicion(0, 0));
        partida.ubicarBarco(1, new Posicion(1, 1));

        assertEquals(5, partida.getBarcosSinUbicar().size());
        // La casilla vieja quedó libre: se puede ubicar otro barco ahí.
        assertDoesNotThrow(() -> partida.ubicarBarco(2, new Posicion(0, 0)));
    }

    @Test
    void ubicarAvisaALosObservadores() {
        partida.ubicarBarco(1, new Posicion(0, 0));

        assertEquals(1, espia.estadosCambiados);
    }

    // ---------- retirarBarco ----------

    @Test
    void retirarUnBarcoLoDevuelveALosSinUbicar() {
        partida.ubicarBarco(1, new Posicion(0, 0));

        partida.retirarBarco(1);

        assertEquals(6, partida.getBarcosSinUbicar().size());
        assertTrue(partida.getBarcosSinUbicar().stream().anyMatch(b -> b.id() == 1));
    }

    @Test
    void retirarUnBarcoQueNoEstabaUbicadoTiraExcepcion() {
        assertThrows(DespliegueInvalidoException.class, () -> partida.retirarBarco(1));
    }

    @Test
    void retirarUnBarcoDelRivalTiraExcepcion() {
        assertThrows(DespliegueInvalidoException.class, () -> partida.retirarBarco(7));
    }

    // ---------- confirmarDespliegue ----------

    @Test
    void confirmarConBarcosSinUbicarTiraExcepcionYNoCambiaLaFase() {
        partida.ubicarBarco(1, new Posicion(0, 0));

        assertThrows(DespliegueInvalidoException.class, () -> partida.confirmarDespliegue());
        assertEquals(FasePartida.DESPLIEGUE_J1, partida.getFase());
        assertEquals("Ana", partida.getJugadorActivo().nombre());
    }

    @Test
    void cuandoJ1ConfirmaLeTocaDesplegarAJ2() {
        ubicarFlotaCompleta(1, 0);

        partida.confirmarDespliegue();

        assertEquals(FasePartida.DESPLIEGUE_J2, partida.getFase());
        assertEquals("Beto", partida.getJugadorActivo().nombre());
        assertEquals(6, partida.getBarcosSinUbicar().size()); // ahora son los de Beto
    }

    @Test
    void cuandoJ2ConfirmaEmpiezaElJuegoConJ1YSusPuntos() {
        ubicarFlotaCompleta(1, 0);
        partida.confirmarDespliegue();
        ubicarFlotaCompleta(7, 9);

        partida.confirmarDespliegue();

        assertEquals(FasePartida.EN_JUEGO, partida.getFase());
        assertEquals("Ana", partida.getJugadorActivo().nombre());
        assertEquals(GestorPartida.PUNTOS_MOVIMIENTO_TURNO, partida.getJugadorActivo().puntosMovimiento());
    }

    @Test
    void confirmarAvisaElCambioDeFaseYElTurnoNuevo() {
        ubicarFlotaCompleta(1, 0);

        partida.confirmarDespliegue();

        assertEquals(List.of(FasePartida.DESPLIEGUE_J2), espia.fases);
        assertEquals(List.of("Beto"), espia.turnos);
    }

    @Test
    void elEventoDeConfirmarQuedaANombreDelQueConfirmo() {
        ubicarFlotaCompleta(1, 0);

        partida.confirmarDespliegue();

        List<EventoPartida> eventos = partida.crearInstantanea().eventos();
        EventoPartida ultimo = eventos.get(eventos.size() - 1);
        assertEquals(1, ultimo.numeroJugador()); // lo confirmó Ana, aunque ahora el activo sea Beto
    }

    @Test
    void despuesDelDespliegueNoSePuedeUbicarNiRetirar() {
        ubicarFlotaCompleta(1, 0);
        partida.confirmarDespliegue();
        ubicarFlotaCompleta(7, 9);
        partida.confirmarDespliegue();

        assertThrows(AccionInvalidaException.class, () -> partida.ubicarBarco(1, new Posicion(1, 0)));
        assertThrows(AccionInvalidaException.class, () -> partida.retirarBarco(1));
        assertThrows(AccionInvalidaException.class, () -> partida.confirmarDespliegue());
    }

    // ---------- cancelarSeleccion (adelantado del #52) ----------

    @Test
    void cancelarSeleccionEnElDespliegueNoHaceNada() {
        assertDoesNotThrow(() -> partida.cancelarSeleccion());
        assertEquals(0, espia.estadosCambiados);
    }

    /** Observador de prueba: cuenta y guarda los avisos que recibe. */
    private static class ObservadorEspia implements ObservadorPartida {
        int estadosCambiados = 0;
        final List<FasePartida> fases = new ArrayList<>();
        final List<String> turnos = new ArrayList<>();

        @Override
        public void onEstadoCambiado() {
            estadosCambiados++;
        }

        @Override
        public void onFaseCambiada(FasePartida fasePartida) {
            fases.add(fasePartida);
        }

        @Override
        public void onTurnoIniciado(String nombreJugador) {
            turnos.add(nombreJugador);
        }

        @Override
        public void onPartidaTerminada(ResultadoPartida resultadoPartida) {
        }
    }
}