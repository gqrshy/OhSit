package com.gqrshy.ohsit.client;

import com.gqrshy.ohsit.OhSitMod;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class OhSitClient implements ClientModInitializer {
    private static KeyBinding sitMenuKey;

    @Override
    public void onInitializeClient() {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                Identifier.of(OhSitMod.MOD_ID, "sitting"),
                1000,
                player -> new ModifierLayer<>()
        );

        sitMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key." + OhSitMod.MOD_ID + ".open_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                "key.categories." + OhSitMod.MOD_ID
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);

        // Clear sitting state on disconnect
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            SitAnimationHelper.clearAll();
        });

        // EMF compatibility: pause EMF animations while sitting
        if (FabricLoader.getInstance().isModLoaded("entity_model_features")) {
            EMFCompat.register();
        }
    }

    private void onClientTick(MinecraftClient client) {
        var player = client.player;
        if (player == null) return;

        // Clear sitting state on death or spectator mode
        if (SitAnimationHelper.isSitting(player.getUuid())) {
            if (player.isDead() || isSpectator(client)) {
                SitAnimationHelper.stopAnimation(player);
                return;
            }
        }

        // Block menu in spectator mode
        while (sitMenuKey.wasPressed()) {
            if (!isSpectator(client)) {
                client.setScreen(new SittingWheelScreen());
            }
        }

        if (SitAnimationHelper.isSitting(player.getUuid())) {
            var velocity = player.getVelocity();
            if (velocity.y > 0) {
                SitAnimationHelper.stopAnimation(player);
            } else {
                player.setVelocity(0, velocity.y, 0);
            }
        }
    }

    private boolean isSpectator(MinecraftClient client) {
        var interactionManager = client.interactionManager;
        return interactionManager != null && interactionManager.getCurrentGameMode() == GameMode.SPECTATOR;
    }
}
