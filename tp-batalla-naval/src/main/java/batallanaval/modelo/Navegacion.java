package batallanaval.modelo;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

public final class Navegacion {
    private Navegacion() {
        // Constructor privado para evitar instanciación
    }

    public static Set<Posicion> casillasAlcanzables(Tablero tablero, Posicion origen, int pasosMax, Set<Posicion> ocupadas) {
        if (pasosMax < 0) {
            throw new IllegalArgumentException("El número de pasos máximos no puede ser negativo.");
        }
        if (!tablero.estaDentro(origen)) {
            throw new IllegalArgumentException("La posición de origen está fuera del tablero.");
        }

        Set<Posicion> visitadas = new HashSet<>();
        visitadas.add(origen);

        List<Nodo> pendientes = new ArrayList<>();
        pendientes.add(new Nodo(origen, 0));
        int siguiente = 0; // variable para contar los nodos en el siguiente nivel

        while (siguiente < pendientes.size()) {
            Nodo actual = pendientes.get(siguiente);
            siguiente++;

            if (actual.getPasos() == pasosMax) {
                continue; // No se pueden explorar más pasos desde esta posición
            }
            for (Posicion vecina : tablero.getVecinos(actual.getPosicion())) {
                boolean sePuedeEntrar = !visitadas.contains(vecina) && !ocupadas.contains(vecina) && !tablero.esDiagonalBloqueada(actual.getPosicion(), vecina);
                if (sePuedeEntrar) {
                    visitadas.add(vecina);
                    pendientes.add(new Nodo(vecina, actual.getPasos() + 1));
                }
            }
        }
        visitadas.remove(origen); // Eliminar la posición de origen del conjunto de visitadas
        return visitadas;

    }
    private static class Nodo {
        private final Posicion posicion;
        private final int pasos;

        Nodo(Posicion posicion, int pasos) {
            this.posicion = posicion;
            this.pasos = pasos;
        }
        Posicion getPosicion() {return posicion;}
        int getPasos() {return pasos;}
    }
}
