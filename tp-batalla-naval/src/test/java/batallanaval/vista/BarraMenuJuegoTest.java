package batallanaval.vista;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.awt.Component;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import org.junit.jupiter.api.Test;

import batallanaval.controlador.FasePartida;

/**
 * Prueba que {@link BarraMenuJuego} tiene los menús del diseño, que cada opción ejecuta su
 * acción, que abandonar pide confirmación y que las opciones se habilitan según lo que dice
 * la partida. No abre ningún diálogo: la confirmación se reemplaza por una respuesta fija.
 */
class BarraMenuJuegoTest {

    private final List<String> acciones = new ArrayList<>();
    private final PartidaFalsa partida = new PartidaFalsa();

    // Respuesta que da el "jugador" cuando se le pregunta si abandona, y cuántas veces se le preguntó.
    private boolean respuestaConfirmacion = true;
    private int vecesPreguntado = 0;

    private BarraMenuJuego crearBarra() {
        return new BarraMenuJuego(
                () -> acciones.add("guardar"),
                () -> acciones.add("cargar"),
                () -> acciones.add("abandonar"),
                () -> acciones.add("reglas"),
                () -> {
                    vecesPreguntado++;
                    return respuestaConfirmacion;
                });
    }

    // Estructura

    @Test
    void tieneLosMenusPartidaYAyudaConSusOpcionesEnOrden() {
        BarraMenuJuego barra = crearBarra();

        assertEquals(2, barra.getMenuCount());
        JMenu partidaMenu = barra.getMenu(0);
        JMenu ayuda = barra.getMenu(1);
        assertEquals("Partida", partidaMenu.getText());
        assertEquals("Ayuda", ayuda.getText());

        // Guardar, Cargar, separador (getItem devuelve null) y Abandonar
        assertEquals("Guardar partida", partidaMenu.getItem(0).getText());
        assertEquals("Cargar partida", partidaMenu.getItem(1).getText());
        assertNull(partidaMenu.getItem(2), "entre Cargar y Abandonar va un separador");
        assertEquals("Abandonar partida", partidaMenu.getItem(3).getText());
        assertEquals("Reglas", ayuda.getItem(0).getText());
    }

