package batallanaval.vista;

import batallanaval.modelo.Posicion;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 * Panel que extiende de JPanel para representar la grilla de 10x10 del tablero de Batalla Naval.
 * Implementa MouseListener y MouseMotionListener para traducir clics a Posicion 
 * y resaltar la casilla bajo el cursor.
 */
public class PanelTablero extends JPanel implements MouseListener, MouseMotionListener {

    private static final int FILAS = 10;
    private static final int COLUMNAS = 10;

    // Guarda la posición de la celda sobre la cual está el mouse actualmente (efecto hover)
    private Posicion casillaHover = null;

    /**
     * Constructor por defecto del panel del tablero.
     * Configura el fondo y registra los listeners del mouse en este componente.
     */
    public PanelTablero() {
        setBackground(Color.DARK_GRAY);
        
        // Registramos los listeners para escuchar las acciones del mouse
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // Limpia y dibuja el fondo estándar

        Graphics2D g2d = (Graphics2D) g;
        // Activamos suavizado (antialiasing) para mejorar la calidad gráfica al escalar
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int anchoPanel = getWidth();
        int altoPanel = getHeight();

        // Calculamos dinámicamente el tamaño de cada celda para que escale con la ventana
        int tamanoCelda = Math.min(anchoPanel / COLUMNAS, altoPanel / FILAS);

        // Centramos la grilla en el panel calculando los márgenes si sobra espacio
        int inicioX = (anchoPanel - (tamanoCelda * COLUMNAS)) / 2;
        int inicioY = (altoPanel - (tamanoCelda * FILAS)) / 2;

        // Recorremos la matriz lógica de 10x10 para dibujar cada celda
        for (int fila = 0; fila < FILAS; fila++) {
            for (int col = 0; col < COLUMNAS; col++) {
                Posicion pos = new Posicion(fila, col);
                int x = inicioX + (col * tamanoCelda);
                int y = inicioY + (fila * tamanoCelda);

                // Dibujamos el relleno base de la celda (color agua preliminar)
                g2d.setColor(new Color(51, 153, 255));
                g2d.fillRect(x, y, tamanoCelda, tamanoCelda);

                // Dibujamos el borde negro alrededor de cada celda
                g2d.setColor(Color.BLACK);
                g2d.drawRect(x, y, tamanoCelda, tamanoCelda);

                // Si el mouse está posicionado encima de esta celda, aplicamos el resaltado (Hover)
                if (casillaHover != null && casillaHover.equals(pos)) {
                    g2d.setColor(new Color(255, 255, 255, 100)); // Blanco semitransparente
                    g2d.fillRect(x, y, tamanoCelda, tamanoCelda);
                    g2d.setColor(Color.WHITE);
                    g2d.setStroke(new BasicStroke(2));
                    g2d.drawRect(x, y, tamanoCelda, tamanoCelda);
                    g2d.setStroke(new BasicStroke(1)); // Restauramos el grosor por defecto del trazo
                }
            }
        }
    }

    /**
     * Traduce las coordenadas de píxeles (pixelX, pixelY) de la pantalla a una 
     * instancia inmutable de Posicion (fila, columna de 0 a 9).
     */
    private Posicion obtenerPosicionDesdeCoordenadas(int pixelX, int pixelY) {
        int anchoPanel = getWidth();
        int altoPanel = getHeight();
        
        int tamanoCelda = Math.min(anchoPanel / COLUMNAS, altoPanel / FILAS);
        if (tamanoCelda <= 0) return null;

        int inicioX = (anchoPanel - (tamanoCelda * COLUMNAS)) / 2;
        int inicioY = (altoPanel - (tamanoCelda * FILAS)) / 2;

        // Restamos el margen de centrado y dividimos por el tamaño de la celda
        int col = (pixelX - inicioX) / tamanoCelda;
        int fila = (pixelY - inicioY) / tamanoCelda;

        // Validamos que la posición esté dentro de los límites válidos de la grilla (0 a 9)
        if (fila >= 0 && fila < FILAS && col >= 0 && col < COLUMNAS) {
            return new Posicion(fila, col);
        }
        return null;
    }

    // Implementación de MouseListener (Issue #86)

    @Override
    public void mouseClicked(MouseEvent e) {
        // Al hacer clic, convertimos la coordenada en una Posicion del modelo
        Posicion posClicked = obtenerPosicionDesdeCoordenadas(e.getX(), e.getY());
        if (posClicked != null) {
            System.out.println("Clic detectado en la posición lógica: " + posClicked);
            // Aquí se conectará luego con el controlador de la partida
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
        // Cuando el cursor sale del panel, limpiamos el hover para que no quede resaltado
        casillaHover = null;
        repaint();
    }

    // Implementación de MouseMotionListener (Issue #86)

    @Override
    public void mouseDragged(MouseEvent e) {}

    @Override
    public void mouseMoved(MouseEvent e) {
        // Cada vez que se mueve el mouse, recalculamos sobre qué casilla está el cursor
        Posicion nuevaPos = obtenerPosicionDesdeCoordenadas(e.getX(), e.getY());
        
        // Si cambió de casilla, actualizamos la variable y repintamos para actualizar el gráfico
        if (casillaHover != nuevaPos && (casillaHover == null || !casillaHover.equals(nuevaPos))) {
            casillaHover = nuevaPos;
            repaint(); 
        }
    }
}