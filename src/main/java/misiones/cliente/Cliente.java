package misiones.cliente;

import misiones.core.Misiones;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/** Keys that open the mission screens (changeable in Options -> Controls). */
public final class Cliente {
    public static final KeyMapping.Category CATEGORIA = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Misiones.MODID, "teclas"));
    public static final KeyMapping TECLA_MENU = new KeyMapping("misiones.tecla.menu", GLFW.GLFW_KEY_M, CATEGORIA);
    public static final KeyMapping TECLA_LISTA = new KeyMapping("misiones.tecla.lista", GLFW.GLFW_KEY_J, CATEGORIA);

    public static void iniciar() {
        RegisterKeyMappingsEvent.BUS.addListener(e -> {
            e.register(TECLA_MENU);
            e.register(TECLA_LISTA);
        });
        InputEvent.Key.BUS.addListener(Cliente::alTecla);
    }

    private static boolean es(KeyMapping tecla, int key) {
        return !tecla.isUnbound() && tecla.getKey().getValue() == key;
    }

    private static void alTecla(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.screen() != null || event.getAction() != GLFW.GLFW_PRESS) return;
        if (es(TECLA_MENU, event.getKey())) mc.gui.setScreen(new PantallaMenu());
        else if (es(TECLA_LISTA, event.getKey())) mc.gui.setScreen(new PantallaLista());
    }

    private Cliente() {}
}
