# Batalla Naval Táctica — Diseño de clases

Actualizado: 8 de octubre de 2026

El juego tiene unos 65 tipos (clases, interfaces, enums y records) repartidos en 7 paquetes. Este documento dice qué hace cada una y sus métodos clave; es el contrato entre módulos y se actualiza junto con el UML.

## Arquitectura

Siete paquetes bajo `batallanaval`, con dependencias en un solo sentido: el modelo no conoce a nadie y la vista solo habla con el contrato de la partida.

| Paquete | Contiene | Puede usar | Responsable |
| --- | --- | --- | --- |
| `modelo` | Posición, casillas, tablero, barcos, jugador | `excepciones` | Marcos (tablero), Santi (barcos), Bota (jugador) |
| `controlador` | Contrato de la partida, `GestorPartida`, fases, observador, vistas de solo lectura e instantánea | `modelo`, `historial`, `excepciones` | Bota |
| `excepciones` | Excepciones propias | nada | Todos |
| `historial` | Historial de partida (eventos que ve el jugador) | `excepciones` | Bota |
| `persistencia` | Guardar y cargar (texto), estadísticas, configuración, mapas desde archivo | `modelo`, `controlador`, `historial`, `excepciones` | Marcos + Santi |
| `vista` | Ventana, pantallas y panel del tablero en Swing | interfaces y records de `controlador`, interfaces de `persistencia`, `excepciones` | Bruno (tablero), Guille (pantallas) |
| `app` | `Main` y armado de la aplicación | todo | Bota |

Reglas que valen para todas las clases:

- Atributos `private`; lo que no cambia, `final`. Colecciones que salen de una clase se devuelven con `List.copyOf` o `Collections.unmodifiableList`.
- La vista nunca recibe un `Barco`, `Jugador` o `Tablero`: recibe `EstadoBarco`, `EstadoJugador` y `Casilla` (inmutables).
- Ninguna clase usa `System.out` para el juego; errores y avisos se muestran en la GUI (barra de estado o diálogo).
- Reglas violadas → excepción de `excepciones` con mensaje para el jugador. Errores de archivo → `PersistenciaException`.
- Lo que se guarda en disco se escribe como texto a través de `InstantaneaPartida`; ninguna clase del modelo es `Serializable`.
- Identificadores en español sin tildes ni ñ (`danio`), constantes en `MAYUSCULAS`.

## modelo — Tablero y mapa (Marcos)

Cuatro clases que saben todo sobre el espacio: dónde se puede pasar, qué se ve y a dónde llega un barco. No saben nada de barcos ni de jugadores.

| Clase | Tipo | Qué hace | Miembros clave |
| --- | --- | --- | --- |
| `Posicion` | Clase `final` inmutable | Una casilla del tablero (fila, columna). Se usa como clave en `Set` y `Map`. | `getFila()`, `getColumna()`, `distanciaChebyshev(Posicion)`, `equals`, `hashCode`, `toString` ("(4,5)") |
| `Casilla` | `enum` | Tipo de casilla con sus propiedades fijas: AGUA, PIEDRA, ISLA, RESTOS. | `bloqueaPaso()`, `bloqueaVision()`, `getSimbolo()`, `static desdeSimbolo(char)` |
| `Tablero` | Clase | Grilla 10x10 de `Casilla`, zonas del mapa y geometría: límites, vecinos, diagonales y línea de visión. Valida el mapa al construirse. | `FILAS`, `COLUMNAS`, `getCasilla`, `estaDentro`, `esTransitable`, `getVecinos`, `esDiagonalBloqueada`, `hayLineaDeVision` (Bresenham, sin contar extremos), `esZonaDespliegue(pos, numeroJugador)`, `esZonaDominacion`, `convertirEnRestos`, `getRestos()` (casillas convertidas, para guardar) |
| `Navegacion` | Clase utilitaria (constructor privado, métodos `static`) | Calcula a qué casillas puede llegar un barco este turno (BFS). | `casillasAlcanzables(Tablero, Posicion origen, int pasosMax, Set<Posicion> ocupadas)` |

Decisiones ya tomadas en las reglas: AGUA y RESTOS no bloquean nada, PIEDRA bloquea solo el paso, ISLA bloquea paso y visión. La zona de dominación son las 4 casillas centrales (filas 4–5, columnas 4–5) y siempre es agua. La diagonal entre dos casillas que bloquean también bloquea. El mapa estándar lo lee `LectorMapas` desde `resources/mapas/estandar.txt`; si hay obstáculos fuera de las filas 3 a 6, `Tablero` lanza `MapaInvalidoException`.

