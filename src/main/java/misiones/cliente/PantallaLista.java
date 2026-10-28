package misiones.cliente;

import misiones.core.Progreso;
import misiones.modelo.Mision;
import misiones.modelo.Objetivo;
import misiones.red.Paquete;
import misiones.red.Red;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.List;

/** Read-only list of the player's missions and how far along each one is (key J). */
public class PantallaLista extends Screen implements DatosCliente.Actualizable {
    private static final int ALTO_FILA = 34;
    private int izq, der, arriba, abajo;
    private double scroll = 0;
    private long ultimoPedido = 0;

    public PantallaLista() {
        super(Component.translatable("misiones.pantalla.lista"));
    }

    @Override
    protected void init() {
        int ancho = Math.min(width - 20, 380);
        izq = (width - ancho) / 2;
        der = izq + ancho;
        arriba = 34;
        abajo = height - 36;
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(width / 2 - 75, height - 28, 150, 20).build());
        pedir();
    }

    private void pedir() {
        ultimoPedido = System.currentTimeMillis();
        Red.alServidor(Paquete.de("pedir"));
    }

    @Override
    public void actualizar() {
        scroll = Mth.clamp(scroll, 0, maxScroll());
    }

    private int maxScroll() {
        return Math.max(0, DatosCliente.misiones.size() * ALTO_FILA - (abajo - arriba));
    }

    /** 0..1 of how much of the mission is done. */
    public static float avance(Mision m) {
        if (m.completada) return 1F;
        long hecho = 0, total = 0;
        for (Objetivo o : m.objetivos) {
            hecho += Math.min(o.progreso, o.cantidad);
            total += o.cantidad;
        }
        return total == 0 ? 0F : (float) hecho / total;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        if (System.currentTimeMillis() - ultimoPedido > 1000) pedir();

        List<Mision> lista = DatosCliente.misiones;
        long hechas = lista.stream().filter(m -> m.completada).count();
        g.centeredText(font, Component.translatable("misiones.titulo", hechas, lista.size()).withStyle(ChatFormatting.GOLD), width / 2, 10, -1);
        MutableComponent info = Component.empty();
        int limite = DatosCliente.ajustes.getIntOr("limite", 0);
        if (limite > 0) info.append(Component.translatable("misiones.limite.info", DatosCliente.activas, limite));
        if (DatosCliente.espera > 0) info.append("   ").append(Component.translatable("misiones.pantalla.espera", DatosCliente.espera));
        g.centeredText(font, info.withStyle(ChatFormatting.GRAY), width / 2, 21, -1);

        g.fill(izq, arriba, der, abajo, 0x88000000);
        if (lista.isEmpty()) {
            g.centeredText(font, Component.translatable("misiones.pantalla.vacia").withStyle(ChatFormatting.GRAY), width / 2, arriba + 20, -1);
            return;
        }

        Component tooltip = null;
        g.enableScissor(izq, arriba, der, abajo);
        int y0 = arriba - (int) scroll;
        for (int i = 0; i < lista.size(); i++) {
            int y = y0 + i * ALTO_FILA;
            if (y + ALTO_FILA < arriba || y > abajo) continue;
            Mision m = lista.get(i);
            boolean encima = mouseX >= izq && mouseX < der && mouseY >= Math.max(y, arriba) && mouseY < Math.min(y + ALTO_FILA, abajo);
            if (encima) g.fill(izq, y, der, y + ALTO_FILA, 0x22FFFFFF);
            if (m.completada) g.fill(izq, y, der, y + ALTO_FILA, 0x2200FF00);

            g.item(Iconos.de(m), izq + 4, y + 8);

            // First line: number, difficulty, daily / chain marks
            MutableComponent cabeza = Component.literal("#" + (i + 1) + " ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal("[").append(Component.translatable(m.dificultad.clave())).append("]").withStyle(m.dificultad.color));
            if (m.diaria) cabeza.append(Component.literal(" ").append(Component.translatable("misiones.pantalla.diaria")).withStyle(ChatFormatting.AQUA));
            if (!m.cadena.isEmpty()) cabeza.append(Component.literal(" ⛓ " + m.cadena).withStyle(ChatFormatting.LIGHT_PURPLE));
            if (m.completada) cabeza.append(Component.literal("  ✔ ").withStyle(ChatFormatting.GREEN)).append(Component.translatable("misiones.pantalla.cumplida").withStyle(ChatFormatting.GREEN));
            g.text(font, cabeza, izq + 26, y + 3, -1);

            // Second line: what it asks for (cut to fit, full text on hover)
            MutableComponent desc = Progreso.descripcion(m, false);
            if (!m.recompensas.isEmpty()) desc.append(Component.literal("  ").append(Component.translatable("misiones.pantalla.premio", Progreso.recompensas(m))).withStyle(ChatFormatting.AQUA));
            int anchoTexto = der - izq - 34;
            List<FormattedCharSequence> lineas = font.split(desc, anchoTexto);
            if (!lineas.isEmpty()) g.text(font, lineas.get(0), izq + 26, y + 14, m.completada ? 0xFF888888 : 0xFFFFFFFF);
            if (encima && lineas.size() > 1) tooltip = desc;

            // Progress bar
            int barraIzq = izq + 26, barraDer = der - 8, barraY = y + 26;
            float avance = avance(m);
            g.fill(barraIzq, barraY, barraDer, barraY + 3, 0xFF333333);
            g.fill(barraIzq, barraY, barraIzq + (int) ((barraDer - barraIzq) * avance), barraY + 3, m.completada ? 0xFF55FF55 : 0xFFFFAA00);
        }
        g.disableScissor();

        if (maxScroll() > 0) {
            int alto = abajo - arriba;
            int barra = Math.max(20, alto * alto / (lista.size() * ALTO_FILA));
            int pos = arriba + (int) ((alto - barra) * (scroll / maxScroll()));
            g.fill(der - 3, pos, der, pos + barra, 0xFFAAAAAA);
        }
        if (tooltip != null) g.setTooltipForNextFrame(font, font.split(tooltip, 250), mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Mth.clamp(scroll - scrollY * ALTO_FILA, 0, maxScroll());
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
