package misiones.modelo;

import net.minecraft.ChatFormatting;

import java.util.Locale;

public enum Dificultad {
    FACIL("facil", ChatFormatting.GREEN, "🟢"),
    MEDIA("media", ChatFormatting.YELLOW, "🟡"),
    DIFICIL("dificil", ChatFormatting.RED, "🔴");

    public final String id;
    public final ChatFormatting color;
    public final String icono;

    Dificultad(String id, ChatFormatting color, String icono) {
        this.id = id;
        this.color = color;
        this.icono = icono;
    }

    public String clave() {
        return "misiones.dificultad." + id;
    }

    /** Accepts Spanish or English names: facil/easy, media/medium/normal, dificil/hard. */
    public static Dificultad leer(String texto, Dificultad porDefecto) {
        if (texto == null) return porDefecto;
        return switch (texto.toLowerCase(Locale.ROOT).replace("á", "a").replace("í", "i")) {
            case "facil", "easy", "f" -> FACIL;
            case "media", "medio", "medium", "normal", "m" -> MEDIA;
            case "dificil", "hard", "d" -> DIFICIL;
            default -> porDefecto;
        };
    }
}
