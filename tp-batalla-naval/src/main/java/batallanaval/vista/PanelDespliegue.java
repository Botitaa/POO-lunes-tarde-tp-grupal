package batallanaval.vista;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.Serial;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import batallanaval.controlador.EstadoBarco;
import batallanaval.controlador.FasePartida;
import batallanaval.controlador.ObservadorPartida;
import batallanaval.controlador.Partida;
import batallanaval.controlador.ResultadoPartida;
import batallanaval.modelo.Posicion;

/**
 * Pantalla de <b>despliegue</b> ({@link Pantalla#DESPLIEGUE}): el {@link PanelTablero} en modo
 * despliegue (sin niebla y con los clics conectados) a la izquierda, y a la derecha la lista de
 * barcos sin ubicar, el estado de la selección y los botones <b>Retirar barco</b> y
 * <b>Confirmar despliegue</b>. Cada jugador despliega por turno (hot-seat): el panel siempre muestra
 * al jugador activo de la partida.
 * <p>
 * Cómo se usa:
 * <ul>
 *   <li>Elegir un barco de la lista (o hacer clic sobre un barco ya ubicado) lo deja
 *       <i>seleccionado</i>.</li>
 *   <li>Un clic en una casilla del tablero ubica ahí el barco seleccionado. Si el barco ya estaba
 *       ubicado, se mueve. Después del clic queda seleccionado el siguiente barco sin ubicar.</li>
 *   <li><b>Retirar barco</b> devuelve a la lista el barco seleccionado (solo si ya estaba ubicado).</li>
 *   <li><b>Confirmar despliegue</b> se habilita únicamente cuando no quedan barcos sin ubicar.</li>
 * </ul>
 * <b>El panel no valida nada:</b> si la casilla está fuera de la zona, ocupada o no se puede
 * pisar, la partida lanza {@code AccionInvalidaException} y su mensaje se muestra con {@code alError}
 * (en la aplicación, la barra de estado de la ventana). Cuando se confirma, la partida cambia de fase
 * y avisa; quien armó la pantalla ({@code ControladorAplicacion}) decide cuál se muestra después.
 * <p>
 * Implementa {@link ObservadorPartida}: quien lo crea lo registra en la partida con
 * {@code agregarObservador}, y desde ahí el panel se refresca y reenvía los avisos al tablero.
 * Quien lo muestra tiene que llamar a {@link #actualizar()} para que lea al jugador activo.
 */
public final class PanelDespliegue extends JPanel implements ObservadorPartida {

    @Serial
    private static final long serialVersionUID = 1L;

    static final String TEXTO_RETIRAR = "Retirar barco";
    static final String TEXTO_CONFIRMAR = "Confirmar despliegue";
    static final String MENSAJE_ELEGIR_BARCO = "Elegí un barco de la lista para ubicarlo.";
    static final String MENSAJE_ELEGIR_UBICADO = "Hacé clic en un barco ya ubicado para retirarlo.";

    private static final String ZONA_ARRIBA = "las 3 filas de arriba";
    private static final String ZONA_ABAJO = "las 3 filas de abajo";

    private static final int SIN_SELECCION = -1;
    private static final int ANCHO_LATERAL = 300;

    private final transient Partida partida;
    private final transient Consumer<String> alError;
    private final PanelTablero panelTablero;

    private final DefaultListModel<EstadoBarco> modeloSinUbicar = new DefaultListModel<>();
    private final JList<EstadoBarco> listaSinUbicar = new JList<>(modeloSinUbicar);
    private final JLabel titulo = new JLabel(" ");
    private final JLabel instrucciones = new JLabel(" ");
    private final JLabel etiquetaProgreso = new JLabel(" ");
    private final JLabel etiquetaSeleccion = new JLabel(" ");
    private final JButton botonRetirar = new JButton(TEXTO_RETIRAR);
    private final JButton botonConfirmar = new JButton(TEXTO_CONFIRMAR);

    private int idSeleccionado = SIN_SELECCION;
    private boolean actualizando = false;

    /**
     * Crea la pantalla y la deja lista para el jugador activo de la partida.
     *
     * @param partida partida en la que se despliega (se usa para el tablero y para las acciones)
     * @param alError qué hacer con el mensaje cuando la partida rechaza una acción
     *                (en la aplicación, {@code ventana::mostrarError})
     * @throws NullPointerException si algún argumento es {@code null}
     */
    public PanelDespliegue(Partida partida, Consumer<String> alError) {
        this.partida = Objects.requireNonNull(partida, "partida");
        this.alError = Objects.requireNonNull(alError, "alError");

        panelTablero = new PanelTablero(partida, partida);
        panelTablero.setModoDespliegue(true);
        panelTablero.setAlClickearCasilla(
                posicion -> EjecutorAcciones.ejecutar(() -> alClickearCasilla(posicion), alError));

        setLayout(new BorderLayout(10, 0));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(panelTablero, BorderLayout.CENTER);
        add(crearPanelLateral(), BorderLayout.EAST);

        conectarEventos();
        actualizar();
    }

