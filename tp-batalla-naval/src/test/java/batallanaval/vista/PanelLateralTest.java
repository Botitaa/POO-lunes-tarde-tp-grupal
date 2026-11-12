package batallanaval.vista;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import batallanaval.controlador.EstadoBarco;
import batallanaval.controlador.EstadoJugador;
import batallanaval.controlador.FasePartida;
import batallanaval.excepciones.HabilidadNoDisponibleException;
import batallanaval.modelo.Posicion;

/**
 * Prueba que {@link PanelLateral} muestra lo que dice la partida, habilita los botones según
 * ese estado y, al tocarlos, solo le pide la acción a la partida. No necesita pantalla:
 * los componentes se crean pero no se muestran.
 */
class PanelLateralTest {

    private static final Posicion ORIGEN = new Posicion(1, 4);

    private final PartidaFalsa partida = new PartidaFalsa();
    private final List<String> errores = new ArrayList<>();

    private PanelLateral crearPanel() {
        PanelLateral panel = new PanelLateral(partida, errores::add);
        partida.agregarObservador(panel);
        return panel;
    }

    // Encabezado

    @Test
    void muestraTurnoRondaPuntosDeMovimientoYDominacionDeAmbos() {
        partida.ronda = 12;
        partida.activo = new EstadoJugador("Bota", 1, 4, 2, 6, 30);
        partida.rival = new EstadoJugador("Santi", 2, 6, 1, 5, 28);

        PanelLateral panel = crearPanel();

        assertEquals("Turno de Bota", texto(panel, "etiquetaTurno"));
        assertEquals("Ronda: 12", texto(panel, "etiquetaRonda"));
        assertEquals("Puntos de movimiento: 4", texto(panel, "etiquetaPuntosMovimiento"));
        assertEquals("Bota: 2", texto(panel, "etiquetaDominacionActivo"));
        assertEquals("Santi: 1", texto(panel, "etiquetaDominacionRival"));
    }

    // Ficha del barco

    @Test
    void sinBarcoSeleccionadoAvisaYSoloDejaTerminarElTurno() {
        PanelLateral panel = crearPanel();

        assertEquals("Ningún barco seleccionado", texto(panel, "etiquetaTipo"));
        assertFalse(boton(panel, "Confirmar movimiento").isEnabled());
        assertFalse(boton(panel, "Deshacer paso").isEnabled());
        assertFalse(boton(panel, "Usar habilidad").isEnabled());
        assertFalse(boton(panel, "Disparar").isEnabled());
        assertTrue(boton(panel, "Terminar turno").isEnabled());
    }

    @Test
    void conBarcoSeleccionadoMuestraSuFicha() {
        partida.seleccionado = new EstadoBarco(3, 1, 'A', ORIGEN, 6, 8, 2, 1, 2, false, 6, 5, true);

        PanelLateral panel = crearPanel();

        assertEquals("Acorazado (1,4)", texto(panel, "etiquetaTipo"));
        assertEquals("Vida: 6 / 8", texto(panel, "etiquetaVida"));
        assertEquals("Casillas restantes: 2", texto(panel, "etiquetaCasillas"));
        assertEquals("Cooldown de disparo: 1", texto(panel, "etiquetaCooldownDisparo"));
        assertEquals("Habilidad: en espera (2 turnos)", texto(panel, "etiquetaHabilidad"));
    }

    // Botones habilitados según el estado

    @Test
    void confirmarYDeshacerSeHabilitanSoloConUnaRutaPlanificada() {
        partida.seleccionado = PartidaFalsa.crucero(2, ORIGEN);
        PanelLateral panel = crearPanel();
        assertFalse(boton(panel, "Confirmar movimiento").isEnabled());

        partida.ruta.add(new Posicion(2, 4));
        partida.ruta.add(new Posicion(3, 4));
        partida.costoRuta = 4;
        panel.actualizar();

        assertTrue(boton(panel, "Confirmar movimiento").isEnabled());
        assertTrue(boton(panel, "Deshacer paso").isEnabled());
        assertEquals("Ruta: 2 pasos, costo 4", texto(panel, "etiquetaRuta"));
    }

