package batallanaval.vista;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.Serial;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import batallanaval.controlador.EstadoBarco;
import batallanaval.controlador.EstadoJugador;
import batallanaval.controlador.FasePartida;
import batallanaval.controlador.ObservadorPartida;
import batallanaval.controlador.Partida;
import batallanaval.controlador.PartidaConsulta;
import batallanaval.controlador.ResultadoPartida;
import batallanaval.excepciones.AccionInvalidaException;
import batallanaval.modelo.Posicion;

/**
 * HUD de la pantalla de juego, a la derecha del tablero. Muestra:
 * <ul>
 *   <li>de quién es el turno y en qué ronda va la partida,</li>
 *   <li>los puntos de movimiento que le quedan a la flota del jugador activo,</li>
 *   <li>los puntos de dominación de los dos jugadores,</li>
 *   <li>la ficha del barco seleccionado (tipo, vida, casillas, cooldowns y habilidad),</li>
 *   <li>los botones del turno: Confirmar movimiento, Deshacer paso, Usar habilidad,
 *       Disparar (con la lista de objetivos válidos) y Terminar turno.</li>
 * </ul>
 * <p>
 * <b>El panel no decide reglas.</b> Todo lo que muestra se lo pregunta a la {@link Partida},
 * que le devuelve records inmutables ({@link EstadoJugador}, {@link EstadoBarco}). Los botones
 * se habilitan con lo que dice ese estado (por ejemplo, Disparar solo si la partida devuelve
 * algún objetivo válido) y, cuando se tocan, solo llaman al método de la partida. Si la
 * partida rechaza la acción, el mensaje de la excepción se le pasa a {@code alError}, que
 * quien arma el panel conecta con la barra de estado de la ventana.
 * <p>
 * <b>Dos formas de refrescarse.</b> Hereda de {@link PanelConHilo}, así que tiene su propio hilo
 * ({@link #iniciar()} / {@link #detener()}): cada {@value #INTERVALO_MS} ms se ejecuta
 * {@link #actualizar()} en el EDT y refresca el <i>marcador</i> (turno, ronda, puntos de
 * movimiento y dominación de ambos jugadores), leyendo solo de {@link PartidaConsulta}.
 * Además implementa {@link ObservadorPartida}: cuando la partida avisa un cambio (selección,
 * ruta, disparo...) se vuelve a leer todo, incluida la ficha del barco y los botones, con
 * {@link #refrescarTodo()}. La ficha y la lista de objetivos no se tocan en cada vuelta del
 * hilo para no pisar la lista desplegada mientras el jugador elige a quién disparar.
 */
public final class PanelLateral extends PanelConHilo implements ObservadorPartida {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Milisegundos entre una actualización del marcador y la siguiente. */
    static final int INTERVALO_MS = 500;

    private static final int ANCHO = 270;
    private static final String SIN_DATO = "-";

    // Usa tres de las cuatro interfaces (consulta, tablero y combate): por eso recibe la Partida entera.
    private final transient Partida partida;
    // El marcador solo lee estado general: lo pide por la interfaz chica (ISP), no por la Partida entera.
    private final transient PartidaConsulta consulta;
    private final transient Consumer<String> alError;

    // Encabezado
    private final JLabel etiquetaTurno = crearEtiqueta("etiquetaTurno");
    private final JLabel etiquetaRonda = crearEtiqueta("etiquetaRonda");
    private final JLabel etiquetaPuntosMovimiento = crearEtiqueta("etiquetaPuntosMovimiento");
    private final JLabel etiquetaDominacionActivo = crearEtiqueta("etiquetaDominacionActivo");
    private final JLabel etiquetaDominacionRival = crearEtiqueta("etiquetaDominacionRival");

    // Ficha del barco seleccionado
    private final JLabel etiquetaTipo = crearEtiqueta("etiquetaTipo");
    private final JLabel etiquetaVida = crearEtiqueta("etiquetaVida");
    private final JLabel etiquetaCasillas = crearEtiqueta("etiquetaCasillas");
    private final JLabel etiquetaCooldownDisparo = crearEtiqueta("etiquetaCooldownDisparo");
    private final JLabel etiquetaHabilidad = crearEtiqueta("etiquetaHabilidad");
    private final JLabel etiquetaRuta = crearEtiqueta("etiquetaRuta");

