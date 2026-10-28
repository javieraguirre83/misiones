package misiones.modelo;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;

/** A reward: an item ("minecraft:diamond" x5) or experience levels ("xp" x10). */
public class Recompensa {
    public String objeto;
    public int cantidad;

    public Recompensa(String objeto, int cantidad) {
        this.objeto = objeto;
        this.cantidad = Math.max(1, cantidad);
    }

    public boolean esXp() {
        return "xp".equalsIgnoreCase(objeto) || "experiencia".equalsIgnoreCase(objeto);
    }

    public CompoundTag guardar() {
        CompoundTag tag = new CompoundTag();
        tag.putString("objeto", objeto);
        tag.putInt("cantidad", cantidad);
        return tag;
    }

    public static Recompensa cargar(CompoundTag tag) {
        return new Recompensa(tag.getStringOr("objeto", "xp"), tag.getIntOr("cantidad", 1));
    }

    public static Recompensa deJson(JsonObject json) {
        return new Recompensa(json.has("objeto") ? json.get("objeto").getAsString() : "xp",
                json.has("cantidad") ? json.get("cantidad").getAsInt() : 1);
    }

    public JsonObject aJson() {
        JsonObject json = new JsonObject();
        json.addProperty("objeto", objeto);
        json.addProperty("cantidad", cantidad);
        return json;
    }
}
