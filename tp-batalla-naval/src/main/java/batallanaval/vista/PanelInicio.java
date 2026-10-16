package batallanaval.vista;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Menú principal: Nueva partida, Cargar partida, Estadísticas, Reglas y Salir.
 * <p>
 * Los botones no deciden nada de la partida: solo le piden a la
 * {@link VentanaPrincipal} que cambie de pantalla.
 */
public class PanelInicio extends JPanel {

    private static final Dimension TAMANIO_BOTON = new Dimension(260, 44);

    private final VentanaPrincipal ventana;

    /**
     * @param ventana ventana desde la que se navega a las otras pantallas
     */
    public PanelInicio(VentanaPrincipal ventana) {
        this.ventana = ventana;
        setLayout(new GridBagLayout());
        armarContenido();
    }

    private void armarContenido() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = GridBagConstraints.RELATIVE;
        c.insets = new Insets(6, 0, 6, 0);

        JLabel titulo = new JLabel("Batalla Naval Táctica", SwingConstants.CENTER);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 36f));
        c.insets = new Insets(0, 0, 4, 0);
        add(titulo, c);

        JLabel subtitulo = new JLabel("Dos jugadores, una computadora, mucha niebla", SwingConstants.CENTER);
        subtitulo.setFont(subtitulo.getFont().deriveFont(Font.ITALIC, 14f));
        c.insets = new Insets(0, 0, 30, 0);
        add(subtitulo, c);

        c.insets = new Insets(6, 0, 6, 0);
        add(crearBoton("Nueva partida", () -> ventana.mostrar(Pantalla.NUEVA_PARTIDA)), c);
        add(crearBoton("Cargar partida", () -> ventana.mostrar(Pantalla.CARGAR)), c);
        add(crearBoton("Estadísticas", () -> ventana.mostrar(Pantalla.ESTADISTICAS)), c);
        add(crearBoton("Reglas", () -> ventana.mostrar(Pantalla.REGLAS)), c);
        add(crearBoton("Salir", this::confirmarSalida), c);
    }

    private JButton crearBoton(String texto, Runnable accion) {
        JButton boton = new JButton(texto);
        boton.setPreferredSize(TAMANIO_BOTON);
        boton.addActionListener(e -> accion.run());
        return boton;
    }

    private void confirmarSalida() {
        int opcion = JOptionPane.showConfirmDialog(
                ventana,
                "¿Seguro que querés salir?",
                "Salir",
                JOptionPane.YES_NO_OPTION);
        if (opcion == JOptionPane.YES_OPTION) {
            ventana.dispose();
        }
    }
}