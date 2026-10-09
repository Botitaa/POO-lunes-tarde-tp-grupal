package batallanaval.excepciones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import org.junit.jupiter.api.Assertions;

import org.junit.jupiter.api.Test;

class ExcepcionesTest {

    @Test
    void lasDeReglasHerendanDeAccionInvalida() {
        Assertions.assertInstanceOf(AccionInvalidaException.class, new DespliegueInvalidoException("x"));
        Assertions.assertInstanceOf(AccionInvalidaException.class, new MovimientoInvalidoException("x"));
        Assertions.assertInstanceOf(AccionInvalidaException.class, new DisparoInvalidoException("x"));
        Assertions.assertInstanceOf(AccionInvalidaException.class, new HabilidadNoDisponibleException("x"));
    }

    @Test
    void mapaInvalidoNoEsAccionInvalida() {
        assertFalse(AccionInvalidaException.class.isAssignableFrom(MapaInvalidoException.class));
    }

    @Test
    void sonUnchecked() {
        Assertions.assertInstanceOf(RuntimeException.class, new AccionInvalidaException("x"));
        Assertions.assertInstanceOf(RuntimeException.class, new MapaInvalidoException("x"));
    }

    @Test
    void conservanElMensaje() {
        Assertions.assertEquals("fuera de rango", new DisparoInvalidoException("fuera de rango").getMessage());
    }
}
