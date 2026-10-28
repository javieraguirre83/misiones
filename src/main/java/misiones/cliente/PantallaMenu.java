package misiones.cliente;

import misiones.core.Progreso;
import misiones.modelo.Dificultad;
import misiones.modelo.Mision;
import misiones.modelo.Tipo;
import misiones.red.Paquete;
import misiones.red.Red;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** The missions menu (key M): my missions, generate, add, chains and settings. */
public class PantallaMenu extends Screen implements DatosCliente.Actualizable {
    private enum Pestana { MISIONES, GENERAR, AGREGAR, CADENAS, AJUSTES }

    private static final int ALTO_FILA = 22;
    private static Pestana pestana = Pestana.MISIONES;

    private int izq, der, arriba;
    private long ultimoPedido = 0;

    // My missions
    private int seleccion = -1;
    private double scroll = 0;
    private boolean confirmarTodas = false;

    // Generate
    private String totalTxt = "5", facilTxt = "3", mediaTxt = "2", dificilTxt = "0";

    // Add
    private static Tipo tipo = Tipo.CONSEGUIR;
    private static String clase = "estructura";
    private static Dificultad dificultad = Dificultad.MEDIA;
    private Listas.Entrada elegido = null;
    private String cantidadTxt = "1";
    private String textoLibre = "";

    // Chains
    private int cadenaSel = 0;

    // Settings
    private boolean ajustesCargados = false;
    private String modo = "balanceado";
    private String pFacil = "50", pMedia = "33", pDificil = "17", limiteTxt = "10";
    private boolean diaria = true, repartir = true;
    private String difDiaria = "azar";

    public PantallaMenu() {
        super(Component.translatable("misiones.menu.titulo"));
    }

    public static int numero(String texto, int porDefecto) {
        try {
            return Integer.parseInt(texto.trim());
        } catch (Exception e) {
            return porDefecto;
        }
    }

    private void enviar(String accion, Consumer<CompoundTag> datos) {
        Paquete p = Paquete.de(accion);
        datos.accept(p.datos());
        Red.alServidor(p);
        ultimoPedido = System.currentTimeMillis();
    }

    private void pedir() {
        enviar("pedir", d -> { });
    }

    @Override
    public void actualizar() {
        if (pestana == Pestana.AJUSTES && !ajustesCargados) rebuildWidgets();
        if (seleccion >= DatosCliente.misiones.size()) seleccion = -1;
        scroll = Mth.clamp(scroll, 0, maxScroll());
    }

    // ---------- layout ----------

    private int fila(int i) {
        return arriba + 6 + i * 24;
    }

    private int abajoLista() {
        return height - 60;
    }

    @Override
    protected void init() {
        int ancho = Math.min(width - 20, 420);
        izq = (width - ancho) / 2;
        der = izq + ancho;
        arriba = 32;

        Pestana[] todas = Pestana.values();
        int anchoTab = ancho / todas.length;
        for (int i = 0; i < todas.length; i++) {
            Pestana p = todas[i];
            Button b = Button.builder(Component.translatable("misiones.menu.pestana." + p.name().toLowerCase()), x -> {
                pestana = p;
                confirmarTodas = false;
                rebuildWidgets();
            }).bounds(izq + i * anchoTab, 6, anchoTab - 2, 20).build();
            b.active = p != pestana;
            addRenderableWidget(b);
        }

        switch (pestana) {
            case MISIONES -> initMisiones();
            case GENERAR -> initGenerar();
            case AGREGAR -> initAgregar();
            case CADENAS -> initCadenas();
            case AJUSTES -> initAjustes();
        }
        if (ultimoPedido == 0) {
            Red.alServidor(Paquete.de("listas"));
            pedir();
        }
    }

    private Button boton(String clave, int x, int y, int w, Button.OnPress accion) {
        return addRenderableWidget(Button.builder(Component.translatable(clave), accion).bounds(x, y, w, 20).build());
    }

    private EditBox campo(int x, int y, int w, String valor, Consumer<String> alCambiar) {
        EditBox e = new EditBox(font, x, y, w, 20, Component.empty());
        e.setValue(valor);
        e.setResponder(alCambiar);
        return addRenderableWidget(e);
    }

    // ---------- tab: my missions ----------

