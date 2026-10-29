package misiones.comandos;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import misiones.core.Ajustes;
import misiones.core.Catalogo;
import misiones.core.Gestor;
import misiones.core.Datos;
import misiones.core.Progreso;
import misiones.modelo.Dificultad;
import misiones.modelo.Mision;
import misiones.modelo.Objetivo;
import misiones.modelo.Recompensa;
import misiones.modelo.Tipo;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.ResourceOrTagKeyArgument;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * /misiones (alias /missions)
 *   lista | generar <total> | generar <fáciles> <medias> <difíciles>
 *   agregar <tipo> ... | agregar plantilla <id> | objetivo <n> <cantidad> <cosa>
 *   recompensa <n> <cantidad> <objeto> | recompensa <n> xp <niveles>
 *   completar <n> | borrar <n> | limpiar | catalogo | recargar
 */
public final class Comandos {
    private static final SimpleCommandExceptionType SIN_MISION = new SimpleCommandExceptionType(Component.translatable("misiones.error.numero"));
    private static final SimpleCommandExceptionType NO_MULTIPLE = new SimpleCommandExceptionType(Component.translatable("misiones.error.multiple"));
    private static final DynamicCommandExceptionType SIN_PLANTILLA = new DynamicCommandExceptionType(id -> Component.translatable("misiones.error.plantilla", id));
    private static final DynamicCommandExceptionType SIN_CADENA = new DynamicCommandExceptionType(id -> Component.translatable("misiones.error.cadena", id));
    private static final DynamicCommandExceptionType INVALIDO = new DynamicCommandExceptionType(t -> Component.translatable("misiones.error.invalido", t));

    private static final SuggestionProvider<CommandSourceStack> DIFICULTADES =
            (ctx, b) -> SharedSuggestionProvider.suggest(new String[]{"facil", "media", "dificil"}, b);
    private static final SuggestionProvider<CommandSourceStack> TIPOS =
            (ctx, b) -> SharedSuggestionProvider.suggest(java.util.Arrays.stream(Tipo.values()).map(t -> t.id).toList(), b);
    private static final SuggestionProvider<CommandSourceStack> CADENAS =
            (ctx, b) -> SharedSuggestionProvider.suggest(Catalogo.cadenas(), b);
    private static final SuggestionProvider<CommandSourceStack> FORMAS_VIAJE =
            (ctx, b) -> SharedSuggestionProvider.suggest(new String[]{"caminar", "correr", "nadar", "bucear", "barco", "caballo", "cerdo",
                    "vagoneta", "elitros", "lanzador", "trepar", "agachado", "saltar", "fantasma"}, b);
    private static final SuggestionProvider<CommandSourceStack> LOGROS =
            (ctx, b) -> SharedSuggestionProvider.suggestResource(ctx.getSource().getServer().getAdvancements().getAllAdvancements().stream()
                    .filter(h -> h.value().display().isPresent()).map(h -> h.id()), b);
    private static final SuggestionProvider<CommandSourceStack> SI_NO =
            (ctx, b) -> SharedSuggestionProvider.suggest(new String[]{"si", "no"}, b);
    private static final SuggestionProvider<CommandSourceStack> PLANTILLAS =
            (ctx, b) -> SharedSuggestionProvider.suggest(Catalogo.plantillas().stream().map(m -> m.plantilla).toList(), b);

