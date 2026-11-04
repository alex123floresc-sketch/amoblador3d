# 07 · Manual de usuario

**Amoblador3D 2.0.0** — Diseñador de interiores 3D con medidas reales
Versión del manual 1.0 · noviembre de 2026

---

## Contenido

1. [Qué es Amoblador3D](#1-qué-es-amoblador3d)
2. [Requisitos e instalación](#2-requisitos-e-instalación)
3. [Primeros pasos](#3-primeros-pasos)
4. [La ventana principal](#4-la-ventana-principal)
5. [Configurar el ambiente](#5-configurar-el-ambiente)
6. [Paredes, piso y techo](#6-paredes-piso-y-techo)
7. [Muebles](#7-muebles)
8. [Diseñador de muebles](#8-diseñador-de-muebles)
9. [Luces](#9-luces)
10. [Casa con varios ambientes](#10-casa-con-varios-ambientes)
11. [Ver y recorrer](#11-ver-y-recorrer)
12. [Presupuesto e informe PDF](#12-presupuesto-e-informe-pdf)
13. [Guardar y compartir proyectos](#13-guardar-y-compartir-proyectos)
14. [Atajos de teclado](#14-atajos-de-teclado)
15. [Preguntas frecuentes y solución de problemas](#15-preguntas-frecuentes-y-solución-de-problemas)
16. [Glosario](#16-glosario)

---

## 1. Qué es Amoblador3D

Amoblador3D te ayuda a decidir cómo amoblar y decorar tu casa **antes de comprar**. Escribes las
medidas reales de tus ambientes, colocas puertas, ventanas y muebles, eliges colores y materiales, y
lo ves en 3D o caminando por dentro. Al final te dice cuánta pintura, cerámico y zócalo necesitas y
cuánto costará aproximadamente.

La aplicación **no deja que un mueble atraviese una pared, tape una puerta o se encime con otro**:
si algo no entra en la realidad, tampoco entra en Amoblador3D.

## 2. Requisitos e instalación

| Necesitas | Mínimo |
|---|---|
| Sistema | Windows 10 u 11 (también Linux o macOS con Java) |
| Memoria | 4 GB de RAM |
| Video | Cualquier tarjeta con aceleración 3D (las integradas funcionan) |
| Espacio | 300 MB |
| Internet | No (solo la primera vez si lo ejecutas desde el código fuente) |

**Formas de abrirlo** (detalle en el [Manual de instalación](09_Manual_de_Instalacion.md)):

- **Versión para Windows con Java incluido:** abre `Amoblador3D.exe` (o instala `Amoblador3D-2.0.0.exe`).
- **Con Java 21 instalado:** doble clic en `amoblador3d.jar`.
- **Desde el código fuente:** doble clic en `Ejecutar-Amoblador3D.cmd`.

## 3. Primeros pasos

Al abrir la aplicación aparece la pantalla **"¿Qué quieres hacer hoy?"**:

| Opción | Qué hace |
|---|---|
| **＋ Habitación vacía** | Empieza de cero con una habitación de 4 × 3,5 m |
| **▦ Desde una plantilla** | Dormitorio, sala, comedor, cocina, baño u oficina ya amoblados |
| **🏠 Casa de ejemplo** | Sala, dormitorio, cocina y baño listos para recorrer y cambiar |
| **📂 Abrir proyecto** | Tus proyectos guardados, con miniatura |

Debajo aparecen tus **proyectos recientes**. Si no quieres ver esta pantalla al iniciar, desmarca
*Mostrar al iniciar* (puedes volver a abrirla en *Ayuda → Pantalla de bienvenida*).

### Tu primer diseño en 5 minutos

1. Elige **Habitación vacía**.
2. En la pestaña **📐 Ambiente**, escribe el ancho y el largo reales de tu cuarto y pulsa Enter.
3. Pulsa **+ Puerta** y **+ Ventana**. Haz clic en cada una y, en el panel derecho, elige la pared
   y la distancia desde la esquina.
4. En la pestaña **🛋 Muebles**, haz clic en *Cama*, *Mesa de noche* y *Closet*.
5. Arrastra los muebles con el ratón hasta donde quieras. Pulsa **R** para girarlos.
6. Pulsa **💾 Guardar** (Ctrl+S) y ponle un nombre.

## 4. La ventana principal

```
┌───────────────────────────────────────────────────────────────────────────┐
│ Archivo  Editar  Proyecto  Ver  Herramientas  Ayuda               (menú)  │
│ ＋Nuevo 📂Abrir 💾Guardar ↶ ↷ | 3D Planta Recorrido | Vistas 📏 ⤢ 📷 🧲   │
│                                    💰 Presupuesto  📄 PDF  ✎ Diseñar mueble│
├───────────────────────────────────────────────────────────────────────────┤
│  [Dormitorio] [Sala] [Cocina]  ＋ Ambiente   🏠 Casa completa  (ambientes) │
├──────────────┬─────────────────────────────────────────┬──────────────────┤
│ 🛋 Muebles   │                                         │  PROPIEDADES     │
│ 📐 Ambiente  │            VISTA 3D / PLANTA            │  de lo que       │
│ 💡 Luz       │                                         │  seleccionaste   │
│ ☰ Lista      │                                         │                  │
│ (panel izq.) │                                         │  (panel derecho) │
├──────────────┴─────────────────────────────────────────┴──────────────────┤
│ Barra de estado: área del piso, paredes y avisos                          │
└───────────────────────────────────────────────────────────────────────────┘
```

| Zona | Para qué sirve |
|---|---|
| **Barra de herramientas** | Acciones frecuentes. Pasa el ratón encima de un botón para ver qué hace y su atajo |
| **Barra de ambientes** | Cambiar de ambiente, agregar uno o ver la casa completa |
| **Panel izquierdo** | Pestañas: **🛋 Muebles** (catálogo), **📐 Ambiente** (medidas, acabados, ventanas, puertas, columnas), **💡 Luz**, **☰ Lista** (todo lo que hay en el ambiente) |
| **Vista central** | El diseño en 3D, en planta o en recorrido |
| **Panel derecho** | Las propiedades de lo que seleccionaste (medidas, posición, color…). Los números se aplican al pulsar Enter o al salir del campo |
| **Barra de estado** | Áreas y mensajes de ayuda |

**Ratón en la vista 3D:** botón izquierdo = girar la cámara · botón derecho = mover la cámara ·
rueda = acercar o alejar · arrastrar un mueble = moverlo · clic = seleccionar.

## 5. Configurar el ambiente

Pestaña **📐 Ambiente**.

### 5.1 Medidas

| Campo | Rango | Nota |
|---|---|---|
| Ancho y largo | 1 a 30 m | Medidas **interiores**: mide de pared a pared por dentro |
| Alto | 2 a 6 m | Del piso al techo |
| Grosor de muro | 5 a 50 cm | Los muros crecen hacia afuera; no te quitan espacio |

> Si achicas el ambiente y una ventana o puerta ya no entra, se elimina y la aplicación te avisa cuál.

### 5.2 Ventanas y puertas

- **+ Ventana** agrega una ventana en el primer lugar libre.
- **+ Puerta ▾** permite elegir: *normal*, *principal*, *doble*, *corrediza*, *portón de cochera* o
  *vano sin puerta*. El tipo se puede cambiar luego en el panel derecho.
- Selecciona la abertura y cambia **pared**, **distancia desde la esquina**, **ancho**, **alto** y
  (en ventanas) **alféizar**.
- Dos aberturas nunca se superponen: si escribes una posición ocupada, el cambio no se aplica y se
  explica por qué.

### 5.3 Columnas y forma

- **+ Columna** (30 × 30 cm) y **+ Muro interno** (1,5 m × 12 cm): bloques del piso al techo. Los
  muebles no los atraviesan y se descuentan del piso en el presupuesto. Se mueven con las flechas o
  desde el panel derecho.
- **Habitación en L…**: elige qué esquina quitar y de qué tamaño.

## 6. Paredes, piso y techo

Haz clic sobre una pared, el piso o el techo (o usa *Paredes, piso y techo* en la pestaña Ambiente).
En el panel derecho elige:

- **Material:** pintura mate, pintura satinada, papel tapiz, ladrillo, piedra, concreto, madera,
  cerámico, mármol, porcelanato o alfombra. Solo aparecen los que tienen sentido para esa
  superficie (no hay alfombra en paredes).
- **Color:** cualquiera; tiñe la textura.
- **Aplicar a todas las paredes:** copia el material y el color a las cuatro paredes.
- **Zócalo:** alto (0 = sin zócalo) y color.

Las texturas están a **escala real**: un ladrillo mide lo que mide un ladrillo.

## 7. Muebles

### 7.1 Agregar

En **🛋 Muebles**, filtra por categoría (Dormitorio, Sala, Comedor, Cocina, Baño, Oficina,
Decoración, Mis diseños…) o escribe en el **buscador** (no importan mayúsculas ni tildes). Haz clic
en un mueble o arrástralo a la vista.

La aplicación busca un lugar libre. Si no cabe derecho, lo prueba girado. Si no cabe de ninguna
forma, te dice por qué (por ejemplo: *"No hay espacio libre para la cama"*).

### 7.2 Mover, girar y cambiar medidas

| Acción | Cómo |
|---|---|
| Mover | Arrastrar con el ratón · flechas (5 cm) · Shift + flechas (1 cm) |
| Girar | **R** / Shift+R (90°) · **Q** / **E** (15°) · asa redonda delante del mueble |
| Cambiar medidas | Asas en los lados del mueble seleccionado, o los campos del panel derecho |
| Agrandar / achicar todo | Botones ±10 % en el panel; *Medidas originales* para volver |
| Duplicar / eliminar | **Ctrl+D** / **Supr** |
| Bloquear | **L** (un mueble bloqueado no se mueve ni se borra por accidente) |

**Imán (G):** al arrastrar, el mueble se pega a las paredes y a los bordes o centros de otros
muebles, y aparecen líneas guía. Mantén **Alt** para soltarlo un momento.

### 7.3 Varios muebles a la vez

- **Shift o Ctrl + clic:** agregar o quitar de la selección.
- **Shift + arrastrar en el vacío:** rectángulo de selección.
- **Ctrl+A:** seleccionar todos.

Luego puedes moverlos, girarlos, duplicarlos o eliminarlos juntos.

### 7.4 Copiar y pegar

**Ctrl+C / Ctrl+X / Ctrl+V** — funciona también entre proyectos distintos.

### 7.5 Colores y acabado

En el panel derecho: **color principal**, **color secundario** (frazada, cojines, frentes de
cajones, patas, encimera, grifería…) y **acabado** (mate, satinado, brillante o metálico). Los
modelos 3D importados se pueden **teñir**.

### 7.6 Importar un modelo 3D

*Archivo → Importar modelo 3D (.glb/.gltf)…* (Ctrl+I). Revisa el nombre, el giro (para que el frente
mire hacia adelante) y las **medidas reales**; con *Mantener proporciones* basta cambiar una.
Modelos gratuitos: Poly Haven, Sketchfab (*Downloadable*), Kenney (*Furniture Kit*). Revisa la
licencia de cada modelo. No se admiten modelos comprimidos con Draco o Meshopt.

## 8. Diseñador de muebles

Botón **✎ Diseñar mueble** (Ctrl+Shift+D).

1. Empieza vacío, desde una plantilla (mesa, mesa redonda, estante, banco) o desde un mueble del catálogo.
2. Agrega piezas: **caja**, **cilindro**, **esfera** o **barra**.
3. En la pestaña **Pieza**, escribe medidas y posición **en centímetros**, y elige color (principal,
   secundario o propio) y acabado.
4. En la pestaña **Mueble**, ponle nombre y categoría (por defecto *Mis diseños*).
5. Guarda. Tu diseño aparece en el catálogo.

Dentro del diseñador: Ctrl+Z/Y deshacer y rehacer, Ctrl+D duplicar pieza, Supr eliminar, flechas mover.
Para editar un mueble ya colocado: selecciónalo y usa *Diseñar a partir de este…*; al guardar se
reemplaza en el mismo lugar. Clic derecho sobre un diseño del catálogo para editarlo, duplicarlo o quitarlo.

## 9. Luces

Pestaña **💡 Luz**.

| Botón | Tipo |
|---|---|
| **+ Bombilla** | Luz puntual, ilumina en todas direcciones |
| **+ Foco** | Cono de luz dirigido a un punto del piso |
| **+ Sol** | Luz solar que entra por una ventana y dibuja su mancha en el piso |

- **Luz ambiental:** claridad general.
- **Día o noche (N):** el modo noche quita el sol y baja la luz ambiental para ver las lámparas. Solo
  cambia cómo se ve; no se guarda en el proyecto.
- Cada objeto se ilumina con **máximo 3 luces** a la vez (límite de JavaFX).
- Si eliminas la última ventana, el sol se elimina.

## 10. Casa con varios ambientes

- **＋ Ambiente** (barra de ambientes): vacío, desde una plantilla o copiando el actual. Se coloca
  pegado a los demás.
- **Clic** en un ambiente de la barra para decorarlo; **clic derecho** para renombrar, duplicar o eliminar.
- **🏠 Casa completa** (Ctrl+4): todos los ambientes juntos en 3D o en planta.
  - **Arrastra** un ambiente para ubicarlo; los muros se pegan a los de los demás y nunca se enciman
    (Alt para mover sin imán).
  - En el panel derecho: medidas, **girar** de 90 en 90° y **piso** (hasta 4).
  - Elige qué piso ver: en 3D se ven ese piso y los de abajo.
  - **Doble clic** en un ambiente para entrar a decorarlo.
- Si la puerta de un ambiente toca el muro de otro, ese muro se abre para pasar.
- El resumen muestra el área y el presupuesto de cada ambiente y de toda la casa, y avisa si
  ninguna puerta da a la calle.

## 11. Ver y recorrer

| Vista | Atajo | Uso |
|---|---|---|
| **3D** | Ctrl+1 | Girar alrededor del ambiente |
| **Planta** | Ctrl+2 | Vista desde arriba con cotas, como un plano |
| **Recorrido** | Ctrl+3 | Caminar a la altura de los ojos |

**Recorrido:** W/S o ↑/↓ avanzar y retroceder · A/D de costado · ←/→ girar · arrastrar para mirar ·
Shift para correr. No se atraviesan paredes ni muebles. El techo se ve solo en el recorrido.

**Otras herramientas:**

- **Vistas:** guardar la vista actual con un nombre (se guarda con el proyecto), volver a ella o ver
  el ambiente desde cada puerta.
- **📏 Medidas (Ctrl+M):** mostrar u ocultar cotas.
- **Medir (M):** dos clics en el piso; los puntos se pegan a paredes y esquinas. Esc para terminar.
- **⤢ Encuadrar (Ctrl+0)** y **📷 Captura PNG (Ctrl+E)**.

## 12. Presupuesto e informe PDF

**💰 Presupuesto** (Ctrl+P) muestra:

- m² de piso y de paredes **sin** ventanas, puertas ni columnas; metros de zócalo.
- **Pintura:** galones para 2 manos (un galón cubre unos 35 m² por mano), redondeado al cuarto de galón.
- **Papel tapiz:** rollos.
- **Pisos (cerámico, madera…):** m² con **10 % de desperdicio**.
- **Muebles** con su precio.

Los precios son **referenciales en soles (Lima, 2026)**. Cámbialos con **doble clic**: se guardan
para todos tus proyectos. **Exportar CSV** abre el presupuesto en Excel.

**📄 PDF** (Ctrl+Shift+P): informe con datos del ambiente, planta con cotas, vista 3D, ventanas y
puertas, muebles y presupuesto. Listo para imprimir o enviar.

## 13. Guardar y compartir proyectos

| Acción | Dónde | Nota |
|---|---|---|
| Guardar / Guardar como | Ctrl+S / Archivo | En la base de datos local, con miniatura |
| Abrir | Ctrl+O | Lista con miniaturas; renombrar o eliminar desde ahí |
| Exportar `.amob3d` | Archivo | Para pasar el proyecto a otra PC. Incluye tus diseños de muebles |
| Importar `.amob3d` | Archivo | Si algo del archivo está dañado, se corrige y se avisa |
| Deshacer / rehacer | Ctrl+Z / Ctrl+Y | Hasta 100 pasos, incluidos los cambios de la casa |

**Autoguardado:** mientras hay cambios sin guardar, se hace una copia cada minuto. Si la aplicación
se cierra de golpe (corte de luz, error), al volver a abrirla te ofrece **recuperar** el trabajo.

## 14. Atajos de teclado

Pulsa **F1** dentro de la aplicación para verlos todos.

| Tecla | Acción |
|---|---|
| Ctrl+N / Ctrl+O / Ctrl+S | Nuevo / Abrir / Guardar |
| Ctrl+Shift+N | Nuevo desde plantilla |
| Ctrl+Z / Ctrl+Y | Deshacer / Rehacer |
| Flechas (Shift: más fino) | Mover el mueble 5 cm (1 cm) |
| R / Shift+R · Q / E | Girar 90° · girar 15° |
| Supr / Ctrl+D | Eliminar / Duplicar |
| Ctrl+C / Ctrl+X / Ctrl+V | Copiar / Cortar / Pegar |
| Ctrl+A | Seleccionar todos los muebles |
| L / G | Bloquear / Imán |
| Ctrl+1 / 2 / 3 / 4 | 3D / Planta / Recorrido / Casa completa |
| N | Modo noche |
| Ctrl+P / Ctrl+Shift+P | Presupuesto / Informe PDF |
| Ctrl+Shift+D | Diseñador de muebles |
| Ctrl+I | Importar modelo 3D |
| Ctrl+M / Ctrl+0 / Ctrl+E | Cotas / Encuadrar / Captura PNG |
| M | Medir distancias (Esc para terminar) |
| F1 | Todos los atajos |

## 15. Preguntas frecuentes y solución de problemas

| Problema | Solución |
|---|---|
| "No hay espacio libre para…" | El mueble no entra en ningún lugar libre. Achica otro mueble, muévelo, o agranda el ambiente |
| No puedo mover un mueble | Puede estar **bloqueado** (pulsa L) o chocaría con algo en ese lugar |
| El mueble no se pega donde quiero | Mantén **Alt** mientras arrastras, o desactiva el imán con **G** |
| La vista 3D se ve negra o lenta | Actualiza el controlador de la tarjeta de video |
| "No se pudo abrir la base de datos" | Hay otra copia de Amoblador3D abierta, o el archivo `.db` está abierto en otro programa. Ciérralos |
| Un modelo importado se ve gigante o diminuto | Corrige sus medidas reales al importarlo, o en el panel derecho |
| Perdí mi trabajo por un corte de luz | Abre la aplicación: te ofrecerá recuperar la última copia (se hace cada minuto) |
| Apareció "Algo salió mal" | Guarda tu trabajo. Abre *Ayuda → Abrir registro de errores* y envía el archivo `amoblador3d-0.log` al equipo de soporte |
| ¿Dónde están mis datos? | En `%APPDATA%\Amoblador3D` (escribe eso en la barra del Explorador de Windows) |

## 16. Glosario

| Término | Significado |
|---|---|
| Alféizar | Altura desde el piso hasta el borde inferior de la ventana |
| Ambiente | Cada habitación de la casa |
| Cota | Medida dibujada en el plano |
| Metrado | Cálculo de cantidades: m² de pared, piso, metros de zócalo |
| Planta | Vista desde arriba, como un plano |
| Vano | Abertura en la pared sin puerta |
| Zócalo | Franja al pie de las paredes |
