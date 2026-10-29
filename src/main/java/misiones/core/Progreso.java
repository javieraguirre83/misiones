package misiones.core;

import misiones.modelo.Mision;
import misiones.modelo.Objetivo;
import misiones.modelo.Recompensa;
import misiones.modelo.Tipo;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.world.entity.EquipmentSlot;
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
import java.util.function.Predicate;

/** Checks progress, completes missions, gives rewards and builds the texts. */
public final class Progreso {

    // ---------- helpers ----------

    public static String normalizar(String id) {
        return Cosas.normalizar(id);
    }

    private static Identifier id(String texto) {
        return Cosas.id(texto);
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
                return level.structureManager().getStructureWithPieceAt(pos, (Holder<Structure> h) -> h.is(key)).isValid();
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

    private static String bonito(String id) {
        return Cosas.bonito(id);
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
        return descripcion(m, true);
    }

    /** Same text, optionally without the emoji at the start (the screens draw an item icon instead). */
    public static MutableComponent descripcion(Mision m, boolean icono) {
        MutableComponent texto = icono ? Component.literal(m.tipo.icono + " ") : Component.empty();
        if (!m.texto.isEmpty() && m.tipo == Tipo.LIBRE) {
            return texto.append(Component.literal(m.texto));
        }
        texto.append(Component.translatable(m.tipo.clave())).append(": ");
        boolean primero = true;
        for (Objetivo o : m.objetivos) {
            if (!primero) texto.append(", ");
            primero = false;
            String cuenta = o.progreso + "/" + o.cantidad + " ";
            MutableComponent parte = switch (m.tipo) {
                case CONSEGUIR, COFRE, FABRICAR, PESCAR, COMER, FUNDIR, ENCANTAR ->
                        Component.literal(cuenta).append(Cosas.nombreObjeto(o.objetivo));
                case EQUIPAR -> (o.cantidad > 1 ? Component.literal(cuenta) : Component.literal(o.cumplido() ? "✔ " : "✘ "))
                        .append(Cosas.nombreObjeto(o.objetivo));
                case MINAR, COSECHAR -> Component.literal(cuenta).append(Cosas.nombreBloque(o.objetivo));
                case MATAR, CRIAR, DOMESTICAR -> Component.literal(cuenta).append(Cosas.nombreMob(o.objetivo));
                case COMERCIAR -> Component.literal(cuenta).append(Cosas.leer(o.objetivo).cualquiera()
                        ? Component.translatable("misiones.cualquier.trato")
                        : Cosas.esMob(o.objetivo) ? Cosas.nombreMob(o.objetivo) : Cosas.nombreObjeto(o.objetivo));
                case POCIONES -> Component.literal(cuenta).append(Cosas.nombrePocion(o.objetivo));
                case VIAJAR -> Component.translatable("misiones.viajar." + Viajes.forma(o.objetivo), o.progreso, o.cantidad);
                case LOGROS -> Cosas.nombreLogro(o.objetivo).copy();
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

    /** Name of one objective without counts (for the menu): "Botas de diamante (Caída de pluma IV)". */
    public static MutableComponent nombreObjetivo(Tipo tipo, String objetivo, String texto) {
        return switch (tipo) {
            case CONSEGUIR, COFRE, FABRICAR, PESCAR, COMER, FUNDIR, ENCANTAR, EQUIPAR -> Cosas.nombreObjeto(objetivo);
            case MINAR, COSECHAR -> Cosas.nombreBloque(objetivo);
            case MATAR, CRIAR, DOMESTICAR -> Cosas.nombreMob(objetivo);
            case COMERCIAR -> Cosas.leer(objetivo).cualquiera() ? Component.translatable("misiones.cualquier.trato")
                    : Cosas.esMob(objetivo) ? Cosas.nombreMob(objetivo) : Cosas.nombreObjeto(objetivo);
            case POCIONES -> Cosas.nombrePocion(objetivo);
            case VIAJAR -> Component.translatable("misiones.menu.viaje." + Viajes.forma(objetivo));
            case LOGROS -> Cosas.nombreLogro(objetivo).copy();
            case EXPLORAR -> nombreLugar(objetivo).copy();
            case SOBREVIVIR -> Component.translatable("misiones.menu.dias");
            case NIVEL -> Component.translatable("misiones.menu.nivel");
            case LIBRE -> Component.literal(texto);
        };
    }

    public static MutableComponent linea(int numero, Mision m) {
        MutableComponent linea = Component.literal("#" + numero + " ").withStyle(ChatFormatting.GOLD);
        if (m.diaria) linea.append(Component.literal("📅 ").withStyle(ChatFormatting.AQUA));
        if (!m.cadena.isEmpty()) linea.append(Component.literal("⛓" + m.cadena + " ").withStyle(ChatFormatting.LIGHT_PURPLE));
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
            else texto.append(Component.literal(r.cantidad + " ")).append(Cosas.nombreObjeto(r.objeto));
        }
        return texto;
    }

    // ---------- updating ----------

    /** Day number of the world (for SOBREVIVIR and the daily mission). */
    public static long dia(Level level) {
        return level.getOverworldClockTime() / 24000L;
    }

    private static boolean poner(Objetivo o, int nuevo) {
        nuevo = Math.max(0, Math.min(nuevo, o.cantidad));
        if (nuevo == o.progreso) return false;
        o.progreso = nuevo;
        return true;
    }

    /** Recalculates the missions that depend on the current state (inventory, armor, place, level, days, distance, advancements). */
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
                            if (Cosas.objeto(o.objetivo, s)) total += s.getCount();
                        }
                        cambio |= poner(o, total);
                    }
                }
                case EQUIPAR -> {
                    for (Objetivo o : m.objetivos) cambio |= poner(o, equipado(player, o.objetivo));
                }
                case EXPLORAR -> {
                    for (Objetivo o : m.objetivos) {
                        if (!o.cumplido() && lugarCumplido(o.objetivo, player)) cambio |= poner(o, o.cantidad);
                    }
                }
                case NIVEL -> {
                    for (Objetivo o : m.objetivos) cambio |= poner(o, player.experienceLevel);
                }
                case SOBREVIVIR -> {
                    if (m.diaInicio < 0) { m.diaInicio = dia(player.level()); cambio = true; }
                    for (Objetivo o : m.objetivos) cambio |= poner(o, (int) (dia(player.level()) - m.diaInicio));
                }
                case VIAJAR -> {
                    for (Objetivo o : m.objetivos) {
                        int valor = Viajes.valor(player, o.objetivo);
                        if (o.base < 0) { o.base = valor; cambio = true; }
                        cambio |= poner(o, Viajes.enBloques(o.objetivo, valor - o.base));
                    }
                }
                case LOGROS -> {
                    for (Objetivo o : m.objetivos) {
                        if (!o.cumplido() && logroHecho(player, o.objetivo)) cambio |= poner(o, o.cantidad);
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

    /** EQUIPAR: armor counts only when worn; weapons, tools and other things count in the inventory. */
    private static int equipado(ServerPlayer player, String objetivo) {
        int total = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack s = player.getItemBySlot(slot);
            if (!s.isEmpty() && esArmadura(player, s) && Cosas.objeto(objetivo, s)) total += s.getCount();
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (!s.isEmpty() && !esArmadura(player, s) && Cosas.objeto(objetivo, s)) total += s.getCount();
        }
        ItemStack mano = player.getOffhandItem();
        if (!mano.isEmpty() && !esArmadura(player, mano) && Cosas.objeto(objetivo, mano)) {
            // The offhand may or may not be part of the inventory list: avoid counting it twice
            boolean yaContado = false;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                if (player.getInventory().getItem(i) == mano) { yaContado = true; break; }
            }
            if (!yaContado) total += mano.getCount();
        }
        return total;
    }

