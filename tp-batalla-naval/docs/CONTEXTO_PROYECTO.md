# CONTEXTO DEL PROYECTO — Batalla Naval Táctica

> **Para la IA que lee esto:** sos asistente de un integrante del grupo que desarrolla este TP. Usá este archivo como fuente de verdad sobre reglas, diseño y convenciones. Si algo que te piden contradice este archivo, avisalo antes de escribir código. No inventes clases, métodos ni reglas que no estén acá: si falta algo, proponelo como sugerencia y marcá que hay que consultarlo con el grupo. Respondé en español rioplatense. Explicá el código que escribas: **la defensa del TP es individual y cada integrante tiene que poder explicar todo**.

---

## 1. Qué es

Trabajo práctico de **Paradigma Orientado a Objetos (3.4.208), UADE, 2º cuatrimestre 2026**. Juego por turnos para 2 jugadores en la misma computadora (hot-seat), en **Java + Swing**, que mezcla la batalla naval clásica con mecánicas de World of Warships: movimiento, rango, visión y niebla de guerra.

La materia evalúa: correspondencia UML ↔ código, encapsulamiento, herencia, clases abstractas, interfaces, polimorfismo, enums, clases inmutables, GRASP, SOLID, colecciones con `Comparable`/`Comparator`, excepciones propias, entrada/salida con flujos, Swing con eventos e hilos. El código tiene que mostrar esos conceptos de forma clara.

## 2. Equipo

| Integrante | GitHub | Rol |
|---|---|---|
| Agustín "Bota" | `Botitaa` | Partida y reglas (`controlador`), logs, `app`, organización de Git y revisión de PRs |
| Marcos | `enevoldsenmar` | Tablero y mapa (`modelo`: Posicion, Casilla, Tablero, Navegacion) + persistencia base |
| Santi | `Faccio7L` | Barcos y combate (`modelo`: Barco y subclases) + persistencia de partidas y estadísticas + UML |
| Bruno | `Bruno-Dominguez` | Vista del tablero en Swing (`PanelTablero`) |
| Guille | `guillermosap111` | Pantallas y flujo en Swing (ventana, pantallas, menú) |

## 3. Reglas del juego (resumen completo)

### Tablero
- 10x10. Filas 0–2: despliegue J1. Filas 3–6: zona neutral (único lugar con obstáculos). Filas 7–9: despliegue J2.
- Zona de dominación: las 4 casillas centrales (filas 4–5, columnas 4–5), siempre agua.
- Mapas en espejo (simetría punto a punto respecto del centro). El MVP tiene un solo mapa:

```
     0 1 2 3 4 5 6 7 8 9
  0  A A A A A A A A A A
  1  A A A A A A A A A A
  2  A A A A A A A A A A
  3  ~ ~ I I ~ ~ ~ P ~ ~
  4  ~ ~ I ~ ~ ~ P ~ ~ ~
  5  ~ ~ ~ P ~ ~ ~ I ~ ~
  6  ~ ~ P ~ ~ ~ I I ~ ~
  7  B B B B B B B B B B
  8  B B B B B B B B B B
  9  B B B B B B B B B B
A = despliegue J1, B = despliegue J2, ~ = agua, I = isla, P = piedra
```

| Casilla | Bloquea paso | Bloquea visión |
|---|---|---|
| AGUA | No | No |
| PIEDRA | Sí | No |
| ISLA | Sí | Sí |
| RESTOS (barco hundido) | No | No |

- Los barcos bloquean el paso, pero no la visión ni los disparos.
- Diagonal entre dos casillas ortogonales que bloquean → la diagonal también bloquea (paso o visión según el tipo). Si bloquea solo una, está libre.
- Todas las distancias son de **Chebyshev**: `max(|f1-f2|, |c1-c2|)`.

### Barcos (cada jugador: 1 acorazado, 2 cruceros, 3 destructores; cada barco ocupa 1 casilla)

