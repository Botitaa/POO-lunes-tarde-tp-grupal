package batallanaval.vista;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import batallanaval.controlador.PartidaConsulta;

/**
 * Pantalla que se muestra al volver del blackout con lo que el rival le hizo al jugador
 * desde su último turno (disparos recibidos y barcos hundidos), y un botón Continuar.
 * <p>
 * Solo tiene sentido mostrarla si hay algo para contar. Por eso {@link #cargarMensajes()}
 * avisa si había mensajes: quien cambia de pantalla la muestra únicamente cuando devuelve
 * {@code true} y, si devuelve {@code false}, sigue directo a la siguiente:
 *
 * <pre>
 * if (panelResumen.cargarMensajes()) {
 *     ventana.mostrar(Pantalla.RESUMEN);
 * } else {
 *     ventana.mostrar(Pantalla.JUEGO);
 * }
 * </pre>
 * <p>
 * Del juego solo conoce la interfaz {@link PartidaConsulta}, de la que lee
 * {@code getMensajesResumen()} (principio de segregación de interfaces: usa únicamente lo que
 * necesita). Lo que pasa al apretar Continuar lo decide quien armó el panel, a través de un
 * {@link Runnable}. Todos los métodos se llaman desde el EDT.
 */
public final class PanelResumen extends JPanel {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient PartidaConsulta partida;
    private final DefaultListModel<String> modelo = new DefaultListModel<>();

    /**
     * @param partida     de donde se leen los mensajes cada vez que se llama a {@link #cargarMensajes()}
     * @param alContinuar qué se hace al apretar el botón Continuar
     * @throws NullPointerException si algún argumento es {@code null}
     */
    public PanelResumen(PartidaConsulta partida, Runnable alContinuar) {
        this.partida = Objects.requireNonNull(partida, "partida");
        Objects.requireNonNull(alContinuar, "alContinuar");

        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(30, 60, 30, 60));

        JLabel titulo = new JLabel("Resumen del turno rival", SwingConstants.CENTER);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 28f));
        add(titulo, BorderLayout.NORTH);

        JList<String> lista = new JList<>(modelo);
        lista.setFont(lista.getFont().deriveFont(16f));
        add(new JScrollPane(lista), BorderLayout.CENTER);

        JButton botonContinuar = new JButton("Continuar");
        botonContinuar.addActionListener(e -> alContinuar.run());
        JPanel pie = new JPanel(new FlowLayout(FlowLayout.CENTER));
        pie.add(botonContinuar);
        add(pie, BorderLayout.SOUTH);
    }

    /**
     * Lee los mensajes de la partida y reemplaza los que mostraba el panel.
     *
     * @return {@code true} si hay mensajes para mostrar; {@code false} si no hay ninguno
     *         (en ese caso conviene saltear esta pantalla)
     */
    public boolean cargarMensajes() {
        List<String> mensajes = partida.getMensajesResumen();
        modelo.clear();
        if (mensajes == null || mensajes.isEmpty()) {
            return false;
        }
        for (String mensaje : mensajes) {
            modelo.addElement(mensaje);
        }
        return true;
    }

    /**
     * Mensajes que está mostrando el panel, en orden. Solo para los tests.
     */
    List<String> getMensajesMostrados() {
        List<String> mostrados = new ArrayList<>();
        for (int i = 0; i < modelo.size(); i++) {
            mostrados.add(modelo.get(i));
        }
        return mostrados;
    }
}
