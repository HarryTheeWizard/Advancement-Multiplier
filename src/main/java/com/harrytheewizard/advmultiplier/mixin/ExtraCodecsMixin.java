package com.harrytheewizard.advmultiplier.mixin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.util.ExtraCodecs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ExtraCodecs.class)
public class ExtraCodecsMixin {

    @Inject(method = "intRange", at = @At("HEAD"), cancellable = true)
    private static void expandStackSizeRange(int min, int max,
            CallbackInfoReturnable<Codec<Integer>> cir) {
        if (max == 99) {
            int newMax = Integer.MAX_VALUE / 2;
            cir.setReturnValue(Codec.INT.validate(v ->
                v >= min && v <= newMax
                    ? DataResult.success(v)
                    : DataResult.error(() -> "Value must be within range ["
                        + min + ";" + newMax + "]: " + v)));
        }
    }
}
