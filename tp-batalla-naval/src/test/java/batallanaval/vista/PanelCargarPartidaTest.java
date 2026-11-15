package batallanaval.vista;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
 * Prueba que {@link PanelCargarPartida} lista los guardados, carga y borra el elegido, y muestra el
 * mensaje de {@link PersistenciaException} cuando algo falla. Necesita crear una
 * {@link VentanaPrincipal}, así que si el entorno no tiene pantalla los tests se saltean.
 */
class PanelCargarPartidaTest {

    private final RepositorioPartidasFalso repositorio = new RepositorioPartidasFalso();
    private final List<Partida> cargadas = new ArrayList<>();
    private final List<String> borradosConsultados = new ArrayList<>();

    private VentanaPrincipal ventana;
    private PanelCargarPartida panel;
    private boolean permiteBorrar = true;

    @BeforeEach
    void armarLaPantalla() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Hace falta un entorno con pantalla para crear una ventana");
        repositorio.partidaACargar = new GestorPartida(jugador("Bota", 1), jugador("Santi", 2),
                Tablero.crearMapaEstandar());
        repositorio.guardados.add(cabecera("partida2", "Ana", "Luis", 7, LocalDateTime.of(2026, 10, 6, 21, 5)));
        repositorio.guardados.add(cabecera("partida1", "Bota", "Santi", 12, LocalDateTime.of(2026, 10, 5, 14, 32)));