    public static void registrar(CommandDispatcher<CommandSourceStack> d, CommandBuildContext build) {
        LiteralArgumentBuilder<CommandSourceStack> raiz = Commands.literal("misiones")
                .executes(Comandos::lista)
                .then(Commands.literal("lista").executes(Comandos::lista))
                .then(Commands.literal("catalogo").executes(Comandos::catalogo)
                        .then(Commands.literal("cadenas").executes(Comandos::catalogoCadenas))
                        .then(Commands.argument("tipo", StringArgumentType.word()).suggests(TIPOS).executes(Comandos::catalogoTipo)))
                .then(Commands.literal("recargar").executes(Comandos::recargar))
                .then(Commands.literal("limpiar").executes(Comandos::limpiar)
                        .then(Commands.literal("hechas").executes(Comandos::limpiarHechas)))
                .then(Commands.literal("generar")
                        .then(Commands.argument("total", IntegerArgumentType.integer(1, 100))
                                .executes(ctx -> generar(ctx, -1, -1, -1))
                                .then(Commands.argument("medias", IntegerArgumentType.integer(0, 100))
                                        .then(Commands.argument("dificiles", IntegerArgumentType.integer(0, 100))
                                                .executes(ctx -> generar(ctx,
                                                        IntegerArgumentType.getInteger(ctx, "total"),
                                                        IntegerArgumentType.getInteger(ctx, "medias"),
                                                        IntegerArgumentType.getInteger(ctx, "dificiles")))))))
                .then(Commands.literal("cadena").then(Commands.argument("nombre", StringArgumentType.word()).suggests(CADENAS)
                        .executes(Comandos::cadena)))
                .then(Commands.literal("borrar").then(numero().executes(Comandos::borrar)))
                .then(Commands.literal("completar").then(numero().executes(Comandos::completar)))
                .then(Commands.literal("objetivo").then(numero()
                        .then(Commands.argument("cantidad", IntegerArgumentType.integer(1))
                                .then(Commands.argument("cosa", StringArgumentType.greedyString())
                                        .suggests((ctx, b) -> SharedSuggestionProvider.suggestResource(
                                                ctx.getSource().registryAccess().lookupOrThrow(Registries.ITEM).listElementIds().map(ResourceKey::identifier), b))
                                        .executes(Comandos::objetivoExtra)))))
                .then(Commands.literal("encantamiento").then(numero()
                        .then(Commands.argument("encantamiento", ResourceOrTagKeyArgument.resourceOrTagKey(Registries.ENCHANTMENT))
                                .executes(ctx -> detalle(ctx, cosa(ctx, "encantamiento", Registries.ENCHANTMENT) + "=1"))
                                .then(Commands.argument("nivel", IntegerArgumentType.integer(1, 255))
                                        .executes(ctx -> detalle(ctx, cosa(ctx, "encantamiento", Registries.ENCHANTMENT) + "="
                                                + IntegerArgumentType.getInteger(ctx, "nivel")))))))
                .then(Commands.literal("adorno").then(numero().executes(ctx -> detalle(ctx, "adorno=*"))))
                .then(Commands.literal("recompensa").then(numero()
                        .then(Commands.literal("xp").then(Commands.argument("niveles", IntegerArgumentType.integer(1))
                                .executes(ctx -> recompensa(ctx, "xp", IntegerArgumentType.getInteger(ctx, "niveles")))))
                        .then(Commands.argument("cantidad", IntegerArgumentType.integer(1))
                                .then(Commands.argument("objeto", ResourceOrTagKeyArgument.resourceOrTagKey(Registries.ITEM))
                                        .executes(ctx -> recompensa(ctx, cosa(ctx, "objeto", Registries.ITEM), IntegerArgumentType.getInteger(ctx, "cantidad")))))))
                .then(ajustes())
                .then(agregar());

        LiteralCommandNode<CommandSourceStack> nodo = d.register(raiz);
        d.register(Commands.literal("missions").executes(Comandos::lista).redirect(nodo));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> numero() {
        return Commands.argument("n", IntegerArgumentType.integer(1));
    }

    // ---------- /misiones agregar ----------

    private static LiteralArgumentBuilder<CommandSourceStack> agregar() {
        return Commands.literal("agregar")
                .then(Commands.literal("plantilla").then(Commands.argument("id", StringArgumentType.word()).suggests(PLANTILLAS)
                        .executes(Comandos::agregarPlantilla)))
                .then(conCosa("conseguir", Tipo.CONSEGUIR, Registries.ITEM))
                .then(conCosa("cofre", Tipo.COFRE, Registries.ITEM))
                .then(conCosa("minar", Tipo.MINAR, Registries.BLOCK))
                .then(conCosa("cosechar", Tipo.COSECHAR, Registries.BLOCK))
                .then(conCosa("matar", Tipo.MATAR, Registries.ENTITY_TYPE))
                .then(conCosa("criar", Tipo.CRIAR, Registries.ENTITY_TYPE))
                .then(conCosa("domesticar", Tipo.DOMESTICAR, Registries.ENTITY_TYPE))
                .then(conCosa("pescar", Tipo.PESCAR, Registries.ITEM))
                .then(conCosa("fabricar", Tipo.FABRICAR, Registries.ITEM))
                .then(conCosa("encantar", Tipo.ENCANTAR, Registries.ITEM))
                .then(conCosa("pociones", Tipo.POCIONES, Registries.POTION))
                .then(conCosa("comer", Tipo.COMER, Registries.ITEM))
                .then(conCosa("fundir", Tipo.FUNDIR, Registries.ITEM))
                .then(conCosa("comerciar", Tipo.COMERCIAR, Registries.ITEM)
                        .then(Commands.literal("con").then(Commands.argument("veces", IntegerArgumentType.integer(1, 100000))
                                .then(conDificultad(Commands.argument("quien", ResourceOrTagKeyArgument.resourceOrTagKey(Registries.ENTITY_TYPE)),
                                        (ctx, dif) -> nueva(ctx, Tipo.COMERCIAR, dif, cosa(ctx, "quien", Registries.ENTITY_TYPE), IntegerArgumentType.getInteger(ctx, "veces")))))))
                .then(conCosa("equipar", Tipo.EQUIPAR, Registries.ITEM))
                .then(Commands.literal("explorar")
                        .then(Commands.literal("dimension").then(conDificultad(Commands.argument("dim", DimensionArgument.dimension()),
                                (ctx, dif) -> nueva(ctx, Tipo.EXPLORAR, dif, "dimension:" + DimensionArgument.getDimension(ctx, "dim").dimension().identifier(), 1))))
                        .then(Commands.literal("estructura").then(conDificultad(Commands.argument("lugar", ResourceOrTagKeyArgument.resourceOrTagKey(Registries.STRUCTURE)),
                                (ctx, dif) -> nueva(ctx, Tipo.EXPLORAR, dif, "estructura:" + cosa(ctx, "lugar", Registries.STRUCTURE), 1))))
                        .then(Commands.literal("bioma").then(conDificultad(Commands.argument("lugar", ResourceOrTagKeyArgument.resourceOrTagKey(Registries.BIOME)),
                                (ctx, dif) -> nueva(ctx, Tipo.EXPLORAR, dif, "bioma:" + cosa(ctx, "lugar", Registries.BIOME), 1))))
                        .then(Commands.literal("altura")
                                .then(Commands.literal("abajo").then(conDificultad(Commands.argument("y", IntegerArgumentType.integer(-64, 320)),
                                        (ctx, dif) -> nueva(ctx, Tipo.EXPLORAR, dif, "altura:<=" + IntegerArgumentType.getInteger(ctx, "y"), 1))))
                                .then(Commands.literal("arriba").then(conDificultad(Commands.argument("y", IntegerArgumentType.integer(-64, 320)),
                                        (ctx, dif) -> nueva(ctx, Tipo.EXPLORAR, dif, "altura:>=" + IntegerArgumentType.getInteger(ctx, "y"), 1))))))
                .then(Commands.literal("viajar").then(Commands.argument("forma", StringArgumentType.word()).suggests(FORMAS_VIAJE)
                        .then(conDificultad(Commands.argument("bloques", IntegerArgumentType.integer(1, 10000000)),
                                (ctx, dif) -> nueva(ctx, Tipo.VIAJAR, dif, misiones.core.Viajes.forma(StringArgumentType.getString(ctx, "forma")),
                                        IntegerArgumentType.getInteger(ctx, "bloques"))))))
                .then(Commands.literal("logros").then(conDificultad(Commands.argument("logro", IdentifierArgument.id()).suggests(LOGROS),
                        (ctx, dif) -> nueva(ctx, Tipo.LOGROS, dif, IdentifierArgument.getId(ctx, "logro").toString(), 1))))
                .then(Commands.literal("sobrevivir").then(conDificultad(Commands.argument("dias", IntegerArgumentType.integer(1, 10000)),
                        (ctx, dif) -> nueva(ctx, Tipo.SOBREVIVIR, dif, "dias", IntegerArgumentType.getInteger(ctx, "dias")))))
                .then(Commands.literal("nivel").then(conDificultad(Commands.argument("nivel", IntegerArgumentType.integer(1, 10000)),
                        (ctx, dif) -> nueva(ctx, Tipo.NIVEL, dif, "nivel", IntegerArgumentType.getInteger(ctx, "nivel")))))
                .then(Commands.literal("libre").then(Commands.argument("texto", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            Mision m = new Mision();
                            m.tipo = Tipo.LIBRE;
                            m.texto = StringArgumentType.getString(ctx, "texto");
                            m.objetivos.add(new Objetivo("libre", 1));
                            return anadir(ctx, m);
                        })));
    }

