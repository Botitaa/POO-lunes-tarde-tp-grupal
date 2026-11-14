package batallanaval.app;

import batallanaval.controlador.FasePartida;
import batallanaval.controlador.GestorPartida;
import batallanaval.controlador.ObservadorPartida;
import batallanaval.controlador.Partida;
import batallanaval.controlador.ResultadoPartida;
import batallanaval.excepciones.PersistenciaException;
import batallanaval.modelo.FabricaFlota;
import batallanaval.modelo.Jugador;
import batallanaval.modelo.Tablero;
import batallanaval.persistencia.CalculadoraRanking;
import batallanaval.persistencia.LectorMapas;
import batallanaval.persistencia.RepositorioEstadisticas;
import batallanaval.persistencia.RepositorioEstadisticasCsv;
import batallanaval.persistencia.RepositorioPartidas;
import batallanaval.persistencia.RepositorioPartidasArchivo;
import batallanaval.vista.AccionGuardarPartida;
import batallanaval.vista.BarraMenuJuego;
import batallanaval.vista.PanelBlackout;
import batallanaval.vista.PanelCargarPartida;
import batallanaval.vista.PanelEstadisticas;
import batallanaval.vista.PanelInicio;
import batallanaval.vista.PanelJuego;
import batallanaval.vista.PanelNuevaPartida;
import batallanaval.vista.PanelReglas;
import batallanaval.vista.PanelResumen;
import batallanaval.vista.Pantalla;
import batallanaval.vista.VentanaPrincipal;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Objects;

/**
 * Arma la aplicación y decide qué pantalla se ve en cada momento (patrón Controlador GRASP a nivel
 * aplicación). Es el único lugar que crea repositorios, partidas y pantallas: las pantallas reciben
 * por constructor lo que necesitan y avisan con un {@link Runnable} cuando se toca un botón.
 * <p>
 * Flujo de una partida (hot-seat):
 * <pre>
 * INICIO → NUEVA_PARTIDA → BLACKOUT → DESPLIEGUE (J1) → BLACKOUT → DESPLIEGUE (J2)
 *        → BLACKOUT → [RESUMEN] → JUEGO → BLACKOUT → [RESUMEN] → JUEGO → … → fin → INICIO
 * </pre>
 * Escucha a la partida como {@link ObservadorPartida}: cada {@code onTurnoIniciado} lleva al blackout
 * y {@code onPartidaTerminada} cierra la partida. Las pantallas que dependen de la partida (despliegue,
 * resumen y juego) se vuelven a crear con cada partida nueva o cargada, dentro de un contenedor fijo,
 * porque {@link VentanaPrincipal} registra una sola vez cada pantalla.
 * <p>
 * Todos los métodos se llaman desde el EDT.
 */
public final class ControladorAplicacion implements ObservadorPartida {

    private static final String MAPA = "estandar";
    private static final int PRIMER_ID_J1 = 1;
    private static final int PRIMER_ID_J2 = 7;

    private final VentanaPrincipal ventana;
    private final RepositorioPartidas repositorioPartidas;
    private final RepositorioEstadisticas repositorioEstadisticas;

    private final BarraMenuJuego barraMenu;
    private final AccionGuardarPartida accionGuardar;
    private final PanelBlackout panelBlackout;
    private final PanelNuevaPartida panelNuevaPartida;
    private final PanelCargarPartida panelCargar;
    private final PanelEstadisticas panelEstadisticas;

    // Contenedores fijos de las pantallas que se rearman con cada partida.
    private final JPanel contenedorDespliegue = new JPanel(new BorderLayout());
    private final JPanel contenedorResumen = new JPanel(new BorderLayout());
    private final JPanel contenedorJuego = new JPanel(new BorderLayout());

    private GestorPartida partida;
    private PanelJuego panelJuego;
    private PanelResumen panelResumen;
    private PanelDespliegueTemporal panelDespliegue;

    /** Arma la aplicación con la ventana y los repositorios reales (archivos en {@code datos/}). */
    public ControladorAplicacion() {
        this(new VentanaPrincipal(), new RepositorioPartidasArchivo(), new RepositorioEstadisticasCsv());
    }

