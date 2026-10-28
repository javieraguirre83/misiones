package misiones.core;

import misiones.modelo.Mision;
import misiones.modelo.Objetivo;
import misiones.modelo.Recompensa;
import misiones.modelo.Tipo;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;
import java.util.Locale;

/** Checks progress, completes missions, gives rewards and builds the texts. */
public final class Progreso {

    // ---------- matching helpers ----------

    public static String normalizar(String id) {
        if (id == null) return "";
        String s = id.trim().toLowerCase(Locale.ROOT);
        if (s.startsWith("#")) {
            String t = s.substring(1);
            return "#" + (t.contains(":") ? t : "minecraft:" + t);
        }
        return s.contains(":") ? s : "minecraft:" + s;
    }

    private static Identifier id(String texto) {
        Identifier i = Identifier.tryParse(texto);
        return i == null ? Identifier.withDefaultNamespace("air") : i;
    }

    public static boolean coincideObjeto(String objetivo, ItemStack stack) {
        if (stack.isEmpty()) return false;
        String o = normalizar(objetivo);
        if (o.startsWith("#")) return stack.is(TagKey.create(Registries.ITEM, id(o.substring(1))));
        return o.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    public static boolean coincideMob(String objetivo, Entity entity) {
        String o = normalizar(objetivo);
        if (o.startsWith("#")) return entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, id(o.substring(1))));
        return o.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
    }

    /** EXPLORAR: dimension:, estructura:, bioma:, altura: */
    public static boolean lugarCumplido(String objetivo, ServerPlayer player) {
        String o = objetivo.trim().toLowerCase(Locale.ROOT);
        ServerLevel level = player.level();
        BlockPos pos = player.blockPosition();
        try {
            if (o.startsWith("dimension:")) {
                String d = normalizar(o.substring(10));
                return level.dimension().equals(ResourceKey.create(Registries.DIMENSION, id(d)));
            }
            if (o.startsWith("bioma:")) {
                String b = normalizar(o.substring(6));
                Holder<Biome> biome = level.getBiome(pos);
                if (b.startsWith("#")) return biome.is(TagKey.create(Registries.BIOME, id(b.substring(1))));
                return biome.is(ResourceKey.create(Registries.BIOME, id(b)));
            }
            if (o.startsWith("estructura:")) {
                String e = normalizar(o.substring(11));
                if (e.startsWith("#")) {
                    TagKey<Structure> tag = TagKey.create(Registries.STRUCTURE, id(e.substring(1)));
                    return level.structureManager().getStructureWithPieceAt(pos, tag).isValid();
                }
                ResourceKey<Structure> key = ResourceKey.create(Registries.STRUCTURE, id(e));
                return level.structureManager().getStructureWithPieceAt(pos, key).isValid();
            }
            if (o.startsWith("altura:")) {
                String a = o.substring(7).replace(" ", "");
                if (a.startsWith("<=")) return player.getY() <= Double.parseDouble(a.substring(2));
                if (a.startsWith(">=")) return player.getY() >= Double.parseDouble(a.substring(2));
                return (int) Math.floor(player.getY()) == Integer.parseInt(a);
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    // ---------- names for display ----------

    public static Component nombreObjeto(String objetivo) {
        String o = normalizar(objetivo);
        if (o.startsWith("#")) return Component.literal("#" + id(o.substring(1)).getPath());
        Item item = BuiltInRegistries.ITEM.getValue(id(o));
        return new ItemStack(item).getHoverName();
    }

    public static Component nombreMob(String objetivo) {
        String o = normalizar(objetivo);
        if (o.startsWith("#")) return Component.literal("#" + id(o.substring(1)).getPath());
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(id(o));
        return type.getDescription();
    }

    private static String bonito(String id) {
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        path = path.replace("#", "").replace('_', ' ');
        return path.isEmpty() ? path : Character.toUpperCase(path.charAt(0)) + path.substring(1);
    }

    public static Component nombreLugar(String objetivo) {
        String o = objetivo.trim().toLowerCase(Locale.ROOT);
        if (o.startsWith("dimension:")) {
            String d = normalizar(o.substring(10));
            return switch (d) {
                case "minecraft:the_nether" -> Component.translatable("misiones.lugar.nether");
                case "minecraft:the_end" -> Component.translatable("misiones.lugar.end");
                case "minecraft:overworld" -> Component.translatable("misiones.lugar.overworld");
                default -> Component.literal(bonito(d));
            };
        }
        if (o.startsWith("bioma:")) {
            String b = normalizar(o.substring(6));
            if (b.startsWith("#")) return Component.translatable("misiones.lugar.bioma", bonito(b));
            Identifier i = id(b);
            return Component.translatable("misiones.lugar.bioma",
                    Component.translatableWithFallback("biome." + i.getNamespace() + "." + i.getPath(), bonito(b)));
        }
        if (o.startsWith("estructura:")) {
            return Component.translatable("misiones.lugar.estructura", bonito(normalizar(o.substring(11))));
        }
        if (o.startsWith("altura:")) {
            String a = o.substring(7).replace(" ", "");
            if (a.startsWith("<=")) return Component.translatable("misiones.lugar.abajo", a.substring(2));
            if (a.startsWith(">=")) return Component.translatable("misiones.lugar.arriba", a.substring(2));
            return Component.translatable("misiones.lugar.altura", a);
        }
        return Component.literal(objetivo);
    }

    /** "Consigue: 67/100 Manzana, 3/5 Hierro" (or the custom text). */
    public static MutableComponent descripcion(Mision m) {
        MutableComponent texto = Component.literal(m.tipo.icono + " ");
        if (!m.texto.isEmpty() && m.tipo == Tipo.LIBRE) {
            return texto.append(Component.literal(m.texto));
        }
        texto.append(Component.translatable(m.tipo.clave())).append(": ");
        boolean primero = true;
        for (Objetivo o : m.objetivos) {
            if (!primero) texto.append(", ");
            primero = false;
            MutableComponent parte = switch (m.tipo) {
                case CONSEGUIR, COFRE, FABRICAR -> Component.literal(o.progreso + "/" + o.cantidad + " ").append(nombreObjeto(o.objetivo));
                case MATAR -> Component.literal(o.progreso + "/" + o.cantidad + " ").append(nombreMob(o.objetivo));
                case EXPLORAR -> nombreLugar(o.objetivo).copy();
                case SOBREVIVIR -> Component.translatable("misiones.dias", o.progreso, o.cantidad);
                case NIVEL -> Component.translatable("misiones.nivel", o.progreso, o.cantidad);
                case LIBRE -> Component.literal(m.texto);
            };
            parte.withStyle(o.cumplido() ? ChatFormatting.GREEN : ChatFormatting.WHITE);
            texto.append(parte);
        }
        if (!m.texto.isEmpty()) texto.append(Component.literal(" — " + m.texto).withStyle(ChatFormatting.GRAY));
        return texto;
    }

    public static MutableComponent linea(int numero, Mision m) {
        MutableComponent linea = Component.literal("#" + numero + " ").withStyle(ChatFormatting.GOLD);
        linea.append(Component.literal("[").append(Component.translatable(m.dificultad.clave())).append("] ").withStyle(m.dificultad.color));
        MutableComponent desc = descripcion(m);
        if (m.completada) desc = Component.literal("✔ ").withStyle(ChatFormatting.GREEN).append(desc.withStyle(ChatFormatting.STRIKETHROUGH, ChatFormatting.DARK_GRAY));
        linea.append(desc);
        if (!m.recompensas.isEmpty() && !m.completada) {
            linea.append(Component.literal("  🎁 ").withStyle(ChatFormatting.AQUA)).append(recompensas(m).withStyle(ChatFormatting.AQUA));
        }
        return linea;
    }

    public static MutableComponent recompensas(Mision m) {
        MutableComponent texto = Component.empty();
        boolean primero = true;
        for (Recompensa r : m.recompensas) {
            if (!primero) texto.append(", ");
            primero = false;
            if (r.esXp()) texto.append(Component.translatable("misiones.recompensa.xp", r.cantidad));
            else texto.append(Component.literal(r.cantidad + " ")).append(nombreObjeto(r.objeto));
        }
        return texto;
    }

    // ---------- updating ----------

    /** Day number of the world (for SOBREVIVIR). */
    public static long dia(Level level) {
        return level.getDayTime() / 24000L;
    }

    /** Recalculates the missions that depend on the current state (inventory, place, level, days). */
    public static void revisarEstado(ServerPlayer player) {
        List<Mision> lista = Datos.lista(player);
        boolean cambio = false;
        for (Mision m : lista) {
            if (m.completada) continue;
            switch (m.tipo) {
                case CONSEGUIR -> {
                    for (Objetivo o : m.objetivos) {
                        int total = 0;
                        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                            ItemStack s = player.getInventory().getItem(i);
                            if (coincideObjeto(o.objetivo, s)) total += s.getCount();
                        }
                        int nuevo = Math.min(total, o.cantidad);
                        if (nuevo != o.progreso) { o.progreso = nuevo; cambio = true; }
                    }
                }
                case EXPLORAR -> {
                    for (Objetivo o : m.objetivos) {
                        if (!o.cumplido() && lugarCumplido(o.objetivo, player)) { o.progreso = o.cantidad; cambio = true; }
                    }
                }
                case NIVEL -> {
                    for (Objetivo o : m.objetivos) {
                        int nuevo = Math.min(player.experienceLevel, o.cantidad);
                        if (nuevo != o.progreso) { o.progreso = nuevo; cambio = true; }
                    }
                }
                case SOBREVIVIR -> {
                    if (m.diaInicio < 0) { m.diaInicio = dia(player.level()); cambio = true; }
                    for (Objetivo o : m.objetivos) {
                        int nuevo = (int) Math.min(dia(player.level()) - m.diaInicio, o.cantidad);
                        if (nuevo != o.progreso) { o.progreso = Math.max(0, nuevo); cambio = true; }
                    }
                }
                default -> { }
            }
        }
        if (cambio) {
            completarListas(player, lista);
            Datos.guardar(player, lista);
        }
    }

    /** Adds progress to counting missions (kill / craft). */
    public static void sumar(ServerPlayer player, Tipo tipo, java.util.function.Predicate<String> coincide, int cuanto) {
        List<Mision> lista = Datos.lista(player);
        boolean cambio = false;
        for (Mision m : lista) {
            if (m.completada || m.tipo != tipo) continue;
            for (Objetivo o : m.objetivos) {
                if (!o.cumplido() && coincide.test(o.objetivo)) {
                    o.progreso = Math.min(o.cantidad, o.progreso + cuanto);
                    cambio = true;
                    player.sendSystemMessage(descripcion(m).withStyle(ChatFormatting.YELLOW), true);
                }
            }
        }
        if (cambio) {
            completarListas(player, lista);
            Datos.guardar(player, lista);
        }
    }

    /** Chest missions: counts what is inside the chest that was just closed. */
    public static void revisarCofre(ServerPlayer player, List<ItemStack> contenido) {
        List<Mision> lista = Datos.lista(player);
        boolean cambio = false;
        for (Mision m : lista) {
            if (m.completada || m.tipo != Tipo.COFRE) continue;
            for (Objetivo o : m.objetivos) {
                int total = 0;
                for (ItemStack s : contenido) {
                    if (coincideObjeto(o.objetivo, s)) total += s.getCount();
                }
                int nuevo = Math.min(total, o.cantidad);
                if (nuevo > o.progreso) { o.progreso = nuevo; cambio = true; }
            }
        }
        if (cambio) {
            completarListas(player, lista);
            Datos.guardar(player, lista);
        }
    }

    /** When the player dies, survive-counters start again. */
    public static void alMorir(ServerPlayer player) {
        List<Mision> lista = Datos.lista(player);
        boolean cambio = false;
        for (Mision m : lista) {
            if (!m.completada && m.tipo == Tipo.SOBREVIVIR) {
                m.diaInicio = dia(player.level());
                m.objetivos.forEach(o -> o.progreso = 0);
                cambio = true;
            }
        }
        if (cambio) Datos.guardar(player, lista);
    }

    private static void completarListas(ServerPlayer player, List<Mision> lista) {
        for (int i = 0; i < lista.size(); i++) {
            Mision m = lista.get(i);
            if (!m.completada && m.tipo != Tipo.LIBRE && m.todosCumplidos()) {
                completar(player, m, i + 1);
            }
        }
    }

    public static void completar(ServerPlayer player, Mision m, int numero) {
        m.completada = true;
        player.sendSystemMessage(Component.translatable("misiones.completada", numero).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        player.sendSystemMessage(descripcion(m).withStyle(ChatFormatting.GRAY));
        player.level().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
        darRecompensas(player, m);
    }

    private static void darRecompensas(ServerPlayer player, Mision m) {
        if (m.recompensas.isEmpty()) return;
        for (Recompensa r : m.recompensas) {
            if (r.esXp()) {
                player.giveExperienceLevels(r.cantidad);
                continue;
            }
            Item item = BuiltInRegistries.ITEM.getValue(id(normalizar(r.objeto)));
            int restante = r.cantidad;
            while (restante > 0) {
                ItemStack stack = new ItemStack(item, Math.min(restante, item.getDefaultMaxStackSize()));
                restante -= stack.getCount();
                if (!player.getInventory().add(stack)) player.drop(stack, false);
            }
        }
        player.sendSystemMessage(Component.translatable("misiones.recompensa.recibida", recompensas(m)).withStyle(ChatFormatting.AQUA));
    }

    private Progreso() {}
}
