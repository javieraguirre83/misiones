package misiones.core;

import misiones.modelo.Mision;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/** Each player's missions, saved with the player in the world. */
public final class Datos {
    private static final String CLAVE = "misiones";
    private static final String ESPERA = "misionesEspera";
    private static final String DIA = "misionesDia";

    public static List<Mision> lista(Player player) {
        List<Mision> lista = new ArrayList<>();
        for (Tag t : player.getPersistentData().getListOrEmpty(CLAVE)) {
            if (t instanceof CompoundTag c) lista.add(Mision.cargar(c));
        }
        return lista;
    }

    public static void guardar(Player player, List<Mision> lista) {
        ListTag tag = new ListTag();
        lista.forEach(m -> tag.add(m.guardar()));
        player.getPersistentData().put(CLAVE, tag);
    }

    /** Missions waiting for a free place (chains / daily when the limit is reached). */
    public static List<Mision> espera(Player player) {
        List<Mision> lista = new ArrayList<>();
        for (Tag t : player.getPersistentData().getListOrEmpty(ESPERA)) {
            if (t instanceof CompoundTag c) lista.add(Mision.cargar(c));
        }
        return lista;
    }

    public static void guardarEspera(Player player, List<Mision> lista) {
        ListTag tag = new ListTag();
        lista.forEach(m -> tag.add(m.guardar()));
        player.getPersistentData().put(ESPERA, tag);
    }

    /** Last world day seen by this player (for the daily mission), -1 = never. */
    public static long ultimoDia(Player player) {
        return player.getPersistentData().getLongOr(DIA, -1L);
    }

    public static void ultimoDia(Player player, long dia) {
        player.getPersistentData().putLong(DIA, dia);
    }

    /** Keeps the missions when Minecraft creates the new player after death. */
    public static void copiar(Player de, Player a) {
        for (String clave : new String[]{CLAVE, ESPERA, DIA}) {
            Tag tag = de.getPersistentData().get(clave);
            if (tag != null) a.getPersistentData().put(clave, tag.copy());
        }
    }

    private Datos() {}
}
