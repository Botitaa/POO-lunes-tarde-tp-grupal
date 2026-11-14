package batallanaval.vista;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.Serial;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

import batallanaval.persistencia.EstadisticaJugador;

/**
 * Pantalla con el ranking de jugadores: una {@link JTable} de solo lectura con la posición, el
 * nombre, las partidas jugadas, ganadas, perdidas y empatadas, y el porcentaje de victorias.
 * Tiene un botón para volver al menú.
 * <p>
 * El panel no calcula nada: muestra las filas en el mismo orden en que las recibe. El orden lo
 * define {@code CalculadoraRanking.calcular(...)}, y quien arma la pantalla (en la aplicación,
 * {@code ControladorAplicacion}) tiene que llamar a {@link #cargarRanking(List)} cada vez que la
 * va a mostrar, para que refleje las partidas más recientes.
 * <p>
 * Tiene su propio hilo ({@link PanelConHilo}): cada {@value #INTERVALO_MS} ms muestra cuántos
 * jugadores hay en el ranking y la hora. El hilo lo inicia y lo detiene la {@link VentanaPrincipal}.
 */
public final class PanelEstadisticas extends PanelConHilo {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Texto que se muestra debajo del título cuando todavía no hay ninguna partida registrada. */
    static final String MENSAJE_SIN_DATOS = "Todavía no hay partidas registradas";

    private static final String[] COLUMNAS = {
        "#", "Jugador", "Jugadas", "Ganadas", "Perdidas", "Empatadas", "% Victorias"
    };

    // Siempre con coma decimal, sin depender del idioma configurado en el sistema operativo.
    private static final Locale LOCALE_PORCENTAJE = Locale.forLanguageTag("es-AR");
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** Cada cuántos milisegundos se actualiza el contador y la hora. */
    static final int INTERVALO_MS = 1000;

    private final VentanaPrincipal ventana;
    private final ModeloRanking modelo = new ModeloRanking();
    private final JLabel avisoSinDatos = new JLabel(MENSAJE_SIN_DATOS, SwingConstants.CENTER);
    private final JLabel etiquetaEstado = new JLabel(" ", SwingConstants.CENTER);

    /**
     * Crea la pantalla con la tabla vacía.
     *
     * @param ventana ventana desde la que se vuelve al menú
     * @throws NullPointerException si {@code ventana} es {@code null}
     */
    public PanelEstadisticas(VentanaPrincipal ventana) {
        this.ventana = Objects.requireNonNull(ventana, "ventana");
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        armarContenido();
    }

    /**
     * Reemplaza las filas de la tabla por las del ranking recibido, en ese mismo orden.
     * Si la lista está vacía, la tabla queda sin filas y se muestra un aviso.
     *
     * @param ranking estadísticas de los jugadores, ya ordenadas (la posición es el orden en la lista)
     * @throws NullPointerException si {@code ranking} es {@code null}
     */
    public void cargarRanking(List<EstadisticaJugador> ranking) {
        Objects.requireNonNull(ranking, "ranking");
        modelo.setRowCount(0);
        int posicion = 1;
        for (EstadisticaJugador estadistica : ranking) {
            modelo.addRow(new Object[] {
                posicion,
                estadistica.nombre(),
                estadistica.jugadas(),
                estadistica.ganadas(),
                estadistica.perdidas(),
                estadistica.empatadas(),
                formatearPorcentaje(estadistica.porcentajeVictorias())
            });
            posicion++;
        }
        avisoSinDatos.setVisible(ranking.isEmpty());
    }

    private static String formatearPorcentaje(double porcentaje) {
        return String.format(LOCALE_PORCENTAJE, "%.1f %%", porcentaje);
    }

    private void armarContenido() {
        JLabel titulo = new JLabel("Ranking de jugadores", SwingConstants.CENTER);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 28f));
        avisoSinDatos.setFont(avisoSinDatos.getFont().deriveFont(Font.PLAIN, 15f));

        JPanel encabezado = new JPanel(new BorderLayout(0, 4));
        encabezado.add(titulo, BorderLayout.NORTH);
        encabezado.add(avisoSinDatos, BorderLayout.SOUTH);
        add(encabezado, BorderLayout.NORTH);

        JTable tabla = new JTable(modelo);
        tabla.setFont(tabla.getFont().deriveFont(15f));
        tabla.setRowHeight(28);
        tabla.setFillsViewportHeight(true);
        tabla.getTableHeader().setReorderingAllowed(false);
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        JButton botonVolver = new JButton("Volver");
        botonVolver.addActionListener(
                EjecutorAcciones.alTocar(() -> ventana.mostrar(Pantalla.INICIO), ventana::mostrarError));
        JPanel pie = new JPanel(new FlowLayout(FlowLayout.CENTER));
        pie.add(botonVolver);
        etiquetaEstado.setName("etiquetaEstado");
        JPanel abajo = new JPanel(new BorderLayout());
        abajo.add(pie, BorderLayout.CENTER);
        abajo.add(etiquetaEstado, BorderLayout.SOUTH);
        add(abajo, BorderLayout.SOUTH);

        // Todavía no se cargó nada: se muestra el aviso hasta la primera llamada a cargarRanking.
        avisoSinDatos.setVisible(true);
    }

    // Hilo propio (PanelConHilo)

    /**
     * Paso del hilo propio: muestra cuántos jugadores hay en el ranking y la hora.
     * Lo ejecuta el EDT cada {@value #INTERVALO_MS} ms.
     */
    @Override
    protected void actualizar() {
        etiquetaEstado.setText(modelo.getRowCount() + " jugadores en el ranking · "
                + LocalTime.now().format(FORMATO_HORA));
    }

    /** @return {@value #INTERVALO_MS} */
    @Override
    protected int getIntervaloMs() {
        return INTERVALO_MS;
    }

    /** Modelo de la tabla: las columnas fijas del ranking y ninguna celda editable. */
    private static final class ModeloRanking extends DefaultTableModel {

        @Serial
        private static final long serialVersionUID = 1L;

        ModeloRanking() {
            super(COLUMNAS, 0);
        }

        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    }
}