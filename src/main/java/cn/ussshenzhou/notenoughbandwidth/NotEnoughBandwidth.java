package cn.ussshenzhou.notenoughbandwidth;

import cn.ussshenzhou.notenoughbandwidth.network.PacketAggregationPacket;
import com.mojang.logging.LogUtils;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import org.slf4j.Logger;

/**
 * @author USS_Shenzhou
 */
@Mod(NotEnoughBandwidth.MODID)
public class NotEnoughBandwidth {
    public static final String MODID = "nebw";
    private static final Logger LOGGER = LogUtils.getLogger();

    public NotEnoughBandwidth(IEventBus modEventBus, ModContainer modContainer) {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(NotEnoughBandwidth.MODID);

        registrar.commonBidirectional(
                PacketAggregationPacket.TYPE,
                StreamCodec.ofMember(PacketAggregationPacket::encode, PacketAggregationPacket::new),
                new DirectionalPayloadHandler<>(PacketAggregationPacket::handler, PacketAggregationPacket::handler)
        );
    }
}
