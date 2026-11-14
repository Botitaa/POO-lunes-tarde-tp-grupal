package batallanaval.vista;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.IOException;
import java.io.Serial;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import batallanaval.controlador.MotivoFin;
import batallanaval.controlador.ResultadoPartida;
import batallanaval.historial.EventoPartida;

/**
 * Pantalla de fin de partida: muestra quién ganó (o si fue empate), por qué terminó y cuántas
 * rondas se jugaron, con tres botones:
 * <ul>
 *   <li><b>Ver historial</b>: despliega en la misma pantalla el historial completo de la partida.
 *       Como la partida ya terminó, no hay niebla de guerra: se ven las acciones de los dos
 *       jugadores.</li>
 *   <li><b>Abrir historial exportado</b>: abre con el programa del sistema el {@code .txt} que se
 *       escribió en {@code datos/historiales/}. Si no se pudo exportar, el botón queda
 *       deshabilitado y la pantalla lo avisa.</li>
 *   <li><b>Volver al inicio</b>: ejecuta la acción que recibió por constructor.</li>
 * </ul>
 * <p>
 * El panel no decide nada ni escribe archivos: el resultado, los eventos y la ruta del historial
 * exportado se los pasa quien lo armó (en la aplicación, {@code ControladorAplicacion}, que además
 * registra las estadísticas) con {@link #mostrarResultado(ResultadoPartida, List, Path)} antes de
 * mostrar la pantalla. Del juego solo conoce records de solo lectura ({@link ResultadoPartida} y
 * {@link EventoPartida}).
 * <p>
 * Tiene su propio hilo ({@link PanelConHilo}): cada {@value #INTERVALO_MS} ms el título alterna
 * entre dorado y el color normal, como festejo. El hilo lo inicia y lo detiene la
 * {@link VentanaPrincipal}.
 */
public final class PanelFinPartida extends PanelConHilo {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Cada cuántos milisegundos cambia el color del título. */
    static final int INTERVALO_MS = 700;

    /** Color de festejo del título. */
    static final Color COLOR_DESTACADO = new Color(212, 160, 23);

    static final String TEXTO_VER_HISTORIAL = "Ver historial";
    static final String TEXTO_OCULTAR_HISTORIAL = "Ocultar historial";
    static final String TEXTO_SIN_EXPORTAR = "No se pudo exportar el historial.";

    /**
     * Abre un archivo con el programa que corresponda. Existe para que los tests puedan reemplazar
     * a {@link Desktop}, que abriría una ventana de verdad.
     */
    @FunctionalInterface
    interface AbridorDeArchivos {
        /**
         * @param archivo archivo a abrir
         * @throws IOException si el sistema no lo pudo abrir
         */
        void abrir(Path archivo) throws IOException;
    }

    private final transient Consumer<String> alError;
    private final transient AbridorDeArchivos abridor;

    private final JLabel titulo = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel motivo = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel rondas = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel archivo = new JLabel(" ", SwingConstants.CENTER);
    private final Color colorNormalTitulo;

    private final DefaultListModel<String> modeloHistorial = new DefaultListModel<>();
    private final JScrollPane panelHistorial = new JScrollPane(new JList<>(modeloHistorial));

    private final JButton botonVerHistorial = new JButton(TEXTO_VER_HISTORIAL);
    private final JButton botonAbrir = new JButton("Abrir historial exportado");
    private final JButton botonVolver = new JButton("Volver al inicio");

    // Ruta del historial exportado de la partida que se está mostrando (null si no se pudo exportar)
    private transient Path archivoExportado;

    /**
     * Crea la pantalla. El historial exportado se abre con {@link Desktop}.
     *
     * @param alVolverAlInicio se ejecuta al tocar "Volver al inicio"
     * @param alError          recibe el mensaje para el jugador si el historial no se puede abrir
     * @throws NullPointerException si algún argumento es {@code null}
     */
    public PanelFinPartida(Runnable alVolverAlInicio, Consumer<String> alError) {
        this(alVolverAlInicio, alError, PanelFinPartida::abrirConElSistema);
    }

    /**
     * Igual que el constructor público, pero recibe cómo se abre el archivo. Es de paquete a
     * propósito: los tests pasan un abridor falso para no abrir programas de verdad.
     */
    PanelFinPartida(Runnable alVolverAlInicio, Consumer<String> alError, AbridorDeArchivos abridor) {
        Objects.requireNonNull(alVolverAlInicio, "alVolverAlInicio");
        this.alError = Objects.requireNonNull(alError, "alError");
        this.abridor = Objects.requireNonNull(abridor, "abridor");
        colorNormalTitulo = titulo.getForeground();

        setLayout(new BorderLayout(0, 15));
        setBorder(BorderFactory.createEmptyBorder(30, 60, 30, 60));
        add(armarEncabezado(), BorderLayout.NORTH);

        // El historial empieza oculto: lo despliega el botón "Ver historial"
        panelHistorial.setBorder(BorderFactory.createTitledBorder("Historial completo de la partida"));
        panelHistorial.setVisible(false);
        add(panelHistorial, BorderLayout.CENTER);

        add(armarBotones(alVolverAlInicio), BorderLayout.SOUTH);
    }

