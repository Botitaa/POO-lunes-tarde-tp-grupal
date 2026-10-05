# Batalla Naval Táctica — Diseño de clases

Actualizado: 5 de octubre de 2026

El juego tiene 70 tipos (clases, interfaces, enums y records) repartidos en 7 paquetes. Este documento dice qué hace cada una y sus métodos clave; es el contrato entre módulos y se actualiza junto con el UML.

## Arquitectura

Siete paquetes bajo `batallanaval`, con dependencias en un solo sentido: el modelo no conoce a nadie y la vista solo habla con el contrato de la partida.

| Paquete | Contiene | Puede usar | Responsable |
| --- | --- | --- | --- |
| `modelo` | Posición, casillas, tablero, barcos, jugador | `excepciones` | Marcos (tablero), Santi (barcos), Bota (jugador) |
| `controlador` | Contrato de la partida, `GestorPartida`, fases, observador, vistas de solo lectura | `modelo`, `log`, `excepciones` | Bota |
| `excepciones` | Excepciones propias | nada | Todos |
| `log` | Log técnico e historial de partida | `excepciones` | Bota |
| `persistencia` | Guardar y cargar, estadísticas, configuración, mapas desde archivo | `modelo`, `controlador`, `log`, `excepciones` | Marcos + Santi |
| `vista` | Ventana, pantallas y panel del tablero en Swing | interfaces y records de `controlador`, interfaces de `persistencia`, `excepciones` | Bruno (tablero), Guille (pantallas) |
| `app` | `Main` y armado de la aplicación | todo | Bota |

Reglas que valen para todas las clases:

- Atributos `private`; lo que no cambia, `final`. Colecciones que salen de una clase se devuelven con `List.copyOf` o `Collections.unmodifiableList`.
- La vista nunca recibe un `Barco`, `Jugador` o `Tablero`: recibe `EstadoBarco`, `EstadoJugador` y `Casilla` (inmutables).
- Ninguna clase usa `System.out`; se loguea con `private static final Logger LOG = Logger.getLogger(X.class.getName());`.
- Reglas violadas → excepción de `excepciones` con mensaje para el jugador. Errores de archivo → `PersistenciaException`.
- Lo que se guarda en disco implementa `Serializable` con `private static final long serialVersionUID = 1L;`.
- Identificadores en español sin tildes ni ñ (`danio`), constantes en `MAYUSCULAS`.

## modelo — Tablero y mapa (Marcos)

Cuatro clases que saben todo sobre el espacio: dónde se puede pasar, qué se ve y a dónde llega un barco. No saben nada de barcos ni de jugadores.

| Clase | Tipo | Qué hace | Miembros clave |
| --- | --- | --- | --- |
| `Posicion` | Clase `final` inmutable, `Serializable` | Una casilla del tablero (fila, columna). Se usa como clave en `Set` y `Map`. | `getFila()`, `getColumna()`, `distanciaChebyshev(Posicion)`, `equals`, `hashCode`, `toString` ("(4,5)") |
| `Casilla` | `enum` | Tipo de casilla con sus propiedades fijas: AGUA, PIEDRA, ISLA, RESTOS. | `bloqueaPaso()`, `bloqueaVision()`, `getSimbolo()`, `static desdeSimbolo(char)` |
| `Tablero` | Clase, `Serializable` | Grilla 10x10 de `Casilla`, zonas del mapa y geometría: límites, vecinos, diagonales y línea de visión. Valida el mapa al construirse. | `FILAS`, `COLUMNAS`, `getCasilla`, `estaDentro`, `esTransitable`, `getVecinos`, `esDiagonalBloqueada`, `hayLineaDeVision` (Bresenham, sin contar extremos), `esZonaDespliegue(pos, numeroJugador)`, `esZonaDominacion`, `convertirEnRestos` |
| `Navegacion` | Clase utilitaria (constructor privado, métodos `static`) | Calcula a qué casillas puede llegar un barco este turno (BFS). | `casillasAlcanzables(Tablero, Posicion origen, int pasosMax, Set<Posicion> ocupadas)` |

