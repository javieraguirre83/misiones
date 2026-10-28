package misiones.core;

import misiones.comandos.Comandos;
import misiones.eventos.Eventos;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Misiones: a mission generator to give every playthrough a goal. */
@Mod(Misiones.MODID)
public final class Misiones {
    public static final String MODID = "misiones";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public Misiones(FMLJavaModLoadingContext context) {
        FMLCommonSetupEvent.getBus(context.getModBusGroup()).addListener(e -> {
            Ajustes.cargar();
            int n = Catalogo.cargar();
            LOGGER.info("Misiones: {} misiones en el catalogo", n);
        });
        Eventos.registrar();
        RegisterCommandsEvent.BUS.addListener(e -> Comandos.registrar(e.getDispatcher(), e.getBuildContext()));
    }
}
