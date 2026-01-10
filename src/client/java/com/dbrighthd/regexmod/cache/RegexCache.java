package com.dbrighthd.regexmod.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Thread‑safe global cache for compiled regular‑expression Patterns.
 * Keeps memory use low and avoids recompiling the same regex over and over.
 */
public final class RegexCache {

    private static final Map<String, Pattern> REGEX_CACHE = new ConcurrentHashMap<>();

    private RegexCache() {
        /* utility class – no instantiation */
    }

    /**
     * Returns a cached Pattern, compiling it if necessary.
     *
     * @param regex the pattern text (without any custom prefix)
     * @param flags java.util.regex.Pattern flags, e.g. Pattern.CASE_INSENSITIVE
     * @return a compiled Pattern instance
     */
    public static Pattern getOrCompilePattern(String regex, int flags) {
        String key = flags + ":" + regex;
        return REGEX_CACHE.computeIfAbsent(key, k -> Pattern.compile(regex, flags));
    }

    /** Clears the entire pattern cache (use on resource reload if desired). */
    public static void clear() {
        REGEX_CACHE.clear();
    }
}
