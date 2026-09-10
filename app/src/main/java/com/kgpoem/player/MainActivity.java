package com.kgpoem.player;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends AppCompatActivity implements PoemAdapter.Listener {
    private static final int NOTIFICATION_PERMISSION_REQUEST = 9;

    private final List<Poem> allPoems = new ArrayList<>();
    private final List<Poem> visiblePoems = new ArrayList<>();

    private PoemAdapter adapter;
    private SharedPreferences favoritePreferences;
    private SharedPreferences playbackPreferences;
    private LinearLayout miniPlayer;
    private TextView miniTitle;
    private TextView count;
    private ImageButton miniPlay;
    private DancingToyView dancingToy;
    private Button favoritesButton;
    private EditText search;
    private Poem currentPoem;
    private boolean favoritesOnly;
    private boolean receiverRegistered;

    private final BroadcastReceiver playbackReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int number = intent.getIntExtra(PlaybackService.EXTRA_NUMBER, 0);
            boolean playing = intent.getBooleanExtra(PlaybackService.EXTRA_PLAYING, false);
            Poem poem = PoemRepository.findByNumber(MainActivity.this, number);
            if (poem != null) {
                currentPoem = poem;
                showMiniPlayer(poem, playing);
                adapter.setNowPlayingNumber(poem.number);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        favoritePreferences = getSharedPreferences("favorites", MODE_PRIVATE);
        playbackPreferences = getSharedPreferences("playback_state", MODE_PRIVATE);
        loadPoems();
        bindViews();
        configurePoemList();
        configureActions();
        restorePlaybackState();
        requestNotificationPermissionIfNeeded();
    }

    private void loadPoems() {
        allPoems.addAll(PoemRepository.getPoems(this));
        for (Poem poem : allPoems) {
            poem.favorite = favoritePreferences.getBoolean(String.valueOf(poem.number), false);
        }
    }

    private void bindViews() {
        miniPlayer = findViewById(R.id.miniPlayer);
        miniTitle = findViewById(R.id.miniTitle);
        miniPlay = findViewById(R.id.miniPlay);
        dancingToy = findViewById(R.id.dancingToy);
        count = findViewById(R.id.count);
        favoritesButton = findViewById(R.id.favorites);
        search = findViewById(R.id.search);
    }

    private void configurePoemList() {
        RecyclerView list = findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setHasFixedSize(true);
        adapter = new PoemAdapter(visiblePoems, this);
        list.setAdapter(adapter);
        applyFilters();
    }

    private void configureActions() {
        miniPlay.setOnClickListener(view -> sendPlaybackAction(PlaybackService.ACTION_TOGGLE));
        findViewById(R.id.miniPrevious)
                .setOnClickListener(view -> sendPlaybackAction(PlaybackService.ACTION_PREVIOUS));
        findViewById(R.id.miniNext)
                .setOnClickListener(view -> sendPlaybackAction(PlaybackService.ACTION_NEXT));
        findViewById(R.id.miniKaraoke).setOnClickListener(view -> openCurrentKaraoke());
        findViewById(R.id.openKaraoke).setOnClickListener(view -> openCurrentKaraoke());
        findViewById(R.id.openGame)
                .setOnClickListener(view -> startActivity(new Intent(this, GameActivity.class)));

        favoritesButton.setOnClickListener(view -> {
            favoritesOnly = !favoritesOnly;
            applyFilters();
        });

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void applyFilters() {
        String query = search == null
                ? ""
                : search.getText().toString().trim().toLowerCase(Locale.getDefault());
        visiblePoems.clear();
        for (Poem poem : allPoems) {
            boolean matchesQuery = query.isEmpty()
                    || poem.title.toLowerCase(Locale.getDefault()).contains(query)
                    || String.valueOf(poem.number).contains(query);
            if (matchesQuery && (!favoritesOnly || poem.favorite)) {
                visiblePoems.add(poem);
            }
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        if (count != null) {
            count.setText(favoritesOnly
                    ? getString(R.string.favorite_count, visiblePoems.size())
                    : getString(R.string.poem_count, visiblePoems.size()));
        }
        if (favoritesButton != null) {
            favoritesButton.setText(favoritesOnly
                    ? R.string.show_all
                    : R.string.show_favorites);
        }
    }

    private void restorePlaybackState() {
        int number = playbackPreferences.getInt(PlaybackService.EXTRA_NUMBER, 0);
        boolean playing = playbackPreferences.getBoolean(PlaybackService.EXTRA_PLAYING, false);
        currentPoem = PoemRepository.findByNumber(this, number);
        if (currentPoem != null) {
            showMiniPlayer(currentPoem, playing);
            adapter.setNowPlayingNumber(currentPoem.number);
        }
    }

    private void showMiniPlayer(@NonNull Poem poem, boolean playing) {
        miniPlayer.setVisibility(View.VISIBLE);
        miniTitle.setText(poem.title);
        miniPlay.setImageResource(playing
                ? android.R.drawable.ic_media_pause
                : android.R.drawable.ic_media_play);
        miniPlay.setContentDescription(getString(playing ? R.string.pause : R.string.play));
        dancingToy.setDancing(playing);
    }

    @Override
    public void play(@NonNull Poem poem) {
        currentPoem = poem;
        Intent intent = new Intent(this, PlaybackService.class)
                .setAction(PlaybackService.ACTION_PLAY)
                .putExtra(PlaybackService.EXTRA_NUMBER, poem.number);
        ContextCompat.startForegroundService(this, intent);
        showMiniPlayer(poem, true);
        adapter.setNowPlayingNumber(poem.number);
    }

    @Override
    public void openKaraoke(@NonNull Poem poem) {
        currentPoem = poem;
        sendPlaybackAction(PlaybackService.ACTION_PAUSE);
        startActivity(new Intent(this, KaraokeActivity.class)
                .putExtra(PlaybackService.EXTRA_NUMBER, poem.number));
    }

    @Override
    public void toggleFavorite(@NonNull Poem poem) {
        poem.favorite = !poem.favorite;
        favoritePreferences.edit()
                .putBoolean(String.valueOf(poem.number), poem.favorite)
                .apply();
        applyFilters();
    }

    private void openCurrentKaraoke() {
        if (currentPoem == null) {
            currentPoem = allPoems.get(0);
        }
        openKaraoke(currentPoem);
    }

    private void sendPlaybackAction(String action) {
        startService(new Intent(this, PlaybackService.class).setAction(action));
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_REQUEST
            );
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!receiverRegistered) {
            IntentFilter filter = new IntentFilter(PlaybackService.ACTION_STATE);
            ContextCompat.registerReceiver(
                    this,
                    playbackReceiver,
                    filter,
                    ContextCompat.RECEIVER_NOT_EXPORTED
            );
            receiverRegistered = true;
        }
        sendPlaybackAction(PlaybackService.ACTION_QUERY);
    }

    @Override
    protected void onStop() {
        if (receiverRegistered) {
            unregisterReceiver(playbackReceiver);
            receiverRegistered = false;
        }
        dancingToy.setDancing(false);
        super.onStop();
    }
}
