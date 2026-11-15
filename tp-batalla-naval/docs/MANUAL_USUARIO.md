# Batalla Naval Táctica — Manual de usuario

Juego por turnos para **dos jugadores en la misma computadora**. Cada uno comanda una flota de 6 barcos sobre un mapa con niebla de guerra: solo podés atacar lo que tu flota ve. Gana quien hunde toda la flota rival o domina el centro del mapa.

> Las reglas completas también están dentro del juego: **Inicio → Reglas** o **Ayuda → Reglas (F1)**.

## 1. Empezar una partida

1. En el menú de inicio tocá **Nueva partida**.
2. Escribí el nombre de cada jugador. Tienen que ser **distintos**, no pueden estar vacíos ni tener el carácter `;`. Con **Sortear quién es J1** se elige al azar quién juega primero.
3. Tocá **Comenzar** (o apretá Enter en un campo de nombre).
4. **Despliegue:** antes de cada despliegue aparece el blackout ("Turno de X") y el jugador toca **Listo**. Cada jugador ubica sus 6 barcos (1 Acorazado, 2 Cruceros, 3 Destructores, uno por casilla):
   - Elegí un barco de la lista **Barcos sin ubicar** y hacé clic en una casilla de tu zona. El Jugador 1 despliega en las filas 0 a 2 (las 3 de arriba) y el Jugador 2 en las filas 7 a 9 (las 3 de abajo). Después de ubicar uno, queda elegido el siguiente.
   - Para cambiar de lugar un barco ya ubicado, hacé clic sobre él y después en otra casilla. **Retirar barco** lo devuelve a la lista.
   - Cuando están los 6 en el mapa se habilita **Confirmar despliegue**. Al confirmar, la computadora pasa al otro jugador.

   No se puede guardar durante el despliegue.

Otras opciones del menú de inicio: **Cargar partida**, **Estadísticas** (ranking de jugadores), **Reglas** y **Salir**.

## 2. Reglas en resumen

**Mapa.** Tablero de 10x10. Las filas 3 a 6 son zona neutral (ahí están los obstáculos). Las 4 casillas centrales, marcadas con borde dorado, son la **zona de dominación**.

| Casilla | Se puede pasar | Bloquea la visión |
| --- | --- | --- |
| Agua | Sí | No |
| Piedra | No | No |
| Isla | No | Sí |
| Restos (barco hundido) | Sí | No |

Los barcos bloquean el paso, pero no la visión ni los disparos. Las distancias se cuentan en casillas y una diagonal cuenta como un solo paso.

**Barcos.**

| | Destructor | Crucero | Acorazado |
| --- | --- | --- | --- |
| Vida | 3 | 5 | 8 |
| Daño | 3 | 2 | 4 |
| Rango de disparo | 2 | 4 | 6 |
| Visión | 2 | 3 | 5 |
| Máx. casillas por turno | 4 | 3 | 2 |
| Costo por casilla | 1 | 2 | 3 |
| Espera tras disparar | 0 | 0 | 1 turno |
| Habilidad | Daño doble (6) | Se repara 2 de vida | Rango 8 |

**Habilidades.** Se activan con el botón **Usar habilidad**, una vez por turno. No cuestan puntos, no impiden moverse y duran hasta el fin del turno. Después hay que esperar 3 turnos para volver a usarlas. Activala **antes** de disparar si querés que afecte al disparo.

**Niebla.** Ves una casilla si alguno de tus barcos vivos la tiene dentro de su visión y con línea de visión libre. La visión se comparte entre tus barcos. Una isla corta la visión y los disparos. El barco que dispara queda visible para el rival durante todo el turno siguiente del rival. Los barcos en la zona de dominación los ven los dos jugadores.

**Cómo se gana.**

- **Hundimiento:** hundir los 6 barcos rivales. La partida termina en el acto.
- **Dominación:** al terminar tu turno sumás 1 punto si tenés al menos un barco en la zona de dominación y el rival ninguno. Con 6 puntos ganás y los puntos no se pierden.
- **Límite:** 40 turnos por jugador. Gana quien tenga más puntos de dominación; si hay empate, más barcos vivos; si sigue el empate, más vida total.

## 3. Cómo se juega un turno

Cada turno tenés **6 puntos de movimiento** para toda la flota (no se acumulan). Podés activar tus barcos de a uno, en el orden que quieras: **primero mover y después disparar** (las dos cosas son opcionales). Un barco que disparó ya no puede moverse.

**Mover un barco**

1. Hacé clic en uno de tus barcos para seleccionarlo. Se resalta y se sombrean las casillas a las que puede llegar.
2. Hacé clic en las casillas por las que querés pasar, paso a paso (una de las 8 casillas vecinas por vez). La ruta aparece numerada con su costo en puntos.
3. Confirmá con **Enter** o con el botón **Confirmar movimiento**.

