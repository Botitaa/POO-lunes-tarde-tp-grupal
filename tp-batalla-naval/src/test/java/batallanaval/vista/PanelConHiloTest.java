package batallanaval.vista;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serial;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PanelConHiloTest {

    private final PanelDePrueba panel = new PanelDePrueba(10);

    /** Deja andando el hilo de eventos de Swing, para que ningún test dependa de cuándo arranca. */
    @BeforeAll
    static void arrancarElHiloDeEventos() throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(() -> { });
    }

    @AfterEach
    void frenarElHilo() {
        panel.detener();
    }

    @Test
    void iniciarArrancaElHiloYEstaCorriendoEsTrue() {
        assertFalse(panel.estaCorriendo(), "antes de iniciar no tiene que estar corriendo");

        panel.iniciar();

        assertTrue(panel.estaCorriendo());
    }

    @Test
    void detenerHaceQueElHiloTermineEnMenosDeUnSegundo() throws InterruptedException {
        panel.iniciar();
        Thread hilo = panel.getHilo();

        panel.detener();
        hilo.join(1000);

        assertFalse(hilo.isAlive(), "el hilo tendría que haber terminado");
        assertFalse(panel.estaCorriendo());
    }

    @Test
    void iniciarYDetenerAlInstanteTambienTerminaElHilo() throws InterruptedException {
        panel.iniciar();
        Thread hilo = panel.getHilo();

        panel.detener();
        hilo.join(1000);

        assertFalse(hilo.isAlive(), "detener() justo después de iniciar() tiene que frenar el hilo");
    }

    @Test
    void iniciarDosVecesSeguidasNoCreaDosHilos() {
        panel.iniciar();
        Thread primero = panel.getHilo();

        panel.iniciar();

        assertSame(primero, panel.getHilo());
    }

    @Test
    void detenerSinHaberIniciadoNoRompeNada() {
        assertDoesNotThrow(panel::detener);
        assertFalse(panel.estaCorriendo());
    }

    @Test
    void detenerDosVecesSeguidasNoRompeNada() {
        panel.iniciar();

        assertDoesNotThrow(panel::detener);
        assertDoesNotThrow(panel::detener);
    }

    @Test
    void sePuedeIniciarDeNuevoDespuesDeDetener() throws InterruptedException {
        panel.iniciar();
        Thread primero = panel.getHilo();
        panel.detener();
        primero.join(1000);

        panel.iniciar();

        assertTrue(panel.estaCorriendo());
        assertNotSame(primero, panel.getHilo());
    }

    @Test
    void actualizarCorreEnElHiloDeEventosDeSwing() throws InterruptedException {
        panel.iniciar();

        esperarActualizaciones(3);
        panel.detener();

        assertTrue(panel.actualizaciones.get() >= 3, "tendría que haber actualizado varias veces");
        assertFalse(panel.corrioFueraDelEdt.get(), "actualizar() solo puede correr en el EDT");
    }

    @Test
    void interruptedExceptionCortaElBucle() throws InterruptedException {
        panel.iniciar();
        Thread hilo = panel.getHilo();

        // Interrumpido sin pasar por detener(): lo mismo que pasa cuando sleep() lanza InterruptedException.
        hilo.interrupt();
        hilo.join(1000);

        assertFalse(hilo.isAlive(), "la interrupción tiene que cortar el bucle");
    }

    @Test
    void iniciarConIntervaloInvalidoLanzaExcepcion() {
        PanelDePrueba panelInvalido = new PanelDePrueba(0);

        assertThrows(IllegalStateException.class, panelInvalido::iniciar);
        assertFalse(panelInvalido.estaCorriendo());
    }

    @Test
    void iniciarDetenerYRunSonFinal() throws NoSuchMethodException {
        for (String nombre : List.of("iniciar", "detener", "estaCorriendo", "run")) {
            int modificadores = PanelConHilo.class.getMethod(nombre).getModifiers();
            assertTrue(Modifier.isFinal(modificadores), nombre + "() tiene que ser final (Template Method)");
        }
    }

    /** Espera, con un límite de 2 segundos, a que el panel haya actualizado al menos {@code minimo} veces. */
    private void esperarActualizaciones(int minimo) throws InterruptedException {
        long limite = System.currentTimeMillis() + 2000;
        while (panel.actualizaciones.get() < minimo && System.currentTimeMillis() < limite) {
            Thread.sleep(10);
        }
    }

    /** Subclase mínima: cuenta las actualizaciones y registra si alguna corrió fuera del EDT. */
    private static final class PanelDePrueba extends PanelConHilo {

        @Serial
        private static final long serialVersionUID = 1L;

        private final transient AtomicInteger actualizaciones = new AtomicInteger();
        private final transient AtomicBoolean corrioFueraDelEdt = new AtomicBoolean();
        private final int intervaloMs;

        PanelDePrueba(int intervaloMs) {
            this.intervaloMs = intervaloMs;
        }

        @Override
        protected void actualizar() {
            if (!SwingUtilities.isEventDispatchThread()) {
                corrioFueraDelEdt.set(true);
            }
            actualizaciones.incrementAndGet();
        }

        @Override
        protected int getIntervaloMs() {
            return intervaloMs;
        }
    }
}