    private JPanel armarEncabezado() {
        titulo.setName("titulo");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 32f));
        motivo.setName("motivo");
        motivo.setFont(motivo.getFont().deriveFont(18f));
        rondas.setName("rondas");
        rondas.setFont(rondas.getFont().deriveFont(16f));
        archivo.setName("archivo");

        JPanel encabezado = new JPanel(new GridLayout(0, 1, 0, 8));
        encabezado.add(titulo);
        encabezado.add(motivo);
        encabezado.add(rondas);
        encabezado.add(archivo);
        return encabezado;
    }

    private JPanel armarBotones(Runnable alVolverAlInicio) {
        botonVerHistorial.addActionListener(e -> alternarHistorial());
        botonAbrir.addActionListener(e -> abrirHistorialExportado());
        botonVolver.addActionListener(e -> alVolverAlInicio.run());

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        botones.add(botonVerHistorial);
        botones.add(botonAbrir);
        botones.add(botonVolver);
        return botones;
    }

    /**
     * Carga en la pantalla cómo terminó la partida. Hay que llamarlo antes de mostrarla. Deja el
     * historial oculto, como al entrar por primera vez.
     *
     * @param resultado        cómo terminó la partida
     * @param eventos          historial completo, en orden (ya sin niebla: la partida terminó)
     * @param archivoExportado dónde quedó el historial exportado, o {@code null} si no se pudo
     *                         exportar (el botón para abrirlo queda deshabilitado)
     * @throws NullPointerException si {@code resultado} o {@code eventos} son {@code null}
     */
    public void mostrarResultado(ResultadoPartida resultado, List<EventoPartida> eventos, Path archivoExportado) {
        Objects.requireNonNull(resultado, "resultado");
        Objects.requireNonNull(eventos, "eventos");

        titulo.setText(textoTitulo(resultado));
        motivo.setText(textoMotivo(resultado.motivo()));
        rondas.setText("Rondas jugadas: " + resultado.rondas());

        modeloHistorial.clear();
        for (EventoPartida evento : eventos) {
            modeloHistorial.addElement(evento.aLinea());
        }
        panelHistorial.setVisible(false);
        botonVerHistorial.setText(TEXTO_VER_HISTORIAL);

        this.archivoExportado = archivoExportado;
        if (archivoExportado == null) {
            archivo.setText(TEXTO_SIN_EXPORTAR);
            botonAbrir.setEnabled(false);
        } else {
            archivo.setText("Historial exportado en " + archivoExportado);
            botonAbrir.setEnabled(true);
        }
        revalidate();
        repaint();
    }

    /** "¡Ganó X!" o "Empate". */
    static String textoTitulo(ResultadoPartida resultado) {
        return resultado.esEmpate() ? "Empate" : "¡Ganó " + resultado.ganador() + "!";
    }

    /** Explicación para el jugador de por qué terminó la partida. */
    static String textoMotivo(MotivoFin motivoFin) {
        return switch (motivoFin) {
            case HUNDIMIENTO -> "Hundió toda la flota rival.";
            case DOMINACION -> "Llegó a los puntos de dominación necesarios para ganar.";
            case LIMITE_TURNOS -> "Se llegó al límite de turnos y ganó el desempate "
                    + "(dominación, barcos vivos y vida total).";
            case EMPATE -> "Se llegó al límite de turnos con todo igualado.";
            case ABANDONO -> "El rival abandonó la partida.";
        };
    }

    /** Muestra u oculta el historial y cambia el texto del botón. */
    private void alternarHistorial() {
        boolean mostrar = !panelHistorial.isVisible();
        panelHistorial.setVisible(mostrar);
        botonVerHistorial.setText(mostrar ? TEXTO_OCULTAR_HISTORIAL : TEXTO_VER_HISTORIAL);
        revalidate();
        repaint();
    }

    /** Abre el historial exportado; si el sistema no puede, el jugador ve dónde está el archivo. */
    private void abrirHistorialExportado() {
        if (archivoExportado == null) {
            alError.accept(TEXTO_SIN_EXPORTAR);
            return;
        }
        try {
            abridor.abrir(archivoExportado);
        } catch (IOException | RuntimeException e) {
            // RuntimeException: Desktop puede lanzar UnsupportedOperationException o SecurityException
            alError.accept("No se pudo abrir el historial. Está en " + archivoExportado);
        }
    }

    /** Abre el archivo con el programa asociado del sistema operativo. */
    private static void abrirConElSistema(Path archivo) throws IOException {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            throw new IOException("El sistema no permite abrir archivos desde el juego");
        }
        Desktop.getDesktop().open(archivo.toFile());
    }

    // Hilo propio (PanelConHilo)

    /**
     * Paso del hilo propio: alterna el color del título entre dorado y el normal. Lo ejecuta el
     * EDT cada {@value #INTERVALO_MS} ms.
     */
    @Override
    protected void actualizar() {
        if (COLOR_DESTACADO.equals(titulo.getForeground())) {
            titulo.setForeground(colorNormalTitulo);
        } else {
            titulo.setForeground(COLOR_DESTACADO);
        }
    }

    /** @return {@value #INTERVALO_MS} */
    @Override
    protected int getIntervaloMs() {
        return INTERVALO_MS;
    }

    // Solo para los tests

    /** Líneas del historial que tiene cargadas el panel, en orden. */
    List<String> getLineasHistorial() {
        List<String> lineas = new ArrayList<>();
        for (int i = 0; i < modeloHistorial.size(); i++) {
            lineas.add(modeloHistorial.get(i));
        }
        return lineas;
    }

    boolean isHistorialVisible() {
        return panelHistorial.isVisible();
    }

    JLabel getTitulo() {
        return titulo;
    }

    JLabel getMotivo() {
        return motivo;
    }

    JLabel getRondas() {
        return rondas;
    }

    JLabel getArchivo() {
        return archivo;
    }

    JButton getBotonVerHistorial() {
        return botonVerHistorial;
    }

    JButton getBotonAbrir() {
        return botonAbrir;
    }

    JButton getBotonVolver() {
        return botonVolver;
    }
}
