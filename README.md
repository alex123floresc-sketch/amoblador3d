# Amoblador3D

Diseñador de interiores 3D con medidas reales, hecho en **Java 21 + JavaFX + SQLite**.

Defines las medidas de la habitación, colocas puertas, ventanas, luces y muebles, eliges el
material y el color de cada pared y del piso, y ves el resultado en 3D o en planta.

## Cómo ejecutarlo

Solo necesitas Java 21 o superior. Maven no hace falta instalarlo: el proyecto trae el
**Maven Wrapper** (`mvnw`), que lo descarga la primera vez.

```powershell
.\mvnw.cmd javafx:run          # abrir la aplicación (Windows)
.\mvnw.cmd test                # ejecutar las pruebas
.\mvnw.cmd package             # crear target\amoblador3d.jar (incluye todo)
java -jar target\amoblador3d.jar
```

En Linux/macOS usa `./mvnw` en lugar de `.\mvnw.cmd`.

## Funciones

- **Habitación**: ancho, largo, alto y grosor de muro; ventanas y puertas que no se superponen.
- **Paredes y piso**: 11 materiales (pintura, ladrillo, piedra, madera, cerámico, mármol,
  porcelanato, alfombra...) con texturas a escala real y cualquier color.
- **Muebles**: paramétricos (cama, mesa, silla, closet, escritorio) con medidas y color editables,
  y modelos 3D `.glb/.gltf` (incluidos o importados). No chocan entre sí, con las paredes ni con
  el paso de las puertas.
- **Luces**: bombillas, focos y sol que entra por una ventana.
- **Proyectos**: se guardan en SQLite con miniatura; también se exportan/importan como `.amob3d`
  (JSON) y la vista se exporta como PNG.
- Deshacer/rehacer, vista 3D y planta 2D, cotas.

### Atajos

| Tecla | Acción |
|---|---|
| Ctrl+N / Ctrl+O / Ctrl+S | Nuevo / Abrir / Guardar |
| Ctrl+Z / Ctrl+Y | Deshacer / Rehacer |
| Flechas (Shift: más fino) | Mover el mueble 5 cm (1 cm) |
| R / Shift+R | Girar el mueble 90° |
| Supr / Ctrl+D | Eliminar / Duplicar |
| Ctrl+1 / Ctrl+2 | Vista 3D / Planta 2D |
| F1 | Todos los atajos |

Ratón en 3D: botón izquierdo gira, derecho mueve, rueda acerca. Arrastra un mueble para moverlo.

## Datos

Los proyectos y los modelos importados se guardan en `%APPDATA%\Amoblador3D`
(se puede cambiar con `-Damoblador.home=C:\otra\carpeta`).

## Estructura del código

```
src/main/java/pe/edu/unaj/amoblador3d/
  Launcher, App   arranque
  model/          datos del proyecto (sin JavaFX)
  design/         reglas: colisiones, aberturas, luces, deshacer; campos del panel de propiedades
  persistence/    SQLite (con migraciones) y formato .amob3d
  catalog/        catálogo de muebles e importación de modelos
  gltf/           lector de modelos glTF 2.0
  scene/          vista 3D: habitación, texturas, luces, muebles, cotas, cámara
  ui/             ventana principal, paneles y diálogos
src/test/java/    pruebas de reglas, persistencia e historial
```
