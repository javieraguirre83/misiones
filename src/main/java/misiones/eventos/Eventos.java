package misiones.eventos;

import misiones.core.Cosas;
import misiones.core.Datos;
import misiones.core.Gestor;
import misiones.core.Progreso;
import misiones.modelo.Tipo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.brewing.PlayerBrewedPotionEvent;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.TradeWithVillagerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.listener.Priority;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/** Watches what the player does to update mission progress. */
public final class Eventos {
    /** Last enchanting seed seen for each player (it changes every time they use the enchanting table). */
    private static final Map<UUID, Integer> SEMILLAS = new ConcurrentHashMap<>();
    /** Piglins that are already admiring gold (so one barter counts once). */
    private static final Map<Entity, Boolean> PIGLINS = new WeakHashMap<>();

    public static void registrar() {
        TickEvent.PlayerTickEvent.Post.BUS.addListener(Eventos::alTick);
        LivingDeathEvent.BUS.addListener(Priority.LOWEST, Eventos::alMorir);
        PlayerEvent.ItemCraftedEvent.BUS.addListener(Eventos::alFabricar);
        PlayerEvent.ItemSmeltedEvent.BUS.addListener(Eventos::alFundir);
        PlayerContainerEvent.Close.BUS.addListener(Eventos::alCerrar);
        PlayerEvent.Clone.BUS.addListener(e -> Datos.copiar(e.getOriginal(), e.getEntity()));
        BlockEvent.BreakEvent.BUS.addListener(Priority.LOWEST, Eventos::alRomper);
        BabyEntitySpawnEvent.BUS.addListener(Priority.LOWEST, Eventos::alNacer);
        AnimalTameEvent.BUS.addListener(Priority.LOWEST, Eventos::alDomar);
        ItemFishedEvent.BUS.addListener(Priority.LOWEST, Eventos::alPescar);
        LivingEntityUseItemEvent.Finish.BUS.addListener(Eventos::alComer);
        PlayerBrewedPotionEvent.BUS.addListener(Eventos::alPreparar);
        TradeWithVillagerEvent.BUS.addListener(Eventos::alComerciar);
        AnvilRepairEvent.BUS.addListener(Eventos::alYunque);
    }

