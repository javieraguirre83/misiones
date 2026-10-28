package misiones.core;

import misiones.modelo.Dificultad;
import misiones.modelo.Mision;
import misiones.modelo.Tipo;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

/** Active-mission limit, missions waiting for a place, chains and the daily mission. */
public final class Gestor {
    private static final Random RANDOM = new Random();

    public static int activas(List<Mision> lista) {
        int n = 0;
        for (Mision m : lista) if (!m.completada) n++;
        return n;
    }

    /** Free places under the limit (a big number when there is no limit). */
    public static int espacio(List<Mision> lista) {
        if (Ajustes.limite <= 0) return 100000;
        return Math.max(0, Ajustes.limite - activas(lista));
    }

    /** Missions that make sense for this player (e.g. not advancements already earned). */
    public static Predicate<Mision> sirve(ServerPlayer player) {
        return m -> {
            if (m.tipo != Tipo.LOGROS) return true;
            return m.objetivos.stream().noneMatch(o -> Progreso.logroHecho(player, o.objetivo));
        };
    }

    /** Adds a mission if there is room, otherwise it waits. Returns true if it was added now. */
    public static boolean agregarOEsperar(ServerPlayer player, List<Mision> lista, Mision m) {
        if (espacio(lista) > 0) {
            lista.add(m);
            player.sendSystemMessage(Component.translatable("misiones.nueva").withStyle(ChatFormatting.GREEN)
                    .append(" ").append(Progreso.linea(lista.size(), m)));
            return true;
        }
        List<Mision> espera = Datos.espera(player);
        espera.add(m);
        Datos.guardarEspera(player, espera);
        player.sendSystemMessage(Component.translatable("misiones.en_espera", espera.size()).withStyle(ChatFormatting.GRAY));
        return false;
    }

    /** Moves waiting missions into the list while there is room. */
    public static boolean rellenar(ServerPlayer player, List<Mision> lista) {
        List<Mision> espera = Datos.espera(player);
        if (espera.isEmpty()) return false;
        boolean cambio = false;
        while (!espera.isEmpty() && espacio(lista) > 0) {
            Mision m = espera.remove(0);
            lista.add(m);
            cambio = true;
            player.sendSystemMessage(Component.translatable("misiones.nueva").withStyle(ChatFormatting.GREEN)
                    .append(" ").append(Progreso.linea(lista.size(), m)));
        }
        if (cambio) Datos.guardarEspera(player, espera);
        return cambio;
    }

    /** After completing a mission: unlock the next one of its chain and fill free places. */
    public static void alCompletar(ServerPlayer player, List<Mision> lista, Mision m) {
        if (!m.siguiente.isEmpty()) {
            Mision plantilla = Catalogo.buscar(m.siguiente);
            if (plantilla != null) {
                player.sendSystemMessage(Component.translatable("misiones.cadena.siguiente", m.cadena).withStyle(ChatFormatting.LIGHT_PURPLE));
                agregarOEsperar(player, lista, plantilla.copia());
            }
        } else if (!m.cadena.isEmpty()) {
            player.sendSystemMessage(Component.translatable("misiones.cadena.terminada", m.cadena).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        }
        rellenar(player, lista);
    }

    /** Every second: a new Minecraft day brings the daily mission. */
    public static void revisarDia(ServerPlayer player) {
        long hoy = Progreso.dia(player.level());
        long ultimo = Datos.ultimoDia(player);
        if (ultimo == hoy) return;
        Datos.ultimoDia(player, hoy);
        if (ultimo < 0 || hoy < ultimo || !Ajustes.diaria) return;
        Dificultad dif = switch (Ajustes.dificultadDiaria) {
            case "facil" -> Dificultad.FACIL;
            case "media" -> Dificultad.MEDIA;
            case "dificil" -> Dificultad.DIFICIL;
            default -> Dificultad.values()[RANDOM.nextInt(3)];
        };
        List<Mision> lista = Datos.lista(player);
        List<String> evitar = new ArrayList<>(lista.stream().map(x -> x.plantilla).toList());
        List<Mision> nueva = Catalogo.alAzar(dif, 1, evitar, sirve(player));
        if (nueva.isEmpty()) return;
        Mision m = nueva.get(0);
        m.diaria = true;
        player.sendSystemMessage(Component.translatable("misiones.diaria", hoy + 1).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        agregarOEsperar(player, lista, m);
        Datos.guardar(player, lista);
    }

    private Gestor() {}
}
