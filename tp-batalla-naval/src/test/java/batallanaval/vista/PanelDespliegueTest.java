package batallanaval.vista;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import batallanaval.controlador.EstadoBarco;
import batallanaval.controlador.FasePartida;
import batallanaval.controlador.GestorPartida;
import batallanaval.modelo.FabricaFlota;
import batallanaval.modelo.Jugador;
import batallanaval.modelo.Posicion;
import batallanaval.modelo.Tablero;

/**
 * Prueba la pantalla de despliegue con una {@link GestorPartida} de verdad (mapa estándar: J1 despliega en
 * las filas 0 a 2 y J2 en las 7 a 9; flota de J1 con ids 1 a 6 y de J2 con ids 7 a 12). Los clics sobre
 * el tablero se hacen con eventos de mouse reales, así se prueba también el cableado con {@link PanelTablero}.
 */
class PanelDespliegueTest {

    private static final int LADO = 500;
    private static final int CELDA = LADO / 10;

    private GestorPartida partida;
    private PanelDespliegue panel;
    private final List<String> errores = new ArrayList<>();

    @BeforeEach
    void armarLaPantalla() {
        Jugador ana = new Jugador(1, "Ana", FabricaFlota.crearFlotaEstandar(1));
        Jugador beto = new Jugador(2, "Beto", FabricaFlota.crearFlotaEstandar(7));
        partida = new GestorPartida(ana, beto, Tablero.crearMapaEstandar());
        panel = new PanelDespliegue(partida, errores::add);
        panel.getPanelTablero().setSize(LADO, LADO);
    }

    // Estado inicial

    @Test
    void alCrearseMuestraAlJugadorActivoYSusSeisBarcosSinUbicar() {
        assertEquals("Despliegue de Ana", texto("tituloDespliegue"));
        assertEquals(6, lista().getModel().getSize());
        assertEquals("Ubicados: 0 de 6", texto("etiquetaProgreso"));
    }

    @Test
    void alCrearseQuedaSeleccionadoElPrimerBarcoSinUbicar() {
        EstadoBarco primero = partida.getBarcosSinUbicar().get(0);

        assertEquals(primero, lista().getSelectedValue());
        assertTrue(texto("etiquetaSeleccion").endsWith("(sin ubicar)"));
    }

    @Test
    void alCrearseNoSePuedeRetirarNiConfirmar() {
        assertFalse(boton("botonRetirar").isEnabled());
        assertFalse(boton("botonConfirmar").isEnabled());
    }

    // Ubicar

    @Test
    void unClicEnLaZonaUbicaElBarcoSeleccionadoYPasaAlSiguiente() {
        List<EstadoBarco> antes = partida.getBarcosSinUbicar();

        clicEn(0, 0);

        assertEquals(new Posicion(0, 0), posicionDe(antes.get(0).id()));
        assertEquals(5, lista().getModel().getSize());
        assertEquals("Ubicados: 1 de 6", texto("etiquetaProgreso"));
        assertEquals(antes.get(1), lista().getSelectedValue(), "el siguiente barco queda seleccionado");
        assertTrue(errores.isEmpty());
    }

    @Test
    void ubicarConClicRespetaElBarcoElegidoEnLaLista() {
        List<EstadoBarco> antes = partida.getBarcosSinUbicar();
        lista().setSelectedIndex(3);

        clicEn(1, 1);

        assertEquals(new Posicion(1, 1), posicionDe(antes.get(3).id()));
    }

    @Test
    void unClicFueraDeLaZonaMuestraElMensajeDeLaPartida() {
        clicEn(5, 5);

        assertEquals(List.of("Solo podés ubicar barcos en tu zona de despliegue."), errores);
        assertEquals(6, lista().getModel().getSize(), "no se ubicó nada");
    }

    @Test
    void sinBarcoSeleccionadoUnClicEnUnaCasillaVaciaPideElegirUno() {
        ubicarLosSeis();

        clicEn(2, 0);

        assertEquals(List.of(PanelDespliegue.MENSAJE_ELEGIR_BARCO), errores);
    }

    @Test
    void unClicSobreUnBarcoYaUbicadoLoSeleccionaSinMoverNada() {
        EstadoBarco primero = partida.getBarcosSinUbicar().get(0);
        clicEn(0, 0);
        lista().setSelectedIndex(2);

        clicEn(0, 0);

        assertTrue(errores.isEmpty());
        assertEquals(new Posicion(0, 0), posicionDe(primero.id()));
        assertTrue(texto("etiquetaSeleccion").endsWith("en fila 0, columna 0"));
        assertTrue(boton("botonRetirar").isEnabled());
        assertTrue(lista().isSelectionEmpty(), "ya no hay un barco de la lista seleccionado");
    }

    @Test
    void unBarcoUbicadoSeMueveConUnClicEnOtraCasilla() {
        EstadoBarco primero = partida.getBarcosSinUbicar().get(0);
        clicEn(0, 0);
        clicEn(0, 0); // lo selecciona

        clicEn(1, 0);

        assertEquals(new Posicion(1, 0), posicionDe(primero.id()));
        assertEquals(1, partida.getBarcosPropios().stream().filter(b -> b.posicion() != null).count());
    }

    // Retirar

    @Test
    void retirarDevuelveElBarcoSeleccionadoALaLista() {
        EstadoBarco primero = partida.getBarcosSinUbicar().get(0);
        clicEn(0, 0);
        clicEn(0, 0); // selecciona el barco ubicado

        boton("botonRetirar").doClick();

        assertNull(posicionDe(primero.id()));
        assertEquals(6, lista().getModel().getSize());
        assertEquals("Ubicados: 0 de 6", texto("etiquetaProgreso"));
        assertFalse(boton("botonRetirar").isEnabled(), "ahora está sin ubicar: no hay nada para retirar");
        assertTrue(errores.isEmpty());
    }

