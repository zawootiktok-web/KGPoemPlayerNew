package com.kgpoem.player;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class PoemRepository {
    public static final int POEM_COUNT = 63;

    private static final int[] AUDIO_RESOURCES = {
            R.raw.poem_01, R.raw.poem_02, R.raw.poem_03, R.raw.poem_04, R.raw.poem_05,
            R.raw.poem_06, R.raw.poem_07, R.raw.poem_08, R.raw.poem_09, R.raw.poem_10,
            R.raw.poem_11, R.raw.poem_12, R.raw.poem_13, R.raw.poem_14, R.raw.poem_15,
            R.raw.poem_16, R.raw.poem_17, R.raw.poem_18, R.raw.poem_19, R.raw.poem_20,
            R.raw.poem_21, R.raw.poem_22, R.raw.poem_23, R.raw.poem_24, R.raw.poem_25,
            R.raw.poem_26, R.raw.poem_27, R.raw.poem_28, R.raw.poem_29, R.raw.poem_30,
            R.raw.poem_31, R.raw.poem_32, R.raw.poem_33, R.raw.poem_34, R.raw.poem_35,
            R.raw.poem_36, R.raw.poem_37, R.raw.poem_38, R.raw.poem_39, R.raw.poem_40,
            R.raw.poem_41, R.raw.poem_42, R.raw.poem_43, R.raw.poem_44, R.raw.poem_45,
            R.raw.poem_46, R.raw.poem_47, R.raw.poem_48, R.raw.poem_49, R.raw.poem_50,
            R.raw.poem_51, R.raw.poem_52, R.raw.poem_53, R.raw.poem_54, R.raw.poem_55,
            R.raw.poem_56, R.raw.poem_57, R.raw.poem_58, R.raw.poem_59, R.raw.poem_60,
            R.raw.poem_61, R.raw.poem_62, R.raw.poem_63
    };

    private static volatile List<Poem> cachedPoems;

    private PoemRepository() {
    }

    @NonNull
    public static List<Poem> getPoems(@NonNull Context context) {
        List<Poem> local = cachedPoems;
        if (local == null) {
            synchronized (PoemRepository.class) {
                local = cachedPoems;
                if (local == null) {
                    local = Collections.unmodifiableList(loadPoems(context.getApplicationContext()));
                    cachedPoems = local;
                }
            }
        }
        return local;
    }

    @Nullable
    public static Poem findByNumber(@NonNull Context context, int number) {
        if (number < 1 || number > POEM_COUNT) {
            return null;
        }
        return getPoems(context).get(number - 1);
    }

    @NonNull
    public static String transcriptAssetName(int number) {
        return String.format(Locale.US, "transcripts/poem_%02d.json", number);
    }

    private static List<Poem> loadPoems(Context context) {
        List<Poem> poems = new ArrayList<>(POEM_COUNT);
        for (int number = 1; number <= POEM_COUNT; number++) {
            int resourceId = AUDIO_RESOURCES[number - 1];
            String title = readTitle(context, number);
            if (title.isEmpty()) {
                title = "တေးကဗျာ အမှတ် " + number;
            }
            poems.add(new Poem(number, resourceId, title));
        }
        return poems;
    }

    private static String readTitle(Context context, int number) {
        try (InputStream input = context.getAssets().open(transcriptAssetName(number));
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            String json = output.toString(StandardCharsets.UTF_8.name());
            return new JSONObject(json).optString("title", "").trim();
        } catch (Exception ignored) {
            return "";
        }
    }
}
