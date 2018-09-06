package com.manroid.freemusic.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.manroid.freemusic.R;
import com.manroid.freemusic.model.Song;
import com.manroid.freemusic.constant.Constants;
import com.manroid.freemusic.view.activity.MainActivity;

import java.io.IOException;
import java.util.List;


public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnInfoListener,
        AudioManager.OnAudioFocusChangeListener,
        MediaPlayer.OnCompletionListener {


    private Song currentsong;
    private MediaPlayer mediaPlayer;
    private List<Song> listSong;
    private int songPosition;
    private final IBinder iBinder = new LocalBinder();
    private int NOTIFY_ID = 100;
    private AudioManager audioManager;
    private BroadcastReceiver broadcastReceiver;
    private NotificationManager notificationManager;
    private Notification notification;
    private PendingIntent pendingIntent;
    private boolean shuffle, repeat, repeatAll;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_UPDATE_CURRENT);
        updateNotificationMessage();
        initMediaPlayer();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (requestAudioFocus() == false) {
            stopSelf();
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_PREVIOUS);
        intentFilter.addAction(Constants.ACTION_PLAY_PAUSE);
        intentFilter.addAction(Constants.ACTION_NEXT);
        intentFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                switch (action) {
//                    case "quit":
//                        // do something
//                        return;
//                    case Constants.ACTION_PREVIOUS:
//                        prevSong();
//                        break;
//                    case Constants.ACTION_PLAY_PAUSE:
//                        break;
//                    case Constants.ACTION_NEXT:
//                        break;
//                    case AudioManager.ACTION_AUDIO_BECOMING_NOISY:
//                        break;
                }
            }
        };

        registerReceiver(broadcastReceiver, intentFilter);


        startForeground(Constants.NOTIFICATION_MAIN, notification);


        return START_STICKY;
    }


    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {

    }

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

    public void playPause() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        } else {
            mediaPlayer.start();
        }
        updateNotificationMessage();
        sendBroadcast(new Intent(Constants.ACTION_PLAY_PAUSE));
    }

    public void pauseSong() {

    }

    public void resume() {

    }

    public void nextSong() {

    }

    public void prevSong() {

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


    public void setShuffle() {
        if (shuffle) shuffle = !shuffle;
        else shuffle = !shuffle;
    }


    public Song getCurrentsong() {
        return currentsong;
    }


    public void nextSongType() {
        if (currentsong == null) {
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

        Song nextItem = currentsong.getNext(repeatAll);
        if (nextItem == null) {
            if (!isPlayingSong()) ;
            sendBroadcast(new Intent(Constants.ACTION_NEW_SONG)); // Notify the activity that there are no more songs to be played
            updateNotificationMessage();
        } else {
            playSong(nextItem);
        }
    }

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

        Song song= currentsong.getPrevious();
        if (song != null) {
            playSong(song);
        }
    }

    public void seekTo(int progress) {
        mediaPlayer.seekTo(progress);
        sendPlayingStateBroadcast();
    }

    private void sendPlayingStateBroadcast() {
        Intent intent = new Intent();
        intent.setAction(Constants.ACTION_PLAY_STATE_CHANGE);
        Bundle bundle = new Bundle();
        bundle.putLong("duration", mediaPlayer.getDuration());
        bundle.putLong("position", mediaPlayer.getCurrentPosition());
        bundle.putBoolean("playing", mediaPlayer.isPlaying());
        intent.putExtras(bundle);
        sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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

    private void updateNotificationMessage() {
        Notification.Builder notificationBuilder = new Notification.Builder(this);
        notificationBuilder.setSmallIcon(R.mipmap.ic_launcher);
        notificationBuilder.setContentIntent(pendingIntent);
        notificationBuilder.setOngoing(true);
        notificationBuilder.setContentTitle(getResources().getString(R.string.app_name));
        notification = notificationBuilder.build();
        notificationManager.notify(Constants.NOTIFICATION_MAIN, notification);
    }


}
