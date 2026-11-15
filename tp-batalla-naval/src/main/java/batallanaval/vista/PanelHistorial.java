package batallanaval.vista;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.io.Serial;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;

import batallanaval.controlador.FasePartida;
import batallanaval.controlador.ObservadorPartida;
import batallanaval.controlador.PartidaConsulta;
import batallanaval.controlador.ResultadoPartida;
import batallanaval.historial.EventoPartida;

/**
 * Historial de la partida que ve el jugador activo: un {@link JList} con los eventos en orden,
 * el más viejo arriba y el <b>último abajo</b>; la lista baja sola hasta el último evento.
 * <p>
 * <b>No filtra nada:</b> muestra exactamente lo que devuelve
 * {@link PartidaConsulta#getHistorialVisible()}, que ya viene filtrado por la niebla de guerra
 * (los movimientos y habilidades del rival no aparecen). Así la regla de qué se ve vive en un
 * solo lugar ({@code GestorPartida}) y la vista no la repite.
 * <p>
 * Cada vez que la partida avisa un cambio, la lista se <b>vuelve a armar entera</b> en lugar
 * de agregar solo los eventos nuevos: en hot-seat el jugador activo cambia en cada turno, y
 * con él cambia qué eventos puede ver. Agregar al final mostraría al jugador 2 lo que solo
 * vio el jugador 1.
 * <p>
 * Es <b>plegable</b>: el botón de la cabecera esconde la lista para que el tablero tenga más
 * lugar, y la vuelve a mostrar. Plegado o no, la lista se sigue actualizando.
 * <p>
 * Implementa {@link ObservadorPartida}: en la pantalla de juego lo registra {@link PanelJuego},
 * que le reenvía los avisos. Todo cambio de componentes se hace en el EDT.
 */
public final class PanelHistorial extends JPanel implements ObservadorPartida {

    @Serial
    private static final long serialVersionUID = 1L;

    static final String TEXTO_PLEGAR = "Ocultar";
    static final String TEXTO_DESPLEGAR = "Mostrar";
    static final String SIN_EVENTOS = "Todavía no pasó nada en esta partida.";

    private static final int FILAS_VISIBLES = 5;
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final transient PartidaConsulta partida;

    // El modelo guarda los eventos (no los textos): el renderer decide cómo se ven.
    private final DefaultListModel<EventoPartida> modelo = new DefaultListModel<>();
    private final JList<EventoPartida> lista = new JList<>(modelo);
    private final JScrollPane desplazamiento = new JScrollPane(lista,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    private final JLabel etiquetaTitulo = new JLabel();
    private final JButton botonPlegar = new JButton(TEXTO_PLEGAR);

    /**
     * Crea el panel y muestra el historial que la partida tiene en este momento.
     * No se registra como observador: lo hace quien lo crea (o {@link PanelJuego}, que le
     * reenvía los avisos).
     *
     * @param partida partida de la que se lee el historial; alcanza con la consulta (ISP),
     *                porque el panel no le pide ninguna acción
     * @throws NullPointerException si la partida es {@code null}
     */
    public PanelHistorial(PartidaConsulta partida) {
        this.partida = Objects.requireNonNull(partida, "partida");

        lista.setName("listaHistorial");
        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.setVisibleRowCount(FILAS_VISIBLES);
        lista.setCellRenderer(new RenderizadorEvento());
        // Registra la lista para que se vea el tooltip de cada fila (la hora del evento).
        ToolTipManager.sharedInstance().registerComponent(lista);

        etiquetaTitulo.setName("etiquetaTituloHistorial");
        botonPlegar.setName("botonPlegarHistorial");
        botonPlegar.addActionListener(e -> setPlegado(!estaPlegado()));

        JPanel cabecera = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        cabecera.add(etiquetaTitulo);
        cabecera.add(botonPlegar);

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        add(cabecera, BorderLayout.NORTH);
        add(desplazamiento, BorderLayout.CENTER);

        actualizar();
    }

    /**
     * Vuelve a leer el historial de la partida y rearma la lista, con el último evento abajo
     * y visible. Se tiene que llamar desde el EDT.
     */
    public void actualizar() {
        List<EventoPartida> eventos = partida.getHistorialVisible();

        modelo.clear();
        for (EventoPartida evento : eventos) {
            modelo.addElement(evento);
        }
        etiquetaTitulo.setText(eventos.isEmpty()
                ? "Historial: " + SIN_EVENTOS
                : "Historial (" + eventos.size() + ")");

        if (!eventos.isEmpty()) {
            lista.ensureIndexIsVisible(eventos.size() - 1); // baja hasta el último evento
        }
    }

    /**
     * Pliega (esconde la lista) o despliega el historial. La cabecera con el título y el botón
     * queda siempre visible, así se puede volver a abrir.
     *
     * @param plegado {@code true} para esconder la lista
     */
    public void setPlegado(boolean plegado) {
        desplazamiento.setVisible(!plegado);
        botonPlegar.setText(plegado ? TEXTO_DESPLEGAR : TEXTO_PLEGAR);
        revalidate(); // el contenedor tiene que volver a repartir el espacio
        repaint();
    }

    /**
     * Dice si el historial está plegado.
     *
     * @return {@code true} si la lista está escondida
     */
    public boolean estaPlegado() {
        return !desplazamiento.isVisible();
    }

    /**
     * Cantidad de eventos que se están mostrando.
     *
     * @return cantidad de filas de la lista
     */
    public int getCantidadEventos() {
        return modelo.getSize();
    }

    // Avisos de la partida (ObservadorPartida). Pueden llegar desde otro hilo: se pasa al EDT.

    /** Se registró una acción (movimiento, disparo, etc.): se rearma la lista. */
    @Override
    public void onEstadoCambiado() {
        actualizarEnEdt();
    }

    /**
     * Cambió la fase (por ejemplo, terminó el despliegue): se rearma la lista.
     *
     * @param fasePartida fase nueva
     */
    @Override
    public void onFaseCambiada(FasePartida fasePartida) {
        actualizarEnEdt();
    }

    /**
     * Empezó el turno de otro jugador: se rearma la lista con lo que puede ver él.
     *
     * @param nombreJugador jugador que empieza el turno
     */
    @Override
    public void onTurnoIniciado(String nombreJugador) {
        actualizarEnEdt();
    }

    /**
     * Terminó la partida: se rearma la lista para mostrar el evento de fin.
     *
     * @param resultadoPartida cómo terminó
     */
    @Override
    public void onPartidaTerminada(ResultadoPartida resultadoPartida) {
        actualizarEnEdt();
    }

    private void actualizarEnEdt() {
        SwingUtilities.invokeLater(this::actualizar);
    }

    /**
     * Cómo se ve cada fila: el texto de {@link EventoPartida#aLinea()} (el mismo formato que el
     * historial exportado) y, al pasar el mouse, la hora del evento. Extiende
     * {@link DefaultListCellRenderer} y sobreescribe un solo método.
     */
    private static final class RenderizadorEvento extends DefaultListCellRenderer {

        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object valor, int indice,
                                                      boolean seleccionado, boolean conFoco) {
            // super arma la etiqueta con los colores de selección; acá solo se cambia el texto.
            super.getListCellRendererComponent(list, valor, indice, seleccionado, conFoco);
            if (valor instanceof EventoPartida evento) {
                setText(evento.aLinea());
                setToolTipText(evento.momento().format(FORMATO_HORA));
            }
            return this;
        }
    }
}
