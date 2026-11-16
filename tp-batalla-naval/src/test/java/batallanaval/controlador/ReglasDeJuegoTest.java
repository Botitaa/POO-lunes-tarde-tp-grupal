package batallanaval.controlador;

import batallanaval.excepciones.*;
import batallanaval.modelo.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reglas del juego de punta a punta, sobre todo cuándo se puede disparar. Cada test arma una
 * partida a medida (con {@code restaurar}, para ubicar los barcos donde haga falta) y juega turnos
 * reales con los métodos de la partida.
 */
class ReglasDeJuegoTest {

    // ---------- armado ----------

    /** Mapa de agua con islas ('I') y piedras ('P') en las posiciones dadas (solo filas 3-6). */
    static Tablero mapa(String... obstaculos) {
        Casilla[][] g = new Casilla[10][10];
        for (Casilla[] fila : g) Arrays.fill(fila, Casilla.AGUA);
        for (String o : obstaculos) {
            String[] p = o.split(",");
            g[Integer.parseInt(p[1])][Integer.parseInt(p[2])] = p[0].equals("I") ? Casilla.ISLA : Casilla.PIEDRA;
        }
        return new Tablero(g);
    }

    /** Barco de la partida: id, tipo, jugador, fila, columna. */
    static DatosBarco b(int id, char tipo, int j, int f, int c) {
        EstadisticasBarco e = switch (tipo) {
            case 'A' -> new Acorazado(0).getEstadisticas();
            case 'C' -> new Crucero(0).getEstadisticas();
            default -> new Destructor(0).getEstadisticas();
        };
        return new DatosBarco(id, tipo, j, new Posicion(f, c), e.vidaMaxima(), 0, 0, e.movimientoMaxPorTurno(), false, false);
    }

    /**
     * Partida EN_JUEGO, turno de J1, ronda 1. Los barcos dados reemplazan a los de por defecto
     * (por id); el resto queda en las esquinas, lejos de todo.
     */
    static GestorPartida partida(Tablero t, DatosBarco... cambios) {
        Map<Integer, DatosBarco> barcos = new LinkedHashMap<>();
        char[] tipos = {'A', 'C', 'C', 'D', 'D', 'D'};
        for (int i = 0; i < 6; i++) {
            barcos.put(i + 1, b(i + 1, tipos[i], 1, 0, i));       // J1 arriba a la izquierda
            barcos.put(i + 7, b(i + 7, tipos[i], 2, 9, 9 - i));   // J2 abajo a la derecha
        }
        for (DatosBarco d : cambios) barcos.put(d.id(), d);
        var inst = new InstantaneaPartida("test", 1, 1, FasePartida.EN_JUEGO,
                List.of(new DatosJugador(1, "Ana", 6, 0, List.of()), new DatosJugador(2, "Beto", 6, 0, List.of())),
                new ArrayList<>(barcos.values()), List.of(), List.of());
        return GestorPartida.restaurar(inst, t);
    }

    static Set<Integer> ids(List<EstadoBarco> l) {
        Set<Integer> s = new TreeSet<>();
        for (EstadoBarco e : l) s.add(e.id());
        return s;
    }

    static EstadoBarco enemigo(GestorPartida p, int id) {
        for (EstadoBarco e : p.getBarcosEnemigosVisibles()) if (e.id() == id) return e;
        return null;
    }

    static EstadoBarco propio(GestorPartida p, int id) {
        for (EstadoBarco e : p.getBarcosPropios()) if (e.id() == id) return e;
        return null;
    }

    // ---------- cuándo se puede disparar ----------

    @Test
    void d01_puedeDispararAUnEnemigoVisibleEnRangoYConLineaLibre() {
        // Destructor J1 en (2,2): rango 2, visión 2. Destructor J2 en (4,2)
        GestorPartida p = partida(mapa(), b(4, 'D', 1, 2, 2), b(10, 'D', 2, 4, 2));
        p.seleccionarBarco(4);
        assertEquals(Set.of(10), ids(p.getObjetivosValidos()));
        p.disparar(10);
        assertNull(enemigo(p, 10), "destructor (vida 3) recibe 3 y se hunde");
    }

