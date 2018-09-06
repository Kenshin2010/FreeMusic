package com.manroid.freemusic;

import java.util.ArrayList;
import java.util.List;

public class Song {

    private long id;
    private String title;
    private String artist;
    private String data;

    public Song(long songID, String songTitle, String songArtist){
        id=songID;
        title=songTitle;
        artist=songArtist;
    }

    public long getID(){return id;}
    public String getTitle(){return title;}
    public String getArtist(){return artist;}
    public Song getNext(boolean repeatAll) {
        List<Song> songs = MainActivity.getListSong();
        int index = songs.indexOf(this);
        if(index<songs.size()-1) {
            return songs.get(index+1);
        } else {
            if(repeatAll) return songs.get(0);
        }
        return null;
    }
}