    @Test
    void lasOpcionesTienenAtajosDeTeclado() {
        BarraMenuJuego barra = crearBarra();

        assertEquals(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK),
                item(barra, "itemGuardar").getAccelerator());
        assertEquals(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK),
                item(barra, "itemCargar").getAccelerator());
        assertEquals(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), item(barra, "itemReglas").getAccelerator());
        // Abandonar no tiene atajo a propósito: no se tiene que poder hacer sin querer.
        assertNull(item(barra, "itemAbandonar").getAccelerator());
    }

    // Habilitado según la partida

    @Test
    void sinPartidaSoloSePuedeCargarYVerLasReglas() {
        BarraMenuJuego barra = crearBarra();

        assertFalse(item(barra, "itemGuardar").isEnabled());
        assertFalse(item(barra, "itemAbandonar").isEnabled());
        assertTrue(item(barra, "itemCargar").isEnabled());
        assertTrue(item(barra, "itemReglas").isEnabled());
    }

    @Test
    void guardarSeHabilitaSoloSiLaPartidaDiceQueSePuedeGuardar() {
        BarraMenuJuego barra = crearBarra();
        partida.puedeGuardarse = false;
        barra.mostrarPartida(partida);
        assertFalse(item(barra, "itemGuardar").isEnabled(), "a mitad de turno no se guarda");
        assertTrue(item(barra, "itemAbandonar").isEnabled(), "pero sí se puede abandonar");

        partida.puedeGuardarse = true;
        barra.actualizar();
        assertTrue(item(barra, "itemGuardar").isEnabled());
    }

    @Test
    void conLaPartidaTerminadaNoSePuedeGuardarNiAbandonar() {
        partida.puedeGuardarse = true;
        partida.fase = FasePartida.TERMINADA;
        partida.terminada = true;
        BarraMenuJuego barra = crearBarra();

        barra.mostrarPartida(partida);

        assertFalse(item(barra, "itemGuardar").isEnabled());
        assertFalse(item(barra, "itemAbandonar").isEnabled());
        assertTrue(item(barra, "itemCargar").isEnabled());
    }

    @Test
    void alVolverAlInicioSinPartidaSeDeshabilitanGuardarYAbandonar() {
        partida.puedeGuardarse = true;
        BarraMenuJuego barra = crearBarra();
        barra.mostrarPartida(partida);

        barra.mostrarPartida(null);

        assertFalse(item(barra, "itemGuardar").isEnabled());
        assertFalse(item(barra, "itemAbandonar").isEnabled());
    }

    @Test
    void cuandoEmpiezaUnTurnoSeVuelveALeerSiSePuedeGuardar()
            throws InterruptedException, InvocationTargetException {
        BarraMenuJuego barra = crearBarra();
        barra.mostrarPartida(partida);
        partida.agregarObservador(barra);
        assertFalse(item(barra, "itemGuardar").isEnabled());

        partida.puedeGuardarse = true;
        barra.onTurnoIniciado("Santi");
        PanelLateralTest.esperarAlEdt();

        assertTrue(item(barra, "itemGuardar").isEnabled());
    }

    @Test
    void cuandoTerminaLaPartidaSeDeshabilitanGuardarYAbandonar()
            throws InterruptedException, InvocationTargetException {
        partida.puedeGuardarse = true;
        BarraMenuJuego barra = crearBarra();
        barra.mostrarPartida(partida);

        partida.terminada = true;
        partida.fase = FasePartida.TERMINADA;
        barra.onPartidaTerminada(null);
        PanelLateralTest.esperarAlEdt();

        assertFalse(item(barra, "itemGuardar").isEnabled());
        assertFalse(item(barra, "itemAbandonar").isEnabled());
    }

    // Acciones

    @Test
    void cadaOpcionEjecutaSuAccion() {
        partida.puedeGuardarse = true;
        BarraMenuJuego barra = crearBarra();
        barra.mostrarPartida(partida);

        item(barra, "itemGuardar").doClick();
        item(barra, "itemCargar").doClick();
        item(barra, "itemReglas").doClick();

        assertEquals(List.of("guardar", "cargar", "reglas"), acciones);
    }

    @Test
    void abandonarPideConfirmacionYSiElJugadorConfirmaAbandona() {
        BarraMenuJuego barra = crearBarra();
        barra.mostrarPartida(partida);
        respuestaConfirmacion = true;

        item(barra, "itemAbandonar").doClick();

        assertEquals(1, vecesPreguntado);
        assertEquals(List.of("abandonar"), acciones);
    }

    @Test
    void siElJugadorNoConfirmaNoSeAbandona() {
        BarraMenuJuego barra = crearBarra();
        barra.mostrarPartida(partida);
        respuestaConfirmacion = false;

        item(barra, "itemAbandonar").doClick();

        assertEquals(1, vecesPreguntado);
        assertTrue(acciones.isEmpty(), "la partida sigue");
    }

    @Test
    void lasOtrasOpcionesNoPidenConfirmacion() {
        partida.puedeGuardarse = true;
        BarraMenuJuego barra = crearBarra();
        barra.mostrarPartida(partida);

        item(barra, "itemGuardar").doClick();
        item(barra, "itemCargar").doClick();
        item(barra, "itemReglas").doClick();

        assertEquals(0, vecesPreguntado);
    }

    @Test
    void noSeAceptanAccionesNulas() {
        Runnable nada = () -> { };
        assertThrows(NullPointerException.class, () -> new BarraMenuJuego(null, nada, nada, nada));
        assertThrows(NullPointerException.class, () -> new BarraMenuJuego(nada, null, nada, nada));
        assertThrows(NullPointerException.class, () -> new BarraMenuJuego(nada, nada, null, nada));
        assertThrows(NullPointerException.class, () -> new BarraMenuJuego(nada, nada, nada, null));
        assertThrows(NullPointerException.class, () -> new BarraMenuJuego(nada, nada, nada, nada, null));
    }

    // Ayudas

    /** Busca un ítem por nombre dentro de los menús de la barra. */
    private static JMenuItem item(BarraMenuJuego barra, String nombre) {
        for (int i = 0; i < barra.getMenuCount(); i++) {
            for (Component componente : barra.getMenu(i).getMenuComponents()) {
                if (nombre.equals(componente.getName())) {
                    return (JMenuItem) componente;
                }
            }
        }
        return fail("no está el ítem " + nombre);
    }
}