    @Test
    void usarHabilidadSeDeshabilitaSiEstaEnEsperaOYaEstaActiva() {
        partida.seleccionado = PartidaFalsa.crucero(2, ORIGEN);
        PanelLateral panel = crearPanel();
        assertTrue(boton(panel, "Usar habilidad").isEnabled(), "disponible: se puede tocar");

        partida.seleccionado = new EstadoBarco(2, 1, 'C', ORIGEN, 5, 5, 3, 0, 3, false, 4, 3, true);
        panel.actualizar();
        assertFalse(boton(panel, "Usar habilidad").isEnabled(), "en espera: deshabilitado");

        partida.seleccionado = new EstadoBarco(2, 1, 'D', ORIGEN, 3, 3, 4, 0, 0, true, 2, 2, true);
        panel.actualizar();
        assertFalse(boton(panel, "Usar habilidad").isEnabled(), "ya activa este turno: deshabilitado");
        assertEquals("Habilidad: activa este turno", texto(panel, "etiquetaHabilidad"));
    }

    @Test
    void dispararSeHabilitaSoloSiLaPartidaDevuelveObjetivos() {
        partida.seleccionado = PartidaFalsa.crucero(2, ORIGEN);
        PanelLateral panel = crearPanel();
        assertFalse(boton(panel, "Disparar").isEnabled());

        partida.objetivos.add(PartidaFalsa.destructorEnemigo(8, new Posicion(4, 4), 2));
        panel.actualizar();

        assertTrue(boton(panel, "Disparar").isEnabled());
        assertEquals(1, combo(panel).getItemCount());
        assertEquals("Destructor (4,4) - vida 2/3", combo(panel).getItemAt(0).toString());
    }

    @Test
    void fueraDelCombateTodoQuedaDeshabilitado() {
        partida.seleccionado = PartidaFalsa.crucero(2, ORIGEN);
        partida.ruta.add(new Posicion(2, 4));
        partida.fase = FasePartida.TERMINADA;
        partida.terminada = true;

        PanelLateral panel = crearPanel();

        for (String texto : List.of("Confirmar movimiento", "Deshacer paso", "Usar habilidad",
                "Disparar", "Terminar turno")) {
            assertFalse(boton(panel, texto).isEnabled(), texto + " tiene que estar deshabilitado");
        }
        assertEquals("Ningún barco seleccionado", texto(panel, "etiquetaTipo"));
    }

    // Los botones solo le piden la acción a la partida

    @Test
    void cadaBotonLlamaASuAccionDeLaPartida() {
        partida.seleccionado = PartidaFalsa.crucero(2, ORIGEN);
        partida.ruta.add(new Posicion(2, 4));
        PanelLateral panel = crearPanel();

        boton(panel, "Confirmar movimiento").doClick();
        boton(panel, "Deshacer paso").doClick();
        boton(panel, "Usar habilidad").doClick();
        boton(panel, "Terminar turno").doClick();

        assertEquals(1, partida.vecesConfirmarMovimiento);
        assertEquals(1, partida.vecesDeshacerPaso);
        assertEquals(1, partida.vecesUsarHabilidad);
        assertEquals(1, partida.vecesTerminarTurno);
        assertTrue(errores.isEmpty());
    }

    @Test
    void dispararUsaElIdDelObjetivoElegidoEnLaLista() {
        partida.seleccionado = PartidaFalsa.crucero(2, ORIGEN);
        partida.objetivos.add(PartidaFalsa.destructorEnemigo(8, new Posicion(4, 4), 3));
        partida.objetivos.add(PartidaFalsa.destructorEnemigo(9, new Posicion(4, 6), 1));
        PanelLateral panel = crearPanel();

        combo(panel).setSelectedIndex(1);
        boton(panel, "Disparar").doClick();

        assertEquals(List.of(9), partida.disparosA);
    }

    @Test
    void alRefrescarSeMantieneElObjetivoElegidoSiSigueSiendoValido() {
        partida.seleccionado = PartidaFalsa.crucero(2, ORIGEN);
        partida.objetivos.add(PartidaFalsa.destructorEnemigo(8, new Posicion(4, 4), 3));
        partida.objetivos.add(PartidaFalsa.destructorEnemigo(9, new Posicion(4, 6), 1));
        PanelLateral panel = crearPanel();
        combo(panel).setSelectedIndex(1);

        panel.actualizar();

        assertEquals("Destructor (4,6) - vida 1/3", combo(panel).getSelectedItem().toString());
    }

