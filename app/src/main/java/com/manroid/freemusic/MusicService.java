package com.manroid.freemusic;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.util.List;


public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnInfoListener,
        MediaPlayer.OnCompletionListener {

    private Song song;
    private boolean shuffle = false;
    private MediaPlayer mediaPlayer;
    private List<Song> listSong;
    private int songPosition;
    private final IBinder iBinder = new LocalBinder();
    private int NOTIFY_ID = 100;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        songPosition = 0;
        initMediaPlayer();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {

    }

    @Override
    public void onCompletion(MediaPlayer mp) {
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
        //start playback
        mp.start();
        //notification
        Intent notIntent = new Intent(this, MainActivity.class);
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendInt = PendingIntent.getActivity(this, 0,
                notIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(this);

        builder.setContentIntent(pendInt)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker("test ticker")
                .setOngoing(true)
                .setContentTitle("Playing")
                .setContentText("test content text");
        Notification not = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            not = builder.build();
        }
        startForeground(NOTIFY_ID, not);

    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {

    }

    public void initMediaPlayer() {
        if (mediaPlayer == null)
            mediaPlayer = new MediaPlayer();//new MediaPlayer instance

        //Set up MediaPlayer event listeners
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnInfoListener(this);
    }

    public void playSong(Song song) {
        //Reset so that the MediaPlayer is not pointing to another data source
        mediaPlayer.reset();
        long currentSongId = song.getID();
        Uri trackUri = ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                currentSongId);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            // Set the data source to the mediaFile location
            mediaPlayer.setDataSource(getApplicationContext(), trackUri);
        } catch (IOException e) {
            e.printStackTrace();
            stopSelf();
        }
        mediaPlayer.prepareAsync();
    }

    public void pause() {

    }

    public void resume() {

    }

    public void next() {

    }

    public void prev() {

    }

    public boolean isPlayingSong() {
        return false;
    }

    public int getDuration() {
        return 0;
    }

    public int getTotalTime() {
        return 0;
    }

    public void setShuffle() {
        if (shuffle) shuffle = !shuffle;
        else shuffle = !shuffle;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
    }

    //release resources when unbind
    @Override
    public boolean onUnbind(Intent intent){
        mediaPlayer.stop();
        mediaPlayer.release();
        return false;
    }

    public class LocalBinder extends Binder {
        public MusicService getService() {
            // Return this instance of LocalService so clients can call public methods
            return MusicService.this;
        }
    }

}