    @Test
    void d02_noPuedeDispararFueraDeRango() {
        // Crucero J1 (id 2) en (2,2): rango 3, visión 4. Enemigo a distancia 4 se ve (visión 4) pero queda fuera de rango (rango 3)
        GestorPartida p = partida(mapa(), b(2, 'C', 1, 2, 2), b(10, 'D', 2, 6, 2));
        p.seleccionarBarco(2);
        assertTrue(p.getObjetivosValidos().isEmpty(), "a distancia 4 un crucero lo ve (visión 4) pero no tiene rango (rango 3)");
        assertThrows(DisparoInvalidoException.class, () -> p.disparar(10));
    }

    @Test
    void d03_visibleGraciasAOtroBarcoPeroFueraDeRangoDelTirador() {
        // Acorazado (visión 5) lo ve, pero el destructor (rango 2) que dispara está lejos
        GestorPartida p = partida(mapa(), b(1, 'A', 1, 2, 0), b(4, 'D', 1, 0, 9), b(10, 'D', 2, 6, 0));
        p.seleccionarBarco(4);
        assertNotNull(enemigo(p, 10), "el acorazado lo ve");
        assertTrue(p.getObjetivosValidos().isEmpty());
        p.seleccionarBarco(1);
        assertEquals(Set.of(10), ids(p.getObjetivosValidos()), "el acorazado sí lo alcanza (dist 4, rango 4)");
    }

    @Test
    void d04_visionCompartida_unBarcoVeYOtroDispara() {
        // Acorazado J1 en (1,4) con habilidad tiene rango 6 y visión 5. Crucero J1 en (3,6): visión 4. Enemigo en (7,4): a 6 del acorazado
        GestorPartida p = partida(mapa(), b(1, 'A', 1, 1, 4), b(2, 'C', 1, 3, 6), b(10, 'D', 2, 7, 4));
        p.seleccionarBarco(1);
        p.usarHabilidad();
        assertEquals(Set.of(10), ids(p.getObjetivosValidos()), "con la visión del crucero, el acorazado con habilidad (rango 6) puede disparar");
    }

    @Test
    void d05_unaIslaEnElMedioImpideDispararAunqueSeVea() {
        // Acorazado J1 en (2,1), isla en (4,1), enemigo en (6,1). Lo ve el crucero J1 desde (4,3)
        GestorPartida p = partida(mapa("I,4,1"), b(1, 'A', 1, 2, 1), b(2, 'C', 1, 4, 3), b(10, 'D', 2, 6, 1));
        assertNotNull(enemigo(p, 10), "el crucero en (4,3) lo ve");
        p.seleccionarBarco(1);
        assertTrue(p.getObjetivosValidos().isEmpty(), "el acorazado tiene la isla en el medio");
        assertThrows(DisparoInvalidoException.class, () -> p.disparar(10));
    }

    @Test
    void d06_unaPiedraNoCortaElDisparo() {
        GestorPartida p = partida(mapa("P,4,1"), b(1, 'A', 1, 2, 1), b(10, 'D', 2, 6, 1));
        p.seleccionarBarco(1);
        assertEquals(Set.of(10), ids(p.getObjetivosValidos()));
    }

    @Test
    void d07_unBarcoEnElMedioNoCortaElDisparo() {
        GestorPartida p = partida(mapa(), b(1, 'A', 1, 2, 1), b(11, 'D', 2, 4, 1), b(10, 'D', 2, 6, 1));
        p.seleccionarBarco(1);
        assertEquals(Set.of(10, 11), ids(p.getObjetivosValidos()));
    }

    @Test
    void d08_unBarcoQueYaDisparoNoVuelveADispararEnElTurno() {
        GestorPartida p = partida(mapa(), b(1, 'A', 1, 2, 1), b(10, 'D', 2, 6, 1), b(11, 'D', 2, 5, 2));
        p.seleccionarBarco(1);
        p.disparar(11);
        assertTrue(p.getObjetivosValidos().isEmpty());
        assertThrows(DisparoInvalidoException.class, () -> p.disparar(10));
    }

    @Test
    void d09_variosBarcosDistintosPuedenDispararEnElMismoTurno() {
        GestorPartida p = partida(mapa(), b(1, 'A', 1, 2, 1), b(2, 'C', 1, 2, 2), b(9, 'C', 2, 5, 1));
        p.seleccionarBarco(1);
        p.disparar(9);
        p.seleccionarBarco(2);
        p.disparar(9);
        assertNull(enemigo(p, 9), "crucero (vida 5) recibe 4 + 2 y se hunde");
    }

