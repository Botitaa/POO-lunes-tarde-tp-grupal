# Entrega: jar ejecutable y tags

## Armar el jar (#116)

Desde `tp-batalla-naval/`:

```bash
mvn clean package
```

Corre todos los tests y deja el jar en `target/batalla-naval-tactica-0.1.0.jar` (el `pom.xml` ya pone `batallanaval.app.Main` como clase principal y mete el mapa `mapas/estandar.txt` adentro del jar).

Para jugar:

```bash
java -jar target/batalla-naval-tactica-0.1.0.jar
```

La primera vez crea, en la carpeta desde donde se ejecuta, `datos/config.properties`, `datos/partidas/` y `datos/historiales/`.

## Probarlo en una máquina que no sea de desarrollo

Requisito: **Java 25** (alcanza con el JRE; no hace falta Maven ni IntelliJ). Con una versión más vieja aparece `UnsupportedClassVersionError`.

1. Copiar solo el `.jar` a una carpeta vacía de la otra máquina (pendrive, Drive, etc.).
2. Abrir una terminal en esa carpeta y correr `java -jar batalla-naval-tactica-0.1.0.jar`. En Windows y Mac también se puede probar con doble clic.
3. Chequear:
    - [ ] Abre el menú principal sin errores en la terminal.
    - [ ] Se crea la carpeta `datos/` al lado del jar.
    - [ ] Partida nueva: despliegue de los dos jugadores, un par de turnos, disparar, moverse.
    - [ ] Guardar desde el menú y volver a cargar la partida.
    - [ ] Al terminar un turno aparece `autoguardado` en Cargar partida.
    - [ ] Jugar hasta el final: pantalla de fin, historial exportado en `datos/historiales/`, ranking actualizado en Estadísticas.
    - [ ] Al cerrar y volver a abrir, la pantalla de nueva partida trae los últimos nombres.
    - [ ] Los textos con tildes y ñ se ven bien.
4. Anotar en qué sistema operativo y con qué versión de Java se probó (`java -version`).

## Tags de Git por entrega (#118)

Los tags se ponen en `main`, sobre el commit que se entrega:

```bash
git checkout main
git pull
git merge --no-ff develop -m "Entrega N"   # solo si la entrega sale de develop
git push origin main
git tag -a entrega-N -m "Entrega parcial N"
git push origin entrega-N
```

- Entregas parciales: `entrega-1`, `entrega-2`, …
- Entrega final: `v1.0` (además, adjuntar el jar en un Release de GitHub con `gh release create v1.0 target/batalla-naval-tactica-0.1.0.jar`).
- Ver los tags: `git tag -n`.