| Atributo | Destructor | Crucero | Acorazado |
|---|---|---|---|
| Vida | 3 | 5 | 8 |
| Costo por casilla (puntos) | 1 | 2 | 3 |
| Máx. casillas por turno | 4 | 3 | 2 |
| Rango de disparo | 2 | 4 | 6 |
| Visión | 2 | 3 | 5 |
| Daño | 3 | 2 | 4 |
| Cooldown de disparo (turnos) | 0 | 0 | 1 |
| Habilidad | Daño doble (6) este turno | Se repara 2 (sin pasar la vida máx.) | Rango 8 este turno |

- Habilidades: se activan con un botón, una vez por turno, no cuestan puntos, no impiden moverse, duran hasta el fin del turno, **espera de 3 turnos** (usada en el turno 1 → disponible en el 4), no revelan al barco. Hay que activarlas antes de disparar para que afecten el disparo.

### Turno
1. Al volver del blackout: resumen de lo que hizo el rival (disparos recibidos con daño y origen, barcos propios hundidos).
2. Se reponen **6 puntos de movimiento** para toda la flota (no se acumulan) y bajan en 1 los cooldowns.
3. El jugador activa sus barcos de a uno en el orden que quiera: **primero mover, después disparar** (ambos opcionales). Un barco que disparó no puede moverse.
4. "Terminar turno" → blackout → el otro toca "Listo".

### Movimiento
- Paso a paso a una de las 8 adyacentes. Solo a AGUA o RESTOS libres, sin salir del tablero.
- Se planifica la ruta (con "Deshacer paso") y después se confirma; mientras se planifica la niebla no cambia.
- Si al confirmar la ruta choca con un enemigo oculto: frena en el paso anterior, se cobran solo los pasos hechos, el enemigo queda revelado hasta fin del turno y el rival no se entera.

### Disparo
Una vez por turno, a un barco enemigo, si: (1) es visible para el jugador, (2) está a distancia ≤ rango, (3) hay línea de visión (no se dispara por encima de islas), (4) el tirador no está en cooldown. **Siempre acierta.** Vida 0 → se hunde y su casilla pasa a RESTOS.

### Visión y niebla
- Una casilla es visible para un jugador si algún barco propio vivo está a distancia ≤ su visión y tiene línea de visión.
- **Visión compartida:** si un barco ve a un enemigo, cualquier barco propio puede dispararle (si cumple su rango y línea de visión).
- Línea de visión: Bresenham entre centros; si una casilla intermedia es ISLA, no hay visión. Los extremos no cuentan.
- **Revelado por disparo:** el que dispara queda visible para el rival durante todo el turno siguiente del rival.
- Los barcos en la zona de dominación son visibles para ambos.
- El mapa y los restos se ven siempre.

### Victoria
- **Hundimiento:** hundir los 6 barcos rivales (termina en el acto).
- **Dominación:** sumar 6 puntos. Al terminar su turno, un jugador suma 1 si tiene ≥1 barco en la zona y el rival ninguno. Los puntos no se pierden.
- **Límite:** 40 turnos por jugador. Gana más dominación → más barcos vivos → más vida total → empate.

## 4. Arquitectura

Todo bajo el paquete raíz `batallanaval`. Dependencias en un solo sentido.

| Paquete | Contiene | Puede usar |
|---|---|---|
| `modelo` | Posicion, Casilla, Tablero, Navegacion, barcos, Jugador, ResumenTurno | `excepciones` |
| `controlador` | Interfaces de partida, GestorPartida, FasePartida, ObservadorPartida, records de solo lectura | `modelo`, `log`, `excepciones` |
| `excepciones` | Excepciones propias | — |
| `log` | Log técnico e historial de partida | `excepciones` |
| `persistencia` | Guardar/cargar, estadísticas, configuración, mapas, exportación | `modelo`, `controlador`, `log`, `excepciones` |
| `vista` | Swing | interfaces y records de `controlador`, interfaces de `persistencia`, `excepciones` |
| `app` | Main, ControladorAplicacion, AutoGuardado | todo |