Decisiones ya tomadas en las reglas: AGUA y RESTOS no bloquean nada, PIEDRA bloquea solo el paso, ISLA bloquea paso y visión. La zona de dominación son las 4 casillas centrales (filas 4–5, columnas 4–5) y siempre es agua. La diagonal entre dos casillas que bloquean también bloquea. El mapa estándar lo lee `LectorMapas` desde `resources/mapas/estandar.txt`; si hay obstáculos fuera de las filas 3 a 6, `Tablero` lanza `MapaInvalidoException`.

## modelo — Barcos (Santi)

Cada barco sabe su vida, sus cooldowns y qué puede hacer este turno; no sabe dónde están los obstáculos ni los enemigos. La partida le pregunta y después le ordena.

| Clase | Tipo | Qué hace | Miembros clave |
| --- | --- | --- | --- |
| `EstadisticasBarco` | `record`, `Serializable` | Valores fijos de un tipo de barco. Valida que nada sea negativo. | `vidaMaxima`, `costoMovimiento`, `movimientoMaxPorTurno`, `rangoDisparo`, `vision`, `danio`, `cooldownDisparoMax`, `cooldownHabilidadMax` |
| `Barco` | Clase abstracta, `Serializable` | Estado de un barco y reglas propias: daño, curación, cooldowns, movimiento del turno, habilidad (Template Method) y revelado. | Constructor `protected Barco(int id, EstadisticasBarco, Posicion)`. `recibirDanio`, `curar`, `estaHundido`, `getDanio`, `getRangoDisparo`, `puedeDisparar`, `registrarDisparo`, `puedeMoverse`, `getCasillasRestantes`, `moverA`, `ubicarEn`, `puedeUsarHabilidad`, `usarHabilidad()` (final), `protected abstract void aplicarHabilidad()`, `reiniciarTurno`, `marcarRevelado`, `estaRevelado`, `abstract char getSimbolo()` |
| `Destructor` | Subclase | Vida 3, costo 1, 4 casillas, rango 2, visión 2, daño 3, cooldown 0. Habilidad: daño doble este turno. | `aplicarHabilidad()` activa el flag; `getDanio()` devuelve 6 si está activa. Símbolo `D` |
| `Crucero` | Subclase | Vida 5, costo 2, 3 casillas, rango 4, visión 3, daño 2, cooldown 0. Habilidad: se repara 2. | `aplicarHabilidad()` llama a `curar(2)`. Símbolo `C` |
| `Acorazado` | Subclase | Vida 8, costo 3, 2 casillas, rango 6, visión 5, daño 4, cooldown 1. Habilidad: rango 8 este turno. | `getRangoDisparo()` devuelve 8 si está activa. Símbolo `A` |
| `FabricaFlota` | Clase utilitaria | Crea la flota estándar con ids únicos, así `Jugador` no conoce las subclases (Creador). | `static List<Barco> crearFlotaEstandar(int primerId)`: 1 acorazado, 2 cruceros, 3 destructores, sin posición |

Todas las habilidades tienen espera de 3 turnos, duran hasta el fin del turno, no cuestan puntos y no revelan al barco. Un barco que disparó no puede moverse en ese turno, y queda revelado para el rival durante su turno siguiente.

## modelo — Jugador (Bota)

| Clase | Tipo | Qué hace | Miembros clave |
| --- | --- | --- | --- |
| `Jugador` | Clase, `Serializable` | Dueño de una flota de 6 barcos y de sus puntos del turno. Responde preguntas sobre su flota (Experto). | `Jugador(String nombre, int numero, List<Barco> flota)`, `getNombre`, `getNumero` (1 o 2), `getFlota` (no modificable), `getBarcosVivos`, `tieneBarcosVivos`, `getVidaTotal`, `getPuntosMovimiento`, `tienePuntos(int)`, `gastarPuntos(int)`, `reponerPuntos(int)`, `getPuntosDominacion`, `sumarPuntoDominacion`, `getResumen` |
| `ResumenTurno` | Clase, `Serializable` | Lo que el rival le hizo al jugador desde su último turno; se muestra al volver del blackout y se limpia. | `agregarDisparoRecibido(Barco objetivo, int danio, Posicion origen)`, `agregarBarcoHundido(Barco, Posicion)`, `getMensajes` (no modificable), `limpiar`, `estaVacio` |

