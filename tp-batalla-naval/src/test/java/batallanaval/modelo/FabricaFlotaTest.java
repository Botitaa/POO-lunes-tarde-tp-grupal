package batallanaval.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import org.junit.jupiter.api.Test;

class FabricaFlotaTest {

    @Test
    void testCrearFlotaEstandar() {
        int primerId = 1;
        List<Barco> flota = FabricaFlota.crearFlotaEstandar(primerId);

        assertNotNull(flota);
        assertEquals(6, flota.size());

        // Verifica que los ids sean correlativos
        assertEquals(1, flota.get(0).getId());
        assertEquals(2, flota.get(1).getId());
        assertEquals(3, flota.get(2).getId());
        assertEquals(4, flota.get(3).getId());
        assertEquals(5, flota.get(4).getId());
        assertEquals(6, flota.get(5).getId());

        // Cuenta cuantos hay de cada tipo
        int acorazados = 0;
        int cruceros = 0;
        int destructores = 0;

        for (Barco barco : flota) {
            if (barco instanceof Acorazado) {
                acorazados++;
            } else if (barco instanceof Crucero) {
                cruceros++;
            } else if (barco instanceof Destructor) {
                destructores++;
            }
        }

        assertEquals(1, acorazados);
        assertEquals(2, cruceros);
        assertEquals(3, destructores);
    }
}
