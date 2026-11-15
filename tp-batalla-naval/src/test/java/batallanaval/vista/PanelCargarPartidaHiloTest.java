package batallanaval.vista;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import batallanaval.controlador.FasePartida;
import batallanaval.controlador.GestorPartida;
import batallanaval.controlador.Partida;
import batallanaval.excepciones.PersistenciaException;
import batallanaval.modelo.FabricaFlota;
import batallanaval.modelo.Jugador;
import batallanaval.modelo.Tablero;
import batallanaval.persistencia.CabeceraGuardado;

/**
 * Prueba que {@link PanelCargarPartida} carga con una {@link TareaCargar} en otro hilo: mientras
 * carga bloquea los botones y avisa, y al terminar (bien o mal) los libera. El "hilo" es un lanzador
 * manual que guarda la tarea sin correrla, para que el test decida cuándo termina. Necesita crear una
 * {@link VentanaPrincipal}: sin pantalla, los tests se saltean.
 */
class PanelCargarPartidaHiloTest {

    private final RepositorioPartidasFalso repositorio = new RepositorioPartidasFalso();
    private final List<Partida> cargadas = new ArrayList<>();
    private final List<Runnable> tareasPendientes = new ArrayList<>();

    private VentanaPrincipal ventana;
    private PanelCargarPartida panel;

    @BeforeEach
    void armarLaPantalla() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Hace falta un entorno con pantalla para crear una ventana");
        repositorio.partidaACargar = new GestorPartida(jugador("Bota", 1), jugador("Santi", 2),
                Tablero.crearMapaEstandar());
        repositorio.guardados.add(new CabeceraGuardado(1, "partida1", LocalDateTime.of(2026, 10, 5, 14, 32),
                "Bota", "Santi", 12, FasePartida.EN_JUEGO));

        ventana = new VentanaPrincipal();
        Executor lanzadorManual = tareasPendientes::add;
        panel = new PanelCargarPartida(ventana, repositorio, cargadas::add, nombre -> true,
                lanzadorManual, Runnable::run);
        ventana.agregarPantalla(Pantalla.INICIO, new JPanel());
        ventana.agregarPantalla(Pantalla.CARGAR, panel);
        panel.actualizarLista();
        tabla().setRowSelectionInterval(0, 0);
    }

    @AfterEach
    void cerrarLaVentana() {
        if (ventana != null) {
            ventana.dispose();
        }
    }

    @Test
    void alCargarLosBotonesQuedanBloqueadosYSeAvisaQueSeEstaCargando() {
        boton("Cargar").doClick();

        assertFalse(boton("Cargar").isEnabled());
        assertFalse(boton("Borrar").isEnabled());
        assertFalse(boton("Volver").isEnabled());
        assertTrue(hayEtiqueta(PanelCargarPartida.MENSAJE_CARGANDO));
        assertEquals(1, tareasPendientes.size());
        assertTrue(cargadas.isEmpty(), "la lectura la hace el hilo: todavía no llegó nada");
        assertTrue(repositorio.nombresCargados.isEmpty());
    }

    @Test
    void cuandoTerminaBienSeEntregaLaPartidaYSeLiberanLosBotones() {
        boton("Cargar").doClick();

        tareasPendientes.get(0).run();

        assertEquals(List.of("partida1"), repositorio.nombresCargados);
        assertEquals(1, cargadas.size());
        assertSame(repositorio.partidaACargar, cargadas.get(0));
        assertTrue(boton("Cargar").isEnabled());
        assertTrue(boton("Borrar").isEnabled());
        assertTrue(boton("Volver").isEnabled());
        assertFalse(hayEtiqueta(PanelCargarPartida.MENSAJE_CARGANDO));
    }

    @Test
    void cuandoTerminaMalSeMuestraElErrorYSeLiberanLosBotones() {
        repositorio.error = new PersistenciaException("Línea 14 del guardado inválida");
        boton("Cargar").doClick();

        tareasPendientes.get(0).run();

        assertTrue(cargadas.isEmpty());
        assertEquals("Línea 14 del guardado inválida", textoBarraEstado());
        assertTrue(boton("Cargar").isEnabled(), "no se puede quedar bloqueado después de un error");
        assertTrue(boton("Volver").isEnabled());
    }

    @Test
    void mientrasCargaNoSeLanzaOtraCarga() {
        boton("Cargar").doClick();

        boton("Cargar").doClick(); // un doble clic o un segundo intento

        assertEquals(1, tareasPendientes.size());
    }

    // Ayudas

    private static Jugador jugador(String nombre, int numero) {
        return new Jugador(numero, nombre, FabricaFlota.crearFlotaEstandar(numero == 1 ? 1 : 7));
    }

    private JTable tabla() {
        return (JTable) buscar(panel, JTable.class, null);
    }

    private JButton boton(String texto) {
        JButton boton = (JButton) buscar(panel, JButton.class, texto);
        assertNotNull(boton, "no hay un botón \"" + texto + "\"");
        return boton;
    }

    private boolean hayEtiqueta(String texto) {
        return buscar(panel, JLabel.class, texto) != null;
    }

    /** Lee la barra de estado de la ventana (la {@code JLabel} que está fuera de las pantallas). */
    private String textoBarraEstado() {
        JLabel barra = (JLabel) buscar(ventana.getContentPane(), JLabel.class, null, panel);
        assertNotNull(barra, "no se encontró la barra de estado");
        return barra.getText();
    }

    private static Component buscar(Container raiz, Class<? extends Component> tipo, String texto) {
        return buscar(raiz, tipo, texto, null);
    }

    private static Component buscar(Container raiz, Class<? extends Component> tipo, String texto,
                                    Component ignorar) {
        for (Component hijo : raiz.getComponents()) {
            if (hijo == ignorar) {
                continue;
            }
            boolean coincide = tipo.isInstance(hijo) && (texto == null || textoDe(hijo).equals(texto));
            if (coincide) {
                return hijo;
            }
            if (hijo instanceof Container contenedor) {
                Component encontrado = buscar(contenedor, tipo, texto, ignorar);
                if (encontrado != null) {
                    return encontrado;
                }
            }
        }
        return null;
    }

    private static String textoDe(Component componente) {
        if (componente instanceof JButton boton) {
            return boton.getText();
        }
        if (componente instanceof JLabel etiqueta) {
            return etiqueta.getText();
        }
        return "";
    }
}
