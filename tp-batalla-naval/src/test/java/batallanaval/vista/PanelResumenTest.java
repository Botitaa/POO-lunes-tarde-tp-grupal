package batallanaval.vista;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JButton;

import org.junit.jupiter.api.Test;

import batallanaval.controlador.PartidaConsulta;

class PanelResumenTest {

    private static final String MENSAJE_1 = "Tu Crucero (4,4) recibió 4 de daño desde (7,4)";
    private static final String MENSAJE_2 = "Tu Destructor (5,5) fue hundido en (5,5)";

    private final List<String> mensajes = new ArrayList<>();
    private final AtomicInteger veces = new AtomicInteger();
    private final PanelResumen panel = new PanelResumen(partidaConMensajes(mensajes), veces::incrementAndGet);

    @Test
    void conMensajesDevuelveTrueYLosMuestraEnOrden() {
        mensajes.add(MENSAJE_1);
        mensajes.add(MENSAJE_2);

        boolean hayQueMostrar = panel.cargarMensajes();

        assertTrue(hayQueMostrar);
        assertEquals(List.of(MENSAJE_1, MENSAJE_2), panel.getMensajesMostrados());
    }

    @Test
    void sinMensajesDevuelveFalseYNoMuestraNada() {
        boolean hayQueMostrar = panel.cargarMensajes();

        assertFalse(hayQueMostrar, "con el resumen vacío la pantalla se tiene que saltear");
        assertTrue(panel.getMensajesMostrados().isEmpty());
    }

    @Test
    void siElOrigenDevuelveNullSeTrataComoVacio() {
        PanelResumen panelConNull = new PanelResumen(partidaConMensajes(null), () -> { });

        assertFalse(panelConNull.cargarMensajes());
        assertTrue(panelConNull.getMensajesMostrados().isEmpty());
    }

    @Test
    void cadaCargaReemplazaLosMensajesAnterioresEnLugarDeAcumularlos() {
        mensajes.add(MENSAJE_1);
        panel.cargarMensajes();

        mensajes.clear();
        mensajes.add(MENSAJE_2);
        boolean hayQueMostrar = panel.cargarMensajes();

        assertTrue(hayQueMostrar);
        assertEquals(List.of(MENSAJE_2), panel.getMensajesMostrados());
    }

    @Test
    void siDespuesDeCargarElResumenQuedaVacioSeLimpiaLaLista() {
        mensajes.add(MENSAJE_1);
        panel.cargarMensajes();

        mensajes.clear();
        boolean hayQueMostrar = panel.cargarMensajes();

        assertFalse(hayQueMostrar);
        assertTrue(panel.getMensajesMostrados().isEmpty(), "no tienen que quedar mensajes del turno anterior");
    }

    @Test
    void apretarContinuarEjecutaLaAccionUnaSolaVez() {
        buscarBoton(panel, "Continuar").doClick();

        assertEquals(1, veces.get());
    }

    @Test
    void cargarMensajesNoEjecutaLaAccionDeContinuar() {
        mensajes.add(MENSAJE_1);

        panel.cargarMensajes();

        assertEquals(0, veces.get());
    }

    @Test
    void noSeAceptanArgumentosNulos() {
        assertThrows(NullPointerException.class, () -> new PanelResumen(null, () -> { }));
        assertThrows(NullPointerException.class, () -> new PanelResumen(partidaConMensajes(mensajes), null));
    }

    /**
     * Una {@link PartidaConsulta} de prueba que solo responde {@code getMensajesResumen()}, que es lo
     * único que usa el panel. Así no hace falta escribir los otros métodos de la interfaz.
     */
    private static PartidaConsulta partidaConMensajes(List<String> mensajes) {
        return (PartidaConsulta) Proxy.newProxyInstance(
                PartidaConsulta.class.getClassLoader(),
                new Class<?>[] { PartidaConsulta.class },
                (proxy, metodo, argumentos) -> {
                    if (metodo.getName().equals("getMensajesResumen")) {
                        return mensajes;
                    }
                    throw new UnsupportedOperationException(metodo.getName());
                });
    }

    /** Busca el botón con ese texto en el árbol completo de componentes. */
    private static JButton buscarBoton(Container contenedor, String texto) {
        for (Component componente : contenedor.getComponents()) {
            if (componente instanceof JButton && texto.equals(((JButton) componente).getText())) {
                return (JButton) componente;
            }
            if (componente instanceof Container) {
                JButton encontrado = buscarBoton((Container) componente, texto);
                if (encontrado != null) {
                    return encontrado;
                }
            }
        }
        return null;
    }
}