    @FunctionalInterface
    private interface Accion {
        int run(CommandContext<CommandSourceStack> ctx, Dificultad dificultad) throws CommandSyntaxException;
    }

    /** Adds an optional difficulty word after an argument. */
    private static ArgumentBuilder<CommandSourceStack, ?> conDificultad(ArgumentBuilder<CommandSourceStack, ?> arg, Accion accion) {
        return arg.executes(ctx -> accion.run(ctx, Dificultad.MEDIA))
                .then(Commands.argument("dificultad", StringArgumentType.word()).suggests(DIFICULTADES)
                        .executes(ctx -> accion.run(ctx, Dificultad.leer(StringArgumentType.getString(ctx, "dificultad"), Dificultad.MEDIA))));
    }

    /** "agregar conseguir <cantidad> <objeto> [dificultad]" */
    private static <T> LiteralArgumentBuilder<CommandSourceStack> conCosa(String nombre, Tipo tipo, ResourceKey<Registry<T>> registro) {
        return Commands.literal(nombre).then(Commands.argument("cantidad", IntegerArgumentType.integer(1, 100000))
                .then(conDificultad(Commands.literal("cualquiera"),
                        (ctx, dif) -> nueva(ctx, tipo, dif, "*", IntegerArgumentType.getInteger(ctx, "cantidad"))))
                .then(conDificultad(Commands.argument("cosa", ResourceOrTagKeyArgument.resourceOrTagKey(registro)),
                        (ctx, dif) -> nueva(ctx, tipo, dif, cosa(ctx, "cosa", registro), IntegerArgumentType.getInteger(ctx, "cantidad")))));
    }

