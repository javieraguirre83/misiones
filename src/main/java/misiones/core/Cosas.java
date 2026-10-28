package misiones.core;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Reading and matching "objetivo" texts:
 *   minecraft:apple, #minecraft:logs, * (anything),
 *   and details in brackets: minecraft:diamond_sword[sharpness=5,unbreaking=3],
 *   #minecraft:chest_armor[adorno=*], minecraft:potion[pocion=strength].
 */
public final class Cosas {

    /** An objective split in its id and its details. */
    public record Cosa(String id, Map<String, String> detalles) {
        public boolean cualquiera() { return "*".equals(id); }
        public boolean esTag() { return id.startsWith("#"); }
    }

    public static Cosa leer(String objetivo) {
        String texto = objetivo == null ? "" : objetivo.trim();
        Map<String, String> detalles = new LinkedHashMap<>();
        int abre = texto.indexOf('[');
        if (abre >= 0) {
            int cierra = texto.lastIndexOf(']');
            String dentro = texto.substring(abre + 1, cierra > abre ? cierra : texto.length());
            for (String parte : dentro.split(",")) {
                String p = parte.trim();
                if (p.isEmpty()) continue;
                int igual = p.indexOf('=');
                if (igual < 0) detalles.put(p.toLowerCase(Locale.ROOT), "1");
                else detalles.put(p.substring(0, igual).trim().toLowerCase(Locale.ROOT), p.substring(igual + 1).trim().toLowerCase(Locale.ROOT));
            }
            texto = texto.substring(0, abre);
        }
        String id = texto.equals("*") || texto.isEmpty() ? "*" : normalizar(texto);
        return new Cosa(id, detalles);
    }

    public static String normalizar(String id) {
        if (id == null) return "";
        String s = id.trim().toLowerCase(Locale.ROOT);
        if (s.equals("*")) return s;
        if (s.startsWith("#")) {
            String t = s.substring(1);
            return "#" + (t.contains(":") ? t : "minecraft:" + t);
        }
        return s.contains(":") ? s : "minecraft:" + s;
    }

    public static Identifier id(String texto) {
        Identifier i = Identifier.tryParse(texto.startsWith("#") ? texto.substring(1) : texto);
        return i == null ? Identifier.withDefaultNamespace("air") : i;
    }

    // ---------- matching ----------

