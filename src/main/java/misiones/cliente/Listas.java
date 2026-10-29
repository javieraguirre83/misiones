package misiones.cliente;

import misiones.core.Cosas;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/** The choices shown in the picker screen. */
public final class Listas {

    /** One choice: the id saved in the mission, the name shown and an icon. */
    public record Entrada(String id, Component nombre, ItemStack icono, String busqueda) {
        public Entrada(String id, Component nombre, ItemStack icono) {
            this(id, nombre, icono, (nombre.getString() + " " + id).toLowerCase());
        }
    }

    public static Entrada cualquiera() {
        return new Entrada("*", Component.translatable("misiones.menu.cualquiera"), new ItemStack(Items.NETHER_STAR));
    }

    public static List<Entrada> objetos() {
        List<Entrada> r = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) continue;
            ItemStack s = new ItemStack(item);
            r.add(new Entrada(BuiltInRegistries.ITEM.getKey(item).toString(), s.getHoverName(), s));
        }
        return r;
    }

    public static List<Entrada> bloques() {
        List<Entrada> r = new ArrayList<>();
        for (Block b : BuiltInRegistries.BLOCK) {
            String id = BuiltInRegistries.BLOCK.getKey(b).toString();
            if (id.equals("minecraft:air")) continue;
            ItemStack icono = b.asItem() == Items.AIR ? ItemStack.EMPTY : new ItemStack(b.asItem());
            if (icono.isEmpty()) icono = Iconos.item(id.replace("carrots", "carrot").replace("potatoes", "potato").replace("beetroots", "beetroot")
                    .replace("_crop", "").replace("_plant", ""));
            r.add(new Entrada(id, b.getName(), icono));
        }
        return r;
    }

    /** Creatures that have a spawn egg (that is, real mobs and animals). */
    public static List<Entrada> mobs() {
        List<Entrada> r = new ArrayList<>();
        for (EntityType<?> t : BuiltInRegistries.ENTITY_TYPE) {
            String id = BuiltInRegistries.ENTITY_TYPE.getKey(t).toString();
            ItemStack huevo = Iconos.item(id + "_spawn_egg");
            if (huevo.isEmpty()) continue;
            r.add(new Entrada(id, t.getDescription(), huevo));
        }
        return r;
    }

    public static List<Entrada> comerciantes() {
        List<Entrada> r = new ArrayList<>();
        for (String id : new String[]{"minecraft:villager", "minecraft:wandering_trader", "minecraft:piglin"}) {
            r.add(new Entrada(id, Cosas.nombreMob(id), Iconos.item(id + "_spawn_egg")));
        }
        return r;
    }

    public static List<Entrada> pociones() {
        List<Entrada> r = new ArrayList<>();
        for (Potion p : BuiltInRegistries.POTION) {
            Holder<Potion> h = BuiltInRegistries.POTION.wrapAsHolder(p);
            String id = BuiltInRegistries.POTION.getKey(p).toString();
            r.add(new Entrada(id, Cosas.nombrePocion(id), PotionContents.createItemStack(Items.POTION, h)));
        }
        return r;
    }

    public static List<Entrada> encantamientos() {
        List<Entrada> r = new ArrayList<>();
        var mc = Minecraft.getInstance();
        if (mc.level == null) return r;
        mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElementIds().forEach(k -> {
            Identifier i = k.identifier();
            r.add(new Entrada(i.toString(), Component.translatableWithFallback("enchantment." + i.getNamespace() + "." + i.getPath(), Cosas.bonito(i.getPath())),
                    new ItemStack(Items.ENCHANTED_BOOK)));
        });
        return r;
    }

    /** Smithing trim patterns (for armor that must have a trim). */
    public static List<Entrada> adornos() {
        List<Entrada> r = new ArrayList<>();
        var mc = Minecraft.getInstance();
        if (mc.level == null) return r;
        mc.level.registryAccess().lookupOrThrow(Registries.TRIM_PATTERN).listElementIds().forEach(k -> {
            Identifier i = k.identifier();
            r.add(new Entrada(i.toString(), Component.translatableWithFallback("trim_pattern." + i.getNamespace() + "." + i.getPath(), Cosas.bonito(i.getPath())),
                    Iconos.item(i.getNamespace() + ":" + i.getPath() + "_armor_trim_smithing_template")));
        });
        return r;
    }

    public static List<Entrada> biomas() {
        List<Entrada> r = new ArrayList<>();
        var mc = Minecraft.getInstance();
        if (mc.level == null) return r;
        mc.level.registryAccess().lookupOrThrow(Registries.BIOME).listElementIds().forEach(k -> {
            Identifier i = k.identifier();
            r.add(new Entrada("bioma:" + i, Component.translatableWithFallback("biome." + i.getNamespace() + "." + i.getPath(), Cosas.bonito(i.getPath())),
                    new ItemStack(Items.GRASS_BLOCK)));
        });
        return r;
    }

    public static List<Entrada> estructuras() {
        List<Entrada> r = new ArrayList<>();
        for (String id : DatosCliente.estructuras) r.add(new Entrada("estructura:" + id, Component.literal(Cosas.bonito(id)), new ItemStack(Items.FILLED_MAP)));
        return r;
    }

    public static List<Entrada> dimensiones() {
        List<Entrada> r = new ArrayList<>();
        for (String id : DatosCliente.dimensiones) {
            ItemStack icono = new ItemStack(id.contains("nether") ? Items.NETHERRACK : id.contains("end") ? Items.END_STONE : Items.GRASS_BLOCK);
            r.add(new Entrada("dimension:" + id, misiones.core.Progreso.nombreLugar("dimension:" + id), icono));
        }
        return r;
    }

    public static List<Entrada> logros() {
        List<Entrada> r = new ArrayList<>();
        for (String id : DatosCliente.logros) r.add(new Entrada(id, Cosas.nombreLogro(id), new ItemStack(Items.KNOWLEDGE_BOOK)));
        return r;
    }

    public static List<Entrada> viajes() {
        List<Entrada> r = new ArrayList<>();
        String[][] formas = {{"caminar", "leather_boots"}, {"correr", "golden_boots"}, {"nadar", "water_bucket"}, {"bucear", "turtle_helmet"},
                {"barco", "oak_boat"}, {"caballo", "saddle"}, {"cerdo", "carrot_on_a_stick"}, {"vagoneta", "minecart"}, {"elitros", "elytra"},
                {"lanzador", "warped_fungus_on_a_stick"}, {"trepar", "ladder"}, {"agachado", "leather_leggings"}, {"saltar", "rabbit_foot"},
                {"caer", "feather"}, {"fantasma", "ghast_tear"}};
        for (String[] f : formas) {
            r.add(new Entrada(f[0], Component.translatable("misiones.menu.viaje." + f[0]), Iconos.item(f[1])));
        }
        return r;
    }

    private Listas() {}
}