**Reglas de diseño que no se rompen:**
- El modelo no conoce a la vista. La vista no recibe nunca `Barco`, `Jugador` ni `Tablero`: recibe `EstadoBarco`, `EstadoJugador` y `Casilla` (inmutables).
- Toda regla vive en `GestorPartida`. Los botones no deciden si una acción es válida: llaman a la partida y la vista se redibuja.
- Atributos `private`; lo que no cambia, `final`. Las colecciones se devuelven con `List.copyOf(...)`.
- Nada de `System.out`: se usa el logger.

## 5. Clases por paquete

### modelo
- `Posicion` — final, inmutable, Serializable. `getFila`, `getColumna`, `distanciaChebyshev`, `equals`, `hashCode`, `toString`.
- `Casilla` — enum AGUA, PIEDRA, ISLA, RESTOS. `bloqueaPaso`, `bloqueaVision`, `getSimbolo`, `static desdeSimbolo(char)`.
- `Tablero` — Serializable. `FILAS = 10`, `COLUMNAS = 10`, `getCasilla`, `estaDentro`, `esTransitable`, `getVecinos`, `esDiagonalBloqueada`, `hayLineaDeVision`, `esZonaDespliegue(pos, numeroJugador)`, `esZonaDominacion`, `convertirEnRestos`. Valida el mapa al construirse (copia defensiva de la grilla); obstáculos fuera de filas 3–6 → `MapaInvalidoException`.
- `Navegacion` — utilitaria. `static Set<Posicion> casillasAlcanzables(Tablero, Posicion origen, int pasosMax, Set<Posicion> ocupadas)` (BFS).
- `EstadisticasBarco` — record con los valores fijos (vidaMaxima, costoMovimiento, movimientoMaxPorTurno, rangoDisparo, vision, danio, cooldownDisparoMax, cooldownHabilidadMax); valida no negativos.
- `Barco` — abstracta, Serializable. Constructor `protected Barco(int id, EstadisticasBarco, Posicion)`. `recibirDanio`, `curar`, `estaHundido`, `getDanio`, `getRangoDisparo`, `puedeDisparar`, `registrarDisparo`, `puedeMoverse`, `getCasillasRestantes`, `moverA`, `ubicarEn`, `puedeUsarHabilidad`, `final usarHabilidad()` (Template Method: valida, llama a `aplicarHabilidad()`, pone la espera), `protected abstract void aplicarHabilidad()`, `reiniciarTurno`, `marcarRevelado`, `estaRevelado`, `abstract char getSimbolo()`.
- `Destructor` (`D`), `Crucero` (`C`), `Acorazado` (`A`) — fijan sus estadísticas y sobreescriben `aplicarHabilidad()`; Destructor sobreescribe `getDanio()` y Acorazado `getRangoDisparo()`.
- `FabricaFlota` — `static List<Barco> crearFlotaEstandar(int primerId)`.
- `Jugador` — Serializable. `Jugador(String nombre, int numero, List<Barco> flota)`, `getFlota` (no modificable), `getBarcosVivos`, `tieneBarcosVivos`, `getVidaTotal`, puntos de movimiento (`tienePuntos`, `gastarPuntos`, `reponerPuntos`), puntos de dominación, `getResumen`.
- `ResumenTurno` — `agregarDisparoRecibido`, `agregarBarcoHundido`, `getMensajes`, `limpiar`, `estaVacio`.