    // Acciones
    private final JButton botonConfirmarMovimiento = new JButton("Confirmar movimiento");
    private final JButton botonDeshacerPaso = new JButton("Deshacer paso");
    private final JButton botonUsarHabilidad = new JButton("Usar habilidad");
    private final DefaultComboBoxModel<OpcionObjetivo> modeloObjetivos = new DefaultComboBoxModel<>();
    private final JComboBox<OpcionObjetivo> comboObjetivos = new JComboBox<>(modeloObjetivos);
    private final JButton botonDisparar = new JButton("Disparar");
    private final JButton botonTerminarTurno = new JButton("Terminar turno");

    /**
     * Crea el panel y lo deja mostrando el estado actual de la partida. No se registra solo como
     * observador: lo hace quien lo crea (o {@link PanelJuego}, que le reenvía los avisos).
     *
     * @param partida partida de la que se lee el estado y a la que se le piden las acciones
     * @param alError qué hacer con el mensaje cuando la partida rechaza una acción
     *                (en la aplicación, {@code ventana::mostrarError})
     * @throws NullPointerException si algún argumento es {@code null}
     */
    public PanelLateral(Partida partida, Consumer<String> alError) {
        this.partida = Objects.requireNonNull(partida, "partida");
        this.consulta = partida;
        this.alError = Objects.requireNonNull(alError, "alError");

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setPreferredSize(new Dimension(ANCHO, 0));

        armarEncabezado();
        armarFichaBarco();
        armarBotones();
        conectarBotones();

        refrescarTodo();
    }

    /**
     * Paso del hilo propio: refresca el marcador (turno, ronda, puntos de movimiento y
     * dominación de ambos jugadores). Lo ejecuta el EDT cada {@value #INTERVALO_MS} ms.
     * El panel no calcula nada: muestra lo que le devuelve la {@link PartidaConsulta}.
     */
    @Override
    protected void actualizar() {
        actualizarMarcador();
    }

    /** @return {@value #INTERVALO_MS}: el marcador se refresca dos veces por segundo */
    @Override
    protected int getIntervaloMs() {
        return INTERVALO_MS;
    }

    /**
     * Vuelve a leer todo el estado de la partida y refresca el marcador, la ficha del barco y
     * los botones. Se tiene que llamar desde el EDT (los avisos de la partida ya pasan por
     * {@code invokeLater} antes de llegar acá).
     */
    void refrescarTodo() {
        boolean enJuego = partida.getFase() == FasePartida.EN_JUEGO && !partida.estaTerminada();
        actualizarMarcador();

        if (!enJuego) {
            // Fuera del combate no se pregunta nada del turno: se muestra la ficha vacía y todo bloqueado.
            mostrarFichaVacia();
            etiquetaRuta.setText(" ");
            modeloObjetivos.removeAllElements();
            deshabilitarTodo();
            return;
        }

        EstadoBarco seleccionado = partida.getBarcoSeleccionado();
        List<Posicion> ruta = partida.getRutaPlanificada();
        List<EstadoBarco> objetivos = partida.getObjetivosValidos();

        actualizarFicha(seleccionado);
        actualizarRuta(ruta);
        actualizarObjetivos(objetivos);

        boolean hayBarco = seleccionado != null;
        boolean hayRuta = hayBarco && !ruta.isEmpty();
        botonConfirmarMovimiento.setEnabled(hayRuta);
        botonDeshacerPaso.setEnabled(hayRuta);
        botonUsarHabilidad.setEnabled(hayBarco && habilidadLista(seleccionado));
        comboObjetivos.setEnabled(!objetivos.isEmpty());
        botonDisparar.setEnabled(!objetivos.isEmpty());
        botonTerminarTurno.setEnabled(true);
    }

    // Avisos de la partida (ObservadorPartida). Pueden llegar desde otro hilo: se pasa al EDT.

    /** La partida cambió (selección, ruta, disparo...): se refresca el panel. */
    @Override
    public void onEstadoCambiado() {
        actualizarEnEdt();
    }

    /**
     * Cambió la fase de la partida: se refresca (por ejemplo, al terminar se bloquean los botones).
     *
     * @param fasePartida fase nueva
     */
    @Override
    public void onFaseCambiada(FasePartida fasePartida) {
        actualizarEnEdt();
    }

    /**
     * Empezó el turno de otro jugador: cambian el nombre, los puntos y la ficha.
     *
     * @param nombreJugador jugador que empieza el turno
     */
    @Override
    public void onTurnoIniciado(String nombreJugador) {
        actualizarEnEdt();
    }

    /**
     * La partida terminó: se refresca para que queden todos los botones deshabilitados.
     *
     * @param resultadoPartida cómo terminó
     */
    @Override
    public void onPartidaTerminada(ResultadoPartida resultadoPartida) {
        actualizarEnEdt();
    }

