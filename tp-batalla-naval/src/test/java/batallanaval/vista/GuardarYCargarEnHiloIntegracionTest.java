package batallanaval.vista;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import batallanaval.controlador.GestorPartida;
import batallanaval.modelo.FabricaFlota;
import batallanaval.modelo.Jugador;
import batallanaval.modelo.Posicion;
import batallanaval.modelo.Tablero;
import batallanaval.persistencia.RepositorioPartidasArchivo;

/**
 * Integración de {@link TareaGuardar} y {@link TareaCargar} con el repositorio de archivos de verdad
 * y con hilos de verdad: se guarda una partida en un archivo temporal desde un hilo propio, se carga
 * desde otro y la partida cargada tiene que quedar igual a la original. Los resultados vuelven por el
 * hilo de eventos de Swing, como en la aplicación.
 */
class GuardarYCargarEnHiloIntegracionTest {

    @TempDir
    Path carpeta;

    @Test
    void unaPartidaGuardadaEnUnHiloSeCargaIgualEnOtro() throws Exception {
        RepositorioPartidasArchivo repositorio = new RepositorioPartidasArchivo(carpeta);
        GestorPartida original = partidaEnJuego();
        assertTrue(original.puedeGuardarse(), "arranca en el inicio de un turno: se puede guardar");

        // --- guardar en un hilo propio ---
        List<String> errores = new ArrayList<>();
        AtomicReference<String> guardado = new AtomicReference<>();
        CountDownLatch guardadoTermino = new CountDownLatch(1);
        Thread hiloGuardar = new Thread(new TareaGuardar(repositorio, original, "integracion",
                guardado::set, errores::add, guardadoTermino::countDown));
        hiloGuardar.start();
        assertTrue(guardadoTermino.await(10, TimeUnit.SECONDS), "el guardado tendría que terminar");
        hiloGuardar.join(1000);

        assertTrue(errores.isEmpty(), "no tendría que haber errores: " + errores);
        assertEquals("integracion", guardado.get());
        assertTrue(Files.exists(carpeta.resolve("integracion.sav")), "se escribió el archivo");

        // --- cargar en otro hilo propio ---
        AtomicReference<GestorPartida> cargada = new AtomicReference<>();
        CountDownLatch cargaTermino = new CountDownLatch(1);
        Thread hiloCargar = new Thread(new TareaCargar(repositorio, "integracion",
                cargada::set, errores::add, cargaTermino::countDown));
        hiloCargar.start();
        assertTrue(cargaTermino.await(10, TimeUnit.SECONDS), "la carga tendría que terminar");
        hiloCargar.join(1000);

        assertTrue(errores.isEmpty(), "no tendría que haber errores: " + errores);
        assertEquals(original.crearInstantanea(), cargada.get().crearInstantanea(),
                "la partida cargada tiene que ser igual a la guardada");
    }

    @Test
    void cargarUnaPartidaQueNoExisteMuestraElErrorYLiberaLosBotones() throws Exception {
        RepositorioPartidasArchivo repositorio = new RepositorioPartidasArchivo(carpeta);
        List<String> errores = new ArrayList<>();
        AtomicReference<GestorPartida> cargada = new AtomicReference<>();
        CountDownLatch termino = new CountDownLatch(1);

        Thread hilo = new Thread(new TareaCargar(repositorio, "no-existe", cargada::set, errores::add,
                termino::countDown));
        hilo.start();

        assertTrue(termino.await(10, TimeUnit.SECONDS), "el finally tiene que correr aunque falle");
        hilo.join(1000);
        assertEquals(1, errores.size(), "un solo mensaje de error para el jugador");
        assertFalse(errores.get(0).isBlank());
        assertNull(cargada.get());
    }

    /** Despliegue fijo de las dos flotas: la partida queda EN_JUEGO, al inicio del turno del jugador 1. */
    private static GestorPartida partidaEnJuego() {
        GestorPartida partida = new GestorPartida(
                new Jugador(1, "Ana", FabricaFlota.crearFlotaEstandar(1)),
                new Jugador(2, "Beto", FabricaFlota.crearFlotaEstandar(7)),
                Tablero.crearMapaEstandar());
        int[][] ana = {{1, 2, 4}, {2, 2, 5}, {3, 2, 8}, {4, 2, 0}, {5, 2, 1}, {6, 2, 9}};
        int[][] beto = {{7, 7, 4}, {8, 7, 5}, {9, 7, 8}, {10, 7, 0}, {11, 7, 1}, {12, 7, 9}};
        for (int[] barco : ana) {
            partida.ubicarBarco(barco[0], new Posicion(barco[1], barco[2]));
        }
        partida.confirmarDespliegue();
        for (int[] barco : beto) {
            partida.ubicarBarco(barco[0], new Posicion(barco[1], barco[2]));
        }
        partida.confirmarDespliegue();
        return partida;
    }
}
