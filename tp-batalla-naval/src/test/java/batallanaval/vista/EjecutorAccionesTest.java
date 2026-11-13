package batallanaval.vista;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import batallanaval.excepciones.AccionInvalidaException;
import batallanaval.excepciones.DespliegueInvalidoException;
import batallanaval.excepciones.DisparoInvalidoException;
import batallanaval.excepciones.HabilidadNoDisponibleException;
import batallanaval.excepciones.MovimientoInvalidoException;

/**
 * Prueba que {@link EjecutorAcciones} muestra el mensaje correcto según la excepción y que,
 * después de un error, se puede seguir jugando. Es lógica pura: no crea componentes Swing.
 */
class EjecutorAccionesTest {

    private final List<String> errores = new ArrayList<>();

    @Test
    void siLaAccionTerminaBienNoSeMuestraNingunError() {
        List<String> hechas = new ArrayList<>();

        boolean ok = EjecutorAcciones.ejecutar(() -> hechas.add("disparo"), errores::add);

        assertTrue(ok);
        assertEquals(List.of("disparo"), hechas);
        assertTrue(errores.isEmpty());
    }

    @Test
    void siLaPartidaRechazaLaAccionSeMuestraElMensajeDeLaExcepcion() {
        boolean ok = EjecutorAcciones.ejecutar(() -> {
            throw new DisparoInvalidoException("El objetivo está fuera de rango.");
        }, errores::add);

        assertFalse(ok);
        assertEquals(List.of("El objetivo está fuera de rango."), errores);
    }

    @Test
    void todasLasExcepcionesDeReglasMuestranSuPropioMensaje() {
        List<AccionInvalidaException> rechazos = List.of(
                new AccionInvalidaException("No es tu turno."),
                new DespliegueInvalidoException("Esa casilla no es de tu zona."),
                new MovimientoInvalidoException("No te alcanzan los puntos."),
                new DisparoInvalidoException("No hay línea de visión."),
                new HabilidadNoDisponibleException("La habilidad está en espera."));

        for (AccionInvalidaException rechazo : rechazos) {
            EjecutorAcciones.ejecutar(() -> {
                throw rechazo;
            }, errores::add);
        }

        assertEquals(List.of("No es tu turno.", "Esa casilla no es de tu zona.", "No te alcanzan los puntos.",
                "No hay línea de visión.", "La habilidad está en espera."), errores);
    }

    @Test
    void unRechazoSinMensajeMuestraUnMensajeGenerico() {
        EjecutorAcciones.ejecutar(() -> {
            throw new AccionInvalidaException(null);
        }, errores::add);
        EjecutorAcciones.ejecutar(() -> {
            throw new AccionInvalidaException("   ");
        }, errores::add);

        assertEquals(List.of(EjecutorAcciones.MENSAJE_ACCION_INVALIDA, EjecutorAcciones.MENSAJE_ACCION_INVALIDA),
                errores);
    }

    @Test
    void otraRuntimeExceptionMuestraErrorInesperadoSinElMensajeTecnico() {
        EjecutorAcciones.ejecutar(() -> {
            throw new IllegalStateException("rutaPlanificada quedó en null");
        }, errores::add);
        EjecutorAcciones.ejecutar(() -> {
            throw new NullPointerException();
        }, errores::add);
        EjecutorAcciones.ejecutar(() -> {
            throw new IndexOutOfBoundsException(3);
        }, errores::add);

        assertEquals(List.of("Ocurrió un error inesperado", "Ocurrió un error inesperado",
                "Ocurrió un error inesperado"), errores);
    }

    @Test
    void despuesDeUnErrorElJuegoSigueAndando() {
        List<String> hechas = new ArrayList<>();

        EjecutorAcciones.ejecutar(() -> {
            throw new IllegalStateException("falla");
        }, errores::add);
        boolean ok = EjecutorAcciones.ejecutar(() -> hechas.add("terminar turno"), errores::add);

        assertTrue(ok);
        assertEquals(List.of("terminar turno"), hechas);
        assertEquals(1, errores.size());
    }

    @Test
    void losErroresDeLaMaquinaVirtualNoSeAtrapan() {
        // Un Error no es una RuntimeException: no se puede recuperar, así que se deja pasar.
        assertThrows(AssertionError.class, () -> EjecutorAcciones.ejecutar(() -> {
            throw new AssertionError("error grave");
        }, errores::add));
        assertTrue(errores.isEmpty());
    }

    @Test
    void elListenerDeUnBotonTambienAtrapaLosErrores() {
        ActionListener listener = EjecutorAcciones.alTocar(() -> {
            throw new HabilidadNoDisponibleException("Ya usaste la habilidad este turno.");
        }, errores::add);

        // Simula el clic: Swing llama a actionPerformed con un evento.
        listener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "Usar habilidad"));

        assertEquals(List.of("Ya usaste la habilidad este turno."), errores);
    }

    @Test
    void noSeAceptanArgumentosNulos() {
        assertThrows(NullPointerException.class, () -> EjecutorAcciones.ejecutar(null, errores::add));
        assertThrows(NullPointerException.class, () -> EjecutorAcciones.ejecutar(() -> { }, null));
        assertThrows(NullPointerException.class, () -> EjecutorAcciones.alTocar(null, errores::add));
        assertThrows(NullPointerException.class, () -> EjecutorAcciones.alTocar(() -> { }, null));
    }
}
