package com.kgpoem.player;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public final class PlaybackService extends Service {
    public static final String ACTION_PLAY = "com.kgpoem.player.action.PLAY";
    public static final String ACTION_TOGGLE = "com.kgpoem.player.action.TOGGLE";
    public static final String ACTION_PAUSE = "com.kgpoem.player.action.PAUSE";
    public static final String ACTION_NEXT = "com.kgpoem.player.action.NEXT";
    public static final String ACTION_PREVIOUS = "com.kgpoem.player.action.PREVIOUS";
    public static final String ACTION_STOP = "com.kgpoem.player.action.STOP";
    public static final String ACTION_QUERY = "com.kgpoem.player.action.QUERY";
    public static final String ACTION_STATE = "com.kgpoem.player.PLAYBACK_STATE";

    public static final String EXTRA_NUMBER = "poem_number";
    public static final String EXTRA_TITLE = "poem_title";
    public static final String EXTRA_PLAYING = "is_playing";
    public static final String EXTRA_POSITION = "position_ms";
    public static final String EXTRA_DURATION = "duration_ms";

    private static final String CHANNEL_ID = "kg_poem_audio";
    private static final int NOTIFICATION_ID = 7;
    private static final String STATE_PREFERENCES = "playback_state";

    private MediaPlayer player;
    private Poem currentPoem;
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private SharedPreferences statePreferences;
    private boolean resumeAfterFocusGain;

    private final AudioManager.OnAudioFocusChangeListener focusChangeListener = focusChange -> {
        if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            if (resumeAfterFocusGain && player != null && !player.isPlaying()) {
                player.start();
                resumeAfterFocusGain = false;
                publishState();
            }
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
                || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            resumeAfterFocusGain = isPlaying();
            pauseInternal();
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            resumeAfterFocusGain = false;
            pauseInternal();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        statePreferences = getSharedPreferences(STATE_PREFERENCES, MODE_PRIVATE);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            return START_NOT_STICKY;
        }

        switch (intent.getAction()) {
            case ACTION_PLAY:
                playNumber(intent.getIntExtra(EXTRA_NUMBER, 1));
                break;
            case ACTION_TOGGLE:
                togglePlayback();
                break;
            case ACTION_PAUSE:
                pauseInternal();
                break;
            case ACTION_NEXT:
                playRelative(1);
                break;
            case ACTION_PREVIOUS:
                playPrevious();
                break;
            case ACTION_STOP:
                stopPlayback();
                break;
            case ACTION_QUERY:
                restoreCurrentPoemIfNeeded();
                publishState();
                if (player == null) {
                    stopSelfResult(startId);
                }
                break;
            default:
                break;
        }
        return START_NOT_STICKY;
    }

    private void playNumber(int number) {
        Poem poem = PoemRepository.findByNumber(this, number);
        if (poem == null || poem.resourceId == 0) {
            stopPlayback();
            return;
        }
        if (!requestAudioFocus()) {
            return;
        }

        releasePlayer();
        currentPoem = poem;
        player = MediaPlayer.create(this, poem.resourceId);
        if (player == null) {
            playRelative(1);
            return;
        }

        player.setOnCompletionListener(ignored -> {
            if (currentPoem != null && currentPoem.number < PoemRepository.POEM_COUNT) {
                playNumber(currentPoem.number + 1);
            } else {
                stopPlayback();
            }
        });
        player.setOnErrorListener((ignored, what, extra) -> {
            if (currentPoem != null && currentPoem.number < PoemRepository.POEM_COUNT) {
                playNumber(currentPoem.number + 1);
            } else {
                stopPlayback();
            }
            return true;
        });
        player.start();
        startForeground(NOTIFICATION_ID, buildNotification());
        publishState();
    }

    private void togglePlayback() {
        if (player == null) {
            restoreCurrentPoemIfNeeded();
            playNumber(currentPoem == null ? 1 : currentPoem.number);
        } else if (player.isPlaying()) {
            pauseInternal();
        } else if (requestAudioFocus()) {
            player.start();
            startForeground(NOTIFICATION_ID, buildNotification());
            publishState();
        }
    }

    private void playRelative(int offset) {
        restoreCurrentPoemIfNeeded();
        int currentNumber = currentPoem == null ? 1 : currentPoem.number;
        int target = Math.max(1, Math.min(PoemRepository.POEM_COUNT, currentNumber + offset));
        if (target != currentNumber || player == null) {
            playNumber(target);
        }
    }

    private void playPrevious() {
        if (player != null && player.getCurrentPosition() > 3000) {
            player.seekTo(0);
            publishState();
            return;
        }
        playRelative(-1);
    }

    private void pauseInternal() {
        if (player != null && player.isPlaying()) {
            player.pause();
        }
        publishState();
    }

    private void stopPlayback() {
        releasePlayer();
        abandonAudioFocus();
        publishState();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }
        stopSelf();
    }

    private void restoreCurrentPoemIfNeeded() {
        if (currentPoem == null) {
            int number = statePreferences.getInt(EXTRA_NUMBER, 1);
            currentPoem = PoemRepository.findByNumber(this, number);
        }
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

    private void publishState() {
        boolean playing = isPlaying();
        int number = currentPoem == null ? 0 : currentPoem.number;
        String title = currentPoem == null ? getString(R.string.app_name) : currentPoem.title;
        int position = player == null ? 0 : player.getCurrentPosition();
        int duration = player == null ? 0 : player.getDuration();

        statePreferences.edit()
                .putInt(EXTRA_NUMBER, number)
                .putString(EXTRA_TITLE, title)
                .putBoolean(EXTRA_PLAYING, playing)
                .apply();

        Intent state = new Intent(ACTION_STATE)
                .setPackage(getPackageName())
                .putExtra(EXTRA_NUMBER, number)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_PLAYING, playing)
                .putExtra(EXTRA_POSITION, position)
                .putExtra(EXTRA_DURATION, duration);
        sendBroadcast(state);

        if (currentPoem != null && player != null) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.notify(NOTIFICATION_ID, buildNotification());
        }
    }

    private boolean isPlaying() {
        return player != null && player.isPlaying();
    }

    private NotificationCompat.Builder notificationBuilder() {
        Intent openApp = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                this,
                100,
                openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        String title = currentPoem == null ? getString(R.string.app_name) : currentPoem.title;
        int number = currentPoem == null ? 0 : currentPoem.number;

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle(title)
                .setContentText(number > 0
                        ? getString(R.string.notification_queue_position, number, PoemRepository.POEM_COUNT)
                        : getString(R.string.app_name))
                .setContentIntent(contentIntent)
                .setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(isPlaying())
                .addAction(
                        android.R.drawable.ic_media_previous,
                        getString(R.string.previous),
                        servicePendingIntent(ACTION_PREVIOUS, 101)
                )
                .addAction(
                        isPlaying() ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play,
                        getString(isPlaying() ? R.string.pause : R.string.play),
                        servicePendingIntent(ACTION_TOGGLE, 102)
                )
                .addAction(
                        android.R.drawable.ic_media_next,
                        getString(R.string.next),
                        servicePendingIntent(ACTION_NEXT, 103)
                );
    }

    private android.app.Notification buildNotification() {
        return notificationBuilder().build();
    }

    private PendingIntent servicePendingIntent(String action, int requestCode) {
        Intent intent = new Intent(this, PlaybackService.class).setAction(action);
        return PendingIntent.getService(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.playback_channel_name),
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription(getString(R.string.playback_channel_description));
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private void releasePlayer() {
        if (player != null) {
            player.setOnCompletionListener(null);
            player.setOnErrorListener(null);
            player.release();
            player = null;
        }
    }

    @Override
    public void onDestroy() {
        releasePlayer();
        abandonAudioFocus();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
