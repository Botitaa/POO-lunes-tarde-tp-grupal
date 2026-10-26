package batallanaval.vista;

import batallanaval.controlador.PartidaTablero;
import batallanaval.modelo.Posicion;
import batallanaval.modelo.Casilla;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Objects;

/**
 * Panel que extiende de JPanel para representar la grilla de 10x10 del tablero de Batalla Naval.
 * Dibuja cada tipo de casilla, las zonas de despliegue y la zona de dominación.
 * Todo lo que dibuja se lo pregunta a PartidaTablero: el panel no conoce el Tablero.
 */
public class PanelTablero extends JPanel implements MouseListener, MouseMotionListener {

    private static final int FILAS = 10;
    private static final int COLUMNAS = 10;

    // Parte de la partida que necesita el panel para dibujar (ISP: no recibe la Partida entera)
    private final PartidaTablero partidaTablero;

    // Guarda la posición de la celda sobre la cual está el mouse actualmente
    private Posicion casillaHover = null;

    /**
     * Crea el panel del tablero.
     *
     * @param partidaTablero consultas de la partida de las que se leen las casillas y las zonas.
     * @throws NullPointerException si partidaTablero es nulo.
     */
    public PanelTablero(PartidaTablero partidaTablero) {
        this.partidaTablero = Objects.requireNonNull(partidaTablero, "La partida no puede ser nula.");
        setBackground(Color.DARK_GRAY);

        // Registramos los listeners para las acciones del mouse
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    /**
     * Dibuja las casillas, las zonas de despliegue, la zona de dominación
     * y el resaltado de la casilla bajo el mouse.
     *
     * @param g contexto gráfico que entrega Swing
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int tamanoCelda = calcularTamanoCelda();
        int inicioX = calcularInicioX(tamanoCelda);
        int inicioY = calcularInicioY(tamanoCelda);

        // Límites de la zona de dominación: se completan mientras se recorre la grilla
        int filaMinDominacion = FILAS;
        int filaMaxDominacion = -1;
        int colMinDominacion = COLUMNAS;
        int colMaxDominacion = -1;

        // Recorremos la matriz lógica de 10x10
        for (int fila = 0; fila < FILAS; fila++) {
            for (int col = 0; col < COLUMNAS; col++) {
                Posicion pos = new Posicion(fila, col);
                int x = inicioX + (col * tamanoCelda);
                int y = inicioY + (fila * tamanoCelda);

                // Le preguntamos a la partida qué casilla hay en esta posición
                Casilla casilla = partidaTablero.getCasilla(pos);

                // Asignamos el color de fondo según el tipo de casilla
                switch (casilla) {
                    case AGUA:
                        g2d.setColor(new Color(51, 153, 255)); // Azul agua
                        break;
                    case ISLA:
                        g2d.setColor(new Color(46, 139, 87));  // Verde isla
                        break;
                    case PIEDRA:
                        g2d.setColor(new Color(112, 128, 144)); // Gris piedra
                        break;
                    case RESTOS:
                        g2d.setColor(new Color(70, 70, 70));   // Fondo oscuro para restos
                        break;
                }
                g2d.fillRect(x, y, tamanoCelda, tamanoCelda);

                // Si la casilla contiene restos de un barco hundido, dibujamos una "X" negra
                if (casilla == Casilla.RESTOS) {
                    g2d.setColor(Color.BLACK);
                    g2d.setStroke(new BasicStroke(2));
                    g2d.drawLine(x + 4, y + 4, x + tamanoCelda - 4, y + tamanoCelda - 4);
                    g2d.drawLine(x + tamanoCelda - 4, y + 4, x + 4, y + tamanoCelda - 4);
                }

                // Marcamos las zonas de despliegue con un tinte semitransparente del color de cada jugador
                if (partidaTablero.esZonaDespliegue(pos, 1)) {
                    g2d.setColor(new Color(230, 90, 50, 50));  // Naranja: jugador 1
                    g2d.fillRect(x, y, tamanoCelda, tamanoCelda);
                } else if (partidaTablero.esZonaDespliegue(pos, 2)) {
                    g2d.setColor(new Color(170, 80, 220, 50)); // Violeta: jugador 2
                    g2d.fillRect(x, y, tamanoCelda, tamanoCelda);
                }

                // Si la casilla es de la zona de dominación, actualizamos sus límites
                if (partidaTablero.esZonaDominacion(pos)) {
                    filaMinDominacion = Math.min(filaMinDominacion, fila);
                    filaMaxDominacion = Math.max(filaMaxDominacion, fila);
                    colMinDominacion = Math.min(colMinDominacion, col);
                    colMaxDominacion = Math.max(colMaxDominacion, col);
                }

                // Dibujamos el borde estándar de la celda
                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(1));
                g2d.drawRect(x, y, tamanoCelda, tamanoCelda);
            }
        }

        // La zona de dominación y el hover se dibujan después del for: si se dibujaran
        // adentro, las celdas siguientes les pintarían el borde negro encima.

        // Marcamos la zona de dominación con un solo borde dorado alrededor de todas sus casillas
        if (filaMaxDominacion >= 0) {
            int x = inicioX + (colMinDominacion * tamanoCelda);
            int y = inicioY + (filaMinDominacion * tamanoCelda);
            int ancho = (colMaxDominacion - colMinDominacion + 1) * tamanoCelda;
            int alto = (filaMaxDominacion - filaMinDominacion + 1) * tamanoCelda;
            g2d.setColor(new Color(218, 165, 32)); // Dorado
            g2d.setStroke(new BasicStroke(3));
            g2d.drawRect(x, y, ancho, alto);
        }

        // Efecto Hover (resaltado blanco semitransparente)
        if (casillaHover != null) {
            int x = inicioX + (casillaHover.getColumna() * tamanoCelda);
            int y = inicioY + (casillaHover.getFila() * tamanoCelda);
            g2d.setColor(new Color(255, 255, 255, 80));
            g2d.fillRect(x, y, tamanoCelda, tamanoCelda);
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRect(x, y, tamanoCelda, tamanoCelda);
        }
    }

    // Cálculos de la grilla: los usan paintComponent y obtenerPosicionDesdeCoordenadas, así siempre coinciden

    /** Tamaño de cada celda para que la grilla entre entera en el panel. */
    private int calcularTamanoCelda() {
        return Math.min(getWidth() / COLUMNAS, getHeight() / FILAS);
    }

    /** Margen izquierdo para que la grilla quede centrada. */
    private int calcularInicioX(int tamanoCelda) {
        return (getWidth() - (tamanoCelda * COLUMNAS)) / 2;
    }

    /** Margen superior para que la grilla quede centrada. */
    private int calcularInicioY(int tamanoCelda) {
        return (getHeight() - (tamanoCelda * FILAS)) / 2;
    }

    /**
     * Traduce las coordenadas de píxeles de la pantalla a una Posicion (fila y columna de 0 a 9).
     * Devuelve null si el punto cae fuera del tablero.
     */
    private Posicion obtenerPosicionDesdeCoordenadas(int pixelX, int pixelY) {
        int tamanoCelda = calcularTamanoCelda();
        if (tamanoCelda <= 0) return null;

        int x = pixelX - calcularInicioX(tamanoCelda);
        int y = pixelY - calcularInicioY(tamanoCelda);

        // Los negativos se descartan antes de dividir: la división entera redondea hacia el cero,
        // así que -20 / 40 da 0 y un clic a la izquierda del tablero caería en la columna 0.
        if (x < 0 || y < 0) return null;

        int col = x / tamanoCelda;
        int fila = y / tamanoCelda;

        if (fila < FILAS && col < COLUMNAS) {
            return new Posicion(fila, col);
        }
        return null;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        Posicion posClicked = obtenerPosicionDesdeCoordenadas(e.getX(), e.getY());
        if (posClicked != null) {
            // Aquí se conectará con la partida cuando el controlador esté integrado
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {
        casillaHover = null;
        repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {}

    @Override
    public void mouseMoved(MouseEvent e) {
        Posicion nuevaPos = obtenerPosicionDesdeCoordenadas(e.getX(), e.getY());
        if (casillaHover != nuevaPos && (casillaHover == null || !casillaHover.equals(nuevaPos))) {
            casillaHover = nuevaPos;
            repaint();
        }
    }
}