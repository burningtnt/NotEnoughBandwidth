package cn.ussshenzhou.notenoughbandwidth.mixin;

import cn.ussshenzhou.notenoughbandwidth.network.compress.CompressedEncoder;
import io.netty.channel.ChannelPipeline;
import net.minecraft.network.Connection;
import net.neoforged.neoforge.network.filters.NetworkFilters;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.NoSuchElementException;

@SuppressWarnings("UnstableApiUsage")
@Mixin(NetworkFilters.class)
public class NetworkFiltersMixin {
    @Inject(method = "injectIfNecessary", at = @At("TAIL"))
    private static void onInject(Connection manager, CallbackInfo ci) {
        ChannelPipeline pipeline = manager.channel().pipeline();
        if (pipeline.get("encoder") != null) {
            pipeline.addAfter("encoder", CompressedEncoder.ID, CompressedEncoder.INSTANCE);
        }
    }

    @Inject(method = "cleanIfNecessary", at = @At("HEAD"))
    private static void onClean(Connection manager, CallbackInfo ci) {
        try {
            manager.channel().pipeline().remove(CompressedEncoder.ID);
        } catch (NoSuchElementException ignored) {
        }
    }
}