    public static boolean objeto(String objetivo, ItemStack stack) {
        if (stack.isEmpty()) return false;
        Cosa c = leer(objetivo);
        if (!c.cualquiera()) {
            if (c.esTag()) {
                if (!stack.is(TagKey.create(Registries.ITEM, id(c.id())))) return false;
            } else if (!c.id().equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())) {
                return false;
            }
        }
        return detalles(stack, c.detalles());
    }

    public static boolean detalles(ItemStack stack, Map<String, String> detalles) {
        for (Map.Entry<String, String> d : detalles.entrySet()) {
            String clave = d.getKey();
            String valor = d.getValue();
            switch (clave) {
                case "pocion", "potion" -> {
                    if (!pocion(stack).equals(normalizar(valor))) return false;
                }
                case "adorno", "trim" -> {
                    ArmorTrim trim = stack.get(DataComponents.TRIM);
                    if (trim == null) return false;
                    if (!valor.equals("*") && !trim.pattern().getRegisteredName().equals(normalizar(valor))) return false;
                }
                default -> {
                    int minimo;
                    try { minimo = Integer.parseInt(valor); } catch (NumberFormatException e) { minimo = 1; }
                    if (nivelEncantamiento(stack, normalizar(clave)) < minimo) return false;
                }
            }
        }
        return true;
    }

    /** Level of an enchantment on an item or enchanted book (0 if it does not have it). */
    public static int nivelEncantamiento(ItemStack stack, String encantamiento) {
        int nivel = 0;
        for (ItemEnchantments lista : new ItemEnchantments[]{
                stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY),
                stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY)}) {
            for (Holder<Enchantment> h : lista.keySet()) {
                if (h.getRegisteredName().equals(encantamiento)) nivel = Math.max(nivel, lista.getLevel(h));
            }
        }
        return nivel;
    }

    /** Sum of all enchantment levels (to see if the anvil added something). */
    public static int totalEncantamientos(ItemStack stack) {
        int total = 0;
        for (ItemEnchantments lista : new ItemEnchantments[]{
                stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY),
                stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY)}) {
            for (Holder<Enchantment> h : lista.keySet()) total += lista.getLevel(h);
        }
        return total;
    }

    /** Potion id of a potion item ("" if none). */
    public static String pocion(ItemStack stack) {
        PotionContents p = stack.get(DataComponents.POTION_CONTENTS);
        if (p == null || p.potion().isEmpty()) return "";
        return p.potion().get().getRegisteredName();
    }

    public static boolean pocionCoincide(String objetivo, ItemStack stack) {
        String p = pocion(stack);
        if (p.isEmpty()) return false;
        String o = normalizar(leer(objetivo).id());
        return o.equals("*") || o.equals(p);
    }

    public static boolean mob(String objetivo, Entity entity) {
        return mob(objetivo, entity.getType());
    }

    public static boolean mob(String objetivo, EntityType<?> type) {
        Cosa c = leer(objetivo);
        if (c.cualquiera()) return true;
        if (c.esTag()) return BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type).is(TagKey.create(Registries.ENTITY_TYPE, id(c.id())));
        return c.id().equals(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
    }

    public static boolean esMob(String objetivo) {
        Cosa c = leer(objetivo);
        if (c.cualquiera()) return false;
        if (c.esTag()) return false;
        return BuiltInRegistries.ENTITY_TYPE.containsKey(id(c.id()));
    }

    public static boolean bloque(String objetivo, BlockState state) {
        Cosa c = leer(objetivo);
        if (c.cualquiera()) return true;
        if (c.esTag()) return state.is(TagKey.create(Registries.BLOCK, id(c.id())));
        return c.id().equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
    }

    private static final Set<String> CON_EDAD = Set.of("minecraft:nether_wart", "minecraft:cocoa", "minecraft:pitcher_crop", "minecraft:sweet_berry_bush");

    /** Is this crop fully grown? (melons, pumpkins, cane, cactus... always count) */
    public static boolean maduro(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CropBlock crop) return crop.isMaxAge(state);
        if (CON_EDAD.contains(BuiltInRegistries.BLOCK.getKey(block).toString())) {
            for (Property<?> p : state.getProperties()) {
                if (p instanceof IntegerProperty edad && p.getName().equals("age")) {
                    int max = 0;
                    for (Integer v : edad.getPossibleValues()) max = Math.max(max, v);
                    return state.getValue(edad) >= max;
                }
            }
        }
        return true;
    }

    // ---------- names for display ----------

    public static MutableComponent nombreObjeto(String objetivo) {
        Cosa c = leer(objetivo);
        MutableComponent nombre;
        if (c.cualquiera()) nombre = Component.translatable("misiones.cualquier.objeto");
        else if (c.esTag()) nombre = Component.literal("#" + id(c.id()).getPath());
        else {
            Item item = BuiltInRegistries.ITEM.getValue(id(c.id()));
            nombre = new ItemStack(item).getHoverName().copy();
        }
        return conDetalles(nombre, c.detalles());
    }

    public static MutableComponent conDetalles(MutableComponent nombre, Map<String, String> detalles) {
        if (detalles.isEmpty()) return nombre;
        nombre.append(" (");
        boolean primero = true;
        for (Map.Entry<String, String> d : detalles.entrySet()) {
            if (!primero) nombre.append(", ");
            primero = false;
            switch (d.getKey()) {
                case "pocion", "potion" -> nombre.append(nombrePocion(d.getValue()));
                case "adorno", "trim" -> nombre.append(Component.translatable("misiones.adorno"));
                default -> {
                    Identifier e = id(normalizar(d.getKey()));
                    nombre.append(Component.translatableWithFallback("enchantment." + e.getNamespace() + "." + e.getPath(), bonito(e.getPath())));
                    nombre.append(" ").append(Component.translatableWithFallback("enchantment.level." + d.getValue(), d.getValue()));
                }
            }
        }
        return nombre.append(")");
    }

    public static MutableComponent nombreMob(String objetivo) {
        Cosa c = leer(objetivo);
        if (c.cualquiera()) return Component.translatable("misiones.cualquier.mob");
        if (c.esTag()) return Component.literal("#" + id(c.id()).getPath());
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(id(c.id()));
        return type.getDescription().copy();
    }

    public static MutableComponent nombreBloque(String objetivo) {
        Cosa c = leer(objetivo);
        if (c.cualquiera()) return Component.translatable("misiones.cualquier.bloque");
        if (c.esTag()) return Component.literal("#" + id(c.id()).getPath());
        return BuiltInRegistries.BLOCK.getValue(id(c.id())).getName().copy();
    }

    public static MutableComponent nombrePocion(String objetivo) {
        String o = normalizar(leer(objetivo).id());
        if (o.equals("*")) return Component.translatable("misiones.cualquier.pocion");
        Identifier i = id(o);
        String path = i.getPath();
        String clave = "misiones.pocion.normal";
        if (path.startsWith("strong_")) { path = path.substring(7); clave = "misiones.pocion.fuerte"; }
        else if (path.startsWith("long_")) { path = path.substring(5); clave = "misiones.pocion.larga"; }
        Component base = Component.translatableWithFallback("item.minecraft.potion.effect." + path, bonito(path));
        return Component.translatable(clave, base);
    }

    public static MutableComponent nombreLogro(String objetivo) {
        Identifier i = id(normalizar(objetivo));
        return Component.translatableWithFallback("advancements." + i.getPath().replace('/', '.') + ".title", bonito(i.getPath()));
    }

    public static String bonito(String id) {
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        path = path.replace("#", "").replace('_', ' ');
        if (path.contains("/")) path = path.substring(path.lastIndexOf('/') + 1);
        return path.isEmpty() ? path : Character.toUpperCase(path.charAt(0)) + path.substring(1);
    }

    private Cosas() {}
}