    private void initMisiones() {
        int ancho = der - izq;
        int w = ancho / 5;
        boolean hay = seleccion >= 0 && seleccion < DatosCliente.misiones.size();
        int y1 = height - 56, y2 = height - 33;
        boton("misiones.menu.completar", izq, y1, w - 2, b -> enviar("completar", d -> d.putInt("n", seleccion + 1))).active = hay;
        boton("misiones.menu.borrar", izq + w, y1, w - 2, b -> {
            enviar("borrar", d -> d.putInt("n", seleccion + 1));
            seleccion = -1;
            rebuildWidgets();
        }).active = hay;
        boton("misiones.menu.premio", izq + 2 * w, y1, w - 2, b -> elegirPremio()).active = hay;
        boton("misiones.menu.encantamiento", izq + 3 * w, y1, w - 2, b -> elegirEncantamiento()).active = hay;
        boton("misiones.menu.adorno", izq + 4 * w, y1, w - 2, b -> enviar("adorno", d -> d.putInt("n", seleccion + 1))).active = hay;

        int w2 = ancho / 3;
        boton("misiones.menu.limpiar_hechas", izq, y2, w2 - 2, b -> enviar("limpiarHechas", d -> { }));
        boton(confirmarTodas ? "misiones.menu.seguro" : "misiones.menu.borrar_todas", izq + w2, y2, w2 - 2, b -> {
            if (confirmarTodas) {
                enviar("limpiar", d -> { });
                seleccion = -1;
            }
            confirmarTodas = !confirmarTodas;
            rebuildWidgets();
        });
        boton("misiones.menu.ver_lista", izq + 2 * w2, y2, w2 - 2, b -> minecraft.gui.setScreen(new PantallaLista()));
    }

    private void elegirPremio() {
        int n = seleccion + 1;
        List<Listas.Entrada> opciones = new ArrayList<>();
        opciones.add(new Listas.Entrada("xp", Component.translatable("misiones.menu.xp"), Iconos.item("minecraft:experience_bottle")));
        opciones.addAll(Listas.objetos());
        minecraft.gui.setScreen(new PantallaElegir(this, Component.translatable("misiones.menu.elegir_premio"), opciones, e ->
                minecraft.gui.setScreen(new PantallaCantidad(this, Component.translatable("misiones.menu.premio"),
                        e.id().equals("xp") ? Component.translatable("misiones.menu.cuantos_niveles") : Component.translatable("misiones.menu.cuantos"), 1,
                        c -> enviar("recompensa", d -> { d.putInt("n", n); d.putString("objeto", e.id()); d.putInt("cantidad", c); })))));
    }

    private void elegirEncantamiento() {
        int n = seleccion + 1;
        minecraft.gui.setScreen(new PantallaElegir(this, Component.translatable("misiones.menu.elegir_encantamiento"), Listas.encantamientos(), e ->
                minecraft.gui.setScreen(new PantallaCantidad(this, Component.translatable("misiones.menu.encantamiento"),
                        Component.translatable("misiones.menu.nivel_minimo"), 1,
                        c -> enviar("encantamiento", d -> { d.putInt("n", n); d.putString("encantamiento", e.id()); d.putInt("nivel", c); })))));
    }

    private int maxScroll() {
        return Math.max(0, DatosCliente.misiones.size() * ALTO_FILA - (abajoLista() - arriba));
    }

