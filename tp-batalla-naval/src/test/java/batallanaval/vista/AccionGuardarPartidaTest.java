package batallanaval.vista;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import batallanaval.controlador.FasePartida;
import batallanaval.controlador.GestorPartida;
import batallanaval.excepciones.PersistenciaException;
import batallanaval.modelo.FabricaFlota;
import batallanaval.modelo.Jugador;
import batallanaval.modelo.Tablero;
import batallanaval.persistencia.CabeceraGuardado;

/**
 * Prueba que {@link AccionGuardarPartida} pide el nombre, no pisa guardados sin permiso y muestra el
 * error cuando el repositorio lanza {@link PersistenciaException}. No abre diálogos ni toca archivos:
 * el nombre y la confirmación entran por los puntos de enganche de paquete, y el repositorio es falso.
 */
class AccionGuardarPartidaTest {

    private final RepositorioPartidasFalso repositorio = new RepositorioPartidasFalso();
    private final List<String> errores = new ArrayList<>();
    private final List<String> avisos = new ArrayList<>();
    private final List<String> pisadosConsultados = new ArrayList<>();

    private GestorPartida partida = partidaGuardable();
    private String nombreEscrito = "partida1";
    private boolean permitePisar = true;
    private int vecesQueSePidioNombre = 0;

    private AccionGuardarPartida crearAccion() {
        Supplier<String> pedirNombre = () -> {
            vecesQueSePidioNombre++;
            return nombreEscrito;
        };
        return new AccionGuardarPartida(repositorio, () -> partida, errores::add, avisos::add, pedirNombre,
                nombre -> {
                    pisadosConsultados.add(nombre);
                    return permitePisar;
                });
    }

    @Test
    void guardaConElNombreEscritoYAvisaQueSeGuardo() {
        nombreEscrito = "  mi-partida_2  ";

        crearAccion().run();

        assertEquals(List.of("mi-partida_2"), repositorio.nombresGuardados, "el nombre se guarda sin espacios sobrantes");
        assertSame(partida, repositorio.ultimaPartidaGuardada);
        assertEquals(List.of("Partida guardada como \"mi-partida_2\"."), avisos);
        assertTrue(errores.isEmpty());
    }

    @Test
    void siElRepositorioLanzaPersistenciaExceptionSeMuestraSuMensaje() {
        repositorio.error = new PersistenciaException("Nombre de partida invalido: ¡hola!");
        nombreEscrito = "¡hola!";

        crearAccion().run();

        assertEquals(List.of("Nombre de partida invalido: ¡hola!"), errores);
        assertTrue(avisos.isEmpty(), "si falla no se avisa que se guardó");
    }

    @Test
    void siFallaLaEscrituraElJuegoSigueYSeMuestraElError() {
        repositorio.error = new PersistenciaException("Error al guardar el archivo: datos/partidas/x.sav",
                new java.io.IOException("disco lleno"));

        crearAccion().run();

        assertEquals(List.of("Error al guardar el archivo: datos/partidas/x.sav"), errores);
    }

    @Test
    void siElJugadorCancelaElDialogoNoSeGuardaNiSeMuestraNada() {
        nombreEscrito = null;

        crearAccion().run();

        assertTrue(repositorio.nombresGuardados.isEmpty());
        assertTrue(errores.isEmpty());
        assertTrue(avisos.isEmpty());
    }

    @Test
    void siElNombreYaExistePreguntaAntesDePisar() {
        repositorio.guardados.add(cabecera("partida1"));
        permitePisar = true;

        crearAccion().run();

        assertEquals(List.of("partida1"), pisadosConsultados);
        assertEquals(List.of("partida1"), repositorio.nombresGuardados);
    }

    @Test
    void siElJugadorNoQuierePisarNoSeGuarda() {
        repositorio.guardados.add(cabecera("partida1"));
        permitePisar = false;

        crearAccion().run();

        assertEquals(List.of("partida1"), pisadosConsultados);
        assertTrue(repositorio.nombresGuardados.isEmpty());
        assertTrue(avisos.isEmpty());
    }

    @Test
    void siElNombreEsNuevoNoPreguntaSiSePisa() {
        crearAccion().run();

        assertTrue(pisadosConsultados.isEmpty());
    }

    @Test
    void sinPartidaMuestraErrorYNoPideNombre() {
        partida = null;

        crearAccion().run();

        assertEquals(List.of(AccionGuardarPartida.MENSAJE_SIN_PARTIDA), errores);
        assertEquals(0, vecesQueSePidioNombre);
    }

    @Test
    void siLaPartidaNoSePuedeGuardarAhoraMuestraErrorYNoPideNombre() {
        partida = partidaEnDespliegue();

        crearAccion().run();

        assertEquals(List.of(AccionGuardarPartida.MENSAJE_NO_SE_PUEDE_GUARDAR), errores);
        assertEquals(0, vecesQueSePidioNombre);
        assertTrue(repositorio.nombresGuardados.isEmpty());
    }

    @Test
    void unErrorInesperadoNoCortaElJuegoYSeMuestraUnMensajeGenerico() {
        AccionGuardarPartida accion = new AccionGuardarPartida(repositorio, () -> partida, errores::add,
                avisos::add, () -> {
                    throw new IllegalStateException("bug");
                }, nombre -> true);

        accion.run();

        assertEquals(List.of(EjecutorAcciones.MENSAJE_ERROR_INESPERADO), errores);
    }

    @Test
    void noSeAceptanArgumentosNulos() {
        assertNotNull(crearAccion());
        assertThrows(NullPointerException.class,
                () -> new AccionGuardarPartida(null, () -> partida, errores::add, avisos::add));
        assertThrows(NullPointerException.class,
                () -> new AccionGuardarPartida(repositorio, null, errores::add, avisos::add));
        assertThrows(NullPointerException.class,
                () -> new AccionGuardarPartida(repositorio, () -> partida, null, avisos::add));
        assertThrows(NullPointerException.class,
                () -> new AccionGuardarPartida(repositorio, () -> partida, errores::add, null));
    }

    // Ayudas

    private static CabeceraGuardado cabecera(String nombre) {
        return new CabeceraGuardado(1, nombre, LocalDateTime.of(2026, 10, 5, 14, 32), "Bota", "Santi", 3,
                FasePartida.EN_JUEGO);
    }

    /** Partida que dice que se puede guardar (la de verdad lo decide según la fase y el turno). */
    private static GestorPartida partidaGuardable() {
        return new GestorPartida(jugador("Bota", 1), jugador("Santi", 2), Tablero.crearMapaEstandar()) {
            @Override
            public boolean puedeGuardarse() {
                return true;
            }
        };
    }

    /** Partida recién creada: está en el despliegue, donde no se puede guardar. */
    private static GestorPartida partidaEnDespliegue() {
        return new GestorPartida(jugador("Bota", 1), jugador("Santi", 2), Tablero.crearMapaEstandar());
    }

    private static Jugador jugador(String nombre, int numero) {
        return new Jugador(numero, nombre, FabricaFlota.crearFlotaEstandar(numero == 1 ? 1 : 7));
    }
}