    private void actualizarEnEdt() {
        SwingUtilities.invokeLater(this::refrescarTodo);
    }

    // Armado de la interfaz (se hace una sola vez, en el constructor)

    private void armarEncabezado() {
        etiquetaTurno.setFont(etiquetaTurno.getFont().deriveFont(Font.BOLD, 18f));
        agregarAlineado(etiquetaTurno);
        agregarAlineado(etiquetaRonda);
        agregarAlineado(etiquetaPuntosMovimiento);

        JPanel dominacion = crearSeccion("Dominación");
        dominacion.add(etiquetaDominacionActivo);
        dominacion.add(etiquetaDominacionRival);
        agregarAlineado(dominacion);
    }

    private void armarFichaBarco() {
        JPanel ficha = crearSeccion("Barco seleccionado");
        ficha.add(etiquetaTipo);
        ficha.add(etiquetaVida);
        ficha.add(etiquetaCasillas);
        ficha.add(etiquetaCooldownDisparo);
        ficha.add(etiquetaHabilidad);
        ficha.add(etiquetaRuta);
        agregarAlineado(ficha);
    }

    private void armarBotones() {
        JPanel acciones = new JPanel(new GridLayout(0, 1, 0, 6));
        acciones.setBorder(BorderFactory.createTitledBorder("Acciones"));
        comboObjetivos.setName("comboObjetivos");
        acciones.add(botonConfirmarMovimiento);
        acciones.add(botonDeshacerPaso);
        acciones.add(botonUsarHabilidad);
        acciones.add(comboObjetivos);
        acciones.add(botonDisparar);
        acciones.add(botonTerminarTurno);
        agregarAlineado(acciones);
        add(Box.createVerticalGlue());
    }

    /**
     * Cada botón solo le pide la acción a la partida. Si la partida avisa el cambio,
     * el panel se refresca por {@link #onEstadoCambiado()}; si la rechaza, se muestra el motivo.
     */
    private void conectarBotones() {
        botonConfirmarMovimiento.addActionListener(e -> ejecutar(partida::confirmarMovimiento));
        botonDeshacerPaso.addActionListener(e -> ejecutar(partida::deshacerPaso));
        botonUsarHabilidad.addActionListener(e -> ejecutar(partida::usarHabilidad));
        botonDisparar.addActionListener(e -> dispararAlObjetivoElegido());
        botonTerminarTurno.addActionListener(e -> ejecutar(partida::terminarTurno));
    }

    private void dispararAlObjetivoElegido() {
        OpcionObjetivo elegido = (OpcionObjetivo) comboObjetivos.getSelectedItem();
        if (elegido == null) {
            alError.accept("Elegí un objetivo antes de disparar.");
            return;
        }
        ejecutar(() -> partida.disparar(elegido.id()));
    }

    /**
     * Ejecuta una acción de la partida. Las excepciones de reglas son unchecked: si llega una
     * (la partida rechazó la acción), no se corta el juego, se muestra su mensaje.
     */
    private void ejecutar(Runnable accion) {
        try {
            accion.run();
        } catch (AccionInvalidaException e) {
            alError.accept(e.getMessage());
        }
    }

    // Refresco de cada parte

    private void actualizarMarcador() {
        EstadoJugador activo = consulta.getJugadorActivo();
        EstadoJugador rival = consulta.getRival();
        etiquetaRonda.setText("Ronda: " + consulta.getRonda());
        if (activo == null || rival == null) {
            etiquetaTurno.setText("Turno de " + SIN_DATO);
            etiquetaPuntosMovimiento.setText("Puntos de movimiento: " + SIN_DATO);
            etiquetaDominacionActivo.setText(SIN_DATO);
            etiquetaDominacionRival.setText(SIN_DATO);
            return;
        }
        etiquetaTurno.setText("Turno de " + activo.nombre());
        etiquetaTurno.setForeground(colorJugador(activo.numero()));
        etiquetaPuntosMovimiento.setText("Puntos de movimiento: " + activo.puntosMovimiento());
        etiquetaDominacionActivo.setText(activo.nombre() + ": " + activo.puntosDominacion());
        etiquetaDominacionRival.setText(rival.nombre() + ": " + rival.puntosDominacion());
    }

