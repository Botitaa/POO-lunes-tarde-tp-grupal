package batallanaval.modelo;

import batallanaval.excepciones.DisparoInvalidoException;
import batallanaval.excepciones.HabilidadNoDisponibleException;
import batallanaval.excepciones.MovimientoInvalidoException;

/**
 * Clase base de un navío. Controla vida, cooldowns, posición y acciones del turno.
 */
public abstract class Barco {

    private final int id;
    private final EstadisticasBarco estadisticas;
    private Posicion posicion;
    private int vida;
    private int cooldownDisparo;
    private int cooldownHabilidad;
    private int casillasRestantes;
    private boolean yaDisparo;
    private boolean habilidadActiva;
    private boolean revelado;

    protected Barco(int id, EstadisticasBarco estadisticas, Posicion posicion) {
        this.id = id;
        this.estadisticas = estadisticas;
        this.posicion = posicion;
        this.vida = estadisticas.vidaMaxima();
        this.cooldownDisparo = 0;
        this.cooldownHabilidad = 0;
        this.casillasRestantes = estadisticas.movimientoMaxPorTurno();
        this.yaDisparo = false;
        this.habilidadActiva = false;
        this.revelado = false;
    }

    protected Barco(int id, EstadisticasBarco estadisticas) {
        this(id, estadisticas, null);
    }

    /** Descuenta daño recibido. Si la vida llega a 0, queda hundido. */
    public void recibirDanio(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("El danio no puede ser negativo.");
        }
        this.vida = this.vida - cantidad;
        if (this.vida < 0) {
            this.vida = 0;
        }
    }

    /** Restaura vida sin superar la vida máxima. No tiene efecto si está hundido. */
    public void curar(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("La curacion no puede ser negativa.");
        }
        if (!estaHundido()) {
            this.vida = this.vida + cantidad;
            if (this.vida > estadisticas.vidaMaxima()) {
                this.vida = estadisticas.vidaMaxima();
            }
        }
    }

    /** Indica si el barco no tiene más vida. */
    public boolean estaHundido() {
        return this.vida <= 0;
    }

    /** Daño de disparo (puede ser modificado por habilidades). */
    public int getDanio() {
        return estadisticas.danio();
    }

    /** Alcance máximo de disparo en distancia Chebyshev. */
    public int getRangoDisparo() {
        return estadisticas.rangoDisparo();
    }

    /** Verifica si una distancia Chebyshev entra en el rango de tiro. */
    public boolean estaEnRango(int distancia) {
        return distancia <= getRangoDisparo();
    }

    /** Habilitado para disparar si está vivo, no disparó este turno y cooldown es 0. */
    public boolean puedeDisparar() {
        return !estaHundido() && !yaDisparo && cooldownDisparo == 0;
    }

    /** Registra el disparo: activa cooldown máximo, marca revelado e inhabilita movimiento. */
    public void registrarDisparo() {
        if (!puedeDisparar()) {
            throw new DisparoInvalidoException("El barco no esta en condiciones de disparar.");
        }
        this.yaDisparo = true;
        this.cooldownDisparo = estadisticas.cooldownDisparoMax();
        this.revelado = true;
    }

    /** Puede moverse si está vivo, no disparó en el turno y le quedan casillas. */
    public boolean puedeMoverse() {
        return !estaHundido() && !yaDisparo && casillasRestantes > 0;
    }

    /** Actualiza la posición y descuenta pasos de las casillas restantes del turno. */
    public void moverA(Posicion destino, int pasos) {
        if (!puedeMoverse()) {
            throw new MovimientoInvalidoException("El barco no puede moverse en este momento.");
        }
        if (pasos <= 0 || pasos > casillasRestantes) {
            throw new MovimientoInvalidoException("Cantidad de pasos invalida.");
        }
        this.posicion = destino;
        this.casillasRestantes = this.casillasRestantes - pasos;
    }

    /** Asigna la posición inicial en fase de despliegue. */
    public void ubicarEn(Posicion posicion) {
        this.posicion = posicion;
    }

    /** Habilidad disponible si está a flote, no se activó este turno y cooldown es 0. */
    public boolean puedeUsarHabilidad() {
        return !estaHundido() && !habilidadActiva && cooldownHabilidad == 0;
    }

    /** Template Method: valida, activa efecto de subclase y fija cooldown máximo. */
    public final void usarHabilidad() {
        if (!puedeUsarHabilidad()) {
            throw new HabilidadNoDisponibleException("La habilidad no esta disponible.");
        }
        this.habilidadActiva = true;
        aplicarHabilidad();
        this.cooldownHabilidad = estadisticas.cooldownHabilidadMax();
    }

    /** Efecto propio de cada subclase (Destructor x2 daño, Crucero se cura 2, Acorazado rango 8). */
    protected abstract void aplicarHabilidad();

    /** Reduce cooldowns en 1, repone casillas de movimiento y reinicia flags al inicio del turno. */
    public void reiniciarTurno() {
        if (cooldownDisparo > 0) {
            cooldownDisparo--;
        }
        if (cooldownHabilidad > 0) {
            cooldownHabilidad--;
        }
        this.yaDisparo = false;
        this.casillasRestantes = estadisticas.movimientoMaxPorTurno();
        this.habilidadActiva = false;
    }

    /** Modifica si el barco es visible para el rival por disparo previo. */
    public void marcarRevelado(boolean revelado) {
        this.revelado = revelado;
    }

    /** Restaura el estado completo al cargar partida guardada. */
    public void restaurar(int vida, Posicion posicion, int cooldownDisparo, int cooldownHabilidad,
                          int casillasRestantes, boolean habilidadActiva, boolean revelado) {
        this.vida = vida;
        this.posicion = posicion;
        this.cooldownDisparo = cooldownDisparo;
        this.cooldownHabilidad = cooldownHabilidad;
        this.casillasRestantes = casillasRestantes;
        this.habilidadActiva = habilidadActiva;
        this.revelado = revelado;
    }

    public int getId() {
        return id;
    }

    public EstadisticasBarco getEstadisticas() {
        return estadisticas;
    }

    public Posicion getPosicion() {
        return posicion;
    }

    public int getVida() {
        return vida;
    }

    public int getVidaMaxima() {
        return estadisticas.vidaMaxima();
    }

    public int getCooldownDisparo() {
        return cooldownDisparo;
    }

    public int getCooldownHabilidad() {
        return cooldownHabilidad;
    }

    public int getCasillasRestantes() {
        return casillasRestantes;
    }

    public boolean yaDisparo() {
        return yaDisparo;
    }

    public boolean estaHabilidadActiva() {
        return habilidadActiva;
    }

    public boolean estaRevelado() {
        return revelado;
    }

    /** Símbolo del barco: 'D' Destructor, 'C' Crucero, 'A' Acorazado. */
    public abstract char getSimbolo();
}
