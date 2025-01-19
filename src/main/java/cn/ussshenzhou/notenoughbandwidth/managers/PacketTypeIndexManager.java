package cn.ussshenzhou.notenoughbandwidth.managers;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.registration.NetworkPayloadSetup;
import org.slf4j.event.Level;

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
        synchronized (NAMESPACES) {
            PacketAggregationManager.init();

            initialized = false;
            NAMESPACES.clear();
            PATHS.clear();
            NAMESPACE_MAP.clear();
            PATH_MAPS.clear();

            List<ResourceLocation> types = new ArrayList<>(setup.channels().get(ConnectionProtocol.PLAY).keySet());
            types.sort(Comparator.comparing(ResourceLocation::getNamespace).thenComparing(ResourceLocation::getPath));
            AtomicInteger namespaceIndex = new AtomicInteger();
            types.forEach(type -> {
                if (!NAMESPACE_MAP.containsKey(type.getNamespace())) {
                    NAMESPACE_MAP.put(type.getNamespace(), namespaceIndex.get());
                    NAMESPACES.add(type.getNamespace());
                    PATHS.add(new ArrayList<>());
                    namespaceIndex.getAndIncrement();
                }
                PATH_MAPS.compute(namespaceIndex.get() - 1, (namespaceId1, pathMap) -> {
                    if (pathMap == null) {
                        pathMap = new Object2IntOpenHashMap<>();
                    }
                    pathMap.put(type.getPath(), pathMap.size());
                    return pathMap;
                });
                PATHS.get(namespaceIndex.get() - 1).add(type.getPath());
            });

            var logger = LogUtils.getLogger();
            if (logger.isEnabledForLevel(Level.DEBUG)) {
                logger.debug("PacketTypeIndexManager initialized.");
                NAMESPACE_MAP.forEach((namespace, id) -> {
                    logger.debug("namespace: {} id: {}", namespace, id);
                    PATH_MAPS.get(id).forEach((path, id1) -> logger.debug("- path: {} id: {}", path, id1));

                });
            }
            initialized = true;
        }
    }

    private static boolean contains(ResourceLocation type) {
        if (!initialized) {
            return false;
        }
        return NAMESPACE_MAP.containsKey(type.getNamespace()) && PATH_MAPS.get(NAMESPACE_MAP.getInt(type.getNamespace())).containsKey(type.getPath());
    }

    public static int getIndex(ResourceLocation type) {
        if (initialized && contains(type)) {
            int namespaceIndex = NAMESPACE_MAP.getInt(type.getNamespace());
            int pathIndex = PATH_MAPS.get(namespaceIndex).getInt(type.getPath());
            if (namespaceIndex < 256 && pathIndex < 256) {
                return 0xc0000000 | (namespaceIndex << 16) | (pathIndex << 8);
            } else {
                return 0x80000000 | (namespaceIndex << 12) | (pathIndex);
            }
        }
        return 0;
    }

    public static int getIndexNotTight(ResourceLocation type) {
        if (initialized && contains(type)) {
            int namespaceIndex = NAMESPACE_MAP.getInt(type.getNamespace());
            int pathIndex = PATH_MAPS.get(namespaceIndex).getInt(type.getPath());
            return 0x80000000 | (namespaceIndex << 12) | (pathIndex);
        }
        return 0;
    }

    public static ResourceLocation getResourceLocation(int index, boolean tight) {
        if (!initialized) {
            return null;
        }
        int namespaceIndex, pathIndex;
        if (tight) {
            namespaceIndex = (index & 0b11111111_00000000) >>> 8;
            pathIndex = (index & 0b00000000_11111111);
        } else {
            namespaceIndex = (index & 0b11111111_11110000_00000000) >>> 12;
            pathIndex = (index & 0b00000000_00001111_11111111);
        }
        return ResourceLocation.fromNamespaceAndPath(NAMESPACES.get(namespaceIndex), PATHS.get(namespaceIndex).get(pathIndex));
    }
}
