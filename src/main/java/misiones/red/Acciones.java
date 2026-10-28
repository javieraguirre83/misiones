package misiones.red;

import misiones.core.Ajustes;
import misiones.core.Catalogo;
import misiones.core.Cosas;
import misiones.core.Datos;
import misiones.core.Gestor;
import misiones.core.Progreso;
import misiones.core.Viajes;
import misiones.modelo.Dificultad;
import misiones.modelo.Mision;
import misiones.modelo.Objetivo;
import misiones.modelo.Recompensa;
import misiones.modelo.Tipo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** What the screens ask the server to do. Every answer is a fresh "estado" with an optional notice. */
public final class Acciones {

    public static void recibir(ServerPlayer player, Paquete p) {
        CompoundTag d = p.datos();
        String aviso = "";
        String[] args = new String[0];
        List<Mision> lista = Datos.lista(player);
        int n = d.getIntOr("n", 0);
        Mision m = n >= 1 && n <= lista.size() ? lista.get(n - 1) : null;

        switch (p.accion()) {
            case "borrar" -> {
                if (m != null) {
                    lista.remove(m);
                    Gestor.rellenar(player, lista);
                    Datos.guardar(player, lista);
                    aviso = "misiones.borrada";
                }
            }
            case "completar" -> {
                if (m != null && !m.completada) {
                    m.objetivos.forEach(o -> o.progreso = o.cantidad);
                    Progreso.completar(player, lista, m, n);
                    Datos.guardar(player, lista);
                }
            }
            case "limpiarHechas" -> {
                int antes = lista.size();
                lista.removeIf(x -> x.completada);
                Gestor.rellenar(player, lista);
                Datos.guardar(player, lista);
                aviso = "misiones.limpiadas.hechas";
                args = new String[]{String.valueOf(antes - lista.size())};
            }
            case "limpiar" -> {
                Datos.guardar(player, new ArrayList<>());
                Datos.guardarEspera(player, new ArrayList<>());
                aviso = "misiones.limpiadas";
            }
            case "generar" -> {
                int[] cuantas = d.contains("total") ? Catalogo.repartir(Math.max(1, Math.min(100, d.getIntOr("total", 1))))
                        : new int[]{limitar(d.getIntOr("facil", 0)), limitar(d.getIntOr("media", 0)), limitar(d.getIntOr("dificil", 0))};
                int[] r = Gestor.generar(player, cuantas);
                if (r[0] < 0) {
                    aviso = "misiones.limite.lleno";
                    args = new String[]{String.valueOf(Ajustes.limite)};
                } else if (r[0] < r[1]) {
                    aviso = "misiones.limite.recortado";
                    args = new String[]{String.valueOf(Ajustes.limite)};
                } else {
                    aviso = "misiones.generadas";
                    args = new String[]{String.valueOf(r[0])};
                }
            }
            case "agregar" -> {
                Tipo tipo = Tipo.leer(d.getStringOr("tipo", ""));
                if (tipo != null) {
                    Mision nueva = new Mision();
                    nueva.tipo = tipo;
                    nueva.dificultad = Dificultad.leer(d.getStringOr("dificultad", "media"), Dificultad.MEDIA);
                    String objetivo = d.getStringOr("objetivo", "*");
                    if (tipo == Tipo.VIAJAR) objetivo = Viajes.forma(objetivo);
                    if (tipo == Tipo.LIBRE) {
                        nueva.texto = d.getStringOr("texto", "");
                        objetivo = "libre";
                    }
                    if (tipo == Tipo.SOBREVIVIR) objetivo = "dias";
                    if (tipo == Tipo.NIVEL) objetivo = "nivel";
                    nueva.objetivos.add(new Objetivo(objetivo, Math.max(1, d.getIntOr("cantidad", 1))));
                    lista.add(nueva);
                    Datos.guardar(player, lista);
                    aviso = "misiones.agregada.simple";
                }
            }
            case "encantamiento", "adorno" -> {
                if (m != null && !m.objetivos.isEmpty()) {
                    String detalle = p.accion().equals("adorno") ? "adorno=*"
                            : Cosas.normalizar(d.getStringOr("encantamiento", "minecraft:unbreaking")) + "=" + Math.max(1, d.getIntOr("nivel", 1));
                    Objetivo o = m.objetivos.get(m.objetivos.size() - 1);
                    int cierra = o.objetivo.lastIndexOf(']');
                    if (o.objetivo.contains("[") && cierra > 0) o.objetivo = o.objetivo.substring(0, cierra) + "," + detalle + "]";
                    else o.objetivo = o.objetivo + "[" + detalle + "]";
                    o.progreso = 0;
                    m.completada = false;
                    Datos.guardar(player, lista);
                }
            }
            case "recompensa" -> {
                if (m != null) {
                    m.recompensas.add(new Recompensa(d.getStringOr("objeto", "xp"), Math.max(1, d.getIntOr("cantidad", 1))));
                    Datos.guardar(player, lista);
                }
            }
            case "cadena" -> {
                Mision inicio = Catalogo.inicioCadena(d.getStringOr("nombre", ""));
                if (inicio != null) {
                    lista.add(inicio.copia());
                    Datos.guardar(player, lista);
                    aviso = "misiones.agregada.simple";
                }
            }
            case "ajustes" -> {
                if (d.contains("modo")) Ajustes.modo = Ajustes.leerModo(d.getStringOr("modo", "balanceado"));
                if (d.contains("facil")) Ajustes.porcentajeFacil = Math.max(0, d.getIntOr("facil", 50));
                if (d.contains("media")) Ajustes.porcentajeMedia = Math.max(0, d.getIntOr("media", 33));
                if (d.contains("dificil")) Ajustes.porcentajeDificil = Math.max(0, d.getIntOr("dificil", 17));
                if (d.contains("limite")) Ajustes.limite = Math.max(0, d.getIntOr("limite", 10));
                if (d.contains("diaria")) Ajustes.diaria = d.getBooleanOr("diaria", true);
                if (d.contains("dificultadDiaria")) Ajustes.dificultadDiaria = d.getStringOr("dificultadDiaria", "azar");
                if (d.contains("repartirTipos")) Ajustes.repartirTipos = d.getBooleanOr("repartirTipos", true);
                Ajustes.guardar();
                if (Gestor.rellenar(player, lista)) Datos.guardar(player, lista);
                aviso = "misiones.ajustes.guardados";
            }
            case "recargar" -> {
                Ajustes.cargar();
                aviso = "misiones.recargadas";
                args = new String[]{String.valueOf(Catalogo.cargar())};
            }
            default -> { }
        }
        if (!p.accion().equals("pedir")) Progreso.revisarEstado(player);
        enviarEstado(player, aviso, args);
    }