### controlador
- `FasePartida` — enum DESPLIEGUE_J1, DESPLIEGUE_J2, EN_JUEGO, TERMINADA.
- `PartidaDespliegue` — `ubicarBarco(int idBarco, Posicion)`, `retirarBarco(int)`, `getBarcosSinUbicar()`, `confirmarDespliegue()`.
- `PartidaCombate` — `seleccionarBarco(int)`, `agregarPaso(Posicion)`, `deshacerPaso()`, `confirmarMovimiento()`, `usarHabilidad()`, `disparar(int idObjetivo)`, `terminarTurno()`.
- `PartidaConsulta` — `getCasilla`, `getJugadorActivo`/`getRival` (`EstadoJugador`), `getBarcosPropios`, `getBarcosEnemigosVisibles` (`List<EstadoBarco>`), `getBarcoSeleccionado` (`Optional<EstadoBarco>`), `getRutaPlanificada`, `getCostoRuta`, `getCasillasAlcanzables`, `getObjetivosValidos`, `esVisible(Posicion)`, `getMensajesResumen`, `getHistorialVisible`, `getRonda`, `getFase`, `estaTerminada`, `getResultado` (`Optional<ResultadoPartida>`), `puedeGuardarse`.
- `Partida` — `extends PartidaDespliegue, PartidaCombate, PartidaConsulta` + `agregarObservador`/`quitarObservador`. **Es el único tipo que conoce la vista.**
- `GestorPartida` — implements `Partida`, Serializable. Constantes `TURNOS_MAX = 40`, `PUNTOS_MOVIMIENTO_TURNO = 6`, `PUNTOS_DOMINACION_VICTORIA = 6`. Recibe el `Tablero` por constructor (agregación). `transient List<ObservadorPartida> observadores` (recreada en `readObject`). Privados: `iniciarTurno`, `cambiarJugadorActivo`, `esVisibleParaJugador`, `actualizarDominacion`, `verificarVictoria`, `resolverLimiteDeTurnos`, `registrarEvento(TipoEvento, String)`, `validarEstado()`.
- `ObservadorPartida` — `onEstadoCambiado()`, `onFaseCambiada(FasePartida)`, `onTurnoIniciado(String nombreJugador)`, `onPartidaTerminada(ResultadoPartida)`.
- `EstadoBarco` (record) — id, simbolo, posicion, vida, vidaMaxima, casillasRestantes, cooldownDisparo, cooldownHabilidad, habilidadActiva, rangoDisparo, vision, esPropio.
- `EstadoJugador` (record) — nombre, numero, puntosMovimiento, puntosDominacion, barcosVivos, vidaTotal.
- `ResultadoPartida` (record, Serializable) — fecha, jugador1, jugador2, ganador (`null` = empate), rondas, motivo; `esEmpate()`.
- `MotivoFin` — enum HUNDIMIENTO, DOMINACION, LIMITE_TURNOS, EMPATE, ABANDONO.

### excepciones
- `AccionInvalidaException extends RuntimeException` (base de reglas) → `DespliegueInvalidoException`, `MovimientoInvalidoException`, `DisparoInvalidoException`, `HabilidadNoDisponibleException`.
- `MapaInvalidoException extends RuntimeException`.
- `PersistenciaException extends Exception` (**checked**, envuelve la causa original).

Criterio: las de reglas son unchecked porque la vista ya deshabilita lo inválido; si llegan, se muestran y se loguean. La de persistencia es checked porque un archivo puede fallar aunque el código esté bien.

### log
- **Log técnico** con `java.util.logging` (sin librerías externas). Cada clase: `private static final Logger LOG = Logger.getLogger(MiClase.class.getName());`
  - `ConfiguracionLog.inicializar(Path)` — consola INFO; archivo `datos/logs/batalla-naval-%g.log` en FINE, 1 MB x 3 rotando, append. Idempotente.
  - `FormatoLog extends Formatter` — `2026-10-05 14:32:10 [FINE] GestorPartida - mensaje` + stack trace.
  - `ManejadorErroresGlobal implements Thread.UncaughtExceptionHandler` — loguea SEVERE y muestra un `JOptionPane` desde el EDT.
  - Niveles: **SEVERE** excepción inesperada o error de E/S · **WARNING** acción inválida que llegó a la lógica, archivo corrupto, línea mal formada · **INFO** inicio/fin de partida, cambio de fase, guardar/cargar · **FINE** cada acción del juego.
