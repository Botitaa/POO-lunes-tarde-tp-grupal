package batallanaval.vista;

import batallanaval.controlador.EstadoBarco;
import batallanaval.controlador.FasePartida;
import batallanaval.controlador.ObservadorPartida;
import batallanaval.controlador.PartidaCombate;
import batallanaval.controlador.PartidaTablero;
import batallanaval.controlador.ResultadoPartida;
import batallanaval.modelo.Posicion;
import batallanaval.modelo.Casilla;
import batallanaval.excepciones.AccionInvalidaException;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
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
 * y dibuja la ruta planificada paso a paso con su costo.
 * Al pasar el mouse sobre un barco muestra un tooltip con sus atributos.
 * Atajos de teclado (key bindings): Esc cancela la selección, Backspace deshace el último paso
 * de la ruta y Enter confirma el movimiento.
 * Todo lo que dibuja se lo pregunta a PartidaTablero: el panel no conoce el Tablero.
 * Qué significa un clic lo decide quien arma el panel con {@link #setAlClickearCasilla(Consumer)}.
 * En el despliegue se activa {@link #setModoDespliegue(boolean)}: sin niebla y sin atajos de combate.
 * Implementa ObservadorPartida: quien crea el panel lo registra en la partida con
 * agregarObservador, y desde ahí el panel se redibuja ante cada cambio.
 */
public class PanelTablero extends JPanel implements MouseListener, MouseMotionListener, ObservadorPartida {

    private static final int FILAS = 10;
    private static final int COLUMNAS = 10;

    // Parte de la partida que necesita el panel para dibujar (ISP: no recibe la Partida entera)
    private final PartidaTablero partidaTablero;

    // Acciones del turno que disparan los atajos de teclado (ISP: solo las de combate)
    private final PartidaCombate partidaCombate;

    // Guarda la posición de la celda sobre la cual está el mouse actualmente
    private Posicion casillaHover = null;

    // Qué hacer cuando se hace clic en una casilla del tablero (null: el clic no hace nada)
    private transient Consumer<Posicion> alClickearCasilla = null;

    // En el despliegue no hay niebla (el jugador tiene que ver su zona) ni atajos de combate
    private boolean modoDespliegue = false;

    /**
     * Crea el panel del tablero.
     *
     * @param partidaTablero consultas de la partida de las que se leen las casillas y las zonas.
     * @param partidaCombate acciones del turno que llaman los atajos de teclado.
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
    }

    /**
     * Define qué pasa cuando el jugador hace clic en una casilla del tablero. El panel solo traduce
     * los píxeles a una {@link Posicion}; quien lo armó decide qué hacer con ella (en el despliegue,
     * ubicar un barco). Los clics fuera de la grilla no se informan.
     *
     * @param alClickearCasilla acción que recibe la casilla clickeada, o {@code null} para que los
     *                          clics no hagan nada
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
     * la ruta planificada, los barcos, el barco seleccionado, la zona de dominación
     * y el resaltado de la casilla bajo el mouse.
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
            }
        }

        // Casillas alcanzables y ruta planificada del barco seleccionado (debajo de los barcos)
        dibujarCasillasAlcanzables(g2d, tamanoCelda, inicioX, inicioY);
        dibujarRuta(g2d, tamanoCelda, inicioX, inicioY);

        // Los barcos se dibujan arriba de las casillas ya pintadas.
        // Los enemigos son solo los que la partida dice que el jugador ve (no se recalcula acá).
        dibujarBarcos(g2d, partidaTablero.getBarcosPropios(), tamanoCelda, inicioX, inicioY);
        dibujarBarcos(g2d, partidaTablero.getBarcosEnemigosVisibles(), tamanoCelda, inicioX, inicioY);

        // El barco seleccionado lleva un borde amarillo, arriba de su dibujo
        dibujarBarcoSeleccionado(g2d, tamanoCelda, inicioX, inicioY);

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
     * Dibuja un barco en su celda: un fondo redondeado con el color del jugador,
     * el símbolo con la vida (por ejemplo "C5") y abajo una barra de vida.
     */
    private void dibujarBarco(Graphics2D g2d, EstadoBarco barco, int x, int y, int tamanoCelda) {
        int margen = tamanoCelda / 8;
        int lado = tamanoCelda - 2 * margen;

        // Fondo con el color del jugador
        g2d.setColor(colorJugador(barco.numeroJugador()));
        g2d.fillRoundRect(x + margen, y + margen, lado, lado, margen, margen);

        // Texto: símbolo + vida, centrado en la celda
        String texto = "" + barco.simbolo() + barco.vida();
        g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(10, tamanoCelda / 3)));
        FontMetrics medidas = g2d.getFontMetrics();
        int xTexto = x + (tamanoCelda - medidas.stringWidth(texto)) / 2;
        int yTexto = y + (tamanoCelda + medidas.getAscent()) / 2 - margen;
        g2d.setColor(Color.WHITE);
        g2d.drawString(texto, xTexto, yTexto);

        // Barra de vida: fondo oscuro y encima la parte que le queda, proporcional a la vida
        int altoBarra = Math.max(3, tamanoCelda / 10);
        int xBarra = x + margen + 2;
        int yBarra = y + tamanoCelda - margen - altoBarra - 2;
        int anchoBarra = lado - 4;
        int anchoVida = anchoBarra * barco.vida() / barco.vidaMaxima();
        g2d.setColor(new Color(60, 60, 60));
        g2d.fillRect(xBarra, yBarra, anchoBarra, altoBarra);
        g2d.setColor(new Color(60, 200, 80)); // Verde
        g2d.fillRect(xBarra, yBarra, anchoVida, altoBarra);
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
     * De los barcos enemigos no se muestran los cooldowns (decisión de diseño: es información del rival).
     */
    private String armarTextoTooltip(EstadoBarco barco) {
        String texto = "<html><b>" + nombreTipo(barco.simbolo()) + "</b>";
        if (!barco.esPropio()) {
            texto += " (enemigo)";
        }
        texto += "<br>Vida: " + barco.vida() + " / " + barco.vidaMaxima();
        texto += "<br>Rango de disparo: " + barco.rangoDisparo();
        if (barco.esPropio()) {
            texto += "<br>Cooldown de disparo: " + barco.cooldownDisparo();
            texto += "<br>Cooldown de habilidad: " + barco.cooldownHabilidad();
        }
        return texto + "</html>";
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
        SwingUtilities.invokeLater(() -> repaint());
    }

    // Implementación de MouseListener

    @Override
    public void mouseClicked(MouseEvent e) {
        Posicion posClicked = obtenerPosicionDesdeCoordenadas(e.getX(), e.getY());
        if (posClicked != null && alClickearCasilla != null) {
            alClickearCasilla.accept(posClicked);
        }
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