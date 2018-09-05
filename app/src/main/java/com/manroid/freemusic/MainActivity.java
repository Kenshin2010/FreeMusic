package com.manroid.freemusic;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MainActivity extends AppCompatActivity implements OnClickSong {

    private RecyclerView songView;
    private ArrayList<Song> songList;
    private SongAdapter songAdapter;
    public MusicService musicService; // The application service
    private Intent serviceIntent;
    private ServiceConnection musicConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initData();
    }

    private void initView() {
        songView = (RecyclerView) findViewById(R.id.song_list);
        songList = new ArrayList<Song>();
        songAdapter = new SongAdapter(songList, this);
        songView.setLayoutManager(new LinearLayoutManager(this));
        songView.setAdapter(songAdapter);
    }

    private void initData() {
        getSongList();
        Collections.sort(songList, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });

        songAdapter.notifyDataSetChanged();
        serviceIntent = new Intent(this, MusicService.class);

    }


    public void getSongList() {
        //query external audio
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);
        //iterate over results if valid
        if (musicCursor != null && musicCursor.moveToFirst()) {
            //get columns
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);
            //add songs to list
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                songList.add(new Song(thisId, thisTitle, thisArtist));
            }
            while (musicCursor.moveToNext());
        }
    }

    @Override
    public void onPickSong(int pos) {
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (musicService != null) {
            unbindService(musicConnection);
            musicService.stopSelf();
        }
    }
}
