package batallanaval.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import batallanaval.excepciones.MovimientoInvalidoException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class JugadorTest {

    private Jugador crearJugador() {
        return new Jugador(1, "Bota", FabricaFlota.crearFlotaEstandar(1));
    }

    private void hundir(Barco barco) {
        barco.recibirDanio(barco.getVidaMaxima());
    }

    // ---------- Constructor ----------

    @Test
    void testConstructorValidoGuardaLosDatos() {
        Jugador jugador = crearJugador();

        assertEquals("Bota", jugador.getNombre());
        assertEquals(1, jugador.getNumero());
        assertEquals(6, jugador.getFlota().size());
        assertEquals(0, jugador.getPuntosMovimiento());
        assertEquals(0, jugador.getPuntosDominacion());
    }

    @Test
    void testConstructorRechazaNombreNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new Jugador(1, null, FabricaFlota.crearFlotaEstandar(1)));
    }

    @Test
    void testConstructorRechazaNombreEnBlanco() {
        assertThrows(IllegalArgumentException.class,
                () -> new Jugador(1, "   ", FabricaFlota.crearFlotaEstandar(1)));
    }

    @Test
    void testConstructorRechazaNumeroInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> new Jugador(0, "Bota", FabricaFlota.crearFlotaEstandar(1)));
        assertThrows(IllegalArgumentException.class,
                () -> new Jugador(3, "Bota", FabricaFlota.crearFlotaEstandar(1)));
    }

    @Test
    void testConstructorRechazaFlotaNull() {
        assertThrows(IllegalArgumentException.class, () -> new Jugador(1, "Bota", null));
    }

    @Test
    void testConstructorRechazaFlotaVacia() {
        assertThrows(IllegalArgumentException.class,
                () -> new Jugador(1, "Bota", new ArrayList<>()));
    }

    @Test
    void testConstructorRechazaIdsRepetidos() {
        Barco barco = FabricaFlota.crearFlotaEstandar(1).get(0);

        assertThrows(IllegalArgumentException.class,
                () -> new Jugador(1, "Bota", List.of(barco, barco)));
    }

    @Test
    void testModificarLaListaOriginalNoAfectaAlJugador() {
        List<Barco> original = new ArrayList<>(FabricaFlota.crearFlotaEstandar(1));
        Jugador jugador = new Jugador(1, "Bota", original);

        original.clear();

        assertEquals(6, jugador.getFlota().size());
    }

    // ---------- Flota ----------

    @Test
    void testGetFlotaNoSePuedeModificar() {
        Jugador jugador = crearJugador();

        assertThrows(UnsupportedOperationException.class, () -> jugador.getFlota().clear());
    }

    @Test
    void testGetBarcoPorIdDevuelveElBarcoCorrecto() {
        Jugador jugador = crearJugador();

        Barco barco = jugador.getBarcoPorId(3);

        assertNotNull(barco);
        assertEquals(3, barco.getId());
    }

    @Test
    void testGetBarcoPorIdInexistenteDevuelveNull() {
        Jugador jugador = crearJugador();

        assertNull(jugador.getBarcoPorId(99));
    }

    @Test
    void testGetBarcosVivosExcluyeLosHundidos() {
        Jugador jugador = crearJugador();
        Barco hundido = jugador.getFlota().get(0);

        hundir(hundido);

        List<Barco> vivos = jugador.getBarcosVivos();
        assertEquals(5, vivos.size());
        assertFalse(vivos.contains(hundido));
    }

    @Test
    void testGetBarcosVivosConUnoHundidoEnElMedio() {
        Jugador jugador = crearJugador();

        hundir(jugador.getFlota().get(2));

        assertEquals(5, jugador.getBarcosVivos().size());
    }

    @Test
    void testTieneBarcosVivosConFlotaIntacta() {
        assertTrue(crearJugador().tieneBarcosVivos());
    }

    @Test
    void testTieneBarcosVivosEsFalsoSiSeHundieronTodos() {
        Jugador jugador = crearJugador();

        for (Barco barco : jugador.getFlota()) {
            hundir(barco);
        }

        assertFalse(jugador.tieneBarcosVivos());
        assertTrue(jugador.getBarcosVivos().isEmpty());
    }

    @Test
    void testGetVidaTotalBajaAlRecibirDanio() {
        Jugador jugador = crearJugador();
        int vidaInicial = jugador.getVidaTotal();
        Barco barco = jugador.getFlota().get(0);

        barco.recibirDanio(1);

        assertEquals(vidaInicial - 1, jugador.getVidaTotal());
    }

    // ---------- Puntos de movimiento ----------

    @Test
    void testReponerPuntosAsignaElValor() {
        Jugador jugador = crearJugador();

        jugador.reponerPuntos(6);
        jugador.reponerPuntos(4);

        assertEquals(4, jugador.getPuntosMovimiento());
    }

    @Test
    void testReponerPuntosNegativosLanzaExcepcion() {
        Jugador jugador = crearJugador();

        assertThrows(IllegalArgumentException.class, () -> jugador.reponerPuntos(-1));
    }

    @Test
    void testGastarPuntosDescuentaDelTotal() {
        Jugador jugador = crearJugador();
        jugador.reponerPuntos(6);

        jugador.gastarPuntos(2);

        assertEquals(4, jugador.getPuntosMovimiento());
    }

    @Test
    void testGastarCeroPuntosEsValido() {
        Jugador jugador = crearJugador();
        jugador.reponerPuntos(6);

        jugador.gastarPuntos(0);

        assertEquals(6, jugador.getPuntosMovimiento());
    }

    @Test
    void testGastarMasPuntosDeLosQueTieneLanzaExcepcionYNoCambiaNada() {
        Jugador jugador = crearJugador();
        jugador.reponerPuntos(6);

        assertThrows(MovimientoInvalidoException.class, () -> jugador.gastarPuntos(10));

        assertEquals(6, jugador.getPuntosMovimiento());
    }

    @Test
    void testGastarPuntosNegativosLanzaExcepcion() {
        Jugador jugador = crearJugador();
        jugador.reponerPuntos(6);

        assertThrows(IllegalArgumentException.class, () -> jugador.gastarPuntos(-1));
    }

    @Test
    void testTienePuntos() {
        Jugador jugador = crearJugador();
        jugador.reponerPuntos(4);

        assertTrue(jugador.tienePuntos(4));
        assertFalse(jugador.tienePuntos(5));
    }

    // ---------- Dominacion y resumen ----------

    @Test
    void testSumarPuntoDominacionIncrementaDeAUno() {
        Jugador jugador = crearJugador();

        jugador.sumarPuntoDominacion();
        jugador.sumarPuntoDominacion();

        assertEquals(2, jugador.getPuntosDominacion());
    }

    @Test
    void testGetResumenArrancaVacioYSiempreEsElMismo() {
        Jugador jugador = crearJugador();

        assertNotNull(jugador.getResumen());
        assertTrue(jugador.getResumen().estaVacio());
        assertSame(jugador.getResumen(), jugador.getResumen());
    }
}