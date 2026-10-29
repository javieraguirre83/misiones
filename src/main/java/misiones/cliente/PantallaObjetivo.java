package misiones.cliente;

import misiones.core.Cosas;
import misiones.modelo.Tipo;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Defines one thing a new mission asks for: what, how many, and (for items) the enchantments
 * and armor trim it must have.
 */
public class PantallaObjetivo extends Screen {

    /** The finished thing: the text saved in the mission, the amount, and how to show it. */
    public record Cosa(String objetivo, int cantidad, Component nombre, ItemStack icono) {}

    private record Encanto(String id, int nivel, Component nombre) {}

    private final Screen anterior;
    private final Tipo tipo;
    private final Consumer<Cosa> alListo;

    private String clase = "estructura";
    private Listas.Entrada elegido = null;
    private String cantidadTxt = "1";
    private final List<Encanto> encantos = new ArrayList<>();
    /** null = no trim required, "*" = any trim, otherwise a trim pattern id. */
    private Listas.Entrada adorno = null;
    private int izq, der;

    public PantallaObjetivo(Screen anterior, Tipo tipo, Consumer<Cosa> alListo) {
        super(Component.translatable("misiones.obj.titulo", Component.translatable("misiones.menu.tipo." + tipo.id)));
        this.anterior = anterior;
        this.tipo = tipo;
        this.alListo = alListo;
        if (tipo == Tipo.VIAJAR) cantidadTxt = "1000";
        if (tipo == Tipo.SOBREVIVIR) cantidadTxt = "3";
        if (tipo == Tipo.NIVEL) cantidadTxt = "10";
    }

    // ---------- what this type needs ----------

    private boolean necesitaElegir() {
        if (tipo == Tipo.SOBREVIVIR || tipo == Tipo.NIVEL) return false;
        return tipo != Tipo.EXPLORAR || !clase.startsWith("altura");
    }

    private boolean necesitaNumero() {
        if (tipo == Tipo.LOGROS) return false;
        return tipo != Tipo.EXPLORAR || clase.startsWith("altura") || clase.equals("estructura");
    }

    /** Enchantments make sense only for items you hold, store or wear. */
    private boolean permiteEncantos() {
        return switch (tipo) {
            case CONSEGUIR, COFRE, EQUIPAR, ENCANTAR, PESCAR -> true;
            case COMERCIAR -> elegido != null && !Cosas.esMob(elegido.id()) && !elegido.id().equals("*");
            default -> false;
        };
    }

    private boolean permiteAdorno() {
        return tipo == Tipo.CONSEGUIR || tipo == Tipo.COFRE || tipo == Tipo.EQUIPAR;
    }

    private String etiquetaNumero() {
        return switch (tipo) {
            case VIAJAR -> elegido != null && elegido.id().equals("saltar") ? "misiones.menu.saltos" : "misiones.menu.bloques";
            case SOBREVIVIR -> "misiones.menu.dias";
            case NIVEL -> "misiones.menu.nivel";
            case EXPLORAR -> clase.equals("estructura") ? "misiones.menu.cuantas_distintas" : "misiones.menu.altura_y";
            default -> "misiones.menu.cantidad";
        };
    }

    private List<Listas.Entrada> opciones() {
        List<Listas.Entrada> r = new ArrayList<>();
        switch (tipo) {
            case CONSEGUIR, COFRE, FABRICAR, PESCAR, COMER, FUNDIR, ENCANTAR, EQUIPAR -> { r.add(Listas.cualquiera()); r.addAll(Listas.objetos()); }
            case COMERCIAR -> { r.add(Listas.cualquiera()); r.addAll(Listas.comerciantes()); r.addAll(Listas.objetos()); }
            case MINAR, COSECHAR -> { r.add(Listas.cualquiera()); r.addAll(Listas.bloques()); }
            case MATAR, CRIAR, DOMESTICAR -> { r.add(Listas.cualquiera()); r.addAll(Listas.mobs()); }
            case POCIONES -> { r.add(Listas.cualquiera()); r.addAll(Listas.pociones()); }
            case EXPLORAR -> r.addAll(switch (clase) {
                case "dimension" -> Listas.dimensiones();
                case "bioma" -> Listas.biomas();
                default -> Listas.estructuras();
            });
            case VIAJAR -> r.addAll(Listas.viajes());
            case LOGROS -> r.addAll(Listas.logros());
            default -> { }
        }
        return r;
    }