    @Test
    void d10_unBarcoQueDisparoNoPuedeMoverse() {
        GestorPartida p = partida(mapa(), b(1, 'A', 1, 2, 1), b(10, 'D', 2, 6, 1));
        p.seleccionarBarco(1);
        p.disparar(10);
        assertThrows(MovimientoInvalidoException.class, () -> p.agregarPaso(new Posicion(1, 1)));
    }

    @Test
    void d11_moverYDespuesDispararSePuede() {
        // Destructor J1 en (1,2), rango 2. Enemigo en (4,2): dist 3. Se mueve a (2,2) y dispara
        GestorPartida p = partida(mapa(), b(4, 'D', 1, 1, 2), b(10, 'D', 2, 4, 2));
        p.seleccionarBarco(4);
        assertTrue(p.getObjetivosValidos().isEmpty());
        p.agregarPaso(new Posicion(2, 2));
        p.confirmarMovimiento();
        assertEquals(Set.of(10), ids(p.getObjetivosValidos()));
    }

    @Test
    void d12_sinBarcoSeleccionadoNoSeDispara() {
        GestorPartida p = partida(mapa(), b(10, 'D', 2, 2, 7));
        assertThrows(DisparoInvalidoException.class, () -> p.disparar(10));
    }

    @Test
    void d13_noSePuedeDispararAUnBarcoPropio() {
        GestorPartida p = partida(mapa());
        p.seleccionarBarco(1);
        assertThrows(DisparoInvalidoException.class, () -> p.disparar(2));
    }

    @Test
    void d14_enemigoEnZonaDeDominacionSeVeSiempreYSePuedeDispararSiEstaEnRango() {
        // Acorazado J1 en (0,4): visión 5, rango 6. Enemigo en (5,5) dominación: dist 5
        GestorPartida p = partida(mapa(), b(4, 'D', 1, 2, 9), b(10, 'D', 2, 5, 5));
        assertNotNull(enemigo(p, 10), "dominación: lo ven los dos");
        p.seleccionarBarco(4);
        assertTrue(p.getObjetivosValidos().isEmpty(), "pero el destructor en (2,9) está fuera de rango (dist 4 > 2)");
    }

    // ---------- habilidades ----------

    @Test
    void h01_acorazadoConHabilidadDisparaARango8() {
        GestorPartida p = partida(mapa(), b(1, 'A', 1, 0, 0), b(2, 'C', 1, 3, 0), b(10, 'D', 2, 6, 0));
        // dist acorazado-enemigo = 6; lo ve el crucero (dist 3)
        p.seleccionarBarco(1);
        assertTrue(p.getObjetivosValidos().isEmpty(), "rango 4 no llega a 6");
        p.usarHabilidad();
        assertEquals(Set.of(10), ids(p.getObjetivosValidos()));
    }

    @Test
    void h02_destructorConHabilidadHace6() {
        GestorPartida p = partida(mapa(), b(4, 'D', 1, 2, 1), b(7, 'A', 2, 4, 1));
        p.seleccionarBarco(4);
        p.usarHabilidad();
        p.disparar(7);
        assertEquals(2, enemigo(p, 7).vida());
    }

    @Test
    void h03_cruceroSeCura2SinPasarseDelMaximo() {
        DatosBarco herido = new DatosBarco(2, 'C', 1, new Posicion(1, 1), 2, 0, 0, 3, false, false);
        GestorPartida p = partida(mapa(), herido);
        p.seleccionarBarco(2);
        p.usarHabilidad();
        assertEquals(4, propio(p, 2).vida());
    }

    @Test
    void h04_habilidadUnaVezPorTurno() {
        GestorPartida p = partida(mapa());
        p.seleccionarBarco(4);
        p.usarHabilidad();
        assertThrows(HabilidadNoDisponibleException.class, p::usarHabilidad);
    }

    // ---------- esperas (cooldowns) ----------

    /** Termina el turno de J1 y el de J2 sin hacer nada: vuelve a J1. */
    static void pasarRonda(GestorPartida p) {
        p.terminarTurno();
        p.terminarTurno();
    }

    @Test
    void c01_acorazadoNoPuedeDispararElTurnoSiguienteAlDisparo() {
        // Manual: "Espera tras disparar: 1 turno" para el acorazado
        GestorPartida p = partida(mapa(), b(1, 'A', 1, 2, 1), b(7, 'A', 2, 6, 1));
        p.seleccionarBarco(1);
        p.disparar(7);
        pasarRonda(p);
        p.seleccionarBarco(1);
        assertTrue(p.getObjetivosValidos().isEmpty(), "en su turno siguiente el acorazado tendría que estar esperando");
    }

