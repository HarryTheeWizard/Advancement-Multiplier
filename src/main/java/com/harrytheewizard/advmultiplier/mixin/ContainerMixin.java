package com.harrytheewizard.advmultiplier.mixin;

import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(Container.class)
public interface ContainerMixin {

    @Overwrite
    default int getMaxStackSize() {
        return Integer.MAX_VALUE / 2;
    }
}
