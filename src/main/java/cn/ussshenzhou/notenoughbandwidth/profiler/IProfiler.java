package cn.ussshenzhou.notenoughbandwidth.profiler;

import net.minecraft.resources.ResourceLocation;

public interface IProfiler {
    void onSendPacket(ResourceLocation type, int size);

    void onReceivePacket(ResourceLocation type, int size);

    static IProfiler getInstance() {
        String profiler = System.getProperty("neb.profiler", "dummy");
        return switch (profiler) {
            case "dummy" -> new IProfiler() {
                @Override
                public void onSendPacket(ResourceLocation type, int size) {
                }

                @Override
                public void onReceivePacket(ResourceLocation type, int size) {
                }
            };
            case "prometheus" -> new PrometheusProfiler();
            default -> throw new IllegalStateException("Unsupported profiler: " + profiler);
        };
    }
}
