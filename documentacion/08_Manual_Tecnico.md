# 08 · Manual técnico del desarrollador

Versión 1.0 · noviembre de 2026 · Software 2.0.0

Para quien va a compilar, mantener o extender Amoblador3D. La arquitectura está en el documento
[04](04_Documento_de_Arquitectura.md) y la base de datos en el [05](05_Diseno_de_Base_de_Datos.md).

## 1. Entorno de desarrollo

| Herramienta | Versión | Nota |
|---|---|---|
| JDK | 21 o superior | Temurin recomendado. El build falla con un mensaje claro si es menor (maven-enforcer) |
| Maven | No hace falta | El proyecto trae Maven Wrapper (`mvnw` / `mvnw.cmd`) |
| IDE | IntelliJ IDEA, VS Code o NetBeans | Abrir la carpeta como proyecto Maven |
| Git | 2.x | Repositorio: `https://github.com/alex123floresc-sketch/amoblador3d` |

## 2. Comandos

```powershell
.\mvnw.cmd javafx:run     # ejecutar la aplicación
.\mvnw.cmd test           # pruebas
.\mvnw.cmd verify         # pruebas + cobertura (target\site\jacoco\index.html); falla si el dominio baja del 70 %
.\mvnw.cmd package        # target\amoblador3d.jar con todas las dependencias
.\packaging\crear-instalador.ps1   # versión para Windows con Java incluido
```

Propiedades útiles de la JVM:

| Propiedad | Para qué |
|---|---|
| `-Damoblador.home=C:\temp\prueba` | Usar otra carpeta de datos (pruebas, USB) |
| `--enable-native-access=ALL-UNNAMED` | Evita avisos de SQLite en Java 22+ (ya configurado en `pom.xml`, `javafx:run` y el manifiesto del `.jar`) |

## 3. Estructura del repositorio

```
amoblador3d/
├── .github/workflows/ci.yml     integración continua (GitHub Actions, Windows)
├── database/                    scripts SQL: SQLite (el de la app) y PostgreSQL
├── documentacion/               esta documentación
├── packaging/                   instalador para Windows (jpackage)
├── src/main/java/pe/edu/unaj/amoblador3d/
│   ├── Launcher, App            arranque
│   ├── model/                   entidades (sin JavaFX)
│   ├── design/                  reglas, historial, imán, plantillas, formularios
│   ├── estimate/                metrado y presupuesto
│   ├── catalog/                 catálogo de muebles
│   ├── persistence/             SQLite, JSON, recuperación, rutas
│   ├── report/                  informe PDF
│   ├── gltf/                    lector glTF 2.0
│   ├── scene/                   vista 3D (JavaFX)
│   ├── ui/                      ventana, paneles y diálogos (JavaFX)
│   └── util/                    unidades, Outcome, registro de errores
├── src/main/resources/          catalog.json, modelos .glb, CSS, ícono
├── src/test/java/               114 pruebas JUnit 5
├── pom.xml                      dependencias y plugins
└── Ejecutar-Amoblador3D.cmd     abrir con doble clic
```

## 4. Convenciones de código

- **Idioma:** comentarios, mensajes y nombres de pruebas en español; nombres de clases y métodos en inglés.
- **Unidades:** todo en **metros** dentro del código; la interfaz muestra cm cuando conviene (`Units`).
- **Coordenadas:** origen en la esquina interior **noroeste** a nivel del piso; X al este, Z al sur, Y arriba.
- **Reglas fuera de la UI:** toda validación va en `design/Design.java` (o en `model`), nunca en
  `ui/`. La interfaz solo llama y muestra el resultado (`Outcome` o mensaje).
- **Mensajes al usuario:** explican el motivo en lenguaje sencillo ("No cabe porque…").
- **Cada cambio pasa por `Design`:** así se registra en el historial (deshacer) y se marca "sin guardar".
- **SQL:** siempre `PreparedStatement` con parámetros; nunca concatenar datos del usuario.
- **Pruebas:** nombres que se leen como una frase (`unaPuertaNoPuedeQuedarBloqueadaPorUnMueble`).

## 5. Cómo extender

### 5.1 Agregar un mueble paramétrico

1. Crea su método en `scene/ParametricBuilder.java` y regístralo en `build`.
2. Declara sus medidas por defecto y su categoría en `catalog/Catalog.java`.
3. Agrega su precio referencial en `estimate/PriceList.java`.
4. Ejecuta las pruebas: `ParametricBuilderTest` verifica que ocupe exactamente
   `x ∈ [-ancho/2, ancho/2]`, `z ∈ [-prof/2, prof/2]`, `y ∈ [0, alto]`, con el frente hacia +Z;
   `CatalogTest` verifica que tenga categoría y precio.

