package cn.ussshenzhou.notenoughbandwidth.modnetwork;

import cn.ussshenzhou.notenoughbandwidth.NotEnoughBandwidth;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;

/**
 * @author USS_Shenzhou
 */
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class ModNetworkRegistry {

    @SubscribeEvent
    public static void networkPacketRegistry(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(NotEnoughBandwidth.MODID);

        registrar.commonBidirectional(PacketAggregationPacket.TYPE, StreamCodec.ofMember(PacketAggregationPacket::encode, PacketAggregationPacket::new), new DirectionalPayloadHandler<>(
                PacketAggregationPacket::handler,
                PacketAggregationPacket::handler
        ));
    }
}
