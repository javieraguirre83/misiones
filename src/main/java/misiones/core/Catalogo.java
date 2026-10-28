package misiones.core;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import misiones.modelo.Dificultad;
import misiones.modelo.Mision;
import misiones.modelo.Tipo;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

/** The list of possible missions: config/misiones/misiones.json (editable by the player). */
public final class Catalogo {
    private static final Gson GSON = new Gson();
    public static final Path ARCHIVO = Path.of("config", "misiones", "misiones.json");
    private static final String RECURSO = "/misiones/catalogo.json";
    private static final int VERSION = 2;

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

    /** Names of the mission chains. */
    public static Set<String> cadenas() {
        Set<String> nombres = new LinkedHashSet<>();
        for (Mision m : PLANTILLAS) if (!m.cadena.isEmpty()) nombres.add(m.cadena);
        return nombres;
    }

    /** First mission of a chain (the one no other mission points to). */
    public static Mision inicioCadena(String cadena) {
        Set<String> apuntadas = new LinkedHashSet<>();
        for (Mision m : PLANTILLAS) if (!m.siguiente.isEmpty()) apuntadas.add(m.siguiente.toLowerCase());
        for (Mision m : PLANTILLAS) {
            if (m.cadena.equalsIgnoreCase(cadena) && !apuntadas.contains(m.plantilla.toLowerCase())) return m;
        }
        return null;
    }

    /** Missions that can come out at random (chain missions only come through their chain). */
    private static boolean sorteable(Mision m) {
        return m.cadena.isEmpty() && (m.tipo != Tipo.LIBRE || !m.texto.isEmpty());
    }

    // ---------- choosing ----------

    /** How many easy/medium/hard missions for a total, with the current mode. */
    public static int[] repartir(int total) {
        int[] r = new int[3];
        if ("azar".equals(Ajustes.modo)) {
            for (int i = 0; i < total; i++) r[RANDOM.nextInt(3)]++;
            return r;
        }
        int suma = Ajustes.porcentajeFacil + Ajustes.porcentajeMedia + Ajustes.porcentajeDificil;
        if (suma <= 0) suma = 1;
        double[] exacto = {total * (double) Ajustes.porcentajeFacil / suma, total * (double) Ajustes.porcentajeMedia / suma,
                total * (double) Ajustes.porcentajeDificil / suma};
        int usados = 0;
        for (int i = 0; i < 3; i++) { r[i] = (int) Math.floor(exacto[i]); usados += r[i]; }
        // Hand out what is left to the biggest remainders (easy first when tied)
        while (usados < total) {
            int mejor = 0;
            double resto = -1;
            for (int i = 0; i < 3; i++) {
                double x = exacto[i] - r[i];
                if (x > resto + 1e-9) { resto = x; mejor = i; }
            }
            r[mejor]++;
            exacto[mejor] = r[mejor];
            usados++;
        }
        return r;
    }

    /** Random missions of one difficulty, spreading the types if enabled. */
    public static List<Mision> alAzar(Dificultad dificultad, int cantidad, List<String> evitar, Predicate<Mision> sirve) {
        List<Mision> resultado = new ArrayList<>();
        if (cantidad <= 0) return resultado;
        List<Mision> candidatas = new ArrayList<>();
        for (Mision m : PLANTILLAS) {
            if (m.dificultad == dificultad && sorteable(m) && sirve.test(m)) candidatas.add(m);
        }
        if (candidatas.isEmpty()) return resultado;
        List<Mision> frescas = new ArrayList<>(candidatas);
        frescas.removeIf(m -> evitar.contains(m.plantilla));
        if (frescas.isEmpty()) frescas = new ArrayList<>(candidatas);
        Collections.shuffle(frescas, RANDOM);

        if (Ajustes.repartirTipos) {
            Map<Tipo, List<Mision>> porTipo = new EnumMap<>(Tipo.class);
            for (Mision m : frescas) porTipo.computeIfAbsent(m.tipo, t -> new ArrayList<>()).add(m);
            List<Tipo> tipos = new ArrayList<>(porTipo.keySet());
            Collections.shuffle(tipos, RANDOM);
            int vuelta = 0;
            while (resultado.size() < cantidad && !tipos.isEmpty()) {
                Tipo t = tipos.get(vuelta % tipos.size());
                List<Mision> grupo = porTipo.get(t);
                resultado.add(grupo.remove(grupo.size() - 1).copia());
                if (grupo.isEmpty()) tipos.remove(t); else vuelta++;
                if (!tipos.isEmpty() && vuelta >= tipos.size()) { vuelta = 0; Collections.shuffle(tipos, RANDOM); }
            }
        } else {
            for (Mision m : frescas) {
                if (resultado.size() >= cantidad) break;
                resultado.add(m.copia());
            }
        }
        // Not enough new ones: allow repeats
        while (resultado.size() < cantidad) {
            resultado.add(candidatas.get(RANDOM.nextInt(candidatas.size())).copia());
        }
        return resultado;
    }

    // ---------- loading ----------

    /** Loads the file, creating it with the default missions the first time (or updating an old v1 file). */
    public static int cargar() {
        PLANTILLAS.clear();
        try {
            Files.createDirectories(ARCHIVO.getParent());
            if (Files.exists(ARCHIVO) && version(ARCHIVO) < VERSION) {
                Path copia = ARCHIVO.resolveSibling("misiones_v1_respaldo.json");
                Files.move(ARCHIVO, copia, StandardCopyOption.REPLACE_EXISTING);
                Misiones.LOGGER.info("Catalogo viejo guardado como {}", copia);
            }
            if (!Files.exists(ARCHIVO)) {
                try (InputStream in = Catalogo.class.getResourceAsStream(RECURSO)) {
                    if (in == null) throw new IllegalStateException("Falta " + RECURSO);
                    Files.copy(in, ARCHIVO);
                }
            }
            try (Reader reader = Files.newBufferedReader(ARCHIVO, StandardCharsets.UTF_8)) {
                leer(GSON.fromJson(reader, JsonObject.class));
            }
        } catch (Exception ex) {
            Misiones.LOGGER.error("No se pudo leer " + ARCHIVO + " - usando las misiones de fabrica", ex);
            PLANTILLAS.clear();
            try (InputStream in = Catalogo.class.getResourceAsStream(RECURSO)) {
                if (in != null) leer(GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonObject.class));
            } catch (Exception ignored) {
            }
        }
        return PLANTILLAS.size();
    }

    private static int version(Path archivo) {
        try (Reader reader = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            return json != null && json.has("version") ? json.get("version").getAsInt() : 1;
        } catch (Exception e) {
            return VERSION; // broken file: leave it for the player to fix
        }
    }

    private static void leer(JsonObject json) {
        if (json == null || !json.has("misiones")) return;
        for (JsonElement e : json.getAsJsonArray("misiones")) {
            if (!e.isJsonObject()) continue;
            try {
                Mision m = Mision.deJson(e.getAsJsonObject());
                if (m != null && !m.objetivos.isEmpty()) PLANTILLAS.add(m);
            } catch (Exception ex) {
                Misiones.LOGGER.warn("Mision con error en el catalogo: {}", e);
            }
        }
    }

    private Catalogo() {}
}
