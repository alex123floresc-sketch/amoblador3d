# 01 · Acta de constitución y documento de visión

| Campo | Valor |
|---|---|
| Proyecto | Amoblador3D — Diseñador de interiores 3D con medidas reales |
| Institución | Universidad Nacional de Juliaca (UNAJ) |
| Escuela profesional | `[completar]` |
| Curso | `[completar]` |
| Docente | `[completar]` |
| Integrantes | `[completar: apellidos y nombres, código]` |
| Semestre | `[completar]` |
| Versión del documento | 1.0 — noviembre de 2026 |

## 1. Problema

Quien va a amoblar o remodelar una vivienda suele decidir "a ojo": compra un mueble y recién en
casa descubre que no entra, que tapa una ventana o que no deja abrir la puerta. Tampoco sabe
cuánta pintura, cerámico o zócalo comprar, y termina con material de más o de menos.

Las herramientas profesionales (CAD de arquitectura) resuelven esto, pero son caras, difíciles de
aprender y pesadas para una computadora común. Las aplicaciones gratuitas en línea piden cuenta,
internet permanente y no calculan materiales con precios del mercado local.

## 2. Propuesta de solución

Una aplicación de escritorio, gratuita y que funciona sin internet, donde el usuario:

1. Define las **medidas reales** de cada ambiente (y de la casa completa, con varios pisos).
2. Coloca ventanas, puertas, columnas, luces y **muebles** que respetan las reglas físicas: no se
   atraviesan, no tapan puertas ni ventanas.
3. Elige **materiales y colores** de paredes, piso y techo.
4. Ve el resultado en **3D, en planta 2D o caminando** dentro del ambiente.
5. Obtiene el **metrado y presupuesto** de materiales en soles y un **informe PDF** para entregar.

## 3. Objetivos

### Objetivo general

Desarrollar una aplicación de escritorio que permita diseñar y amoblar ambientes de una vivienda en
3D con medidas reales y estimar el costo de sus acabados y muebles.

### Objetivos específicos

| Código | Objetivo | Indicador de logro |
|---|---|---|
| OE1 | Modelar habitaciones y casas de varios ambientes con medidas interiores reales | Medidas en metros con precisión de 1 cm; hasta 4 pisos |
| OE2 | Validar automáticamente la ubicación de muebles y aberturas | 0 superposiciones posibles (lo verifican las pruebas automáticas) |
| OE3 | Visualizar el diseño en 3D, planta 2D y recorrido en primera persona | Tres modos de vista operativos |
| OE4 | Calcular el metrado de acabados y un presupuesto referencial | Áreas descontando vanos; pintura, piso, zócalo y muebles en S/ |
| OE5 | Guardar los proyectos de forma confiable | Base de datos SQLite con transacciones, autoguardado y migraciones |
| OE6 | Asegurar la calidad con pruebas automatizadas | ≥ 100 pruebas unitarias e integración sin fallas |

## 4. Alcance

### Incluye

- Diseño de ambientes rectangulares o en L, con columnas y muros internos.
- Casa con varios ambientes, giro de 90° y hasta 4 pisos.
- 18 muebles paramétricos, modelos 3D `.glb/.gltf` importados y un diseñador de muebles propio.
- 11 materiales de acabado con texturas a escala real.
- Luces (bombilla, foco, sol por ventana) y modo noche.
- Metrado, presupuesto editable, exportación CSV e informe PDF.
- Proyectos en base de datos SQLite, archivos `.amob3d` (JSON) e imágenes PNG.
- Instalador para Windows que incluye su propio Java.

### No incluye (fuera de alcance)

- Planos estructurales, instalaciones eléctricas o sanitarias.
- Paredes en ángulo distinto de 90° o curvas.
- Trabajo colaborativo en línea o sincronización en la nube.
- Versión web o móvil.
- Precios en tiempo real de tiendas (los precios son referenciales y editables).

## 5. Interesados (stakeholders)

| Interesado | Interés | Influencia |
|---|---|---|
| Usuario final (familias, estudiantes, pequeños contratistas) | Diseñar su vivienda y saber cuánto gastará | Alta |
| Docente del curso | Que el proyecto cumpla los objetivos académicos | Alta |
| Equipo de desarrollo | Construir y mantener la aplicación | Alta |
| Carpinteros y vendedores de muebles | Mostrar al cliente cómo quedará un mueble | Media |

## 6. Visión del producto

> **Para** personas que van a amoblar o remodelar su vivienda **que** necesitan saber si los
> muebles entran y cuánto costarán los acabados, **Amoblador3D es** un diseñador de interiores 3D
> de escritorio **que** valida las medidas reales y calcula el presupuesto en soles. **A diferencia
> de** los programas CAD profesionales y las aplicaciones en línea, **nuestro producto** es
> gratuito, funciona sin internet, se aprende en minutos y usa precios del mercado local.

## 7. Supuestos y restricciones

- **Restricción técnica:** Java 21 o superior, JavaFX, SQLite (sin servidor de base de datos).
- **Restricción de hardware:** computadora con aceleración 3D básica (tarjeta integrada basta).
- **Supuesto:** las medidas que ingresa el usuario son interiores (espacio libre).
- **Supuesto:** los precios referenciales son de Lima, 2026, y el usuario los ajusta a su ciudad.

## 8. Criterios de éxito

1. Todas las funciones del alcance operan en Windows 10/11.
2. El conjunto de pruebas automáticas pasa al 100 %.
3. Un usuario nuevo crea y amobla una habitación en menos de 10 minutos con el manual de usuario.
4. El proyecto se entrega con su documentación completa (esta carpeta).

## 9. Aprobación

| Rol | Nombre | Firma | Fecha |
|---|---|---|---|
| Docente | `[completar]` | | |
| Jefe de proyecto | `[completar]` | | |