### 5.2 Incluir un modelo 3D en la aplicación

1. Copia el `.glb` en `src/main/resources/assets/models/`.
2. Agrega una entrada en `src/main/resources/assets/catalog.json`:
   ```json
   { "id": "mi-sillon", "name": "Sillón", "file": "models/mi-sillon.glb",
     "width": 0.9, "depth": 0.85, "height": 0.95, "rotation": 0, "category": "Sala" }
   ```
3. Recompila.

### 5.3 Agregar un material de superficie

1. Agrega el valor al enum `model/SurfaceMaterial.java` (id, nombre, color sugerido y dónde se usa).
2. Dibuja su textura en `scene/SurfaceTextures.java`.
3. Define cómo se compra en `estimate/PurchaseSpec.java` y su precio en `PriceList.java`.
4. Si el script PostgreSQL tiene un CHECK de materiales, actualízalo.

### 5.4 Cambiar la base de datos

Ver [05 · sección 5](05_Diseno_de_Base_de_Datos.md#5-historial-de-migraciones): agregar un paso en
`STEPS`, subir `SCHEMA_VERSION`, actualizar los scripts de `database/` y la prueba del script.

### 5.5 Agregar un campo al panel de propiedades

Agrega un `Field` en `design/Editors.java` para el tipo de entidad, con los mismos límites que usa
`Design`. `PropertiesPanel` dibuja el formulario solo.

## 6. Registro de errores

Desde la versión 2.0.0 la aplicación registra lo que pasa con `java.util.logging` (clase
`util/AppLog.java`):

| Qué | Dónde |
|---|---|
| Archivo | `%APPDATA%\Amoblador3D\logs\amoblador3d-0.log` (5 archivos de 1 MB que rotan) |
| Contenido | Inicio (versión, Java, sistema), avisos de carga, errores mostrados al usuario y errores no controlados con su traza |
| Acceso del usuario | *Ayuda → Abrir registro de errores* |

Uso en el código:

```java
private static final Logger LOG = AppLog.of(MiClase.class);
LOG.warning("No se pudo leer el modelo " + archivo);
LOG.log(Level.SEVERE, "Falló el guardado", e);
```

Todo `Dialogs.error(...)` queda registrado automáticamente, y cualquier excepción no atrapada en
cualquier hilo se registra y muestra un aviso "Algo salió mal".

## 7. Integración continua

`.github/workflows/ci.yml` se ejecuta en cada *push* a `main` o a ramas `fase-*` y en cada *pull
request*: instala Temurin 21 en Windows, ejecuta `mvnw verify` y publica como artefactos los
resultados de las pruebas, el informe de cobertura y el `amoblador3d.jar`.

## 8. Flujo de trabajo con Git

- Rama `main`: versiones entregadas.
- Ramas `fase-N-descripcion`: cada fase de desarrollo; se integra con *pull request* cuando las
  pruebas pasan.
- Mensajes de commit en español, en imperativo y describiendo el resultado
  ("Fase 7: casa con varios ambientes…").

## 9. Dependencias

| Librería | Versión | Licencia | Uso |
|---|---|---|---|
| OpenJFX (javafx-controls) | 21.0.4 | GPL v2 + Classpath Exception | Interfaz y 3D |
| sqlite-jdbc (Xerial) | 3.53.4.0 | Apache 2.0 | Base de datos |
| Gson | 2.10.1 | Apache 2.0 | JSON (`.amob3d`, catálogo, glTF) |
| Apache PDFBox | 3.0.5 | Apache 2.0 | Informe PDF |
| JUnit Jupiter | 5.13.4 | EPL 2.0 | Pruebas |
| JaCoCo | 0.8.14 | EPL 2.0 | Cobertura (solo al construir) |

## 10. Problemas conocidos para el desarrollador

| Síntoma | Causa y solución |
|---|---|
| `java -jar` dice que faltan componentes de JavaFX | Se ejecutó `App` en vez de `Launcher`. Usa siempre `Launcher` como clase principal |
| Avisos `WARNING: A restricted method… System::load` | Java 22+ sin `--enable-native-access`. Ya está configurado; si ejecutas desde el IDE, agrégalo a las opciones de la VM |
| Avisos `LF will be replaced by CRLF` | `.gitattributes` normaliza los finales de línea; son inofensivos |
| El `.jar` no abre en otro sistema operativo | JavaFX trae partes nativas por sistema; compila en el mismo sistema donde se usará |