## modelo — Barcos (Santi)

Cada barco sabe su vida, sus cooldowns y qué puede hacer este turno; no sabe dónde están los obstáculos ni los enemigos. La partida le pregunta y después le ordena.

| Clase | Tipo | Qué hace | Miembros clave |
| --- | --- | --- | --- |
| `EstadisticasBarco` | `record` | Valores fijos de un tipo de barco. Valida que nada sea negativo. | `vidaMaxima`, `costoMovimiento`, `movimientoMaxPorTurno`, `rangoDisparo`, `vision`, `danio`, `cooldownDisparoMax`, `cooldownHabilidadMax` |
| `Barco` | Clase abstracta | Estado de un barco y reglas propias: daño, curación, cooldowns, movimiento del turno, habilidad (Template Method) y revelado. | Constructor `protected Barco(int id, EstadisticasBarco, Posicion)`. `recibirDanio`, `curar`, `estaHundido`, `getDanio`, `getRangoDisparo`, `puedeDisparar`, `registrarDisparo`, `puedeMoverse`, `getCasillasRestantes`, `moverA`, `ubicarEn`, `puedeUsarHabilidad`, `usarHabilidad()` (final), `protected abstract void aplicarHabilidad()`, `reiniciarTurno`, `marcarRevelado`, `estaRevelado`, `abstract char getSimbolo()`, `restaurar(...)` (recibe vida, posición, cooldowns y flags guardados) |
| `Destructor` | Subclase | Vida 3, costo 1, 4 casillas, rango 2, visión 2, daño 3, cooldown 0. Habilidad: daño doble este turno. | `aplicarHabilidad()` activa el flag; `getDanio()` devuelve 6 si está activa. Símbolo `D` |
| `Crucero` | Subclase | Vida 5, costo 2, 3 casillas, rango 4, visión 3, daño 2, cooldown 0. Habilidad: se repara 2. | `aplicarHabilidad()` llama a `curar(2)`. Símbolo `C` |
| `Acorazado` | Subclase | Vida 8, costo 3, 2 casillas, rango 6, visión 5, daño 4, cooldown 1. Habilidad: rango 8 este turno. | `getRangoDisparo()` devuelve 8 si está activa. Símbolo `A` |
| `FabricaFlota` | Clase utilitaria | Crea la flota estándar con ids únicos, así `Jugador` no conoce las subclases (Creador). | `static List<Barco> crearFlotaEstandar(int primerId)`: 1 acorazado, 2 cruceros, 3 destructores, sin posición |

Todas las habilidades tienen espera de 3 turnos, duran hasta el fin del turno, no cuestan puntos y no revelan al barco. Un barco que disparó no puede moverse en ese turno, y queda revelado para el rival durante su turno siguiente.

## modelo — Jugador (Bota)

