package org.teacon.neb;

import com.github.luben.zstd.Zstd;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.common.Mod;

import java.lang.invoke.MethodHandles;

/**
 * @author USS_Shenzhou
 */
@Mod(NotEnoughBandwidth.MODID)
public final class NotEnoughBandwidth {
    public static final String MODID = "nebw";

    public NotEnoughBandwidth() {
        try {
            MethodHandles.lookup().ensureInitialized(Zstd.class);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        } catch (UnsatisfiedLinkError e) {
            throw new UnsupportedOperationException(
                    "NotEnoughBandwidth cannot load ZStandard JNI for your platform. " +
                            "Please report this issue to TeaCon."
            );
        }
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