    /**
     * Vuelve a leer la partida: pone el nombre del jugador activo, rearma la lista de barcos sin
     * ubicar, corrige la selección y habilita o no los botones. Quien muestra la pantalla la llama
     * cada vez que cambia el jugador que despliega. Llamar desde el EDT.
     */
    public void actualizar() {
        actualizando = true;
        try {
            List<EstadoBarco> propios = partida.getBarcosPropios();
            List<EstadoBarco> sinUbicar = partida.getBarcosSinUbicar();

            modeloSinUbicar.clear();
            sinUbicar.forEach(modeloSinUbicar::addElement);

            if (buscarPropioPorId(propios, idSeleccionado) == null) {
                idSeleccionado = sinUbicar.isEmpty() ? SIN_SELECCION : sinUbicar.get(0).id();
            }
            EstadoBarco seleccionado = buscarPropioPorId(propios, idSeleccionado);
            boolean seleccionadoSinUbicar = seleccionado != null && seleccionado.posicion() == null;

            if (seleccionadoSinUbicar) {
                listaSinUbicar.setSelectedValue(seleccionado, true);
            } else {
                listaSinUbicar.clearSelection();
            }

            titulo.setText("Despliegue de " + partida.getJugadorActivo().nombre());
            instrucciones.setText(textoInstrucciones(partida.getJugadorActivo().numero()));
            etiquetaProgreso.setText("Ubicados: " + (propios.size() - sinUbicar.size()) + " de " + propios.size());
            etiquetaSeleccion.setText(describirSeleccion(seleccionado));
            botonRetirar.setEnabled(seleccionado != null && !seleccionadoSinUbicar);
            botonConfirmar.setEnabled(sinUbicar.isEmpty());
        } finally {
            actualizando = false;
        }
        panelTablero.repaint();
    }

    /** El tablero del despliegue. Es de paquete para que los tests puedan hacer clic sobre él. */
    PanelTablero getPanelTablero() {
        return panelTablero;
    }

    // Acciones

    /**
     * Qué pasa con un clic en una casilla: si hay un barco propio, se lo selecciona; si no, se ubica
     * ahí el barco seleccionado. Es de paquete porque los tests lo llaman sin calcular píxeles.
     */
    void alClickearCasilla(Posicion posicion) {
        EstadoBarco propio = buscarPropioEn(posicion);
        if (propio != null) {
            idSeleccionado = propio.id();
            actualizar();
            return;
        }
        if (idSeleccionado == SIN_SELECCION) {
            alError.accept(MENSAJE_ELEGIR_BARCO);
            return;
        }
        partida.ubicarBarco(idSeleccionado, posicion);
        // Listo ese barco, se pasa al siguiente sin ubicar (si no quedan, no hay selección).
        List<EstadoBarco> sinUbicar = partida.getBarcosSinUbicar();
        idSeleccionado = sinUbicar.isEmpty() ? SIN_SELECCION : sinUbicar.get(0).id();
        actualizar();
    }

    private void retirarSeleccionado() {
        EstadoBarco seleccionado = buscarPropioPorId(partida.getBarcosPropios(), idSeleccionado);
        if (seleccionado == null || seleccionado.posicion() == null) {
            alError.accept(MENSAJE_ELEGIR_UBICADO);
            return;
        }
        partida.retirarBarco(seleccionado.id());
        actualizar();
    }

    private void confirmar() {
        partida.confirmarDespliegue();
    }

    private void alElegirEnLista() {
        if (actualizando) {
            return;
        }
        EstadoBarco elegido = listaSinUbicar.getSelectedValue();
        if (elegido != null) {
            idSeleccionado = elegido.id();
            actualizar();
        }
    }

    // Consultas sobre la lista de barcos propios

    private EstadoBarco buscarPropioEn(Posicion posicion) {
        for (EstadoBarco barco : partida.getBarcosPropios()) {
            if (posicion.equals(barco.posicion())) {
                return barco;
            }
        }
        return null;
    }

    private static EstadoBarco buscarPropioPorId(List<EstadoBarco> propios, int id) {
        for (EstadoBarco barco : propios) {
            if (barco.id() == id) {
                return barco;
            }
        }
        return null;
    }

    /** Pasos para el jugador, con la zona que le toca (el J1 despliega arriba y el J2 abajo). */
    private static String textoInstrucciones(int numeroJugador) {
        String zona = numeroJugador == 1 ? ZONA_ARRIBA : ZONA_ABAJO;
        // Los saltos de línea están a mano: el ajuste automático de un JLabel con HTML recorta el texto.
        return "<html>1. Elegí un barco de la lista.<br>"
                + "2. Hacé clic en una casilla de tu zona<br>"
                + "&nbsp;&nbsp;&nbsp;&nbsp;(" + zona + ").<br>"
                + "3. Para cambiar uno ya ubicado, hacé<br>"
                + "&nbsp;&nbsp;&nbsp;&nbsp;clic sobre él y después en otra<br>"
                + "&nbsp;&nbsp;&nbsp;&nbsp;casilla, o retiralo.<br>"
                + "Con todos en su lugar, confirmá.</html>";
    }