| Clase | Tipo | Qué hace | Miembros clave |
| --- | --- | --- | --- |
| `Jugador` | Clase | Dueño de una flota de 6 barcos y de sus puntos del turno. Responde preguntas sobre su flota (Experto). | `Jugador(String nombre, int numero, List<Barco> flota)`, `getNombre`, `getNumero` (1 o 2), `getFlota` (no modificable), `getBarcosVivos`, `tieneBarcosVivos`, `getVidaTotal`, `getPuntosMovimiento`, `tienePuntos(int)`, `gastarPuntos(int)`, `reponerPuntos(int)`, `getPuntosDominacion`, `sumarPuntoDominacion`, `getResumen` |
| `ResumenTurno` | Clase | Lo que el rival le hizo al jugador desde su último turno; se muestra al volver del blackout y se limpia. | `agregarDisparoRecibido(Barco objetivo, int danio, Posicion origen)`, `agregarBarcoHundido(Barco, Posicion)`, `getMensajes` (no modificable), `limpiar`, `estaVacio` |

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
| `GestorPartida` | Clase | Implementa `Partida`: valida cada acción, la aplica, la anota en el historial y avisa a los observadores. | Constantes `TURNOS_MAX = 40`, `PUNTOS_MOVIMIENTO_TURNO = 6`, `PUNTOS_DOMINACION_VICTORIA = 6`. Atributos: `jugador1`, `jugador2`, `jugadorActivo`, `tablero` (agregación), `fase`, `ronda`, `barcoSeleccionado`, `rutaPlanificada`, `historial`, `resultado`, `accionRealizadaEnTurno`, `List<ObservadorPartida> observadores`. Públicos para guardar: `crearInstantanea()` y `static restaurar(InstantaneaPartida, Tablero)` (la lista de observadores queda vacía; la vista se registra de nuevo). Privados: `iniciarTurno`, `cambiarJugadorActivo`, `esVisibleParaJugador`, `actualizarDominacion`, `verificarVictoria`, `resolverLimiteDeTurnos`, `registrarEvento(TipoEvento, String)`, `notificar...`, `validarEstado()` |
| `InstantaneaPartida`, `DatosJugador`, `DatosBarco` | `record` | Foto completa de una partida para guardarla (Memento): es lo único que ven los repositorios. | `InstantaneaPartida(String mapa, int jugadorActivo, int ronda, FasePartida fase, List<DatosJugador> jugadores, List<DatosBarco> barcos, List<Posicion> restos, List<EventoPartida> eventos)`; `DatosJugador(numero, nombre, puntosMovimiento, puntosDominacion, List<String> resumenPendiente)`; `DatosBarco(id, tipo, jugador, posicion, vida, cooldownDisparo, cooldownHabilidad, casillasRestantes, habilidadActiva, revelado)` |
| `ObservadorPartida` | Interfaz | La implementan la vista y el autoguardado para enterarse de cambios. | `onEstadoCambiado()`, `onFaseCambiada(FasePartida)`, `onTurnoIniciado(String nombreJugador)`, `onPartidaTerminada(ResultadoPartida)` |
| `EstadoBarco` | `record` | Foto de solo lectura de un barco para la vista. | `id`, `simbolo`, `posicion`, `vida`, `vidaMaxima`, `casillasRestantes`, `cooldownDisparo`, `cooldownHabilidad`, `habilidadActiva`, `rangoDisparo`, `vision`, `esPropio` |
| `EstadoJugador` | `record` | Foto de solo lectura de un jugador. | `nombre`, `numero`, `puntosMovimiento`, `puntosDominacion`, `barcosVivos`, `vidaTotal` |
| `ResultadoPartida` | `record` | Cómo terminó la partida; lo usan la pantalla final y las estadísticas. | `fecha`, `jugador1`, `jugador2`, `ganador` (`null` si empate), `rondas`, `motivo`; `esEmpate()` |
| `MotivoFin` | `enum` | Por qué terminó. | HUNDIMIENTO, DOMINACION, LIMITE_TURNOS, EMPATE, ABANDONO |

## excepciones

Las de reglas son unchecked: la vista ya deshabilita lo inválido, así que llegar ahí es un error que se muestra en la GUI sin cortar el juego. `PersistenciaException` es checked porque un archivo puede fallar aunque el código esté bien, y el compilador obliga a manejarlo.

| Clase | Extiende | Se lanza cuando |
| --- | --- | --- |
| `AccionInvalidaException` | `RuntimeException` | Base de las de reglas; acción en una fase que no corresponde |
| `DespliegueInvalidoException` | `AccionInvalidaException` | Casilla fuera de zona, ocupada u obstáculo; confirmar sin los 6 |
| `MovimientoInvalidoException` | `AccionInvalidaException` | Paso no adyacente, bloqueado, sin puntos o barco que ya disparó |
| `DisparoInvalidoException` | `AccionInvalidaException` | Objetivo no visible, fuera de rango, sin línea de visión o tirador en cooldown |
| `HabilidadNoDisponibleException` | `AccionInvalidaException` | Habilidad en espera o ya usada este turno |
| `MapaInvalidoException` | `RuntimeException` | Mapa mal formado u obstáculos fuera de la zona neutral |
| `PersistenciaException` | `Exception` | Cualquier error al leer o escribir archivos; envuelve la causa original |

## historial — Historial de partida (Bota)

El **historial de partida** es parte del juego: lo ve el jugador, se guarda con la partida y se exporta al final. No hay log técnico (`java.util.logging` queda fuera): los errores se muestran en la GUI y las acciones rechazadas no se anotan.

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

`record` inmutable: una entrada del historial.

```java
public record EventoPartida(int ronda, int numeroJugador, TipoEvento tipo,
                            String descripcion, LocalDateTime momento) {
    public boolean esVisiblePara(int numeroJugador) // tipo.esPublico() || this.numeroJugador == numeroJugador
    public String aLinea()                         // "[R12] J2 DISPARO: Acorazado (7,4) -> Crucero (4,4), -4"
}
```

