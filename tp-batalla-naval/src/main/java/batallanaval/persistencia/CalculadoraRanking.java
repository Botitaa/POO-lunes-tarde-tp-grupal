package batallanaval.persistencia;

import batallanaval.controlador.ResultadoPartida;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Clase utilitaria encargada de consolidar y clasificar el ranking de jugadores
 * a partir de un listado de resultados de partidas.
 */
public final class CalculadoraRanking {

    private CalculadoraRanking() {
    }

    /**
     * Agrupa los resultados por jugador acumulando partidas jugadas, ganadas, perdidas
     * y empatadas, y devuelve la lista ordenada por: mayor cantidad de partidas ganadas,
     * mayor porcentaje de victorias y nombre alfabético.
     *
     * @param resultados lista de resultados de partidas finalizadas a procesar.
     * @return lista inmutable de estadísticas de jugadores ordenada según los criterios del ranking.
     */
    public static List<EstadisticaJugador> calcular(List<ResultadoPartida> resultados) {
        if (resultados == null || resultados.isEmpty()) {
            return List.of();
        }

        Map<String, Acumulador> mapa = new HashMap<>();

        for (ResultadoPartida res : resultados) {
            if (res == null) {
                continue;
            }

            String j1 = res.jugador1();
            String j2 = res.jugador2();

            Acumulador acc1 = mapa.computeIfAbsent(j1, k -> new Acumulador());
            Acumulador acc2 = mapa.computeIfAbsent(j2, k -> new Acumulador());

            acc1.jugadas++;
            acc2.jugadas++;

            if (res.esEmpate() || res.ganador() == null) {
                acc1.empatadas++;
                acc2.empatadas++;
            } else if (res.ganador().equals(j1)) {
                acc1.ganadas++;
                acc2.perdidas++;
            } else if (res.ganador().equals(j2)) {
                acc2.ganadas++;
                acc1.perdidas++;
            }
        }

        List<EstadisticaJugador> ranking = new ArrayList<>();
        for (Map.Entry<String, Acumulador> entry : mapa.entrySet()) {
            Acumulador acc = entry.getValue();
            ranking.add(new EstadisticaJugador(
                    entry.getKey(),
                    acc.jugadas,
                    acc.ganadas,
                    acc.perdidas,
                    acc.empatadas
            ));
        }

        Comparator<EstadisticaJugador> comparador = Comparator
                .comparingInt(EstadisticaJugador::ganadas).reversed()
                .thenComparing(Comparator.comparingDouble(EstadisticaJugador::porcentajeVictorias).reversed())
                .thenComparing(EstadisticaJugador::nombre);

        ranking.sort(comparador);

        return List.copyOf(ranking);
    }

    private static class Acumulador {
        private int jugadas;
        private int ganadas;
        private int perdidas;
        private int empatadas;
    }
}
