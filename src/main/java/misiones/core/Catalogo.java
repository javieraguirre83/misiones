package misiones.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import misiones.modelo.Dificultad;
import misiones.modelo.Mision;
import misiones.modelo.Objetivo;
import misiones.modelo.Recompensa;
import misiones.modelo.Tipo;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** The list of possible missions: config/misiones/misiones.json (editable by the player). */
public final class Catalogo {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static final Path ARCHIVO = Path.of("config", "misiones", "misiones.json");

    private static final List<Mision> PLANTILLAS = new ArrayList<>();
    private static final Random RANDOM = new Random();

    public static List<Mision> plantillas() {
        return Collections.unmodifiableList(PLANTILLAS);
    }

    public static Mision buscar(String id) {
        for (Mision m : PLANTILLAS) {
            if (m.plantilla.equalsIgnoreCase(id)) return m;
        }
        return null;
    }

    /** Random missions of one difficulty (without repeating if possible). */
    public static List<Mision> alAzar(Dificultad dificultad, int cantidad, List<String> evitar) {
        List<Mision> candidatas = new ArrayList<>();
        for (Mision m : PLANTILLAS) {
            if (m.dificultad == dificultad) candidatas.add(m);
        }
        return elegir(candidatas, cantidad, evitar);
    }

    /** Random missions of any difficulty. */
    public static List<Mision> alAzar(int cantidad, List<String> evitar) {
        return elegir(new ArrayList<>(PLANTILLAS), cantidad, evitar);
    }

    private static List<Mision> elegir(List<Mision> candidatas, int cantidad, List<String> evitar) {
        List<Mision> resultado = new ArrayList<>();
        if (candidatas.isEmpty()) return resultado;
        List<Mision> frescas = new ArrayList<>(candidatas);
        frescas.removeIf(m -> evitar.contains(m.plantilla));
        Collections.shuffle(frescas, RANDOM);
        for (Mision m : frescas) {
            if (resultado.size() >= cantidad) break;
            resultado.add(m.copia());
        }
        // Not enough new ones: allow repeats
        while (resultado.size() < cantidad) {
            resultado.add(candidatas.get(RANDOM.nextInt(candidatas.size())).copia());
        }
        return resultado;
    }

    /** Loads the file, creating it with the default missions the first time. */
    public static int cargar() {
        PLANTILLAS.clear();
        try {
            if (!Files.exists(ARCHIVO)) {
                guardarPorDefecto();
            }
            try (Reader reader = Files.newBufferedReader(ARCHIVO, StandardCharsets.UTF_8)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                if (json != null && json.has("misiones")) {
                    for (JsonElement e : json.getAsJsonArray("misiones")) {
                        if (!e.isJsonObject()) continue;
                        Mision m = Mision.deJson(e.getAsJsonObject());
                        if (m != null && !m.objetivos.isEmpty()) PLANTILLAS.add(m);
                    }
                }
            }
        } catch (Exception ex) {
            Misiones.LOGGER.error("No se pudo leer " + ARCHIVO + " - usando las misiones de ejemplo", ex);
            PLANTILLAS.clear();
            PLANTILLAS.addAll(porDefecto());
        }
        return PLANTILLAS.size();
    }

    private static void guardarPorDefecto() throws Exception {
        Files.createDirectories(ARCHIVO.getParent());
        JsonObject json = new JsonObject();
        json.addProperty("_ayuda", "Tipos: conseguir, cofre, matar, fabricar, explorar, sobrevivir, nivel, libre. "
                + "Dificultad: facil, media, dificil. Explorar: dimension:<id>, estructura:<id o #tag>, bioma:<id o #tag>, altura:<=N o altura:>=N. "
                + "Objetos y monstruos aceptan #tag (ej. #minecraft:logs). Recompensas: un objeto o \"xp\" (niveles).");
        JsonArray lista = new JsonArray();
        porDefecto().forEach(m -> lista.add(m.aJson()));
        json.add("misiones", lista);
        try (Writer writer = Files.newBufferedWriter(ARCHIVO, StandardCharsets.UTF_8)) {
            GSON.toJson(json, writer);
        }
    }

