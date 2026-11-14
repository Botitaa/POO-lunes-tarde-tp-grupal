package batallanaval.vista;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import batallanaval.controlador.GestorPartida;
import batallanaval.excepciones.PersistenciaException;
import batallanaval.modelo.FabricaFlota;
import batallanaval.modelo.Jugador;
import batallanaval.modelo.Tablero;

/**
 * Prueba {@link TareaGuardar}: el esquema {@code try/catch/finally} (avisa, muestra el error y
 * siempre vuelve a habilitar los botones) y que corra en un hilo distinto del que la lanza, con los
 * resultados entregados al hilo de eventos (EDT). No toca archivos ni abre ventanas.
 */
class TareaGuardarTest {

    private final RepositorioPartidasFalso repositorio = new RepositorioPartidasFalso();
    private final GestorPartida partida = new GestorPartida(jugador("Bota", 1), jugador("Santi", 2),
            Tablero.crearMapaEstandar());

    /** Anota en orden qué callback se ejecutó, para chequear también el orden. */
    private final List<String> eventos = new ArrayList<>();

    private TareaGuardar tareaSincronica(RepositorioPartidasFalso repo) {
        return new TareaGuardar(repo, partida, "partida1",
                nombre -> eventos.add("guardado:" + nombre),
                mensaje -> eventos.add("error:" + mensaje),
                () -> eventos.add("terminar"),
                Runnable::run);
    }

    @Test
    void guardaLaPartidaConElNombreYAvisaAlFinal() {
        tareaSincronica(repositorio).run();

        assertEquals(List.of("partida1"), repositorio.nombresGuardados);
        assertEquals(partida, repositorio.ultimaPartidaGuardada);
        assertEquals(List.of("guardado:partida1", "terminar"), eventos);
    }

    @Test
    void siElRepositorioLanzaPersistenciaExceptionSeMuestraSuMensajeYSeHabilitanLosBotones() {
        repositorio.error = new PersistenciaException("Error al guardar el archivo: datos/partidas/partida1.sav",
                new java.io.IOException("disco lleno"));

        tareaSincronica(repositorio).run();

        assertEquals(List.of("error:Error al guardar el archivo: datos/partidas/partida1.sav", "terminar"), eventos,
                "el finally tiene que correr aunque falle el guardado");
    }

    @Test
    void unErrorInesperadoNoMataElHiloEnSilencioYTambienHabilitaLosBotones() {
        RepositorioPartidasFalso roto = new RepositorioPartidasFalso() {
            @Override
            public void guardar(GestorPartida p, String nombre) {
                throw new IllegalStateException("bug");
            }
        };

        tareaSincronica(roto).run();

        assertEquals(List.of("error:" + TareaGuardar.MENSAJE_ERROR_INESPERADO, "terminar"), eventos);
    }

    @Test
    void terminarSeEjecutaSiempreUnaSolaVezYAlFinal() {
        tareaSincronica(repositorio).run();
        assertEquals("terminar", eventos.get(eventos.size() - 1));
        assertEquals(1, eventos.stream().filter("terminar"::equals).count());

        eventos.clear();
        repositorio.error = new PersistenciaException("falla");
        tareaSincronica(repositorio).run();
        assertEquals("terminar", eventos.get(eventos.size() - 1));
        assertEquals(1, eventos.stream().filter("terminar"::equals).count());
    }

    @Test
    void conUnHiloPropioElGuardadoNoBloqueaAQuienLoLanzaYLosAvisosVanAlEdt() throws Exception {
        CountDownLatch repositorioPuedeTerminar = new CountDownLatch(1);
        AtomicReference<Thread> hiloQueGuardo = new AtomicReference<>();
        AtomicReference<Boolean> avisoEnEdt = new AtomicReference<>();
        AtomicReference<Boolean> terminarEnEdt = new AtomicReference<>();
        CountDownLatch termino = new CountDownLatch(1);

        RepositorioPartidasFalso lento = new RepositorioPartidasFalso() {
            @Override
            public void guardar(GestorPartida p, String nombre) throws PersistenciaException {
                hiloQueGuardo.set(Thread.currentThread());
                try {
                    // Simula un disco lento: el hilo propio queda esperando acá.
                    repositorioPuedeTerminar.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                super.guardar(p, nombre);
            }
        };
        TareaGuardar tarea = new TareaGuardar(lento, partida, "partida1",
                nombre -> avisoEnEdt.set(SwingUtilities.isEventDispatchThread()),
                mensaje -> eventos.add("error:" + mensaje),
                () -> {
                    terminarEnEdt.set(SwingUtilities.isEventDispatchThread());
                    termino.countDown();
                });

        Thread hilo = new Thread(tarea, "hilo-guardar-test");
        hilo.start();

        // El guardado todavía no terminó, pero quien lo lanzó ya está libre para seguir.
        assertFalse(termino.await(100, TimeUnit.MILLISECONDS), "no tendría que haber terminado todavía");
        repositorioPuedeTerminar.countDown();
        assertTrue(termino.await(5, TimeUnit.SECONDS), "el guardado tendría que terminar");
        hilo.join(1000);

        assertNotSame(Thread.currentThread(), hiloQueGuardo.get(), "guardó en otro hilo");
        assertFalse(SwingUtilities.isEventDispatchThread());
        assertEquals(Boolean.TRUE, avisoEnEdt.get(), "el aviso se entrega en el EDT");
        assertEquals(Boolean.TRUE, terminarEnEdt.get(), "volver a habilitar los botones también va en el EDT");
        assertEquals(List.of("partida1"), lento.nombresGuardados);
        assertTrue(eventos.isEmpty());
    }

    @Test
    void noSeAceptanArgumentosNulos() {
        assertThrows(NullPointerException.class, () -> new TareaGuardar(null, partida, "a", n -> { }, m -> { }, () -> { }));
        assertThrows(NullPointerException.class, () -> new TareaGuardar(repositorio, null, "a", n -> { }, m -> { }, () -> { }));
        assertThrows(NullPointerException.class, () -> new TareaGuardar(repositorio, partida, null, n -> { }, m -> { }, () -> { }));
        assertThrows(NullPointerException.class, () -> new TareaGuardar(repositorio, partida, "a", null, m -> { }, () -> { }));
        assertThrows(NullPointerException.class, () -> new TareaGuardar(repositorio, partida, "a", n -> { }, null, () -> { }));
        assertThrows(NullPointerException.class, () -> new TareaGuardar(repositorio, partida, "a", n -> { }, m -> { }, null));
    }

    private static Jugador jugador(String nombre, int numero) {
        return new Jugador(numero, nombre, FabricaFlota.crearFlotaEstandar(numero == 1 ? 1 : 7));
    }
}