    private static void alTick(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player() instanceof ServerPlayer player)) return;
        revisarMesaEncantar(player);
        if (player.tickCount % 20 == 0) {
            Progreso.revisarEstado(player);
            Gestor.revisarDia(player);
            revisarPiglins(player);
        }
    }

    // ---------- kill / breed / tame ----------

    private static void alMorir(LivingDeathEvent event) {
        // A player killed a mob
        if (event.getSource().getEntity() instanceof ServerPlayer killer && event.getEntity() != killer) {
            Progreso.sumar(killer, Tipo.MATAR, o -> Cosas.mob(o, event.getEntity()), 1);
        }
        // The player died: survive missions start again
        if (event.getEntity() instanceof ServerPlayer player) {
            Progreso.alMorir(player);
        }
    }

    private static void alNacer(BabyEntitySpawnEvent event) {
        if (event.getCausedByPlayer() instanceof ServerPlayer player && event.getChild() != null) {
            Progreso.sumar(player, Tipo.CRIAR, o -> Cosas.mob(o, event.getChild()), 1);
        }
    }

    private static void alDomar(AnimalTameEvent event) {
        if (event.getTamer() instanceof ServerPlayer player) {
            Progreso.sumar(player, Tipo.DOMESTICAR, o -> Cosas.mob(o, event.getAnimal()), 1);
        }
    }

    // ---------- blocks ----------

    private static void alRomper(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        BlockState state = event.getState();
        Progreso.sumar(player, Tipo.MINAR, o -> Cosas.bloque(o, state), 1);
        if (Cosas.maduro(state)) Progreso.sumar(player, Tipo.COSECHAR, o -> Cosas.bloque(o, state), 1);
    }

    // ---------- items ----------

    private static void alFabricar(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack hecho = event.getCrafting();
            Progreso.sumar(player, Tipo.FABRICAR, o -> Cosas.objeto(o, hecho), Math.max(1, hecho.getCount()));
        }
    }

    private static void alFundir(PlayerEvent.ItemSmeltedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack hecho = event.getSmelting();
            Progreso.sumar(player, Tipo.FUNDIR, o -> Cosas.objeto(o, hecho), Math.max(1, hecho.getCount()));
        }
    }

    private static void alPescar(ItemFishedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        for (ItemStack pez : event.getDrops()) {
            Progreso.sumar(player, Tipo.PESCAR, o -> Cosas.objeto(o, pez), Math.max(1, pez.getCount()));
        }
    }

    private static void alComer(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack comida = event.getItem();
        if (comida.isEmpty()) return;
        boolean esComida = comida.has(DataComponents.FOOD);
        Progreso.sumar(player, Tipo.COMER, o -> Cosas.leer(o).cualquiera() ? esComida : Cosas.objeto(o, comida), 1);
    }

    private static void alPreparar(PlayerBrewedPotionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack pocion = event.getStack();
            Progreso.sumar(player, Tipo.POCIONES, o -> Cosas.pocionCoincide(o, pocion), Math.max(1, pocion.getCount()));
        }
    }

    // ---------- enchanting ----------

    /** The enchanting table has no event: the player's enchanting seed changes when they enchant something. */
    private static void revisarMesaEncantar(ServerPlayer player) {
        int semilla = player.getEnchantmentSeed();
        Integer antes = SEMILLAS.put(player.getUUID(), semilla);
        if (antes == null || antes == semilla) return;
        ItemStack encantado = ItemStack.EMPTY;
        if (player.containerMenu instanceof EnchantmentMenu menu) encantado = menu.getSlot(0).getItem();
        if (encantado.isEmpty()) encantado = player.containerMenu.getCarried();
        contarEncantado(player, encantado);
    }

    private static void alYunque(AnvilRepairEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack salida = event.getOutput();
        if (Cosas.totalEncantamientos(salida) > Cosas.totalEncantamientos(event.getLeft())) contarEncantado(player, salida);
    }

    private static void contarEncantado(ServerPlayer player, ItemStack item) {
        Progreso.sumar(player, Tipo.ENCANTAR, o -> Cosas.leer(o).cualquiera() && Cosas.leer(o).detalles().isEmpty()
                || !item.isEmpty() && Cosas.objeto(o, item), 1);
    }

    // ---------- trading ----------

    private static void alComerciar(TradeWithVillagerEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Entity comerciante = event.getAbstractVillager();
        ItemStack recibido = event.getMerchantOffer().getResult();
        Progreso.sumar(player, Tipo.COMERCIAR, o -> {
            if (Cosas.leer(o).cualquiera()) return true;
            if (Cosas.esMob(o)) return Cosas.mob(o, comerciante);
            return Cosas.objeto(o, recibido);
        }, 1);
    }

    /** Piglin bartering has no event: a nearby adult piglin starting to admire gold counts as one barter. */
    private static void revisarPiglins(ServerPlayer player) {
        List<Entity> cerca = new ArrayList<>(player.level().getEntities(player, player.getBoundingBox().inflate(16),
                e -> e instanceof LivingEntity && "minecraft:piglin".equals(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString())));
        for (Entity e : cerca) {
            LivingEntity piglin = (LivingEntity) e;
            boolean admira = !piglin.isBaby() && Cosas.objeto("minecraft:gold_ingot", piglin.getOffhandItem());
            Boolean antes = PIGLINS.get(piglin);
            if (admira && !Boolean.TRUE.equals(antes)) {
                PIGLINS.put(piglin, true);
                Progreso.sumar(player, Tipo.COMERCIAR, o -> Cosas.leer(o).cualquiera() || Cosas.esMob(o) && Cosas.mob(o, piglin), 1);
            } else if (!admira && antes != null) {
                PIGLINS.remove(piglin);
            }
        }
    }

    // ---------- chests ----------

    /** When a chest (or any storage) is closed, look at what is inside it. */
    private static void alCerrar(PlayerContainerEvent.Close event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        AbstractContainerMenu menu = event.getContainer();
        List<ItemStack> contenido = new ArrayList<>();
        boolean hayCofre = false;
        for (Slot slot : menu.slots) {
            if (slot.container == player.getInventory()) continue;
            hayCofre = true;
            contenido.add(slot.getItem());
        }
        if (hayCofre) Progreso.revisarCofre(player, contenido);
    }

    private Eventos() {}
}