    private void actualizarFicha(EstadoBarco barco) {
        if (barco == null) {
            mostrarFichaVacia();
            return;
        }
        etiquetaTipo.setText(nombreTipo(barco.simbolo()) + " " + barco.posicion());
        etiquetaVida.setText("Vida: " + barco.vida() + " / " + barco.vidaMaxima());
        etiquetaCasillas.setText("Casillas restantes: " + barco.casillasRestantes());
        etiquetaCooldownDisparo.setText("Cooldown de disparo: " + barco.cooldownDisparo());
        etiquetaHabilidad.setText("Habilidad: " + textoHabilidad(barco));
    }

    private void mostrarFichaVacia() {
        etiquetaTipo.setText("Ningún barco seleccionado");
        etiquetaVida.setText(" ");
        etiquetaCasillas.setText(" ");
        etiquetaCooldownDisparo.setText(" ");
        etiquetaHabilidad.setText(" ");
    }

    private void actualizarRuta(List<Posicion> ruta) {
        if (ruta.isEmpty()) {
            etiquetaRuta.setText(" ");
        } else {
            etiquetaRuta.setText("Ruta: " + ruta.size() + " pasos, costo " + partida.getCostoRuta());
        }
    }

    /**
     * Rellena la lista de objetivos con los que devuelve la partida (ella decide qué es visible
     * y qué está en rango). Si el objetivo que estaba elegido sigue en la lista, queda elegido.
     */
    private void actualizarObjetivos(List<EstadoBarco> objetivos) {
        OpcionObjetivo anterior = (OpcionObjetivo) comboObjetivos.getSelectedItem();
        modeloObjetivos.removeAllElements();
        for (EstadoBarco objetivo : objetivos) {
            OpcionObjetivo opcion = new OpcionObjetivo(objetivo.id(), textoObjetivo(objetivo));
            modeloObjetivos.addElement(opcion);
            if (anterior != null && anterior.id() == opcion.id()) {
                modeloObjetivos.setSelectedItem(opcion);
            }
        }
    }

    private void deshabilitarTodo() {
        botonConfirmarMovimiento.setEnabled(false);
        botonDeshacerPaso.setEnabled(false);
        botonUsarHabilidad.setEnabled(false);
        comboObjetivos.setEnabled(false);
        botonDisparar.setEnabled(false);
        botonTerminarTurno.setEnabled(false);
    }

    // Textos

    /**
     * La habilidad se puede tocar si no está en espera y no se activó este turno.
     * Es solo para habilitar el botón: si igual llega una acción inválida, la partida la rechaza.
     */
    private static boolean habilidadLista(EstadoBarco barco) {
        return barco.cooldownHabilidad() == 0 && !barco.habilidadActiva();
    }

    private static String textoHabilidad(EstadoBarco barco) {
        if (barco.habilidadActiva()) {
            return "activa este turno";
        }
        if (barco.cooldownHabilidad() > 0) {
            return "en espera (" + barco.cooldownHabilidad() + " turnos)";
        }
        return "disponible";
    }

    private static String textoObjetivo(EstadoBarco objetivo) {
        return nombreTipo(objetivo.simbolo()) + " " + objetivo.posicion()
                + " - vida " + objetivo.vida() + "/" + objetivo.vidaMaxima();
    }

    /** Traduce el símbolo del barco al nombre de su tipo. */
    private static String nombreTipo(char simbolo) {
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

    /** Mismo color por jugador que usa el tablero (naranja J1, violeta J2). */
    private static Color colorJugador(int numeroJugador) {
        if (numeroJugador == 1) {
            return new Color(230, 90, 50);
        }
        return new Color(170, 80, 220);
    }

    // Ayudas para armar componentes

    private static JLabel crearEtiqueta(String nombre) {
        JLabel etiqueta = new JLabel(" ");
        etiqueta.setName(nombre);
        return etiqueta;
    }

    private static JPanel crearSeccion(String titulo) {
        JPanel seccion = new JPanel(new GridLayout(0, 1, 0, 2));
        seccion.setBorder(BorderFactory.createTitledBorder(titulo));
        return seccion;
    }

    /** Agrega el componente alineado a la izquierda, con un poco de aire abajo (BoxLayout). */
    private void agregarAlineado(JComponent componente) {
        componente.setAlignmentX(Component.LEFT_ALIGNMENT);
        componente.setMaximumSize(new Dimension(Integer.MAX_VALUE, componente.getPreferredSize().height));
        add(componente);
        add(Box.createVerticalStrut(8));
    }

    /**
     * Una opción de la lista de objetivos: el id del barco enemigo (lo que necesita
     * {@code disparar(int)}) y el texto que ve el jugador. Es inmutable (record).
     */
    private record OpcionObjetivo(int id, String texto) {
        @Override
        public String toString() {
            return texto;
        }
    }
}