    private void dibujarMisiones(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        List<Mision> lista = DatosCliente.misiones;
        int abajo = abajoLista();
        g.fill(izq, arriba, der, abajo, 0x88000000);
        if (lista.isEmpty()) {
            g.centeredText(font, Component.translatable("misiones.menu.sin_misiones").withStyle(ChatFormatting.GRAY), width / 2, arriba + 12, -1);
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
            if (i == seleccion) g.fill(izq, y, der, y + ALTO_FILA, 0x55FFFFFF);
            else if (encima) g.fill(izq, y, der, y + ALTO_FILA, 0x22FFFFFF);
            g.item(Iconos.de(m), izq + 3, y + 2);
            MutableComponent linea = Component.literal("#" + (i + 1) + " ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal("[").append(Component.translatable(m.dificultad.clave())).append("] ").withStyle(m.dificultad.color));
            if (m.completada) linea.append(Component.literal("✔ ").withStyle(ChatFormatting.GREEN));
            MutableComponent desc = Progreso.descripcion(m, false);
            if (!m.recompensas.isEmpty()) desc.append(Component.literal("  ").append(Component.translatable("misiones.pantalla.premio", Progreso.recompensas(m))).withStyle(ChatFormatting.AQUA));
            linea.append(desc);
            List<FormattedCharSequence> partes = font.split(linea, der - izq - 30);
            if (!partes.isEmpty()) g.text(font, partes.get(0), izq + 23, y + 4, m.completada ? 0xFF888888 : 0xFFFFFFFF);
            if (encima && partes.size() > 1) tooltip = linea;
            float avance = PantallaLista.avance(m);
            g.fill(izq + 23, y + 16, der - 6, y + 18, 0xFF333333);
            g.fill(izq + 23, y + 16, izq + 23 + (int) ((der - 6 - izq - 23) * avance), y + 18, m.completada ? 0xFF55FF55 : 0xFFFFAA00);
        }
        g.disableScissor();
        if (tooltip != null) g.setTooltipForNextFrame(font, font.split(tooltip, 260), mouseX, mouseY);
        if (seleccion < 0) g.centeredText(font, Component.translatable("misiones.menu.selecciona").withStyle(ChatFormatting.GRAY), width / 2, abajo + 1, -1);
    }

    // ---------- tab: generate ----------

    private void initGenerar() {
        int x = izq + 110;
        campo(x, fila(2), 50, totalTxt, s -> totalTxt = s);
        boton("misiones.menu.generar", x + 56, fila(2), 120, b -> enviar("generar", d -> d.putInt("total", Math.max(1, numero(totalTxt, 5)))));
        campo(x, fila(4), 40, facilTxt, s -> facilTxt = s);
        campo(x + 44, fila(4), 40, mediaTxt, s -> mediaTxt = s);
        campo(x + 88, fila(4), 40, dificilTxt, s -> dificilTxt = s);
        boton("misiones.menu.generar_asi", x + 134, fila(4), 120, b -> enviar("generar", d -> {
            d.putInt("facil", Math.max(0, numero(facilTxt, 0)));
            d.putInt("media", Math.max(0, numero(mediaTxt, 0)));
            d.putInt("dificil", Math.max(0, numero(dificilTxt, 0)));
        }));
    }

    private void dibujarGenerar(GuiGraphicsExtractor g) {
        CompoundTag a = DatosCliente.ajustes;
        String m = a.getStringOr("modo", "balanceado");
        Component modoTxt = m.equals("azar") ? Component.translatable("misiones.modo.azar")
                : Component.translatable("misiones.menu.modo_balanceado", a.getIntOr("facil", 50), a.getIntOr("media", 33), a.getIntOr("dificil", 17));
        g.text(font, Component.translatable("misiones.menu.modo_actual", modoTxt), izq, fila(0), 0xFFFFFFFF);
        int limite = a.getIntOr("limite", 0);
        Component libres = limite > 0 ? Component.translatable("misiones.menu.libres", Math.max(0, limite - DatosCliente.activas), limite)
                : Component.translatable("misiones.sin_limite");
        g.text(font, libres, izq, fila(0) + 11, 0xFFAAAAAA);
        g.text(font, Component.translatable("misiones.menu.total"), izq, fila(2) + 6, 0xFFFFFFFF);
        g.text(font, Component.translatable("misiones.menu.por_dificultad"), izq, fila(4) + 6, 0xFFFFFFFF);
        int x = izq + 110;
        g.text(font, Component.translatable("misiones.dificultad.facil").withStyle(ChatFormatting.GREEN), x, fila(4) - 10, -1);
        g.text(font, Component.translatable("misiones.dificultad.media").withStyle(ChatFormatting.YELLOW), x + 44, fila(4) - 10, -1);
        g.text(font, Component.translatable("misiones.dificultad.dificil").withStyle(ChatFormatting.RED), x + 88, fila(4) - 10, -1);
        g.textWithWordWrap(font, Component.translatable("misiones.menu.ayuda_generar").withStyle(ChatFormatting.GRAY), izq, fila(6), der - izq, 0xFFAAAAAA);
    }

    // ---------- tab: add ----------

    private boolean necesitaElegir() {
        if (tipo == Tipo.SOBREVIVIR || tipo == Tipo.NIVEL || tipo == Tipo.LIBRE) return false;
        return tipo != Tipo.EXPLORAR || !clase.startsWith("altura");
    }

    private boolean necesitaNumero() {
        if (tipo == Tipo.LOGROS || tipo == Tipo.LIBRE) return false;
        return tipo != Tipo.EXPLORAR || clase.startsWith("altura");
    }

    private String etiquetaNumero() {
        return switch (tipo) {
            case VIAJAR -> elegido != null && elegido.id().equals("saltar") ? "misiones.menu.saltos" : "misiones.menu.bloques";
            case SOBREVIVIR -> "misiones.menu.dias";
            case NIVEL -> "misiones.menu.nivel";
            case EXPLORAR -> "misiones.menu.altura_y";
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

    private void initAgregar() {
        int x = izq + 100, w = Math.min(220, der - x);
        addRenderableWidget(CycleButton.builder((Tipo t) -> Component.translatable("misiones.menu.tipo." + t.id), tipo)
                .withValues(List.of(Tipo.values()))
                .create(x, fila(0), w, 20, Component.translatable("misiones.menu.tipo"), (b, v) -> {
                    tipo = v;
                    elegido = null;
                    rebuildWidgets();
                }));
        if (tipo == Tipo.EXPLORAR) {
            addRenderableWidget(CycleButton.builder((String c) -> Component.translatable("misiones.menu.clase." + c), clase)
                    .withValues(List.of("estructura", "bioma", "dimension", "altura_abajo", "altura_arriba"))
                    .create(x, fila(1), w, 20, Component.translatable("misiones.menu.clase"), (b, v) -> {
                        clase = v;
                        elegido = null;
                        rebuildWidgets();
                    }));
        }
        if (necesitaElegir()) {
            Component texto = elegido == null ? Component.translatable("misiones.menu.elegir") : elegido.nombre();
            addRenderableWidget(Button.builder(texto, b -> minecraft.gui.setScreen(new PantallaElegir(this,
                    Component.translatable("misiones.menu.tipo." + tipo.id), opciones(), e -> { elegido = e; rebuildWidgets(); })))
                    .bounds(x, fila(2), w, 20).build());
        }
        if (necesitaNumero()) campo(x, fila(3), 80, cantidadTxt, s -> cantidadTxt = s);
        if (tipo == Tipo.LIBRE) {
            EditBox e = campo(x, fila(3), Math.min(260, der - x), textoLibre, s -> textoLibre = s);
            e.setMaxLength(200);
        }
        addRenderableWidget(CycleButton.builder((Dificultad d) -> Component.translatable(d.clave()), dificultad)
                .withValues(List.of(Dificultad.values()))
                .create(x, fila(4), w, 20, Component.translatable("misiones.menu.dificultad"), (b, v) -> dificultad = v));
        Button agregar = boton("misiones.menu.agregar", x, fila(5) + 4, w, b -> agregar());
        agregar.active = !necesitaElegir() || elegido != null;
    }

    private void agregar() {
        int n = numero(cantidadTxt, 1);
        enviar("agregar", d -> {
            d.putString("tipo", tipo.id);
            d.putString("dificultad", dificultad.id);
            String objetivo = elegido == null ? "*" : elegido.id();
            int cantidad = Math.max(1, n);
            if (tipo == Tipo.EXPLORAR) {
                cantidad = 1;
                if (clase.equals("altura_abajo")) objetivo = "altura:<=" + n;
                if (clase.equals("altura_arriba")) objetivo = "altura:>=" + n;
            }
            if (tipo == Tipo.LOGROS) cantidad = 1;
            if (tipo == Tipo.LIBRE) d.putString("texto", textoLibre.isBlank() ? "?" : textoLibre);
            d.putString("objetivo", objetivo);
            d.putInt("cantidad", cantidad);
        });
    }

    private void dibujarAgregar(GuiGraphicsExtractor g) {
        g.text(font, Component.translatable("misiones.menu.tipo"), izq, fila(0) + 6, 0xFFFFFFFF);
        if (tipo == Tipo.EXPLORAR) g.text(font, Component.translatable("misiones.menu.clase"), izq, fila(1) + 6, 0xFFFFFFFF);
        if (necesitaElegir()) g.text(font, Component.translatable("misiones.menu.que"), izq, fila(2) + 6, 0xFFFFFFFF);
        if (necesitaNumero()) g.text(font, Component.translatable(etiquetaNumero()), izq, fila(3) + 6, 0xFFFFFFFF);
        if (tipo == Tipo.LIBRE) g.text(font, Component.translatable("misiones.menu.texto"), izq, fila(3) + 6, 0xFFFFFFFF);
        g.text(font, Component.translatable("misiones.menu.dificultad"), izq, fila(4) + 6, 0xFFFFFFFF);
        g.textWithWordWrap(font, Component.translatable("misiones.menu.ayuda." + tipo.id).withStyle(ChatFormatting.GRAY),
                izq, fila(6) + 6, der - izq, 0xFFAAAAAA);
    }

    // ---------- tab: chains ----------

    private void initCadenas() {
        List<DatosCliente.Cadena> cadenas = DatosCliente.cadenas;
        if (cadenaSel >= cadenas.size()) cadenaSel = 0;
        for (int i = 0; i < cadenas.size() && i < 8; i++) {
            int idx = i;
            Button b = Button.builder(Component.literal("⛓ " + cadenas.get(i).nombre()), x -> { cadenaSel = idx; rebuildWidgets(); })
                    .bounds(izq, fila(i), 110, 20).build();
            b.active = i != cadenaSel;
            addRenderableWidget(b);
        }
        if (!cadenas.isEmpty()) {
            boton("misiones.menu.empezar_cadena", izq + 120, height - 56, der - izq - 120, b ->
                    enviar("cadena", d -> d.putString("nombre", cadenas.get(cadenaSel).nombre())));
        }
    }

    private void dibujarCadenas(GuiGraphicsExtractor g) {
        List<DatosCliente.Cadena> cadenas = DatosCliente.cadenas;
        if (cadenas.isEmpty()) {
            g.centeredText(font, Component.translatable("misiones.menu.sin_cadenas").withStyle(ChatFormatting.GRAY), width / 2, arriba + 20, -1);
            return;
        }
        DatosCliente.Cadena c = cadenas.get(Math.min(cadenaSel, cadenas.size() - 1));
        int x = izq + 120, y = fila(0);
        g.text(font, Component.literal("⛓ " + c.nombre()).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), x, y, -1);
        y += 14;
        for (int i = 0; i < c.pasos().size(); i++) {
            Mision m = c.pasos().get(i);
            g.item(Iconos.de(m), x, y - 4);
            MutableComponent t = Component.literal((i + 1) + ". ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal("[").append(Component.translatable(m.dificultad.clave())).append("] ").withStyle(m.dificultad.color))
                    .append(Progreso.descripcion(m, false));
            List<FormattedCharSequence> partes = font.split(t, der - x - 20);
            if (!partes.isEmpty()) g.text(font, partes.get(0), x + 18, y, 0xFFFFFFFF);
            y += 20;
            if (y > height - 70) break;
        }
    }

    // ---------- tab: settings ----------

    private void cargarAjustes() {
        CompoundTag a = DatosCliente.ajustes;
        if (a.isEmpty()) return;
        modo = a.getStringOr("modo", "balanceado");
        pFacil = String.valueOf(a.getIntOr("facil", 50));
        pMedia = String.valueOf(a.getIntOr("media", 33));
        pDificil = String.valueOf(a.getIntOr("dificil", 17));
        limiteTxt = String.valueOf(a.getIntOr("limite", 10));
        diaria = a.getBooleanOr("diaria", true);
        difDiaria = a.getStringOr("dificultadDiaria", "azar");
        repartir = a.getBooleanOr("repartirTipos", true);
        ajustesCargados = true;
    }

    /** Settings are saved as soon as something changes (like Minecraft's own options). */
    private void guardarAjustes() {
        enviar("ajustes", d -> {
            d.putString("modo", modo);
            d.putInt("facil", Math.max(0, numero(pFacil, 50)));
            d.putInt("media", Math.max(0, numero(pMedia, 33)));
            d.putInt("dificil", Math.max(0, numero(pDificil, 17)));
            d.putInt("limite", Math.max(0, numero(limiteTxt, 10)));
            d.putBoolean("diaria", diaria);
            d.putString("dificultadDiaria", difDiaria);
            d.putBoolean("repartirTipos", repartir);
        });
    }

    /** A number box that saves when it holds a valid number. */
    private void campoAjuste(int x, int y, int w, String valor, Consumer<String> alCambiar) {
        campo(x, y, w, valor, s -> {
            alCambiar.accept(s);
            if (numero(s, -1) >= 0) guardarAjustes();
        });
    }

    private void initAjustes() {
        if (!ajustesCargados) cargarAjustes();
        int x = izq + 150, w = Math.min(160, der - x);
        addRenderableWidget(CycleButton.builder((String m) -> Component.translatable("misiones.modo." + m), modo)
                .withValues(List.of("balanceado", "azar")).displayOnlyValue()
                .create(x, fila(0), w, 20, Component.translatable("misiones.menu.modo"), (b, v) -> { modo = v; guardarAjustes(); }));
        campoAjuste(x, fila(1), 40, pFacil, s -> pFacil = s);
        campoAjuste(x + 44, fila(1), 40, pMedia, s -> pMedia = s);
        campoAjuste(x + 88, fila(1), 40, pDificil, s -> pDificil = s);
        campoAjuste(x, fila(2), 50, limiteTxt, s -> limiteTxt = s);
        addRenderableWidget(CycleButton.onOffBuilder(diaria).displayOnlyValue()
                .create(x, fila(3), w, 20, Component.translatable("misiones.menu.diaria"), (b, v) -> { diaria = v; guardarAjustes(); }));
        addRenderableWidget(CycleButton.builder((String d) -> d.equals("azar") ? Component.translatable("misiones.modo.azar")
                        : Component.translatable("misiones.dificultad." + d), difDiaria)
                .withValues(List.of("azar", "facil", "media", "dificil")).displayOnlyValue()
                .create(x, fila(4), w, 20, Component.translatable("misiones.menu.dificultad_diaria"), (b, v) -> { difDiaria = v; guardarAjustes(); }));
        addRenderableWidget(CycleButton.onOffBuilder(repartir).displayOnlyValue()
                .create(x, fila(5), w, 20, Component.translatable("misiones.menu.repartir"), (b, v) -> { repartir = v; guardarAjustes(); }));
        int w2 = (der - izq) / 2;
        boton("misiones.menu.abrir_carpeta", izq, height - 56, w2 - 2, b -> abrirCarpeta());
        boton("misiones.menu.recargar", izq + w2, height - 56, w2 - 2, b -> enviar("recargar", d -> { }));
    }

    private void abrirCarpeta() {
        try {
            Path carpeta = Path.of("config", "misiones").toAbsolutePath();
            Files.createDirectories(carpeta);
            Util.getPlatform().openPath(carpeta);
        } catch (Exception e) {
            DatosCliente.aviso = Component.translatable("misiones.menu.no_carpeta");
            DatosCliente.avisoHasta = System.currentTimeMillis() + 4000;
        }
    }

    private void dibujarAjustes(GuiGraphicsExtractor g) {
        String[] etiquetas = {"misiones.menu.modo", "misiones.menu.porcentajes", "misiones.menu.limite", "misiones.menu.diaria",
                "misiones.menu.dificultad_diaria", "misiones.menu.repartir"};
        for (int i = 0; i < etiquetas.length; i++) g.text(font, Component.translatable(etiquetas[i]), izq, fila(i) + 6, 0xFFFFFFFF);
        g.text(font, Component.translatable("misiones.menu.limite_ayuda").withStyle(ChatFormatting.GRAY), izq + 210, fila(2) + 6, -1);
    }

    // ---------- drawing and input ----------

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        if (System.currentTimeMillis() - ultimoPedido > 2000) pedir();
        switch (pestana) {
            case MISIONES -> dibujarMisiones(g, mouseX, mouseY);
            case GENERAR -> dibujarGenerar(g);
            case AGREGAR -> dibujarAgregar(g);
            case CADENAS -> dibujarCadenas(g);
            case AJUSTES -> dibujarAjustes(g);
        }
        if (DatosCliente.aviso != null && System.currentTimeMillis() < DatosCliente.avisoHasta) {
            g.centeredText(font, DatosCliente.aviso.copy().withStyle(ChatFormatting.YELLOW), width / 2, height - 11, -1);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doble) {
        if (super.mouseClicked(event, doble)) return true;
        if (pestana == Pestana.MISIONES) {
            double mx = event.x(), my = event.y();
            if (mx >= izq && mx < der && my >= arriba && my < abajoLista()) {
                int i = (int) ((my - arriba + scroll) / ALTO_FILA);
                if (i >= 0 && i < DatosCliente.misiones.size()) {
                    seleccion = i == seleccion ? -1 : i;
                    confirmarTodas = false;
                    rebuildWidgets();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (pestana == Pestana.MISIONES) {
            scroll = Mth.clamp(scroll - scrollY * ALTO_FILA, 0, maxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
