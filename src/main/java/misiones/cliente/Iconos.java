package misiones.cliente;

import misiones.core.Cosas;
import misiones.modelo.Mision;
import misiones.modelo.Tipo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** The item drawn next to each mission. */
public final class Iconos {

    public static ItemStack de(Mision m) {
        if (!m.objetivos.isEmpty()) {
            Cosas.Cosa c = Cosas.leer(m.objetivos.get(0).objetivo);
            if (!c.cualquiera() && !c.esTag()) {
                ItemStack s = switch (m.tipo) {
                    case CONSEGUIR, COFRE, FABRICAR, PESCAR, COMER, FUNDIR, ENCANTAR, EQUIPAR, MINAR, COSECHAR -> item(c.id());
                    case COMERCIAR -> Cosas.esMob(c.id()) ? item(c.id() + "_spawn_egg") : item(c.id());
                    case MATAR, CRIAR, DOMESTICAR -> item(c.id() + "_spawn_egg");
                    default -> ItemStack.EMPTY;
                };
                if (!s.isEmpty()) return s;
            }
        }
        return tipo(m.tipo);
    }

    public static ItemStack tipo(Tipo t) {
        return item(switch (t) {
            case CONSEGUIR -> "minecraft:apple";
            case COFRE -> "minecraft:chest";
            case MINAR -> "minecraft:iron_pickaxe";
            case COSECHAR -> "minecraft:wheat";
            case MATAR -> "minecraft:iron_sword";
            case CRIAR -> "minecraft:egg";
            case DOMESTICAR -> "minecraft:bone";
            case PESCAR -> "minecraft:fishing_rod";
            case FABRICAR -> "minecraft:crafting_table";
            case ENCANTAR -> "minecraft:enchanted_book";
            case POCIONES -> "minecraft:brewing_stand";
            case COMER -> "minecraft:cooked_beef";
            case FUNDIR -> "minecraft:furnace";
            case COMERCIAR -> "minecraft:emerald";
            case EQUIPAR -> "minecraft:iron_chestplate";
            case EXPLORAR -> "minecraft:compass";
            case VIAJAR -> "minecraft:leather_boots";
            case LOGROS -> "minecraft:knowledge_book";
            case SOBREVIVIR -> "minecraft:clock";
            case NIVEL -> "minecraft:experience_bottle";
            case LIBRE -> "minecraft:writable_book";
        });
    }

    public static ItemStack item(String id) {
        Item item = BuiltInRegistries.ITEM.getValue(Cosas.id(Cosas.normalizar(id)));
        return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private Iconos() {}
}