`Jugador` recibe la flota ya creada por `FabricaFlota` en vez de crearla él: así se puede testear con flotas armadas a mano.

## controlador — Partida y reglas (Bota)

`GestorPartida` es el único lugar donde se aplican las reglas (Controlador GRASP). La vista lo ve a través de tres interfaces chicas (ISP) unidas en `Partida`.

| Clase | Tipo | Qué hace | Miembros clave |
| --- | --- | --- | --- |
| `FasePartida` | `enum` | Fase actual. | DESPLIEGUE_J1, DESPLIEGUE_J2, EN_JUEGO, TERMINADA |
| `PartidaDespliegue` | Interfaz | Acciones del despliegue. | `ubicarBarco(int idBarco, Posicion)`, `retirarBarco(int idBarco)`, `getBarcosSinUbicar()`, `confirmarDespliegue()` |
| `PartidaCombate` | Interfaz | Acciones del turno. | `seleccionarBarco(int id)`, `agregarPaso(Posicion)`, `deshacerPaso()`, `confirmarMovimiento()`, `usarHabilidad()`, `disparar(int idObjetivo)`, `terminarTurno()` |
| `PartidaConsulta` | Interfaz | Todo lo que la vista lee, siempre en objetos inmutables. | `getCasilla(Posicion)`, `getJugadorActivo()`, `getRival()` (`EstadoJugador`), `getBarcosPropios()`, `getBarcosEnemigosVisibles()` (`List<EstadoBarco>`), `getBarcoSeleccionado()` (`Optional<EstadoBarco>`), `getRutaPlanificada`, `getCostoRuta`, `getCasillasAlcanzables`, `getObjetivosValidos`, `esVisible(Posicion)`, `getMensajesResumen`, `getHistorialVisible()`, `getRonda`, `getFase`, `estaTerminada`, `getResultado()` (`Optional<ResultadoPartida>`), `puedeGuardarse()` |
| `Partida` | Interfaz | Une las tres; es el tipo que recibe la vista. Agrega el registro de observadores. | `extends PartidaDespliegue, PartidaCombate, PartidaConsulta`; `agregarObservador`, `quitarObservador` |
| `GestorPartida` | Clase, `Serializable` | Implementa `Partida`: valida cada acción, la aplica, la anota en el historial, la loguea y avisa a los observadores. | Constantes `TURNOS_MAX = 40`, `PUNTOS_MOVIMIENTO_TURNO = 6`, `PUNTOS_DOMINACION_VICTORIA = 6`. Atributos: `jugador1`, `jugador2`, `jugadorActivo`, `tablero` (agregación), `fase`, `ronda`, `barcoSeleccionado`, `rutaPlanificada`, `historial`, `resultado`, `accionRealizadaEnTurno`, `transient List<ObservadorPartida> observadores`. Privados: `iniciarTurno`, `cambiarJugadorActivo`, `esVisibleParaJugador`, `actualizarDominacion`, `verificarVictoria`, `resolverLimiteDeTurnos`, `registrarEvento(TipoEvento, String)`, `notificar...`, `readObject` (recrea la lista de observadores al cargar), `validarEstado()` |
| `ObservadorPartida` | Interfaz | La implementan la vista y el autoguardado para enterarse de cambios. | `onEstadoCambiado()`, `onFaseCambiada(FasePartida)`, `onTurnoIniciado(String nombreJugador)`, `onPartidaTerminada(ResultadoPartida)` |
| `EstadoBarco` | `record` | Foto de solo lectura de un barco para la vista. | `id`, `simbolo`, `posicion`, `vida`, `vidaMaxima`, `casillasRestantes`, `cooldownDisparo`, `cooldownHabilidad`, `habilidadActiva`, `rangoDisparo`, `vision`, `esPropio` |
| `EstadoJugador` | `record` | Foto de solo lectura de un jugador. | `nombre`, `numero`, `puntosMovimiento`, `puntosDominacion`, `barcosVivos`, `vidaTotal` |
| `ResultadoPartida` | `record`, `Serializable` | Cómo terminó la partida; lo usan la pantalla final y las estadísticas. | `fecha`, `jugador1`, `jugador2`, `ganador` (`null` si empate), `rondas`, `motivo`; `esEmpate()` |
| `MotivoFin` | `enum` | Por qué terminó. | HUNDIMIENTO, DOMINACION, LIMITE_TURNOS, EMPATE, ABANDONO |

