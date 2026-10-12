package batallanaval.vista;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Panel que extiende de JPanel para representar la grilla de 10x10 del tablero de Batalla Naval,
 * dibujando las celdas en paintComponent y escalando dinámicamente con el tamaño de la ventana.
 */
public class PanelTablero extends JPanel {

    private static final int FILAS = 10;
    private static final int COLUMNAS = 10;

    /**
     * Constructor por defecto del panel del tablero.
     */
    public PanelTablero() {
        setBackground(Color.DARK_GRAY);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
        // Activamos suavizado para mejorar la calidad gráfica al escalar
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int anchoPanel = getWidth();
        int altoPanel = getHeight();

        // Calculamos el tamaño de cada celda dinámicamente para que escale con la ventana
        int tamanoCelda = Math.min(anchoPanel / COLUMNAS, altoPanel / FILAS);

        // Centramos la grilla en el panel si sobra espacio
        int inicioX = (anchoPanel - (tamanoCelda * COLUMNAS)) / 2;
        int inicioY = (altoPanel - (tamanoCelda * FILAS)) / 2;

        for (int fila = 0; fila < FILAS; fila++) {
            for (int col = 0; col < COLUMNAS; col++) {
                int x = inicioX + (col * tamanoCelda);
                int y = inicioY + (fila * tamanoCelda);

                // Dibujamos el fondo base de cada celda (agua preliminar)
                g2d.setColor(new Color(51, 153, 255));
                g2d.fillRect(x, y, tamanoCelda, tamanoCelda);

                // Dibujamos el borde negro de la celda
                g2d.setColor(Color.BLACK);
                g2d.drawRect(x, y, tamanoCelda, tamanoCelda);
            }
        }
    }
}