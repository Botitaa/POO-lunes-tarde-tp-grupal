package batallanaval.modelo;

import batallanaval.excepciones.DisparoInvalidoException;
import batallanaval.excepciones.HabilidadNoDisponibleException;
import batallanaval.excepciones.MovimientoInvalidoException;


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


    public void recibirDanio(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("El danio no puede ser negativo.");
        }
        this.vida = this.vida - cantidad;
        if (this.vida < 0) {
            this.vida = 0;
        }
    }

   
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

  
    public boolean estaHundido() {
        return this.vida <= 0;
    }

    
    public int getDanio() {
        return estadisticas.danio();
    }

   
    public int getRangoDisparo() {
        return estadisticas.rangoDisparo();
    }

    /**
     * Comprueba si una distancia se encuentra dentro del rango de tiro del barco.
     *
     * @param distancia distancia de Chebyshev al objetivo.
     * @return {@code true} si la distancia es menor o igual al rango de disparo.
     */
    public boolean estaEnRango(int distancia) {
        return distancia <= getRangoDisparo();
    }

    /**
     * Indica si el barco está habilitado para disparar en el estado actual del turno.
     *
     * @return {@code true} si está a flote, no disparó este turno y no está en cooldown.
     */
    public boolean puedeDisparar() {
        return !estaHundido() && !yaDisparo && cooldownDisparo == 0;
    }

    /**
     * Registra un disparo efectuado por el barco, fijando el cooldown correspondiente,
     * marcándolo como revelado y deshabilitando nuevos disparos o movimientos este turno.
     *
     * @throws DisparoInvalidoException si el barco no estaba en condiciones de disparar.
     */
    public void registrarDisparo() {
        if (!puedeDisparar()) {
            throw new DisparoInvalidoException("El barco no esta en condiciones de disparar.");
        }
        this.yaDisparo = true;
        this.cooldownDisparo = estadisticas.cooldownDisparoMax();
        this.revelado = true;
    }

    /**
     * Indica si el barco puede desplazarse en este momento del turno.
     * Un barco que ya disparó no puede moverse.
     *
     * @return {@code true} si está a flote, no disparó este turno y tiene casillas restantes.
     */
    public boolean puedeMoverse() {
        return !estaHundido() && !yaDisparo && casillasRestantes > 0;
    }

    /**
     * Desplaza el barco a una nueva coordenada descontando las casillas avanzadas.
     *
     * @param destino nueva posición.
     * @param pasos   cantidad de casillas avanzadas.
     * @throws MovimientoInvalidoException si no puede moverse o la cantidad de pasos es inválida.
     */
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

    /**
     * Asigna la posición inicial del barco durante la fase de despliegue.
     *
     * @param posicion posición asignada en el tablero.
     */
    public void ubicarEn(Posicion posicion) {
        this.posicion = posicion;
    }

    /**
     * Indica si la habilidad especial del navío está disponible para ser activada.
     *
     * @return {@code true} si está a flote, no se activó este turno y no está en espera.
     */
    public boolean puedeUsarHabilidad() {
        return !estaHundido() && !habilidadActiva && cooldownHabilidad == 0;
    }

    /**
     * Activa la habilidad especial del barco mediante el patrón Template Method:
     * valida disponibilidad, aplica el efecto propio de la subclase y establece el cooldown.
     *
     * @throws HabilidadNoDisponibleException si la habilidad no puede usarse.
     */
    public final void usarHabilidad() {
        if (!puedeUsarHabilidad()) {
            throw new HabilidadNoDisponibleException("La habilidad no esta disponible.");
        }
        this.habilidadActiva = true;
        aplicarHabilidad();
        this.cooldownHabilidad = estadisticas.cooldownHabilidadMax();
    }

    /**
     * Efecto concreto de la habilidad especial definido por cada subclase.
     */
    protected abstract void aplicarHabilidad();

    /**
     * Restablece el estado del barco al comienzo de un nuevo turno propio:
     * decrementa cooldowns pendientes, rehabilita el movimiento y desactiva la habilidad temporal.
     */
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

    /**
     * Modifica el estado de visibilidad forzada (revelado) del barco.
     *
     * @param revelado {@code true} para mantenerlo revelado; {@code false} en caso contrario.
     */
    public void marcarRevelado(boolean revelado) {
        this.revelado = revelado;
    }

    /**
     * Restaura el estado completo del barco al cargar una partida guardada.
     *
     * @param vida              vida restante.
     * @param posicion          coordenada en el mapa.
     * @param cooldownDisparo   turnos restantes de recarga de disparo.
     * @param cooldownHabilidad turnos restantes de recarga de habilidad.
     * @param casillasRestantes casillas que le quedan por mover en el turno.
     * @param habilidadActiva   indica si la habilidad temporal estaba activa.
     * @param revelado          indica si el barco estaba descubierto.
     */
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

    public abstract char getSimbolo();
}
