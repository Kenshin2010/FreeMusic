package com.manroid.freemusic;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

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
    public MusicService musicService; // The application service
    private Intent serviceIntent;
    private ServiceConnection musicConnection;
    private BroadcastReceiver broadcastReceiver;
    private Button btnPlayPause, btnPrev, btnNext;
    private boolean showRemainingTime = false;
    private boolean pollingThreadRunning; // true if thread is active, false otherwise
    private static final int POLLING_INTERVAL = 450; // Refresh time of the seekbar


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
    }

    private void initView() {
        seekBar = (SeekBar) findViewById(R.id.timeline_seek_bar);
        tvTimelineAll = (TextView) findViewById(R.id.timeline_all);
        tvTimelineNow = (TextView) findViewById(R.id.timeline_now);
        songView = (RecyclerView) findViewById(R.id.song_list);
        btnPlayPause = (Button) findViewById(R.id.play_pause);
        btnNext = (Button) findViewById(R.id.play_next);
        btnPrev = (Button) findViewById(R.id.play_previous);

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


    @Override
    public void onPickSong(int pos) {
        btnPlayPause.setText("Pause");
        musicService.playSong(songList.get(pos));
    }

    @Override
    public void onStart() {
        super.onStart();
        // The service is bound to this activity
        if (musicService == null) {
            startService(serviceIntent); // Starts the service if it is not running
            createMusicConnection();
            bindService(serviceIntent, musicConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_PLAY_PAUSE_CHANGE);
        intentFilter.addAction(Constants.ACTION_NEW_SONG);
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Constants.ACTION_NEW_SONG)) {
                    Log.d("FREE_MUSIC", "===============");
                    startPollingThread();
                } else if (intent.getAction().equals(Constants.ACTION_PLAY_PAUSE_CHANGE)) {
                    // dont some thing
                }

            }
        };
        registerReceiver(broadcastReceiver, intentFilter);
        updatePlayPauseButton();
    }

    private void updatePlayPauseButton() {

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

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.play_pause && musicService != null) {
            showRemainingTime = !showRemainingTime;
            musicService.playPause();
            if (musicService.isPlayingSong()) {
                updatePosition();
                btnPlayPause.setText("pause");
                startPollingThread();
            } else {
                btnPlayPause.setText("play");
                stopPollingThread();
                updatePosition();
            }
        }else if (v.getId() == R.id.play_next && musicService != null){
            musicService.nextSongType();
        }else {
            musicService.previousItem(false);
        }

    }
//
//    private void updatePlayingSong() {
//        Song song = musicService.getCurrentsong();
//        if (song != null) {
//
//        }
//    }


    private void updatePosition() {
        int progress = musicService.getCurrentPosition();
        int duration = musicService.getDuration();
        seekBar.setMax(musicService.getDuration());
        seekBar.setProgress(musicService.getCurrentPosition());
        tvTimelineAll.setText(Utils.formatTime(duration));
        tvTimelineNow.setText(Utils.formatTime(progress));
    }

    private void stopPollingThread() {
        pollingThreadRunning = false;
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

    @Override
    public void onPause() {
        super.onPause();
//        stopPollingThread(); // Stop the polling thread
//        unregisterReceiver(broadcastReceiver); // Disable broadcast receiver
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) { // Event is triggered only if the seekbar position was modified by the user
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
}
