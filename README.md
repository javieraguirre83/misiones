# Misiones para Minecraft 26.2 (Forge)

Generador de misiones para darle un **objetivo** a cada partida: desde "consigue 16 manzanas" hasta "vence al Wither".

## ⬇️ Descargar

**[Descargar el mod (.jar)](https://github.com/javieraguirre83/misiones/releases/download/ultima/Misiones-1.0.0-forge-26.2.jar)**

1. Descarga el archivo `.jar`.
2. Presiona **Windows + R**, escribe `%appdata%\.minecraft\mods` y dale Enter.
3. Pega ahí el `.jar` y abre Minecraft con el perfil de **Forge 26.2**.

Los textos salen en **español o inglés** según el idioma de tu Minecraft. El comando funciona como `/misiones` o `/missions`.

---

## 🎲 Generar misiones al azar

| Comando | Qué hace |
|---|---|
| `/misiones generar 10` | 10 misiones al azar (de cualquier dificultad) |
| `/misiones generar 5 3 2` | 5 fáciles 🟢, 3 medias 🟡 y 2 difíciles 🔴 |
| `/misiones` | Ver tu lista y el avance de cada una |

## ✍️ Agregar misiones a mano

Al final puedes poner la dificultad: `facil`, `media` o `dificil` (si no la pones, es media). Presiona **Tab** para ver sugerencias de objetos, monstruos, estructuras y biomas.

| Tipo | Ejemplo | Cómo se cumple |
|---|---|---|
| 🍎 Conseguir | `/misiones agregar conseguir 100 minecraft:apple facil` | Tener 100 manzanas **en tu inventario** |
| 📦 Cofre | `/misiones agregar cofre 64 minecraft:oak_log` | Cerrar un cofre que tenga 64 troncos de roble |
| ⚔️ Matar | `/misiones agregar matar 20 minecraft:zombie` | Matar 20 zombis |
| 🔨 Fabricar | `/misiones agregar fabricar 1 minecraft:beacon dificil` | Fabricar un faro |
| 🧭 Dimensión | `/misiones agregar explorar dimension minecraft:the_nether` | Entrar al Nether |
| 🧭 Estructura | `/misiones agregar explorar estructura minecraft:ancient_city` | Pisar una ciudad antigua |
| 🧭 Bioma | `/misiones agregar explorar bioma minecraft:mushroom_fields` | Llegar a un bioma de champiñones |
| 🧭 Altura | `/misiones agregar explorar altura abajo -50` | Bajar a la altura -50 (o `arriba 200`) |
| ⏳ Sobrevivir | `/misiones agregar sobrevivir 10 dificil` | 10 días sin morir (si mueres, vuelve a 0) |
| ⭐ Nivel | `/misiones agregar nivel 30` | Llegar a nivel 30 de experiencia |
| ✍️ Libre | `/misiones agregar libre Construye un castillo` | Tú decides cuándo está lista: `/misiones completar <número>` |
| 📜 Del catálogo | `/misiones agregar plantilla wither` | Agrega una misión del catálogo por su nombre |

También sirven **etiquetas** (cualquier objeto de un grupo) con `#`, por ejemplo `#minecraft:logs` (cualquier tronco) o `#minecraft:wool` (cualquier lana).

### Pedir varias cosas en una misión
Agrega más objetivos a una misión de conseguir, cofre, matar o fabricar:
```
/misiones agregar cofre 32 minecraft:iron_ingot
/misiones objetivo 1 16 minecraft:gold_ingot
/misiones objetivo 1 5 minecraft:diamond
```
(el `1` es el número de la misión en tu lista)

## 🎁 Recompensas (opcionales)

| Comando | Qué hace |
|---|---|
| `/misiones recompensa 3 5 minecraft:diamond` | Al cumplir la misión #3 recibes 5 diamantes |
| `/misiones recompensa 3 xp 10` | Al cumplir la misión #3 recibes 10 niveles de experiencia |

## 🧹 Otros comandos

| Comando | Qué hace |
|---|---|
| `/misiones borrar 3` | Borra la misión #3 |
| `/misiones limpiar` | Borra todas tus misiones |
| `/misiones completar 3` | Marca como cumplida la misión #3 |
| `/misiones catalogo` | Ver todas las misiones posibles del azar |
| `/misiones recargar` | Volver a leer el archivo del catálogo (después de editarlo) |

---

## 📄 Personalizar el catálogo

Las misiones al azar salen del archivo **`config/misiones/misiones.json`** (dentro de tu carpeta `.minecraft`). Se crea solo la primera vez, con más de 50 misiones de ejemplo. Puedes cambiar cantidades, borrar misiones o agregar las tuyas. Después usa `/misiones recargar`.

Ejemplo de una misión en el archivo:
```json
{
  "id": "cofre_minerales",
  "tipo": "cofre",
  "dificultad": "dificil",
  "objetivos": [
    { "objetivo": "minecraft:iron_block", "cantidad": 16 },
    { "objetivo": "minecraft:diamond_block", "cantidad": 4 }
  ],
  "recompensas": [
    { "objeto": "minecraft:netherite_ingot", "cantidad": 1 },
    { "objeto": "xp", "cantidad": 10 }
  ]
}
```

- **tipo:** `conseguir`, `cofre`, `matar`, `fabricar`, `explorar`, `sobrevivir`, `nivel`, `libre`
- **dificultad:** `facil`, `media`, `dificil`
- **explorar:** `dimension:minecraft:the_nether`, `estructura:minecraft:village_plains` (o `estructura:#minecraft:village`), `bioma:minecraft:desert`, `altura:<=0`, `altura:>=200`
- **sobrevivir / nivel:** pon el número de días o el nivel en `cantidad`
- **libre:** agrega `"texto": "lo que quieras"`
- **recompensas:** cualquier objeto, o `"xp"` para niveles de experiencia (opcional)

Las misiones se guardan en cada mundo; cada partida tiene las suyas.
