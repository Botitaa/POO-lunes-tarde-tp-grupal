package batallanaval.modelo;

import batallanaval.excepciones.MovimientoInvalidoException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Jugador {

    private final int numero;
    private final String nombre;
    private final List<Barco> flota;
    private final Map<Integer, Barco> barcosPorId;
    private int puntosMovimiento;
    private int puntosDominacion;
    private final ResumenTurno resumen;

    public Jugador(int numero, String nombre, List<Barco> flota) {

        if(nombre == null || nombre.isBlank()){
            throw new IllegalArgumentException("el nombre no puede ser vacio o null");
        }

        if (numero != 1 && numero != 2){
            throw new IllegalArgumentException("el numero del jugador tiene que ser 1 o 2");
        }

        if (flota == null || flota.isEmpty()){
            throw new IllegalArgumentException("la flota no puede estar vacia");
        }

        this.numero = numero;
        this.nombre = nombre;
        this.flota = List.copyOf(flota);

        resumen = new ResumenTurno();
        barcosPorId = new HashMap<>();

        for (Barco barco: this.flota){
            if(barcosPorId.put(barco.getId(),barco) != null){
                throw new IllegalArgumentException("hay dos barcos con el mismo id: " + barco.getId());
            }

        }
    }

    public ResumenTurno getResumen() {
        return resumen;
    }

    public int getPuntosDominacion() {
        return puntosDominacion;
    }

    public int getPuntosMovimiento() {
        return puntosMovimiento;
    }

    public Barco getBarcoPorId(int id) {

        return barcosPorId.get(id);
    }

    public List<Barco> getFlota() {
        return List.copyOf(flota);
    }

    public String getNombre() {
        return nombre;
    }

    public int getNumero() {
        return numero;
    }

    public List<Barco> getBarcosVivos() {

        List<Barco> barcosVivos = new ArrayList<>();

        for(Barco barco: flota){
            if (!barco.estaHundido()){
                barcosVivos.add(barco);
            }
        }

        return barcosVivos;
    }

    public boolean tieneBarcosVivos(){

        for(Barco barco: flota){
            if (!barco.estaHundido()) {return true;}
        }

        return false;
    }

    public int getVidaTotal(){
        int vidaTotal = 0;

        for(Barco barco: flota){
            vidaTotal += barco.getVida();
        }

        return vidaTotal;
    }

    public boolean tienePuntos(int n){return puntosMovimiento >= n;}

    public void gastarPuntos(int n){

        if (n < 0) {
            throw new IllegalArgumentException("tu movimiento no puede ser negativo");
        } else if(puntosMovimiento < n){
            throw new MovimientoInvalidoException("no tenes puntos suficientes");
        } else{
            puntosMovimiento -= n;
        }
    }

    public void reponerPuntos(int n ){

        if (n < 0) {
            throw new IllegalArgumentException("tu movimiento no puede ser negativo");
        } else {
            puntosMovimiento = n;
        }
    }

    public void sumarPuntoDominacion(){puntosDominacion += 1;}

}
