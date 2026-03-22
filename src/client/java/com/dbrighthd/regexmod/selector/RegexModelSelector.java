package com.dbrighthd.regexmod.selector;

import com.dbrighthd.regexmod.cache.StringPool;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.SelectItemModel.ModelSelector;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * ModelSelector that supports exact‑match and regex cases,
 * while keeping memory usage bounded.
 */
public class RegexModelSelector<T> implements ModelSelector<T> {

    /* Size‑bounded LRU cache for dynamic regex hits */
    private static final int MAX_DYNAMIC_MATCHES = 2_048;

    private final Map<String, ItemModel> dynamicCache = new LinkedHashMap<>(256, .75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, ItemModel> eldest) {
            return size() > MAX_DYNAMIC_MATCHES;
        }
    };

    private final Object2ObjectOpenHashMap<T, ItemModel> exactMatches;
    private final List<Pair<Pattern, ItemModel>> regexCases;
    private final ItemModel fallback;

    public RegexModelSelector(Object2ObjectOpenHashMap<T, ItemModel> exactMatches,
                              List<Pair<Pattern, ItemModel>> regexCases,
                              ItemModel fallback) {
        this.exactMatches = exactMatches;
        this.regexCases   = regexCases;
        this.fallback     = fallback;
    }

    @Override
    public ItemModel get(T value, ClientLevel clientLevel) {
        ItemModel m = exactMatches.get(value);
        if (m != fallback) return m;

        String s = null;
        if (value instanceof Component tv) s = tv.getString();
        else if (value instanceof Identifier id) s = id.toString();
        else if (value instanceof String str) s = str;

        s = StringPool.canonical(s);  // may return null

        if (s == null) return fallback;

        ItemModel cached = dynamicCache.get(s);
        if (cached != null) return cached;

        for (Pair<Pattern, ItemModel> rc : regexCases) {
            if (rc.getFirst().matcher(s).matches()) {
                dynamicCache.put(s, rc.getSecond());
                return rc.getSecond();
            }
        }

        dynamicCache.put(s, fallback);
        return fallback;
    }
}
