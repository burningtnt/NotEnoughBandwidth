package cn.ussshenzhou.notenoughbandwidth;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * @author USS_Shenzhou
 */
@Mod(NotEnoughBandwidth.MODID)
public class NotEnoughBandwidth {
    public static final String MODID = "nebw";
    private static final Logger LOGGER = LogUtils.getLogger();

    public NotEnoughBandwidth(IEventBus modEventBus, ModContainer modContainer) {
        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        //modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }


}