    /**
     * Arma la aplicación con lo que recibe (inyección por constructor).
     *
     * @param ventana                 ventana única del juego
     * @param repositorioPartidas     donde se guardan y cargan las partidas
     * @param repositorioEstadisticas donde se registran los resultados para el ranking
     * @throws NullPointerException si algún argumento es {@code null}
     */
    ControladorAplicacion(VentanaPrincipal ventana, RepositorioPartidas repositorioPartidas,
                          RepositorioEstadisticas repositorioEstadisticas) {
        this.ventana = Objects.requireNonNull(ventana, "ventana");
        this.repositorioPartidas = Objects.requireNonNull(repositorioPartidas, "repositorioPartidas");
        this.repositorioEstadisticas = Objects.requireNonNull(repositorioEstadisticas, "repositorioEstadisticas");

        // Guardar corre en un hilo propio (TareaGuardar): mientras tanto se bloquean los botones del
        // blackout y se liberan al terminar, salga bien o mal.
        accionGuardar = new AccionGuardarPartida(repositorioPartidas, () -> partida,
                ventana::mostrarError, ventana::mostrarMensaje,
                this::bloquearBotonesDelBlackout, this::liberarBotonesDelBlackout);
        barraMenu = new BarraMenuJuego(accionGuardar, () -> ventana.mostrar(Pantalla.CARGAR),
                this::abandonar, () -> ventana.mostrar(Pantalla.REGLAS));
        panelBlackout = new PanelBlackout(this::alTocarListo, accionGuardar);
        // TEMPORAL (#73): los nombres van a salir de ConfiguracionJuego (los últimos que se usaron).
        panelNuevaPartida = new PanelNuevaPartida(ventana, null, null, this::comenzarPartidaNueva);
        panelCargar = new PanelCargarPartida(ventana, repositorioPartidas, this::alCargarPartida);
        panelEstadisticas = new PanelEstadisticas(ventana);
    }

    /** Registra las pantallas, instala el menú y muestra la ventana en el inicio. */
    public void iniciar() {
        ventana.agregarPantalla(Pantalla.INICIO, new PanelInicio(ventana));
        ventana.agregarPantalla(Pantalla.NUEVA_PARTIDA, panelNuevaPartida);
        ventana.agregarPantalla(Pantalla.CARGAR, panelCargar);
        ventana.agregarPantalla(Pantalla.ESTADISTICAS, panelEstadisticas);
        ventana.agregarPantalla(Pantalla.REGLAS, new PanelReglas(ventana));
        ventana.agregarPantalla(Pantalla.BLACKOUT, panelBlackout);
        ventana.agregarPantalla(Pantalla.DESPLIEGUE, contenedorDespliegue);
        ventana.agregarPantalla(Pantalla.RESUMEN, contenedorResumen);
        ventana.agregarPantalla(Pantalla.JUEGO, contenedorJuego);
        // TEMPORAL (#106): registrar Pantalla.FIN_PARTIDA con el PanelFinPartida de Guille.

        // PanelInicio navega solo; estas dos pantallas se recargan cada vez que se muestran.
        alMostrar(panelCargar, panelCargar::actualizarLista);
        alMostrar(panelEstadisticas, this::cargarRanking);

        ventana.setJMenuBar(barraMenu);
        ventana.mostrar(Pantalla.INICIO);
        ventana.setVisible(true);
    }

    // ---------- partida nueva, cargada o abandonada ----------

    private void comenzarPartidaNueva() {
        GestorPartida nueva = new GestorPartida(
                new Jugador(1, panelNuevaPartida.getNombreJugador1(), FabricaFlota.crearFlotaEstandar(PRIMER_ID_J1)),
                new Jugador(2, panelNuevaPartida.getNombreJugador2(), FabricaFlota.crearFlotaEstandar(PRIMER_ID_J2)),
                cargarMapa());
        instalarPartida(nueva);
        mostrarBlackout();
    }

    private Tablero cargarMapa() {
        try {
            return LectorMapas.cargar(MAPA);
        } catch (PersistenciaException e) {
            ventana.mostrarError("No se pudo leer el mapa; se usa el estándar. " + e.getMessage());
            return Tablero.crearMapaEstandar();
        }
    }

    private void alCargarPartida(Partida cargada) {
        if (!(cargada instanceof GestorPartida gestor)) {
            ventana.mostrarError("La partida cargada no se puede jugar.");
            return;
        }
        if (gestor.estaTerminada()) {
            ventana.mostrarError("Esa partida ya terminó.");
            return;
        }
        instalarPartida(gestor);
        mostrarBlackout();
    }

    private void abandonar() {
        soltarPartida();
        ventana.mostrar(Pantalla.INICIO);
        ventana.mostrarMensaje("Partida abandonada.");
    }

    /** Deja lista una partida: arma sus pantallas y registra a quienes la escuchan. */
    private void instalarPartida(GestorPartida nueva) {
        soltarPartida();
        partida = nueva;

        panelJuego = new PanelJuego(nueva, ventana::mostrarError);
        panelResumen = new PanelResumen(nueva, () -> ventana.mostrar(Pantalla.JUEGO));
        panelDespliegue = new PanelDespliegueTemporal(nueva, ventana::mostrarError);
        reemplazar(contenedorJuego, panelJuego);
        reemplazar(contenedorResumen, panelResumen);
        reemplazar(contenedorDespliegue, panelDespliegue);

        nueva.agregarObservador(this);
        nueva.agregarObservador(panelJuego);
        nueva.agregarObservador(barraMenu);
        // TEMPORAL (#80): nueva.agregarObservador(new AutoGuardado(...)) cuando Faccio lo suba.
        barraMenu.mostrarPartida(nueva);
    }

