package com.kgpoem.player;

import androidx.annotation.NonNull;

public final class KaraokeLine {
    public final long startMs;
    public final long endMs;
    @NonNull public final String text;

    public KaraokeLine(long startMs, long endMs, @NonNull String text) {
        this.startMs = startMs;
        this.endMs = endMs;
        this.text = text;
    }
}
