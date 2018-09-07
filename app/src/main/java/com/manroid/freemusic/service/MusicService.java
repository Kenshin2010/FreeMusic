package com.manroid.freemusic.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.RemoteControlClient;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.widget.RemoteViews;

import com.manroid.freemusic.R;
import com.manroid.freemusic.model.Song;
import com.manroid.freemusic.constant.Constants;
import com.manroid.freemusic.view.activity.MainActivity;

import java.io.IOException;
import java.util.List;
import java.util.Random;


public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnInfoListener,
        AudioManager.OnAudioFocusChangeListener,
        MediaPlayer.OnCompletionListener {

    private Random random;

    private Song currentsong;
    private MediaPlayer mediaPlayer;
    private final IBinder iBinder = new LocalBinder();
    private AudioManager audioManager;
    private BroadcastReceiver broadcastReceiver;
    private NotificationManager notificationManager;
    private Notification notification;

    private PendingIntent pendingIntent;
    private PendingIntent quitPendingIntent;
    private PendingIntent previousPendingIntent;
    private PendingIntent playpausePendingIntent;
    private PendingIntent nextPendingIntent;

    private boolean shuffle, repeat, repeatAll;
    private RemoteControlClient remoteControlClient;
    private TelephonyManager telephonyManager;
    private MusicPhoneStateListener phoneStateListener;

    private ComponentName mediaButtonReceiverComponent;
    private Bitmap icon;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onCreate() {
        super.onCreate();

        random = new Random(System.nanoTime());

        // Initialize the telephony manager
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        phoneStateListener = new MusicPhoneStateListener();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Initialize pending intents
        quitPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(Constants.ACTION_QUIT), 0);
        previousPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(Constants.ACTION_PREVIOUS), 0);
        playpausePendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(Constants.ACTION_PLAY_PAUSE_NOTIFICATION), 0);
        nextPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(Constants.ACTION_NEXT), 0);
        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_UPDATE_CURRENT);

        initMediaPlayer();

        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE); // Start listen for telephony events

        // Inizialize the audio manager
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mediaButtonReceiverComponent = new ComponentName(getPackageName(), MediaButtonReceiver.class.getName());
        audioManager.registerMediaButtonEventReceiver(mediaButtonReceiverComponent);
        audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        // Initialize remote control client
        icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setComponent(mediaButtonReceiverComponent);
        PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, mediaButtonIntent, 0);

        remoteControlClient = new RemoteControlClient(mediaPendingIntent);
        remoteControlClient.setTransportControlFlags(RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE | RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS | RemoteControlClient.FLAG_KEY_MEDIA_NEXT);
        audioManager.registerRemoteControlClient(remoteControlClient);

        updateNotificationMessage();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_QUIT);
        intentFilter.addAction(Constants.ACTION_PREVIOUS);
        intentFilter.addAction(Constants.ACTION_PREVIOUS_NORESTART);
        intentFilter.addAction(Constants.ACTION_PLAY_PAUSE_NOTIFICATION);
        intentFilter.addAction(Constants.ACTION_NEXT);
        intentFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        broadcastReceiver = new BroadcastReceiver() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                switch (action) {
                    case Constants.ACTION_QUIT:
                        sendBroadcast(new Intent(Constants.ACTION_QUIT_ACTIVITY));
                        sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
                        stopSelf();
                        return;
                    case Constants.ACTION_PREVIOUS:
                        previousItem(false);
                        break;
                    case Constants.ACTION_PREVIOUS_NORESTART:
                        previousItem(true);
                        break;
                    case Constants.ACTION_PLAY_PAUSE_NOTIFICATION:
                        playPause();
                        break;
                    case Constants.ACTION_NEXT:
                        nextSongType();
                        break;
                    case AudioManager.ACTION_AUDIO_BECOMING_NOISY:

                        break;
                }
            }
        };

        registerReceiver(broadcastReceiver, intentFilter);
        startForeground(Constants.NOTIFICATION_MAIN, notification);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (requestAudioFocus() == false) {
            stopSelf();
        }
        return START_STICKY;
    }


    /* Phone state listener class. */
    private class MusicPhoneStateListener extends PhoneStateListener {

        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {
                case TelephonyManager.CALL_STATE_IDLE:

                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:

                    break;
                case TelephonyManager.CALL_STATE_RINGING:

                    break;
            }
        }
    }


    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onCompletion(MediaPlayer mp) {
        nextSongType();
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
        Intent notIntent = new Intent(this, MainActivity.class);
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        updateNotificationMessage();
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {

    }

    public void initMediaPlayer() {
        if (mediaPlayer == null)
            mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnInfoListener(this);
    }

    public boolean isRepeat() {
        return repeat;
    }

    public void setRepeat(boolean repeat) {
        this.repeat = repeat;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void playSong(Song song) {
        currentsong = song;
        mediaPlayer.reset();
        mediaPlayer.setOnCompletionListener(null);
        try {
            long currentSongId = song.getID();
            Uri trackUri = ContentUris.withAppendedId(
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    currentSongId);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(getApplicationContext(), trackUri);
            try {
                mediaPlayer.prepare();
            } catch (IOException e) {
                currentsong = null;
            }
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.start();
            sendBroadcast(new Intent(Constants.ACTION_NEW_SONG)); // Sends a broadcast to the activity

        } catch (Exception e) {
            currentsong = null;
            updateNotificationMessage();
            sendBroadcast(new Intent(Constants.ACTION_NEW_SONG)); // Sends a broadcast to the activity
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void playPause() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        } else {
            mediaPlayer.start();
        }
        updateNotificationMessage();
        sendBroadcast(new Intent(Constants.ACTION_PLAY_PAUSE));
    }

    public boolean isPlayingSong() {
        if (currentsong == null) return false;
        return mediaPlayer.isPlaying();
    }

    public int getDuration() {
        if (currentsong == null) return 100;
        return mediaPlayer.getDuration();
    }

    public int getCurrentPosition() {
        if (currentsong == null) return 0;
        return mediaPlayer.getCurrentPosition();
    }


    public boolean isShuffle() {
        return shuffle;
    }

    public void setShuffle(boolean shuffle) {
        this.shuffle = shuffle;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void nextSongType() {
        if (currentsong == null) {
            return;
        }

        if (repeat) {
            playSong(currentsong);
            return;
        }

        if (shuffle) {
            randomItem();
            return;
        }

        Song nextItem = currentsong.getNext(repeatAll);
        if (nextItem == null) {
            if (!isPlayingSong()) ;
            sendBroadcast(new Intent(Constants.ACTION_NEW_SONG)); // Notify the activity that there are no more songs to be played
            updateNotificationMessage();
        } else {
            playSong(nextItem);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void randomItem() {
        Song randomSong = currentsong.getRandom(random);
        if (randomSong != null) {
            playSong(randomSong);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void previousItem(boolean noRestart) {
        if (currentsong == null) return;

        if (!noRestart && getCurrentPosition() > 2000) {
            playSong(currentsong);
            return;
        }

        if (repeat) {
            playSong(currentsong);
            return;
        }

        if (shuffle) {
//            randomItem();
            return;
        }

        Song song = currentsong.getPrevious();
        if (song != null) {
            playSong(song);
        }
    }

    public void seekTo(int progress) {
        mediaPlayer.seekTo(progress);
        sendPlayingStateBroadcast();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE); // Stop listen for telephony events
        notificationManager.cancel(Constants.NOTIFICATION_MAIN);
        audioManager.unregisterRemoteControlClient(remoteControlClient);
        audioManager.unregisterMediaButtonEventReceiver(mediaButtonReceiverComponent);
        audioManager.abandonAudioFocus(null);
        mediaPlayer.release();
        stopForeground(true);
        unregisterReceiver(broadcastReceiver);
        removeAudioFocus();
    }

    //release resources when unbind
    @Override
    public boolean onUnbind(Intent intent) {
        mediaPlayer.stop();
        mediaPlayer.release();
        return false;
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                if (mediaPlayer == null) initMediaPlayer();
                else if (!mediaPlayer.isPlaying()) mediaPlayer.start();
                mediaPlayer.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (mediaPlayer.isPlaying()) mediaPlayer.pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (mediaPlayer.isPlaying()) mediaPlayer.setVolume(0.1f, 0.1f);
                break;
        }
    }

    public class LocalBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }


    private boolean requestAudioFocus() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            //Focus gained
            return true;
        }
        //Could not gain focus
        return false;
    }

    private boolean removeAudioFocus() {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
                audioManager.abandonAudioFocus(this);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void updateNotificationMessage() {

        if (currentsong == null) {
            remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
        } else {
            if (isPlayingSong()) {
                remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
            } else {
                remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
            }
            RemoteControlClient.MetadataEditor metadataEditor = remoteControlClient.editMetadata(true);
            metadataEditor.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, currentsong.getTitle());
            metadataEditor.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, currentsong.getArtist());
            metadataEditor.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, currentsong.getArtist());
            metadataEditor.putLong(MediaMetadataRetriever.METADATA_KEY_DURATION, getDuration());
            metadataEditor.apply();
        }

        sendPlayingStateBroadcast();

        /* Update notification */
        Notification.Builder notificationBuilder = new Notification.Builder(this);
        notificationBuilder.setSmallIcon(R.drawable.audio_white);
        notificationBuilder.setContentIntent(pendingIntent);
        notificationBuilder.setOngoing(true);

        if (Build.VERSION.SDK_INT >= 21) {
            int playPauseIcon = isPlayingSong() ? R.drawable.button_pause : R.drawable.button_play;
            if (currentsong == null) {
                notificationBuilder.setContentTitle(getString(R.string.noSong));
                notificationBuilder.setContentText(getString(R.string.app_name));
            } else {
                notificationBuilder.setContentTitle(currentsong.getTitle());
                notificationBuilder.setContentText(currentsong.getArtist());
            }
            notificationBuilder.addAction(R.drawable.button_quit, getString(R.string.quit), quitPendingIntent);
            notificationBuilder.addAction(R.drawable.button_previous, getString(R.string.previous), previousPendingIntent);
            notificationBuilder.addAction(playPauseIcon, getString(R.string.pause), playpausePendingIntent);
            notificationBuilder.addAction(R.drawable.button_next, getString(R.string.next), nextPendingIntent);
            notificationBuilder.setColor(getResources().getColor(R.color.colorTextNotification));
            notificationBuilder.setStyle(new Notification.MediaStyle().setShowActionsInCompactView(2));
            notification = notificationBuilder.build();
        } else {
            int playPauseIcon = isPlayingSong() ? R.drawable.pause : R.drawable.play;
            notificationBuilder.setContentTitle(getResources().getString(R.string.app_name));

            RemoteViews notificationLayout = new RemoteViews(getPackageName(), R.layout.layout_notification);

            if (currentsong == null) {
                notificationLayout.setTextViewText(R.id.textViewArtist, getString(R.string.app_name));
                notificationLayout.setTextViewText(R.id.textViewTitle, getString(R.string.noSong));
                notificationLayout.setImageViewBitmap(R.id.imageViewNotification, BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));
            } else {
                String title = currentsong.getArtist();
                if (!title.equals("")) title += " - ";
                title += currentsong.getTitle();
                notificationBuilder.setContentText(title);
                notificationLayout.setTextViewText(R.id.textViewArtist, currentsong.getArtist());
                notificationLayout.setTextViewText(R.id.textViewTitle, currentsong.getTitle());
                notificationLayout.setImageViewBitmap(R.id.imageViewNotification, BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));
            }
            notificationLayout.setOnClickPendingIntent(R.id.buttonNotificationQuit, quitPendingIntent);
            notificationLayout.setOnClickPendingIntent(R.id.buttonNotificationPrevious, previousPendingIntent);
            notificationLayout.setImageViewResource(R.id.buttonNotificationPlayPause, playPauseIcon);
            notificationLayout.setOnClickPendingIntent(R.id.buttonNotificationPlayPause, playpausePendingIntent);
            notificationLayout.setOnClickPendingIntent(R.id.buttonNotificationNext, nextPendingIntent);
            notification = notificationBuilder.build();
            notification.bigContentView = notificationLayout;
        }

        notificationManager.notify(Constants.NOTIFICATION_MAIN, notification);
    }

    private void sendPlayingStateBroadcast() {
        Intent intent = new Intent();
        //intent.setAction("com.android.music.metachanged");
        intent.setAction("com.android.music.playstatechanged");
        Bundle bundle = new Bundle();
        if (currentsong != null) {
            bundle.putString("track", currentsong.getTitle());
            bundle.putString("artist", currentsong.getArtist());
            bundle.putLong("duration", mediaPlayer.getDuration());
            bundle.putLong("position", mediaPlayer.getCurrentPosition());
            bundle.putBoolean("playing", mediaPlayer.isPlaying());
        } else {
            bundle.putBoolean("playing", false);
        }
        intent.putExtras(bundle);
        sendBroadcast(intent);
    }


    public static class MediaButtonReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event.getAction() != KeyEvent.ACTION_DOWN) return;
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    context.sendBroadcast(new Intent(Constants.ACTION_PLAY_PAUSE_NOTIFICATION));
                    return;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    context.sendBroadcast(new Intent(Constants.ACTION_PREVIOUS_NORESTART));
                    return;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    context.sendBroadcast(new Intent(Constants.ACTION_NEXT));
                    return;
            }
        }
    }

}