    /** Deja de escuchar la partida actual (si hay) y vuelve al estado "sin partida". */
    private void soltarPartida() {
        if (partida != null) {
            partida.quitarObservador(this);
            partida.quitarObservador(panelJuego);
            partida.quitarObservador(barraMenu);
        }
        partida = null;
        barraMenu.mostrarPartida(null);
    }

    // ---------- guardado en un hilo propio ----------

    /** Se ejecuta en el EDT justo antes de lanzar el hilo que guarda: no se puede tocar nada mientras tanto. */
    private void bloquearBotonesDelBlackout() {
        panelBlackout.setBotonesHabilitados(false);
    }

    /** Se ejecuta en el EDT cuando el hilo que guarda terminó (bien o mal). */
    private void liberarBotonesDelBlackout() {
        panelBlackout.setBotonesHabilitados(true);
    }

    // ---------- cambio de pantallas durante la partida ----------

    private void mostrarBlackout() {
        if (partida == null) {
            return;
        }
        panelBlackout.mostrarTurnoDe(partida.getJugadorActivo().nombre());
        panelBlackout.setBotonesHabilitados(true);
        ventana.mostrar(Pantalla.BLACKOUT);
        barraMenu.actualizar();
    }

    /** El jugador que recibe la computadora tocó "Listo": se muestra lo que le toca según la fase. */
    private void alTocarListo() {
        if (partida == null) {
            ventana.mostrar(Pantalla.INICIO);
            return;
        }
        FasePartida fase = partida.getFase();
        if (fase == FasePartida.DESPLIEGUE_J1 || fase == FasePartida.DESPLIEGUE_J2) {
            panelDespliegue.actualizar();
            ventana.mostrar(Pantalla.DESPLIEGUE);
        } else if (fase == FasePartida.EN_JUEGO) {
            if (panelResumen.cargarMensajes()) {
                ventana.mostrar(Pantalla.RESUMEN);
            } else {
                ventana.mostrar(Pantalla.JUEGO);
            }
        }
    }

    private void terminarPartida(ResultadoPartida resultado) {
        try {
            repositorioEstadisticas.registrar(resultado);
        } catch (PersistenciaException e) {
            ventana.mostrarError("No se pudo guardar el resultado en las estadísticas: " + e.getMessage());
        }
        // TEMPORAL (#74): ExportadorHistorial.exportar(historial, resultado, RutasApp.HISTORIALES).
        // TEMPORAL (#106): mostrar PanelFinPartida en lugar del cartel.
        JOptionPane.showMessageDialog(ventana, textoResultado(resultado), "Fin de la partida",
                JOptionPane.INFORMATION_MESSAGE);
        soltarPartida();
        ventana.mostrar(Pantalla.INICIO);
    }

    static String textoResultado(ResultadoPartida resultado) {
        String quien = resultado.esEmpate() ? "Empate" : "Ganó " + resultado.ganador();
        return quien + " (" + resultado.motivo() + ") en " + resultado.rondas() + " rondas.";
    }

    // ---------- avisos de la partida (ObservadorPartida) ----------

    /** Nada que hacer: las pantallas de la partida se refrescan solas. */
    @Override
    public void onEstadoCambiado() {
        // El controlador solo reacciona a cambios de turno y al fin de la partida.
    }

    /** Nada que hacer: cada cambio de fase viene seguido de un {@link #onTurnoIniciado(String)}. */
    @Override
    public void onFaseCambiada(FasePartida fase) {
        // Ver onTurnoIniciado.
    }

    /**
     * Empezó el turno de otro jugador (o su despliegue): se tapa la pantalla con el blackout.
     * Se cambia de pantalla con {@code invokeLater} para que los demás observadores terminen de
     * procesar el aviso antes.
     *
     * @param nombreJugador jugador que empieza
     */
    @Override
    public void onTurnoIniciado(String nombreJugador) {
        SwingUtilities.invokeLater(this::mostrarBlackout);
    }

    /**
     * Terminó la partida: se registra el resultado y se vuelve al inicio.
     *
     * @param resultado cómo terminó
     */
    @Override
    public void onPartidaTerminada(ResultadoPartida resultado) {
        SwingUtilities.invokeLater(() -> terminarPartida(resultado));
    }

    // ---------- auxiliares ----------

    private void cargarRanking() {
        try {
            panelEstadisticas.cargarRanking(CalculadoraRanking.calcular(repositorioEstadisticas.obtenerTodos()));
        } catch (PersistenciaException e) {
            ventana.mostrarError("No se pudieron leer las estadísticas: " + e.getMessage());
        }
    }

    private static void reemplazar(JPanel contenedor, JPanel panel) {
        contenedor.removeAll();
        contenedor.add(panel, BorderLayout.CENTER);
        contenedor.revalidate();
        contenedor.repaint();
    }

    private static void alMostrar(JPanel panel, Runnable accion) {
        panel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                accion.run();
            }
        });
    }

    /** Partida en curso, o {@code null} si no hay ninguna. Para los tests. */
    GestorPartida getPartida() {
        return partida;
    }
}