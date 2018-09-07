package com.manroid.freemusic.view.activity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.manroid.freemusic.constant.Constants;
import com.manroid.freemusic.util.File;
import com.manroid.freemusic.service.MusicService;
import com.manroid.freemusic.interfaces.OnClickSong;
import com.manroid.freemusic.R;
import com.manroid.freemusic.model.Song;
import com.manroid.freemusic.adapter.SongAdapter;
import com.manroid.freemusic.util.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity implements
        View.OnClickListener,
        SeekBar.OnSeekBarChangeListener,
        OnClickSong {

    private RecyclerView songView;
    private TextView tvTimelineNow, tvTimelineAll;
    private SeekBar seekBar;
    private static ArrayList<Song> songList;
    private SongAdapter songAdapter;
    public MusicService musicService;
    private Intent serviceIntent;
    private ServiceConnection musicConnection;
    private BroadcastReceiver broadcastReceiver;
    private ImageView btnPlayPause, btnPrev, btnNext, btnRepeat, btnShuffle;
    private boolean showRemainingTime = false;
    private boolean pollingThreadRunning;
    private static final int POLLING_INTERVAL = 450;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initData();
        initEvent();
    }

    private void initEvent() {
        seekBar.setOnSeekBarChangeListener(this);
        btnPlayPause.setOnClickListener(this);
        btnNext.setOnClickListener(this);
        btnPrev.setOnClickListener(this);
        btnRepeat.setOnClickListener(this);
        btnShuffle.setOnClickListener(this);
    }

    private void initView() {

        seekBar = (SeekBar) findViewById(R.id.timeline_seek_bar);
        tvTimelineAll = (TextView) findViewById(R.id.timeline_all);
        tvTimelineNow = (TextView) findViewById(R.id.timeline_now);
        songView = (RecyclerView) findViewById(R.id.song_list);
        btnPlayPause = (ImageView) findViewById(R.id.play_pause);
        btnNext = (ImageView) findViewById(R.id.play_next);
        btnPrev = (ImageView) findViewById(R.id.play_previous);
        btnRepeat = (ImageView) findViewById(R.id.repeat);
        btnShuffle = (ImageView) findViewById(R.id.shuffle);

        songList = new ArrayList<Song>();
        songAdapter = new SongAdapter(songList, this);
        songView.setLayoutManager(new LinearLayoutManager(this));
        songView.setAdapter(songAdapter);
    }

    private void initData() {
        songList.addAll(File.getSongList(this));
        Collections.sort(songList, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });

        songAdapter.notifyDataSetChanged();
        serviceIntent = new Intent(this, MusicService.class);

    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onPickSong(int pos) {
        btnPlayPause.setBackgroundResource(R.drawable.custom_action_pause);
        musicService.playSong(songList.get(pos));
    }

    @Override
    public void onStart() {
        super.onStart();
        if (musicService == null) {
            startService(serviceIntent);
            createMusicConnection();
            bindService(serviceIntent, musicConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_PLAY_PAUSE_CHANGE);
        intentFilter.addAction(Constants.ACTION_PLAY_PAUSE);
        intentFilter.addAction(Constants.ACTION_NEW_SONG);
        intentFilter.addAction(Constants.ACTION_QUIT_ACTIVITY);
        broadcastReceiver = new BroadcastReceiver() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Constants.ACTION_NEW_SONG)) {
                    Log.d("FREE_MUSIC", "===============");
                    startPollingThread();
                } else if (intent.getAction().equals(Constants.ACTION_PLAY_PAUSE_CHANGE)) {
                    //dont something
                }else if (intent.getAction().equals(Constants.ACTION_PLAY_PAUSE)) {
                    updateLayoutPlayPause();
                }else if (intent.getAction().equals(Constants.ACTION_QUIT_ACTIVITY)) {
                    finish();
                }


            }
        };

        registerReceiver(broadcastReceiver, intentFilter);
    }

    private void createMusicConnection() {
        musicConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {
                musicService = ((MusicService.LocalBinder) service).getService();
            }

            @Override
            public void onServiceDisconnected(ComponentName className) {
                musicService = null;
            }
        };
    }

    public static List<Song> getListSong() {
        return songList;
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void updateLayoutPlayPause(){
        if (musicService.isPlayingSong()) {
            btnPlayPause.setBackgroundResource(R.drawable.custom_action_pause);
            startPollingThread();
        } else {
            btnPlayPause.setBackgroundResource(R.drawable.custom_action_play);
            stopPollingThread();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.play_pause && musicService != null) {
            musicService.playPause();
            if (musicService.isPlayingSong()) {
                updatePosition();
                btnPlayPause.setBackgroundResource(R.drawable.custom_action_pause);
                startPollingThread();
            } else {
                btnPlayPause.setBackgroundResource(R.drawable.custom_action_play);
                stopPollingThread();
                updatePosition();
            }
        }else if (v.getId() == R.id.play_next && musicService != null){
            musicService.nextSongType();
        }else if (v.getId() == R.id.play_previous && musicService != null){
            musicService.previousItem(false);
        }else if (v.getId() == R.id.shuffle && musicService != null){

            boolean isShuffleSong = musicService.isShuffle();

            if (!isShuffleSong){
                musicService.setShuffle(!isShuffleSong);
                btnShuffle.setBackgroundResource(R.drawable.custom_action_shuffle_2);
            }else {
                musicService.setShuffle(!isShuffleSong);
                btnShuffle.setBackgroundResource(R.drawable.custom_action_shuffle);
            }

        }else if (v.getId() == R.id.repeat && musicService != null){

            boolean isRepeatSong = musicService.isRepeat();

            if (!isRepeatSong ){
                musicService.setRepeat(!isRepeatSong );
                btnRepeat.setBackgroundResource(R.drawable.custom_action_repeat_2);
            }else {
                musicService.setRepeat(!isRepeatSong );
                btnRepeat.setBackgroundResource(R.drawable.custom_action_repeat);
            }

        }

    }

    private void updatePosition() {
        int progress = musicService.getCurrentPosition();
        int duration = musicService.getDuration();
        seekBar.setMax(musicService.getDuration());
        seekBar.setProgress(musicService.getCurrentPosition());
        tvTimelineAll.setText(Utils.formatTime(duration));
        tvTimelineNow.setText(Utils.formatTime(progress));
    }


    private void startPollingThread() {
        pollingThreadRunning = true;
        new Thread() {
            public void run() {
                while (pollingThreadRunning) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            if (musicService != null) {
                                updatePosition();
                            }
                        }
                    });
                    try {
                        Thread.sleep(POLLING_INTERVAL);
                    } catch (Exception e) {
                    }
                }
            }
        }.start();
    }

    private void stopPollingThread() {
        pollingThreadRunning = false;
    }


    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            if (seekBar.equals(seekBar)) {
                musicService.seekTo(progress);
            }
            updatePosition();
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (musicService != null) {
            unbindService(musicConnection);
            musicService.stopSelf();
            unregisterReceiver(broadcastReceiver);
            stopPollingThread();
        }
    }
}
