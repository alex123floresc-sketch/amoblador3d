# Documentación de Amoblador3D

**Proyecto:** Amoblador3D — Diseñador de interiores 3D con medidas reales
**Institución:** Universidad Nacional de Juliaca (UNAJ)
**Versión del software:** 2.0.0 · **Versión de la documentación:** 1.0 · **Fecha:** noviembre de 2026

**Facultad:** Facultad de Ciencias de Ingenierías · **Escuela:** Ingeniería de Sistemas y Software
**Asignatura:** Bases de Datos · **Docente:** Noe Tipo Mamani
**Integrantes:** Flores Calcina, Alexander Edson · Sandia Condori, George James · Huanca Tiña, Erick Reison · Flores Ccori, Brayan

> Los pocos datos que aún faltan (semestre, reparto de roles, horas y costos reales) están marcados
> como `[completar]`. Búscalos con Ctrl+F antes de entregar.

## Documentos

Un proyecto de software universitario debe tener estos documentos. Están ordenados según las
etapas del ciclo de vida: **inicio → requisitos → diseño → construcción → pruebas → entrega**.

<!-- Tabla: Documentos del proyecto, propósito, lectores y norma de referencia -->
| N.° | Documento | Para qué sirve | Quién lo lee | Norma de referencia |
|---|---|---|---|---|
| 01 | [Acta de constitución y documento de visión](01_Acta_de_Constitucion_y_Vision.md) | Qué problema se resuelve, objetivos, alcance, interesados | Docente, equipo | PMBOK 7 |
| 02 | [Especificación de requisitos de software (ERS)](02_Especificacion_de_Requisitos_ERS.md) | Qué debe hacer el sistema: requisitos funcionales y no funcionales | Docente, equipo, evaluador | IEEE 830 / ISO/IEC/IEEE 29148 |
| 03 | [Casos de uso](03_Casos_de_Uso.md) | Cómo interactúa el usuario con el sistema, paso a paso | Equipo, evaluador | UML 2.5 |
| 04 | [Documento de arquitectura de software](04_Documento_de_Arquitectura.md) | Cómo está construido: capas, paquetes, clases clave, decisiones | Desarrolladores | ISO/IEC/IEEE 42010, modelo C4 |
| 05 | [Diseño de base de datos](05_Diseno_de_Base_de_Datos.md) | Modelo entidad-relación, diccionario de datos, migraciones | Desarrolladores, DBA | — |
| 06 | [Plan y reporte de pruebas](06_Plan_y_Reporte_de_Pruebas.md) | Qué se probó, cómo y con qué resultado | Docente, QA | IEEE 829 / ISO/IEC/IEEE 29119 |
| 07 | [Manual de usuario](07_Manual_de_Usuario.md) | Cómo usar la aplicación | Usuario final | ISO/IEC/IEEE 26514 |
| 08 | [Manual técnico del desarrollador](08_Manual_Tecnico.md) | Cómo compilar, extender y mantener el código | Desarrolladores | — |
| 09 | [Manual de instalación y despliegue](09_Manual_de_Instalacion.md) | Cómo instalar en otra computadora y generar el instalador | Usuario, soporte | — |
| 10 | [Plan del proyecto, cronograma y riesgos](10_Plan_de_Proyecto.md) | Metodología, fases, cronograma, riesgos, costos | Docente, equipo | PMBOK 7, ISO/IEC 12207 |
| 11 | [Informe final del proyecto](11_Informe_Final.md) | Documento académico de entrega: resumen, resultados, conclusiones | Docente, jurado | APA 7.ª edición |
| 12 | [Historial de versiones (CHANGELOG)](12_Historial_de_Versiones.md) | Qué cambió en cada fase | Todos | Keep a Changelog |
| 13 | [Normas de redacción y formato](13_Normas_de_Redaccion.md) | Normas APA 7, normas técnicas por documento, reglas de redacción y lista de verificación | Equipo | APA 7.ª ed. |

### Documentos que ya existen en la raíz del repositorio

<!-- Tabla: Documentos complementarios ubicados en la raíz del repositorio -->
| Archivo | Contenido |
|---|---|
| [`README.md`](../README.md) | Presentación del proyecto, funciones, atajos y formato `.amob3d` |
| [`GUIA_EJECUCION.md`](../GUIA_EJECUCION.md) | Paso a paso para ejecutarlo, bases de datos y problemas frecuentes |
| [`database/`](../database) | Scripts SQL (SQLite y PostgreSQL) |
| [`documentacion/capturas/`](capturas) | Capturas de pantalla reales de la aplicación (usadas en el manual de usuario y el informe final) |
| Javadoc en el código | Cada clase explica qué hace y por qué |

### Orden de lectura sugerido

- **Docente / jurado:** 11 → 01 → 02 → 06.
- **Usuario que solo quiere usar la aplicación:** 09 → 07.
- **Desarrollador que va a continuar el proyecto:** 04 → 05 → 08 → 06.

## Versión en Word (carpeta `doc 2`)

En [`doc 2`](doc%202) están los mismos documentos en formato Word (`.docx`), con normas **APA 7**:

- **Carátula institucional de la UNAJ** (logo, facultad, escuela, título, integrantes, docente,
  asignatura, lugar y año).
- **Encabezado** en todas las páginas, salvo la carátula: logo de la UNAJ, universidad, facultad y
  escuela. El número de página va al pie, a la derecha.
- **Índice general** automático y, cuando el documento las tiene, **índice de tablas** e
  **índice de figuras** con el título específico y la página de cada una.
- Times New Roman 12, márgenes de 2,54 cm, sangría de 1,27 cm, títulos por niveles, tablas y figuras
  numeradas con su título en cursiva y los diagramas como imágenes.

El detalle de las normas está en el documento 13. Los datos de la carátula están en `META`, al
inicio de [`herramientas/generar.js`](herramientas/generar.js), y los logos en `herramientas/recursos/`.

- Los textos `[completar]` aparecen **resaltados en amarillo**.
- Al abrir un documento, Word puede preguntar si desea **actualizar los campos**: responde *Sí* para
  que se llenen los índices y sus números de página.
- Para dar título a una tabla o diagrama nuevo, escribe antes del bloque un comentario
  `<!-- Tabla: título -->` o `<!-- Figura: título -->`. Si no lo pones, se usa el nombre de la sección.
- Los Word se generan a partir de estos Markdown. Si cambias un `.md`, vuelve a generarlos (necesita
  Node.js y Microsoft Edge, que dibuja los diagramas):

  ```powershell
  cd documentacion\herramientas
  npm install        # solo la primera vez
  npm run generar    # reescribe los .docx de "doc 2"
  ```

  Ojo: si editaste un Word a mano, al regenerar se pierden esos cambios.

Los diagramas de los Markdown están escritos en **Mermaid**: GitHub los dibuja solos.