### HistorialPartida

Clase que guarda los eventos en orden. La crea `GestorPartida` y viaja dentro del guardado.

```java
private final List<EventoPartida> eventos = new ArrayList<>();
public void registrar(EventoPartida evento)          // rechaza null
public List<EventoPartida> getEventos()              // List.copyOf
public List<EventoPartida> getEventosDeRonda(int ronda)
public List<EventoPartida> getEventosVisiblesPara(int numeroJugador)
public int cantidad()
```

**Cómo se conecta:** `GestorPartida` tiene un método privado `registrarEvento(TipoEvento tipo, String descripcion)` que agrega el `EventoPartida` al historial. Cada acción confirmada lo llama una vez. Las acciones rechazadas no van al historial: la vista muestra el mensaje de la excepción.

## persistencia — Archivos (Marcos + Santi)

Se guardan cuatro cosas: la partida (texto), las estadísticas (CSV), la configuración (`.properties`) y el historial exportado (`.txt`); además se lee el mapa (`.txt`). Todo vive bajo `datos/` en la carpeta donde corre el juego, y la vista solo conoce las interfaces `RepositorioPartidas` y `RepositorioEstadisticas` (DIP).

```text
datos/
  config.properties
  estadisticas.csv
  partidas/partida1.sav, autoguardado.sav
  historiales/2026-10-05_1432_Bota-vs-Santi.txt
src/main/resources/mapas/estandar.txt
```

**Reparto:** Marcos hace `RutasApp`, `LectorMapas`, `ConfiguracionJuego` y `ExportadorHistorial`. Santi hace la instantánea (`crearInstantanea`/`restaurar`), los repositorios de partidas y estadísticas, `CabeceraGuardado`, `EstadisticaJugador` y `CalculadoraRanking`.

### RutasApp

Utilitaria con las rutas fijas, para que ningún otro archivo escriba un `"datos/..."` a mano.

```java
public static final Path BASE = Path.of("datos");
public static final Path PARTIDAS = BASE.resolve("partidas");
public static final Path HISTORIALES, ESTADISTICAS, CONFIGURACION;
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

`record` que se escribe como segunda línea del archivo. Permite listar guardados (nombres, ronda, fecha) leyendo solo esa línea y chequear la versión del formato.

```java
public record CabeceraGuardado(int versionFormato, String nombre, LocalDateTime fecha,
        String jugador1, String jugador2, int ronda, FasePartida fase) {}
```

### RepositorioPartidasArchivo

`implements RepositorioPartidas`. Un archivo de **texto** `partidas/<nombre>.sav` por partida, escrito con `BufferedWriter` y leído con `BufferedReader` (try-with-resources), igual que `LectorMapas`. No hay serialización de objetos.

```java
public static final int VERSION_FORMATO = 1;
private final Path carpeta;

