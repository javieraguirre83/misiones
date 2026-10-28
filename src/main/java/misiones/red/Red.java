package misiones.red;

import misiones.core.Misiones;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

/** Network channel for the mission screens. */
public final class Red {
    public static SimpleChannel CANAL;

    public static void registrar() {
        CANAL = ChannelBuilder.named(Identifier.fromNamespaceAndPath(Misiones.MODID, "pantallas"))
                .networkProtocolVersion(1)
                .simpleChannel();
        CANAL.messageBuilder(Paquete.class)
                .encoder(Paquete::escribir)
                .decoder(Paquete::new)
                .consumerMainThread(Red::recibir)
                .add();
        CANAL.build();
    }

    private static void recibir(Paquete p, CustomPayloadEvent.Context ctx) {
        if (ctx.isServerSide()) {
            ServerPlayer player = ctx.getSender();
            if (player != null) Acciones.recibir(player, p);
        } else {
            misiones.cliente.DatosCliente.recibir(p);
        }
        ctx.setPacketHandled(true);
    }

    public static void alServidor(Paquete p) {
        CANAL.send(p, PacketDistributor.SERVER.noArg());
    }

    public static void alJugador(ServerPlayer player, Paquete p) {
        CANAL.send(p, PacketDistributor.PLAYER.with(player));
    }

    private Red() {}
}