- **Historial de partida** (lo ve el jugador, viaja en el guardado):
  - `TipoEvento` — enum con `boolean publico`: DESPLIEGUE, MOVIMIENTO, HABILIDAD privados; DISPARO, HUNDIMIENTO, DOMINACION, FIN_TURNO, FIN_PARTIDA públicos.
  - `EventoPartida` — record Serializable (ronda, numeroJugador, tipo, descripcion, momento); `esVisiblePara(int)`, `aLinea()`.
  - `HistorialPartida` — Serializable: `registrar`, `getEventos`, `getEventosDeRonda`, `getEventosVisiblesPara(int)`, `cantidad`.
  - `GestorPartida.registrarEvento(...)` agrega al historial **y** loguea FINE. Solo acciones confirmadas; los rechazos van solo al log (WARNING).

### persistencia
Todo bajo `datos/` (carpeta donde corre el juego):
```
datos/config.properties
datos/estadisticas.csv
datos/logs/batalla-naval-0.log
datos/partidas/<nombre>.sav, autoguardado.sav
datos/historiales/<fecha_hora>_<J1>-vs-<J2>.txt
src/main/resources/mapas/estandar.txt
```
- `RutasApp` — constantes `Path` + `crearCarpetas()`. Nadie escribe `"datos/..."` a mano.
- `LectorMapas.cargar(String recurso)` — lee el mapa de `resources/mapas/` con `BufferedReader`; error → `PersistenciaException` con número de línea.
- `RepositorioPartidas` (interfaz) — `guardar(GestorPartida, String)`, `cargar(String)`, `listar()` (`List<CabeceraGuardado>`), `borrar`, `existe`.
- `CabeceraGuardado` — record Serializable (versionFormato, nombre, fecha, jugador1, jugador2, ronda, fase). Se escribe antes de la partida para listar sin deserializar todo.
- `RepositorioPartidasArchivo` — `ObjectOutputStream`: cabecera + `GestorPartida`. Guarda en `.sav.tmp` y mueve con `ATOMIC_MOVE` (un corte no rompe el guardado anterior). Al cargar: chequea `VERSION_FORMATO = 1`, deserializa y llama a `validarEstado()`. `IOException`, `ClassNotFoundException`, `ClassCastException`, `InvalidClassException` → `PersistenciaException`.
- Solo se guarda cuando `puedeGuardarse()` es `true` (inicio de turno o blackout, sin ruta a medias).
- Serializable: `Posicion`, `Tablero`, `EstadisticasBarco`, `Barco` (+ subclases), `Jugador`, `ResumenTurno`, `GestorPartida`, `HistorialPartida`, `EventoPartida`, `ResultadoPartida`, `CabeceraGuardado`. Todos con `private static final long serialVersionUID = 1L;`.
- `RepositorioEstadisticas` (interfaz) / `RepositorioEstadisticasCsv` — `registrar(ResultadoPartida)` en append con encabezado `fecha;jugador1;jugador2;ganador;rondas;motivo`; `obtenerTodos()` saltea líneas rotas con WARNING.
- `EstadisticaJugador` (record: nombre, jugadas, ganadas, perdidas, empatadas; `porcentajeVictorias()`) y `CalculadoraRanking.calcular(List<ResultadoPartida>)` con `Map` + `Comparator` (ganadas, porcentaje, nombre).
- `ConfiguracionJuego` — `Properties` (jugador1.ultimo, jugador2.ultimo, autoguardado, carpeta.partidas); si falta o está roto, defaults + WARNING.
- `ExportadorHistorial.exportar(historial, resultado, carpeta)` — `.txt` al terminar la partida.

