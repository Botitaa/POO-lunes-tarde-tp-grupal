package batallanaval.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import batallanaval.excepciones.DisparoInvalidoException;
import batallanaval.excepciones.HabilidadNoDisponibleException;
import batallanaval.excepciones.MovimientoInvalidoException;
import org.junit.jupiter.api.Test;

class BarcoTest {

    @Test
    void testDanioYLimites() {
        Crucero crucero = new Crucero(1);
        assertEquals(5, crucero.getVida());

        crucero.recibirDanio(3);
        assertEquals(2, crucero.getVida());

        // No baja de 0
        crucero.recibirDanio(10);
        assertEquals(0, crucero.getVida());
        assertTrue(crucero.estaHundido());

        // Daño negativo lanza excepcion
        assertThrows(IllegalArgumentException.class, () -> crucero.recibirDanio(-1));
    }

    @Test
    void testCuracionYLimites() {
        Crucero crucero = new Crucero(1);
        crucero.recibirDanio(3); // Queda en 2
        assertEquals(2, crucero.getVida());

        crucero.curar(2); // Queda en 4
        assertEquals(4, crucero.getVida());

        crucero.curar(5); // No supera vida maxima (5)
        assertEquals(5, crucero.getVida());

        // Curacion negativa lanza excepcion
        assertThrows(IllegalArgumentException.class, () -> crucero.curar(-1));

        // Barco hundido no se puede curar
        crucero.recibirDanio(10);
        assertTrue(crucero.estaHundido());
        crucero.curar(3);
        assertEquals(0, crucero.getVida());
    }

    @Test
    void testDisparoYCooldown() {
        Acorazado acorazado = new Acorazado(1);
        assertTrue(acorazado.puedeDisparar());
        assertEquals(0, acorazado.getCooldownDisparo());
        assertFalse(acorazado.estaRevelado());

        acorazado.registrarDisparo();
        assertTrue(acorazado.yaDisparo());
        assertTrue(acorazado.estaRevelado());
        assertFalse(acorazado.puedeDisparar());
        assertEquals(1, acorazado.getCooldownDisparo());

        // No puede disparar de nuevo este turno
        assertThrows(DisparoInvalidoException.class, acorazado::registrarDisparo);
    }

    @Test
    void testNoMoverseTrasDisparar() {
        Destructor destructor = new Destructor(1);
        assertTrue(destructor.puedeMoverse());

        destructor.registrarDisparo();
        assertFalse(destructor.puedeMoverse());
    }

    @Test
    void testMovimientoYCasillasRestantes() {
        Destructor destructor = new Destructor(1);
        assertEquals(4, destructor.getCasillasRestantes());
        assertTrue(destructor.puedeMoverse());

        // Pasos negativos o superiores al limite lanzan excepcion
        assertThrows(MovimientoInvalidoException.class, () -> destructor.moverA(null, 0));
        assertThrows(MovimientoInvalidoException.class, () -> destructor.moverA(null, 5));
    }

    @Test
    void testReiniciarTurno() {
        Acorazado acorazado = new Acorazado(1);
        acorazado.registrarDisparo();
        assertEquals(1, acorazado.getCooldownDisparo());
        assertTrue(acorazado.yaDisparo());

        acorazado.reiniciarTurno();
        assertEquals(0, acorazado.getCooldownDisparo());
        assertFalse(acorazado.yaDisparo());
        assertEquals(2, acorazado.getCasillasRestantes());
    }

    @Test
    void testHabilidadDestructorDanioDoble() {
        Destructor destructor = new Destructor(1);
        assertEquals(3, destructor.getDanio());
        assertTrue(destructor.puedeUsarHabilidad());

        destructor.usarHabilidad();
        assertTrue(destructor.estaHabilidadActiva());
        assertEquals(6, destructor.getDanio());
        assertEquals(3, destructor.getCooldownHabilidad());

        // No puede volver a usarla en cooldown
        assertFalse(destructor.puedeUsarHabilidad());
        assertThrows(HabilidadNoDisponibleException.class, destructor::usarHabilidad);

        // Al reiniciar turno se desactiva el efecto
        destructor.reiniciarTurno();
        assertFalse(destructor.estaHabilidadActiva());
        assertEquals(3, destructor.getDanio());
        assertEquals(2, destructor.getCooldownHabilidad());
    }

    @Test
    void testHabilidadAcorazadoRangoExtendido() {
        Acorazado acorazado = new Acorazado(1);
        assertEquals(6, acorazado.getRangoDisparo());

        acorazado.usarHabilidad();
        assertEquals(8, acorazado.getRangoDisparo());

        acorazado.reiniciarTurno();
        assertEquals(6, acorazado.getRangoDisparo());
    }

    @Test
    void testHabilidadCruceroAutocuracion() {
        Crucero crucero = new Crucero(1);
        crucero.recibirDanio(3);
        assertEquals(2, crucero.getVida());

        crucero.usarHabilidad();
        assertEquals(4, crucero.getVida());
    }

    @Test
    void testRestaurarEstado() {
        Crucero crucero = new Crucero(1);
        crucero.restaurar(3, null, 0, 2, 1, false, true);

        assertEquals(3, crucero.getVida());
        assertEquals(0, crucero.getCooldownDisparo());
        assertEquals(2, crucero.getCooldownHabilidad());
        assertEquals(1, crucero.getCasillasRestantes());
        assertFalse(crucero.estaHabilidadActiva());
        assertTrue(crucero.estaRevelado());
    }
}
