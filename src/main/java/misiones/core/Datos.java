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

    /** Keeps the missions when Minecraft creates the new player after death. */
    public static void copiar(Player de, Player a) {
        Tag tag = de.getPersistentData().get(CLAVE);
        if (tag != null) a.getPersistentData().put(CLAVE, tag.copy());
    }

    private Datos() {}
}