    private static boolean esArmadura(ServerPlayer player, ItemStack s) {
        return player.getEquipmentSlotForItem(s).getType() == EquipmentSlot.Type.HUMANOID_ARMOR;
    }

    public static boolean logroHecho(ServerPlayer player, String objetivo) {
        Identifier i = Identifier.tryParse(Cosas.normalizar(objetivo));
        if (i == null) return false;
        AdvancementHolder h = player.level().getServer().getAdvancements().get(i);
        return h != null && player.getAdvancements().getOrStartProgress(h).isDone();
    }

    /** Adds progress to counting missions (kill, craft, mine, fish...). */
    public static void sumar(ServerPlayer player, Tipo tipo, Predicate<String> coincide, int cuanto) {
        if (cuanto <= 0) return;
        List<Mision> lista = Datos.lista(player);
        boolean cambio = false;
        for (Mision m : lista) {
            if (m.completada || m.tipo != tipo) continue;
            boolean avanzo = false;
            for (Objetivo o : m.objetivos) {
                if (!o.cumplido() && coincide.test(o.objetivo)) {
                    o.progreso = Math.min(o.cantidad, o.progreso + cuanto);
                    avanzo = true;
                }
            }
            if (avanzo) {
                cambio = true;
                player.sendSystemMessage(descripcion(m).withStyle(ChatFormatting.YELLOW), true);
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
                    if (Cosas.objeto(o.objetivo, s)) total += s.getCount();
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
                completar(player, lista, m, i + 1);
            }
        }
    }

    /** Completes a mission: message, sound, rewards, next mission of the chain and missions waiting for a place. */
    public static void completar(ServerPlayer player, List<Mision> lista, Mision m, int numero) {
        m.completada = true;
        player.sendSystemMessage(Component.translatable("misiones.completada", numero).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        player.sendSystemMessage(descripcion(m).withStyle(ChatFormatting.GRAY));
        player.level().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
        darRecompensas(player, m);
        Gestor.alCompletar(player, lista, m);
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