    @Test
    void siLaPartidaRechazaLaAccionSeMuestraSuMensaje() {
        partida.seleccionado = PartidaFalsa.crucero(2, ORIGEN);
        PanelLateral panel = crearPanel();
        partida.rechazo = new HabilidadNoDisponibleException("La habilidad está en espera.");

        boton(panel, "Usar habilidad").doClick();

        assertEquals(List.of("La habilidad está en espera."), errores);
        assertEquals(0, partida.vecesUsarHabilidad);
    }

    // Observador

    @Test
    void cuandoLaPartidaAvisaUnCambioElPanelSeRefrescaEnElEdt()
            throws InterruptedException, InvocationTargetException {
        PanelLateral panel = crearPanel();
        partida.seleccionado = PartidaFalsa.crucero(2, ORIGEN);
        partida.activo = new EstadoJugador("Bota", 1, 3, 0, 6, 34);

        panel.onEstadoCambiado();
        esperarAlEdt();

        assertEquals("Crucero (1,4)", texto(panel, "etiquetaTipo"));
        assertEquals("Puntos de movimiento: 3", texto(panel, "etiquetaPuntosMovimiento"));
    }

    @Test
    void despuesDeUnaAccionAceptadaElPanelMuestraElEstadoNuevo()
            throws InterruptedException, InvocationTargetException {
        partida.seleccionado = PartidaFalsa.crucero(2, ORIGEN);
        partida.objetivos.add(PartidaFalsa.destructorEnemigo(8, new Posicion(4, 4), 3));
        PanelLateral panel = crearPanel();

        // Como hace la partida real: al disparar el objetivo deja de ser válido y avisa el cambio.
        partida.objetivos.clear();
        boton(panel, "Disparar").doClick();
        esperarAlEdt();

        assertFalse(boton(panel, "Disparar").isEnabled());
        assertEquals(List.of(8), partida.disparosA);
    }

    @Test
    void noSeAceptanArgumentosNulos() {
        assertThrows(NullPointerException.class, () -> new PanelLateral(null, errores::add));
        assertThrows(NullPointerException.class, () -> new PanelLateral(partida, null));
    }

    // Ayudas

    /** Espera a que el EDT procese lo que tenía pendiente (los {@code invokeLater} del panel). */
    static void esperarAlEdt() throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(() -> { });
    }

    private static String texto(Container panel, String nombre) {
        Component componente = buscarPorNombre(panel, nombre);
        assertNotNull(componente, "no está la etiqueta " + nombre);
        return ((JLabel) componente).getText();
    }

    @SuppressWarnings("unchecked")
    private static JComboBox<Object> combo(Container panel) {
        Component componente = buscarPorNombre(panel, "comboObjetivos");
        assertNotNull(componente, "no está la lista de objetivos");
        return (JComboBox<Object>) componente;
    }

    static JButton boton(Container contenedor, String texto) {
        JButton encontrado = buscarBoton(contenedor, texto);
        assertNotNull(encontrado, "no está el botón " + texto);
        return encontrado;
    }

    /** Recorre el árbol de componentes y devuelve el que tiene ese nombre, o {@code null}. */
    static Component buscarPorNombre(Container contenedor, String nombre) {
        for (Component componente : contenedor.getComponents()) {
            if (nombre.equals(componente.getName())) {
                return componente;
            }
            if (componente instanceof Container) {
                Component hallado = buscarPorNombre((Container) componente, nombre);
                if (hallado != null) {
                    return hallado;
                }
            }
        }
        return null;
    }

    private static JButton buscarBoton(Container contenedor, String texto) {
        for (Component componente : contenedor.getComponents()) {
            if (componente instanceof JButton && texto.equals(((JButton) componente).getText())) {
                return (JButton) componente;
            }
            if (componente instanceof Container) {
                JButton hallado = buscarBoton((Container) componente, texto);
                if (hallado != null) {
                    return hallado;
                }
            }
        }
        return null;
    }
}
