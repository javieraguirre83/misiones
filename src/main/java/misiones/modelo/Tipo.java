package misiones.modelo;

import java.util.Locale;

/** Kinds of mission. */
public enum Tipo {
    CONSEGUIR("conseguir", "🍎", true),    // have items in your inventory
    COFRE("cofre", "📦", true),            // put items in a chest
    MINAR("minar", "⛏", true),             // break blocks
    COSECHAR("cosechar", "🌾", true),      // harvest grown crops
    MATAR("matar", "⚔", true),             // kill mobs (bosses too)
    CRIAR("criar", "🐄", true),            // breed animals
    DOMESTICAR("domesticar", "🐺", true),  // tame animals
    PESCAR("pescar", "🎣", true),          // fish things
    FABRICAR("fabricar", "🔨", true),      // craft items
    ENCANTAR("encantar", "✨", true),      // enchant items (table or anvil)
    POCIONES("pociones", "🧪", true),      // brew potions
    COMER("comer", "🍗", true),            // eat food
    FUNDIR("fundir", "🔥", true),          // smelt or cook in a furnace
    COMERCIAR("comerciar", "🤝", true),    // trade with villagers / barter with piglins
    EQUIPAR("equipar", "🛡", true),        // wear armor / carry weapons and tools (with enchantments)
    EXPLORAR("explorar", "🧭", true),      // reach a dimension, structure, biome or height
    VIAJAR("viajar", "🗺", true),          // travel a distance (walking, boat, elytra...)
    LOGROS("logros", "🏆", true),          // get Minecraft advancements
    SOBREVIVIR("sobrevivir", "⏳", false), // survive N days without dying
    NIVEL("nivel", "⭐", false),           // reach an XP level
    LIBRE("libre", "✍", false);            // free text, completed by hand

    public final String id;
    public final String icono;
    /** Can have several objectives (e.g. 10 iron + 5 gold). */
    public final boolean multiple;

    Tipo(String id, String icono, boolean multiple) {
        this.id = id;
        this.icono = icono;
        this.multiple = multiple;
    }

    public String clave() {
        return "misiones.tipo." + id;
    }

    public static Tipo leer(String texto) {
        if (texto == null) return null;
        String t = texto.toLowerCase(Locale.ROOT).replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u");
        for (Tipo tipo : values()) {
            if (tipo.id.equals(t)) return tipo;
        }
        return switch (t) {
            case "obtain", "get", "collect" -> CONSEGUIR;
            case "chest", "store" -> COFRE;
            case "mine", "dig" -> MINAR;
            case "harvest", "farm" -> COSECHAR;
            case "kill" -> MATAR;
            case "breed" -> CRIAR;
            case "tame", "domar" -> DOMESTICAR;
            case "fish" -> PESCAR;
            case "craft" -> FABRICAR;
            case "enchant" -> ENCANTAR;
            case "brew", "potions", "pocion" -> POCIONES;
            case "eat" -> COMER;
            case "smelt", "cook", "cocinar" -> FUNDIR;
            case "trade", "barter", "comercio" -> COMERCIAR;
            case "equip", "wear" -> EQUIPAR;
            case "explore" -> EXPLORAR;
            case "travel" -> VIAJAR;
            case "advancement", "advancements", "logro" -> LOGROS;
            case "survive" -> SOBREVIVIR;
            case "level" -> NIVEL;
            case "free", "custom" -> LIBRE;
            default -> null;
        };
    }
}
