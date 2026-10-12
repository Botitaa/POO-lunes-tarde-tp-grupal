package batallanaval.modelo;

import java.util.ArrayList;
import java.util.List;

public class FabricaFlota {

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