// guardar: valida el nombre (letras, números, - y _, hasta 40), exige partida.puedeGuardarse(),
//   pide partida.crearInstantanea() y escribe el archivo. Cualquier IOException -> PersistenciaException.
// cargar: lee línea por línea, chequea la marca y VERSION_FORMATO, arma la InstantaneaPartida,
//   llama a GestorPartida.restaurar(instantanea, tablero) y a validarEstado().
//   Línea mal formada -> PersistenciaException("Línea 14 del guardado inválida"), con la causa original.
// listar: lee solo las dos primeras líneas de cada archivo; uno roto se saltea.
```

**Formato del archivo** (una línea por dato, campos separados por `;`; los nombres de jugador no pueden tener `;`):

```text
BATALLA-NAVAL;1
CABECERA;partida1;2026-10-05T14:32:10;Bota;Santi;12;EN_JUEGO
PARTIDA;estandar;2
JUGADOR;1;Bota;6;1
JUGADOR;2;Santi;6;0
RESUMEN;2;Tu Crucero (4,4) recibió 4 de daño desde (7,4)
BARCO;1;A;1;1;3;8;0;2;2;false;false
BARCO;2;C;1;0;5;5;0;0;3;false;true
RESTOS;5;3
EVENTO;12;2;DISPARO;2026-10-05T14:31:50;Acorazado (7,4) -> Crucero (4,4), -4
```

| Línea | Campos |
| --- | --- |
| `BATALLA-NAVAL` | marca + versión del formato |
| `CABECERA` | nombre, fecha, jugador1, jugador2, ronda, fase (es lo único que lee `listar`; ronda y fase de la partida salen de acá) |
| `PARTIDA` | nombre del mapa (recurso), número del jugador activo |
| `JUGADOR` | número, nombre, puntos de movimiento, puntos de dominación |
| `RESUMEN` | número del jugador, mensaje pendiente (el mensaje es el último campo y puede tener `;`) |
| `BARCO` | id, tipo (`D`/`C`/`A`), jugador, fila, columna (vacías si aún no se ubicó), vida, cooldown de disparo, cooldown de habilidad, casillas restantes, habilidad activa, revelado |
| `RESTOS` | fila, columna de cada barco hundido |
| `EVENTO` | ronda, jugador, tipo, momento, descripción (último campo, puede tener `;`; se lee con `split(";", 6)`) |

**Cuándo se puede guardar:** `GestorPartida.puedeGuardarse()` devuelve `true` solo al inicio de un turno (sin acciones realizadas, sin ruta planificada) o en la pantalla de blackout. Así nunca se guarda un movimiento a medias, y por eso la instantánea no necesita ruta ni barco seleccionado. Después de cargar, la vista se registra de nuevo como observador.

### RepositorioEstadisticas (interfaz) y RepositorioEstadisticasCsv

```java
void registrar(ResultadoPartida resultado) throws PersistenciaException;
List<ResultadoPartida> obtenerTodos() throws PersistenciaException;
```

La implementación CSV usa `;` como separador y una línea de encabezado `fecha;jugador1;jugador2;ganador;rondas;motivo`. `registrar` agrega con `Files.newBufferedWriter(archivo, CREATE, APPEND)` (si el archivo no existía, escribe antes el encabezado). `obtenerTodos` lee con `BufferedReader`; una línea mal formada se saltea y no rompe el resto. Los nombres de jugador no pueden tener `;` (se valida en la pantalla de nueva partida).

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

Envuelve un `java.util.Properties`. Si el archivo no existe o está roto, usa valores por defecto y lo reescribe.

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
| `TareaGuardar` | `implements Runnable` | Se ejecuta en un `Thread` propio para no congelar la ventana; al terminar, con `SwingUtilities.invokeLater`, avisa éxito o muestra el error de `PersistenciaException`. |
| `TareaCargar` | `implements Runnable` | Carga en un `Thread` propio; con `invokeLater` entrega la partida a `ControladorAplicacion` o muestra el error. |

`TareaGuardar` y `TareaCargar` son las que cubren hilos (Unidad 6): se crean con `new Thread(tarea).start()`. Todo cambio de componentes Swing se hace en el EDT.

## app — Arranque (Bota)

| Clase | Tipo | Qué hace |
| --- | --- | --- |
| `Main` | Clase con `main` | En orden: `RutasApp.crearCarpetas()` y después `SwingUtilities.invokeLater(() -> new ControladorAplicacion().iniciar())`. |
| `ControladorAplicacion` | Clase | Arma todo (inyección por constructor a mano): crea `ConfiguracionJuego`, `RepositorioPartidasArchivo`, `RepositorioEstadisticasCsv` y `VentanaPrincipal`. Crea o carga la partida (con `GestorPartida.restaurar`), registra observadores y decide qué pantalla mostrar según los eventos (`onFaseCambiada`, `onTurnoIniciado`, `onPartidaTerminada`). Al terminar: registra el resultado, exporta el historial y muestra `PanelFinPartida`. |
| `AutoGuardado` | `implements ObservadorPartida` | Si `autoguardado=true`, en cada `onTurnoIniciado` guarda `autoguardado.sav` con una `TareaGuardar`. Un fallo se avisa en la barra de estado y no interrumpe la partida. |

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
| `RepositorioPartidasArchivoTest` | Guardar y cargar da una instantánea igual; archivo corrupto (con número de línea), de otra versión o inexistente lanza `PersistenciaException`; `listar` saltea archivos rotos | Santi |
| `RepositorioEstadisticasCsvTest` | Append, encabezado una sola vez, líneas rotas salteadas | Santi |
| `CalculadoraRankingTest` | Conteo y orden del ranking, empates | Santi |
| `LectorMapasTest` y `ConfiguracionJuegoTest` | Mapa estándar correcto, mapa mal formado, config faltante cae en defaults | Marcos |