## excepciones

Las de reglas son unchecked: la vista ya deshabilita lo inválido, así que llegar ahí es un error que se muestra y se loguea. `PersistenciaException` es checked porque un archivo puede fallar aunque el código esté bien, y el compilador obliga a manejarlo.

| Clase | Extiende | Se lanza cuando |
| --- | --- | --- |
| `AccionInvalidaException` | `RuntimeException` | Base de las de reglas; acción en una fase que no corresponde |
| `DespliegueInvalidoException` | `AccionInvalidaException` | Casilla fuera de zona, ocupada u obstáculo; confirmar sin los 6 |
| `MovimientoInvalidoException` | `AccionInvalidaException` | Paso no adyacente, bloqueado, sin puntos o barco que ya disparó |
| `DisparoInvalidoException` | `AccionInvalidaException` | Objetivo no visible, fuera de rango, sin línea de visión o tirador en cooldown |
| `HabilidadNoDisponibleException` | `AccionInvalidaException` | Habilidad en espera o ya usada este turno |
| `MapaInvalidoException` | `RuntimeException` | Mapa mal formado u obstáculos fuera de la zona neutral |
| `PersistenciaException` | `Exception` | Cualquier error al leer o escribir archivos; envuelve la causa original |

## log — Log técnico e historial (Bota)

Hay dos registros separados. El **log técnico** es para el equipo (errores y depuración) y va a `datos/logs/`. El **historial de partida** es parte del juego: lo ve el jugador, se guarda con la partida y se exporta al final. Se usa `java.util.logging`, sin librerías externas.

### ConfiguracionLog

Clase utilitaria que configura el logger raíz una sola vez, al arrancar `Main`. Llamarla dos veces no duplica handlers.

```java
public final class ConfiguracionLog {
    private static boolean inicializado = false;
    private ConfiguracionLog() {}

    // Consola en INFO; archivo datos/logs/batalla-naval-%g.log en FINE,
    // 1 MB por archivo, 3 archivos rotando, modo append, con FormatoLog.
    // Si no puede crear el archivo, sigue solo con consola y avisa por stderr.
    public static synchronized void inicializar(Path carpetaLogs) { ... }
}
```

### FormatoLog

`extends java.util.logging.Formatter`. Una línea por evento y, si hay excepción, el stack trace debajo.

```java
// 2026-10-05 14:32:10 [FINE] GestorPartida - J1 movio Destructor#3 a (5,4)
@Override public String format(LogRecord registro) { ... }
```

### ManejadorErroresGlobal

`implements Thread.UncaughtExceptionHandler`. Atrapa cualquier excepción que nadie manejó, la loguea en SEVERE y muestra un `JOptionPane` con "Ocurrió un error inesperado" (desde el EDT con `SwingUtilities.invokeLater`) en vez de que la ventana quede colgada.

```java
public static void instalar() // Thread.setDefaultUncaughtExceptionHandler(new ManejadorErroresGlobal())
@Override public void uncaughtException(Thread hilo, Throwable error) { ... }
```

**Niveles (convención del equipo):**

| Nivel | Cuándo | Ejemplo |
| --- | --- | --- |
| SEVERE | Excepción no esperada o error de E/S | No se pudo escribir el guardado |
| WARNING | Acción inválida que llegó a la lógica, archivo corrupto, línea mal formada | `DisparoInvalidoException: fuera de rango` |
| INFO | Hitos: inicio y fin de partida, cambio de fase, guardar y cargar | Partida cargada: "partida1" ronda 12 |
| FINE | Cada acción del juego | J2 disparó a Crucero#2 (-4) |

### TipoEvento

