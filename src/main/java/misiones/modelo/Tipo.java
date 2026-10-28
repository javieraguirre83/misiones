package misiones.modelo;

import java.util.Locale;

/** Kinds of mission. */
public enum Tipo {
    CONSEGUIR("conseguir", "🍎", true),   // have items in your inventory
    COFRE("cofre", "📦", true),           // put items in a chest
    MATAR("matar", "⚔", true),            // kill mobs (bosses too)
    FABRICAR("fabricar", "🔨", true),     // craft items
    EXPLORAR("explorar", "🧭", false),    // reach a dimension, structure, biome or height
    SOBREVIVIR("sobrevivir", "⏳", false), // survive N days without dying
    NIVEL("nivel", "⭐", false),          // reach an XP level
    LIBRE("libre", "✍", false);           // free text, completed by hand

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
        String t = texto.toLowerCase(Locale.ROOT);
        for (Tipo tipo : values()) {
            if (tipo.id.equals(t)) return tipo;
        }
        return switch (t) {
            case "obtain", "get", "collect" -> CONSEGUIR;
            case "chest", "store" -> COFRE;
            case "kill" -> MATAR;
            case "craft" -> FABRICAR;
            case "explore" -> EXPLORAR;
            case "survive" -> SOBREVIVIR;
            case "level" -> NIVEL;
            case "free", "custom" -> LIBRE;
            default -> null;
        };
    }
}
