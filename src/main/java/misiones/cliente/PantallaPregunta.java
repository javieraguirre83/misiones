package misiones.cliente;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

/** A question with a few answer buttons (and optionally a text box, e.g. a file name). */
public class PantallaPregunta extends Screen {
    public record Opcion(Component texto, Consumer<String> accion) {}

    private final Screen anterior;
    private final Component pregunta;
    private final List<Opcion> opciones;
    private final String textoInicial;
    private EditBox campo;

    /** textoInicial = null means no text box. */
    public PantallaPregunta(Screen anterior, Component titulo, Component pregunta, String textoInicial, List<Opcion> opciones) {
        super(titulo);
        this.anterior = anterior;
        this.pregunta = pregunta;
        this.textoInicial = textoInicial;
        this.opciones = opciones;
    }

    @Override
    protected void init() {
        int y = height / 2 - 10;
        if (textoInicial != null) {
            campo = new EditBox(font, width / 2 - 100, y - 4, 200, 20, Component.empty());
            campo.setMaxLength(60);
            campo.setValue(textoInicial);
            addRenderableWidget(campo);
            setInitialFocus(campo);
            y += 26;
        }
        for (Opcion o : opciones) {
            addRenderableWidget(Button.builder(o.texto(), b -> {
                String texto = campo == null ? "" : campo.getValue();
                minecraft.gui.setScreen(anterior);
                o.accion().accept(texto);
            }).bounds(width / 2 - 100, y, 200, 20).build());
            y += 24;
        }
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> minecraft.gui.setScreen(anterior))
                .bounds(width / 2 - 100, y + 4, 200, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.centeredText(font, title, width / 2, height / 2 - 60, 0xFFFFFFFF);
        g.textWithWordWrap(font, pregunta, width / 2 - 150, height / 2 - 44, 300, 0xFFAAAAAA);
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
