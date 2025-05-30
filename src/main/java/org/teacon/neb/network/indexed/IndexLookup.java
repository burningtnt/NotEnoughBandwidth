package org.teacon.neb.network.indexed;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.registration.PayloadRegistration;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public final class IndexLookup {
    private static final int EMPTY_INT = -2;

    private static final IndexLookup EMPTY_LOOKUP = new IndexLookup(Collections.emptySet());
    private static final AtomicReference<IndexLookup> INSTANCE = new AtomicReference<>();

    public static IndexLookup getInstance() {
        IndexLookup instance = INSTANCE.getPlain();
        if (instance == null) {
            instance = INSTANCE.get();
        }

        return Objects.requireNonNullElse(instance, EMPTY_LOOKUP);
    }

    @SuppressWarnings({"UnstableApiUsage"})
    public static void initialize(Map<ResourceLocation, PayloadRegistration<?>> payloads) {
        Set<ResourceLocation> packets = new HashSet<>();
        for (PayloadRegistration<?> registration : payloads.values()) {
            // TODO: How to deal with optional packets?
            if (registration.optional()) {
                continue;
            }

            packets.add(registration.id());
        }

        INSTANCE.set(new IndexLookup(packets));
    }

    private final Object2IntMap<String> namespaces2id;

    private final PathLookup[] id2namespaces;

    private record PathLookup(ResourceLocation[] id2RL, Object2IntMap<String> path2id) {
    }

    private IndexLookup(Collection<ResourceLocation> p) {
        Map<String, List<String>> packets = new HashMap<>();
        for (ResourceLocation type : p) {
            packets.computeIfAbsent(type.getNamespace(), _0 -> new ArrayList<>()).add(type.getPath());
        }

        if (packets.size() > 4096) {
            throw new IllegalStateException("Too may namespaces.");
        }

        namespaces2id = new Object2IntOpenHashMap<>();
        namespaces2id.defaultReturnValue(EMPTY_INT);
        id2namespaces = new PathLookup[packets.size()];

        List<String> namespaces = new ArrayList<>(packets.keySet());
        namespaces.sort(null);
        for (int i = 0; i < namespaces.size(); i++) {
            String namespace = namespaces.get(i);

            List<String> paths = packets.get(namespace);
            if (paths.size() > 4096) {
                throw new IllegalStateException("Too many paths for namespace " + namespace);
            }
            paths.sort(null);

            ResourceLocation[] id2RL = new ResourceLocation[paths.size()];
            Object2IntMap<String> path2id = new Object2IntOpenHashMap<>();
            path2id.defaultReturnValue(EMPTY_INT);

            for (int j = 0; j < paths.size(); j++) {
                String path = paths.get(j);

                id2RL[j] = ResourceLocation.fromNamespaceAndPath(namespace, path);
                path2id.put(path, j);
            }

            id2namespaces[i] = new PathLookup(id2RL, path2id);
            namespaces2id.put(namespace, i);
        }
    }

    public /* data */ record Result(int namespace, int path) {
    }

    public static final Result EMPTY = new Result(EMPTY_INT, EMPTY_INT);

    public Result getIndex(ResourceLocation type) {
        int ni = namespaces2id.getInt(type.getNamespace());
        if (ni == EMPTY_INT) {
            return EMPTY;
        }

        int pi = id2namespaces[ni].path2id.getInt(type.getPath());
        if (pi == EMPTY_INT) {
            return EMPTY;
        }

        return new Result(ni, pi);
    }

    public ResourceLocation getType(int namespace, int path) {
        return id2namespaces[namespace].id2RL[path];
    }
}
