package misiones.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Settings: config/misiones/ajustes.json (also changeable with /misiones ajustes ...). */
public final class Ajustes {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static final Path ARCHIVO = Path.of("config", "misiones", "ajustes.json");

    /** "balanceado" (more easy than hard) or "azar" (any difficulty). */
    public static String modo = "balanceado";
    public static int porcentajeFacil = 50;
    public static int porcentajeMedia = 33;
    public static int porcentajeDificil = 17;
    /** Maximum active (not completed) missions. 0 = no limit. */
    public static int limite = 10;
    /** A new mission every Minecraft day. */
    public static boolean diaria = true;
    /** Difficulty of the daily mission: "azar", "facil", "media" or "dificil". */
    public static String dificultadDiaria = "azar";
    /** Spread mission types when generating (not 5 kill missions in a row). */
    public static boolean repartirTipos = true;
    /** How much random-generated amounts move up or down, in percent (0 = exact catalog amounts). */
    public static int variacion = 50;

    public static void cargar() {
        try {
            if (Files.exists(ARCHIVO)) {
                try (Reader r = Files.newBufferedReader(ARCHIVO, StandardCharsets.UTF_8)) {
                    JsonObject j = GSON.fromJson(r, JsonObject.class);
                    if (j != null) {
                        if (j.has("modo")) modo = leerModo(j.get("modo").getAsString());
                        if (j.has("balanceado") && j.get("balanceado").isJsonObject()) {
                            JsonObject b = j.getAsJsonObject("balanceado");
                            if (b.has("facil")) porcentajeFacil = Math.max(0, b.get("facil").getAsInt());
                            if (b.has("media")) porcentajeMedia = Math.max(0, b.get("media").getAsInt());
                            if (b.has("dificil")) porcentajeDificil = Math.max(0, b.get("dificil").getAsInt());
                        }
                        if (j.has("limite")) limite = Math.max(0, j.get("limite").getAsInt());
                        if (j.has("diaria")) diaria = j.get("diaria").getAsBoolean();
                        if (j.has("dificultadDiaria")) dificultadDiaria = j.get("dificultadDiaria").getAsString().toLowerCase(Locale.ROOT);
                        if (j.has("repartirTipos")) repartirTipos = j.get("repartirTipos").getAsBoolean();
                        if (j.has("variacion")) variacion = Math.max(0, Math.min(100, j.get("variacion").getAsInt()));
                    }
                }
            }
        } catch (Exception e) {
            Misiones.LOGGER.error("No se pudo leer " + ARCHIVO, e);
        }
        guardar();
    }

    public static String leerModo(String texto) {
        String t = texto.toLowerCase(Locale.ROOT);
        return t.startsWith("azar") || t.equals("random") ? "azar" : "balanceado";
    }

    public static void guardar() {
        try {
            Files.createDirectories(ARCHIVO.getParent());
            JsonObject j = new JsonObject();
            j.addProperty("_ayuda", "modo: balanceado o azar. limite: misiones activas a la vez (0 = sin limite). "
                    + "diaria: true/false. dificultadDiaria: azar, facil, media o dificil. "
                    + "repartirTipos: true = no repetir el mismo tipo seguido. "
                    + "variacion: cuanto cambian al azar las cantidades (porcentaje, 0 = exactas).");
            j.addProperty("modo", modo);
            JsonObject b = new JsonObject();
            b.addProperty("facil", porcentajeFacil);
            b.addProperty("media", porcentajeMedia);
            b.addProperty("dificil", porcentajeDificil);
            j.add("balanceado", b);
            j.addProperty("limite", limite);
            j.addProperty("diaria", diaria);
            j.addProperty("dificultadDiaria", dificultadDiaria);
            j.addProperty("repartirTipos", repartirTipos);
            j.addProperty("variacion", variacion);
            try (Writer w = Files.newBufferedWriter(ARCHIVO, StandardCharsets.UTF_8)) {
                GSON.toJson(j, w);
            }
        } catch (Exception e) {
            Misiones.LOGGER.error("No se pudo guardar " + ARCHIVO, e);
        }
    }

    private Ajustes() {}
}
