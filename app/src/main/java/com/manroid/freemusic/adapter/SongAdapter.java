package com.manroid.freemusic.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.manroid.freemusic.R;
import com.manroid.freemusic.model.Song;
import com.manroid.freemusic.interfaces.OnClickSong;

import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.RecyclerViewHolder> {

    private List<Song> data;
    private OnClickSong onClickSong;

    public SongAdapter(List<Song> data, OnClickSong onClickSong) {
        this.data = data;
        this.onClickSong = onClickSong;
    }

    @Override
    public RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.item_song, parent, false);
        return new RecyclerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerViewHolder holder, int position) {
        Song currSong = data.get(position);
        holder.songView.setText(currSong.getTitle());
        holder.artistView.setText(currSong.getArtist());
    }

    @Override
    public int getItemCount() {
        return data.size();
    }


    class RecyclerViewHolder extends RecyclerView.ViewHolder {

        TextView songView;
        TextView artistView;

        RecyclerViewHolder(View itemView) {
            super(itemView);
            songView = (TextView) itemView.findViewById(R.id.song_title);
            artistView = (TextView) itemView.findViewById(R.id.song_artist);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onClickSong.onPickSong(getLayoutPosition());
                }
            });

        }
    }
}