`enum` con un atributo que dice si el rival puede verlo; así la niebla de guerra se respeta también en el historial.

```java
public enum TipoEvento {
    DESPLIEGUE(false), MOVIMIENTO(false), HABILIDAD(false),
    DISPARO(true), HUNDIMIENTO(true), DOMINACION(true),
    FIN_TURNO(true), FIN_PARTIDA(true);
    private final boolean publico;
    public boolean esPublico() { ... }
}
```

### EventoPartida

`record` inmutable y `Serializable`: una entrada del historial.

```java
public record EventoPartida(int ronda, int numeroJugador, TipoEvento tipo,
                            String descripcion, LocalDateTime momento) implements Serializable {
    public boolean esVisiblePara(int numeroJugador) // tipo.esPublico() || this.numeroJugador == numeroJugador
    public String aLinea()                         // "[R12] J2 DISPARO: Acorazado (7,4) -> Crucero (4,4), -4"
}
```

### HistorialPartida

Clase `Serializable` que guarda los eventos en orden. La crea `GestorPartida` y viaja dentro del guardado.

```java
private final List<EventoPartida> eventos = new ArrayList<>();
public void registrar(EventoPartida evento)          // rechaza null
public List<EventoPartida> getEventos()              // List.copyOf
public List<EventoPartida> getEventosDeRonda(int ronda)
public List<EventoPartida> getEventosVisiblesPara(int numeroJugador)
public int cantidad()
```

**Cómo se conecta:** `GestorPartida` tiene un método privado `registrarEvento(TipoEvento tipo, String descripcion)` que hace las dos cosas a la vez: agrega el `EventoPartida` al historial y escribe `LOG.fine(...)`. Cada acción confirmada lo llama una vez. Las acciones rechazadas no van al historial, solo al log en WARNING.

## persistencia — Archivos (Marcos + Santi)

Se guardan cuatro cosas: la partida (binario serializado), las estadísticas (CSV), la configuración (`.properties`) y el historial exportado (`.txt`); además se lee el mapa (`.txt`). Todo vive bajo `datos/` en la carpeta donde corre el juego, y la vista solo conoce las interfaces `RepositorioPartidas` y `RepositorioEstadisticas` (DIP).

```text
datos/
  config.properties
  estadisticas.csv
  logs/batalla-naval-0.log
  partidas/partida1.sav, autoguardado.sav
  historiales/2026-10-05_1432_Bota-vs-Santi.txt
src/main/resources/mapas/estandar.txt
```

**Reparto:** Marcos hace `RutasApp`, `LectorMapas`, `ConfiguracionJuego` y `ExportadorHistorial`. Santi hace los repositorios de partidas y estadísticas, `CabeceraGuardado`, `EstadisticaJugador` y `CalculadoraRanking`.

### RutasApp

Utilitaria con las rutas fijas, para que ningún otro archivo escriba un `"datos/..."` a mano.

```java
public static final Path BASE = Path.of("datos");
public static final Path PARTIDAS = BASE.resolve("partidas");
public static final Path LOGS, HISTORIALES, ESTADISTICAS, CONFIGURACION;
public static void crearCarpetas() throws PersistenciaException // Files.createDirectories
```

### LectorMapas

Lee un mapa de texto desde los recursos del proyecto y devuelve el `Tablero`. Usa los símbolos de `Casilla` (`~` agua, `I` isla, `P` piedra); las filas de despliegue se escriben como agua.

```java
public static Tablero cargar(String nombreRecurso) throws PersistenciaException
// getResourceAsStream("/mapas/" + nombre) + BufferedReader con try-with-resources.
// 10 líneas de 10 símbolos separados por espacio; si no, PersistenciaException con número de línea.
```

### RepositorioPartidas (interfaz)

```java
void guardar(GestorPartida partida, String nombre) throws PersistenciaException;
GestorPartida cargar(String nombre) throws PersistenciaException;
List<CabeceraGuardado> listar() throws PersistenciaException;   // más nuevo primero
void borrar(String nombre) throws PersistenciaException;
boolean existe(String nombre);
```

### CabeceraGuardado

