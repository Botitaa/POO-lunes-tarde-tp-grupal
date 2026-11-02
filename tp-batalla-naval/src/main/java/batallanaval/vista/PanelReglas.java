package batallanaval.vista;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.io.Serial;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

/**
 * Pantalla con las reglas del juego: un texto de solo lectura dentro de un
 * {@link JScrollPane}, con un botón para volver al menú.
 */
public final class PanelReglas extends JPanel {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String TEXTO_REGLAS = """
            Tablero
            - 10x10. Filas 0–2: despliegue J1. Filas 3–6: zona neutral (único lugar con obstáculos). Filas \
            7–9: despliegue J2.
            - Zona de dominación: las 4 casillas centrales (filas 4–5, columnas 4–5), siempre agua.
            - Mapas en espejo (simetría punto a punto respecto del centro). El MVP tiene un solo mapa.

            - AGUA: Bloquea paso: No; Bloquea visión: No
            - PIEDRA: Bloquea paso: Sí; Bloquea visión: No
            - ISLA: Bloquea paso: Sí; Bloquea visión: Sí
            - RESTOS (barco hundido): Bloquea paso: No; Bloquea visión: No

            - Los barcos bloquean el paso, pero no la visión ni los disparos.
            - Diagonal entre dos casillas ortogonales que bloquean → la diagonal también bloquea (paso o \
            visión según el tipo). Si bloquea solo una, está libre.
            - Todas las distancias son de Chebyshev: max(|f1-f2|, |c1-c2|).

            Barcos (cada jugador: 1 acorazado, 2 cruceros, 3 destructores; cada barco ocupa 1 casilla)

            - Destructor: Vida: 3; Costo por casilla (puntos): 1; Máx. casillas por turno: 4; Rango de \
            disparo: 2; Visión: 2; Daño: 3; Cooldown de disparo (turnos): 0; Habilidad: Daño doble (6) este \
            turno
            - Crucero: Vida: 5; Costo por casilla (puntos): 2; Máx. casillas por turno: 3; Rango de disparo: \
            4; Visión: 3; Daño: 2; Cooldown de disparo (turnos): 0; Habilidad: Se repara 2 (sin pasar la \
            vida máx.)
            - Acorazado: Vida: 8; Costo por casilla (puntos): 3; Máx. casillas por turno: 2; Rango de \
            disparo: 6; Visión: 5; Daño: 4; Cooldown de disparo (turnos): 1; Habilidad: Rango 8 este turno

            - Habilidades: se activan con un botón, una vez por turno, no cuestan puntos, no impiden \
            moverse, duran hasta el fin del turno, espera de 3 turnos (usada en el turno 1 → disponible en \
            el 4), no revelan al barco. Hay que activarlas antes de disparar para que afecten el disparo.

            Turno
            1. Al volver del blackout: resumen de lo que hizo el rival (disparos recibidos con daño y \
            origen, barcos propios hundidos).
            2. Se reponen 6 puntos de movimiento para toda la flota (no se acumulan) y bajan en 1 los \
            cooldowns.
            3. El jugador activa sus barcos de a uno en el orden que quiera: primero mover, después disparar \
            (ambos opcionales). Un barco que disparó no puede moverse.
            4. "Terminar turno" → blackout → el otro toca "Listo".

            Movimiento
            - Paso a paso a una de las 8 adyacentes. Solo a AGUA o RESTOS libres, sin salir del tablero.
            - Se planifica la ruta (con "Deshacer paso") y después se confirma; mientras se planifica la \
            niebla no cambia.
            - Si al confirmar la ruta choca con un enemigo oculto: frena en el paso anterior, se cobran solo \
            los pasos hechos, el enemigo queda revelado hasta fin del turno y el rival no se entera.

            Disparo
            Una vez por turno, a un barco enemigo, si: (1) es visible para el jugador, (2) está a distancia \
            ≤ rango, (3) hay línea de visión (no se dispara por encima de islas), (4) el tirador no está en \
            cooldown. Siempre acierta. Vida 0 → se hunde y su casilla pasa a RESTOS.

            Visión y niebla
            - Una casilla es visible para un jugador si algún barco propio vivo está a distancia ≤ su visión \
            y tiene línea de visión.
            - Visión compartida: si un barco ve a un enemigo, cualquier barco propio puede dispararle (si \
            cumple su rango y línea de visión).
            - Línea de visión: Bresenham entre centros; si una casilla intermedia es ISLA, no hay visión. \
            Los extremos no cuentan.
            - Revelado por disparo: el que dispara queda visible para el rival durante todo el turno \
            siguiente del rival.
            - Los barcos en la zona de dominación son visibles para ambos.
            - El mapa y los restos se ven siempre.

            Victoria
            - Hundimiento: hundir los 6 barcos rivales (termina en el acto).
            - Dominación: sumar 6 puntos. Al terminar su turno, un jugador suma 1 si tiene ≥1 barco en la \
            zona y el rival ninguno. Los puntos no se pierden.
            - Límite: 40 turnos por jugador. Gana más dominación → más barcos vivos → más vida total → \
            empate.
            """;

    private final VentanaPrincipal ventana;

    /**
     * @param ventana ventana desde la que se vuelve al menú
     * @throws NullPointerException si {@code ventana} es {@code null}
     */
    public PanelReglas(VentanaPrincipal ventana) {
        this.ventana = Objects.requireNonNull(ventana, "ventana");
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        armarContenido();
    }

    private void armarContenido() {
        JLabel titulo = new JLabel("Reglas del juego", SwingConstants.CENTER);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 28f));
        add(titulo, BorderLayout.NORTH);

        JTextArea texto = new JTextArea(TEXTO_REGLAS);
        texto.setEditable(false);
        texto.setLineWrap(true);
        texto.setWrapStyleWord(true);
        texto.setFont(texto.getFont().deriveFont(15f));
        texto.setMargin(new Insets(10, 12, 10, 12));
        texto.setCaretPosition(0);
        add(new JScrollPane(texto), BorderLayout.CENTER);

        JButton botonVolver = new JButton("Volver");
        botonVolver.addActionListener(e -> ventana.mostrar(Pantalla.INICIO));
        JPanel pie = new JPanel(new FlowLayout(FlowLayout.CENTER));
        pie.add(botonVolver);
        add(pie, BorderLayout.SOUTH);
    }
}
