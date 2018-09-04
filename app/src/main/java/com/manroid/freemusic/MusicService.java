package com.manroid.freemusic;

import android.app.Service;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;

public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnCompletionListener {


    public static final String ACTION_BOO = "com.manroid.freemusic.action.BOO";
    private MediaPlayer mMediaPlayer;
    private boolean CanDo = true;
    private int mPosition = 0;
    private ArrayList<Integer> mPlaylistNum;
    private ArrayList<String> mPlaylist;
    private boolean Flag = true;
    private String mMusicTitleOld;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnBufferingUpdateListener(this);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnSeekCompleteListener(this);
        mMediaPlayer.setOnCompletionListener(this);


    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {

    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (mPosition == (mPlaylistNum.size() - 1)) {
            mPosition = 0;
        } else {
            mPosition++;
        }
        startNewMusic(mPlaylist.get(mPosition), mPosition);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
        CanDo = true;
        sendBroadcast(new Intent(MusicService.ACTION_BOO).putExtra(MainActivity.UPDATE_ICON, 8));

    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {

    }


    private void startNewMusic(String musicFileName, int position) {
        Flag = true;
        try {
            mMediaPlayer.reset();
            mMusicTitleOld = musicFileName;
            AssetFileDescriptor afd = am.openFd("music/" + musicFileName);
            mMediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mMediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mMusicName = ((ItemMusicList) mMusicList.get(position)).getMusicTitle();
        mMusicArtist = ((ItemMusicList) mMusicList.get(position)).getSinger();
        //TODO只有播放新音乐的时候，需要设置新名字并更新！

        sendBroadcast(new Intent(MusicService.ACTION_BOO)
                .putExtra(MainActivity.UPDATE_ICON, 1)
                .putExtra(MusicService.UPDATE_MUSIC_NAME,mNamelist.get(mPosition))
                .putExtra(MusicService.UPDATE_MUSIC_SINGER,mSingerlist.get(mPosition))
        );
    }
}