Mientras planificás la ruta no se mueve nada ni cambia la niebla. Si al confirmar aparece un enemigo oculto en el camino, el barco frena en el paso anterior, solo se cobran los pasos hechos y el enemigo queda a la vista hasta que termine el turno.

**Disparar**

1. Seleccioná el barco que va a disparar.
2. Elegí el objetivo en la lista del panel lateral. La lista solo muestra los enemigos que podés atacar: que veas, dentro del rango y con línea de visión.
3. Tocá **Disparar**. Siempre acierta. Si la vida del blanco llega a 0, se hunde y su casilla pasa a ser restos.

**Terminar el turno.** Tocá **Terminar turno**. La pantalla se oscurece (blackout) con el cartel "Turno de X" para que el rival no vea tu tablero. El otro jugador toca **Listo**, ve un resumen de los disparos recibidos y de los barcos propios hundidos, y toca **Continuar**.

## 4. Controles

| Acción | Cómo |
| --- | --- |
| Seleccionar un barco / armar la ruta | Clic sobre el tablero |
| Ver los atributos de un barco | Dejar el mouse quieto sobre el barco (aparece un cuadro) |
| Confirmar el movimiento | `Enter` o botón **Confirmar movimiento** |
| Deshacer el último paso de la ruta | `Backspace` o botón **Deshacer paso** |
| Cancelar la selección | `Esc` |
| Activar la habilidad del barco | Botón **Usar habilidad** |
| Disparar | Elegir objetivo en la lista y botón **Disparar** |
| Terminar el turno | Botón **Terminar turno** |
| Guardar la partida | `Ctrl+S` o menú **Partida → Guardar** |
| Cargar una partida | `Ctrl+O` o menú **Partida → Cargar** |
| Ver las reglas | `F1` o menú **Ayuda → Reglas** |
| Abandonar la partida | Menú **Partida → Abandonar** (pide confirmación) |
| Mostrar u ocultar el historial | Botón **Ocultar** / **Mostrar** en el panel de abajo |

Los botones del panel lateral se habilitan solos según lo que se puede hacer en ese momento.

**Qué significa cada color**

| Color | Significa |
| --- | --- |
| Azul | Agua |
| Verde | Isla |
| Gris | Piedra |
| X negra | Restos de un barco hundido |
| Azul oscuro apagado | Niebla: casilla que no ves |
| Borde dorado | Zona de dominación |
| Naranja / violeta | Barcos del Jugador 1 / del Jugador 2 |
| Borde amarillo | Barco seleccionado |
| Celeste con números | Ruta planificada, con su costo |

Cada barco se dibuja con su letra y su vida actual (por ejemplo `C5` es un Crucero con 5 de vida): `D` Destructor, `C` Crucero, `A` Acorazado. Debajo tiene una barra de vida.

## 5. Guardar y cargar

**Guardar**

1. Elegí **Partida → Guardar** (`Ctrl+S`), o tocá **Guardar partida** en la pantalla de blackout.
2. Escribí un nombre: letras, números, `-` y `_`, hasta 40 caracteres. Se propone `partida1`.
3. Si ya existe un guardado con ese nombre, el juego pregunta si querés reemplazarlo.

Solo se puede guardar **al inicio de un turno o en el blackout**, con la partida en juego y sin una ruta a medio planificar. Si no se puede, la barra de estado te lo avisa. No se guarda durante el despliegue.

**Cargar**

1. Elegí **Cargar partida** en el menú de inicio, o **Partida → Cargar** (`Ctrl+O`).
2. Se muestra la lista de partidas guardadas con los jugadores, la ronda y la fecha, de la más nueva a la más vieja.
3. Elegí una y tocá **Cargar** (o hacé doble clic). Con **Borrar** la eliminás, previa confirmación.

Las partidas se guardan como archivos `.sav` en la carpeta `datos/partidas/`, dentro de la carpeta desde la que se abre el juego. Si un archivo está dañado o es de otra versión del juego, el motivo aparece en la barra de estado y la partida en curso no se toca.

## 6. Cuando algo sale mal

Los avisos y errores aparecen en la **barra de estado**, abajo de la ventana, y el juego sigue funcionando.

| Mensaje | Qué hacer |
| --- | --- |
| "Los dos jugadores tienen que tener nombres distintos" | Cambiá uno de los nombres |
| "Solo podés ubicar barcos en tu zona de despliegue" | Hacé clic dentro de las 3 filas que te corresponden |
| "La partida solo se puede guardar al inicio de un turno o en la pantalla de blackout" | Terminá el turno y guardá en el blackout, o cancelá la ruta y guardá |
| "Nombre de partida invalido" | Usá solo letras, números, `-` y `_` |
| Un mensaje sobre un archivo (no existe, formato inválido, error al escribir) | Elegí otra partida o revisá que la carpeta `datos/` se pueda escribir |