    private static <T> String cosa(CommandContext<CommandSourceStack> ctx, String arg, ResourceKey<Registry<T>> registro) throws CommandSyntaxException {
        return ResourceOrTagKeyArgument.getResourceOrTagKey(ctx, arg, registro, INVALIDO).asPrintable();
    }

    private static int nueva(CommandContext<CommandSourceStack> ctx, Tipo tipo, Dificultad dif, String objetivo, int cantidad) throws CommandSyntaxException {
        Mision m = new Mision();
        m.tipo = tipo;
        m.dificultad = dif;
        m.objetivos.add(new Objetivo(objetivo, cantidad));
        return anadir(ctx, m);
    }

    private static int anadir(CommandContext<CommandSourceStack> ctx, Mision m) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        List<Mision> lista = Datos.lista(player);
        lista.add(m);
        Datos.guardar(player, lista);
        ctx.getSource().sendSuccess(() -> Component.translatable("misiones.agregada").withStyle(ChatFormatting.GREEN)
                .append(" ").append(Progreso.linea(lista.size(), m)), false);
        Progreso.revisarEstado(player);
        return lista.size();
    }

    private static int agregarPlantilla(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String id = StringArgumentType.getString(ctx, "id");
        Mision plantilla = Catalogo.buscar(id);
        if (plantilla == null) throw SIN_PLANTILLA.create(id);
        return anadir(ctx, plantilla.copia().concretar(0));
    }

    // ---------- other subcommands ----------

    private static int lista(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Progreso.revisarEstado(player);
        List<Mision> lista = Datos.lista(player);
        if (lista.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.translatable("misiones.vacia").withStyle(ChatFormatting.GRAY), false);
            return 0;
        }
        long hechas = lista.stream().filter(m -> m.completada).count();
        ctx.getSource().sendSuccess(() -> Component.translatable("misiones.titulo", hechas, lista.size()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        for (int i = 0; i < lista.size(); i++) {
            Mision m = lista.get(i);
            int n = i + 1;
            ctx.getSource().sendSuccess(() -> Progreso.linea(n, m), false);
        }
        int espera = Datos.espera(player).size();
        if (espera > 0) ctx.getSource().sendSuccess(() -> Component.translatable("misiones.espera.lista", espera).withStyle(ChatFormatting.GRAY), false);
        if (Ajustes.limite > 0) ctx.getSource().sendSuccess(() -> Component.translatable("misiones.limite.info", Gestor.activas(lista), Ajustes.limite).withStyle(ChatFormatting.DARK_GRAY), false);
        return lista.size();
    }

    private static int generar(CommandContext<CommandSourceStack> ctx, int faciles, int medias, int dificiles) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int[] cuantas = faciles < 0 ? Catalogo.repartir(IntegerArgumentType.getInteger(ctx, "total")) : new int[]{faciles, medias, dificiles};
        int[] r = Gestor.generar(player, cuantas);
        if (r[0] < 0) {
            ctx.getSource().sendFailure(Component.translatable("misiones.limite.lleno", Ajustes.limite));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("misiones.generadas", r[0]).withStyle(ChatFormatting.GREEN), false);
        if (r[0] < r[1]) {
            ctx.getSource().sendSuccess(() -> Component.translatable("misiones.limite.recortado", Ajustes.limite).withStyle(ChatFormatting.GOLD), false);
        }
        return lista(ctx);
    }

    private static int cadena(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String nombre = StringArgumentType.getString(ctx, "nombre");
        Mision inicio = Catalogo.inicioCadena(nombre);
        if (inicio == null) throw SIN_CADENA.create(nombre);
        return anadir(ctx, inicio.copia().concretar(0));
    }

    private static Mision obtener(CommandContext<CommandSourceStack> ctx, List<Mision> lista) throws CommandSyntaxException {
        int n = IntegerArgumentType.getInteger(ctx, "n");
        if (n < 1 || n > lista.size()) throw SIN_MISION.create();
        return lista.get(n - 1);
    }

    private static int borrar(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        List<Mision> lista = Datos.lista(player);
        Mision m = obtener(ctx, lista);
        lista.remove(m);
        Gestor.rellenar(player, lista);
        Datos.guardar(player, lista);
        ctx.getSource().sendSuccess(() -> Component.translatable("misiones.borrada").withStyle(ChatFormatting.YELLOW), false);
        return 1;
    }

    private static int completar(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        List<Mision> lista = Datos.lista(player);
        Mision m = obtener(ctx, lista);
        if (!m.completada) {
            m.objetivos.forEach(o -> o.progreso = o.cantidad);
            Progreso.completar(player, lista, m, IntegerArgumentType.getInteger(ctx, "n"));
            Datos.guardar(player, lista);
        }
        return 1;
    }

    private static int limpiar(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Datos.guardar(player, new ArrayList<>());
        Datos.guardarEspera(player, new ArrayList<>());
        ctx.getSource().sendSuccess(() -> Component.translatable("misiones.limpiadas").withStyle(ChatFormatting.YELLOW), false);
        return 1;
    }

    private static int limpiarHechas(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        List<Mision> lista = Datos.lista(player);
        int antes = lista.size();
        lista.removeIf(m -> m.completada);
        Gestor.rellenar(player, lista);
        Datos.guardar(player, lista);
        int quitadas = antes - lista.size();
        ctx.getSource().sendSuccess(() -> Component.translatable("misiones.limpiadas.hechas", quitadas).withStyle(ChatFormatting.YELLOW), false);
        return quitadas;
    }

    /** Adds a detail (enchantment, trim) to the last objective of a mission. */
    private static int detalle(CommandContext<CommandSourceStack> ctx, String detalle) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        List<Mision> lista = Datos.lista(player);
        Mision m = obtener(ctx, lista);
        if (m.objetivos.isEmpty()) throw SIN_MISION.create();
        Objetivo o = m.objetivos.get(m.objetivos.size() - 1);
        int cierra = o.objetivo.lastIndexOf(']');
        if (o.objetivo.contains("[") && cierra > 0) o.objetivo = o.objetivo.substring(0, cierra) + "," + detalle + "]";
        else o.objetivo = o.objetivo + "[" + detalle + "]";
        o.progreso = 0;
        m.completada = false;
        Datos.guardar(player, lista);
        int n = IntegerArgumentType.getInteger(ctx, "n");
        ctx.getSource().sendSuccess(() -> Progreso.linea(n, m), false);
        Progreso.revisarEstado(player);
        return 1;
    }

    private static int objetivoExtra(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        List<Mision> lista = Datos.lista(player);
        Mision m = obtener(ctx, lista);
        if (!m.tipo.multiple) throw NO_MULTIPLE.create();
        String texto = StringArgumentType.getString(ctx, "cosa").trim();
        String cosa = m.tipo == Tipo.VIAJAR ? misiones.core.Viajes.forma(texto)
                : texto.equals("*") || texto.equalsIgnoreCase("cualquiera") ? "*"
                : texto.contains("[") ? Progreso.normalizar(texto.substring(0, texto.indexOf('['))) + texto.substring(texto.indexOf('['))
                : Progreso.normalizar(texto);
        m.objetivos.add(new Objetivo(cosa, IntegerArgumentType.getInteger(ctx, "cantidad")));
        m.completada = false;
        Datos.guardar(player, lista);
        int n = IntegerArgumentType.getInteger(ctx, "n");
        ctx.getSource().sendSuccess(() -> Progreso.linea(n, m), false);
        Progreso.revisarEstado(player);
        return 1;
    }

    private static int recompensa(CommandContext<CommandSourceStack> ctx, String objeto, int cantidad) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        List<Mision> lista = Datos.lista(player);
        Mision m = obtener(ctx, lista);
        m.recompensas.add(new Recompensa(objeto, cantidad));
        Datos.guardar(player, lista);
        int n = IntegerArgumentType.getInteger(ctx, "n");
        ctx.getSource().sendSuccess(() -> Progreso.linea(n, m), false);
        return 1;
    }

    /** Summary: how many missions of each type and difficulty. */
    private static int catalogo(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> Component.translatable("misiones.catalogo.titulo", Catalogo.plantillas().size()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        for (Tipo t : Tipo.values()) {
            int[] n = new int[3];
            for (Mision m : Catalogo.plantillas()) if (m.tipo == t && m.cadena.isEmpty()) n[m.dificultad.ordinal()]++;
            if (n[0] + n[1] + n[2] == 0) continue;
            ctx.getSource().sendSuccess(() -> Component.literal(t.icono + " ").append(Component.translatable(t.clave())).append(": ")
                    .append(Component.literal(n[0] + " ").withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(n[1] + " ").withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(String.valueOf(n[2])).withStyle(ChatFormatting.RED)), false);
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("misiones.catalogo.ayuda").withStyle(ChatFormatting.GRAY), false);
        return Catalogo.plantillas().size();
    }

    private static int catalogoTipo(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String texto = StringArgumentType.getString(ctx, "tipo");
        Tipo tipo = Tipo.leer(texto);
        if (tipo == null) throw INVALIDO.create(texto);
        for (Dificultad dif : Dificultad.values()) {
            ctx.getSource().sendSuccess(() -> Component.translatable(dif.clave()).withStyle(dif.color, ChatFormatting.BOLD), false);
            for (Mision m : Catalogo.plantillas()) {
                if (m.dificultad != dif || m.tipo != tipo || !m.cadena.isEmpty()) continue;
                ctx.getSource().sendSuccess(() -> Component.literal(" " + m.plantilla + ": ").withStyle(ChatFormatting.GOLD)
                        .append(Progreso.descripcion(m)), false);
            }
        }
        return 1;
    }

    private static int catalogoCadenas(CommandContext<CommandSourceStack> ctx) {
        for (String nombre : Catalogo.cadenas()) {
            ctx.getSource().sendSuccess(() -> Component.literal("⛓ " + nombre).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), false);
            Mision m = Catalogo.inicioCadena(nombre);
            int paso = 1;
            java.util.Set<String> vistos = new java.util.HashSet<>();
            while (m != null && vistos.add(m.plantilla)) {
                Mision actual = m;
                int p = paso++;
                ctx.getSource().sendSuccess(() -> Component.literal("  " + p + ". ").withStyle(ChatFormatting.GOLD)
                        .append(Component.literal("[").append(Component.translatable(actual.dificultad.clave())).append("] ").withStyle(actual.dificultad.color))
                        .append(Progreso.descripcion(actual)), false);
                m = actual.siguiente.isEmpty() ? null : Catalogo.buscar(actual.siguiente);
            }
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("misiones.cadena.ayuda").withStyle(ChatFormatting.GRAY), false);
        return Catalogo.cadenas().size();
    }

    // ---------- /misiones ajustes ----------

    private static LiteralArgumentBuilder<CommandSourceStack> ajustes() {
        return Commands.literal("ajustes").executes(Comandos::verAjustes)
                .then(Commands.literal("limite").then(Commands.argument("valor", IntegerArgumentType.integer(0, 1000))
                        .executes(ctx -> { Ajustes.limite = IntegerArgumentType.getInteger(ctx, "valor"); return guardarAjustes(ctx); })))
                .then(Commands.literal("modo")
                        .then(Commands.literal("balanceado").executes(ctx -> { Ajustes.modo = "balanceado"; return guardarAjustes(ctx); }))
                        .then(Commands.literal("azar").executes(ctx -> { Ajustes.modo = "azar"; return guardarAjustes(ctx); })))
                .then(Commands.literal("balanceado").then(Commands.argument("facil", IntegerArgumentType.integer(0, 100))
                        .then(Commands.argument("media", IntegerArgumentType.integer(0, 100))
                                .then(Commands.argument("dificil", IntegerArgumentType.integer(0, 100)).executes(ctx -> {
                                    Ajustes.porcentajeFacil = IntegerArgumentType.getInteger(ctx, "facil");
                                    Ajustes.porcentajeMedia = IntegerArgumentType.getInteger(ctx, "media");
                                    Ajustes.porcentajeDificil = IntegerArgumentType.getInteger(ctx, "dificil");
                                    return guardarAjustes(ctx);
                                })))))
                .then(Commands.literal("diaria").then(Commands.argument("valor", StringArgumentType.word()).suggests(SI_NO)
                        .executes(ctx -> { Ajustes.diaria = si(StringArgumentType.getString(ctx, "valor")); return guardarAjustes(ctx); })))
                .then(Commands.literal("dificultaddiaria").then(Commands.argument("valor", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(new String[]{"azar", "facil", "media", "dificil"}, b))
                        .executes(ctx -> {
                            String v = StringArgumentType.getString(ctx, "valor").toLowerCase();
                            Ajustes.dificultadDiaria = v.startsWith("azar") ? "azar" : Dificultad.leer(v, Dificultad.MEDIA).id;
                            return guardarAjustes(ctx);
                        })))
                .then(Commands.literal("repartirtipos").then(Commands.argument("valor", StringArgumentType.word()).suggests(SI_NO)
                        .executes(ctx -> { Ajustes.repartirTipos = si(StringArgumentType.getString(ctx, "valor")); return guardarAjustes(ctx); })))
                .then(Commands.literal("variacion").then(Commands.argument("porcentaje", IntegerArgumentType.integer(0, 100))
                        .executes(ctx -> { Ajustes.variacion = IntegerArgumentType.getInteger(ctx, "porcentaje"); return guardarAjustes(ctx); })));
    }

    private static boolean si(String texto) {
        String t = texto.toLowerCase();
        return t.equals("si") || t.equals("sí") || t.equals("yes") || t.equals("true") || t.equals("on");
    }

    private static int guardarAjustes(CommandContext<CommandSourceStack> ctx) {
        Ajustes.guardar();
        if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
            List<Mision> lista = Datos.lista(player);
            if (Gestor.rellenar(player, lista)) Datos.guardar(player, lista);
        }
        return verAjustes(ctx);
    }

    private static int verAjustes(CommandContext<CommandSourceStack> ctx) {
        Component si = Component.translatable("misiones.si"), no = Component.translatable("misiones.no");
        ctx.getSource().sendSuccess(() -> Component.translatable("misiones.ajustes.titulo").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        ctx.getSource().sendSuccess(() -> Component.translatable("misiones.ajustes.modo", Component.translatable("misiones.modo." + Ajustes.modo),
                Ajustes.porcentajeFacil, Ajustes.porcentajeMedia, Ajustes.porcentajeDificil), false);
        ctx.getSource().sendSuccess(() -> Component.translatable("misiones.ajustes.limite",
                Ajustes.limite > 0 ? Component.literal(String.valueOf(Ajustes.limite)) : Component.translatable("misiones.sin_limite")), false);
        ctx.getSource().sendSuccess(() -> Component.translatable("misiones.ajustes.diaria", Ajustes.diaria ? si : no,
                Ajustes.dificultadDiaria.equals("azar") ? Component.translatable("misiones.modo.azar")
                        : Component.translatable(Dificultad.leer(Ajustes.dificultadDiaria, Dificultad.MEDIA).clave())), false);
        ctx.getSource().sendSuccess(() -> Component.translatable("misiones.ajustes.repartir", Ajustes.repartirTipos ? si : no), false);
        ctx.getSource().sendSuccess(() -> Component.translatable("misiones.ajustes.variacion", Ajustes.variacion), false);
        return 1;
    }

    private static int recargar(CommandContext<CommandSourceStack> ctx) {
        Ajustes.cargar();
        int n = Catalogo.cargar();
        ctx.getSource().sendSuccess(() -> Component.translatable("misiones.recargadas", n).withStyle(ChatFormatting.GREEN), false);
        return n;
    }

    private Comandos() {}
}
