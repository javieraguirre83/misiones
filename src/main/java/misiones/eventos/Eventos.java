package misiones.eventos;

import misiones.core.Datos;
import misiones.core.Progreso;
import misiones.modelo.Tipo;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.List;

/** Watches what the player does to update mission progress. */
public final class Eventos {

    public static void registrar() {
        TickEvent.PlayerTickEvent.Post.BUS.addListener(Eventos::alTick);
        LivingDeathEvent.BUS.addListener(Eventos::alMorir);
        PlayerEvent.ItemCraftedEvent.BUS.addListener(Eventos::alFabricar);
        PlayerContainerEvent.Close.BUS.addListener(Eventos::alCerrar);
        PlayerEvent.Clone.BUS.addListener(e -> Datos.copiar(e.getOriginal(), e.getEntity()));
    }

    /** Every second: inventory, places, level and days. */
    private static void alTick(TickEvent.PlayerTickEvent.Post event) {
        if (event.player() instanceof ServerPlayer player && player.tickCount % 20 == 0) {
            Progreso.revisarEstado(player);
        }
    }

    private static void alMorir(LivingDeathEvent event) {
        // A player killed a mob
        if (event.getSource().getEntity() instanceof ServerPlayer killer && event.getEntity() != killer) {
            Progreso.sumar(killer, Tipo.MATAR, objetivo -> Progreso.coincideMob(objetivo, event.getEntity()), 1);
        }
        // The player died: survive missions start again
        if (event.getEntity() instanceof ServerPlayer player) {
            Progreso.alMorir(player);
        }
    }

    private static void alFabricar(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack hecho = event.getCrafting();
            Progreso.sumar(player, Tipo.FABRICAR, objetivo -> Progreso.coincideObjeto(objetivo, hecho), Math.max(1, hecho.getCount()));
        }
    }

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
