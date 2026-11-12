package batallanaval.vista;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.BorderLayout;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;

import org.junit.jupiter.api.Test;

import batallanaval.controlador.FasePartida;
import batallanaval.excepciones.AccionInvalidaException;
import batallanaval.modelo.Posicion;

/**
 * Prueba que {@link PanelJuego} arma la pantalla con el tablero al centro y el panel lateral
 * a la derecha, y que reenvía los avisos de la partida a los dos.
 */
class PanelJuegoTest {

    private final PartidaFalsa partida = new PartidaFalsa();
    private final List<String> errores = new ArrayList<>();
    private final PanelJuego panel = new PanelJuego(partida, errores::add);

    @Test
    void elTableroVaAlCentroYElPanelLateralALaDerecha() {
        BorderLayout disposicion = (BorderLayout) panel.getLayout();

        assertSame(panel.getPanelTablero(), disposicion.getLayoutComponent(BorderLayout.CENTER));
        assertSame(panel.getPanelLateral(), disposicion.getLayoutComponent(BorderLayout.EAST));
    }

    @Test
    void registrandoSoloLaPantallaElPanelLateralSeEnteraDeLosCambios()
            throws InterruptedException, InvocationTargetException {
        partida.agregarObservador(panel);
        partida.seleccionado = PartidaFalsa.crucero(2, new Posicion(1, 4));

        // La partida avisa un cambio (acá, al agregar un paso a la ruta).
        partida.agregarPaso(new Posicion(2, 4));
        PanelLateralTest.esperarAlEdt();

        JLabel tipo = (JLabel) PanelLateralTest.buscarPorNombre(panel, "etiquetaTipo");
        assertEquals("Crucero (1,4)", tipo.getText());
        assertTrue(PanelLateralTest.boton(panel, "Confirmar movimiento").isEnabled());
    }

    @Test
    void alTerminarLaPartidaSeBloqueanLosBotones() throws InterruptedException, InvocationTargetException {
        partida.fase = FasePartida.TERMINADA;
        partida.terminada = true;

        panel.onPartidaTerminada(null);
        PanelLateralTest.esperarAlEdt();

        assertFalse(PanelLateralTest.boton(panel, "Terminar turno").isEnabled());
    }

    @Test
    void losErroresDeLosBotonesLleganAQuienArmoLaPantalla() {
        partida.rechazo = new AccionInvalidaException("No es tu turno.");

        PanelLateralTest.boton(panel, "Terminar turno").doClick();

        assertEquals(List.of("No es tu turno."), errores);
    }

    @Test
    void noSeAceptanArgumentosNulos() {
        assertThrows(NullPointerException.class, () -> new PanelJuego(null, errores::add));
        assertThrows(NullPointerException.class, () -> new PanelJuego(partida, null));
    }
}
