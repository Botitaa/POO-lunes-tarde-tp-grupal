# Batalla Naval Táctica — Prueba manual guiada

Recorrido de punta a punta para hacer **antes de cada entrega** (y antes de probar el jar en otra máquina, QA-07). Cubre lo que los tests automáticos no ven: la interfaz, los clics, los mensajes y los archivos reales. Lleva unos 25 minutos.

Cada paso dice qué hacer y qué tiene que pasar. Marcá la casilla si pasó. Si algo no coincide, seguí con el resto y anotalo en el [registro](#registro-de-corridas) del final.

**Coordenadas:** `(fila, columna)`, contando desde 0. La fila 0 es la de arriba y la columna 0 la de la izquierda. El mapa estándar:

```
     0 1 2 3 4 5 6 7 8 9
  0  ~ ~ ~ ~ ~ ~ ~ ~ ~ ~    filas 0 a 2: despliegue del Jugador 1
  1  ~ ~ ~ ~ ~ ~ ~ ~ ~ ~
  2  ~ ~ ~ ~ ~ ~ ~ ~ ~ ~
  3  ~ ~ I I ~ ~ ~ P ~ ~    filas 3 a 6: zona neutral
  4  ~ ~ I ~ ~ ~ P ~ ~ ~    (4,4) (4,5) (5,4) (5,5): zona de dominación (borde dorado)
  5  ~ ~ ~ P ~ ~ ~ I ~ ~
  6  ~ ~ P ~ ~ ~ I I ~ ~
  7  ~ ~ ~ ~ ~ ~ ~ ~ ~ ~    filas 7 a 9: despliegue del Jugador 2
  8  ~ ~ ~ ~ ~ ~ ~ ~ ~ ~
  9  ~ ~ ~ ~ ~ ~ ~ ~ ~ ~
~ agua   I isla (verde)   P piedra (gris)
```

## 0. Preparación

- [ ] Están mergeados en `develop` los PRs de despliegue (#98), guardado en hilo (#109) y pantalla de fin (#106), y estás en `develop` actualizado.
- [ ] `mvn -f tp-batalla-naval/pom.xml test` pasa sin errores.
- [ ] Abrí el juego (en IntelliJ: Run sobre `batallanaval.app.Main`, o `mvn -f tp-batalla-naval/pom.xml compile exec:java`) y cerralo. Al abrirse se crea la carpeta `datos/` en la carpeta desde la que corre el juego (según cómo lo abras, la raíz del repo o `tp-batalla-naval/`).
- [ ] Copiá los archivos de `tp-batalla-naval/docs/prueba-manual/partidas/` a `datos/partidas/`. Son partidas armadas para llegar rápido a cada final ([qué tiene cada una](#partidas-de-prueba)).
- [ ] Si querés que las estadísticas del paso 11 den justo, renombrá `datos/estadisticas.csv` (si existe) antes de empezar y devolvelo al final.

## 1. Nueva partida

- [ ] **Hacé:** Inicio → **Nueva partida**. Dejá vacío el Jugador 1 y tocá **Comenzar**.
      **Tiene que pasar:** la barra de estado dice "El nombre del Jugador 1 no puede estar vacío" y no se pasa de pantalla.
- [ ] **Hacé:** escribí `Ana` y `Ana`, tocá **Comenzar**.
      **Tiene que pasar:** "Los dos jugadores tienen que tener nombres distintos".
- [ ] **Hacé:** escribí `A;na` en el Jugador 1, tocá **Comenzar**.
      **Tiene que pasar:** "El nombre del Jugador 1 no puede tener el carácter ';'".
- [ ] **Hacé:** escribí `Ana` (Jugador 1) y `Beto` (Jugador 2). **No** toques Sortear: los pasos siguientes cuentan con que Ana es la Jugadora 1. Tocá **Comenzar**.
      **Tiene que pasar:** blackout negro con "Turno de Ana" y el aviso que titila.

## 2. Despliegue

- [ ] **Hacé:** **Listo**.
      **Tiene que pasar:** pantalla de despliegue sin niebla, con la lista **Barcos sin ubicar** (1 Acorazado, 2 Cruceros, 3 Destructores) y **Confirmar despliegue** deshabilitado.
- [ ] **Hacé:** mirá el menú **Partida**.
      **Tiene que pasar:** **Guardar** está deshabilitado (no se guarda durante el despliegue).
- [ ] **Hacé:** elegí el Acorazado en la lista y hacé clic en `(5,5)`.
      **Tiene que pasar:** "Solo podés ubicar barcos en tu zona de despliegue." y el barco sigue en la lista.
- [ ] **Hacé:** con el Acorazado todavía elegido, hacé clic, en este orden, en `(2,4)`, `(0,0)`, `(0,9)`, `(2,5)`, `(2,1)`, `(1,8)`. Después de cada clic queda elegido el siguiente barco de la lista.
      **Tiene que pasar:** quedan el Acorazado en `(2,4)`, los Cruceros en `(0,0)` y `(0,9)` y los Destructores en `(2,5)`, `(2,1)` y `(1,8)`. **Confirmar despliegue** se habilita.
- [ ] **Hacé:** clic sobre el Acorazado de `(2,4)` y **Retirar barco**.
      **Tiene que pasar:** vuelve a la lista y **Confirmar despliegue** se deshabilita. Volvé a ubicarlo en `(2,4)`.
- [ ] **Hacé:** **Confirmar despliegue**.
      **Tiene que pasar:** blackout "Turno de Beto".
- [ ] **Hacé:** **Listo**, elegí el Acorazado de Beto en la lista y ubicá sus barcos en este orden: `(7,4)`, `(9,0)`, `(9,9)`, `(8,1)`, `(8,6)`, `(8,8)`. **Confirmar despliegue**.
      **Tiene que pasar:** blackout "Turno de Ana". Al tocar **Listo** se pasa directo al juego (no hay resumen: Beto todavía no hizo nada).

## 3. Disparo fuera de rango (turno de Ana)

- [ ] **Hacé:** mirá el tablero.
      **Tiene que pasar:** el Acorazado enemigo de `(7,4)` se ve (lo ve tu Acorazado, que tiene visión 5). Las filas 8 y 9 están con niebla.
- [ ] **Hacé:** seleccioná el Destructor de `(2,5)`.
      **Tiene que pasar:** el combo de objetivos y el botón **Disparar** están deshabilitados: el enemigo está a distancia 5 y el rango del Destructor es 2. Ningún barco enemigo tiene borde rojo.
- [ ] **Hacé:** seleccioná el Acorazado de `(2,4)`.
      **Tiene que pasar:** el Acorazado enemigo aparece en la lista y con borde rojo (visión compartida: el rango del Acorazado es 6).
- [ ] **Hacé:** **Disparar**.
      **Tiene que pasar:** el enemigo pasa de `A8` a `A4` y hay un destello en su casilla. El Acorazado de Ana ya no puede moverse ni volver a disparar en este turno.

## 4. Movimiento con diagonal bloqueada (turno de Ana)

- [ ] **Hacé:** seleccioná el Destructor de `(2,5)` y hacé clic en `(3,4)` y después en `(4,3)`.
      **Tiene que pasar:** la ruta aparece en celeste, numerada 1 y 2, con costo 2.
- [ ] **Hacé:** clic en `(5,2)`.
      **Tiene que pasar:** "No se puede pasar por esa casilla." y la ruta no cambia. La diagonal está bloqueada porque las dos casillas del costado bloquean: la isla `(4,2)` y la piedra `(5,3)`.
- [ ] **Hacé:** clic en `(5,4)`.
      **Tiene que pasar:** se agrega como paso 3 (costo 3). Esa diagonal sí se puede: solo bloquea una de las dos casillas del costado, la piedra `(5,3)`.
- [ ] **Hacé:** **Backspace**.
      **Tiene que pasar:** se borra el paso 3.
- [ ] **Hacé:** mirá el menú **Partida**.
      **Tiene que pasar:** **Guardar** está deshabilitado: hay un movimiento a medias y ya se disparó en este turno.
- [ ] **Hacé:** **Enter**.
      **Tiene que pasar:** el Destructor queda en `(4,3)` y los puntos de movimiento bajan de 6 a 4. La niebla se recalcula recién ahora.

## 5. Fin de turno, blackout, guardado y resumen

- [ ] **Hacé:** **Terminar turno**.
      **Tiene que pasar:** blackout "Turno de Beto". El tablero de Ana no se ve.
- [ ] **Hacé:** **Guardar partida**, escribí `manual-1` y aceptá.
      **Tiene que pasar:** la barra de estado dice: Partida guardada como "manual-1". Aparece `datos/partidas/manual-1.sav`.
- [ ] **Hacé:** **Guardar partida** de nuevo con el mismo nombre.
      **Tiene que pasar:** pregunta si querés reemplazarla. Con **No** no pasa nada; con **Sí** vuelve a decir que se guardó.
- [ ] **Hacé:** **Listo**.
      **Tiene que pasar:** pantalla de resumen con "Tu A (7,4) recibió 4 de daño desde (2,4)" y el aviso que titila. **Continuar** lleva al juego.
- [ ] **Hacé:** mirá el historial (panel de abajo).
      **Tiene que pasar:** Beto ve el disparo de Ana, pero **no** su despliegue ni su movimiento (son privados).
- [ ] **Hacé:** **Terminar turno** sin hacer nada.
      **Tiene que pasar:** blackout "Turno de Ana".

## 6. Cargar una partida

- [ ] **Hacé:** menú **Partida → Cargar** (`Ctrl+O`).
      **Tiene que pasar:** la lista muestra `manual-1` (Ana, Beto, ronda 1) y las partidas de prueba. **No** aparece `prueba-otra-version`: los guardados de otra versión se saltean.
- [ ] **Hacé:** elegí `manual-1` y **Cargar**.
      **Tiene que pasar:** blackout "Turno de Beto". Con **Listo** vuelve a aparecer el resumen del disparo (se guardó pendiente). En el juego: el Acorazado de Beto está en `A4` y el Destructor de Ana en `(4,3)`.

## 7. Archivos dañados

- [ ] **Hacé:** **Partida → Cargar**, elegí `prueba-rota` y **Cargar**.
      **Tiene que pasar:** la barra de estado dice "Línea 6 del guardado inválida: formato numérico incorrecto" y la partida en curso no cambia.
- [ ] **Hacé:** lo mismo con `prueba-imposible`.
      **Tiene que pasar:** "Error al restaurar partida guardada: Estado invalido: dos barcos vivos en la misma posicion: (2,4)".
- [ ] **Hacé:** con la lista abierta, borrá `manual-1.sav` desde el explorador de archivos. Después elegí `manual-1` en la lista y **Cargar**.
      **Tiene que pasar:** "El archivo de partida no existe: …" con la ruta del archivo.
- [ ] **Hacé:** elegí `prueba-rota` y **Borrar**.
      **Tiene que pasar:** pide confirmación. Con **Sí** desaparece de la lista y de `datos/partidas/`.

## 8. Hundimiento y pantalla de fin

- [ ] **Hacé:** cargá `prueba-hundimiento` y tocá **Listo**.
      **Tiene que pasar:** turno de Ana, ronda 8. A Beto le quedan dos Destructores con 2 de vida, en `(6,4)` y `(7,5)`. Los demás barcos de Beto son restos (X negra).
- [ ] **Hacé:** seleccioná el Acorazado de `(2,4)` y disparale al Destructor de `(6,4)`.
      **Tiene que pasar:** el Destructor se hunde, su casilla pasa a restos y la partida sigue.
- [ ] **Hacé:** seleccioná el Destructor de `(5,5)` y disparale al de `(7,5)`.
      **Tiene que pasar:** pantalla de fin con "¡Ganó Ana!", "Hundió toda la flota rival.", "Rondas jugadas: 8" y la ruta del historial exportado. El título titila en dorado.
- [ ] **Hacé:** **Ver historial**.
      **Tiene que pasar:** se despliega el historial completo, sin niebla, con los dos disparos. El botón pasa a decir **Ocultar historial** y con otro clic se oculta.
- [ ] **Hacé:** **Abrir historial exportado**.
      **Tiene que pasar:** se abre el `.txt` de `datos/historiales/` (nombre `fecha_hora_Ana-vs-Beto.txt`), con un encabezado (jugadores, resultado y rondas) y una línea por evento.
- [ ] **Hacé:** **Volver al inicio**.
      **Tiene que pasar:** menú de inicio. El menú **Partida** tiene Guardar y Abandonar deshabilitados (no hay partida en curso).

## 9. Dominación

- [ ] **Hacé:** cargá `prueba-dominacion` y tocá **Listo**.
      **Tiene que pasar:** turno de Ana, ronda 10. Dominación: Ana 5, Beto 2. Ana tiene un Destructor en `(4,4)` y Beto ningún barco en la zona.
- [ ] **Hacé:** **Terminar turno**.
      **Tiene que pasar:** pantalla de fin con "¡Ganó Ana!", "Llegó a los puntos de dominación necesarios para ganar." y "Rondas jugadas: 10".

## 10. Límite de turnos

- [ ] **Hacé:** cargá `prueba-limite` y tocá **Listo**.
      **Tiene que pasar:** turno de Beto, ronda 40 (la última). Dominación: Ana 3, Beto 1.
- [ ] **Hacé:** **Terminar turno**.
      **Tiene que pasar:** pantalla de fin con "¡Ganó Ana!", el texto "Se llegó al límite de turnos y ganó el desempate…" y "Rondas jugadas: 40".
- [ ] **Hacé:** **Volver al inicio**, cargá `prueba-limite-empate`, tocá **Listo** y **Terminar turno**.
      **Tiene que pasar:** "Empate", "Se llegó al límite de turnos con todo igualado." y "Rondas jugadas: 40".

## 11. Estadísticas

- [ ] **Hacé:** Inicio → **Estadísticas**.
      **Tiene que pasar:** si arrancaste con el archivo de estadísticas vacío: Ana con 4 jugadas, 3 ganadas, 0 perdidas y 1 empatada (75,0 %), y Beto con 4 jugadas, 0 ganadas, 3 perdidas y 1 empatada. Si ya había partidas, cada línea sumó lo mismo.
- [ ] **Hacé:** **Salir**.
      **Tiene que pasar:** el programa termina del todo (no queda nada corriendo en IntelliJ).

## Partidas de prueba

Están en `docs/prueba-manual/partidas/`. Todas usan el mapa estándar y los jugadores Ana (J1) y Beto (J2), con la flota desplegada como en el paso 2. Al cargarlas, el archivo queda en `datos/partidas/`, así que se pueden usar todas las veces que haga falta.

| Archivo | Turno | Qué tiene | Paso |
| --- | --- | --- | --- |
| `prueba-hundimiento.sav` | Ana, ronda 8 | A Beto le quedan dos Destructores con 2 de vida en `(6,4)` y `(7,5)`; el Destructor de Ana está en `(5,5)` | 8 |
| `prueba-dominacion.sav` | Ana, ronda 10 | Ana con 5 de dominación y un Destructor en `(4,4)` | 9 |
| `prueba-limite.sav` | Beto, ronda 40 | Dominación 3 a 1 a favor de Ana | 10 |
| `prueba-limite-empate.sav` | Beto, ronda 40 | Todo igualado: dominación 2 a 2 y las dos flotas enteras | 10 |
| `prueba-rota.sav` | — | La vida del primer barco (línea 6) no es un número | 7 |
| `prueba-imposible.sav` | — | Dos barcos de Ana en `(2,4)` | 7 |
| `prueba-otra-version.sav` | — | Primera línea `BATALLA-NAVAL;2` | 6 |

`PartidasDePruebaManualTest` las carga y verifica que cada una termine como dice esta tabla. Si cambia el formato del guardado o una regla y ese test falla, hay que regenerarlas. Lo más fácil es guardar una partida con el juego y editar el `.sav` a mano siguiendo el formato de `DISENO_CLASES.md`.

## Registro de corridas

Si un paso falla, abrí un issue con el número de paso, qué pasó, una captura y el commit probado (`git log -1 --oneline`), y anotalo acá.

| Fecha | Quién | Commit | Resultado | Issues abiertos |
| --- | --- | --- | --- | --- |
| | | | | |
