package batallanaval.vista;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import batallanaval.modelo.Posicion;

/**
 * Prueba lo que {@link PanelTablero} le deja decidir a quien lo arma: qué hace un clic y el modo
 * despliegue (sin niebla y sin atajos de combate). No hace falta pantalla: el tablero se dibuja en
 * una imagen en memoria.
 */
class PanelTableroTest {

    private static final int LADO = 500;
    private static final int CELDA = LADO / 10;

    private PartidaConFiltro partida;
    private PanelTablero panel;
    private final List<Posicion> clics = new ArrayList<>();

    @BeforeEach
    void armarElTablero() {
        partida = new PartidaConFiltro();
        panel = new PanelTablero(partida, partida);
        panel.setSize(LADO, LADO);
    }

    // Clics

    @Test
    void unClicEnUnaCasillaInformaSuPosicion() {
        panel.setAlClickearCasilla(clics::add);

        clicEn(2, 3);

        assertEquals(List.of(new Posicion(2, 3)), clics);
    }

    @Test
    void cadaClicSeInformaPorSeparado() {
        panel.setAlClickearCasilla(clics::add);

        clicEn(0, 0);
        clicEn(9, 9);

        assertEquals(List.of(new Posicion(0, 0), new Posicion(9, 9)), clics);
    }

    @Test
    void sinAccionConfiguradaElClicNoHaceNada() {
        clicEn(4, 4);

        assertTrue(clics.isEmpty());
    }

    @Test
    void unClicFueraDeLaGrillaNoSeInforma() {
        panel.setSize(LADO + 100, LADO); // sobra lugar a los costados: la grilla queda centrada
        panel.setAlClickearCasilla(clics::add);

        panel.mouseClicked(new MouseEvent(panel, MouseEvent.MOUSE_CLICKED, 0L, 0, 10, 10, 1, false));

        assertTrue(clics.isEmpty());
    }

    // Modo despliegue: niebla

    @Test
    void sinModoDesplieguePintaLaNieblaEnLasCasillasQueNoSeVen() {
        int conVision = colorDeLaCasilla00();
        partida.visible = false;

        int sinVision = colorDeLaCasilla00();

        assertNotEquals(conVision, sinVision);
    }

    @Test
    void enModoDesplieguePintaLasCasillasSinNieblaAunqueLaPartidaDigaQueNoSeVen() {
        int conVision = colorDeLaCasilla00();
        partida.visible = false;
        panel.setModoDespliegue(true);

        assertEquals(conVision, colorDeLaCasilla00());
    }

    // Modo despliegue: atajos de teclado

    @Test
    void sinModoDespliegueLosAtajosLlamanALaPartida() {
        dispararAtajo("confirmarMovimiento");
        dispararAtajo("deshacerPaso");

        assertEquals(1, partida.vecesConfirmarMovimiento);
        assertEquals(1, partida.vecesDeshacerPaso);
    }

    @Test
    void enModoDesplieguePasarEnterOBackspaceNoLlamaALaPartida() {
        panel.setModoDespliegue(true);

        dispararAtajo("confirmarMovimiento");
        dispararAtajo("deshacerPaso");
        dispararAtajo("cancelarSeleccion");

        assertEquals(0, partida.vecesConfirmarMovimiento);
        assertEquals(0, partida.vecesDeshacerPaso);
    }

    @Test
    void alSalirDelModoDespliegueLosAtajosVuelvenAFuncionar() {
        panel.setModoDespliegue(true);
        panel.setModoDespliegue(false);

        dispararAtajo("confirmarMovimiento");

        assertEquals(1, partida.vecesConfirmarMovimiento);
    }

    @Test
    void nombreTipoTraduceElSimboloDelBarco() {
        assertEquals("Acorazado", PanelTablero.nombreTipo('A'));
        assertEquals("Crucero", PanelTablero.nombreTipo('C'));
        assertEquals("Destructor", PanelTablero.nombreTipo('D'));
        assertEquals("Barco", PanelTablero.nombreTipo('?'));
    }

    // Ayudas

    /** Partida falsa en la que el test decide si se ve o no el mapa. */
    private static final class PartidaConFiltro extends PartidaFalsa {
        boolean visible = true;

        @Override
        public boolean esVisible(Posicion posicion) {
            return visible;
        }
    }

    /** Hace clic en el centro de una casilla (la grilla ocupa todo el panel: cada casilla mide {@code CELDA}). */
    private void clicEn(int fila, int columna) {
        int x = columna * CELDA + CELDA / 2;
        int y = fila * CELDA + CELDA / 2;
        panel.mouseClicked(new MouseEvent(panel, MouseEvent.MOUSE_CLICKED, 0L, 0, x, y, 1, false));
    }

    private void dispararAtajo(String nombre) {
        panel.getActionMap().get(nombre).actionPerformed(new ActionEvent(panel, ActionEvent.ACTION_PERFORMED, nombre));
    }

    /** Dibuja el tablero en una imagen y devuelve el color de un punto de la casilla (0, 0). */
    private int colorDeLaCasilla00() {
        BufferedImage imagen = new BufferedImage(LADO, LADO, BufferedImage.TYPE_INT_ARGB);
        panel.paint(imagen.getGraphics());
        return imagen.getRGB(CELDA / 4, CELDA / 4);
    }
}
