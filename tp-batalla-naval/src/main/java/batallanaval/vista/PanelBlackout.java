package batallanaval.vista;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.Serial;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * Pantalla de blackout: fondo negro con el cartel "Turno de X" y dos botones, <b>Listo</b> y
 * <b>Guardar partida</b>. Se muestra entre un turno y el siguiente para que el jugador que
 * termina no deje ver su tablero y el otro lo reciba recién cuando toca "Listo" (hot-seat).
 * <p>
 * Es el único lugar de la aplicación desde donde se puede guardar la partida.
 * <p>
 * El panel no decide nada: cuando se toca un botón, ejecuta la acción que recibió por
 * constructor. Quien lo creó (en la aplicación, {@code ControladorAplicacion}) es el que
 * cambia de pantalla o guarda la partida (en un hilo propio, para no congelar la ventana).
 * Antes de mostrar esta pantalla hay que llamar a {@link #mostrarTurnoDe(String)}.
 * <p>
 * Tiene su propio hilo ({@link PanelConHilo}): cada {@value #INTERVALO_MS} ms hace parpadear un
 * aviso debajo del cartel. El hilo lo inicia y lo detiene la {@link VentanaPrincipal}.
 */
public final class PanelBlackout extends PanelConHilo {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Dimension TAMANIO_BOTON = new Dimension(220, 44);

    /** Cada cuántos milisegundos se prende o se apaga el aviso. */
    static final int INTERVALO_MS = 600;

    /** Aviso que parpadea debajo del cartel. */
    static final String TEXTO_AVISO = "Tocá Listo cuando tengas la computadora";

    private final transient Runnable alListo;
    private final transient Runnable alGuardar;
    private final JLabel cartelTurno = new JLabel(" ", SwingConstants.CENTER);
    private final JButton botonListo = new JButton("Listo");
    private final JButton botonGuardar = new JButton("Guardar partida");
    private final JLabel aviso = new JLabel(TEXTO_AVISO, SwingConstants.CENTER);

    /**
     * @param alListo   se ejecuta cuando el jugador que recibe la computadora toca "Listo"
     * @param alGuardar se ejecuta cuando se toca "Guardar partida"
     * @throws NullPointerException si alguna de las dos acciones es {@code null}
     */
    public PanelBlackout(Runnable alListo, Runnable alGuardar) {
        this.alListo = Objects.requireNonNull(alListo, "alListo");
        this.alGuardar = Objects.requireNonNull(alGuardar, "alGuardar");
        setBackground(Color.BLACK);
        setLayout(new GridBagLayout());
        armarContenido();
    }

    /**
     * Cambia el cartel para indicar de quién es el turno que viene.
     *
     * @param nombreJugador nombre del jugador que va a jugar
     * @throws NullPointerException si {@code nombreJugador} es {@code null}
     */
    public void mostrarTurnoDe(String nombreJugador) {
        Objects.requireNonNull(nombreJugador, "nombreJugador");
        cartelTurno.setText("Turno de " + nombreJugador);
    }

    /**
     * Habilita o deshabilita los dos botones. Sirve para bloquearlos mientras se guarda la
     * partida y volver a habilitarlos cuando termina (con o sin error).
     *
     * @param habilitados {@code true} para que se puedan tocar, {@code false} para bloquearlos
     */
    public void setBotonesHabilitados(boolean habilitados) {
        botonListo.setEnabled(habilitados);
        botonGuardar.setEnabled(habilitados);
    }

    private void armarContenido() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = GridBagConstraints.RELATIVE;

        cartelTurno.setFont(cartelTurno.getFont().deriveFont(Font.BOLD, 48f));
        cartelTurno.setForeground(Color.WHITE);
        c.insets = new Insets(0, 0, 10, 0);
        add(cartelTurno, c);

        aviso.setName("aviso");
        aviso.setFont(aviso.getFont().deriveFont(Font.PLAIN, 16f));
        aviso.setForeground(Color.LIGHT_GRAY);
        c.insets = new Insets(0, 0, 30, 0);
        add(aviso, c);

        c.insets = new Insets(6, 0, 6, 0);
        configurarBoton(botonListo, alListo);
        add(botonListo, c);
        configurarBoton(botonGuardar, alGuardar);
        add(botonGuardar, c);
    }

    // Hilo propio (PanelConHilo)

    /**
     * Paso del hilo propio: prende o apaga el aviso. Se cambia el texto (y no se oculta la
     * etiqueta) para que los botones no se muevan. Lo ejecuta el EDT cada {@value #INTERVALO_MS} ms.
     */
    @Override
    protected void actualizar() {
        if (aviso.getText().equals(TEXTO_AVISO)) {
            aviso.setText(" ");
        } else {
            aviso.setText(TEXTO_AVISO);
        }
    }

    /** @return {@value #INTERVALO_MS} */
    @Override
    protected int getIntervaloMs() {
        return INTERVALO_MS;
    }

    private void configurarBoton(JButton boton, Runnable accion) {
        boton.setPreferredSize(TAMANIO_BOTON);
        boton.addActionListener(e -> accion.run());
    }
}