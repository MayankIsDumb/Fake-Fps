package com.example.fakefps.mixin;

import com.example.fakefps.FakeFPSMod;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftClientMixin {

    @Inject(method = "getFps", at = @At("RETURN"), cancellable = true)
    private void onGetCurrentFps(CallbackInfoReturnable<Integer> cir) {
        FakeFPSMod mod = FakeFPSMod.getInstance();
        if (mod != null && mod.isEnabled()) {
            cir.setReturnValue(mod.getCurrentFps());
        }
    }
}
