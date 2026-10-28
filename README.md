# Misiones para Minecraft 26.2 (Forge)

Generador de misiones para darle un **objetivo** a cada partida: desde "consigue 16 manzanas" hasta "vence al Wither con una espada Filo V".

- **20 tipos de misión** y **600 misiones** de fábrica (10 por tipo en cada dificultad).
- **Misión diaria**: cada día de Minecraft te llega una nueva.
- **Cadenas**: al cumplir una misión se desbloquea la siguiente.
- **Límite** de misiones activas a la vez (10 de fábrica).
- Todo se puede cambiar: puedes editar las misiones, agregar las tuyas y crear tus propias cadenas.

## ⬇️ Descargar

**[Descargar el mod (.jar)](https://github.com/javieraguirre83/misiones/releases/download/ultima/Misiones-2.0.0-forge-26.2.jar)**

1. Descarga el archivo `.jar` (si tenías la versión 1, borra el `.jar` viejo de la carpeta `mods`).
2. Presiona **Windows + R**, escribe `%appdata%\.minecraft\mods` y dale Enter.
3. Pega ahí el `.jar` y abre Minecraft con el perfil de **Forge 26.2**.

Los textos salen en **español o inglés** según el idioma de tu Minecraft. El comando funciona como `/misiones` o `/missions`. **No necesitas trucos activados.**

---

## 🎲 Generar misiones al azar

| Comando | Qué hace |
|---|---|
| `/misiones generar 10` | 10 misiones según el modo (de fábrica: **balanceado**) |
| `/misiones generar 5 3 2` | Tú eliges: 5 fáciles 🟢, 3 medias 🟡 y 2 difíciles 🔴 |
| `/misiones` | Ver tu lista y el avance de cada una |

- **Balanceado:** más o menos la mitad fáciles, un tercio medias y el resto difíciles.
- **Al azar:** cualquier dificultad.
- Además se **reparten los tipos**, para que no te salgan 5 de matar seguidas.
- No te salen logros que ya tienes.

## 📜 Los 20 tipos de misión

| Tipo | Cómo se cumple |
|---|---|
| 🍎 Conseguir | Tener las cosas **en tu inventario** |
| 📦 Cofre | Guardar las cosas en un cofre y cerrarlo |
| ⛏ Minar | Romper bloques (piedra, diamante, escombros…) |
| 🌾 Cosechar | Romper cultivos **ya crecidos** (trigo, zanahorias, verruga…) |
| ⚔ Matar | Matar monstruos o animales (también jefes) |
| 🐄 Criar | Que nazcan crías de animales que alimentaste |
| 🐺 Domesticar | Domar lobos, gatos, loros, caballos, burros, mulas o llamas |
| 🎣 Pescar | Sacar cosas con la caña |
| 🔨 Fabricar | Craftear objetos |
| ✨ Encantar | Encantar en la mesa o agregar encantamientos en el yunque |
| 🧪 Pociones | Sacar pociones del soporte para pociones |
| 🍗 Comer | Comer o beber |
| 🔥 Fundir | Sacar cosas del horno, alto horno o ahumador |
| 🤝 Comerciar | Tratos con aldeanos y comerciantes, o trueques con piglins |
| 🛡 Equipar | Traer **puesta** la armadura (las armas y herramientas basta con tenerlas en el inventario), con los encantamientos que pida |
| 🧭 Explorar | Llegar a una dimensión, estructura, bioma o altura |
| 🗺 Viajar | Recorrer bloques caminando, en barco, con élitros, etc. |
| 🏆 Logros | Conseguir un logro de Minecraft |
| ⏳ Sobrevivir | Pasar días sin morir (si mueres, vuelve a 0) |
| ⭐ Nivel | Llegar a un nivel de experiencia |
| ✍ Libre | Tú escribes la misión y tú decides cuándo está lista |

---

## ⛓ Cadenas de misiones

Al cumplir una misión de la cadena te llega la siguiente. Si ya estás en tu límite de misiones, la siguiente **espera** y llega en cuanto tengas lugar.

| Comando | Qué hace |
|---|---|
| `/misiones catalogo cadenas` | Ver las cadenas y sus pasos |
| `/misiones cadena wither` | Empezar la cadena "wither" |

Cadenas de fábrica: **wither** (del hierro al Wither), **end** (hasta matar al dragón y ponerte élitros), **granjero**, **mago** y **explorador**.

## 📅 Misión diaria y límite

- Cada vez que empieza un nuevo día en Minecraft te llega **una misión nueva** (marcada con 📅).
- El límite de misiones activas es **10**. Las completadas no cuentan. Si te llega una misión diaria o de cadena estando en el límite, se queda en espera.
- Las misiones que **agregas a mano** siempre entran, sin importar el límite.

## ⚙ Ajustes

| Comando | Qué hace |
|---|---|
| `/misiones ajustes` | Ver los ajustes actuales |
| `/misiones ajustes limite 15` | Cambiar el límite (0 = sin límite) |
| `/misiones ajustes modo azar` | Modo al azar (o `balanceado`) |
| `/misiones ajustes balanceado 60 30 10` | Porcentajes del modo balanceado: fáciles, medias, difíciles |
| `/misiones ajustes diaria no` | Apagar la misión diaria (o `si`) |
| `/misiones ajustes dificultaddiaria facil` | Dificultad de la diaria: `azar`, `facil`, `media` o `dificil` |
| `/misiones ajustes repartirtipos no` | Dejar de repartir los tipos |

También están en el archivo `config/misiones/ajustes.json`.

---

## ✍️ Agregar misiones a mano

Al final puedes poner la dificultad: `facil`, `media` o `dificil` (si no la pones, es media). Presiona **Tab** para ver sugerencias. Donde pide una cosa también puedes escribir `cualquiera`.

| Ejemplo | Qué pide |
|---|---|
| `/misiones agregar conseguir 100 minecraft:apple facil` | Tener 100 manzanas |
| `/misiones agregar cofre 64 minecraft:oak_log` | Guardar 64 troncos de roble en un cofre |
| `/misiones agregar minar 10 #minecraft:diamond_ores dificil` | Minar 10 menas de diamante (de cualquier tipo) |
| `/misiones agregar cosechar 50 minecraft:wheat` | Cosechar 50 de trigo crecido |
| `/misiones agregar matar 20 minecraft:zombie` | Matar 20 zombis |
| `/misiones agregar criar 5 minecraft:cow` | Que nazcan 5 becerros |
| `/misiones agregar domesticar 2 minecraft:wolf` | Domar 2 lobos |
| `/misiones agregar pescar 10 cualquiera` | Pescar 10 cosas |
| `/misiones agregar fabricar 1 minecraft:beacon dificil` | Fabricar un faro |
| `/misiones agregar encantar 3 cualquiera` | Encantar 3 cosas |
| `/misiones agregar pociones 2 minecraft:strength` | Preparar 2 pociones de fuerza |
| `/misiones agregar comer 5 minecraft:golden_carrot` | Comer 5 zanahorias doradas |
| `/misiones agregar fundir 32 minecraft:iron_ingot` | Sacar 32 lingotes de hierro del horno |
| `/misiones agregar comerciar 10 cualquiera` | Hacer 10 tratos |
| `/misiones agregar comerciar con 5 minecraft:piglin` | Hacer 5 trueques con piglins |
| `/misiones agregar equipar 1 minecraft:diamond_chestplate` | Traer puesto un peto de diamante |
| `/misiones agregar explorar estructura minecraft:ancient_city` | Pisar una ciudad antigua |
| `/misiones agregar explorar bioma minecraft:mushroom_fields` | Llegar a un bioma de champiñones |
| `/misiones agregar explorar dimension minecraft:the_nether` | Entrar al Nether |
| `/misiones agregar explorar altura abajo -50` | Bajar a la altura -50 (o `arriba 200`) |
| `/misiones agregar viajar elitros 5000` | Volar 5000 bloques con élitros |
| `/misiones agregar logros minecraft:story/mine_diamond` | Conseguir el logro "¡Diamantes!" |
| `/misiones agregar sobrevivir 10 dificil` | 10 días sin morir |
| `/misiones agregar nivel 30` | Llegar a nivel 30 |
| `/misiones agregar libre Construye un castillo` | Tú la marcas con `/misiones completar <número>` |
| `/misiones agregar plantilla wither` | Una misión del catálogo por su nombre |

Formas de **viajar**: `caminar`, `correr`, `nadar`, `bucear`, `barco`, `caballo`, `cerdo`, `vagoneta`, `elitros`, `lanzador`, `trepar`, `agachado`, `saltar` (cuenta saltos).

**Etiquetas:** con `#` vale cualquier cosa de un grupo. Por ejemplo, `#minecraft:logs` es cualquier tronco y `#minecraft:swords` cualquier espada.

### Agregar más cosas o detalles a una misión
(el `1` es el número de la misión en tu lista)

| Comando | Qué hace |
|---|---|
| `/misiones objetivo 1 16 minecraft:gold_ingot` | La misión #1 ahora también pide 16 lingotes de oro |
| `/misiones encantamiento 1 minecraft:sharpness 5` | La última cosa de la misión #1 debe tener Filo V (o más) |
| `/misiones adorno 1` | La última cosa de la misión #1 debe tener un adorno de herrería |

Ejemplo, "ponte unas botas de diamante con Caída de pluma IV":
```
/misiones agregar equipar 1 minecraft:diamond_boots dificil
/misiones encantamiento 1 minecraft:feather_falling 4
```

## 🎁 Recompensas (opcionales)

Las misiones no dan nada de fábrica, pero se puede agregar una recompensa:

| Comando | Qué hace |
|---|---|
| `/misiones recompensa 3 5 minecraft:diamond` | Al cumplir la misión #3 recibes 5 diamantes |
| `/misiones recompensa 3 xp 10` | Al cumplir la misión #3 recibes 10 niveles de experiencia |

## 🧹 Otros comandos

| Comando | Qué hace |
|---|---|
| `/misiones borrar 3` | Borra la misión #3 |
| `/misiones completar 3` | Marca como cumplida la misión #3 |
| `/misiones limpiar hechas` | Quita de la lista las misiones ya cumplidas |
| `/misiones limpiar` | Borra todas tus misiones (y las que estaban en espera) |
| `/misiones catalogo` | Cuántas misiones hay de cada tipo |
| `/misiones catalogo minar` | Ver todas las misiones de un tipo |
| `/misiones recargar` | Volver a leer los archivos después de editarlos |

---

## 📄 Agregar tus propias misiones al catálogo

Las misiones al azar salen del archivo **`config/misiones/misiones.json`**, dentro de tu carpeta `.minecraft`. Se crea solo la primera vez que abres el juego con el mod. Si tenías la versión 1, el archivo viejo se guarda como `misiones_v1_respaldo.json`.

**Pasos:**
1. Cierra Minecraft, o déjalo abierto y usa `/misiones recargar` al terminar.
2. Abre `misiones.json` con el **Bloc de notas**.
3. Cada misión ocupa **una línea**. Copia una línea parecida a la que quieres, pégala abajo y cámbiale los datos.
4. Fíjate en que **todas las líneas terminen en coma**, menos la última de la lista.
5. Guarda y usa `/misiones recargar`. Si escribiste algo mal, el juego avisa en el registro y usa las misiones de fábrica.

Una línea se ve así:
```json
{"id": "manzanas", "tipo": "conseguir", "dificultad": "facil", "objetivos": [{"objetivo": "minecraft:apple", "cantidad": 16}]},
```

| Campo | Qué poner |
|---|---|
| `id` | Un nombre único, sin espacios (ej. `mi_mision_1`) |
| `tipo` | `conseguir`, `cofre`, `minar`, `cosechar`, `matar`, `criar`, `domesticar`, `pescar`, `fabricar`, `encantar`, `pociones`, `comer`, `fundir`, `comerciar`, `equipar`, `explorar`, `viajar`, `logros`, `sobrevivir`, `nivel` o `libre` |
| `dificultad` | `facil`, `media` o `dificil` |
| `objetivos` | Lo que pide. Puedes poner varios separados por coma |
| `texto` | (opcional) Un texto extra; en las misiones `libre` es el texto de la misión |
| `recompensas` | (opcional) Ej. `"recompensas": [{"objeto": "minecraft:diamond", "cantidad": 5}]` o `{"objeto": "xp", "cantidad": 10}` |

**Qué va en `objetivo` según el tipo:**

| Tipo | Ejemplos de `objetivo` |
|---|---|
| conseguir, cofre, fabricar, pescar, comer, fundir, encantar, equipar | `minecraft:apple`, `#minecraft:logs`, `*` (lo que sea) |
| minar, cosechar | Un bloque: `minecraft:diamond_ore`, `#minecraft:iron_ores`, `minecraft:wheat` |
| matar, criar, domesticar | Una criatura: `minecraft:zombie`, `minecraft:wolf` |
| pociones | `minecraft:strength`, `minecraft:strong_healing`, `minecraft:long_fire_resistance`, `*` |
| comerciar | `*` (cualquier trato), `minecraft:villager`, `minecraft:wandering_trader`, `minecraft:piglin`, o lo que recibes (`minecraft:enchanted_book`) |
| explorar | `dimension:minecraft:the_end`, `estructura:minecraft:stronghold`, `estructura:#minecraft:village`, `bioma:minecraft:desert`, `altura:<=0`, `altura:>=200` |
| viajar | `caminar`, `elitros`, `barco`… y en `cantidad` los bloques |
| logros | `minecraft:story/mine_diamond`, `minecraft:end/kill_dragon` |
| sobrevivir, nivel | `dias` o `nivel`, y en `cantidad` el número |

**Detalles entre corchetes** (para equipar, encantar, conseguir, cofre…):
- `minecraft:diamond_sword[sharpness=5]`: espada de diamante con Filo V o más.
- `minecraft:netherite_pickaxe[efficiency=5,fortune=3]`: varios encantamientos a la vez.
- `#minecraft:chest_armor[adorno=*]`: cualquier peto con adorno de herrería.
- `minecraft:potion[pocion=minecraft:strength]`: una poción de fuerza (por ejemplo, para tenerla en el inventario).

Ejemplo completo: "ponte toda la armadura de diamante con Protección III":
```json
{"id": "mi_armadura", "tipo": "equipar", "dificultad": "dificil", "objetivos": [{"objetivo": "minecraft:diamond_helmet[protection=3]", "cantidad": 1}, {"objetivo": "minecraft:diamond_chestplate[protection=3]", "cantidad": 1}, {"objetivo": "minecraft:diamond_leggings[protection=3]", "cantidad": 1}, {"objetivo": "minecraft:diamond_boots[protection=3]", "cantidad": 1}]},
```

### Crear tu propia cadena

Agrega a cada misión de la cadena `"cadena": "nombre"`, y en cada paso (menos el último) pon `"siguiente": "id del paso que sigue"`. Las misiones de cadena no salen al azar; se empiezan con `/misiones cadena nombre`.

```json
{"id": "pesca_1", "tipo": "pescar", "dificultad": "facil", "cadena": "pesca", "siguiente": "pesca_2", "objetivos": [{"objetivo": "*", "cantidad": 10}]},
{"id": "pesca_2", "tipo": "comer", "dificultad": "facil", "cadena": "pesca", "siguiente": "pesca_3", "objetivos": [{"objetivo": "minecraft:cooked_cod", "cantidad": 5}]},
{"id": "pesca_3", "tipo": "pescar", "dificultad": "media", "cadena": "pesca", "objetivos": [{"objetivo": "minecraft:enchanted_book", "cantidad": 1}]},
```

> 💡 Para volver a las misiones de fábrica, borra `misiones.json` y abre el juego (o usa `/misiones recargar`): se crea de nuevo.

Las misiones de cada jugador se guardan en el mundo; cada partida tiene las suyas.
