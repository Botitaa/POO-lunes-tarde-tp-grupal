package batallanaval.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import batallanaval.excepciones.DisparoInvalidoException;
import batallanaval.excepciones.HabilidadNoDisponibleException;
import batallanaval.excepciones.MovimientoInvalidoException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class BarcoTest {

    @Test
    void testDanioYLimites() {
        Crucero crucero = new Crucero(1);
        assertEquals(5, crucero.getVida());

        crucero.recibirDanio(3);
        assertEquals(2, crucero.getVida());

        // La vida no baja de 0
        crucero.recibirDanio(10);
        assertEquals(0, crucero.getVida());
        assertTrue(crucero.estaHundido());

        // Danio negativo lanza excepcion
        assertThrows(IllegalArgumentException.class, () -> crucero.recibirDanio(-1));
    }

    @Test
    void testCuracionYLimites() {
        Crucero crucero = new Crucero(1);
        crucero.recibirDanio(3); // Queda en 2
        assertEquals(2, crucero.getVida());

        crucero.curar(2); // Queda en 4
        assertEquals(4, crucero.getVida());

        crucero.curar(5); // No supera la vida maxima (5)
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
    void testCooldownsATravesDeVariosTurnos() {
        // Probamos el ciclo de cooldown de habilidad (espera de 3 turnos)
        Destructor destructor = new Destructor(1);
        assertTrue(destructor.puedeUsarHabilidad());

        // Turno 1: Usa la habilidad
        destructor.usarHabilidad();
        assertEquals(3, destructor.getCooldownHabilidad());
        assertFalse(destructor.puedeUsarHabilidad());

        // Turno 2: Pasa un turno
        destructor.reiniciarTurno();
        assertEquals(2, destructor.getCooldownHabilidad());
        assertFalse(destructor.puedeUsarHabilidad());

        // Turno 3: Pasa otro turno
        destructor.reiniciarTurno();
        assertEquals(1, destructor.getCooldownHabilidad());
        assertFalse(destructor.puedeUsarHabilidad());

        // Turno 4: Termina la espera, puede usarla de nuevo
        destructor.reiniciarTurno();
        assertEquals(0, destructor.getCooldownHabilidad());
        assertTrue(destructor.puedeUsarHabilidad());
    }

    @Test
    void testHabilidadDestructorDanioDoble() {
        Destructor destructor = new Destructor(1);
        assertEquals(3, destructor.getDanio());

        destructor.usarHabilidad();
        assertTrue(destructor.estaHabilidadActiva());
        assertEquals(6, destructor.getDanio());

        // Al terminar el turno vuelve al danio base
        destructor.reiniciarTurno();
        assertFalse(destructor.estaHabilidadActiva());
        assertEquals(3, destructor.getDanio());
    }

    @Test
    void testHabilidadAcorazadoRangoExtendido() {
        Acorazado acorazado = new Acorazado(1);
        assertEquals(6, acorazado.getRangoDisparo());

        acorazado.usarHabilidad();
        assertTrue(acorazado.estaHabilidadActiva());
        assertEquals(8, acorazado.getRangoDisparo());

        // Al terminar el turno vuelve al rango base
        acorazado.reiniciarTurno();
        assertFalse(acorazado.estaHabilidadActiva());
        assertEquals(6, acorazado.getRangoDisparo());
    }

    @Test
    void testHabilidadCruceroAutocuracion() {
        Crucero crucero = new Crucero(1);
        crucero.recibirDanio(3); // Queda en 2
        assertEquals(2, crucero.getVida());

        crucero.usarHabilidad(); // Se cura 2, queda en 4
        assertEquals(4, crucero.getVida());
    }

    @Test
    void testPolimorfismoConListaDeBarcos() {
        // Lista generica de Barco conteniendo las 3 subclases
        List<Barco> flota = new ArrayList<>();
        flota.add(new Acorazado(1));
        flota.add(new Crucero(2));
        flota.add(new Destructor(3));

        // Cada subclase responde polimorficamente con su propio simbolo
        assertEquals('A', flota.get(0).getSimbolo());
        assertEquals('C', flota.get(1).getSimbolo());
        assertEquals('D', flota.get(2).getSimbolo());

        // Probamos polimorfismo aplicando danio a todos por igual
        for (Barco barco : flota) {
            barco.recibirDanio(1);
        }

        assertEquals(7, flota.get(0).getVida()); // 8 - 1
        assertEquals(4, flota.get(1).getVida()); // 5 - 1
        assertEquals(2, flota.get(2).getVida()); // 3 - 1
    }

    @Test
    void testDisparoYNoMoverseTrasDisparar() {
        Acorazado acorazado = new Acorazado(1);
        assertTrue(acorazado.puedeDisparar());
        assertTrue(acorazado.puedeMoverse());

        acorazado.registrarDisparo();
        assertTrue(acorazado.yaDisparo());
        assertTrue(acorazado.estaRevelado());
        assertFalse(acorazado.puedeDisparar());
        assertFalse(acorazado.puedeMoverse());

        // No puede disparar dos veces en el mismo turno
        assertThrows(DisparoInvalidoException.class, acorazado::registrarDisparo);
    }

    @Test
    void testMovimientoYCasillasRestantes() {
        Destructor destructor = new Destructor(1);
        assertEquals(4, destructor.getCasillasRestantes());

        // Pasos invalidos lanzan excepcion
        assertThrows(MovimientoInvalidoException.class, () -> destructor.moverA(null, 0));
        assertThrows(MovimientoInvalidoException.class, () -> destructor.moverA(null, 5));
    }
}
