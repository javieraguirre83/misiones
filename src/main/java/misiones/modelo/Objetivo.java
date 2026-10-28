package misiones.modelo;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;

/**
 * One thing a mission asks for.
 * objetivo: an item/mob id ("minecraft:apple"), a tag ("#minecraft:logs"),
 *           or for EXPLORAR: "dimension:minecraft:the_nether", "estructura:minecraft:village_plains",
 *           "bioma:minecraft:desert", "altura:<=0" / "altura:>=200".
 */
public class Objetivo {
    public String objetivo;
    public int cantidad;
    public int progreso = 0;

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
        return o;
    }

    public CompoundTag guardar() {
        CompoundTag tag = new CompoundTag();
        tag.putString("objetivo", objetivo);
        tag.putInt("cantidad", cantidad);
        tag.putInt("progreso", progreso);
        return tag;
    }

    public static Objetivo cargar(CompoundTag tag) {
        Objetivo o = new Objetivo(tag.getStringOr("objetivo", ""), tag.getIntOr("cantidad", 1));
        o.progreso = tag.getIntOr("progreso", 0);
        return o;
    }

    public static Objetivo deJson(JsonObject json) {
        String id = json.has("objetivo") ? json.get("objetivo").getAsString() : "";
        int cantidad = json.has("cantidad") ? json.get("cantidad").getAsInt() : 1;
        return new Objetivo(id, cantidad);
    }

    public JsonObject aJson() {
        JsonObject json = new JsonObject();
        json.addProperty("objetivo", objetivo);
        json.addProperty("cantidad", cantidad);
        return json;
    }
}
