package misiones.cliente;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** A searchable list to pick an item, block, mob, potion... Clicking one returns to the previous screen. */
public class PantallaElegir extends Screen {
    private static final int ALTO = 20;
    private final Screen anterior;
    private final List<Listas.Entrada> todas;
    private final Consumer<Listas.Entrada> alElegir;
    private List<Listas.Entrada> visibles = new ArrayList<>();
    private EditBox buscar;
    private String texto = "";
    private double scroll = 0;
    private int izq, der, arriba, abajo;

    public PantallaElegir(Screen anterior, Component titulo, List<Listas.Entrada> entradas, Consumer<Listas.Entrada> alElegir) {
        super(titulo);
        this.anterior = anterior;
        this.todas = entradas;
        this.alElegir = alElegir;
    }

    @Override
    protected void init() {
        int ancho = Math.min(width - 20, 320);
        izq = (width - ancho) / 2;
        der = izq + ancho;
        arriba = 46;
        abajo = height - 34;
        buscar = new EditBox(font, izq, 24, ancho, 18, Component.translatable("misiones.menu.buscar"));
        buscar.setHint(Component.translatable("misiones.menu.buscar").withStyle(ChatFormatting.DARK_GRAY));
        buscar.setValue(texto);
        buscar.setResponder(s -> { texto = s; filtrar(); });
        addRenderableWidget(buscar);
        setInitialFocus(buscar);
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> minecraft.gui.setScreen(anterior))
                .bounds(width / 2 - 75, height - 28, 150, 20).build());
        filtrar();
    }

    private void filtrar() {
        String t = texto.toLowerCase().trim();
        visibles = new ArrayList<>();
        for (Listas.Entrada e : todas) if (t.isEmpty() || e.busqueda().contains(t)) visibles.add(e);
        scroll = Mth.clamp(scroll, 0, maxScroll());
    }

    private int maxScroll() {
        return Math.max(0, visibles.size() * ALTO - (abajo - arriba));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.centeredText(font, title, width / 2, 10, 0xFFFFFFFF);
        g.fill(izq, arriba, der, abajo, 0x88000000);
        g.enableScissor(izq, arriba, der, abajo);
        int y0 = arriba - (int) scroll;
        for (int i = 0; i < visibles.size(); i++) {
            int y = y0 + i * ALTO;
            if (y + ALTO < arriba || y > abajo) continue;
            Listas.Entrada e = visibles.get(i);
            if (mouseX >= izq && mouseX < der && mouseY >= y && mouseY < y + ALTO && mouseY >= arriba && mouseY < abajo) {
                g.fill(izq, y, der, y + ALTO, 0x33FFFFFF);
            }
            if (!e.icono().isEmpty()) g.item(e.icono(), izq + 2, y + 2);
            g.text(font, e.nombre(), izq + 22, y + 6, 0xFFFFFFFF);
            String id = e.id();
            int anchoId = font.width(id);
            if (font.width(e.nombre()) + anchoId + 40 < der - izq) g.text(font, id, der - anchoId - 6, y + 6, 0xFF777777);
        }
        g.disableScissor();
        if (visibles.isEmpty()) g.centeredText(font, Component.translatable("misiones.menu.nada").withStyle(ChatFormatting.GRAY), width / 2, arriba + 10, -1);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doble) {
        if (super.mouseClicked(event, doble)) return true;
        double mx = event.x(), my = event.y();
        if (mx >= izq && mx < der && my >= arriba && my < abajo) {
            int i = (int) ((my - arriba + scroll) / ALTO);
            if (i >= 0 && i < visibles.size()) {
                Listas.Entrada e = visibles.get(i);
                minecraft.gui.setScreen(anterior);
                alElegir.accept(e);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Mth.clamp(scroll - scrollY * ALTO * 2, 0, maxScroll());
        return true;
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
