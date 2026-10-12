package batallanaval.modelo;

public abstract class Barco {

    private final int id;
    private final EstadisticasBarco estadisticas;
    private int vida;
    private int cooldownDisparo;
    private int cooldownHabilidad;
    private Posicion posicion;
    private boolean yaDisparo;
    private int casillasMovidasTurno;
    private boolean revelado;
    private boolean habilidadActiva;

    protected Barco(int id, EstadisticasBarco estadisticas, Posicion posicion) {
        this.id = id;
        this.estadisticas = estadisticas;
        this.posicion = posicion;
        this.vida = estadisticas.vidaMaxima();
        this.cooldownDisparo = 0;
        this.cooldownHabilidad = 0;
        this.yaDisparo = false;
        this.casillasMovidasTurno = 0;
        this.revelado = false;
        this.habilidadActiva = false;
    }

    protected Barco(int id, EstadisticasBarco estadisticas) {
        this(id, estadisticas, null);
    }

    public void recibirDanio(int cantidad) {
        this.vida = this.vida - cantidad;
        if (this.vida < 0) {
            this.vida = 0;
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

    public boolean estaEnRango(int distancia) {
        return distancia <= getRangoDisparo();
    }

    public boolean puedeDisparar() {
        return !estaHundido() && !yaDisparo && cooldownDisparo == 0;
    }

    public void registrarDisparo() {
        this.yaDisparo = true;
        this.cooldownDisparo = estadisticas.cooldownDisparoMax();
        this.revelado = true;
    }

    public boolean puedeUsarHabilidad() {
        return !estaHundido() && !habilidadActiva && cooldownHabilidad == 0;
    }

    public final void usarHabilidad() {
        if (puedeUsarHabilidad()) {
            this.habilidadActiva = true;
            aplicarHabilidad();
            this.cooldownHabilidad = estadisticas.cooldownHabilidadMax();
        }
    }

    protected abstract void aplicarHabilidad();

    public void reiniciarTurno() {
        if (cooldownDisparo > 0) {
            cooldownDisparo--;
        }
        if (cooldownHabilidad > 0) {
            cooldownHabilidad--;
        }
        this.yaDisparo = false;
        this.casillasMovidasTurno = 0;
        this.habilidadActiva = false;
    }

    public boolean estaHabilidadActiva() {
        return habilidadActiva;
    }

    public void setHabilidadActiva(boolean habilidadActiva) {
        this.habilidadActiva = habilidadActiva;
    }

    public int getId() {
        return id;
    }

    public EstadisticasBarco getEstadisticas() {
        return estadisticas;
    }

    public int getVida() {
        return vida;
    }

    public void setVida(int vida) {
        this.vida = vida;
    }

    public int getCooldownDisparo() {
        return cooldownDisparo;
    }

    public void setCooldownDisparo(int cooldownDisparo) {
        this.cooldownDisparo = cooldownDisparo;
    }

    public int getCooldownHabilidad() {
        return cooldownHabilidad;
    }

    public void setCooldownHabilidad(int cooldownHabilidad) {
        this.cooldownHabilidad = cooldownHabilidad;
    }

    public Posicion getPosicion() {
        return posicion;
    }

    public void setPosicion(Posicion posicion) {
        this.posicion = posicion;
    }

    public void ubicarEn(Posicion posicion) {
        this.posicion = posicion;
    }

    public boolean yaDisparo() {
        return yaDisparo;
    }

    public void setYaDisparo(boolean yaDisparo) {
        this.yaDisparo = yaDisparo;
    }

    public int getCasillasMovidasTurno() {
        return casillasMovidasTurno;
    }

    public void setCasillasMovidasTurno(int casillasMovidasTurno) {
        this.casillasMovidasTurno = casillasMovidasTurno;
    }

    public boolean estaRevelado() {
        return revelado;
    }

    public void setRevelado(boolean revelado) {
        this.revelado = revelado;
    }

    public void marcarRevelado(boolean revelado) {
        this.revelado = revelado;
    }

    public abstract char getSimbolo();
}
