package batallanaval.vista;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import batallanaval.controlador.Partida;
import batallanaval.excepciones.PersistenciaException;
import batallanaval.persistencia.CabeceraGuardado;
import batallanaval.persistencia.RepositorioPartidas;

/**
 * Pantalla para <b>cargar una partida guardada</b> ({@link Pantalla#CARGAR}): una {@link JTable}
 * de solo lectura con las partidas que devuelve {@code RepositorioPartidas.listar()} (nombre,
 * jugadores, ronda y fecha, la más nueva primero) y los botones <b>Cargar</b>, <b>Borrar</b> y
 * <b>Volver</b>. Cargar también se hace con doble clic sobre una fila.
 * <p>
 * El panel no sabe leer archivos ni armar partidas: se las pide al {@link RepositorioPartidas}
 * <b>en un hilo propio</b> ({@link TareaCargar}, Unidad 6), para que la ventana no se congele
 * mientras se lee y se valida el archivo. Mientras carga, los botones quedan bloqueados y se
 * vuelven a habilitar al terminar, salga bien o mal.
 * Cuando la carga sale bien, le entrega la partida (ya en el EDT) a la acción {@code alCargar} y quien armó la
 * pantalla (en la aplicación, {@code ControladorAplicacion}) decide qué hacer: registrar los
 * observadores y mostrar la pantalla que corresponda. Si algo falla con una
 * {@link PersistenciaException} (archivo borrado a mano, corrupto, de otra versión...), el
 * mensaje de la excepción se muestra en la barra de estado de la ventana y en un aviso dentro
 * del panel, y el juego sigue.
 * <p>
 * Quien la arma tiene que llamar a {@link #actualizarLista()} cada vez que la va a mostrar, para
 * que refleje los guardados más recientes (igual que {@link PanelEstadisticas#cargarRanking}).
 */
public final class PanelCargarPartida extends JPanel {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Aviso debajo del título cuando no hay ninguna partida guardada. */
    static final String MENSAJE_SIN_PARTIDAS = "No hay partidas guardadas";

    static final String MENSAJE_ELEGIR_PARTIDA = "Elegí una partida de la lista.";

    /** Aviso debajo del título mientras el hilo de fondo lee el archivo. */
    static final String MENSAJE_CARGANDO = "Cargando partida...";

    private static final String[] COLUMNAS = {"Nombre", "Jugadores", "Ronda", "Fecha"};
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final transient VentanaPrincipal ventana;
    private final transient RepositorioPartidas repositorio;
    private final transient Consumer<Partida> alCargar;
    private final transient Predicate<String> confirmarBorrado;
    private final transient Executor lanzador;
    private final transient Executor enElEdt;

    // Solo se toca desde el EDT: evita lanzar dos cargas a la vez (por ejemplo, con doble clic).
    private boolean cargando = false;

    private final ModeloGuardados modelo = new ModeloGuardados();
    private final JTable tabla = new JTable(modelo);
    private final JLabel aviso = new JLabel(" ", SwingConstants.CENTER);
    private final JButton botonCargar = new JButton("Cargar");
    private final JButton botonBorrar = new JButton("Borrar");
    private final JButton botonVolver = new JButton("Volver");

    /**
     * Crea la pantalla con la tabla vacía; hay que llamar a {@link #actualizarLista()} para
     * llenarla.
     *
     * @param ventana     ventana que muestra los errores en la barra de estado y a la que se vuelve
     *                    (Volver lleva a {@link Pantalla#INICIO})
     * @param repositorio de donde se lista, se carga y se borra
     * @param alCargar    qué hacer con la partida ya cargada
     * @throws NullPointerException si algún argumento es {@code null}
     */
    public PanelCargarPartida(VentanaPrincipal ventana, RepositorioPartidas repositorio,
                              Consumer<Partida> alCargar) {
        this(ventana, repositorio, alCargar, PanelCargarPartida::confirmarBorradoConDialogo,
                tarea -> new Thread(tarea, "hilo-cargar").start(), SwingUtilities::invokeLater);
    }

    /**
     * Igual que el constructor público, pero recibe cómo se confirma el borrado y carga <b>sin
     * hilo</b>: todo corre en el momento y en el hilo que llama. Es de paquete a propósito: lo usan
     * los tests para no abrir un diálogo ni depender de otro hilo.
     *
     * @param confirmarBorrado recibe el nombre de la partida y devuelve {@code true} si se borra
     */
    PanelCargarPartida(VentanaPrincipal ventana, RepositorioPartidas repositorio,
                       Consumer<Partida> alCargar, Predicate<String> confirmarBorrado) {
        this(ventana, repositorio, alCargar, confirmarBorrado, Runnable::run, Runnable::run);
    }

    /**
     * Constructor completo, de paquete: además recibe quién lanza la {@link TareaCargar}
     * ({@code lanzador}) y quién entrega sus resultados al EDT ({@code enElEdt}).
     */
    PanelCargarPartida(VentanaPrincipal ventana, RepositorioPartidas repositorio,
                       Consumer<Partida> alCargar, Predicate<String> confirmarBorrado,
                       Executor lanzador, Executor enElEdt) {
        this.ventana = Objects.requireNonNull(ventana, "ventana");
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio");
        this.alCargar = Objects.requireNonNull(alCargar, "alCargar");
        this.confirmarBorrado = Objects.requireNonNull(confirmarBorrado, "confirmarBorrado");
        this.lanzador = Objects.requireNonNull(lanzador, "lanzador");
        this.enElEdt = Objects.requireNonNull(enElEdt, "enElEdt");
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        armarContenido();
        actualizarBotones();
    }

