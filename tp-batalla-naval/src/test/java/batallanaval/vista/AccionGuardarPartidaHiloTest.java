package batallanaval.vista;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.Test;

import batallanaval.controlador.GestorPartida;
import batallanaval.controlador.InstantaneaPartida;
import batallanaval.excepciones.PersistenciaException;
import batallanaval.modelo.FabricaFlota;
import batallanaval.modelo.Jugador;
import batallanaval.modelo.Posicion;
import batallanaval.modelo.Tablero;

/**
 * Prueba que {@link AccionGuardarPartida} guarda a través de una {@link TareaGuardar} lanzada en otro
 * hilo: arma la instantánea antes de lanzarlo, bloquea al empezar, libera al terminar (salga bien o
 * mal) y no deja lanzar dos guardados a la vez. El "hilo" se reemplaza por un lanzador manual que guarda la tarea sin correrla, así el test
 * decide cuándo termina y no depende del reloj.
 */
class AccionGuardarPartidaHiloTest {

    private final RepositorioPartidasFalso repositorio = new RepositorioPartidasFalso();
    private final List<String> errores = new ArrayList<>();
    private final List<String> avisos = new ArrayList<>();
    private final List<String> eventos = new ArrayList<>();
    private final List<Runnable> tareasPendientes = new ArrayList<>();

    private final GestorPartida partida = partidaGuardable();
    private Executor lanzador = tareasPendientes::add;

    private AccionGuardarPartida crearAccion() {
        return new AccionGuardarPartida(repositorio, () -> partida, errores::add, avisos::add,
                () -> "partida1", nombre -> true,
                () -> eventos.add("empezar"), () -> eventos.add("terminar"),
                lanzador, Runnable::run);
    }

    @Test
    void alGuardarBloqueaYLanzaLaTareaPeroNoEscribeTodavia() {
        crearAccion().run();

        assertEquals(List.of("empezar"), eventos, "se bloquea antes de lanzar el hilo");
        assertEquals(1, tareasPendientes.size(), "se lanzó una tarea");
        assertTrue(repositorio.nombresGuardados.isEmpty(), "la escritura la hace el hilo, no quien llama");
        assertTrue(avisos.isEmpty());
    }

    @Test
    void laInstantaneaSeArmaAntesDeLanzarElHilo() {
        // Partida que anota cuándo le piden la instantánea, en la misma lista que empezar y terminar
        GestorPartida partidaQueAnota = new GestorPartida(jugador("Bota", 1), jugador("Santi", 2),
                Tablero.crearMapaEstandar()) {
            @Override
            public boolean puedeGuardarse() {
                return true;
            }

            @Override
            public InstantaneaPartida crearInstantanea() {
                eventos.add("instantanea");
                return super.crearInstantanea();
            }
        };
        lanzador = tarea -> {
            eventos.add("lanzar");
            tareasPendientes.add(tarea);
        };
        AccionGuardarPartida accion = new AccionGuardarPartida(repositorio, () -> partidaQueAnota,
                errores::add, avisos::add, () -> "partida1", nombre -> true,
                () -> eventos.add("empezar"), () -> eventos.add("terminar"), lanzador, Runnable::run);

        accion.run();
        assertEquals(List.of("instantanea", "empezar", "lanzar"), eventos,
                "la copia se arma en el hilo de Swing, antes de que exista el hilo de fondo");

        tareasPendientes.get(0).run();
        assertEquals(List.of("instantanea", "empezar", "lanzar", "terminar"), eventos,
                "el hilo de fondo no vuelve a pedir la instantánea: solo escribe la que recibió");
    }

    @Test
    void siLaPartidaCambiaMientrasElHiloNoTerminoSeGuardaElEstadoDelMomentoDeGuardar() {
        InstantaneaPartida alTocarGuardar = partida.crearInstantanea();
        crearAccion().run();

        // El hilo de fondo todavía no escribió y el jugador sigue tocando el tablero
        partida.ubicarBarco(1, new Posicion(0, 0));
        tareasPendientes.get(0).run();

        assertEquals(alTocarGuardar, repositorio.ultimaInstantaneaGuardada,
                "se guarda la foto del momento en que se tocó Guardar");
        assertNotEquals(partida.crearInstantanea(), repositorio.ultimaInstantaneaGuardada,
                "la partida sí cambió: el cambio no se coló en lo que escribe el hilo");
    }