### vista
- Tablero (Bruno): `PanelTablero extends JPanel implements ObservadorPartida` (dibuja todo, convierte clics en `Posicion`, key bindings Esc/Backspace/Enter, tooltip), `ModoInteraccion` (DESPLIEGUE, SELECCION, PLANIFICANDO_RUTA, ELIGIENDO_OBJETIVO, BLOQUEADO), `PaletaColores`.
- Pantallas (Guille): `VentanaPrincipal` (JFrame + `CardLayout`), `Pantalla` (enum), `PanelInicio`, `PanelNuevaPartida`, `PanelDespliegue`, `PanelBlackout` (único lugar donde se guarda), `PanelResumen`, `PanelJuego`, `PanelLateral`, `PanelHistorial`, `PanelFinPartida`, `PanelCargarPartida`, `PanelEstadisticas`, `PanelReglas`, `BarraMenuJuego`, `TareaGuardar` y `TareaCargar` (`SwingWorker`, cubren hilos).
- Código visual: agua azul, niebla azul oscuro grisado, isla verde, piedra gris, restos X negra, dominación borde dorado, barco = letra + vida (ej. `C5`) con color por jugador, seleccionado borde amarillo, rango tinte rojo suave, ruta celeste numerada, objetivos con borde rojo.
- Todo cambio de componentes Swing en el EDT (`SwingUtilities.invokeLater`).

### app
- `Main` — `RutasApp.crearCarpetas()` → `ConfiguracionLog.inicializar(...)` → `ManejadorErroresGlobal.instalar()` → `invokeLater(() -> new ControladorAplicacion().iniciar())`.
- `ControladorAplicacion` — crea repositorios, configuración y ventana; crea o carga la partida; registra observadores; cambia de pantalla según eventos; al terminar registra estadísticas y exporta el historial.
- `AutoGuardado implements ObservadorPartida` — guarda `autoguardado.sav` en cada `onTurnoIniciado` si está habilitado; si falla, WARNING y sigue.

## 6. Convenciones del equipo

- **Entorno:** última versión LTS del JDK (la misma para todos), IntelliJ IDEA, Maven, UTF-8, JUnit 5.
- **Nombres:** en español, sin tildes ni ñ (`danio`, no `daño`). Clases `PascalCase`, métodos y atributos `camelCase`, constantes `MAYUSCULAS`. Métodos con verbo (`puedeDisparar`, `calcularDanio`).
- **Git:**
  - `main` solo recibe versiones entregadas, con tag (`entrega-1`, `entrega-2`, …, `v1.0`).
  - `develop` es la rama de integración.
  - Cada tarea es un issue y se trabaja en su rama desde `develop`: `tarea/<numero-issue>-<descripcion>` (ej. `tarea/12-linea-de-vision`).
  - Commits en español, en presente, con el número de issue: `#12 Agrega calculo de linea de vision`. Chicos, uno por cambio lógico.
  - Todo entra a `develop` por PR, que revisa Bota. Nadie aprueba su propio PR. El autor resuelve sus conflictos.
- **Definición de terminado:** compila sin warnings, pasan los tests, respeta este diseño (si lo cambia, se actualiza el UML en el mismo PR), Javadoc en métodos públicos, otro integrante lo revisó y lo puede explicar.
- **Tests:** todo lo que no es Swing. Los de persistencia usan `@TempDir`. Los de reglas simulan partidas completas por código.

## 7. Cómo pedirle ayuda a la IA (para el integrante)

Al empezar una tarea, pasale a la IA este archivo y el texto del issue. Pedidos útiles:
- "Implementá `<Clase>` según el CONTEXTO, con Javadoc y tests JUnit 5. Explicame cada decisión."
- "Revisá este código contra el CONTEXTO: encapsulamiento, nombres, excepciones y logs."
- "Haceme 5 preguntas de defensa sobre esta clase y corregime las respuestas."

Si la IA propone cambiar una firma pública de otro módulo, **no lo hagas solo**: abrí un comentario en el issue y avisale al dueño del módulo.

## 8. Documentos de referencia

- *Batalla Naval Táctica — Propuesta de Trabajo Práctico* (reglas completas y casos borde).
- *Batalla Naval Táctica — Diseño de clases* (firmas detalladas de cada clase).
- *Batalla Naval Táctica — Backlog de tareas* (issues con ID, dependencias y orden).
- UML en el repo: `docs/uml/`.

Pedile a Bota acceso a los documentos si no los tenés.
