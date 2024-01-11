package com.github.aws404.polypackhost.mixin;

import com.github.aws404.polypackhost.PolypackHttpServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftDedicatedServer.class)
public class MinecraftServerMixin {

	@Inject(method = "setupServer", at = @At("TAIL"))
	private void initPolypackHost(CallbackInfoReturnable<Boolean> cir) {
		PolypackHttpServer.init((MinecraftDedicatedServer) (Object) this);
	}

	@Inject(method = "shutdown", at = @At("TAIL"))
	private void stopPolypackHost(CallbackInfo ci) {
		PolypackHttpServer.stop();
	}
}
