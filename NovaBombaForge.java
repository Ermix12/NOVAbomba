package pl.novabomba.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import pl.novabomba.NovaBomba;

@Mod("novabomba")
public class NovaBombaForge {
    public NovaBombaForge() {
        if (FMLEnvironment.dist == Dist.CLIENT) MinecraftForge.EVENT_BUS.register(new ClientHandler());
    }

    public static class ClientHandler {
        @SubscribeEvent
        public void onTick(TickEvent.ClientTickEvent e) {
            if (e.phase == TickEvent.Phase.END) NovaBomba.tick();
        }

        @SubscribeEvent
        public void onGui(RenderGuiEvent.Post e) {
            NovaBomba.renderHud(e.getGuiGraphics(), e.getPartialTick());
        }

        @SubscribeEvent
        public void onLevel(RenderLevelStageEvent e) {
            if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES)
                NovaBomba.renderWorld(e.getPoseStack(), e.getCamera(), e.getPartialTick());
        }
    }
}