    /**
     * Vuelve a leer la lista de guardados del repositorio y reemplaza las filas de la tabla. Si no
     * hay ninguno, se muestra un aviso; si el repositorio falla, la tabla queda vacía y se muestra
     * el mensaje del error. Llamar desde el EDT.
     */
    public void actualizarLista() {
        modelo.setRowCount(0);
        try {
            List<CabeceraGuardado> guardados = repositorio.listar();
            for (CabeceraGuardado guardado : guardados) {
                modelo.addRow(new Object[] {
                    guardado.nombre(),
                    guardado.jugador1() + " vs " + guardado.jugador2(),
                    guardado.ronda(),
                    guardado.fecha().format(FORMATO_FECHA)
                });
            }
            aviso.setText(guardados.isEmpty() ? MENSAJE_SIN_PARTIDAS : " ");
        } catch (PersistenciaException e) {
            aviso.setText(e.getMessage());
            ventana.mostrarError(e.getMessage());
        }
        actualizarBotones();
    }

    // Acciones

    /**
     * Carga la partida elegida en un hilo propio ({@link TareaCargar}) y, cuando termina, se la
     * entrega a {@code alCargar} en el EDT; si falla, muestra el error en la barra de estado.
     */
    private void cargarSeleccionada() {
        if (cargando) {
            return; // ya hay una carga en curso (por ejemplo, un doble clic repetido)
        }
        String nombre = nombreSeleccionado();
        if (nombre == null) {
            ventana.mostrarError(MENSAJE_ELEGIR_PARTIDA);
            return;
        }
        ocuparse(true);
        try {
            lanzador.execute(new TareaCargar(repositorio, nombre, alCargar, ventana::mostrarError,
                    () -> ocuparse(false), enElEdt));
        } catch (RuntimeException e) {
            // Si no se pudo ni lanzar el hilo, la tarea nunca va a correr su finally.
            ocuparse(false);
            throw e;
        }
    }

    /** Bloquea o libera los botones mientras el hilo de fondo carga. Siempre en el EDT. */
    private void ocuparse(boolean ocupado) {
        cargando = ocupado;
        if (ocupado) {
            aviso.setText(MENSAJE_CARGANDO);
        } else if (MENSAJE_CARGANDO.equals(aviso.getText())) {
            aviso.setText(" ");
        }
        actualizarBotones();
    }

    /** Borra la partida elegida si el jugador lo confirma, y vuelve a listar. */
    private void borrarSeleccionada() {
        String nombre = nombreSeleccionado();
        if (nombre == null) {
            ventana.mostrarError(MENSAJE_ELEGIR_PARTIDA);
            return;
        }
        if (!confirmarBorrado.test(nombre)) {
            return;
        }
        try {
            repositorio.borrar(nombre);
            actualizarLista();
            ventana.mostrarMensaje("Partida \"" + nombre + "\" borrada.");
        } catch (PersistenciaException e) {
            ventana.mostrarError(e.getMessage());
            actualizarLista(); // puede que el archivo ya no exista: la lista se pone al día
        }
    }

    private String nombreSeleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            return null;
        }
        return (String) modelo.getValueAt(tabla.convertRowIndexToModel(fila), 0);
    }

    /** Cargar y Borrar solo se pueden tocar con una fila elegida, y ningún botón mientras se carga. */
    private void actualizarBotones() {
        boolean hayElegida = tabla.getSelectedRow() >= 0;
        botonCargar.setEnabled(hayElegida && !cargando);
        botonBorrar.setEnabled(hayElegida && !cargando);
        botonVolver.setEnabled(!cargando);
    }

    private static boolean confirmarBorradoConDialogo(String nombre) {
        int opcion = JOptionPane.showConfirmDialog(null,
                "¿Borrar la partida \"" + nombre + "\"? No se puede deshacer.",
                "Borrar partida", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return opcion == JOptionPane.YES_OPTION;
    }

    // Armado de la interfaz (una sola vez, en el constructor)

    private void armarContenido() {
        JLabel titulo = new JLabel("Cargar partida", SwingConstants.CENTER);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 28f));
        aviso.setFont(aviso.getFont().deriveFont(Font.PLAIN, 15f));
        JPanel encabezado = new JPanel(new BorderLayout(0, 4));
        encabezado.add(titulo, BorderLayout.NORTH);
        encabezado.add(aviso, BorderLayout.SOUTH);
        add(encabezado, BorderLayout.NORTH);

        tabla.setFont(tabla.getFont().deriveFont(15f));
        tabla.setRowHeight(28);
        tabla.setFillsViewportHeight(true);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.getSelectionModel().addListSelectionListener(e -> actualizarBotones());
        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tabla.getSelectedRow() >= 0) {
                    EjecutorAcciones.ejecutar(PanelCargarPartida.this::cargarSeleccionada, ventana::mostrarError);
                }
            }
        });
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        botonCargar.addActionListener(EjecutorAcciones.alTocar(this::cargarSeleccionada, ventana::mostrarError));
        botonBorrar.addActionListener(EjecutorAcciones.alTocar(this::borrarSeleccionada, ventana::mostrarError));
        botonVolver.addActionListener(
                EjecutorAcciones.alTocar(() -> ventana.mostrar(Pantalla.INICIO), ventana::mostrarError));
        JPanel pie = new JPanel(new FlowLayout(FlowLayout.CENTER));
        pie.add(botonCargar);
        pie.add(botonBorrar);
        pie.add(botonVolver);
        add(pie, BorderLayout.SOUTH);
    }

    /** Modelo de la tabla: las columnas fijas y ninguna celda editable. */
    private static final class ModeloGuardados extends DefaultTableModel {

        @Serial
        private static final long serialVersionUID = 1L;

        ModeloGuardados() {
            super(COLUMNAS, 0);
        }

        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    }
}
