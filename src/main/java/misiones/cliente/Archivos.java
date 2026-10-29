package misiones.cliente;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import misiones.modelo.Mision;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Exported mission lists: config/misiones/exportadas/*.json (to edit or share). */
public final class Archivos {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static final Path CARPETA = Path.of("config", "misiones", "exportadas");

    /** Turns a typed name into a safe file name ("Mi lista!" -> "Mi_lista"). */
    public static String nombreSeguro(String nombre) {
        String s = nombre.trim().replaceAll("[^A-Za-z0-9áéíóúñÁÉÍÓÚÑ_-]+", "_").replaceAll("^_+|_+$", "");
        return s.isEmpty() ? "mis_misiones" : s;
    }

    /** Saves the missions; with "avance" the progress is kept, otherwise they start from 0. Returns the file. */
    public static Path exportar(List<Mision> misiones, String nombre, boolean avance) throws Exception {
        Files.createDirectories(CARPETA);
        Path archivo = CARPETA.resolve(nombreSeguro(nombre) + ".json");
        JsonObject json = new JsonObject();
        json.addProperty("_ayuda", "Lista de misiones exportada. Se puede editar (mismo formato que misiones.json) "
                + "y cargar con M -> Mis misiones -> Importar.");
        json.addProperty("conAvance", avance);
        JsonArray lista = new JsonArray();
        for (Mision m : misiones) lista.add(m.aJson(avance));
        json.add("misiones", lista);
        try (Writer w = Files.newBufferedWriter(archivo, StandardCharsets.UTF_8)) {
            GSON.toJson(json, w);
        }
        return archivo;
    }

    /** The exported files, newest first. */
    public static List<Path> archivos() {
        List<Path> r = new ArrayList<>();
        if (!Files.isDirectory(CARPETA)) return r;
        try (Stream<Path> s = Files.list(CARPETA)) {
            s.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".json")).forEach(r::add);
        } catch (Exception ignored) {
        }
        r.sort((a, b) -> {
            try {
                return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
            } catch (Exception e) {
                return 0;
            }
        });
        return r;
    }

    /** Reads the missions of a file (skips the ones with mistakes). */
    public static List<Mision> leer(Path archivo) throws Exception {
        List<Mision> r = new ArrayList<>();
        try (Reader reader = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            JsonElement raiz = GSON.fromJson(reader, JsonElement.class);
            JsonArray lista = raiz != null && raiz.isJsonObject() && raiz.getAsJsonObject().has("misiones")
                    ? raiz.getAsJsonObject().getAsJsonArray("misiones")
                    : raiz != null && raiz.isJsonArray() ? raiz.getAsJsonArray() : new JsonArray();
            for (JsonElement e : lista) {
                if (!e.isJsonObject()) continue;
                try {
                    Mision m = Mision.deJson(e.getAsJsonObject());
                    if (m != null && !m.objetivos.isEmpty()) r.add(m);
                } catch (Exception ignored) {
                }
            }
        }
        return r;
    }

    private Archivos() {}
}