    // ---------- screen ----------

    private int fila(int i) {
        return 30 + i * 24;
    }

    @Override
    protected void init() {
        int ancho = Math.min(width - 20, 380);
        izq = (width - ancho) / 2;
        der = izq + ancho;
        int x = izq + 110, w = Math.min(220, der - x);
        int f = 0;

        if (tipo == Tipo.EXPLORAR) {
            addRenderableWidget(CycleButton.builder((String c) -> Component.translatable("misiones.menu.clase." + c), clase)
                    .withValues(List.of("estructura", "bioma", "dimension", "altura_abajo", "altura_arriba")).displayOnlyValue()
                    .create(x, fila(f), w, 20, Component.translatable("misiones.menu.clase"), (b, v) -> { clase = v; elegido = null; rebuildWidgets(); }));
        }
        f++;
        if (necesitaElegir()) {
            Component texto = elegido == null ? Component.translatable("misiones.menu.elegir") : elegido.nombre();
            addRenderableWidget(Button.builder(texto, b -> minecraft.gui.setScreen(new PantallaElegir(this, title, opciones(), e -> {
                elegido = e;
                if (!permiteEncantos()) encantos.clear();
                rebuildWidgets();
            }))).bounds(x, fila(f), w, 20).build());
        }
        f++;
        if (necesitaNumero()) {
            EditBox e = new EditBox(font, x, fila(f), 80, 20, Component.empty());
            e.setValue(cantidadTxt);
            e.setResponder(s -> cantidadTxt = s);
            addRenderableWidget(e);
        }
        f++;
        if (permiteEncantos()) {
            addRenderableWidget(Button.builder(Component.translatable("misiones.obj.mas_encantamiento"), b -> elegirEncanto())
                    .bounds(x, fila(f), 130, 20).build());
            Button quitar = Button.builder(Component.translatable("misiones.obj.quitar"), b -> { encantos.clear(); rebuildWidgets(); })
                    .bounds(x + 134, fila(f), Math.max(60, w - 134), 20).build();
            quitar.active = !encantos.isEmpty();
            addRenderableWidget(quitar);
        }
        f++;
        if (permiteAdorno()) {
            Component texto = adorno == null ? Component.translatable("misiones.obj.sin_adorno") : adorno.nombre();
            addRenderableWidget(Button.builder(texto, b -> elegirAdorno()).bounds(x, fila(f + 1), w, 20).build());
        }

        Button listo = Button.builder(CommonComponents.GUI_DONE, b -> listo()).bounds(width / 2 - 102, height - 28, 100, 20).build();
        listo.active = !necesitaElegir() || elegido != null;
        addRenderableWidget(listo);
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> minecraft.gui.setScreen(anterior))
                .bounds(width / 2 + 2, height - 28, 100, 20).build());
    }

    private void elegirEncanto() {
        minecraft.gui.setScreen(new PantallaElegir(this, Component.translatable("misiones.menu.elegir_encantamiento"), Listas.encantamientos(), e ->
                minecraft.gui.setScreen(new PantallaCantidad(this, e.nombre().copy(), Component.translatable("misiones.menu.nivel_minimo"), 1, n -> {
                    encantos.removeIf(x -> x.id().equals(e.id()));
                    encantos.add(new Encanto(e.id(), n, e.nombre()));
                }))));
    }

    private void elegirAdorno() {
        List<Listas.Entrada> opciones = new ArrayList<>();
        opciones.add(new Listas.Entrada("", Component.translatable("misiones.obj.sin_adorno"), new ItemStack(Items.BARRIER)));
        opciones.add(new Listas.Entrada("*", Component.translatable("misiones.obj.cualquier_adorno"), new ItemStack(Items.SMITHING_TABLE)));
        opciones.addAll(Listas.adornos());
        minecraft.gui.setScreen(new PantallaElegir(this, Component.translatable("misiones.obj.adorno"), opciones, e -> {
            adorno = e.id().isEmpty() ? null : e;
            rebuildWidgets();
        }));
    }

    /** Builds the objective text, e.g. minecraft:diamond_boots[minecraft:feather_falling=4,adorno=*]. */
    private void listo() {
        int n = PantallaMenu.numero(cantidadTxt, 1);
        String objetivo = elegido == null ? "*" : elegido.id();
        int cantidad = Math.max(1, n);
        Component nombre = elegido == null ? Component.empty() : elegido.nombre();
        ItemStack icono = elegido == null ? Iconos.tipo(tipo) : elegido.icono();
        if (tipo == Tipo.EXPLORAR && clase.startsWith("altura")) {
            objetivo = (clase.equals("altura_abajo") ? "altura:<=" : "altura:>=") + n;
            nombre = Component.translatable(clase.equals("altura_abajo") ? "misiones.lugar.abajo" : "misiones.lugar.arriba", n);
            cantidad = 1;
        }
        if (tipo == Tipo.EXPLORAR && !clase.equals("estructura") || tipo == Tipo.LOGROS) cantidad = 1;
        if (tipo == Tipo.SOBREVIVIR) nombre = Component.translatable("misiones.dias", 0, cantidad);
        if (tipo == Tipo.NIVEL) nombre = Component.translatable("misiones.nivel", 0, cantidad);

        List<String> detalles = new ArrayList<>();
        for (Encanto e : encantos) detalles.add(e.id() + "=" + e.nivel());
        if (adorno != null) detalles.add("adorno=" + adorno.id());
        if (!detalles.isEmpty()) objetivo = objetivo + "[" + String.join(",", detalles) + "]";

        MutableComponent texto = nombre.copy();
        if (!encantos.isEmpty() || adorno != null) {
            texto.append(" (");
            boolean primero = true;
            for (Encanto e : encantos) {
                if (!primero) texto.append(", ");
                primero = false;
                texto.append(e.nombre()).append(" ").append(Component.translatableWithFallback("enchantment.level." + e.nivel(), String.valueOf(e.nivel())));
            }
            if (adorno != null) {
                if (!primero) texto.append(", ");
                texto.append(adorno.id().equals("*") ? Component.translatable("misiones.adorno") : adorno.nombre());
            }
            texto.append(")");
        }
        Cosa cosa = new Cosa(objetivo, cantidad, texto, icono);
        minecraft.gui.setScreen(anterior);
        alListo.accept(cosa);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.centeredText(font, title, width / 2, 12, 0xFFFFFFFF);
        int f = 0;
        if (tipo == Tipo.EXPLORAR) g.text(font, Component.translatable("misiones.menu.clase"), izq, fila(f) + 6, 0xFFFFFFFF);
        f++;
        if (necesitaElegir()) g.text(font, Component.translatable("misiones.menu.que"), izq, fila(f) + 6, 0xFFFFFFFF);
        f++;
        if (necesitaNumero()) g.text(font, Component.translatable(etiquetaNumero()), izq, fila(f) + 6, 0xFFFFFFFF);
        f++;
        if (permiteEncantos()) {
            g.text(font, Component.translatable("misiones.obj.encantamientos"), izq, fila(f) + 6, 0xFFFFFFFF);
            MutableComponent lista = Component.empty();
            if (encantos.isEmpty()) lista.append(Component.translatable("misiones.obj.ninguno"));
            for (int i = 0; i < encantos.size(); i++) {
                Encanto e = encantos.get(i);
                if (i > 0) lista.append(", ");
                lista.append(e.nombre()).append(" ").append(Component.translatableWithFallback("enchantment.level." + e.nivel(), String.valueOf(e.nivel())));
            }
            g.textWithWordWrap(font, lista.withStyle(ChatFormatting.AQUA), izq + 110, fila(f) + 23, der - izq - 110, -1);
        }
        f++;
        if (permiteAdorno()) g.text(font, Component.translatable("misiones.obj.adorno"), izq, fila(f + 1) + 6, 0xFFFFFFFF);
        g.textWithWordWrap(font, Component.translatable("misiones.menu.ayuda." + tipo.id).withStyle(ChatFormatting.GRAY),
                izq, height - 58, der - izq, 0xFFAAAAAA);
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
