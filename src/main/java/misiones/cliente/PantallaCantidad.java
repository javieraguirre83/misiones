package misiones.cliente;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.function.IntConsumer;

/** Asks for one number (amount, level...). */
public class PantallaCantidad extends Screen {
    private final Screen anterior;
    private final Component pregunta;
    private final int inicial;
    private final IntConsumer alAceptar;
    private EditBox campo;

    public PantallaCantidad(Screen anterior, Component titulo, Component pregunta, int inicial, IntConsumer alAceptar) {
        super(titulo);
        this.anterior = anterior;
        this.pregunta = pregunta;
        this.inicial = inicial;
        this.alAceptar = alAceptar;
    }

    @Override
    protected void init() {
        campo = new EditBox(font, width / 2 - 50, height / 2 - 10, 100, 20, pregunta);
        campo.setValue(String.valueOf(inicial));
        addRenderableWidget(campo);
        setInitialFocus(campo);
        addRenderableWidget(Button.builder(CommonComponents.GUI_OK, b -> aceptar()).bounds(width / 2 - 102, height / 2 + 20, 100, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> minecraft.gui.setScreen(anterior)).bounds(width / 2 + 2, height / 2 + 20, 100, 20).build());
    }

    private void aceptar() {
        int n = PantallaMenu.numero(campo.getValue(), inicial);
        minecraft.gui.setScreen(anterior);
        alAceptar.accept(Math.max(1, n));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.centeredText(font, title, width / 2, height / 2 - 50, 0xFFFFFFFF);
        g.centeredText(font, pregunta, width / 2, height / 2 - 26, 0xFFAAAAAA);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(anterior);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
