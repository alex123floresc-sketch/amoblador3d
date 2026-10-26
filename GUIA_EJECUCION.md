# Guía para ejecutar Amoblador3D

Paso a paso para abrir la aplicación en una computadora con Windows (en Linux o macOS es igual,
cambiando `.\mvnw.cmd` por `./mvnw`).

## 1. Instalar Java 21 o superior

La aplicación necesita el **JDK** (no basta el JRE) en su versión 21 o más nueva.

1. Descárgalo de <https://adoptium.net> (Temurin 21, *Windows x64 .msi*).
2. Durante la instalación marca **"Set JAVA_HOME variable"** y **"Add to PATH"**.
3. Abre una terminal nueva (PowerShell) y comprueba:

   ```powershell
   java -version
   ```

   Debe decir `21` o un número mayor. Si dice "no se reconoce", cierra y vuelve a abrir la
   terminal; si sigue igual, revisa el PATH (ver *Problemas frecuentes*).

**No hace falta instalar Maven**: el proyecto trae el *Maven Wrapper* (`mvnw.cmd`), que descarga
Maven y las librerías (JavaFX, SQLite, Gson) automáticamente la primera vez.

## 2. Descargar el proyecto

Con Git:

```powershell
git clone https://github.com/alex123floresc-sketch/amoblador3d.git
cd amoblador3d
```

O sin Git: en GitHub, botón **Code → Download ZIP**, descomprímelo y abre una terminal dentro de
la carpeta `amoblador3d` (en el Explorador: clic derecho en la carpeta → *Abrir en Terminal*).

## 3. Ejecutar la aplicación

```powershell
.\mvnw.cmd javafx:run
```

La primera vez tarda unos minutos porque descarga todo (necesita internet); las siguientes abre en
segundos. Al arrancar se crea sola la base de datos SQLite, no hay que configurar nada.

## 4. Crear el ejecutable `.jar` (opcional)

Para tener un solo archivo que se abre sin la terminal de desarrollo:

```powershell
.\mvnw.cmd package
java -jar target\amoblador3d.jar
```

El `.jar` incluye todas las librerías. Se puede copiar a otra computadora con Java 21 y el mismo
sistema operativo (JavaFX trae partes propias de cada sistema) y abrirse con doble clic.

## 5. Ejecutar las pruebas (opcional)

```powershell
.\mvnw.cmd test
```

Debe terminar con `Tests run: 25, Failures: 0, Errors: 0` y `BUILD SUCCESS`.

## 6. Abrirlo desde un IDE (opcional)

- **IntelliJ IDEA**: *File → Open* y elige la carpeta (la reconoce como proyecto Maven). Ejecuta
  la clase `pe.edu.unaj.amoblador3d.Launcher`, o en el panel *Maven*: `Plugins → javafx → javafx:run`.
- **VS Code**: instala el *Extension Pack for Java*, abre la carpeta y ejecuta `Launcher.java`
  (botón *Run* sobre el método `main`).
- **NetBeans**: *File → Open Project*, elige la carpeta y pulsa *Run*.

Se ejecuta `Launcher` y no `App` porque JavaFX no arranca desde un `.jar` si la clase principal
hereda de `Application`.

## Dónde se guardan los datos

