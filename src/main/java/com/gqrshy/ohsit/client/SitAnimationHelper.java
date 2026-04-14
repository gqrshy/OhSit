package com.gqrshy.ohsit.client;

import com.gqrshy.ohsit.OhSitMod;
import com.gqrshy.ohsit.sit.SitPose;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SitAnimationHelper {
    private static final Identifier ANIMATION_LAYER_ID = Identifier.of(OhSitMod.MOD_ID, "sitting");
    private static final Map<UUID, SitPose> sittingPlayers = new HashMap<>();

    public static ModifierLayer<IAnimation> registerAnimationLayer(AbstractClientPlayerEntity player) {
        ModifierLayer<IAnimation> layer = new ModifierLayer<>();
        PlayerAnimationAccess.getPlayerAssociatedData(player).set(ANIMATION_LAYER_ID, layer);
        return layer;
    }

    @SuppressWarnings("unchecked")
    public static ModifierLayer<IAnimation> getAnimationLayer(AbstractClientPlayerEntity player) {
        IAnimation animation = PlayerAnimationAccess.getPlayerAssociatedData(player).get(ANIMATION_LAYER_ID);
        return (ModifierLayer<IAnimation>) animation;
    }

    public static void playAnimation(AbstractClientPlayerEntity player, SitPose pose) {
        ModifierLayer<IAnimation> layer = getAnimationLayer(player);
        if (layer == null) return;

        var playable = PlayerAnimationRegistry.getAnimation(
                Identifier.of(OhSitMod.MOD_ID, pose.getAnimationName())
        );
        if (playable instanceof KeyframeAnimation anim) {
            layer.setAnimation(new KeyframeAnimationPlayer(anim));
        }

        sittingPlayers.put(player.getUuid(), pose);
    }

    public static void stopAnimation(AbstractClientPlayerEntity player) {
        ModifierLayer<IAnimation> layer = getAnimationLayer(player);
        if (layer == null) return;

        var getup = PlayerAnimationRegistry.getAnimation(
                Identifier.of(OhSitMod.MOD_ID, "getup")
        );
        if (getup instanceof KeyframeAnimation anim) {
            layer.setAnimation(new KeyframeAnimationPlayer(anim));
        } else {
            layer.setAnimation(null);
        }

        sittingPlayers.remove(player.getUuid());
    }

    public static boolean isSitting(UUID playerId) {
        return sittingPlayers.containsKey(playerId);
    }

    public static SitPose getActivePose(UUID playerId) {
        return sittingPlayers.get(playerId);
    }

    public static void clearAll() {
        sittingPlayers.clear();
    }
}