    @Test
    void c02_habilidadVuelveAEstarDisponibleTresTurnosDespuesDeUsarla() {
        // Se usa en el turno 1 y vuelve en el turno 4 (la espera de 3 cuenta el turno en que se usó)
        GestorPartida p = partida(mapa());
        p.seleccionarBarco(4);
        p.usarHabilidad(); // turno 1 de J1
        List<Boolean> disponible = new ArrayList<>();
        for (int turno = 2; turno <= 5; turno++) {
            pasarRonda(p);
            boolean ok;
            try { p.seleccionarBarco(4); p.usarHabilidad(); ok = true; } catch (HabilidadNoDisponibleException e) { ok = false; }
            disponible.add(ok);
            if (ok) break;
        }
        assertEquals(List.of(false, false, true), disponible);
    }

    // ---------- revelado del tirador ----------

    @Test
    void r01_elTiradorQuedaVisibleParaElRivalEnSuTurnoSiguienteYDespuesSeOculta() {
        // Acorazado J1 en (0,1) dispara a destructor J2 en (4,1) (dist 4). El J2 no ve el acorazado (visión destructor 2)
        GestorPartida p = partida(mapa(), b(1, 'A', 1, 0, 1), b(2, 'C', 1, 3, 1), b(10, 'D', 2, 4, 1));
        p.seleccionarBarco(1);
        assertTrue(ids(p.getObjetivosValidos()).contains(10));
        p.disparar(10);
        p.terminarTurno(); // turno J2
        assertEquals(2, p.getJugadorActivo().numero());
        assertNotNull(enemigo(p, 1), "el acorazado que disparó queda visible para J2");
        p.terminarTurno(); // turno J1
        p.terminarTurno(); // turno J2 de nuevo
        assertNull(enemigo(p, 1), "un turno después ya no se ve");
    }

    // ---------- movimiento ----------

    @Test
    void m01_choqueConEnemigoOcultoFrenaAntesYCobraSoloLosPasosHechos() {
        // Destructor J1 en (2,2) va hacia (4,0). El enemigo en (4,0) está oculto detrás de la isla de (3,1)
        GestorPartida p = partida(mapa("I,3,1"), b(1, 'A', 1, 0, 9), b(2, 'C', 1, 0, 8), b(3, 'C', 1, 0, 7),
                b(4, 'D', 1, 2, 2), b(10, 'D', 2, 4, 0));
        assertNull(enemigo(p, 10), "detrás de la isla");
        p.seleccionarBarco(4);
        p.agregarPaso(new Posicion(3, 2));
        p.agregarPaso(new Posicion(4, 1));
        // planificar no mueve nada: el enemigo sigue oculto
        p.agregarPaso(new Posicion(4, 0)); // casilla con enemigo oculto
        p.confirmarMovimiento();
        assertEquals(new Posicion(4, 1), propio(p, 4).posicion(), "frena en el paso anterior");
        assertEquals(4, p.getJugadorActivo().puntosMovimiento(), "cobra 2 pasos (6-2)");
        assertNotNull(enemigo(p, 10), "el enemigo queda a la vista");
    }

    @Test
    void m02_noSePuedePlanificarSobreUnEnemigoVisible() {
        GestorPartida p = partida(mapa(), b(4, 'D', 1, 2, 2), b(10, 'D', 2, 3, 2));
        p.seleccionarBarco(4);
        assertThrows(MovimientoInvalidoException.class, () -> p.agregarPaso(new Posicion(3, 2)));
    }

    @Test
    void m03_diagonalEntreDosPiedrasBloqueada_yConUnaSolaSePuede() {
        GestorPartida p = partida(mapa("P,3,3", "P,4,2"), b(4, 'D', 1, 3, 2));
        p.seleccionarBarco(4);
        assertThrows(MovimientoInvalidoException.class, () -> p.agregarPaso(new Posicion(4, 3)));
        GestorPartida q = partida(mapa("P,3,3"), b(4, 'D', 1, 3, 2));
        q.seleccionarBarco(4);
        q.agregarPaso(new Posicion(4, 3));
    }

