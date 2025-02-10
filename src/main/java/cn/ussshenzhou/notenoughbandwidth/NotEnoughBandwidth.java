package cn.ussshenzhou.notenoughbandwidth;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/**
 * @author USS_Shenzhou
 */
@Mod(NotEnoughBandwidth.MODID)
public class NotEnoughBandwidth {
    public static final String MODID = "nebw";

    public NotEnoughBandwidth(IEventBus modEventBus, ModContainer modContainer) {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
