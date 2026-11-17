# Enlaces del proyecto Space Cat

**Space Cat** (antes *Amoblador3D*) es una aplicación de escritorio para Windows que permite
diseñar y amueblar ambientes en 3D con medidas reales y obtener el metrado y el presupuesto en
soles. Desarrollada por el equipo **FSHF**.

El proyecto tiene tres partes, cada una en su propio repositorio de GitHub. Las dos páginas web
están publicadas con **GitHub Pages** y se pueden abrir desde cualquier navegador, sin instalar nada.

## Resumen

| Parte | Página web (para verla) | Repositorio (código) |
|---|---|---|
| Aplicación | [Descargas (Releases)](https://github.com/alex123floresc-sketch/amoblador3d/releases/latest) | <https://github.com/alex123floresc-sketch/amoblador3d> |
| Web del docente | <https://alex123floresc-sketch.github.io/amoblador3d_web/> | <https://github.com/alex123floresc-sketch/amoblador3d_web> |
| Web de clientes | <https://alex123floresc-sketch.github.io/Space_Cat/> | <https://github.com/alex123floresc-sketch/Space_Cat> |

## 1. Aplicación Space Cat (este repositorio)

- **Repositorio:** <https://github.com/alex123floresc-sketch/amoblador3d>
- **Qué contiene:** el código fuente completo en Java 21 + JavaFX (vista 3D), la base de datos
  SQLite (y su versión en PostgreSQL en `database/`), las pruebas automáticas y los scripts para
  crear el instalador de Windows (`packaging/`).
- **Versión actual:** 2.1.0 — <https://github.com/alex123floresc-sketch/amoblador3d/releases/tag/v2.1.0>

Descargas directas de la versión 2.1.0:

| Archivo | Para qué | Tamaño |
|---|---|---|
| [SpaceCat-2.1.0.exe](https://github.com/alex123floresc-sketch/amoblador3d/releases/download/v2.1.0/SpaceCat-2.1.0.exe) | Instalador para Windows 10/11. Trae su propio Java y crea accesos directos. **Recomendado.** | 61 MB |
| [SpaceCat-2.1.0-windows.zip](https://github.com/alex123floresc-sketch/amoblador3d/releases/download/v2.1.0/SpaceCat-2.1.0-windows.zip) | Versión portátil: se descomprime y se abre, sin instalar. | 60 MB |
| [SpaceCat-2.1.0.jar](https://github.com/alex123floresc-sketch/amoblador3d/releases/download/v2.1.0/SpaceCat-2.1.0.jar) | Para quien ya tiene Java 21 o superior instalado. | 25 MB |

Para ejecutarlo desde el código fuente:

```powershell
git clone https://github.com/alex123floresc-sketch/amoblador3d.git
cd amoblador3d
.\mvnw.cmd javafx:run
```

## 2. Web del docente

- **Página:** <https://alex123floresc-sketch.github.io/amoblador3d_web/>
- **Repositorio:** <https://github.com/alex123floresc-sketch/amoblador3d_web>
- **Para quién:** el docente del curso. Presenta el producto y el trabajo técnico del equipo.
- **Tecnología:** HTML, CSS y JavaScript sin frameworks. Se publica desde la rama `main`.
- **Secciones:**
  [Novedades 2.1.0](https://alex123floresc-sketch.github.io/amoblador3d_web/#novedades) ·
  [Qué hace](https://alex123floresc-sketch.github.io/amoblador3d_web/#que-hace) ·
  [Para quién es](https://alex123floresc-sketch.github.io/amoblador3d_web/#para-quien) ·
  [Cómo usarla](https://alex123floresc-sketch.github.io/amoblador3d_web/#como-usar) ·
  [Atajos de teclado](https://alex123floresc-sketch.github.io/amoblador3d_web/#atajos) ·
  [Código fuente](https://alex123floresc-sketch.github.io/amoblador3d_web/#codigo) ·
  [Descargas](https://alex123floresc-sketch.github.io/amoblador3d_web/#descargas) ·
  [Preguntas frecuentes](https://alex123floresc-sketch.github.io/amoblador3d_web/#preguntas)
- **Documentos en PDF:**
  - [Manual de usuario](https://alex123floresc-sketch.github.io/amoblador3d_web/descargas/05_Manual_de_Usuario.pdf)
  - [Manual de instalación](https://alex123floresc-sketch.github.io/amoblador3d_web/descargas/06_Manual_de_Instalacion.pdf)
  - [Código fuente (resumen)](https://alex123floresc-sketch.github.io/amoblador3d_web/descargas/09_Codigo_Fuente.pdf)
  - [Código fuente completo](https://alex123floresc-sketch.github.io/amoblador3d_web/descargas/10_Codigo_Fuente_Completo.pdf)

## 3. Web de clientes

- **Página:** <https://alex123floresc-sketch.github.io/Space_Cat/>
- **Repositorio:** <https://github.com/alex123floresc-sketch/Space_Cat>
- **Para quién:** las personas que van a usar la aplicación. Explica qué hace, cómo instalarla y
  dónde descargarla, en lenguaje sencillo.
- **Tecnología:** Astro + Tailwind CSS + Fomantic UI. Se publica sola con GitHub Actions al hacer
  push a `main`.
- **Secciones:**
  [Funciones](https://alex123floresc-sketch.github.io/Space_Cat/#funciones) ·
  [Cómo se usa](https://alex123floresc-sketch.github.io/Space_Cat/#como) ·
  [Instalar](https://alex123floresc-sketch.github.io/Space_Cat/#instalar) ·
  [Descargar](https://alex123floresc-sketch.github.io/Space_Cat/#descargar) ·
  [Ayuda](https://alex123floresc-sketch.github.io/Space_Cat/#ayuda)
- **Documentos en PDF:**
  - [Manual de usuario](https://alex123floresc-sketch.github.io/Space_Cat/descargas/Space_Cat_Manual_de_Usuario.pdf)
  - [Manual de instalación](https://alex123floresc-sketch.github.io/Space_Cat/descargas/Space_Cat_Manual_de_Instalacion.pdf)

## Soporte

Equipo **FSHF** — Juliaca, Puno. WhatsApp: **919 192 008**.
