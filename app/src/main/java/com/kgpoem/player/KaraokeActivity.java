package com.kgpoem.player;

import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class KaraokeActivity extends AppCompatActivity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<KaraokeLine> lines = new ArrayList<>();

    private TextView title;
    private TextView progress;
    private Button playButton;
    private Button previousButton;
    private Button nextButton;
    private CheckBox autoNext;
    private SeekBar seekBar;
    private RecyclerView lyricsList;
    private KaraokeAdapter lyricsAdapter;
    private MediaPlayer player;
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private Poem currentPoem;
    private int currentNumber;
    private boolean userSeeking;

    private final Runnable progressUpdater = new Runnable() {
        @Override
        public void run() {
            updateProgress();
            if (player != null && player.isPlaying()) {
                handler.postDelayed(this, 120L);
            }
        }
    };

    private final AudioManager.OnAudioFocusChangeListener focusChangeListener = focusChange -> {
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS
                || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            pausePlayback();
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
                && player != null) {
            player.setVolume(0.25f, 0.25f);
        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN && player != null) {
            player.setVolume(1f, 1f);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_karaoke);

        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        bindViews();
        configureLyricsList();
        configureActions();

        startService(new Intent(this, PlaybackService.class).setAction(PlaybackService.ACTION_PAUSE));
        int requestedNumber = getIntent().getIntExtra(PlaybackService.EXTRA_NUMBER, 1);
        loadPoem(requestedNumber, false);
    }

    private void bindViews() {
        title = findViewById(R.id.karaokeTitle);
        progress = findViewById(R.id.karaokeProgress);
        playButton = findViewById(R.id.karaokePlay);
        previousButton = findViewById(R.id.karaokePrevious);
        nextButton = findViewById(R.id.karaokeNext);
        autoNext = findViewById(R.id.karaokeAutoNext);
        seekBar = findViewById(R.id.karaokeSeek);
        lyricsList = findViewById(R.id.lyricsList);
    }

    private void configureLyricsList() {
        lyricsAdapter = new KaraokeAdapter();
        lyricsList.setLayoutManager(new LinearLayoutManager(this));
        lyricsList.setAdapter(lyricsAdapter);
    }

    private void configureActions() {
        findViewById(R.id.back).setOnClickListener(view -> finish());
        playButton.setOnClickListener(view -> togglePlayback());
        previousButton.setOnClickListener(view -> moveToPoem(-1));
        nextButton.setOnClickListener(view -> moveToPoem(1));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int value, boolean fromUser) {
                if (fromUser && player != null) {
                    progress.setText(formatProgress(value, player.getDuration()));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
                userSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
                if (player != null) {
                    player.seekTo(bar.getProgress());
                    updateProgress();
                }
                userSeeking = false;
            }
        });
    }

    private void loadPoem(int number, boolean startImmediately) {
        Poem poem = PoemRepository.findByNumber(this, number);
        if (poem == null) {
            return;
        }

        releasePlayer();
        currentPoem = poem;
        currentNumber = poem.number;
        title.setText(getString(R.string.karaoke_title_format, poem.number, poem.title));
        previousButton.setEnabled(poem.number > 1);
        nextButton.setEnabled(poem.number < PoemRepository.POEM_COUNT);
        playButton.setText(R.string.start_singing);
        seekBar.setProgress(0);
        progress.setText(formatProgress(0, 0));

        loadLyrics(poem.number);
        player = MediaPlayer.create(this, poem.resourceId);
        if (player == null) {
            Toast.makeText(this, R.string.audio_load_failed, Toast.LENGTH_LONG).show();
            playButton.setEnabled(false);
            return;
        }
        playButton.setEnabled(true);
        seekBar.setMax(player.getDuration());
        progress.setText(formatProgress(0, player.getDuration()));
        player.setOnCompletionListener(ignored -> onPoemCompleted());
        player.setOnErrorListener((ignored, what, extra) -> {
            Toast.makeText(this, R.string.audio_playback_failed, Toast.LENGTH_LONG).show();
            onPoemCompleted();
            return true;
        });

        if (startImmediately) {
            startPlayback();
        }
    }

    private void loadLyrics(int number) {
        lines.clear();
        try {
            JSONObject root = new JSONObject(readAsset(PoemRepository.transcriptAssetName(number)));
            JSONArray lyricArray = root.getJSONArray("lines");
            for (int index = 0; index < lyricArray.length(); index++) {
                JSONObject item = lyricArray.getJSONObject(index);
                long start = Math.round(item.getDouble("start") * 1000.0);
                long end = Math.round(item.getDouble("end") * 1000.0);
                String text = item.getString("text").trim();
                if (!text.isEmpty()) {
                    lines.add(new KaraokeLine(start, end, text));
                }
            }
        } catch (Exception ignored) {
            lines.add(new KaraokeLine(0L, Long.MAX_VALUE, getString(R.string.lyrics_unavailable)));
        }
        lyricsAdapter.submitLines(lines);
        lyricsList.scrollToPosition(0);
    }

    private String readAsset(@NonNull String assetName) throws Exception {
        try (InputStream input = getAssets().open(assetName);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }

    private void togglePlayback() {
        if (player == null) {
            return;
        }
        if (player.isPlaying()) {
            pausePlayback();
        } else {
            if (player.getCurrentPosition() >= player.getDuration() - 200) {
                player.seekTo(0);
            }
            startPlayback();
        }
    }

    private void startPlayback() {
        if (player == null || !requestAudioFocus()) {
            return;
        }
        player.setVolume(1f, 1f);
        player.start();
        playButton.setText(R.string.pause_singing);
        handler.removeCallbacks(progressUpdater);
        handler.post(progressUpdater);
    }

    private void pausePlayback() {
        if (player != null && player.isPlaying()) {
            player.pause();
        }
        playButton.setText(R.string.continue_singing);
        handler.removeCallbacks(progressUpdater);
        updateProgress();
    }

    private void moveToPoem(int offset) {
        boolean continuePlaying = player != null && player.isPlaying();
        int target = Math.max(1, Math.min(PoemRepository.POEM_COUNT, currentNumber + offset));
        if (target != currentNumber) {
            loadPoem(target, continuePlaying);
        }
    }

    private void onPoemCompleted() {
        handler.removeCallbacks(progressUpdater);
        updateProgress();
        if (autoNext.isChecked() && currentNumber < PoemRepository.POEM_COUNT) {
            handler.postDelayed(() -> loadPoem(currentNumber + 1, true), 450L);
        } else {
            playButton.setText(R.string.sing_again);
        }
    }

    private void updateProgress() {
        if (player == null) {
            return;
        }
        int position = player.getCurrentPosition();
        int duration = player.getDuration();
        if (!userSeeking) {
            seekBar.setProgress(position);
        }
        progress.setText(formatProgress(position, duration));

        int activePosition = findActiveLine(position);
        if (lyricsAdapter.setActivePosition(activePosition)
                && activePosition != RecyclerView.NO_POSITION) {
            lyricsList.smoothScrollToPosition(activePosition);
        }
    }

    private int findActiveLine(long positionMs) {
        for (int index = 0; index < lines.size(); index++) {
            KaraokeLine line = lines.get(index);
            if (positionMs < line.startMs) {
                return RecyclerView.NO_POSITION;
            }
            if (positionMs < line.endMs) {
                return index;
            }
        }
        return RecyclerView.NO_POSITION;
    }

    private boolean requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioFocusRequest == null) {
                AudioAttributes attributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build();
                audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAudioAttributes(attributes)
                        .setOnAudioFocusChangeListener(focusChangeListener)
                        .build();
            }
            return audioManager.requestAudioFocus(audioFocusRequest)
                    == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }
        return audioManager.requestAudioFocus(
                focusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
        ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    private void abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest);
        } else {
            audioManager.abandonAudioFocus(focusChangeListener);
        }
    }

    private String formatProgress(long positionMs, long durationMs) {
        return getString(
                R.string.karaoke_progress_format,
                formatTime(positionMs),
                formatTime(durationMs)
        );
    }

    private static String formatTime(long milliseconds) {
        long totalSeconds = Math.max(0L, milliseconds / 1000L);
        return String.format(Locale.US, "%02d:%02d", totalSeconds / 60L, totalSeconds % 60L);
    }

    private void releasePlayer() {
        handler.removeCallbacks(progressUpdater);
        if (player != null) {
            player.setOnCompletionListener(null);
            player.setOnErrorListener(null);
            player.release();
            player = null;
        }
    }

    @Override
    protected void onStop() {
        pausePlayback();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        releasePlayer();
        abandonAudioFocus();
        super.onDestroy();
    }
}
