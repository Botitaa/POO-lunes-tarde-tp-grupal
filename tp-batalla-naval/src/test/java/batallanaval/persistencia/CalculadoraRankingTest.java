package batallanaval.persistencia;

import batallanaval.controlador.MotivoFin;
import batallanaval.controlador.ResultadoPartida;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CalculadoraRankingTest {

    @Test
    void calcularConListaVaciaONulaRetornaListaVacia() {
        assertTrue(CalculadoraRanking.calcular(null).isEmpty());
        assertTrue(CalculadoraRanking.calcular(List.of()).isEmpty());
    }

    @Test
    void calcularAcumulaPartidasGanadasPerdidasYEmpatadasCorrectamente() {
        LocalDateTime ahora = LocalDateTime.now();
        List<ResultadoPartida> resultados = List.of(
                new ResultadoPartida(ahora, "Bota", "Santi", "Bota", 10, MotivoFin.HUNDIMIENTO),
                new ResultadoPartida(ahora, "Bota", "Santi", "Santi", 15, MotivoFin.DOMINACION),
                new ResultadoPartida(ahora, "Bota", "Santi", null, 40, MotivoFin.EMPATE)
        );

        List<EstadisticaJugador> ranking = CalculadoraRanking.calcular(resultados);

        assertEquals(2, ranking.size());

        EstadisticaJugador bota = ranking.stream().filter(e -> e.nombre().equals("Bota")).findFirst().orElseThrow();
        assertEquals(3, bota.jugadas());
        assertEquals(1, bota.ganadas());
        assertEquals(1, bota.perdidas());
        assertEquals(1, bota.empatadas());
        assertEquals(100.0 / 3.0, bota.porcentajeVictorias(), 0.0001);

        EstadisticaJugador santi = ranking.stream().filter(e -> e.nombre().equals("Santi")).findFirst().orElseThrow();
        assertEquals(3, santi.jugadas());
        assertEquals(1, santi.ganadas());
        assertEquals(1, santi.perdidas());
        assertEquals(1, santi.empatadas());
        assertEquals(100.0 / 3.0, santi.porcentajeVictorias(), 0.0001);
    }

    @Test
    void calcularOrdenaPorMayorCantidadDeVictorias() {
        LocalDateTime ahora = LocalDateTime.now();
        List<ResultadoPartida> resultados = List.of(
                new ResultadoPartida(ahora, "A", "B", "A", 10, MotivoFin.HUNDIMIENTO),
                new ResultadoPartida(ahora, "A", "C", "A", 12, MotivoFin.HUNDIMIENTO),
                new ResultadoPartida(ahora, "A", "D", "A", 14, MotivoFin.HUNDIMIENTO),
                new ResultadoPartida(ahora, "B", "C", "B", 8, MotivoFin.DOMINACION),
                new ResultadoPartida(ahora, "B", "D", "B", 9, MotivoFin.DOMINACION),
                new ResultadoPartida(ahora, "C", "D", "C", 20, MotivoFin.HUNDIMIENTO)
        );

        List<EstadisticaJugador> ranking = CalculadoraRanking.calcular(resultados);

        assertEquals(4, ranking.size());
        assertEquals("A", ranking.get(0).nombre());
        assertEquals(3, ranking.get(0).ganadas());

        assertEquals("B", ranking.get(1).nombre());
        assertEquals(2, ranking.get(1).ganadas());

        assertEquals("C", ranking.get(2).nombre());
        assertEquals(1, ranking.get(2).ganadas());

        assertEquals("D", ranking.get(3).nombre());
        assertEquals(0, ranking.get(3).ganadas());
    }

    @Test
    void calcularDesempataPorMayorPorcentajeDeVictorias() {
        LocalDateTime ahora = LocalDateTime.now();
        List<ResultadoPartida> resultados = List.of(
                // Eficiente: 2 jugadas, 2 ganadas -> 100% victorias
                new ResultadoPartida(ahora, "Eficiente", "Rival1", "Eficiente", 5, MotivoFin.HUNDIMIENTO),
                new ResultadoPartida(ahora, "Eficiente", "Rival2", "Eficiente", 5, MotivoFin.HUNDIMIENTO),

                // Luchador: 4 jugadas, 2 ganadas, 2 perdidas -> 50% victorias
                new ResultadoPartida(ahora, "Luchador", "Rival1", "Luchador", 10, MotivoFin.HUNDIMIENTO),
                new ResultadoPartida(ahora, "Luchador", "Rival2", "Luchador", 10, MotivoFin.HUNDIMIENTO),
                new ResultadoPartida(ahora, "Luchador", "Rival3", "Rival3", 10, MotivoFin.HUNDIMIENTO),
                new ResultadoPartida(ahora, "Luchador", "Rival4", "Rival4", 10, MotivoFin.HUNDIMIENTO)
        );

        List<EstadisticaJugador> ranking = CalculadoraRanking.calcular(resultados);

        assertEquals("Eficiente", ranking.get(0).nombre());
        assertEquals(2, ranking.get(0).ganadas());
        assertEquals(100.0, ranking.get(0).porcentajeVictorias());

        assertEquals("Luchador", ranking.get(1).nombre());
        assertEquals(2, ranking.get(1).ganadas());
        assertEquals(50.0, ranking.get(1).porcentajeVictorias());
    }

    @Test
    void calcularDesempataPorNombreAlfabeticamente() {
        LocalDateTime ahora = LocalDateTime.now();
        List<ResultadoPartida> resultados = List.of(
                new ResultadoPartida(ahora, "Carlos", "Rival1", "Carlos", 10, MotivoFin.HUNDIMIENTO),
                new ResultadoPartida(ahora, "Ana", "Rival2", "Ana", 10, MotivoFin.HUNDIMIENTO),
                new ResultadoPartida(ahora, "Beto", "Rival3", "Beto", 10, MotivoFin.HUNDIMIENTO)
        );

        List<EstadisticaJugador> ranking = CalculadoraRanking.calcular(resultados);

        assertEquals("Ana", ranking.get(0).nombre());
        assertEquals("Beto", ranking.get(1).nombre());
        assertEquals("Carlos", ranking.get(2).nombre());
    }

    @Test
    void estadisticaJugadorPorcentajeVictoriasConCeroJugadasRetornaCero() {
        EstadisticaJugador est = new EstadisticaJugador("Nadie", 0, 0, 0, 0);
        assertEquals(0.0, est.porcentajeVictorias());
    }

    @Test
    void estadisticaJugadorCalculaPorcentajeVictoriasCorrectamente() {
        EstadisticaJugador est = new EstadisticaJugador("Jugador", 4, 1, 2, 1);
        assertEquals(25.0, est.porcentajeVictorias());
    }

    @Test
    void estadisticaJugadorValidaArgumentosNoNulosNiNegativos() {
        assertThrows(NullPointerException.class, () -> new EstadisticaJugador(null, 1, 1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new EstadisticaJugador("A", -1, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new EstadisticaJugador("A", 1, -1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new EstadisticaJugador("A", 1, 0, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> new EstadisticaJugador("A", 1, 0, 0, -1));
    }

    @Test
    void calcularDevuelveListaInmutable() {
        ResultadoPartida r = new ResultadoPartida(LocalDateTime.now(), "A", "B", "A", 10, MotivoFin.HUNDIMIENTO);
        List<EstadisticaJugador> ranking = CalculadoraRanking.calcular(List.of(r));

        assertThrows(UnsupportedOperationException.class, () -> ranking.add(
                new EstadisticaJugador("C", 1, 1, 0, 0)
        ));
    }

    @Test
    void calcularIgnoraElementosNulosEnLaLista() {
        List<ResultadoPartida> listaConNulo = new ArrayList<>();
        listaConNulo.add(new ResultadoPartida(LocalDateTime.now(), "A", "B", "A", 10, MotivoFin.HUNDIMIENTO));
        listaConNulo.add(null);

        List<EstadisticaJugador> ranking = CalculadoraRanking.calcular(listaConNulo);

        assertEquals(2, ranking.size());
        assertEquals("A", ranking.get(0).nombre());
        assertEquals("B", ranking.get(1).nombre());
    }
}
