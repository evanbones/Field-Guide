package com.evandev.fieldguide.optimization;

import com.evandev.fieldguide.entry.EntryResolver;
import java.util.*;
import net.minecraft.resources.ResourceLocation;

/** Session-owned set: all mutation paths update the fallback compatibility index. */
public final class IndexedUnlockSet extends AbstractSet<String> {
    private final Set<String> values = new HashSet<>();
    private final Map<ResourceLocation, int[]> baseIndex = new HashMap<>();
    private final Map<ResourceLocation, Map<String, int[]>> variantIndex = new HashMap<>();
    private boolean hasNull;

    @Override public int size() { return values.size(); }
    @Override public boolean contains(Object key) { return values.contains(key); }
    @Override public boolean add(String value) {
        if (!values.add(value)) return false;
        if (value == null) hasNull = true;
        else change(value, 1);
        return true;
    }
    @Override public boolean remove(Object value) {
        if (!values.remove(value)) return false;
        if (value == null) hasNull = false;
        else if (value instanceof String text) change(text, -1);
        else rebuild();
        return true;
    }
    @Override public void clear() { values.clear(); baseIndex.clear(); variantIndex.clear(); hasNull = false; }
    @Override public Iterator<String> iterator() {
        Iterator<String> delegate = values.iterator();
        return new Iterator<>() {
            private String last;
            @Override public boolean hasNext() { return delegate.hasNext(); }
            @Override public String next() { return last = delegate.next(); }
            @Override public void remove() {
                delegate.remove();
                if (last == null) hasNull = false;
                else change(last, -1);
            }
        };
    }
    private void rebuild() {
        baseIndex.clear(); variantIndex.clear(); hasNull = false;
        for (String value : values) {
            if (value == null) hasNull = true;
            else change(value, 1);
        }
    }
    private void change(String value, int delta) {
        // Invalid legacy identifiers were ignored by the original string overload.
        ResourceLocation raw;
        int type;
        int split = value.indexOf('#');
        String variant = split < 0 ? "" : value.substring(split + 1);
        // A trailing '#' is not a base unlock, and no nonempty variant matches it.
        if (split >= 0 && variant.isEmpty()) return;
        try {
            ResourceLocation stored = ResourceLocation.parse(split < 0 ? value : value.substring(0, split));
            raw = EntryResolver.getRawId(stored);
            var explicit = EntryResolver.explicitType(stored);
            type = explicit == null ? 0 : explicit.ordinal() + 1;
        } catch (Exception invalid) { return; }
        if (variant.isEmpty()) {
            if (delta > 0) baseIndex.computeIfAbsent(raw, ignored -> new int[4])[type]++;
            else {
                int[] counts = baseIndex.get(raw);
                counts[type]--;
                if (empty(counts)) baseIndex.remove(raw);
            }
        } else if (delta > 0) {
            variantIndex.computeIfAbsent(raw, ignored -> new HashMap<>())
                    .computeIfAbsent(variant, ignored -> new int[4])[type]++;
        } else {
            Map<String, int[]> variants = variantIndex.get(raw);
            int[] counts = variants.get(variant);
            counts[type]--;
            if (empty(counts)) variants.remove(variant);
            if (variants.isEmpty()) variantIndex.remove(raw);
        }
    }
    private static boolean empty(int[] counts) { return (counts[0] | counts[1] | counts[2] | counts[3]) == 0; }
    public boolean canUseIndex() { return !hasNull; }
    public boolean matches(ResourceLocation raw, ResourceLocation query, String variant) {
        int[] counts;
        if (variant == null || variant.isEmpty()) counts = baseIndex.get(raw);
        else {
            Map<String, int[]> variants = variantIndex.get(raw);
            if (variants == null) return false;
            counts = variants.get(variant);
        }
        if (counts == null) return false;
        var explicit = EntryResolver.explicitType(query);
        return explicit == null || counts[0] != 0 || counts[explicit.ordinal() + 1] != 0;
    }
}
