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
            OBJETIVO
            Dos jugadores comparten la misma computadora y se turnan. Cada uno comanda una flota de \
            6 barcos. Gana quien hunde toda la flota rival o quien domina el centro del mapa.

            TABLERO
            - Es de 10x10. Las filas 0 a 2 son la zona de despliegue del Jugador 1 y las filas 7 a 9 \
            la del Jugador 2. Las filas 3 a 6 son la zona neutral, donde están los obstáculos.
            - La zona de dominación son las 4 casillas centrales (filas 4 y 5, columnas 4 y 5). \
            Siempre es agua.
            - Agua: se puede pasar y se ve a través.
            - Piedra: bloquea el paso, pero no la visión.
            - Isla: bloquea el paso y la visión.
            - Restos: es donde se hundió un barco; se puede pasar.
            - Los barcos bloquean el paso, pero no la visión ni los disparos.
            - Todas las distancias se miden en casillas, y una diagonal cuenta como un solo paso.
            - Si las dos casillas vecinas de una diagonal bloquean, la diagonal también queda \
            bloqueada (para el paso o la visión, según el tipo). Si bloquea solo una, la diagonal \
            está libre.
            
            BARCOS
            Cada jugador tiene 1 Acorazado, 2 Cruceros y 3 Destructores. Cada barco ocupa 1 casilla.
            - Destructor: vida 3, daño 3, rango 2, visión 2. Se mueve hasta 4 casillas por turno y \
            cada casilla cuesta 1 punto. Dispara todos los turnos.
            - Crucero: vida 5, daño 2, rango 4, visión 3. Se mueve hasta 3 casillas por turno y cada \
            casilla cuesta 2 puntos. Dispara todos los turnos.
            - Acorazado: vida 8, daño 4, rango 6, visión 5. Se mueve hasta 2 casillas por turno y \
            cada casilla cuesta 3 puntos. Después de disparar tiene que esperar 1 turno.

            HABILIDADES
            - Destructor: hace daño doble (6) en ese turno.
            - Crucero: se repara 2 puntos de vida, sin pasar de su vida máxima.
            - Acorazado: su rango sube a 8 en ese turno.
            Se activan con un botón, una vez por turno. No cuestan puntos de movimiento, no impiden \
            moverse y duran hasta el fin del turno. Después hay que esperar 3 turnos para volver a \
            usarlas (si se usa en el turno 1, vuelve a estar disponible en el 4). No revelan al \
            barco. Hay que activarlas antes de disparar para que afecten al disparo.

            CÓMO ES UN TURNO
            1. Al volver del blackout se muestra un resumen de lo que hizo el rival: los disparos \
            recibidos (con el daño y de dónde vinieron) y los barcos propios hundidos.
            2. Se reponen 6 puntos de movimiento para toda la flota (no se acumulan) y bajan en 1 \
            las esperas de disparo.
            3. Se activan los barcos de a uno, en el orden que se quiera: primero mover y después \
            disparar, y las dos cosas son opcionales. Un barco que disparó ya no puede moverse.
            4. Con "Terminar turno" la pantalla se oscurece (blackout) y el otro jugador toca \
            "Listo" para empezar el suyo.

            MOVIMIENTO
            - Se avanza paso a paso a una de las 8 casillas vecinas, solo hacia agua o restos libres \
            y sin salir del tablero.
            - Primero se planifica la ruta (se puede deshacer paso a paso) y después se confirma. \
            Mientras se planifica, la niebla no cambia.
            - Si al confirmar la ruta aparece un enemigo oculto, el barco frena en el paso anterior, \
            solo se cobran los pasos hechos y el enemigo queda revelado hasta que termine el turno. \
            El rival no se entera.

            DISPARO
            Se dispara una vez por turno a un barco enemigo, siempre que:
            - el jugador lo vea,
            - esté dentro del rango del barco que dispara,
            - haya línea de visión (no se dispara por encima de una isla), y
            - el barco que dispara no esté esperando.
            Siempre acierta. Si la vida del blanco llega a 0, se hunde y su casilla pasa a ser \
            restos.

            VISIÓN Y NIEBLA
            - Una casilla es visible si algún barco propio vivo está dentro de su visión y tiene \
            línea de visión hacia ella.
            - La visión se comparte: si un barco ve a un enemigo, cualquier barco propio puede \
            dispararle si cumple su propio rango y línea de visión.
            - El barco que dispara queda visible para el rival durante todo el turno siguiente de \
            ese rival.
            - Los barcos que están en la zona de dominación los ven los dos jugadores.
            - El mapa y los restos se ven siempre.

            CÓMO SE GANA
            - Hundimiento: se hunden los 6 barcos rivales. La partida termina en el acto.
            - Dominación: al terminar su turno, un jugador suma 1 punto si tiene al menos un barco \
            en la zona de dominación y el rival ninguno. Con 6 puntos gana. Los puntos no se \
            pierden.
            - Límite de turnos: la partida dura como máximo 40 turnos por jugador. Si se llega al \
            límite, gana quien tenga más puntos de dominación; si hay empate, quien tenga más barcos \
            vivos; si sigue el empate, quien tenga más vida total. Si todo es igual, es empate.
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
