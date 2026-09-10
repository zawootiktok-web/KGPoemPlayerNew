package com.kgpoem.player;

import androidx.annotation.NonNull;

public final class Poem {
    public final int number;
    public final int resourceId;
    @NonNull public final String title;
    public boolean favorite;

    public Poem(int number, int resourceId, @NonNull String title) {
        this.number = number;
        this.resourceId = resourceId;
        this.title = title;
    }
}