    // Confirmar

    @Test
    void confirmarSeHabilitaSoloConLosSeisBarcosUbicados() {
        for (int columna = 0; columna < 5; columna++) {
            clicEn(0, columna);
        }
        assertFalse(boton("botonConfirmar").isEnabled());

        clicEn(0, 5);

        assertTrue(boton("botonConfirmar").isEnabled());
        assertEquals("Ubicados: 6 de 6", texto("etiquetaProgreso"));
        assertEquals("Ningún barco elegido", texto("etiquetaSeleccion"));
    }

    @Test
    void confirmarPasaAlDespliegueDelSegundoJugador() {
        ubicarLosSeis();

        boton("botonConfirmar").doClick();

        assertEquals(FasePartida.DESPLIEGUE_J2, partida.getFase());
        assertTrue(errores.isEmpty());
    }

    @Test
    void alActualizarParaElOtroJugadorMuestraSusBarcosYReiniciaLaSeleccion() {
        ubicarLosSeis();
        boton("botonConfirmar").doClick();

        panel.actualizar();

        assertEquals("Despliegue de Beto", texto("tituloDespliegue"));
        assertEquals(6, lista().getModel().getSize());
        assertEquals("Ubicados: 0 de 6", texto("etiquetaProgreso"));
        assertEquals(partida.getBarcosSinUbicar().get(0), lista().getSelectedValue());
        assertFalse(boton("botonConfirmar").isEnabled());
    }

    @Test
    void elSegundoJugadorUbicaEnSuZona() {
        ubicarLosSeis();
        boton("botonConfirmar").doClick();
        panel.actualizar();

        clicEn(9, 0);
        clicEn(2, 0); // zona de J1: la partida lo rechaza

        assertEquals(new Posicion(9, 0), posicionDe(7));
        assertEquals(List.of("Solo podés ubicar barcos en tu zona de despliegue."), errores);
    }

    @Test
    void lasInstruccionesIndicanLaZonaDeCadaJugador() {
        assertTrue(texto("instrucciones").contains("las 3 filas de arriba"));

        ubicarLosSeis();
        boton("botonConfirmar").doClick();
        panel.actualizar();

        assertTrue(texto("instrucciones").contains("las 3 filas de abajo"));
    }

    // Lista

    @Test
    void elegirUnBarcoEnLaListaActualizaLaSeleccion() {
        EstadoBarco tercero = partida.getBarcosSinUbicar().get(2);

        lista().setSelectedIndex(2);

        assertEquals("Elegido: " + PanelTablero.nombreTipo(tercero.simbolo()) + " (sin ubicar)",
                texto("etiquetaSeleccion"));
    }

    // Dibujo y avisos de la partida

    @Test
    void elTableroSeDibujaSinFallarEnElDespliegue() {
        // Durante el despliegue el rival tiene barcos sin posición: dibujar no tiene que explotar.
        BufferedImage imagen = new BufferedImage(LADO, LADO, BufferedImage.TYPE_INT_ARGB);

        assertDoesNotThrow(() -> panel.getPanelTablero().paint(imagen.getGraphics()));
        clicEn(0, 0);
        assertDoesNotThrow(() -> panel.getPanelTablero().paint(imagen.getGraphics()));
    }

    @Test
    void siLaPartidaAvisaUnCambioLaPantallaSeRefresca() throws InterruptedException, InvocationTargetException {
        partida.agregarObservador(panel);

        partida.ubicarBarco(partida.getBarcosSinUbicar().get(0).id(), new Posicion(1, 1));
        SwingUtilities.invokeAndWait(() -> { });

        assertEquals("Ubicados: 1 de 6", texto("etiquetaProgreso"));
    }

    @Test
    void noSeAceptanArgumentosNulos() {
        assertThrows(NullPointerException.class, () -> new PanelDespliegue(null, errores::add));
        assertThrows(NullPointerException.class, () -> new PanelDespliegue(partida, null));
    }

    // Ayudas

    /** Hace un clic real de mouse en el centro de la casilla, sobre el tablero de la pantalla. */
    private void clicEn(int fila, int columna) {
        int x = columna * CELDA + CELDA / 2;
        int y = fila * CELDA + CELDA / 2;
        panel.getPanelTablero().mouseClicked(
                new MouseEvent(panel.getPanelTablero(), MouseEvent.MOUSE_CLICKED, 0L, 0, x, y, 1, false));
    }

    /** Ubica los 6 barcos del jugador activo en la fila 0 (de su zona), uno por columna. */
    private void ubicarLosSeis() {
        for (int columna = 0; columna < 6; columna++) {
            clicEn(0, columna);
        }
    }

    private Posicion posicionDe(int idBarco) {
        return partida.getBarcosPropios().stream()
                .filter(barco -> barco.id() == idBarco)
                .findFirst()
                .orElseThrow()
                .posicion();
    }

    @SuppressWarnings("unchecked")
    private JList<EstadoBarco> lista() {
        return (JList<EstadoBarco>) buscar(panel, "listaSinUbicar");
    }

    private JButton boton(String nombre) {
        return (JButton) buscar(panel, nombre);
    }

    private String texto(String nombre) {
        return ((JLabel) buscar(panel, nombre)).getText();
    }

    private static Component buscar(Container raiz, String nombre) {
        for (Component hijo : raiz.getComponents()) {
            if (nombre.equals(hijo.getName())) {
                return hijo;
            }
            if (hijo instanceof Container contenedor) {
                Component encontrado = buscar(contenedor, nombre);
                if (encontrado != null) {
                    return encontrado;
                }
            }
        }
        return null;
    }

    private static void assertNull(Object valor) {
        assertEquals(null, valor);
    }
}
