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
import misiones.core.Catalogo;
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
    private static final DynamicCommandExceptionType INVALIDO = new DynamicCommandExceptionType(t -> Component.translatable("misiones.error.invalido", t));

    private static final SuggestionProvider<CommandSourceStack> DIFICULTADES =
            (ctx, b) -> SharedSuggestionProvider.suggest(new String[]{"facil", "media", "dificil"}, b);
    private static final SuggestionProvider<CommandSourceStack> PLANTILLAS =
            (ctx, b) -> SharedSuggestionProvider.suggest(Catalogo.plantillas().stream().map(m -> m.plantilla).toList(), b);

    public static void registrar(CommandDispatcher<CommandSourceStack> d, CommandBuildContext build) {
        LiteralArgumentBuilder<CommandSourceStack> raiz = Commands.literal("misiones")
                .executes(Comandos::lista)
                .then(Commands.literal("lista").executes(Comandos::lista))
                .then(Commands.literal("catalogo").executes(Comandos::catalogo))
                .then(Commands.literal("recargar").executes(Comandos::recargar))
                .then(Commands.literal("limpiar").executes(Comandos::limpiar))
                .then(Commands.literal("generar")
                        .then(Commands.argument("total", IntegerArgumentType.integer(1, 100))
                                .executes(ctx -> generar(ctx, -1, -1, -1))
                                .then(Commands.argument("medias", IntegerArgumentType.integer(0, 100))
                                        .then(Commands.argument("dificiles", IntegerArgumentType.integer(0, 100))
                                                .executes(ctx -> generar(ctx,
                                                        IntegerArgumentType.getInteger(ctx, "total"),
                                                        IntegerArgumentType.getInteger(ctx, "medias"),
                                                        IntegerArgumentType.getInteger(ctx, "dificiles")))))))
                .then(Commands.literal("borrar").then(numero().executes(Comandos::borrar)))
                .then(Commands.literal("completar").then(numero().executes(Comandos::completar)))
                .then(Commands.literal("objetivo").then(numero()
                        .then(Commands.argument("cantidad", IntegerArgumentType.integer(1))
                                .then(Commands.argument("cosa", StringArgumentType.greedyString())
                                        .suggests((ctx, b) -> SharedSuggestionProvider.suggestResource(
                                                ctx.getSource().registryAccess().lookupOrThrow(Registries.ITEM).listElementIds().map(ResourceKey::identifier), b))
                                        .executes(Comandos::objetivoExtra)))))
                .then(Commands.literal("recompensa").then(numero()
                        .then(Commands.literal("xp").then(Commands.argument("niveles", IntegerArgumentType.integer(1))
                                .executes(ctx -> recompensa(ctx, "xp", IntegerArgumentType.getInteger(ctx, "niveles")))))
                        .then(Commands.argument("cantidad", IntegerArgumentType.integer(1))
                                .then(Commands.argument("objeto", ResourceOrTagKeyArgument.resourceOrTagKey(Registries.ITEM))
                                        .executes(ctx -> recompensa(ctx, cosa(ctx, "objeto", Registries.ITEM), IntegerArgumentType.getInteger(ctx, "cantidad")))))))
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
                .then(conCosa("fabricar", Tipo.FABRICAR, Registries.ITEM))
                .then(conCosa("matar", Tipo.MATAR, Registries.ENTITY_TYPE))
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
    private static <T> LiteralArgumentBuilder<CommandSourceStack> conCosa(String nombre, Tipo tipo, ResourceKey<? extends Registry<T>> registro) {
        return Commands.literal(nombre).then(Commands.argument("cantidad", IntegerArgumentType.integer(1, 100000))
                .then(conDificultad(Commands.argument("cosa", ResourceOrTagKeyArgument.resourceOrTagKey(registro)),
                        (ctx, dif) -> nueva(ctx, tipo, dif, cosa(ctx, "cosa", registro), IntegerArgumentType.getInteger(ctx, "cantidad")))));
    }

    private static <T> String cosa(CommandContext<CommandSourceStack> ctx, String arg, ResourceKey<? extends Registry<T>> registro) throws CommandSyntaxException {
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
        return anadir(ctx, plantilla.copia());
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
        return lista.size();
    }

    private static int generar(CommandContext<CommandSourceStack> ctx, int faciles, int medias, int dificiles) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        List<Mision> lista = Datos.lista(player);
        List<String> yaTiene = new ArrayList<>(lista.stream().map(m -> m.plantilla).toList());
        List<Mision> nuevas = new ArrayList<>();
        if (faciles < 0) {
            nuevas.addAll(Catalogo.alAzar(IntegerArgumentType.getInteger(ctx, "total"), yaTiene));
        } else {
            nuevas.addAll(Catalogo.alAzar(Dificultad.FACIL, faciles, yaTiene));
            nuevas.addAll(Catalogo.alAzar(Dificultad.MEDIA, medias, yaTiene));
            nuevas.addAll(Catalogo.alAzar(Dificultad.DIFICIL, dificiles, yaTiene));
        }
        lista.addAll(nuevas);
        Datos.guardar(player, lista);
        ctx.getSource().sendSuccess(() -> Component.translatable("misiones.generadas", nuevas.size()).withStyle(ChatFormatting.GREEN), false);
        return lista(ctx);
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
            Progreso.completar(player, m, IntegerArgumentType.getInteger(ctx, "n"));
            Datos.guardar(player, lista);
        }
        return 1;
    }

    private static int limpiar(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Datos.guardar(player, new ArrayList<>());
        ctx.getSource().sendSuccess(() -> Component.translatable("misiones.limpiadas").withStyle(ChatFormatting.YELLOW), false);
        return 1;
    }

    private static int objetivoExtra(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        List<Mision> lista = Datos.lista(player);
        Mision m = obtener(ctx, lista);
        if (!m.tipo.multiple) throw NO_MULTIPLE.create();
        String cosa = Progreso.normalizar(StringArgumentType.getString(ctx, "cosa").trim());
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

    private static int catalogo(CommandContext<CommandSourceStack> ctx) {
        for (Dificultad dif : Dificultad.values()) {
            ctx.getSource().sendSuccess(() -> Component.translatable(dif.clave()).withStyle(dif.color, ChatFormatting.BOLD), false);
            for (Mision m : Catalogo.plantillas()) {
                if (m.dificultad != dif) continue;
                ctx.getSource().sendSuccess(() -> Component.literal(" " + m.plantilla + ": ").withStyle(ChatFormatting.GOLD)
                        .append(Progreso.descripcion(m)), false);
            }
        }
        return Catalogo.plantillas().size();
    }

    private static int recargar(CommandContext<CommandSourceStack> ctx) {
        int n = Catalogo.cargar();
        ctx.getSource().sendSuccess(() -> Component.translatable("misiones.recargadas", n).withStyle(ChatFormatting.GREEN), false);
        return n;
    }

    private Comandos() {}
}
