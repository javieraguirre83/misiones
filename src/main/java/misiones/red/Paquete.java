package misiones.red;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/** One message between the game screen (client) and the world (server): an action name plus data. */
public class Paquete {
    private final CompoundTag datos;

    public Paquete(CompoundTag datos) {
        this.datos = datos;
    }

    public Paquete(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        this.datos = tag == null ? new CompoundTag() : tag;
    }

    public static Paquete de(String accion) {
        CompoundTag tag = new CompoundTag();
        tag.putString("accion", accion);
        return new Paquete(tag);
    }

    public CompoundTag datos() {
        return datos;
    }

    public String accion() {
        return datos.getStringOr("accion", "");
    }

    public void escribir(FriendlyByteBuf buf) {
        buf.writeNbt(datos);
    }
}
