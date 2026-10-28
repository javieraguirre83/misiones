package misiones.modelo;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/** A mission (also used as a template inside misiones.json). */
public class Mision {
    public String plantilla = "";
    public Tipo tipo = Tipo.LIBRE;
    public Dificultad dificultad = Dificultad.MEDIA;
    /** Optional custom text. If empty, the text is built from the objectives. */
    public String texto = "";
    public final List<Objetivo> objetivos = new ArrayList<>();
    public final List<Recompensa> recompensas = new ArrayList<>();
    public boolean completada = false;
    /** Day the survive count started (for SOBREVIVIR). */
    public long diaInicio = -1;
    /** Chain name (empty = not part of a chain). */
    public String cadena = "";
    /** Template id that unlocks when this one is completed (chains). */
    public String siguiente = "";
    /** It was the mission of the day. */
    public boolean diaria = false;

    public boolean todosCumplidos() {
        if (objetivos.isEmpty()) return false;
        for (Objetivo o : objetivos) {
            if (!o.cumplido()) return false;
        }
        return true;
    }

    public Mision copia() {
        Mision m = new Mision();
        m.plantilla = plantilla;
        m.tipo = tipo;
        m.dificultad = dificultad;
        m.texto = texto;
        objetivos.forEach(o -> m.objetivos.add(o.copia()));
        recompensas.forEach(r -> m.recompensas.add(new Recompensa(r.objeto, r.cantidad)));
        m.completada = completada;
        m.diaInicio = diaInicio;
        m.cadena = cadena;
        m.siguiente = siguiente;
        m.diaria = diaria;
        return m;
    }

    // ---------- save / load in the world ----------

    public CompoundTag guardar() {
        CompoundTag tag = new CompoundTag();
        tag.putString("plantilla", plantilla);
        tag.putString("tipo", tipo.id);
        tag.putString("dificultad", dificultad.id);
        tag.putString("texto", texto);
        tag.putBoolean("completada", completada);
        tag.putLong("diaInicio", diaInicio);
        tag.putString("cadena", cadena);
        tag.putString("siguiente", siguiente);
        tag.putBoolean("diaria", diaria);
        ListTag objs = new ListTag();
        objetivos.forEach(o -> objs.add(o.guardar()));
        tag.put("objetivos", objs);
        ListTag recs = new ListTag();
        recompensas.forEach(r -> recs.add(r.guardar()));
        tag.put("recompensas", recs);
        return tag;
    }

    public static Mision cargar(CompoundTag tag) {
        Mision m = new Mision();
        m.plantilla = tag.getStringOr("plantilla", "");
        Tipo tipo = Tipo.leer(tag.getStringOr("tipo", "libre"));
        m.tipo = tipo == null ? Tipo.LIBRE : tipo;
        m.dificultad = Dificultad.leer(tag.getStringOr("dificultad", "media"), Dificultad.MEDIA);
        m.texto = tag.getStringOr("texto", "");
        m.completada = tag.getBooleanOr("completada", false);
        m.diaInicio = tag.getLongOr("diaInicio", -1L);
        m.cadena = tag.getStringOr("cadena", "");
        m.siguiente = tag.getStringOr("siguiente", "");
        m.diaria = tag.getBooleanOr("diaria", false);
        for (Tag t : tag.getListOrEmpty("objetivos")) {
            if (t instanceof CompoundTag c) m.objetivos.add(Objetivo.cargar(c));
        }
        for (Tag t : tag.getListOrEmpty("recompensas")) {
            if (t instanceof CompoundTag c) m.recompensas.add(Recompensa.cargar(c));
        }
        return m;
    }

    // ---------- templates file (misiones.json) ----------

    public static Mision deJson(JsonObject json) {
        Mision m = new Mision();
        m.plantilla = json.has("id") ? json.get("id").getAsString() : "";
        Tipo tipo = Tipo.leer(json.has("tipo") ? json.get("tipo").getAsString() : null);
        if (tipo == null) return null;
        m.tipo = tipo;
        m.dificultad = Dificultad.leer(json.has("dificultad") ? json.get("dificultad").getAsString() : null, Dificultad.MEDIA);
        m.texto = json.has("texto") ? json.get("texto").getAsString() : "";
        m.cadena = json.has("cadena") ? json.get("cadena").getAsString() : "";
        m.siguiente = json.has("siguiente") ? json.get("siguiente").getAsString() : "";
        if (json.has("objetivos") && json.get("objetivos").isJsonArray()) {
            for (JsonElement e : json.getAsJsonArray("objetivos")) {
                if (e.isJsonObject()) m.objetivos.add(Objetivo.deJson(e.getAsJsonObject()));
            }
        }
        if (json.has("recompensas") && json.get("recompensas").isJsonArray()) {
            for (JsonElement e : json.getAsJsonArray("recompensas")) {
                if (e.isJsonObject()) m.recompensas.add(Recompensa.deJson(e.getAsJsonObject()));
            }
        }
        if (m.tipo == Tipo.LIBRE && m.objetivos.isEmpty()) m.objetivos.add(new Objetivo("libre", 1));
        return m;
    }

    public JsonObject aJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", plantilla);
        json.addProperty("tipo", tipo.id);
        json.addProperty("dificultad", dificultad.id);
        if (!texto.isEmpty()) json.addProperty("texto", texto);
        if (!cadena.isEmpty()) json.addProperty("cadena", cadena);
        if (!siguiente.isEmpty()) json.addProperty("siguiente", siguiente);
        JsonArray objs = new JsonArray();
        objetivos.forEach(o -> objs.add(o.aJson()));
        json.add("objetivos", objs);
        if (!recompensas.isEmpty()) {
            JsonArray recs = new JsonArray();
            recompensas.forEach(r -> recs.add(r.aJson()));
            json.add("recompensas", recs);
        }
        return json;
    }
}
