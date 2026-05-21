package com.harrytheewizard.advmultiplier.mixin;

import com.harrytheewizard.advmultiplier.MultiplierManager;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsMixin {

    @Shadow private ServerPlayer player;

    @Shadow public abstract AdvancementProgress getOrStartProgress(AdvancementHolder holder);

    @Unique private boolean advMultiplier$wasNotDone;

    @Inject(method = "award", at = @At("HEAD"))
    private void capturePreState(AdvancementHolder holder, String criterion,
                                 CallbackInfoReturnable<Boolean> cir) {
        advMultiplier$wasNotDone = !getOrStartProgress(holder).isDone();
    }

    @Inject(method = "award", at = @At("RETURN"))
    private void onAward(AdvancementHolder holder, String criterion,
                         CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && advMultiplier$wasNotDone && getOrStartProgress(holder).isDone()) {
            MultiplierManager.onAdvancementGranted(player, holder);
        }
    }
}
