package batallanaval.vista;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import batallanaval.controlador.MotivoFin;
import batallanaval.controlador.ResultadoPartida;
import batallanaval.historial.EventoPartida;
import batallanaval.historial.TipoEvento;

/**
 * Prueba {@link PanelFinPartida}: qué muestra según cómo terminó la partida, el historial que se
 * despliega y se oculta, el botón para abrir el historial exportado y el hilo que hace titilar el
 * título. No abre ventanas ni programas: el abridor de archivos es falso.
 */
class PanelFinPartidaTest {

    private static final Path EXPORTADO = Path.of("datos", "historiales", "2026-11-14_2000_Bota-vs-Santi.txt");

    private final List<String> errores = new ArrayList<>();
    private final List<Path> abiertos = new ArrayList<>();
    private int vecesVolver = 0;

    private final PanelFinPartida panel = new PanelFinPartida(() -> vecesVolver++, errores::add, abiertos::add);

    // Qué muestra

    @Test
    void conGanadorMuestraQuienGanoElMotivoYLasRondas() {
        panel.mostrarResultado(resultado("Bota", MotivoFin.HUNDIMIENTO, 12), eventos(), EXPORTADO);

        assertEquals("¡Ganó Bota!", panel.getTitulo().getText());
        assertEquals("Hundió toda la flota rival.", panel.getMotivo().getText());
        assertEquals("Rondas jugadas: 12", panel.getRondas().getText());
        assertEquals("Historial exportado en " + EXPORTADO, panel.getArchivo().getText());
    }

    @Test
    void sinGanadorMuestraEmpate() {
        panel.mostrarResultado(resultado(null, MotivoFin.EMPATE, 40), eventos(), EXPORTADO);

        assertEquals("Empate", panel.getTitulo().getText());
    }

    @Test
    void cadaMotivoTieneSuExplicacion() {
        for (MotivoFin motivo : MotivoFin.values()) {
            assertFalse(PanelFinPartida.textoMotivo(motivo).isBlank(), "falta el texto de " + motivo);
        }
    }

    // Historial

    @Test
    void elHistorialSeCargaEnOrdenYEmpiezaOculto() {
        panel.mostrarResultado(resultado("Bota", MotivoFin.HUNDIMIENTO, 12), eventos(), EXPORTADO);

        assertEquals(List.of(
                "[R1] J1 DESPLIEGUE: Despliegue confirmado",
                "[R1] J2 DISPARO: Acorazado (7,4) -> Crucero (4,4), -4"), panel.getLineasHistorial());
        assertFalse(panel.isHistorialVisible());
    }

    @Test
    void verHistorialLoMuestraYLoVuelveAOcultar() {
        panel.mostrarResultado(resultado("Bota", MotivoFin.HUNDIMIENTO, 12), eventos(), EXPORTADO);

        panel.getBotonVerHistorial().doClick();
        assertTrue(panel.isHistorialVisible());
        assertEquals(PanelFinPartida.TEXTO_OCULTAR_HISTORIAL, panel.getBotonVerHistorial().getText());

        panel.getBotonVerHistorial().doClick();
        assertFalse(panel.isHistorialVisible());
        assertEquals(PanelFinPartida.TEXTO_VER_HISTORIAL, panel.getBotonVerHistorial().getText());
    }

    @Test
    void unaPartidaNuevaReemplazaElHistorialYLoDejaOculto() {
        panel.mostrarResultado(resultado("Bota", MotivoFin.HUNDIMIENTO, 12), eventos(), EXPORTADO);
        panel.getBotonVerHistorial().doClick();

        panel.mostrarResultado(resultado("Santi", MotivoFin.DOMINACION, 8), List.of(), EXPORTADO);

        assertTrue(panel.getLineasHistorial().isEmpty());
        assertFalse(panel.isHistorialVisible());
        assertEquals(PanelFinPartida.TEXTO_VER_HISTORIAL, panel.getBotonVerHistorial().getText());
    }

    // Historial exportado

    @Test
    void abrirHistorialExportadoAbreElArchivoDeEstaPartida() {
        panel.mostrarResultado(resultado("Bota", MotivoFin.HUNDIMIENTO, 12), eventos(), EXPORTADO);

        assertTrue(panel.getBotonAbrir().isEnabled());
        panel.getBotonAbrir().doClick();

        assertEquals(List.of(EXPORTADO), abiertos);
        assertTrue(errores.isEmpty());
    }

    @Test
    void siNoSeExportoElBotonQuedaDeshabilitadoYSeAvisa() {
        panel.mostrarResultado(resultado("Bota", MotivoFin.HUNDIMIENTO, 12), eventos(), null);

        assertFalse(panel.getBotonAbrir().isEnabled());
        assertEquals(PanelFinPartida.TEXTO_SIN_EXPORTAR, panel.getArchivo().getText());
    }

    @Test
    void siElSistemaNoPuedeAbrirloElJugadorVeDondeEstaElArchivo() {
        PanelFinPartida conAbridorRoto = new PanelFinPartida(() -> { }, errores::add, archivo -> {
            throw new IOException("sin programa asociado");
        });
        conAbridorRoto.mostrarResultado(resultado("Bota", MotivoFin.HUNDIMIENTO, 12), eventos(), EXPORTADO);

        conAbridorRoto.getBotonAbrir().doClick();

        assertEquals(List.of("No se pudo abrir el historial. Está en " + EXPORTADO), errores);
    }

    // Volver

    @Test
    void volverAlInicioEjecutaLaAccionRecibida() {
        panel.getBotonVolver().doClick();

        assertEquals(1, vecesVolver);
    }

    @Test
    void noSeAceptanNulos() {
        assertThrows(NullPointerException.class, () -> new PanelFinPartida(null, errores::add));
        assertThrows(NullPointerException.class, () -> new PanelFinPartida(() -> { }, null));
        assertThrows(NullPointerException.class, () -> panel.mostrarResultado(null, eventos(), EXPORTADO));
        assertThrows(NullPointerException.class,
                () -> panel.mostrarResultado(resultado("Bota", MotivoFin.HUNDIMIENTO, 1), null, EXPORTADO));
    }

    // Hilo propio

    @Test
    void esUnaPantallaConHiloQueNoArrancaSola() {
        assertFalse(panel.estaCorriendo());
        assertTrue(panel.getIntervaloMs() > 0);
    }

    @Test
    void actualizarHaceTitilarElTitulo() {
        Color antes = panel.getTitulo().getForeground();

        panel.actualizar();
        assertEquals(PanelFinPartida.COLOR_DESTACADO, panel.getTitulo().getForeground());
        assertNotEquals(antes, panel.getTitulo().getForeground());

        panel.actualizar();
        assertEquals(antes, panel.getTitulo().getForeground());
    }

    // Ayudas

    private static ResultadoPartida resultado(String ganador, MotivoFin motivo, int rondas) {
        return new ResultadoPartida(LocalDateTime.of(2026, 11, 14, 20, 0), "Bota", "Santi", ganador, rondas, motivo);
    }

    private static List<EventoPartida> eventos() {
        LocalDateTime momento = LocalDateTime.of(2026, 11, 14, 19, 30);
        return List.of(
                new EventoPartida(1, 1, TipoEvento.DESPLIEGUE, "Despliegue confirmado", momento),
                new EventoPartida(1, 2, TipoEvento.DISPARO, "Acorazado (7,4) -> Crucero (4,4), -4", momento));
    }
}
