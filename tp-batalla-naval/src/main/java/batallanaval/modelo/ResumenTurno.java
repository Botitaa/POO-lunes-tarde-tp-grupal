package batallanaval.modelo;

import java.util.ArrayList;
import java.util.List;

/**
 * Almacena los mensajes de los eventos ocurridos al jugador durante el turno rival 
 * (disparos recibidos, barcos hundidos). Se muestra al volver del blackout y se limpia.
 */
public class ResumenTurno {

    private final List<String> mensajes;

    /**
     * Constructor por defecto que inicializa la lista de mensajes del turno.
     */
    public ResumenTurno() {
        this.mensajes = new ArrayList<>();
    }

    /**
     * Registra un disparo recibido por un barco del jugador.
     * 
     * @param objetivo Barco propio que recibió el impacto.
     * @param danio Cantidad de daño recibido.
     * @param origen Posición desde donde disparó el rival.
     */
    public void agregarDisparoRecibido(Barco objetivo, int danio, Posicion origen) {
        String mensaje = String.format("Tu %c (%d,%d) recibió %d de daño desde %s",
                objetivo.getSimbolo(),
                objetivo.getPosicion().getFila(),
                objetivo.getPosicion().getColumna(),
                danio,
                origen.toString());
        mensajes.add(mensaje);
    }

    /**
     * Registra que un barco propio ha sido hundido por el rival.
     * 
     * @param barco Barco propio hundido.
     * @param posicion Posición en la que se encontraba al hundirse.
     */
    public void agregarBarcoHundido(Barco barco, Posicion posicion) {
        String mensaje = String.format("¡Tu %c en la posición %s fue hundido!",
                barco.getSimbolo(),
                posicion.toString());
        mensajes.add(mensaje);
    }

    /**
     * Devuelve una vista de solo lectura de los mensajes acumulados para proteger el encapsulamiento.
     * 
     * @return Lista inmodificable de strings con los mensajes del turno.
     */
    public List<String> getMensajes() {
        return List.copyOf(mensajes);
    }

    /**
     * Limpia todos los mensajes acumulados al finalizar la visualización del resumen.
     */
    public void limpiar() {
        mensajes.clear();
    }

    /**
     * Comprueba si hay mensajes pendientes de mostrar al jugador.
     * 
     * @return true si no hay mensajes, false en caso contrario.
     */
    public boolean estaVacio() {
        return mensajes.isEmpty();
    }
}