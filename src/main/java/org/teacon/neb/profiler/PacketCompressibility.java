package org.teacon.neb.profiler;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;

public final class PacketCompressibility {
    private static final int SAMPLE = Integer.parseInt(Objects.requireNonNullElse(System.getenv("NEB_PROFILER_COMPRESSIBILITY_SAMPLE"), "50"));

    public PacketCompressibility() {
    }

    // TODO: After Valhalla is released, turn to value record instead.
    private final double[] samples = new double[SAMPLE * 2]; // value1, weight1, value2, weight2, ...
    private static final VarHandle SAMPLES_VH = MethodHandles.arrayElementVarHandle(double[].class).withInvokeExactBehavior();

    private volatile int index = 0;
    private volatile double totalValue = 0D, totalWeight = 0D;

    public synchronized double putSample(double value, double weight) {
        int index = this.index;
        double totalValue = this.totalValue, totalWeight = this.totalWeight;

        int sampleValueI = index << 1, sampleWeightI = sampleValueI | 1;

        double replacedSampleValue = (double) SAMPLES_VH.getVolatile(samples, sampleValueI);
        if (replacedSampleValue != 0) {
            double replacedSampleWeight = (double) SAMPLES_VH.getVolatile(samples, sampleWeightI);
            totalValue -= replacedSampleValue * replacedSampleWeight;
            totalWeight -= replacedSampleWeight;
        }

        SAMPLES_VH.setVolatile(samples, sampleValueI, value);
        SAMPLES_VH.setVolatile(samples, sampleWeightI, weight);
        totalValue += value * weight;
        totalWeight += weight;

        index = (index + 1) % SAMPLE;

        this.index = index;
        this.totalValue = totalValue;
        this.totalWeight = totalWeight;

        return totalValue / totalWeight;
    }
}