    private static int limitar(int x) {
        return Math.max(0, Math.min(100, x));
    }

    /** Sends the player's missions, settings and chains to their screen. */
    public static void enviarEstado(ServerPlayer player, String aviso, String... args) {
        CompoundTag d = new CompoundTag();
        d.putString("accion", "estado");
        List<Mision> lista = Datos.lista(player);
        ListTag misiones = new ListTag();
        lista.forEach(x -> misiones.add(x.guardar()));
        d.put("misiones", misiones);
        d.putInt("espera", Datos.espera(player).size());
        d.putInt("activas", Gestor.activas(lista));

        CompoundTag a = new CompoundTag();
        a.putString("modo", Ajustes.modo);
        a.putInt("facil", Ajustes.porcentajeFacil);
        a.putInt("media", Ajustes.porcentajeMedia);
        a.putInt("dificil", Ajustes.porcentajeDificil);
        a.putInt("limite", Ajustes.limite);
        a.putBoolean("diaria", Ajustes.diaria);
        a.putString("dificultadDiaria", Ajustes.dificultadDiaria);
        a.putBoolean("repartirTipos", Ajustes.repartirTipos);
        d.put("ajustes", a);

        ListTag cadenas = new ListTag();
        for (String nombre : Catalogo.cadenas()) {
            CompoundTag c = new CompoundTag();
            c.putString("nombre", nombre);
            ListTag pasos = new ListTag();
            Set<String> vistos = new HashSet<>();
            Mision paso = Catalogo.inicioCadena(nombre);
            while (paso != null && vistos.add(paso.plantilla)) {
                pasos.add(paso.guardar());
                paso = paso.siguiente.isEmpty() ? null : Catalogo.buscar(paso.siguiente);
            }
            c.put("pasos", pasos);
            cadenas.add(c);
        }
        d.put("cadenas", cadenas);
        d.putInt("catalogo", Catalogo.plantillas().size());

        d.putString("aviso", aviso == null ? "" : aviso);
        d.putInt("nargs", args.length);
        for (int i = 0; i < args.length; i++) d.putString("arg" + i, args[i]);
        Red.alJugador(player, new Paquete(d));
    }

    private Acciones() {}
}
