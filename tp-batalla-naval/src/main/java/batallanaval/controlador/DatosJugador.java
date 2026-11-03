package batallanaval.controlador;

import java.util.List;

public record DatosJugador(
        int numero,
        String nombre,
        int puntosMovimiento,
        int puntosDominacion,
        List<String> resumenPendiente
) {
}
