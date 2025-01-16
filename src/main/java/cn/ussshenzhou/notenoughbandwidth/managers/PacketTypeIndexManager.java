package cn.ussshenzhou.notenoughbandwidth.managers;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.registration.NetworkPayloadSetup;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author USS_Shenzhou
 */
@SuppressWarnings("UnstableApiUsage")
public class PacketTypeIndexManager {
    private static volatile boolean initialized = false;
    private static final ArrayList<String> NAMESPACES = new ArrayList<>();
    private static final ArrayList<ArrayList<String>> PATHS = new ArrayList<>();
    private static final Object2IntMap<String> NAMESPACE_MAP = new Object2IntOpenHashMap<>();
    private static final HashMap<Integer, Object2IntMap<String>> PATH_MAPS = new HashMap<>();

    public static void init(NetworkPayloadSetup setup) {
        initialized = false;
        NAMESPACES.clear();
        PATHS.clear();
        List<ResourceLocation> types = new ArrayList<>(setup.channels().get(ConnectionProtocol.PLAY).keySet());
        types.sort(Comparator.comparing(ResourceLocation::getNamespace).thenComparing(ResourceLocation::getPath));
        AtomicInteger namespaceIndex = new AtomicInteger();
        types.forEach(type -> {
            if (!NAMESPACE_MAP.containsKey(type.getNamespace())) {
                NAMESPACE_MAP.put(type.getNamespace(), namespaceIndex.get());
                namespaceIndex.getAndIncrement();
            }
            PATH_MAPS.compute(namespaceIndex.get(), (namespaceIndex1, pathMap) -> {
                if (pathMap == null) {
                    pathMap = new Object2IntOpenHashMap<>();
                }
                pathMap.put(type.getPath(), pathMap.size());
                return pathMap;
            });
        });
        initialized = true;
    }

    public static int getIndex(ResourceLocation type) {
        if (!initialized) {
            return -1;
        }
        int namespaceIndex = NAMESPACE_MAP.getInt(type.getNamespace());
        int pathIndex = PATH_MAPS.get(namespaceIndex).getInt(type.getPath());
        return namespaceIndex << 12 | pathIndex;
    }

    public static ResourceLocation getResourceLocation(int index) {
        if (!initialized) {
            return null;
        }
        int namespaceIndex = (index & 0b11111111_11110000_00000000) >> 12;
        int pathIndex = (index & 0b00000000_00001111_11111111);
        return ResourceLocation.fromNamespaceAndPath(NAMESPACES.get(namespaceIndex), PATHS.get(namespaceIndex).get(pathIndex));
    }
}
