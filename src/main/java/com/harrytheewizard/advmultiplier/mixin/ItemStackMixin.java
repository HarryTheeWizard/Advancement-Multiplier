package com.harrytheewizard.advmultiplier.mixin;

import net.minecraft.world.item.ItemInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(ItemInstance.class)
public interface ItemStackMixin {

    @Overwrite
    default int getMaxStackSize() {
        return Integer.MAX_VALUE / 2;
    }
}