`record Serializable` que se escribe antes de la partida en el mismo archivo. Permite listar guardados (nombres, ronda, fecha) sin deserializar la partida entera y chequear la versión del formato.

```java
public record CabeceraGuardado(int versionFormato, String nombre, LocalDateTime fecha,
        String jugador1, String jugador2, int ronda, FasePartida fase) implements Serializable {}
```

### RepositorioPartidasArchivo

`implements RepositorioPartidas`. Un archivo `partidas/<nombre>.sav` por partida con `ObjectOutputStream`: primero la cabecera, después el `GestorPartida`.

```java
public static final int VERSION_FORMATO = 1;
private final Path carpeta;

// guardar: valida el nombre (letras, números, - y _, hasta 40), exige partida.puedeGuardarse(),
//   escribe en <nombre>.sav.tmp y después Files.move(..., REPLACE_EXISTING, ATOMIC_MOVE).
//   Si se corta a la mitad, el guardado anterior queda intacto.
// cargar: lee la cabecera, si versionFormato != VERSION_FORMATO -> PersistenciaException;
//   lee el GestorPartida y llama a validarEstado().
//   IOException, ClassNotFoundException, ClassCastException e InvalidClassException
//   se envuelven en PersistenciaException("El archivo está dañado o es de otra versión", causa).
// listar: lee solo cabeceras; un archivo roto se saltea con LOG.warning.
```

**Qué tiene que ser `Serializable`:** `Posicion`, `Tablero`, `EstadisticasBarco`, `Barco` (y por herencia sus subclases), `Jugador`, `ResumenTurno`, `GestorPartida`, `HistorialPartida`, `EventoPartida`, `ResultadoPartida`, `CabeceraGuardado`. Los `enum` ya lo son. En `GestorPartida`, `observadores` es `transient` y se recrea en `readObject`; los `Logger` son `static`, así que no se serializan. Después de cargar, la vista se vuelve a registrar como observador.

**Cuándo se puede guardar:** `GestorPartida.puedeGuardarse()` devuelve `true` solo al inicio de un turno (sin acciones realizadas, sin ruta planificada) o en la pantalla de blackout. Así nunca se guarda un movimiento a medias.

### RepositorioEstadisticas (interfaz) y RepositorioEstadisticasCsv

```java
void registrar(ResultadoPartida resultado) throws PersistenciaException;
List<ResultadoPartida> obtenerTodos() throws PersistenciaException;
```

La implementación CSV usa `;` como separador y una línea de encabezado `fecha;jugador1;jugador2;ganador;rondas;motivo`. `registrar` agrega con `Files.newBufferedWriter(archivo, CREATE, APPEND)` (si el archivo no existía, escribe antes el encabezado). `obtenerTodos` lee con `BufferedReader`; una línea mal formada se saltea con `LOG.warning` y no rompe el resto. Los nombres de jugador no pueden tener `;` (se valida en la pantalla de nueva partida).

### EstadisticaJugador y CalculadoraRanking

```java
public record EstadisticaJugador(String nombre, int jugadas, int ganadas,
                                 int perdidas, int empatadas) {
    public double porcentajeVictorias() // 0 si jugadas == 0
}

// CalculadoraRanking: agrupa los resultados por jugador en un Map<String, ...>
// y ordena con un Comparator: más ganadas, después mayor porcentaje, después nombre.
public static List<EstadisticaJugador> calcular(List<ResultadoPartida> resultados)
```

Se separa del repositorio para que la regla del ranking se pueda testear sin archivos (SRP).

### ConfiguracionJuego

Envuelve un `java.util.Properties`. Si el archivo no existe o está roto, usa valores por defecto, loguea WARNING y lo reescribe.

```java
// Claves: jugador1.ultimo, jugador2.ultimo, autoguardado (true), carpeta.partidas
public static ConfiguracionJuego cargar(Path archivo)          // nunca lanza: cae en defaults
public void guardar() throws PersistenciaException
public String getUltimoJugador1() / setUltimoJugador1(String)  // idem jugador 2
public boolean isAutoguardado() / setAutoguardado(boolean)
```

### ExportadorHistorial

