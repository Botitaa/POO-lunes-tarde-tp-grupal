package batallanaval.vista;

/**
 * Pantallas que puede mostrar la {@link VentanaPrincipal}.
 * <p>
 * Cada valor se usa también como nombre de la "carta" dentro del {@code CardLayout}
 * (ver {@link VentanaPrincipal#agregarPantalla(Pantalla, javax.swing.JPanel)}).
 */
public enum Pantalla {
    INICIO,
    NUEVA_PARTIDA,
    DESPLIEGUE,
    BLACKOUT,
    RESUMEN,
    JUEGO,
    FIN_PARTIDA,
    ESTADISTICAS,
    REGLAS,
    CARGAR
}