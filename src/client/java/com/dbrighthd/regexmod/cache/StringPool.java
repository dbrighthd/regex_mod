package com.dbrighthd.regexmod.cache;

public final class StringPool {

    private StringPool() {}

    public static String canonical(String s) {
        return s == null ? null : s.intern();
    }
}
