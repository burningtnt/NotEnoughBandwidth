package cn.ussshenzhou.notenoughbandwidth.mixin;

import cn.ussshenzhou.notenoughbandwidth.helpers.CustomPacketPrefixHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author USS_Shenzhou
 */
@Mixin(targets = "net.minecraft.network.protocol.common.custom.CustomPacketPayload$1")
public class CustomPacketPayloadMixin {
    @Redirect(method = "writeCap", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/FriendlyByteBuf;writeResourceLocation(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/network/FriendlyByteBuf;"))
    private FriendlyByteBuf nebwIndexedHeaderEncode(FriendlyByteBuf buf, ResourceLocation resourceLocation) {
        CustomPacketPrefixHelper.get()
                .index(resourceLocation)
                .save(buf);
        return buf;
    }

    @Redirect(method = "decode(Lnet/minecraft/network/FriendlyByteBuf;)Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/FriendlyByteBuf;readResourceLocation()Lnet/minecraft/resources/ResourceLocation;"))
    private ResourceLocation nebwIndexedHeaderDecode(FriendlyByteBuf buf) {
        return CustomPacketPrefixHelper.getType(buf);
    }
}
