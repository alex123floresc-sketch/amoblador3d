# 09 · Manual de instalación y despliegue

Versión 1.0 · noviembre de 2026 · Software 2.0.0

Hay tres formas de instalar Amoblador3D. Elige según quién lo va a usar.

| Opción | Para quién | Necesita Java | Dificultad |
|---|---|---|---|
| A. Versión para Windows con Java incluido | Usuario final | No | Muy fácil |
| B. Archivo `.jar` | Quien ya tiene Java 21+ | Sí | Fácil |
| C. Desde el código fuente | Desarrolladores y evaluadores | Sí (JDK) | Media |

## Requisitos del equipo

| Elemento | Mínimo | Recomendado |
|---|---|---|
| Sistema operativo | Windows 10 64 bits | Windows 11 |
| Procesador | Doble núcleo 2 GHz | Cuatro núcleos |
| Memoria RAM | 4 GB | 8 GB |
| Video | Aceleración 3D (integrada) | Tarjeta dedicada |
| Disco | 300 MB libres | 1 GB (si importas muchos modelos) |
| Pantalla | 1280 × 720 | 1920 × 1080 |

## Opción A — Versión para Windows con Java incluido

**Generarla** (la hace el equipo de desarrollo, una vez por versión):

1. Instala el JDK 21 o superior (trae `jpackage`).
2. Doble clic en `packaging\crear-instalador.cmd` (o `.\packaging\crear-instalador.ps1` en PowerShell;
   con `-SinPruebas` es más rápido).
3. En `target\instalador\` aparecen:
   - Carpeta `Amoblador3D\` con `Amoblador3D.exe` y `Amoblador3D-2.0.0-windows.zip` (unos 60 MB) para compartir.
   - Si está instalado [WiX Toolset](https://wixtoolset.org): el instalador `Amoblador3D-2.0.0.exe`.

**Instalarla** (el usuario):

- **Con el ZIP:** descomprímelo en cualquier carpeta y abre `Amoblador3D.exe`. No se instala nada.
- **Con el instalador `.exe`:** ejecútalo, elige la carpeta y termina. Crea accesos directos en el
  menú Inicio y en el escritorio. Se instala solo para el usuario actual (no pide administrador).

Si Windows SmartScreen muestra *"Windows protegió su PC"*: *Más información → Ejecutar de todas
formas* (aparece porque el ejecutable no está firmado digitalmente).

## Opción B — Archivo `.jar`

1. Instala Java 21+ desde <https://adoptium.net> marcando *Add to PATH*.
2. Comprueba en PowerShell: `java -version` (debe decir 21 o más).
3. Doble clic en `amoblador3d.jar`, o: `java -jar amoblador3d.jar`.

El `.jar` se genera con `.\mvnw.cmd package` (queda en `target\`) o se descarga de los artefactos de
GitHub Actions. Debe usarse en el mismo sistema operativo en que se compiló.

## Opción C — Desde el código fuente

```powershell
git clone https://github.com/alex123floresc-sketch/amoblador3d.git
cd amoblador3d
.\mvnw.cmd javafx:run
```

O doble clic en `Ejecutar-Amoblador3D.cmd`. La primera vez descarga Maven y las librerías (necesita
internet y tarda unos minutos). Detalle completo y problemas frecuentes: [`GUIA_EJECUCION.md`](../GUIA_EJECUCION.md).

## Verificar la instalación

1. Abre la aplicación: debe aparecer *"¿Qué quieres hacer hoy?"*.
2. Elige *Casa de ejemplo*: debe verse en 3D.
3. *Ayuda → Acerca de Amoblador3D*: muestra la versión (2.0.0) y la carpeta de datos.

## Datos del usuario

| Qué | Dónde |
|---|---|
| Proyectos, diseños, precios | `%APPDATA%\Amoblador3D\amoblador3d.db` |
| Modelos 3D importados | `%APPDATA%\Amoblador3D\modelos\` |
| Copia de recuperación | `%APPDATA%\Amoblador3D\recuperacion.json` |
| Registro de errores | `%APPDATA%\Amoblador3D\logs\` |

**Copia de seguridad:** con la aplicación cerrada, copia la carpeta `%APPDATA%\Amoblador3D` completa.
**Pasar a otra PC:** copia esa carpeta al mismo lugar en la otra PC, o exporta cada proyecto como `.amob3d`.
**Usar otra carpeta** (por ejemplo una USB): `java -Damoblador.home=D:\MisDatos -jar amoblador3d.jar`.

## Actualizar a una versión nueva

Instala o descomprime la versión nueva encima o en otra carpeta. Los datos no se tocan: al abrir,
la base de datos se actualiza sola al nuevo esquema sin perder proyectos. **No** se puede abrir una
base creada por una versión más nueva con una más antigua (la aplicación lo avisa).

## Desinstalar

- **Instalador:** *Configuración → Aplicaciones → Amoblador3D → Desinstalar*.
- **ZIP o `.jar`:** borra la carpeta.
- Para borrar también tus proyectos, elimina `%APPDATA%\Amoblador3D` (no se puede deshacer).

## Base de datos PostgreSQL (opcional, para consultas)

La aplicación usa SQLite. Para revisar el modelo de datos en PostgreSQL 13+:

```powershell
& "C:\Program Files\PostgreSQL\17\bin\psql.exe" -U postgres -f database\amoblador3d_postgresql.sql
```

Instrucciones para pgAdmin y consultas de ejemplo en [`GUIA_EJECUCION.md`](../GUIA_EJECUCION.md#bases-de-datos).