    @Test
    void cuandoLaTareaTerminaBienAvisaYLibera() {
        crearAccion().run();

        tareasPendientes.get(0).run();

        assertEquals(List.of("partida1"), repositorio.nombresGuardados);
        assertEquals(List.of("Partida guardada como \"partida1\"."), avisos);
        assertEquals(List.of("empezar", "terminar"), eventos);
        assertTrue(errores.isEmpty());
    }

    @Test
    void cuandoLaTareaFallaMuestraElErrorYLiberaDeTodosModos() {
        repositorio.error = new PersistenciaException("Error al guardar el archivo: datos/partidas/partida1.sav");
        crearAccion().run();

        tareasPendientes.get(0).run();

        assertEquals(List.of("Error al guardar el archivo: datos/partidas/partida1.sav"), errores);
        assertTrue(avisos.isEmpty());
        assertEquals(List.of("empezar", "terminar"), eventos);
    }

    @Test
    void mientrasSeGuardaNoSeLanzaOtroGuardado() {
        AccionGuardarPartida accion = crearAccion();
        accion.run();

        accion.run();

        assertEquals(1, tareasPendientes.size(), "el segundo pedido no lanza otro hilo");
        assertEquals(List.of(AccionGuardarPartida.MENSAJE_YA_SE_ESTA_GUARDANDO), errores);
        assertEquals(List.of("empezar"), eventos, "tampoco bloquea de nuevo");
    }

    @Test
    void cuandoTerminaElGuardadoSePuedeVolverAGuardar() {
        AccionGuardarPartida accion = crearAccion();
        accion.run();
        tareasPendientes.get(0).run();

        accion.run();
        tareasPendientes.get(1).run();

        assertEquals(List.of("partida1", "partida1"), repositorio.nombresGuardados);
        assertTrue(errores.isEmpty());
    }

    @Test
    void siNoSePuedeLanzarElHiloSeLiberaYElJugadorVeUnMensajeGenerico() {
        lanzador = tarea -> {
            throw new IllegalStateException("no hay hilos");
        };
        AccionGuardarPartida accion = crearAccion();

        accion.run();

        assertEquals(List.of("empezar", "terminar"), eventos, "no se puede quedar bloqueado para siempre");
        assertEquals(List.of(EjecutorAcciones.MENSAJE_ERROR_INESPERADO), errores);
    }

    @Test
    void sinQueSePuedaGuardarNoSeBloqueaNadaNiSeLanzaNingunHilo() {
        AccionGuardarPartida accion = new AccionGuardarPartida(repositorio, () -> partidaEnDespliegue(),
                errores::add, avisos::add, () -> "partida1", nombre -> true,
                () -> eventos.add("empezar"), () -> eventos.add("terminar"), lanzador, Runnable::run);

        accion.run();

        assertEquals(List.of(AccionGuardarPartida.MENSAJE_NO_SE_PUEDE_GUARDAR), errores);
        assertTrue(eventos.isEmpty());
        assertTrue(tareasPendientes.isEmpty());
    }

    // Ayudas

    private static GestorPartida partidaGuardable() {
        return new GestorPartida(jugador("Bota", 1), jugador("Santi", 2), Tablero.crearMapaEstandar()) {
            @Override
            public boolean puedeGuardarse() {
                return true;
            }
        };
    }

    private static GestorPartida partidaEnDespliegue() {
        return new GestorPartida(jugador("Bota", 1), jugador("Santi", 2), Tablero.crearMapaEstandar());
    }

    private static Jugador jugador(String nombre, int numero) {
        return new Jugador(numero, nombre, FabricaFlota.crearFlotaEstandar(numero == 1 ? 1 : 7));
    }
}
