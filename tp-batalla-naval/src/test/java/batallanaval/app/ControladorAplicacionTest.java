package batallanaval.app;

import batallanaval.controlador.MotivoFin;
import batallanaval.controlador.ResultadoPartida;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests de lo que el controlador de la aplicación calcula sin abrir ventanas. El flujo entre pantallas
 * se prueba a mano (ver la descripción del PR del #203).
 */
class ControladorAplicacionTest {

    @Test
    void elCartelDeFinNombraAlGanadorElMotivoYLasRondas() {
        ResultadoPartida resultado = new ResultadoPartida(LocalDateTime.now(), "Ana", "Beto", "Ana", 5,
                MotivoFin.HUNDIMIENTO);

        assertEquals("Ganó Ana (HUNDIMIENTO) en 5 rondas.", ControladorAplicacion.textoResultado(resultado));
    }

    @Test
    void elCartelDeFinAvisaElEmpate() {
        ResultadoPartida resultado = new ResultadoPartida(LocalDateTime.now(), "Ana", "Beto", null, 40,
                MotivoFin.EMPATE);

        assertEquals("Empate (EMPATE) en 40 rondas.", ControladorAplicacion.textoResultado(resultado));
    }
}