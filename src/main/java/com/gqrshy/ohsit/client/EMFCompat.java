package com.gqrshy.ohsit.client;

import net.minecraft.entity.player.PlayerEntity;

import java.lang.reflect.Method;
import java.util.function.Function;

public class EMFCompat {
    public static void register() {
        try {
            Class<?> apiClass = Class.forName(
                    "traben.entity_model_features.EMFAnimationApi"
            );
            Method method = apiClass.getMethod(
                    "registerPauseCondition", Function.class
            );
            method.invoke(null, (Function<?, Boolean>) entity -> {
                if (entity instanceof PlayerEntity player) {
                    return SitAnimationHelper.isSitting(player.getUuid());
                }
                return false;
            });
        } catch (Exception ignored) {
        }
    }
}