    private static String describirSeleccion(EstadoBarco barco) {
        if (barco == null) {
            return "Ningún barco elegido";
        }
        String nombre = PanelTablero.nombreTipo(barco.simbolo());
        if (barco.posicion() == null) {
            return "Elegido: " + nombre + " (sin ubicar)";
        }
        return "Elegido: " + nombre + " en fila " + barco.posicion().getFila()
                + ", columna " + barco.posicion().getColumna();
    }

    private static String describirBarco(EstadoBarco barco) {
        return PanelTablero.nombreTipo(barco.simbolo()) + " (" + barco.simbolo() + ")  -  vida "
                + barco.vidaMaxima() + ", rango " + barco.rangoDisparo() + ", visión " + barco.vision();
    }

    // Armado de la interfaz (una sola vez, en el constructor)

    private JPanel crearPanelLateral() {
        JPanel lateral = new JPanel(new BorderLayout(0, 10));
        lateral.setPreferredSize(new Dimension(ANCHO_LATERAL, 0));

        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 20f));
        instrucciones.setFont(instrucciones.getFont().deriveFont(Font.PLAIN, 13f));
        JPanel arriba = new JPanel(new BorderLayout(0, 8));
        arriba.add(titulo, BorderLayout.NORTH);
        arriba.add(instrucciones, BorderLayout.CENTER);
        lateral.add(arriba, BorderLayout.NORTH);

        listaSinUbicar.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaSinUbicar.setCellRenderer(new RenderizadorBarcos());
        JScrollPane scroll = new JScrollPane(listaSinUbicar);
        scroll.setBorder(BorderFactory.createTitledBorder("Barcos sin ubicar"));
        lateral.add(scroll, BorderLayout.CENTER);

        etiquetaProgreso.setFont(etiquetaProgreso.getFont().deriveFont(Font.BOLD));
        etiquetaSeleccion.setForeground(new Color(60, 60, 60));
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        botones.add(botonRetirar);
        botones.add(botonConfirmar);
        JPanel abajo = new JPanel(new GridLayout(0, 1, 0, 6));
        abajo.add(etiquetaProgreso);
        abajo.add(etiquetaSeleccion);
        abajo.add(botones);
        lateral.add(abajo, BorderLayout.SOUTH);

        listaSinUbicar.setName("listaSinUbicar");
        titulo.setName("tituloDespliegue");
        instrucciones.setName("instrucciones");
        etiquetaProgreso.setName("etiquetaProgreso");
        etiquetaSeleccion.setName("etiquetaSeleccion");
        botonRetirar.setName("botonRetirar");
        botonConfirmar.setName("botonConfirmar");
        return lateral;
    }

    private void conectarEventos() {
        listaSinUbicar.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                alElegirEnLista();
            }
        });
        botonRetirar.addActionListener(EjecutorAcciones.alTocar(this::retirarSeleccionado, alError));
        botonConfirmar.addActionListener(EjecutorAcciones.alTocar(this::confirmar, alError));
    }

    /** Muestra cada barco de la lista con su nombre y sus estadísticas, no con el texto del record. */
    private static final class RenderizadorBarcos extends DefaultListCellRenderer {

        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public Component getListCellRendererComponent(JList<?> lista, Object valor, int indice,
                                                      boolean seleccionado, boolean conFoco) {
            super.getListCellRendererComponent(lista, valor, indice, seleccionado, conFoco);
            if (valor instanceof EstadoBarco barco) {
                setText(describirBarco(barco));
            }
            return this;
        }
    }

    // ObservadorPartida

    /** Cambió algo (se ubicó o retiró un barco): se refresca la pantalla en el EDT. */
    @Override
    public void onEstadoCambiado() {
        panelTablero.onEstadoCambiado();
        SwingUtilities.invokeLater(this::actualizar);
    }

    /** Cambió la fase (por ejemplo, de despliegue a juego): el tablero se redibuja. */
    @Override
    public void onFaseCambiada(FasePartida fase) {
        panelTablero.onFaseCambiada(fase);
    }

    /** Empezó el turno de otro jugador: el tablero se redibuja. */
    @Override
    public void onTurnoIniciado(String nombreJugador) {
        panelTablero.onTurnoIniciado(nombreJugador);
    }

    /** Terminó la partida: el tablero se redibuja. */
    @Override
    public void onPartidaTerminada(ResultadoPartida resultado) {
        panelTablero.onPartidaTerminada(resultado);
    }
}
