package pl.novabomba.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import pl.novabomba.NovaBomba;

public class NovaBombaFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> NovaBomba.tick());
        HudRenderCallback.EVENT.register((graphics, tickDelta) -> NovaBomba.renderHud(graphics, tickDelta));
        WorldRenderEvents.LAST.register(ctx -> NovaBomba.renderWorld(ctx.matrixStack(), ctx.camera(), ctx.tickDelta()));
    }
}