        ventana = new VentanaPrincipal();
        panel = new PanelCargarPartida(ventana, repositorio, cargadas::add, nombre -> {
            borradosConsultados.add(nombre);
            return permiteBorrar;
        });
        ventana.agregarPantalla(Pantalla.INICIO, new JPanel());
        ventana.agregarPantalla(Pantalla.CARGAR, panel);
    }

    @AfterEach
    void cerrarLaVentana() {
        if (ventana != null) {
            ventana.dispose();
        }
    }

    @Test
    void alCrearseLaTablaTieneLasColumnasYNingunaFila() {
        JTable tabla = buscarTabla();

        assertEquals(4, tabla.getColumnCount());
        assertEquals("Nombre", tabla.getColumnName(0));
        assertEquals("Jugadores", tabla.getColumnName(1));
        assertEquals("Ronda", tabla.getColumnName(2));
        assertEquals("Fecha", tabla.getColumnName(3));
        assertEquals(0, tabla.getRowCount());
    }

    @Test
    void actualizarListaMuestraLosGuardadosEnElOrdenQueLosDevuelveElRepositorio() {
        panel.actualizarLista();

        JTable tabla = buscarTabla();
        assertEquals(2, tabla.getRowCount());
        assertEquals("partida2", tabla.getValueAt(0, 0));
        assertEquals("Ana vs Luis", tabla.getValueAt(0, 1));
        assertEquals(7, tabla.getValueAt(0, 2));
        assertEquals("06/10/2026 21:05", tabla.getValueAt(0, 3));
        assertEquals("partida1", tabla.getValueAt(1, 0));
    }

    @Test
    void sinGuardadosSeMuestraUnAviso() {
        repositorio.guardados.clear();

        panel.actualizarLista();

        assertEquals(0, buscarTabla().getRowCount());
        assertTrue(hayEtiquetaConTexto(PanelCargarPartida.MENSAJE_SIN_PARTIDAS));
    }

    @Test
    void cargarYBorrarEstanDeshabilitadosHastaElegirUnaFila() {
        panel.actualizarLista();
        assertFalse(boton("Cargar").isEnabled());
        assertFalse(boton("Borrar").isEnabled());

        buscarTabla().setRowSelectionInterval(0, 0);

        assertTrue(boton("Cargar").isEnabled());
        assertTrue(boton("Borrar").isEnabled());
    }

    @Test
    void cargarLePideLaPartidaElegidaAlRepositorioYSeLaEntregaALaAccion() {
        panel.actualizarLista();
        buscarTabla().setRowSelectionInterval(1, 1);

        boton("Cargar").doClick();

        assertEquals(List.of("partida1"), repositorio.nombresCargados);
        assertEquals(1, cargadas.size());
        assertSame(repositorio.partidaACargar, cargadas.get(0));
    }

    @Test
    void siLaCargaFallaSeMuestraElMensajeDeLaExcepcionYNoSeEntregaNada() {
        panel.actualizarLista();
        repositorio.error = new PersistenciaException("Línea 14 del guardado inválida");
        buscarTabla().setRowSelectionInterval(0, 0);

        boton("Cargar").doClick();

        assertTrue(cargadas.isEmpty());
        assertEquals("Línea 14 del guardado inválida", textoBarraEstado());
    }

    @Test
    void borrarConConfirmacionBorraYActualizaLaLista() {
        panel.actualizarLista();
        buscarTabla().setRowSelectionInterval(0, 0);

        boton("Borrar").doClick();

        assertEquals(List.of("partida2"), borradosConsultados);
        assertEquals(List.of("partida2"), repositorio.nombresBorrados);
        assertEquals(1, buscarTabla().getRowCount());
        assertEquals("partida1", buscarTabla().getValueAt(0, 0));
    }

    @Test
    void siElJugadorNoConfirmaNoSeBorraNada() {
        panel.actualizarLista();
        buscarTabla().setRowSelectionInterval(0, 0);
        permiteBorrar = false;

        boton("Borrar").doClick();

        assertTrue(repositorio.nombresBorrados.isEmpty());
        assertEquals(2, buscarTabla().getRowCount());
    }

    @Test
    void siElBorradoFallaSeMuestraElMensajeDeLaExcepcion() {
        panel.actualizarLista();
        buscarTabla().setRowSelectionInterval(0, 0);
        repositorio.error = new PersistenciaException("El archivo a borrar no existe: datos/partidas/partida2.sav");

        boton("Borrar").doClick();

        assertEquals("El archivo a borrar no existe: datos/partidas/partida2.sav", textoBarraEstado());
    }

    @Test
    void siNoSePuedeLeerLaListaSeMuestraElMensajeYLaTablaQuedaVacia() {
        repositorio.error = new PersistenciaException("Error al listar las partidas en datos/partidas");

        panel.actualizarLista();

        assertEquals(0, buscarTabla().getRowCount());
        assertTrue(hayEtiquetaConTexto("Error al listar las partidas en datos/partidas"));
        assertEquals("Error al listar las partidas en datos/partidas", textoBarraEstado());
    }

    @Test
    void volverLlevaAlInicio() {
        ventana.mostrar(Pantalla.CARGAR);

        boton("Volver").doClick();

        assertEquals(Pantalla.INICIO, ventana.getPantallaActual());
    }

    @Test
    void noSeAceptanArgumentosNulos() {
        assertThrows(NullPointerException.class, () -> new PanelCargarPartida(null, repositorio, cargadas::add));
        assertThrows(NullPointerException.class, () -> new PanelCargarPartida(ventana, null, cargadas::add));
        assertThrows(NullPointerException.class, () -> new PanelCargarPartida(ventana, repositorio, null));
    }

    // Ayudas

    private static CabeceraGuardado cabecera(String nombre, String j1, String j2, int ronda, LocalDateTime fecha) {
        return new CabeceraGuardado(1, nombre, fecha, j1, j2, ronda, FasePartida.EN_JUEGO);
    }

    private static Jugador jugador(String nombre, int numero) {
        return new Jugador(numero, nombre, FabricaFlota.crearFlotaEstandar(numero == 1 ? 1 : 7));
    }

    private JTable buscarTabla() {
        return (JTable) buscar(panel, JTable.class, null);
    }

    private JButton boton(String texto) {
        return (JButton) buscar(panel, JButton.class, texto);
    }

    private boolean hayEtiquetaConTexto(String texto) {
        return buscar(panel, JLabel.class, texto) != null;
    }

    /** Lee la barra de estado de la ventana (la única {@code JLabel} de la ventana fuera de las pantallas). */
    private String textoBarraEstado() {
        BarraDeEstado barra = new BarraDeEstado();
        barra.buscar(ventana.getContentPane());
        assertNotNull(barra.texto, "no se encontró la barra de estado");
        return barra.texto;
    }

    private static Component buscar(Container raiz, Class<? extends Component> tipo, String texto) {
        for (Component hijo : raiz.getComponents()) {
            boolean coincide = tipo.isInstance(hijo) && (texto == null || textoDe(hijo).equals(texto));
            if (coincide) {
                return hijo;
            }
            if (hijo instanceof Container contenedor) {
                Component encontrado = buscar(contenedor, tipo, texto);
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

    /** Busca la barra de estado: es el {@code JLabel} hijo directo de la ventana (no está dentro de una pantalla). */
    private static final class BarraDeEstado {
        String texto;

        void buscar(Container contenedor) {
            for (Component hijo : contenedor.getComponents()) {
                if (hijo instanceof JLabel etiqueta) {
                    texto = etiqueta.getText();
                }
            }
        }
    }
}
