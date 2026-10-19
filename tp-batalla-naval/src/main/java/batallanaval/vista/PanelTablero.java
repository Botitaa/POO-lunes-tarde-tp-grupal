package batallanaval.vista;

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

/**
 * Panel que extiende de JPanel para representar la grilla de 10x10 del tablero de Batalla Naval.
 * Implementa la visualización de cada tipo de casilla y la zona de dominación.
 */
public class PanelTablero extends JPanel implements MouseListener, MouseMotionListener {

    private static final int FILAS = 10;
    private static final int COLUMNAS = 10;

    // Guarda la posición de la celda sobre la cual está el mouse actualmente
    private Posicion casillaHover = null;

    /**
     * Constructor por defecto del panel del tablero.
     */
    public PanelTablero() {
        setBackground(Color.DARK_GRAY);
        
        // Registramos los listeners para las acciones del mouse
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int anchoPanel = getWidth();
        int altoPanel = getHeight();

        // Cálculo dinámico del tamaño de celda para adaptarse al panel
        int tamanoCelda = Math.min(anchoPanel / COLUMNAS, altoPanel / FILAS);
        int inicioX = (anchoPanel - (tamanoCelda * COLUMNAS)) / 2;
        int inicioY = (altoPanel - (tamanoCelda * FILAS)) / 2;

        // Recorremos la matriz lógica de 10x10
        for (int fila = 0; fila < FILAS; fila++) {
            for (int col = 0; col < COLUMNAS; col++) {
                Posicion pos = new Posicion(fila, col);
                int x = inicioX + (col * tamanoCelda);
                int y = inicioY + (fila * tamanoCelda);

                // Nota: Aquí se conectará con el método de consulta cuando el controlador esté integrado.
                // Por ahora definimos una casilla por defecto (AGUA) para maquetación y pruebas visuales.
                Casilla casilla = Casilla.AGUA; 

                // Asignamos el color de fondo según el tipo de casilla (Paleta oficial)
                if (casilla != null) {
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
                } else {
                    g2d.setColor(Color.DARK_GRAY);
                }
                g2d.fillRect(x, y, tamanoCelda, tamanoCelda);

                // Si la casilla contiene restos de un barco hundido, dibujamos una "X" negra[cite: 6]
                if (casilla == Casilla.RESTOS) {
                    g2d.setColor(Color.BLACK);
                    g2d.setStroke(new BasicStroke(2));
                    g2d.drawLine(x + 4, y + 4, x + tamanoCelda - 4, y + tamanoCelda - 4);
                    g2d.drawLine(x + tamanoCelda - 4, y + 4, x + 4, y + tamanoCelda - 4);
                }

                // Dibujamos el borde estándar de la celda
                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(1));
                g2d.drawRect(x, y, tamanoCelda, tamanoCelda);

                // Marcamos la Zona de Dominación (las 4 casillas centrales: filas 4-5, columnas 4-5) con borde dorado[cite: 6]
                if ((fila == 4 || fila == 5) && (col == 4 || col == 5)) {
                    g2d.setColor(new Color(218, 165, 32)); // Dorado
                    g2d.setStroke(new BasicStroke(3));
                    g2d.drawRect(x, y, tamanoCelda, tamanoCelda);
                }

                // Efecto Hover (resaltado blanco semitransparente)
                if (casillaHover != null && casillaHover.equals(pos)) {
                    g2d.setColor(new Color(255, 255, 255, 80));
                    g2d.fillRect(x, y, tamanoCelda, tamanoCelda);
                    g2d.setColor(Color.WHITE);
                    g2d.setStroke(new BasicStroke(2));
                    g2d.drawRect(x, y, tamanoCelda, tamanoCelda);
                }
            }
        }
    }

    private Posicion obtenerPosicionDesdeCoordenadas(int pixelX, int pixelY) {
        int anchoPanel = getWidth();
        int altoPanel = getHeight();
        
        int tamanoCelda = Math.min(anchoPanel / COLUMNAS, altoPanel / FILAS);
        if (tamanoCelda <= 0) return null;

        int inicioX = (anchoPanel - (tamanoCelda * COLUMNAS)) / 2;
        int inicioY = (altoPanel - (tamanoCelda * FILAS)) / 2;

        int col = (pixelX - inicioX) / tamanoCelda;
        int fila = (pixelY - inicioY) / tamanoCelda;

        if (fila >= 0 && fila < FILAS && col >= 0 && col < COLUMNAS) {
            return new Posicion(fila, col);
        }
        return null;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        Posicion posClicked = obtenerPosicionDesdeCoordenadas(e.getX(), e.getY());
        if (posClicked != null) {
            System.out.println("Clic en posición lógica: " + posClicked);
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