    @Test
    void m04_puntosDeMovimientoCompartidosEntreLaFlota() {
        // Acorazado cuesta 3 por casilla: 2 casillas = 6 puntos; ya no queda para nadie
        GestorPartida p = partida(mapa());
        p.seleccionarBarco(1);
        p.agregarPaso(new Posicion(1, 0));
        p.agregarPaso(new Posicion(2, 0));
        p.confirmarMovimiento();
        p.seleccionarBarco(4);
        assertThrows(MovimientoInvalidoException.class, () -> p.agregarPaso(new Posicion(1, 3)));
    }

    @Test
    void m05_alcanzablesYAgregarPasoCoinciden() {
        GestorPartida p = partida(mapa("I,3,1", "P,3,2", "I,4,4"), b(4, 'D', 1, 2, 2), b(10, 'D', 2, 4, 2));
        p.seleccionarBarco(4);
        Set<Posicion> alcanzables = p.getCasillasAlcanzables();
        for (int f = 0; f < 10; f++) for (int c = 0; c < 10; c++) {
            Posicion destino = new Posicion(f, c);
            if (destino.distanciaChebyshev(new Posicion(2, 2)) != 1) continue;
            boolean ok;
            try { p.agregarPaso(destino); ok = true; p.deshacerPaso(); } catch (MovimientoInvalidoException e) { ok = false; }
            assertEquals(ok, alcanzables.contains(destino), "vecina " + destino);
        }
    }

    // ---------- dominación y fin ----------

    @Test
    void v01_dominacionSumaSoloSiElRivalNoTieneBarcosEnLaZona() {
        GestorPartida p = partida(mapa(), b(4, 'D', 1, 4, 4));
        p.terminarTurno();
        assertEquals(1, p.getRival().puntosDominacion());
        GestorPartida q = partida(mapa(), b(4, 'D', 1, 4, 4), b(10, 'D', 2, 5, 5));
        q.terminarTurno();
        assertEquals(0, q.getRival().puntosDominacion());
    }

    @Test
    void v02_seisPuntosDeDominacionGana() {
        GestorPartida p = partida(mapa(), b(4, 'D', 1, 4, 4));
        for (int i = 0; i < 5; i++) pasarRonda(p);
        p.terminarTurno();
        assertTrue(p.estaTerminada());
        assertEquals(MotivoFin.DOMINACION, p.getResultado().motivo());
        assertEquals("Ana", p.getResultado().ganador());
    }

    @Test
    void v03_hundirUnBarcoNoTerminaSiQuedanOtros() {
        GestorPartida p = partida(mapa(),
                b(1, 'A', 1, 2, 0), b(7, 'A', 2, 9, 9),
                new DatosBarco(10, 'D', 2, new Posicion(5, 0), 1, 0, 0, 4, false, false));
        // al rival le quedan otros barcos vivos: hundir uno no termina la partida
        p.seleccionarBarco(1);
        p.disparar(10);
        assertFalse(p.estaTerminada());
    }

    @Test
    void v04_limiteDeTurnos() {
        GestorPartida p = partida(mapa());
        for (int i = 0; i < 39; i++) pasarRonda(p);
        assertEquals(40, p.getRonda());
        p.terminarTurno();
        assertFalse(p.estaTerminada(), "falta el turno 40 de J2");
        p.terminarTurno();
        assertTrue(p.estaTerminada());
        assertEquals(MotivoFin.EMPATE, p.getResultado().motivo());
    }

    // ---------- guardado ----------

    @Test
    void g01_elTiradorRevelado_seMantieneAlGuardarYCargar() {
        GestorPartida p = partida(mapa(), b(1, 'A', 1, 0, 1), b(2, 'C', 1, 3, 1), b(10, 'D', 2, 4, 1));
        p.seleccionarBarco(1);
        p.disparar(10);
        p.terminarTurno();
        assertTrue(p.puedeGuardarse());
        GestorPartida cargada = GestorPartida.restaurar(p.crearInstantanea(), mapa());
        assertNotNull(enemigo(cargada, 1));
    }

    @Test
    void g02_esperaDelAcorazadoSeMantieneAlGuardarYCargar() {
        GestorPartida p = partida(mapa(), b(1, 'A', 1, 2, 1), b(7, 'A', 2, 6, 1));
        p.seleccionarBarco(1);
        p.disparar(7);
        p.terminarTurno();
        GestorPartida cargada = GestorPartida.restaurar(p.crearInstantanea(), mapa());
        assertEquals(p.crearInstantanea().barcos(), cargada.crearInstantanea().barcos());
    }
}
