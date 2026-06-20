package com.dbrighthd.regexmod.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;


public final class RegexCache {

    private static final Map<String, Pattern> REGEX_CACHE = new ConcurrentHashMap<>();

    private RegexCache() {}
    public static Pattern getOrCompilePattern(String regex, int flags) {
        String key = flags + ":" + regex;
        return REGEX_CACHE.computeIfAbsent(key, k -> Pattern.compile(regex, flags));
    }

    public static void clear() {
        REGEX_CACHE.clear();
    }
}