Al terminar la partida escribe el historial completo (ya no hay niebla) en `datos/historiales/` con nombre `fecha_hora_J1-vs-J2.txt`.

```java
public static Path exportar(HistorialPartida historial, ResultadoPartida resultado, Path carpeta)
        throws PersistenciaException
// BufferedWriter + try-with-resources: encabezado con jugadores, ganador y motivo,
// después una línea por evento con EventoPartida.aLinea(). Devuelve la ruta creada.
```

## vista — Tablero (Bruno)

El panel dibuja y traduce clics a llamadas a `Partida`; nunca decide si algo es válido.

| Clase | Tipo | Qué hace | Miembros clave |
| --- | --- | --- | --- |
| `PanelTablero` | `extends JPanel implements ObservadorPartida` | Dibuja grilla, casillas, niebla, zona de dominación, barcos con su vida, selección, casillas alcanzables, ruta numerada y objetivos en rojo. Convierte clics en `Posicion` y llama a la partida según el modo. | `PanelTablero(Partida)`, `setModo(ModoInteraccion)`, `paintComponent(Graphics)`, `posicionEn(Point)`, `onEstadoCambiado()` → `repaint()`. Key bindings: Esc, Backspace, Enter. Tooltip con atributos del barco |
| `ModoInteraccion` | `enum` | Qué significa un clic ahora. | DESPLIEGUE, SELECCION, PLANIFICANDO_RUTA, ELIGIENDO_OBJETIVO, BLOQUEADO |
| `PaletaColores` | Clase utilitaria | Todos los colores en un lugar, según el código visual de las reglas. | `AGUA` azul, `NIEBLA` azul oscuro, `ISLA` verde, `PIEDRA` gris, `RESTOS` X negra, `DOMINACION` borde dorado, `SELECCION` borde amarillo, `RANGO` rojo suave, `RUTA` celeste, `colorJugador(int)` |

## vista — Pantallas y flujo (Guille)

Una sola ventana con `CardLayout`. Cada pantalla es un `JPanel` que recibe lo que necesita por constructor; ninguna crea repositorios ni partidas (eso lo hace `ControladorAplicacion`).

| Clase | Tipo | Qué hace |
| --- | --- | --- |
| `VentanaPrincipal` | `extends JFrame` | Contiene el `CardLayout`, la barra de menú y la barra de estado. `mostrar(Pantalla)`, `mostrarError(String)`, `mostrarMensaje(String)`. |
| `Pantalla` | `enum` | INICIO, NUEVA_PARTIDA, DESPLIEGUE, BLACKOUT, RESUMEN, JUEGO, FIN_PARTIDA, ESTADISTICAS, REGLAS, CARGAR. |
| `PanelInicio` | `JPanel` | Botones Nueva partida, Cargar partida, Estadísticas, Reglas, Salir. |
| `PanelNuevaPartida` | `JPanel` | Nombres de J1 y J2 (no vacíos, distintos, sin `;`), precargados de `ConfiguracionJuego`; sorteo de quién es J1. |
| `PanelDespliegue` | `JPanel` | `PanelTablero` en modo DESPLIEGUE + lista de barcos sin ubicar + Confirmar despliegue (habilitado con los 6). |
| `PanelBlackout` | `JPanel` | Fondo negro, "Turno de X", botón Listo y botón Guardar partida (solo acá se puede guardar). |
| `PanelResumen` | `JPanel` | Mensajes de `getMensajesResumen()` y botón Continuar; se saltea si está vacío. |
| `PanelJuego` | `JPanel` | `PanelTablero` en el centro + `PanelLateral` a la derecha + `PanelHistorial` abajo (plegable). |
| `PanelLateral` | `JPanel` | Barco seleccionado (tipo, vida, casillas, cooldowns), puntos de movimiento, dominación de ambos, ronda; botones Confirmar movimiento, Usar habilidad, Deshacer paso, Terminar turno, habilitados según el estado. |
| `PanelHistorial` | `JPanel` | `JList` con `getHistorialVisible()`, último evento abajo. |
| `PanelFinPartida` | `JPanel` | Ganador, motivo, rondas; botones Ver historial, Abrir historial exportado, Volver al inicio. |
| `PanelCargarPartida` | `JPanel` | `JTable` con los guardados (`listar()`): nombre, jugadores, ronda, fecha; botones Cargar y Borrar. |
| `PanelEstadisticas` | `JPanel` | `JTable` con el ranking de `CalculadoraRanking`. |
| `PanelReglas` | `JPanel` | Texto de reglas en un `JScrollPane`. |
| `BarraMenuJuego` | `extends JMenuBar` | Partida → Guardar, Cargar, Abandonar (con confirmación); Ayuda → Reglas. |
| `TareaGuardar` | `extends SwingWorker<Void, Void>` | Guarda en segundo plano para no congelar la ventana; en `done()` avisa éxito o muestra el error de `PersistenciaException`. |
| `TareaCargar` | `extends SwingWorker<GestorPartida, Void>` | Carga en segundo plano; en `done()` entrega la partida a `ControladorAplicacion` o muestra el error. |

