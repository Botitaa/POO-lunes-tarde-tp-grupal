package batallanaval.vista;

import batallanaval.controlador.EstadoBarco;
import batallanaval.controlador.FasePartida;
import batallanaval.controlador.ObservadorPartida;
import batallanaval.controlador.PartidaCombate;
import batallanaval.controlador.PartidaTablero;
import batallanaval.controlador.ResultadoPartida;
import batallanaval.modelo.Posicion;
import batallanaval.modelo.Casilla;
import batallanaval.modelo.Disparo;
import batallanaval.excepciones.AccionInvalidaException;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Panel que extiende de JPanel para representar la grilla de 10x10 del tablero de Batalla Naval.
 * Dibuja cada tipo de casilla, las zonas de despliegue, la zona de dominación,
 * los barcos propios y los enemigos visibles (símbolo, vida y barra de vida, con el color del jugador)
 * y la niebla de guerra sobre las casillas que el jugador no ve.
 * Cuando hay un barco seleccionado, lo resalta, sombrea las casillas a las que puede llegar
 * y dibuja la ruta planificada paso a paso con su costo. Si ese barco puede disparar, marca en rojo
 * los objetivos válidos, y un clic sobre uno de ellos dispara. También marca hasta dónde llega el disparo
 * del barco elegido. Cada disparo (con clic o con el botón del panel) se anima: una bala que va del
 * tirador al blanco, una explosión y el daño que sube ("-3"); si hunde al blanco, dice "¡Hundido!".
 * Al empezar el turno marca los barcos propios que recibieron disparos en el turno del rival, con el
 * daño y una línea desde donde vino el tiro. Los barcos propios que ya no pueden moverse ni disparar
 * se dibujan en gris. Cada barco se dibuja como un casco visto desde arriba, apuntando hacia el rival.
 * Un clic en un barco propio lo selecciona y, con un barco seleccionado, un clic en otra casilla
 * agrega ese paso a la ruta (después se confirma con Enter o con el botón del panel lateral).
 * Al pasar el mouse sobre un barco muestra un tooltip con sus atributos.
 * Atajos de teclado (key bindings): Esc cancela la selección, Backspace deshace el último paso
 * de la ruta y Enter confirma el movimiento.
 * Todo lo que dibuja se lo pregunta a PartidaTablero: el panel no conoce el Tablero.
 * Quien arma el panel puede cambiar qué hace un clic con {@link #setAlClickearCasilla(Consumer)}
 * (la pantalla de despliegue lo usa para ubicar barcos).
 * En el despliegue se activa {@link #setModoDespliegue(boolean)}: sin niebla y sin atajos de combate.
 * Implementa ObservadorPartida: quien crea el panel lo registra en la partida con
 * agregarObservador, y desde ahí el panel se redibuja ante cada cambio.
 */
public class PanelTablero extends JPanel implements MouseListener, MouseMotionListener, ObservadorPartida {

    private static final int FILAS = 10;
    private static final int COLUMNAS = 10;

    // Duración de la animación de un disparo y de un hundimiento, en milisegundos
    private static final int DURACION_DISPARO = 1100;
    private static final int DURACION_HUNDIMIENTO = 1600;
    // Primera parte de la animación: la bala viaja del tirador al blanco
    private static final int DURACION_VIAJE_BALA = 250;
    // Cada cuánto se redibuja la animación (unos 30 cuadros por segundo)
    private static final int MS_POR_CUADRO = 33;

    // Parte de la partida que necesita el panel para dibujar (ISP: no recibe la Partida entera)
    private final PartidaTablero partidaTablero;

    // Acciones del turno que llaman los atajos de teclado y los clics: seleccionar, mover y disparar (ISP: solo las de combate)
    private final PartidaCombate partidaCombate;

    // Guarda la posición de la celda sobre la cual está el mouse actualmente
    private Posicion casillaHover = null;

    // Animación del último disparo: cuál se está animando, cuándo empezó y el Timer que redibuja
    private transient Disparo disparoAnimado = null;
    private long inicioAnimacion = 0;
    private final Timer temporizadorAnimacion;

    // Qué hacer cuando se hace clic en una casilla (null: el clic es de la fase de juego)
    private transient Consumer<Posicion> alClickearCasilla = null;

    // En el despliegue no hay niebla (el jugador tiene que ver su zona) ni atajos de combate
    private boolean modoDespliegue = false;

    /**
     * Crea el panel del tablero.
     *
     * @param partidaTablero consultas de la partida de las que se leen las casillas y las zonas.
     * @param partidaCombate acciones del turno que llaman los atajos de teclado y los clics en el tablero.
     * @throws NullPointerException si alguno de los dos es nulo.
     */
    public PanelTablero(PartidaTablero partidaTablero, PartidaCombate partidaCombate) {
        this.partidaTablero = Objects.requireNonNull(partidaTablero, "La partida no puede ser nula.");
        this.partidaCombate = Objects.requireNonNull(partidaCombate, "La partida no puede ser nula.");
        setBackground(Color.DARK_GRAY);

        // Activa los tooltips del panel; el texto de cada uno lo arma getToolTipText(MouseEvent)
        setToolTipText("");

        // Registramos los listeners para las acciones del mouse
        addMouseListener(this);
        addMouseMotionListener(this);

        // Atajos de teclado: Esc, Backspace y Enter
        registrarAtajosDeTeclado();

        // Timer de Swing para la animación de los disparos: corre en el hilo de eventos (EDT),
        // así que puede repintar sin problemas. Se frena solo cuando termina la animación.
        temporizadorAnimacion = new Timer(MS_POR_CUADRO, e -> avanzarAnimacion());
    }

    /**
     * Define qué pasa cuando el jugador hace clic en una casilla del tablero. El panel solo traduce
     * los píxeles a una {@link Posicion}; quien lo armó decide qué hacer con ella (en el despliegue,
     * ubicar un barco). Los clics fuera de la grilla no se informan. Si no se define ninguna acción,
     * el clic hace lo de la fase de juego: disparar, seleccionar un barco o agregar un paso.
     *
     * @param alClickearCasilla acción que recibe la casilla clickeada, o {@code null} para usar el
     *                          comportamiento de la fase de juego
     */
    public void setAlClickearCasilla(Consumer<Posicion> alClickearCasilla) {
        this.alClickearCasilla = alClickearCasilla;
    }

    /**
     * Activa o desactiva el modo despliegue. Con el modo activo no se dibuja la niebla (el jugador
     * tiene que ver toda su zona para ubicar los barcos) y los atajos Esc, Backspace y Enter no
     * hacen nada, porque son de la fase de juego. Por defecto está desactivado.
     *
     * @param modoDespliegue {@code true} para el despliegue, {@code false} para jugar
     */
    public void setModoDespliegue(boolean modoDespliegue) {
        this.modoDespliegue = modoDespliegue;
        repaint();
    }

    /**
     * Dibuja las casillas, las zonas de despliegue, la niebla de guerra, las casillas alcanzables,
     * la ruta planificada, los barcos, los objetivos válidos, el barco seleccionado, la zona de dominación,
     * el resaltado de la casilla bajo el mouse y la animación de un disparo.
     *
     * @param g contexto gráfico que entrega Swing
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int tamanoCelda = calcularTamanoCelda();
        int inicioX = calcularInicioX(tamanoCelda);
        int inicioY = calcularInicioY(tamanoCelda);

        // Límites de la zona de dominación: se completan mientras se recorre la grilla
        int filaMinDominacion = FILAS;
        int filaMaxDominacion = -1;
        int colMinDominacion = COLUMNAS;
        int colMaxDominacion = -1;

        // Recorremos la matriz lógica de 10x10
        for (int fila = 0; fila < FILAS; fila++) {
            for (int col = 0; col < COLUMNAS; col++) {
                Posicion pos = new Posicion(fila, col);
                int x = inicioX + (col * tamanoCelda);
                int y = inicioY + (fila * tamanoCelda);

                // Le preguntamos a la partida qué casilla hay en esta posición
                Casilla casilla = partidaTablero.getCasilla(pos);

                // Asignamos el color de fondo según el tipo de casilla
                switch (casilla) {
                    case AGUA:
                        g2d.setColor(new Color(51, 153, 255)); // Azul agua
                        break;
                    case ISLA:
                        g2d.setColor(new Color(46, 139, 87));  // Verde isla
                        break;
                    case PIEDRA:
                        g2d.setColor(new Color(112, 128, 144)); // Gris piedra
                        break;
                    case RESTOS:
                        g2d.setColor(new Color(70, 70, 70));   // Fondo oscuro para restos
                        break;
                }
                g2d.fillRect(x, y, tamanoCelda, tamanoCelda);

                // Si la casilla contiene restos de un barco hundido, dibujamos una "X" negra
                if (casilla == Casilla.RESTOS) {
                    g2d.setColor(Color.BLACK);
                    g2d.setStroke(new BasicStroke(2));
                    g2d.drawLine(x + 4, y + 4, x + tamanoCelda - 4, y + tamanoCelda - 4);
                    g2d.drawLine(x + tamanoCelda - 4, y + 4, x + 4, y + tamanoCelda - 4);
                }

                // Marcamos las zonas de despliegue con un tinte semitransparente del color de cada jugador
                if (partidaTablero.esZonaDespliegue(pos, 1)) {
                    g2d.setColor(new Color(230, 90, 50, 50));  // Naranja: jugador 1
                    g2d.fillRect(x, y, tamanoCelda, tamanoCelda);
                } else if (partidaTablero.esZonaDespliegue(pos, 2)) {
                    g2d.setColor(new Color(170, 80, 220, 50)); // Violeta: jugador 2
                    g2d.fillRect(x, y, tamanoCelda, tamanoCelda);
                }

                // Niebla de guerra: si el jugador no ve esta casilla, la oscurecemos.
                // El mapa se sigue viendo debajo (el terreno y los restos se ven siempre).
                // Qué es visible lo decide la partida, el panel solo pregunta.
                if (!modoDespliegue && !partidaTablero.esVisible(pos)) {
                    g2d.setColor(new Color(30, 40, 60, 150)); // Azul oscuro grisado, semitransparente
                    g2d.fillRect(x, y, tamanoCelda, tamanoCelda);
                }

                // Si la casilla es de la zona de dominación, actualizamos sus límites
                if (partidaTablero.esZonaDominacion(pos)) {
                    filaMinDominacion = Math.min(filaMinDominacion, fila);
                    filaMaxDominacion = Math.max(filaMaxDominacion, fila);
                    colMinDominacion = Math.min(colMinDominacion, col);
                    colMaxDominacion = Math.max(colMaxDominacion, col);
                }

                // Dibujamos el borde estándar de la celda
                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(1));
                g2d.drawRect(x, y, tamanoCelda, tamanoCelda);

                // Coordenada de la casilla (fila,columna), chiquita en la esquina de arriba a la izquierda
                dibujarCoordenada(g2d, fila, col, x, y, tamanoCelda);
            }
        }

        // Alcance de disparo del barco elegido, casillas alcanzables y ruta (debajo de los barcos)
        dibujarAlcanceDeDisparo(g2d, tamanoCelda, inicioX, inicioY);
        dibujarCasillasAlcanzables(g2d, tamanoCelda, inicioX, inicioY);
        dibujarRuta(g2d, tamanoCelda, inicioX, inicioY);

        // Los barcos se dibujan arriba de las casillas ya pintadas.
        // Los enemigos son solo los que la partida dice que el jugador ve (no se recalcula acá).
        dibujarBarcos(g2d, partidaTablero.getBarcosPropios(), tamanoCelda, inicioX, inicioY);
        dibujarBarcos(g2d, partidaTablero.getBarcosEnemigosVisibles(), tamanoCelda, inicioX, inicioY);

        // Lo que le pegaron a este jugador en el turno del rival
        dibujarDisparosRecibidos(g2d, tamanoCelda, inicioX, inicioY);

        // Objetivos válidos del barco seleccionado: borde rojo sobre cada uno
        dibujarObjetivosValidos(g2d, tamanoCelda, inicioX, inicioY);

        // El barco seleccionado lleva un borde amarillo, arriba de su dibujo
        dibujarBarcoSeleccionado(g2d, tamanoCelda, inicioX, inicioY);

        // Los barcos propios con la habilidad activa llevan un contorno celeste (adentro del amarillo)
        for (EstadoBarco barco : partidaTablero.getBarcosPropios()) {
            if (barco.habilidadActiva() && barco.posicion() != null) {
                dibujarContornoHabilidad(g2d, inicioX + barco.posicion().getColumna() * tamanoCelda,
                        inicioY + barco.posicion().getFila() * tamanoCelda, tamanoCelda);
            }
        }

        // La zona de dominación y el hover se dibujan después del for: si se dibujaran
        // adentro, las celdas siguientes les pintarían el borde negro encima.

        // Marcamos la zona de dominación con un solo borde dorado alrededor de todas sus casillas
        if (filaMaxDominacion >= 0) {
            int x = inicioX + (colMinDominacion * tamanoCelda);
            int y = inicioY + (filaMinDominacion * tamanoCelda);
            int ancho = (colMaxDominacion - colMinDominacion + 1) * tamanoCelda;
            int alto = (filaMaxDominacion - filaMinDominacion + 1) * tamanoCelda;
            g2d.setColor(new Color(218, 165, 32)); // Dorado
            g2d.setStroke(new BasicStroke(3));
            g2d.drawRect(x, y, ancho, alto);
        }

        // Efecto Hover (resaltado blanco semitransparente)
        if (casillaHover != null) {
            int x = inicioX + (casillaHover.getColumna() * tamanoCelda);
            int y = inicioY + (casillaHover.getFila() * tamanoCelda);
            g2d.setColor(new Color(255, 255, 255, 80));
            g2d.fillRect(x, y, tamanoCelda, tamanoCelda);
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRect(x, y, tamanoCelda, tamanoCelda);
        }

        // Animación del último disparo, arriba de todo
        dibujarAnimacionDisparo(g2d, tamanoCelda, inicioX, inicioY);
    }

    // Dibujo de barcos

    /**
     * Dibuja los barcos de la lista que ya tienen posición. Los datos llegan de la partida
     * como EstadoBarco (solo lectura): el panel nunca recibe un Barco del modelo.
     */
    private void dibujarBarcos(Graphics2D g2d, List<EstadoBarco> barcos, int tamanoCelda, int inicioX, int inicioY) {
        for (EstadoBarco barco : barcos) {
            Posicion pos = barco.posicion();
            if (pos != null) { // durante el despliegue puede haber barcos sin ubicar
                int x = inicioX + (pos.getColumna() * tamanoCelda);
                int y = inicioY + (pos.getFila() * tamanoCelda);
                dibujarBarco(g2d, barco, x, y, tamanoCelda);
            }
        }
    }

    /**
     * Dibuja un barco en su celda como un casco visto desde arriba, con la proa apuntando hacia el
     * rival (abajo para el jugador 1, arriba para el 2) y una torreta por cada nivel de tamaño:
     * 1 el destructor, 2 el crucero y 3 el acorazado. En una esquina va el símbolo con la vida
     * (por ejemplo "C5") y abajo una barra de vida. Los barcos propios que ya no pueden hacer nada
     * en este turno se dibujan en gris.
     */
    private void dibujarBarco(Graphics2D g2d, EstadoBarco barco, int x, int y, int tamanoCelda) {
        boolean apagado = barco.esPropio() && !modoDespliegue && !partidaTablero.puedeActuar(barco.id());
        Color base = apagado ? new Color(125, 125, 125) : colorJugador(barco.numeroJugador());

        double ancho = proporcionAncho(barco.simbolo()) * tamanoCelda;
        double largo = 0.8 * tamanoCelda;
        double centroX = x + tamanoCelda / 2.0;
        double centroY = y + tamanoCelda * 0.47;

        Graphics2D g = (Graphics2D) g2d.create();
        g.translate(centroX, centroY);
        if (barco.numeroJugador() == 2) {
            g.scale(1, -1); // el jugador 2 apunta hacia arriba
        }

        // Casco: popa redondeada arriba y proa en punta abajo (coordenadas locales, centro en 0,0)
        double popa = -largo / 2;
        double proa = largo / 2;
        Path2D casco = new Path2D.Double();
        casco.moveTo(-ancho / 2, popa + ancho * 0.3);
        casco.quadTo(-ancho / 2, popa, 0, popa);
        casco.quadTo(ancho / 2, popa, ancho / 2, popa + ancho * 0.3);
        casco.lineTo(ancho / 2, proa - largo * 0.38);
        casco.quadTo(ancho / 2, proa - largo * 0.1, 0, proa);
        casco.quadTo(-ancho / 2, proa - largo * 0.1, -ancho / 2, proa - largo * 0.38);
        casco.closePath();
        g.setColor(new Color(0, 0, 0, 70)); // sombra
        g.translate(1.5, 2);
        g.fill(casco);
        g.translate(-1.5, -2);
        g.setColor(base);
        g.fill(casco);
        g.setColor(base.darker().darker());
        g.setStroke(new BasicStroke(Math.max(1.5f, tamanoCelda / 40f)));
        g.draw(casco);

        // Cubierta: una franja más clara en el medio del casco
        g.setColor(mezclar(base, Color.WHITE, 0.35));
        g.fill(new Path2D.Double(new java.awt.geom.RoundRectangle2D.Double(
                -ancho * 0.18, popa + largo * 0.12, ancho * 0.36, largo * 0.62, ancho * 0.2, ancho * 0.2)));

        // Torretas con el cañón apuntando a la proa
        int torretas = cantidadTorretas(barco.simbolo());
        double radio = ancho * 0.2;
        for (int i = 0; i < torretas; i++) {
            double ty = popa + largo * 0.28 + i * largo * (torretas == 1 ? 0 : 0.42 / (torretas - 1));
            if (torretas == 1) {
                ty = 0;
            }
            g.setColor(base.darker().darker());
            g.setStroke(new BasicStroke(Math.max(2f, tamanoCelda / 28f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Line2D.Double(0, ty, 0, ty + radio * 2.1));
            g.setColor(base.darker());
            g.fill(new Ellipse2D.Double(-radio, ty - radio, radio * 2, radio * 2));
        }
        g.dispose();

        // Etiqueta con símbolo y vida, en la esquina de arriba a la derecha
        // (la de la izquierda es para la coordenada de la casilla)
        String texto = "" + barco.simbolo() + barco.vida();
        g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(9, tamanoCelda / 5)));
        FontMetrics medidas = g2d.getFontMetrics();
        int anchoEtiqueta = medidas.stringWidth(texto) + 6;
        int altoEtiqueta = medidas.getAscent() + 2;
        g2d.setColor(new Color(20, 20, 30, 190));
        int xEtiqueta = x + tamanoCelda - anchoEtiqueta - 2;
        g2d.fillRoundRect(xEtiqueta, y + 2, anchoEtiqueta, altoEtiqueta, 6, 6);
        g2d.setColor(Color.WHITE);
        g2d.drawString(texto, xEtiqueta + 3, y + 1 + medidas.getAscent());

        // Barra de vida: fondo oscuro y encima la parte que le queda, proporcional a la vida
        int margen = tamanoCelda / 8;
        int altoBarra = Math.max(3, tamanoCelda / 12);
        int xBarra = x + margen;
        int yBarra = y + tamanoCelda - altoBarra - 3;
        int anchoBarra = tamanoCelda - 2 * margen;
        int anchoVida = anchoBarra * barco.vida() / barco.vidaMaxima();
        g2d.setColor(new Color(40, 40, 40));
        g2d.fillRect(xBarra, yBarra, anchoBarra, altoBarra);
        g2d.setColor(colorVida(barco.vida(), barco.vidaMaxima()));
        g2d.fillRect(xBarra, yBarra, anchoVida, altoBarra);
    }

    /**
     * Escribe la coordenada "fila,columna" en la esquina de arriba a la izquierda de la casilla,
     * con la misma numeración que usan el historial y los avisos, por ejemplo "4,7".
     */
    private static void dibujarCoordenada(Graphics2D g2d, int fila, int col, int x, int y, int tamanoCelda) {
        String texto = fila + "," + col;
        g2d.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, Math.max(8, tamanoCelda / 7)));
        FontMetrics medidas = g2d.getFontMetrics();
        int xTexto = x + 3;
        int yTexto = y + 2 + medidas.getAscent();
        g2d.setColor(new Color(0, 0, 0, 120));
        g2d.drawString(texto, xTexto + 1, yTexto + 1);
        g2d.setColor(new Color(255, 255, 255, 200));
        g2d.drawString(texto, xTexto, yTexto);
    }

    /**
     * Contorno que indica que el barco tiene la habilidad activa en este turno: un halo celeste
     * ancho y semitransparente, y encima un borde celeste fuerte.
     */
    private static void dibujarContornoHabilidad(Graphics2D g2d, int x, int y, int tamanoCelda) {
        int arco = tamanoCelda / 4;
        int margen = Math.max(7, tamanoCelda / 9);
        int lado = tamanoCelda - 2 * margen;
        g2d.setColor(new Color(0, 230, 255, 110));
        g2d.setStroke(new BasicStroke(Math.max(7f, tamanoCelda / 8f)));
        g2d.drawRoundRect(x + margen, y + margen, lado, lado, arco, arco);
        g2d.setColor(new Color(0, 240, 255));
        g2d.setStroke(new BasicStroke(Math.max(3f, tamanoCelda / 18f)));
        g2d.drawRoundRect(x + margen, y + margen, lado, lado, arco, arco);
    }

    /** Ancho del casco, en proporción a la celda: cuanto más grande el barco, más ancho. */
    private static double proporcionAncho(char simbolo) {
        switch (simbolo) {
            case 'A':
                return 0.6;
            case 'C':
                return 0.5;
            default:
                return 0.42;
        }
    }

    private static int cantidadTorretas(char simbolo) {
        switch (simbolo) {
            case 'A':
                return 3;
            case 'C':
                return 2;
            default:
                return 1;
        }
    }

    /** Verde con mucha vida, amarillo a la mitad y rojo cuando queda poca. */
    private static Color colorVida(int vida, int vidaMaxima) {
        double parte = vidaMaxima == 0 ? 0 : (double) vida / vidaMaxima;
        if (parte > 0.6) {
            return new Color(60, 200, 80);
        }
        if (parte > 0.3) {
            return new Color(240, 190, 40);
        }
        return new Color(230, 50, 40);
    }

    /** Mezcla dos colores: con {@code parte} 0 queda el primero y con 1 el segundo. */
    private static Color mezclar(Color a, Color b, double parte) {
        return new Color(
                (int) (a.getRed() + (b.getRed() - a.getRed()) * parte),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * parte),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * parte));
    }

    /** Color que identifica a cada jugador (el mismo tono que su zona de despliegue). */
    private Color colorJugador(int numeroJugador) {
        if (numeroJugador == 1) {
            return new Color(230, 90, 50);  // Naranja: jugador 1
        }
        return new Color(170, 80, 220);     // Violeta: jugador 2
    }

    // Selección, casillas alcanzables y ruta

    /**
     * Resalta con un borde amarillo el barco seleccionado. Si no hay selección
     * (la partida devuelve null), no dibuja nada.
     */
    private void dibujarBarcoSeleccionado(Graphics2D g2d, int tamanoCelda, int inicioX, int inicioY) {
        EstadoBarco seleccionado = partidaTablero.getBarcoSeleccionado();
        if (seleccionado == null || seleccionado.posicion() == null) {
            return;
        }
        int x = inicioX + (seleccionado.posicion().getColumna() * tamanoCelda);
        int y = inicioY + (seleccionado.posicion().getFila() * tamanoCelda);
        g2d.setColor(Color.YELLOW);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawRect(x + 2, y + 2, tamanoCelda - 4, tamanoCelda - 4);
    }

    /**
     * Marca hasta dónde llega el disparo del barco elegido: un cuadrado de su rango alrededor
     * (las distancias se cuentan en casillas y la diagonal vale uno), con un tinte rojo muy suave y
     * un borde punteado. Las islas pueden tapar algunas casillas de adentro: eso lo decide la partida
     * al armar los objetivos válidos, que van marcados en rojo fuerte.
     */
    private void dibujarAlcanceDeDisparo(Graphics2D g2d, int tamanoCelda, int inicioX, int inicioY) {
        EstadoBarco seleccionado = partidaTablero.getBarcoSeleccionado();
        if (modoDespliegue || seleccionado == null || seleccionado.posicion() == null
                || seleccionado.cooldownDisparo() > 0 || !partidaTablero.puedeActuar(seleccionado.id())) {
            return;
        }
        int rango = seleccionado.rangoDisparo();
        int filaMin = Math.max(0, seleccionado.posicion().getFila() - rango);
        int filaMax = Math.min(FILAS - 1, seleccionado.posicion().getFila() + rango);
        int colMin = Math.max(0, seleccionado.posicion().getColumna() - rango);
        int colMax = Math.min(COLUMNAS - 1, seleccionado.posicion().getColumna() + rango);
        int x = inicioX + colMin * tamanoCelda;
        int y = inicioY + filaMin * tamanoCelda;
        int ancho = (colMax - colMin + 1) * tamanoCelda;
        int alto = (filaMax - filaMin + 1) * tamanoCelda;
        g2d.setColor(new Color(255, 60, 60, 30));
        g2d.fillRect(x, y, ancho, alto);
        g2d.setColor(new Color(255, 70, 70, 210));
        g2d.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {8f, 6f}, 0f));
        g2d.drawRect(x + 1, y + 1, ancho - 2, alto - 2);
    }

    /**
     * Sombrea las casillas a las que puede llegar el barco seleccionado este turno.
     * El cálculo lo hace la partida (con los puntos de movimiento y los obstáculos);
     * el panel solo las pinta.
     */
    private void dibujarCasillasAlcanzables(Graphics2D g2d, int tamanoCelda, int inicioX, int inicioY) {
        Set<Posicion> alcanzables = partidaTablero.getCasillasAlcanzables();
        g2d.setColor(new Color(255, 255, 255, 70)); // Blanco semitransparente
        for (Posicion pos : alcanzables) {
            int x = inicioX + (pos.getColumna() * tamanoCelda);
            int y = inicioY + (pos.getFila() * tamanoCelda);
            g2d.fillRect(x, y, tamanoCelda, tamanoCelda);
        }
    }

    /**
     * Dibuja la ruta planificada: cada paso en celeste con su número de orden (1, 2, 3...)
     * y, en el último paso, el costo total de la ruta en puntos de movimiento.
     */
    private void dibujarRuta(Graphics2D g2d, int tamanoCelda, int inicioX, int inicioY) {
        List<Posicion> ruta = partidaTablero.getRutaPlanificada();
        if (ruta.isEmpty()) {
            return;
        }
        g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(10, tamanoCelda / 3)));
        FontMetrics medidas = g2d.getFontMetrics();
        int numeroPaso = 1;
        for (Posicion pos : ruta) {
            int x = inicioX + (pos.getColumna() * tamanoCelda);
            int y = inicioY + (pos.getFila() * tamanoCelda);

            // Fondo celeste del paso
            g2d.setColor(new Color(135, 206, 250, 180));
            g2d.fillRect(x, y, tamanoCelda, tamanoCelda);

            // Número del paso, centrado
            String texto = String.valueOf(numeroPaso);
            int xTexto = x + (tamanoCelda - medidas.stringWidth(texto)) / 2;
            int yTexto = y + (tamanoCelda + medidas.getAscent()) / 2 - tamanoCelda / 10;
            g2d.setColor(new Color(10, 40, 80)); // Azul oscuro, se lee sobre el celeste
            g2d.drawString(texto, xTexto, yTexto);
            numeroPaso++;
        }

        // Costo total de la ruta, chico, abajo del último paso
        Posicion ultimo = ruta.get(ruta.size() - 1);
        int x = inicioX + (ultimo.getColumna() * tamanoCelda);
        int y = inicioY + (ultimo.getFila() * tamanoCelda);
        String costo = "costo " + partidaTablero.getCostoRuta();
        g2d.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, Math.max(9, tamanoCelda / 5)));
        FontMetrics medidasCosto = g2d.getFontMetrics();
        g2d.drawString(costo, x + (tamanoCelda - medidasCosto.stringWidth(costo)) / 2, y + tamanoCelda - 4);
    }

    // Objetivos válidos y disparo con clic

    /**
     * Marca con un borde rojo cada objetivo válido. La partida decide cuáles son
     * (visibles, en rango, con línea de visión y con el tirador listo para disparar);
     * si no hay barco seleccionado o no puede disparar, la lista viene vacía y no se marca nada.
     */
    private void dibujarObjetivosValidos(Graphics2D g2d, int tamanoCelda, int inicioX, int inicioY) {
        g2d.setColor(new Color(220, 30, 30)); // Rojo
        g2d.setStroke(new BasicStroke(3));
        for (EstadoBarco objetivo : partidaTablero.getObjetivosValidos()) {
            Posicion pos = objetivo.posicion();
            if (pos != null) {
                int x = inicioX + (pos.getColumna() * tamanoCelda);
                int y = inicioY + (pos.getFila() * tamanoCelda);
                g2d.drawRect(x + 2, y + 2, tamanoCelda - 4, tamanoCelda - 4);
            }
        }
    }

    /**
     * Si en la posición clickeada hay un objetivo válido, le dispara.
     * El panel no valida el disparo: llama a la partida y, si lo rechaza, muestra el mensaje.
     *
     * @return true si había un objetivo en esa casilla (el clic ya se usó para disparar)
     */
    private boolean dispararSiHayObjetivo(Posicion pos) {
        for (EstadoBarco objetivo : partidaTablero.getObjetivosValidos()) {
            if (pos.equals(objetivo.posicion())) {
                try {
                    partidaCombate.disparar(objetivo.id()); // la animación la arranca el aviso de la partida
                } catch (AccionInvalidaException ex) {
                    mostrarError(ex);
                }
                return true;
            }
        }
        return false;
    }

    // Selección y movimiento con clic

    /**
     * Si en la posición clickeada hay un barco propio, lo selecciona.
     *
     * @return true si había un barco propio en esa casilla (el clic ya se usó para seleccionar)
     */
    private boolean seleccionarSiEsPropio(Posicion pos) {
        for (EstadoBarco barco : partidaTablero.getBarcosPropios()) {
            if (pos.equals(barco.posicion())) {
                try {
                    partidaCombate.seleccionarBarco(barco.id());
                } catch (AccionInvalidaException ex) {
                    mostrarError(ex);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Con un barco seleccionado, agrega la casilla clickeada como siguiente paso de la ruta.
     * Si no hay barco seleccionado no hace nada. Las reglas (casilla vecina, obstáculos,
     * puntos de movimiento) las valida la partida; si el paso no vale, se muestra el mensaje.
     */
    private void agregarPasoSiHaySeleccion(Posicion pos) {
        if (partidaTablero.getBarcoSeleccionado() == null) {
            return;
        }
        try {
            partidaCombate.agregarPaso(pos);
        } catch (AccionInvalidaException ex) {
            mostrarError(ex);
        }
    }

    // Animación de los disparos y marcas de lo que le pegaron al jugador

    /**
     * Si la partida tiene un disparo nuevo (con clic o con el botón del panel), arranca su
     * animación. Se llama en el EDT cada vez que la partida avisa un cambio.
     */
    private void revisarDisparoNuevo() {
        Disparo ultimo = partidaTablero.getUltimoDisparo();
        if (ultimo != null && ultimo != disparoAnimado) {
            disparoAnimado = ultimo;
            inicioAnimacion = System.currentTimeMillis();
            temporizadorAnimacion.restart();
        }
    }

    /** Un cuadro de la animación: redibuja y, cuando terminó, frena el Timer. */
    private void avanzarAnimacion() {
        if (disparoAnimado == null || tiempoDeAnimacion() > duracion(disparoAnimado)) {
            temporizadorAnimacion.stop();
        }
        repaint();
    }

    private long tiempoDeAnimacion() {
        return System.currentTimeMillis() - inicioAnimacion;
    }

    private static int duracion(Disparo disparo) {
        return disparo.hundimiento() ? DURACION_HUNDIMIENTO : DURACION_DISPARO;
    }

    /**
     * Dibuja el cuadro actual de la animación del último disparo: primero la bala viaja del tirador
     * al blanco, después una explosión en el blanco y el daño que sube y se desvanece. Si lo hundió,
     * la explosión es más grande y aparece "¡Hundido!".
     */
    private void dibujarAnimacionDisparo(Graphics2D g2d, int tamanoCelda, int inicioX, int inicioY) {
        if (disparoAnimado == null) {
            return;
        }
        long t = tiempoDeAnimacion();
        int total = duracion(disparoAnimado);
        if (t > total) {
            return;
        }
        double ox = centro(inicioX, disparoAnimado.origen().getColumna(), tamanoCelda);
        double oy = centro(inicioY, disparoAnimado.origen().getFila(), tamanoCelda);
        double dx = centro(inicioX, disparoAnimado.destino().getColumna(), tamanoCelda);
        double dy = centro(inicioY, disparoAnimado.destino().getFila(), tamanoCelda);

        if (t < DURACION_VIAJE_BALA) {
            double avance = (double) t / DURACION_VIAJE_BALA;
            double bx = ox + (dx - ox) * avance;
            double by = oy + (dy - oy) * avance;
            g2d.setColor(new Color(255, 200, 60, 200));
            g2d.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.draw(new Line2D.Double(ox, oy, bx, by));
            g2d.setColor(Color.WHITE);
            double r = tamanoCelda * 0.09;
            g2d.fill(new Ellipse2D.Double(bx - r, by - r, r * 2, r * 2));
            return;
        }

        double avance = (double) (t - DURACION_VIAJE_BALA) / (total - DURACION_VIAJE_BALA);
        int opacidad = (int) (255 * (1 - avance));

        // Estela del tiro, cada vez más transparente
        g2d.setColor(new Color(255, 200, 60, opacidad / 3));
        g2d.setStroke(new BasicStroke(2f));
        g2d.draw(new Line2D.Double(ox, oy, dx, dy));

        // Explosión que crece y se apaga
        double radioMax = tamanoCelda * (disparoAnimado.hundimiento() ? 0.85 : 0.55);
        double radio = tamanoCelda * 0.15 + (radioMax - tamanoCelda * 0.15) * Math.min(1, avance * 1.6);
        g2d.setColor(new Color(255, 120, 0, Math.max(0, opacidad * 2 / 3)));
        g2d.fill(new Ellipse2D.Double(dx - radio, dy - radio, radio * 2, radio * 2));
        g2d.setColor(new Color(255, 240, 120, Math.max(0, opacidad)));
        g2d.fill(new Ellipse2D.Double(dx - radio / 2, dy - radio / 2, radio, radio));

        // Daño que sube
        String danio = "-" + disparoAnimado.danio();
        g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(14, tamanoCelda / 2)));
        FontMetrics medidas = g2d.getFontMetrics();
        int tx = (int) (dx - medidas.stringWidth(danio) / 2.0);
        int ty = (int) (dy - tamanoCelda * 0.2 - tamanoCelda * 0.6 * avance);
        dibujarTextoConBorde(g2d, danio, tx, ty, new Color(255, 70, 50, Math.max(0, opacidad)));

        if (disparoAnimado.hundimiento()) {
            String hundido = "¡Hundido!";
            g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(13, tamanoCelda / 3)));
            FontMetrics m = g2d.getFontMetrics();
            dibujarTextoConBorde(g2d, hundido, (int) (dx - m.stringWidth(hundido) / 2.0),
                    (int) (dy + tamanoCelda * 0.45), new Color(255, 230, 0, Math.max(0, opacidad)));
        }
    }

    /**
     * Marca los disparos que recibió el jugador en el turno del rival: una línea punteada roja desde
     * donde vino el tiro, una mira en ese lugar y, sobre el barco que recibió, un globo con el daño
     * (o "hundido" si lo hundieron). Quedan durante todo el turno.
     */
    private void dibujarDisparosRecibidos(Graphics2D g2d, int tamanoCelda, int inicioX, int inicioY) {
        if (modoDespliegue) {
            return;
        }
        for (Disparo disparo : partidaTablero.getDisparosRecibidos()) {
            double ox = centro(inicioX, disparo.origen().getColumna(), tamanoCelda);
            double oy = centro(inicioY, disparo.origen().getFila(), tamanoCelda);
            double dx = centro(inicioX, disparo.destino().getColumna(), tamanoCelda);
            double dy = centro(inicioY, disparo.destino().getFila(), tamanoCelda);

            g2d.setColor(new Color(255, 50, 50, 160));
            g2d.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, new float[] {6f, 5f}, 0f));
            g2d.draw(new Line2D.Double(ox, oy, dx, dy));

            // Mira en el lugar desde donde dispararon
            double r = tamanoCelda * 0.18;
            g2d.setStroke(new BasicStroke(2f));
            g2d.draw(new Ellipse2D.Double(ox - r, oy - r, r * 2, r * 2));
            g2d.draw(new Line2D.Double(ox - r * 1.4, oy, ox + r * 1.4, oy));
            g2d.draw(new Line2D.Double(ox, oy - r * 1.4, ox, oy + r * 1.4));

            // Globo con el daño, abajo al centro de la casilla que recibió (arriba de la barra de vida)
            String texto = disparo.hundimiento() ? "hundido" : "-" + disparo.danio();
            g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(10, tamanoCelda / 5)));
            FontMetrics medidas = g2d.getFontMetrics();
            int ancho = medidas.stringWidth(texto) + 8;
            int alto = medidas.getAscent() + 4;
            int bx = inicioX + disparo.destino().getColumna() * tamanoCelda + (tamanoCelda - ancho) / 2;
            int by = inicioY + (disparo.destino().getFila() + 1) * tamanoCelda - alto - Math.max(3, tamanoCelda / 12) - 5;
            g2d.setColor(new Color(200, 20, 20, 230));
            g2d.fillRoundRect(bx, by, ancho, alto, alto, alto);
            g2d.setColor(Color.WHITE);
            g2d.drawString(texto, bx + 4, by + medidas.getAscent() + 1);
        }
    }

    /** Coordenada en píxeles del centro de la fila o columna {@code indice}. */
    private static double centro(int inicio, int indice, int tamanoCelda) {
        return inicio + indice * tamanoCelda + tamanoCelda / 2.0;
    }

    /** Texto con un borde oscuro alrededor, para que se lea sobre cualquier fondo. */
    private static void dibujarTextoConBorde(Graphics2D g2d, String texto, int x, int y, Color color) {
        g2d.setColor(new Color(0, 0, 0, color.getAlpha()));
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx != 0 || dy != 0) {
                    g2d.drawString(texto, x + dx, y + dy);
                }
            }
        }
        g2d.setColor(color);
        g2d.drawString(texto, x, y);
    }

    // Atajos de teclado

    /**
     * Registra los atajos con key bindings (InputMap + ActionMap) en lugar de KeyListener:
     * así funcionan aunque el foco esté en otro componente de la ventana (por ejemplo, un botón).
     * Cada tecla se asocia a un nombre en el InputMap, y cada nombre a una acción en el ActionMap.
     */
    private void registrarAtajosDeTeclado() {
        InputMap teclas = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap acciones = getActionMap();

        teclas.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancelarSeleccion");
        teclas.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "deshacerPaso");
        teclas.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "confirmarMovimiento");

        acciones.put("cancelarSeleccion", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (modoDespliegue) {
                    return;
                }
                try {
                    partidaCombate.cancelarSeleccion();
                } catch (AccionInvalidaException ex) {
                    mostrarError(ex);
                }
            }
        });
        acciones.put("deshacerPaso", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (modoDespliegue) {
                    return;
                }
                try {
                    partidaCombate.deshacerPaso();
                } catch (AccionInvalidaException ex) {
                    mostrarError(ex);
                }
            }
        });
        acciones.put("confirmarMovimiento", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (modoDespliegue) {
                    return;
                }
                try {
                    partidaCombate.confirmarMovimiento();
                } catch (AccionInvalidaException ex) {
                    mostrarError(ex);
                }
            }
        });
    }

    /**
     * Muestra en un diálogo el mensaje de una acción rechazada por la partida.
     * El panel no decide si la acción es válida: la intenta y, si la partida la rechaza, avisa.
     */
    private void mostrarError(AccionInvalidaException ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Acción no válida", JOptionPane.WARNING_MESSAGE);
    }

    // Tooltip de barcos

    /**
     * Swing lo llama cuando el mouse se queda quieto sobre el panel. Si hay un barco
     * en esa casilla devuelve el texto con sus atributos; si no, null y no se muestra nada.
     *
     * @param e evento del mouse con la posición del cursor
     * @return texto del tooltip, o null si no hay barco bajo el mouse
     */
    @Override
    public String getToolTipText(MouseEvent e) {
        Posicion pos = obtenerPosicionDesdeCoordenadas(e.getX(), e.getY());
        if (pos == null) {
            return null;
        }
        EstadoBarco barco = buscarBarcoEn(pos);
        if (barco == null) {
            return null;
        }
        return armarTextoTooltip(barco);
    }

    /**
     * Busca un barco en la posición: primero entre los propios y después entre los
     * enemigos visibles. Devuelve null si no hay ninguno.
     */
    private EstadoBarco buscarBarcoEn(Posicion pos) {
        for (EstadoBarco barco : partidaTablero.getBarcosPropios()) {
            if (pos.equals(barco.posicion())) {
                return barco;
            }
        }
        for (EstadoBarco barco : partidaTablero.getBarcosEnemigosVisibles()) {
            if (pos.equals(barco.posicion())) {
                return barco;
            }
        }
        return null;
    }

    /**
     * Arma el texto del tooltip en HTML (Swing lo acepta para poder usar varias líneas).
     * De los barcos enemigos no se muestran los cooldowns ni las casillas restantes del turno
     * (decisión de diseño: es información del rival).
     */
    private String armarTextoTooltip(EstadoBarco barco) {
        String texto = "<html><b>" + nombreTipo(barco.simbolo()) + "</b>";
        if (!barco.esPropio()) {
            texto += " (enemigo)";
        }
        if (barco.posicion() != null) {
            texto += " " + barco.posicion();
        }
        texto += "<br>Vida: " + barco.vida() + " / " + barco.vidaMaxima()
                + " · Daño: " + barco.danio();
        texto += "<br>Rango de disparo: " + barco.rangoDisparo() + " · Visión: " + barco.vision();
        String unidadCosto = barco.costoMovimiento() == 1 ? "pto/casilla" : "ptos/casilla";
        if (barco.esPropio()) {
            texto += "<br>Costo de movimiento: " + barco.costoMovimiento() + " " + unidadCosto
                    + " · Casillas este turno: " + barco.casillasRestantes() + " / " + barco.movimientoMaxPorTurno();
        } else {
            texto += "<br>Costo de movimiento: " + barco.costoMovimiento() + " " + unidadCosto
                    + " · Movimiento máx.: " + barco.movimientoMaxPorTurno() + " casillas/turno";
        }
        if (barco.simbolo() == 'A') {
            texto += "<br>Cadencia: 1 turno de espera después de disparar";
        }
        texto += "<br><b>Habilidad:</b> " + descripcionHabilidad(barco.simbolo()) + " (espera: 3 turnos)";
        if (barco.esPropio()) {
            texto += "<br>" + estadoHabilidad(barco);
            if (barco.cooldownDisparo() > 0) {
                texto += "<br>Recargando el disparo: " + barco.cooldownDisparo() + " turno(s)";
            }
        }
        return texto + "</html>";
    }

    /** Qué hace la habilidad de cada tipo de barco, para el tooltip. */
    static String descripcionHabilidad(char simbolo) {
        switch (simbolo) {
            case 'A':
                return "su rango de disparo sube a 6 en este turno.";
            case 'C':
                return "se repara 2 de vida.";
            case 'D':
                return "hace daño doble (6) en este turno.";
            default:
                return "-";
        }
    }

    private static String estadoHabilidad(EstadoBarco barco) {
        if (barco.habilidadActiva()) {
            return "Habilidad activa este turno.";
        }
        if (barco.cooldownHabilidad() > 0) {
            return "Habilidad en espera: " + barco.cooldownHabilidad() + " turno(s).";
        }
        return "Habilidad lista: tocá Usar habilidad (antes de disparar).";
    }

    /** Traduce el símbolo del barco al nombre de su tipo. */
    static String nombreTipo(char simbolo) {
        switch (simbolo) {
            case 'A':
                return "Acorazado";
            case 'C':
                return "Crucero";
            case 'D':
                return "Destructor";
            default:
                return "Barco";
        }
    }

    // Cálculos de la grilla: los usan paintComponent y obtenerPosicionDesdeCoordenadas, así siempre coinciden

    /** Tamaño de cada celda para que la grilla entre entera en el panel. */
    private int calcularTamanoCelda() {
        return Math.min(getWidth() / COLUMNAS, getHeight() / FILAS);
    }

    /** Margen izquierdo para que la grilla quede centrada. */
    private int calcularInicioX(int tamanoCelda) {
        return (getWidth() - (tamanoCelda * COLUMNAS)) / 2;
    }

    /** Margen superior para que la grilla quede centrada. */
    private int calcularInicioY(int tamanoCelda) {
        return (getHeight() - (tamanoCelda * FILAS)) / 2;
    }

    /**
     * Traduce las coordenadas de píxeles de la pantalla a una Posicion (fila y columna de 0 a 9).
     * Devuelve null si el punto cae fuera del tablero.
     */
    private Posicion obtenerPosicionDesdeCoordenadas(int pixelX, int pixelY) {
        int tamanoCelda = calcularTamanoCelda();
        if (tamanoCelda <= 0) return null;

        int x = pixelX - calcularInicioX(tamanoCelda);
        int y = pixelY - calcularInicioY(tamanoCelda);

        // Los negativos se descartan antes de dividir: la división entera redondea hacia el cero,
        // así que -20 / 40 da 0 y un clic a la izquierda del tablero caería en la columna 0.
        if (x < 0 || y < 0) return null;

        int col = x / tamanoCelda;
        int fila = y / tamanoCelda;

        if (fila < FILAS && col < COLUMNAS) {
            return new Posicion(fila, col);
        }
        return null;
    }

    // Implementación de ObservadorPartida

    /**
     * La partida avisa que cambió algo (un movimiento, un disparo, una selección): se redibuja el tablero.
     */
    @Override
    public void onEstadoCambiado() {
        repintarEnEdt();
    }

    /**
     * La partida cambió de fase (por ejemplo, del despliegue al juego): se redibuja el tablero.
     *
     * @param fasePartida fase nueva.
     */
    @Override
    public void onFaseCambiada(FasePartida fasePartida) {
        repintarEnEdt();
    }

    /**
     * Empezó el turno de otro jugador: se redibuja porque cambia lo que se ve del tablero.
     *
     * @param nombreJugador nombre del jugador que empieza el turno.
     */
    @Override
    public void onTurnoIniciado(String nombreJugador) {
        repintarEnEdt();
    }

    /**
     * La partida terminó: se redibuja para mostrar el estado final.
     *
     * @param resultadoPartida resultado de la partida.
     */
    @Override
    public void onPartidaTerminada(ResultadoPartida resultadoPartida) {
        repintarEnEdt();
    }

    /**
     * Pide el repintado en el hilo de eventos de Swing (EDT). Los componentes Swing solo
     * se deben tocar desde ese hilo, y la partida podría avisar desde otro (por ejemplo,
     * después de cargar una partida en un Thread aparte).
     */
    private void repintarEnEdt() {
        SwingUtilities.invokeLater(() -> {
            revisarDisparoNuevo();
            repaint();
        });
    }

    // Implementación de MouseListener

    @Override
    public void mouseClicked(MouseEvent e) {
        Posicion posClicked = obtenerPosicionDesdeCoordenadas(e.getX(), e.getY());
        if (posClicked == null) {
            return;
        }
        // Si quien armó el panel definió qué hacer con un clic (por ejemplo, la pantalla de
        // despliegue para ubicar barcos), se usa eso y nada más
        if (alClickearCasilla != null) {
            alClickearCasilla.accept(posClicked);
            return;
        }
        // Si no, el clic es de la fase de juego. Se prueba en orden: disparar a un objetivo,
        // seleccionar un barco propio o agregar un paso. Cada método devuelve true si usó el clic,
        // así no se hacen dos acciones con el mismo clic.
        if (dispararSiHayObjetivo(posClicked)) {
            return;
        }
        if (seleccionarSiEsPropio(posClicked)) {
            return;
        }
        agregarPasoSiHaySeleccion(posClicked);
    }

    @Override
    public void mousePressed(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {
        casillaHover = null;
        repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {}

    @Override
    public void mouseMoved(MouseEvent e) {
        Posicion nuevaPos = obtenerPosicionDesdeCoordenadas(e.getX(), e.getY());
        if (casillaHover != nuevaPos && (casillaHover == null || !casillaHover.equals(nuevaPos))) {
            casillaHover = nuevaPos;
            repaint();
        }
    }
}