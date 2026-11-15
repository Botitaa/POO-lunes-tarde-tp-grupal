package batallanaval;

import static org.junit.jupiter.api.Assertions.assertEquals;

import batallanaval.app.Main;
import org.junit.jupiter.api.Test;

/** Verifica que el proyecto compila y que JUnit 5 corre. */
class ProyectoTest {

    @Test
    void elTituloDeLaVentanaEsElNombreDelJuego() {
        assertEquals("Batalla Naval Táctica", Main.TITULO);
    }
}
