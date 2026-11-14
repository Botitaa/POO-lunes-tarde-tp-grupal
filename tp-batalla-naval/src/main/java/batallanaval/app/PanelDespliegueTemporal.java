package batallanaval.app;

import batallanaval.controlador.EstadoBarco;
import batallanaval.controlador.Partida;
import batallanaval.excepciones.AccionInvalidaException;
import batallanaval.modelo.Posicion;
import batallanaval.modelo.Tablero;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Consumer;

/**
 * TEMPORAL (#98): reemplazo mínimo de la pantalla de despliegue hasta que esté {@code PanelDespliegue}.
 * Ubica la flota del jugador activo al azar en su zona y confirma el despliegue, para poder jugar la
 * partida completa. Cuando llegue el panel de verdad, se borra esta clase y se cambia una línea en
 * {@link ControladorAplicacion#instalarPartida}.
 */
final class PanelDespliegueTemporal extends JPanel {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient Partida partida;
    private final transient Consumer<String> alError;
    private final transient Random azar;
    private final JLabel titulo = new JLabel(" ", SwingConstants.CENTER);

    PanelDespliegueTemporal(Partida partida, Consumer<String> alError) {
        this(partida, alError, new Random());
    }

    PanelDespliegueTemporal(Partida partida, Consumer<String> alError, Random azar) {
        this.partida = Objects.requireNonNull(partida, "partida");
        this.alError = Objects.requireNonNull(alError, "alError");
        this.azar = Objects.requireNonNull(azar, "azar");

        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = GridBagConstraints.RELATIVE;
        c.insets = new Insets(10, 0, 10, 0);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 28f));
        add(titulo, c);
        add(new JLabel("(Pantalla provisoria hasta que esté la de despliegue)", SwingConstants.CENTER), c);
        JButton boton = new JButton("Ubicar al azar y confirmar");
        boton.addActionListener(e -> desplegarAlAzar());
        add(boton, c);
    }

    /** Pone el nombre del jugador que despliega. Se llama antes de mostrar la pantalla. */
    void actualizar() {
        titulo.setText("Despliegue de " + partida.getJugadorActivo().nombre());
    }

    /** Ubica cada barco sin ubicar en una casilla libre al azar de su zona y confirma. */
    void desplegarAlAzar() {
        try {
            int numero = partida.getJugadorActivo().numero();
            List<Posicion> casillas = new ArrayList<>();
            for (int fila = 0; fila < Tablero.FILAS; fila++) {
                for (int columna = 0; columna < Tablero.COLUMNAS; columna++) {
                    Posicion posicion = new Posicion(fila, columna);
                    if (partida.esZonaDespliegue(posicion, numero)) {
                        casillas.add(posicion);
                    }
                }
            }
            Collections.shuffle(casillas, azar);
            for (EstadoBarco barco : partida.getBarcosSinUbicar()) {
                ubicarEnLaPrimeraQueSePueda(barco.id(), casillas);
            }
            partida.confirmarDespliegue();
        } catch (AccionInvalidaException e) {
            alError.accept(e.getMessage());
        }
    }

    private void ubicarEnLaPrimeraQueSePueda(int idBarco, List<Posicion> casillas) {
        while (!casillas.isEmpty()) {
            Posicion posicion = casillas.remove(casillas.size() - 1);
            try {
                partida.ubicarBarco(idBarco, posicion);
                return;
            } catch (AccionInvalidaException e) {
                // casilla no transitable: se prueba la siguiente
            }
        }
    }
}