`TareaGuardar` y `TareaCargar` son las que cubren hilos (Unidad 6). Todo cambio de componentes Swing se hace en el EDT.

## app — Arranque (Bota)

| Clase | Tipo | Qué hace |
| --- | --- | --- |
| `Main` | Clase con `main` | En orden: `RutasApp.crearCarpetas()`, `ConfiguracionLog.inicializar(RutasApp.LOGS)`, `ManejadorErroresGlobal.instalar()`, y después `SwingUtilities.invokeLater(() -> new ControladorAplicacion().iniciar())`. |
| `ControladorAplicacion` | Clase | Arma todo (inyección por constructor a mano): crea `ConfiguracionJuego`, `RepositorioPartidasArchivo`, `RepositorioEstadisticasCsv` y `VentanaPrincipal`. Crea o carga la partida, registra observadores y decide qué pantalla mostrar según los eventos (`onFaseCambiada`, `onTurnoIniciado`, `onPartidaTerminada`). Al terminar: registra el resultado, exporta el historial y muestra `PanelFinPartida`. |
| `AutoGuardado` | `implements ObservadorPartida` | Si `autoguardado=true`, en cada `onTurnoIniciado` guarda `autoguardado.sav` con una `TareaGuardar`. Un fallo se loguea en WARNING y no interrumpe la partida. |

## Tests (JUnit 5)

Se testea todo lo que no es Swing. Cada autor escribe los tests de sus clases; los de persistencia usan `@TempDir` para no tocar `datos/`.

| Clase de test | Qué verifica | Autor |
| --- | --- | --- |
| `PosicionTest` | Igualdad, `hashCode` en `HashSet`, distancia de Chebyshev | Marcos |
| `TableroTest` | Límites, vecinos, diagonal bloqueada (4 esquinas), línea de visión, zonas, mapa inválido | Marcos |
| `NavegacionTest` | Alcanzables con obstáculos, barcos ocupando casillas y límite de pasos | Marcos |
| `BarcoTest` | Daño y curación en los límites, cooldowns en varios turnos, no moverse tras disparar | Santi |
| `HabilidadesTest` | Las 3 habilidades, su espera de 3 turnos y que duren un solo turno | Santi |
| `JugadorTest` | Puntos, barcos vivos, vida total | Bota |
| `GestorPartidaTest` | Despliegue, turnos, movimiento, disparo, visión compartida, revelado, choque con enemigo oculto, dominación, las 3 victorias y desempates, cada excepción | Bota |
| `HistorialPartidaTest` | Orden de eventos y filtro por jugador sin filtrar información oculta | Bota |
| `RepositorioPartidasArchivoTest` | Guardar y cargar da una partida equivalente; archivo corrupto, de otra versión o inexistente lanza `PersistenciaException`; `listar` saltea archivos rotos | Santi |
| `RepositorioEstadisticasCsvTest` | Append, encabezado una sola vez, líneas rotas salteadas | Santi |
| `CalculadoraRankingTest` | Conteo y orden del ranking, empates | Santi |
| `LectorMapasTest` y `ConfiguracionJuegoTest` | Mapa estándar correcto, mapa mal formado, config faltante cae en defaults | Marcos |
