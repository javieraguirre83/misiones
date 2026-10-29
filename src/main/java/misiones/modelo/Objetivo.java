package misiones.modelo;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * One thing a mission asks for.
 * objetivo: an item/mob id ("minecraft:apple"), a tag ("#minecraft:logs"),
 *           or for EXPLORAR: "dimension:minecraft:the_nether", "estructura:minecraft:village_plains",
 *           "bioma:minecraft:desert", "altura:<=0" / "altura:>=200".
 */
public class Objetivo {
    private static final Random RANDOM = new Random();

    public String objetivo;
    public int cantidad;
    public int progreso = 0;
    /** Starting value of a statistic (VIAJAR), -1 = not taken yet. */
    public int base = -1;
    /** Catalog range like "20-50" (empty = fixed amount). The real amount is chosen when the mission is given. */
    public String rango = "";
    /** Structures already visited (EXPLORAR with an amount: "visit 10 different villages"). */
    public final List<String> visitados = new ArrayList<>();

    public Objetivo(String objetivo, int cantidad) {
        this.objetivo = objetivo == null ? "" : objetivo;
        this.cantidad = Math.max(1, cantidad);
    }

    public boolean cumplido() {
        return progreso >= cantidad;
    }

    public Objetivo copia() {
        Objetivo o = new Objetivo(objetivo, cantidad);
        o.progreso = progreso;
        o.base = base;
        o.rango = rango;
        o.visitados.addAll(visitados);
        return o;
    }

    // ---------- ranges and variation ----------

    /** {min, max} of a range like "20-50", or null if it is not a valid range. */
    public static int[] leerRango(String texto) {
        if (texto == null) return null;
        String t = texto.trim();
        int guion = t.indexOf('-', 1);
        try {
            if (guion < 0) return null;
            int a = Integer.parseInt(t.substring(0, guion).trim());
            int b = Integer.parseInt(t.substring(guion + 1).trim());
            return new int[]{Math.max(1, Math.min(a, b)), Math.max(1, Math.max(a, b))};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Chooses the real amount: a range always picks a number inside it;
     * otherwise, if "variacion" (percent) is above 0, the amount moves up or down at random.
     * Amounts of 1 stay 1.
     */
    public void concretar(int variacion) {
        int[] r = leerRango(rango);
        if (r != null) {
            cantidad = r[0] + RANDOM.nextInt(r[1] - r[0] + 1);
        } else if (variacion > 0 && cantidad > 1) {
            int min = Math.max(1, (int) Math.round(cantidad * (1 - variacion / 100.0)));
            int max = Math.max(min, (int) Math.round(cantidad * (1 + variacion / 100.0)));
            cantidad = min + RANDOM.nextInt(max - min + 1);
        }
        rango = "";
        progreso = Math.min(progreso, cantidad);
    }

    // ---------- save / load ----------

    public CompoundTag guardar() {
        CompoundTag tag = new CompoundTag();
        tag.putString("objetivo", objetivo);
        tag.putInt("cantidad", cantidad);
        tag.putInt("progreso", progreso);
        tag.putInt("base", base);
        if (!rango.isEmpty()) tag.putString("rango", rango);
        if (!visitados.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String v : visitados) {
                if (sb.length() > 0) sb.append(';');
                sb.append(v);
            }
            tag.putString("visitados", sb.toString());
        }
        return tag;
    }

    public static Objetivo cargar(CompoundTag tag) {
        Objetivo o = new Objetivo(tag.getStringOr("objetivo", ""), tag.getIntOr("cantidad", 1));
        o.progreso = tag.getIntOr("progreso", 0);
        o.base = tag.getIntOr("base", -1);
        o.rango = tag.getStringOr("rango", "");
        String v = tag.getStringOr("visitados", "");
        if (!v.isEmpty()) {
            for (String parte : v.split(";")) {
                if (!parte.isBlank()) o.visitados.add(parte.trim());
            }
        }
        return o;
    }

    /** "cantidad" can be a number (16) or a range ("20-50"). */
    public static Objetivo deJson(JsonObject json) {
        String id = json.has("objetivo") ? json.get("objetivo").getAsString() : "";
        int cantidad = 1;
        String rango = "";
        if (json.has("cantidad")) {
            JsonElement c = json.get("cantidad");
            String texto = c.getAsString().trim();
            int[] r = leerRango(texto);
            if (r != null) {
                rango = r[0] + "-" + r[1];
                cantidad = r[1];
            } else {
                try { cantidad = Integer.parseInt(texto); } catch (NumberFormatException e) { cantidad = 1; }
            }
        }
        Objetivo o = new Objetivo(id, cantidad);
        o.rango = rango;
        if (json.has("progreso")) o.progreso = Math.max(0, Math.min(o.cantidad, json.get("progreso").getAsInt()));
        return o;
    }

    public JsonObject aJson() {
        return aJson(false);
    }

    /** With "avance" the progress is saved too (exported missions). */
    public JsonObject aJson(boolean avance) {
        JsonObject json = new JsonObject();
        json.addProperty("objetivo", objetivo);
        if (!rango.isEmpty()) json.addProperty("cantidad", rango);
        else json.addProperty("cantidad", cantidad);
        if (avance && progreso > 0) json.addProperty("progreso", progreso);
        return json;
    }
}
