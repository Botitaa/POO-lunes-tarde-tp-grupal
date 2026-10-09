# Batalla Naval Táctica

Juego por turnos para dos jugadores en la misma computadora, hecho en Java + Swing para **Paradigma Orientado a Objetos (3.4.208), UADE, 2º cuatrimestre 2026**. Mezcla la batalla naval clásica con mecánicas de World of Warships: los barcos se mueven, tienen rango y visión, y solo se ataca lo que la flota ve.

- Reglas completas, arquitectura y convenciones: [`docs/CONTEXTO_PROYECTO.md`](docs/CONTEXTO_PROYECTO.md)
- Qué hace cada clase y sus métodos: [`docs/DISENO_CLASES.md`](docs/DISENO_CLASES.md)
- Tareas: [issues](https://github.com/Botitaa/POO-lunes-tarde-tp-grupal/issues) y [tablero kanban](https://github.com/users/Botitaa/projects/3)

## Cómo correrlo

Requisitos: el JDK en la versión que dice `<java.version>` en `pom.xml` (la misma para todo el equipo) y Maven (`brew install maven` en Mac).

Todos los comandos se corren dentro de esta carpeta (`tp-batalla-naval/`):

| Comando | Qué hace |
| --- | --- |
| `mvn test` | Compila y corre los tests de JUnit 5 |
| `mvn compile exec:java` | Compila y abre el juego |
| `mvn package` | Arma el jar ejecutable en `target/` |
| `java -jar target/batalla-naval-tactica-0.1.0.jar` | Corre el jar |

En IntelliJ: clic derecho en `pom.xml` → **Add as Maven Project**. Después se corre `batallanaval.app.Main` con el botón verde.

## Estructura

```
src/main/java/batallanaval/
  modelo/        Posicion, Casilla, Tablero, Barco y subclases, Jugador
  controlador/   contrato de la partida y GestorPartida (las reglas)
  excepciones/   excepciones propias
  historial/     historial de la partida
  persistencia/  guardar/cargar, estadísticas, configuración, mapas
  vista/         Swing: ventana, pantallas y tablero
  app/           Main y armado de la aplicación
src/main/resources/mapas/   mapas en texto
src/test/java/              tests JUnit
```

Cada paquete tiene un `package-info.java` que explica qué va adentro y de quién puede depender. La vista solo habla con la interfaz `Partida`; nunca recibe `Barco`, `Jugador` ni `Tablero`.

## Cómo trabajamos

### Ramas

- `main`: solo versiones entregadas, cada una con su tag (`entrega-1`, `entrega-2`, …, `v1.0`).
- `develop`: rama de integración y rama por defecto. Todo entra acá por PR.
- `tarea/<numero-issue>-<descripcion>`: una rama corta por issue, sale de `develop` y vive pocos días.

Nadie pushea directo a `main` ni a `develop`.

### Hacer una tarea, paso a paso

```bash
git checkout develop
git pull
gh issue develop 24 --base develop --name tarea/24-tab-03-tablero --checkout   # el comando exacto está al pie de cada issue
# ...programar, con tests...
mvn test
git add tp-batalla-naval
git commit -m "#24 Agrega Tablero con grilla y estaDentro"
git push -u origin tarea/24-tab-03-tablero
gh pr create --base develop --title "[TAB-03] Tablero" --body "Closes #24"
```

- En el tablero, mové tu tarjeta a **In Progress** cuando arrancás. Cuando se mergea el PR, el `Closes #N` cierra el issue y la tarjeta pasa sola a **Done**.
- Si la tarea dura más de un par de días, traé lo nuevo con `git pull origin develop` dentro de tu rama.
- Varios issues chicos y seguidos del mismo módulo pueden ir en una rama: `Closes #28, closes #29`.

### Commits

En español, en presente, con el número de issue adelante y uno por cambio lógico:

```
#24 Agrega Tablero con grilla y estaDentro
#24 Valida que las zonas de despliegue no tengan obstáculos
```

### Pull requests

- Siempre contra `develop`, completando la plantilla.
- Lo revisa y aprueba otro integrante; nadie aprueba su propio PR.
- El autor resuelve sus conflictos.
- Se mergea con **Squash and merge** y se borra la rama.

### Definición de terminado

Una tarea está terminada cuando:

- [ ] compila sin warnings (`mvn test` en verde),
- [ ] tiene tests si toca `modelo`, `controlador` o `persistencia`,
- [ ] respeta `docs/DISENO_CLASES.md` (si cambia una firma pública, se actualiza el UML en el mismo PR),
- [ ] tiene Javadoc en los métodos públicos,
- [ ] otro integrante la revisó y la puede explicar (la defensa es individual).

### Convenciones de código

- Identificadores en español, sin tildes ni ñ (`danio`, no `daño`). Clases en `PascalCase`, métodos y atributos en `camelCase`, constantes en `MAYUSCULAS`.
- Atributos siempre `private`; lo que no cambia, `final`. Las colecciones salen con `List.copyOf(...)`.
- Nada de `System.out`: los errores y avisos se muestran en la GUI (barra de estado o diálogo).
- Reglas violadas → excepción del paquete `excepciones`, con un mensaje que entienda el jugador.

## Equipo

| Integrante | GitHub | Rol |
| --- | --- | --- |
| Agustín "Bota" | [@Botitaa](https://github.com/Botitaa) | Partida y reglas, Git |
| Marcos | [@enevoldsenmar](https://github.com/enevoldsenmar) | Tablero y mapa, persistencia base |
| Santi | [@Faccio7L](https://github.com/Faccio7L) | Barcos y combate, persistencia, UML |
| Bruno | [@Bruno-Dominguez](https://github.com/Bruno-Dominguez) | Vista del tablero (Swing), historial |
| Guille | [@guillermosap111](https://github.com/guillermosap111) | Pantallas y flujo (Swing) |
