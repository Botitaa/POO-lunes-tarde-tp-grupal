package batallanaval.vista;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
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
 * Prueba {@link TareaCargar}: entrega la partida cargada, muestra el error si el archivo falla y
 * siempre vuelve a habilitar los botones; y que lea en un hilo distinto del que la lanza.
 */
class TareaCargarTest {

    private final RepositorioPartidasFalso repositorio = new RepositorioPartidasFalso();
    private final GestorPartida partidaGuardada = new GestorPartida(jugador("Bota", 1), jugador("Santi", 2),
            Tablero.crearMapaEstandar());
    private final List<String> eventos = new ArrayList<>();
    private final List<GestorPartida> entregadas = new ArrayList<>();

    private TareaCargar tareaSincronica(RepositorioPartidasFalso repo, java.util.function.Consumer<GestorPartida> alCargar) {
        return new TareaCargar(repo, "partida1", alCargar,
                mensaje -> eventos.add("error:" + mensaje),
                () -> eventos.add("terminar"),
                Runnable::run);
    }

    @Test
    void cargaLaPartidaElegidaYSeLaEntregaAlFinal() {
        repositorio.partidaACargar = partidaGuardada;

        tareaSincronica(repositorio, partida -> {
            entregadas.add(partida);
            eventos.add("cargada");
        }).run();

        assertEquals(List.of("partida1"), repositorio.nombresCargados);
        assertEquals(1, entregadas.size());
        assertSame(partidaGuardada, entregadas.get(0));
        assertEquals(List.of("cargada", "terminar"), eventos);
    }

    @Test
    void siElArchivoFallaSeMuestraElMensajeNoSeEntregaNadaYSeHabilitanLosBotones() {
        repositorio.error = new PersistenciaException("Línea 14 del guardado inválida");

        tareaSincronica(repositorio, entregadas::add).run();

        assertTrue(entregadas.isEmpty());
        assertEquals(List.of("error:Línea 14 del guardado inválida", "terminar"), eventos);
    }

    @Test
    void unErrorInesperadoAlLeerSeMuestraGenericoYSeHabilitanLosBotones() {
        RepositorioPartidasFalso roto = new RepositorioPartidasFalso() {
            @Override
            public GestorPartida cargar(String nombre) {
                throw new IllegalStateException("bug");
            }
        };

        tareaSincronica(roto, entregadas::add).run();

        assertTrue(entregadas.isEmpty());
        assertEquals(List.of("error:" + EjecutorAcciones.MENSAJE_ERROR_INESPERADO, "terminar"), eventos);
    }

    @Test
    void siQuienRecibeLaPartidaFallaElJugadorVeUnMensajeGenericoYLosBotonesSeHabilitan() {
        repositorio.partidaACargar = partidaGuardada;

        tareaSincronica(repositorio, partida -> {
            throw new IllegalStateException("bug al instalar la partida");
        }).run();

        assertEquals(List.of("error:" + EjecutorAcciones.MENSAJE_ERROR_INESPERADO, "terminar"), eventos);
    }

    @Test
    void conUnHiloPropioLaLecturaNoBloqueaAQuienLaLanzaYLosResultadosVanAlEdt() throws Exception {
        CountDownLatch repositorioPuedeTerminar = new CountDownLatch(1);
        AtomicReference<Thread> hiloQueLeyo = new AtomicReference<>();
        AtomicReference<Boolean> entregaEnEdt = new AtomicReference<>();
        AtomicReference<GestorPartida> recibida = new AtomicReference<>();
        CountDownLatch termino = new CountDownLatch(1);

        RepositorioPartidasFalso lento = new RepositorioPartidasFalso() {
            @Override
            public GestorPartida cargar(String nombre) throws PersistenciaException {
                hiloQueLeyo.set(Thread.currentThread());
                try {
                    repositorioPuedeTerminar.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return partidaGuardada;
            }
        };
        TareaCargar tarea = new TareaCargar(lento, "partida1",
                partida -> {
                    entregaEnEdt.set(SwingUtilities.isEventDispatchThread());
                    recibida.set(partida);
                },
                mensaje -> eventos.add("error:" + mensaje),
                termino::countDown);

        Thread hilo = new Thread(tarea, "hilo-cargar-test");
        hilo.start();

        assertFalse(termino.await(100, TimeUnit.MILLISECONDS), "no tendría que haber terminado todavía");
        repositorioPuedeTerminar.countDown();
        assertTrue(termino.await(5, TimeUnit.SECONDS), "la carga tendría que terminar");
        hilo.join(1000);

        assertNotSame(Thread.currentThread(), hiloQueLeyo.get(), "leyó en otro hilo");
        assertEquals(Boolean.TRUE, entregaEnEdt.get(), "la partida se entrega en el EDT");
        assertSame(partidaGuardada, recibida.get());
        assertTrue(eventos.isEmpty());
    }

    @Test
    void noSeAceptanArgumentosNulos() {
        assertThrows(NullPointerException.class, () -> new TareaCargar(null, "a", p -> { }, m -> { }, () -> { }));
        assertThrows(NullPointerException.class, () -> new TareaCargar(repositorio, null, p -> { }, m -> { }, () -> { }));
        assertThrows(NullPointerException.class, () -> new TareaCargar(repositorio, "a", null, m -> { }, () -> { }));
        assertThrows(NullPointerException.class, () -> new TareaCargar(repositorio, "a", p -> { }, null, () -> { }));
        assertThrows(NullPointerException.class, () -> new TareaCargar(repositorio, "a", p -> { }, m -> { }, null));
    }

    private static Jugador jugador(String nombre, int numero) {
        return new Jugador(numero, nombre, FabricaFlota.crearFlotaEstandar(numero == 1 ? 1 : 7));
    }
}
