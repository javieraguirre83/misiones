package misiones.cliente;

import misiones.modelo.Mision;
import misiones.red.Paquete;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** The last state the server sent to the screens. */
public final class DatosCliente {
    public record Cadena(String nombre, List<Mision> pasos) {}

    public static List<Mision> misiones = new ArrayList<>();
    public static List<Cadena> cadenas = new ArrayList<>();
    public static CompoundTag ajustes = new CompoundTag();
    public static int espera = 0;
    public static int activas = 0;
    public static int catalogo = 0;
    public static Component aviso = null;
    public static long avisoHasta = 0;
    /** Goes up every time new data arrives, so screens know when to rebuild. */
    public static int version = 0;

    public static void recibir(Paquete p) {
        if (!p.accion().equals("estado")) return;
        CompoundTag d = p.datos();
        List<Mision> lista = new ArrayList<>();
        for (Tag t : d.getListOrEmpty("misiones")) if (t instanceof CompoundTag c) lista.add(Mision.cargar(c));
        misiones = lista;
        List<Cadena> cs = new ArrayList<>();
        for (Tag t : d.getListOrEmpty("cadenas")) {
            if (!(t instanceof CompoundTag c)) continue;
            List<Mision> pasos = new ArrayList<>();
            for (Tag x : c.getListOrEmpty("pasos")) if (x instanceof CompoundTag pc) pasos.add(Mision.cargar(pc));
            cs.add(new Cadena(c.getStringOr("nombre", ""), pasos));
        }
        cadenas = cs;
        ajustes = d.getCompoundOrEmpty("ajustes");
        espera = d.getIntOr("espera", 0);
        activas = d.getIntOr("activas", 0);
        catalogo = d.getIntOr("catalogo", 0);
        String clave = d.getStringOr("aviso", "");
        if (!clave.isEmpty()) {
            int n = d.getIntOr("nargs", 0);
            Object[] args = new Object[n];
            for (int i = 0; i < n; i++) args[i] = d.getStringOr("arg" + i, "");
            aviso = Component.translatable(clave, args);
            avisoHasta = System.currentTimeMillis() + 4000;
        }
        version++;
        if (Minecraft.getInstance().gui.screen() instanceof Actualizable a) a.actualizar();
    }

    /** Screens that redraw their widgets when new data arrives. */
    public interface Actualizable {
        void actualizar();
    }

    private DatosCliente() {}
}
