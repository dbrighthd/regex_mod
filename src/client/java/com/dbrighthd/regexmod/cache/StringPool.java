package com.dbrighthd.regexmod.cache;

/**
 * Simple canonicalisation utility so repeated equal strings share
 * the *same* object reference, preventing runaway identity growth in
 * ItemRenderState model‑key lists.
 */
public final class StringPool {

    private StringPool() { /* util – no instances */ }

    /** Returns a canonical (pooled) instance for the supplied string. */
    public static String canonical(String s) {
        // Using the VM’s internal pool is fine here – property values are limited.
        return s == null ? null : s.intern();
    }
}
