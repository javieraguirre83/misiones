package misiones.core;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;

import java.util.Locale;

/** VIAJAR: distances from the player's statistics (walking, boat, elytra...). */
public final class Viajes {

    /** Canonical name of a way of travelling ("walk" -> "caminar"). */
    public static String forma(String objetivo) {
        String t = objetivo == null ? "" : objetivo.trim().toLowerCase(Locale.ROOT).replace("í", "i").replace("é", "e");
        return switch (t) {
            case "caminar", "walk", "andar" -> "caminar";
            case "correr", "sprint", "run" -> "correr";
            case "nadar", "swim" -> "nadar";
            case "bucear", "underwater", "dive" -> "bucear";
            case "barco", "bote", "boat" -> "barco";
            case "caballo", "horse" -> "caballo";
            case "cerdo", "pig" -> "cerdo";
            case "vagoneta", "minecart" -> "vagoneta";
            case "elitros", "elytra", "planear" -> "elitros";
            case "lanzador", "strider" -> "lanzador";
            case "trepar", "climb", "escalar" -> "trepar";
            case "agachado", "crouch", "sneak" -> "agachado";
            case "volar", "fly" -> "volar";
            case "caer", "fall" -> "caer";
            case "saltar", "jump", "saltos" -> "saltar";
            case "fantasma", "happy_ghast", "ghast" -> "fantasma";
            default -> "caminar";
        };
    }

    private static Identifier estadistica(String forma) {
        return switch (forma) {
            case "correr" -> Stats.SPRINT_ONE_CM;
            case "nadar" -> Stats.SWIM_ONE_CM;
            case "bucear" -> Stats.WALK_UNDER_WATER_ONE_CM;
            case "barco" -> Stats.BOAT_ONE_CM;
            case "caballo" -> Stats.HORSE_ONE_CM;
            case "cerdo" -> Stats.PIG_ONE_CM;
            case "vagoneta" -> Stats.MINECART_ONE_CM;
            case "elitros" -> Stats.AVIATE_ONE_CM;
            case "lanzador" -> Stats.STRIDER_ONE_CM;
            case "trepar" -> Stats.CLIMB_ONE_CM;
            case "agachado" -> Stats.CROUCH_ONE_CM;
            case "volar" -> Stats.FLY_ONE_CM;
            case "caer" -> Stats.FALL_ONE_CM;
            case "saltar" -> Stats.JUMP;
            case "fantasma" -> Stats.HAPPY_GHAST_ONE_CM;
            default -> Stats.WALK_ONE_CM;
        };
    }

    /** Current value of the statistic (centimetres, or jumps). */
    public static int valor(ServerPlayer player, String objetivo) {
        return player.getStats().getValue(Stats.CUSTOM, estadistica(forma(objetivo)));
    }

    /** Converts the statistic difference to blocks (jumps stay as they are). */
    public static int enBloques(String objetivo, int diferencia) {
        return "saltar".equals(forma(objetivo)) ? diferencia : diferencia / 100;
    }

    private Viajes() {}
}
