package com.musicapp.mobile;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.musicapp.mobile.api.SongResponse;

import java.util.ArrayList;
import java.util.List;

public class RecentSongAdapter extends RecyclerView.Adapter<RecentSongAdapter.ViewHolder> {

    public interface Listener {
        void onSongClicked(SongResponse.Song song);
    }

    private final Context context;
    private final Listener listener;
    private final List<SongResponse.Song> songs = new ArrayList<>();

    public RecentSongAdapter(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setSongs(List<SongResponse.Song> newSongs) {
        songs.clear();
        if (newSongs != null) songs.addAll(newSongs);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent_song, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SongResponse.Song song = songs.get(position);

        String title = song.getTitle() != null ? song.getTitle() : "";
        String artist = song.getArtist() != null ? song.getArtist() : "";
        holder.tvTitle.setText(title);
        holder.tvArtist.setText(artist);

        String thumbnailUrl = song.getThumbnailUrl();
        if (thumbnailUrl != null && !thumbnailUrl.trim().isEmpty()) {
            Glide.with(context)
                    .load(thumbnailUrl)
                    .placeholder(R.drawable.ic_waveform)
                    .error(R.drawable.ic_waveform)
                    .into(holder.imgThumb);
        } else {
            holder.imgThumb.setImageResource(R.drawable.ic_waveform);
        }

        View.OnClickListener playClick = v -> {
            if (listener != null) listener.onSongClicked(song);
        };

        holder.btnPlay.setOnClickListener(playClick);
        holder.itemView.setOnClickListener(playClick);
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgThumb;
        final ImageButton btnPlay;
        final TextView tvTitle;
        final TextView tvArtist;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgThumb = itemView.findViewById(R.id.imgRecentThumb);
            btnPlay = itemView.findViewById(R.id.btnRecentPlay);
            tvTitle = itemView.findViewById(R.id.tvRecentTitle);
            tvArtist = itemView.findViewById(R.id.tvRecentArtist);
        }
    }
}

