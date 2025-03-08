package cn.ussshenzhou.notenoughbandwidth.profiler;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.resources.ResourceLocation;

public interface IProfiler {
    void onTransmitPacket(Object2IntMap<ResourceLocation> originalSizes, double totalSize, double compressibility);

    void onReceivePacket(Object2IntMap<ResourceLocation> originalSizes, double totalSize, double compressibility);

    static IProfiler getInstance() {
        String port = System.getenv("NEB_PROM_PORT");
        if (port == null) {
            return new IProfiler() {
                @Override
                public void onTransmitPacket(Object2IntMap<ResourceLocation> originalSizes, double totalSize, double compressibility) {
                }

                @Override
                public void onReceivePacket(Object2IntMap<ResourceLocation> originalSizes, double totalSize, double compressibility) {
                }
            };
        }

        return new PrometheusProfiler(Integer.parseInt(port));
    }
}