    // ---------- default missions ----------

    private static Mision m(String id, Tipo tipo, Dificultad d, Object... objetivos) {
        Mision m = new Mision();
        m.plantilla = id;
        m.tipo = tipo;
        m.dificultad = d;
        for (int i = 0; i + 1 < objetivos.length; i += 2) {
            m.objetivos.add(new Objetivo((String) objetivos[i], (Integer) objetivos[i + 1]));
        }
        return m;
    }

    private static Mision libre(String id, Dificultad d, String texto) {
        Mision m = m(id, Tipo.LIBRE, d, "libre", 1);
        m.texto = texto;
        return m;
    }

    private static Mision premio(Mision m, String objeto, int cantidad) {
        m.recompensas.add(new Recompensa(objeto, cantidad));
        return m;
    }

    public static List<Mision> porDefecto() {
        Dificultad F = Dificultad.FACIL, M = Dificultad.MEDIA, D = Dificultad.DIFICIL;
        List<Mision> l = new ArrayList<>();

        // ---- FÁCILES ----
        l.add(m("manzanas", Tipo.CONSEGUIR, F, "minecraft:apple", 16));
        l.add(m("troncos", Tipo.CONSEGUIR, F, "#minecraft:logs", 64));
        l.add(m("carbon", Tipo.CONSEGUIR, F, "minecraft:coal", 32));
        l.add(m("trigo", Tipo.CONSEGUIR, F, "minecraft:wheat", 32));
        l.add(m("lana", Tipo.CONSEGUIR, F, "#minecraft:wool", 16));
        l.add(m("piedra_cofre", Tipo.COFRE, F, "minecraft:cobblestone", 128));
        l.add(m("zombis", Tipo.MATAR, F, "minecraft:zombie", 10));
        l.add(m("esqueletos", Tipo.MATAR, F, "minecraft:skeleton", 10));
        l.add(m("aranas", Tipo.MATAR, F, "minecraft:spider", 8));
        l.add(m("antorchas", Tipo.FABRICAR, F, "minecraft:torch", 64));
        l.add(m("pan", Tipo.FABRICAR, F, "minecraft:bread", 16));
        l.add(m("cama", Tipo.FABRICAR, F, "#minecraft:beds", 1));
        l.add(m("aldea", Tipo.EXPLORAR, F, "estructura:#minecraft:village", 1));
        l.add(m("sobrevivir_3", Tipo.SOBREVIVIR, F, "dias", 3));
        l.add(m("nivel_5", Tipo.NIVEL, F, "nivel", 5));

        // ---- MEDIAS ----
        l.add(m("hierro", Tipo.CONSEGUIR, M, "minecraft:iron_ingot", 32));
        l.add(m("oro", Tipo.CONSEGUIR, M, "minecraft:gold_ingot", 16));
        l.add(m("manzanas_100", Tipo.CONSEGUIR, M, "minecraft:apple", 100));
        l.add(m("lapis_redstone", Tipo.CONSEGUIR, M, "minecraft:lapis_lazuli", 32, "minecraft:redstone", 32));
        l.add(m("cofre_comida", Tipo.COFRE, M, "minecraft:cooked_beef", 32, "minecraft:bread", 32, "minecraft:baked_potato", 32));
        l.add(m("creepers", Tipo.MATAR, M, "minecraft:creeper", 15));
        l.add(m("endermans", Tipo.MATAR, M, "minecraft:enderman", 5));
        l.add(m("brujas", Tipo.MATAR, M, "minecraft:witch", 3));
        l.add(m("ahogados", Tipo.MATAR, M, "minecraft:drowned", 10));
        l.add(m("saqueadores", Tipo.MATAR, M, "minecraft:pillager", 10));
        l.add(m("armadura_hierro", Tipo.FABRICAR, M, "minecraft:iron_helmet", 1, "minecraft:iron_chestplate", 1,
                "minecraft:iron_leggings", 1, "minecraft:iron_boots", 1));
        l.add(m("mesa_encantar", Tipo.FABRICAR, M, "minecraft:enchanting_table", 1));
        l.add(m("nether", Tipo.EXPLORAR, M, "dimension:minecraft:the_nether", 1));
        l.add(m("desierto", Tipo.EXPLORAR, M, "bioma:minecraft:desert", 1));
        l.add(m("jungla", Tipo.EXPLORAR, M, "bioma:#minecraft:is_jungle", 1));
        l.add(m("capa_0", Tipo.EXPLORAR, M, "altura:<=0", 1));
        l.add(m("mina", Tipo.EXPLORAR, M, "estructura:minecraft:mineshaft", 1));
        l.add(m("sobrevivir_7", Tipo.SOBREVIVIR, M, "dias", 7));
        l.add(m("nivel_20", Tipo.NIVEL, M, "nivel", 20));
        l.add(libre("granja", M, "Construye una granja de cultivos de 9x9"));

        // ---- DIFÍCILES ----
        l.add(premio(m("diamantes", Tipo.CONSEGUIR, D, "minecraft:diamond", 32), "xp", 10));
        l.add(m("cofre_minerales", Tipo.COFRE, D, "minecraft:iron_block", 16, "minecraft:gold_block", 8, "minecraft:diamond_block", 4));
        l.add(m("netherita", Tipo.CONSEGUIR, D, "minecraft:netherite_ingot", 1));
        l.add(m("blazes", Tipo.MATAR, D, "minecraft:blaze", 20));
        l.add(m("esqueletos_wither", Tipo.MATAR, D, "minecraft:wither_skeleton", 15));
        l.add(m("ghasts", Tipo.MATAR, D, "minecraft:ghast", 5));
        l.add(m("guardian_anciano", Tipo.MATAR, D, "minecraft:elder_guardian", 1));
        l.add(m("devastador", Tipo.MATAR, D, "minecraft:ravager", 1));
        l.add(premio(m("warden", Tipo.MATAR, D, "minecraft:warden", 1), "minecraft:diamond", 8));
        l.add(premio(m("wither", Tipo.MATAR, D, "minecraft:wither", 1), "minecraft:netherite_ingot", 1));
        l.add(premio(m("dragon", Tipo.MATAR, D, "minecraft:ender_dragon", 1), "xp", 30));
        l.add(m("armadura_diamante", Tipo.FABRICAR, D, "minecraft:diamond_helmet", 1, "minecraft:diamond_chestplate", 1,
                "minecraft:diamond_leggings", 1, "minecraft:diamond_boots", 1));
        l.add(m("faro", Tipo.FABRICAR, D, "minecraft:beacon", 1));
        l.add(m("end", Tipo.EXPLORAR, D, "dimension:minecraft:the_end", 1));
        l.add(m("fortaleza", Tipo.EXPLORAR, D, "estructura:minecraft:fortress", 1));
        l.add(m("ciudad_antigua", Tipo.EXPLORAR, D, "estructura:minecraft:ancient_city", 1));
        l.add(m("mansion", Tipo.EXPLORAR, D, "estructura:minecraft:mansion", 1));
        l.add(m("champinones", Tipo.EXPLORAR, D, "bioma:minecraft:mushroom_fields", 1));
        l.add(m("sobrevivir_30", Tipo.SOBREVIVIR, D, "dias", 30));
        l.add(m("nivel_50", Tipo.NIVEL, D, "nivel", 50));
        l.add(libre("castillo", D, "Construye un castillo con 4 torres"));
        return l;
    }

    private Catalogo() {}
}