| Qué | Dónde |
|---|---|
| Base de datos (proyectos guardados) | `%APPDATA%\Amoblador3D\amoblador3d.db` |
| Modelos 3D importados | `%APPDATA%\Amoblador3D\modelos\` |

Para usar otra carpeta (por ejemplo, una USB):

```powershell
java -Damoblador.home=D:\MisDatos -jar target\amoblador3d.jar
```

## Bases de datos

La aplicación usa **SQLite** (un archivo, sin servidor). En la carpeta `database/` están los
scripts de la base de datos:

| Archivo | Para qué |
|---|---|
| `amoblador3d_sqlite.sql` | El esquema exacto que usa la aplicación, con un proyecto de ejemplo. |
| `amoblador3d_postgresql.sql` | La misma base escrita en PostgreSQL, con reglas extra y vistas de consulta. |

### Ver la base SQLite

Instala [DB Browser for SQLite](https://sqlitebrowser.org) y abre
`%APPDATA%\Amoblador3D\amoblador3d.db` (mejor con la aplicación cerrada). Para crear una base
nueva desde el script: *Archivo → Importar → Base de datos desde archivo SQL* y elige
`database\amoblador3d_sqlite.sql`. Si copias ese `.db` como
`%APPDATA%\Amoblador3D\amoblador3d.db`, la aplicación mostrará el *Dormitorio de ejemplo* en
*Archivo → Abrir proyecto*.

### Cargar la base en PostgreSQL

Necesitas PostgreSQL 13 o superior (<https://www.postgresql.org/download/windows/>). El script crea
la base `amoblador3d` si no existe y dentro el esquema `amoblador3d` con todas las tablas. Se
puede volver a ejecutar: borra el esquema y lo crea de nuevo.

**Con psql** (en Windows suele no estar en el PATH; usa la ruta completa y cambia `17` por tu
versión):

```powershell
& "C:\Program Files\PostgreSQL\17\bin\psql.exe" -U postgres -f database\amoblador3d_postgresql.sql
```

Te pedirá la contraseña del usuario `postgres` que elegiste al instalar PostgreSQL.

**Con pgAdmin**: conéctate al servidor, crea la base `amoblador3d` (clic derecho en
*Databases → Create → Database*), abre en ella el *Query Tool*, carga el archivo y **borra o
comenta** las líneas que empiezan con `\` (`\set`, `\gexec`, `\connect`) y la consulta
`SELECT 'CREATE DATABASE ...'`, porque son comandos de psql que pgAdmin no entiende. Luego pulsa
*Execute* (F5).

Consultas para probar:

```sql
SET search_path TO amoblador3d;
SELECT * FROM project_summary;     -- proyectos con área y cantidad de muebles, luces, puertas...
SELECT * FROM project_occupancy;   -- % del piso ocupado por muebles
SELECT * FROM project_finishes;    -- material y color de cada pared y del piso
```

La base de PostgreSQL es para consultas, informes y para documentar el modelo de datos: la
aplicación sigue guardando sus proyectos en SQLite.

## Problemas frecuentes

| Mensaje / síntoma | Solución |
|---|---|
| `"java" no se reconoce como un comando` | Java no está en el PATH. Reinstala marcando *Add to PATH*, o agrega `C:\Program Files\Eclipse Adoptium\jdk-21...\bin` a la variable PATH, y abre una terminal nueva. |
| `JAVA_HOME not found` / `JAVA_HOME is set to an invalid directory` | Crea la variable de entorno `JAVA_HOME` con la carpeta del JDK (la que contiene `bin`, sin `\bin` al final). |
| `release version 21 not supported` | Tienes un Java más antiguo. Instala el 21 y revisa con `java -version`. |
| La primera ejecución falla al descargar | Revisa tu conexión a internet (o el proxy de la red de la universidad) y vuelve a ejecutar. |
| `.\mvnw.cmd : no se puede cargar ... la ejecución de scripts está deshabilitada` | Estás en la carpeta equivocada o PowerShell lo bloquea: ejecuta `cmd /c mvnw.cmd javafx:run`. |
| Aparece "No se pudo abrir la base de datos" | Otra copia de la aplicación está abierta, o el archivo `.db` está abierto en DB Browser con cambios sin guardar. Cierra la otra y vuelve a abrir. |
| La vista 3D se ve negra o muy lenta | Actualiza el controlador de la tarjeta de video. |
| `psql: error: ... password authentication failed` | La contraseña de `postgres` no es la correcta; es la que pusiste al instalar PostgreSQL. |
