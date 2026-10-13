package batallanaval.modelo;

import java.util.ArrayList;
import java.util.List;

/**
 * Fábrica utilitaria para crear la flota estándar de un jugador.
 * <p>
 * Aplica el patrón Creador (GRASP): centraliza la instanciación de las subclases
 * concretas para que {@code Jugador} dependa únicamente de la abstracción {@link Barco}.
 * </p>
 */
public class FabricaFlota {

    /**
     * Crea la flota estándar de 6 barcos (1 acorazado, 2 cruceros y 3 destructores)
     * sin posición asignada y con identificadores correlativos a partir de {@code primerId}.
     *
     * @param primerId identificador inicial para los barcos de la flota.
     * @return lista con los 6 barcos de la flota estándar.
     */
    public static List<Barco> crearFlotaEstandar(int primerId) {
        List<Barco> flota = new ArrayList<>();
        int id = primerId;

        flota.add(new Acorazado(id++));
        flota.add(new Crucero(id++));
        flota.add(new Crucero(id++));
        flota.add(new Destructor(id++));
        flota.add(new Destructor(id++));
        flota.add(new Destructor(id++));

        return flota;
    }
}
