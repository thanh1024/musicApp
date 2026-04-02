package com.musicapp.mobile;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.musicapp.mobile.api.SongResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PlaylistExpandableAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_PLAYLIST = 1;
    private static final int TYPE_SONG = 2;

    public interface Listener {
        void onPlaylistClicked(long playlistId);
        void onSongClicked(SongResponse.Song song);
    }

    private final Context context;
    private final Listener listener;

    private final List<Row> rows = new ArrayList<>();
    private Long expandedPlaylistId = null;

    public PlaylistExpandableAdapter(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setPlaylists(JSONArray arr) {
        rows.clear();
        expandedPlaylistId = null;
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject p = arr.optJSONObject(i);
                if (p == null) continue;
                long id = p.optLong("id", -1L);
                String name = p.optString("name", "");
                String desc = p.optString("description", "");
                if ("null".equalsIgnoreCase(desc)) desc = "";
                if (id > 0) rows.add(Row.playlist(id, name, desc));
            }
        }
        notifyDataSetChanged();
    }

    public Long getExpandedPlaylistId() {
        return expandedPlaylistId;
    }

    public void collapseExpanded() {
        if (expandedPlaylistId == null) return;
        int headerIndex = findPlaylistHeaderIndex(expandedPlaylistId);
        if (headerIndex < 0) {
            expandedPlaylistId = null;
            notifyDataSetChanged();
            return;
        }
        int removed = removeSongRowsAfter(headerIndex);
        expandedPlaylistId = null;
        if (removed > 0) notifyItemRangeRemoved(headerIndex + 1, removed);
        notifyItemChanged(headerIndex);
    }

    public void togglePlaylist(long playlistId) {
        int headerIndex = findPlaylistHeaderIndex(playlistId);
        if (headerIndex < 0) return;

        // collapse if same
        if (expandedPlaylistId != null && expandedPlaylistId == playlistId) {
            int removed = removeSongRowsAfter(headerIndex);
            expandedPlaylistId = null;
            if (removed > 0) notifyItemRangeRemoved(headerIndex + 1, removed);
            notifyItemChanged(headerIndex);
            return;
        }

        // collapse old
        if (expandedPlaylistId != null) {
            int oldHeaderIndex = findPlaylistHeaderIndex(expandedPlaylistId);
            if (oldHeaderIndex >= 0) {
                int removed = removeSongRowsAfter(oldHeaderIndex);
                if (removed > 0) notifyItemRangeRemoved(oldHeaderIndex + 1, removed);
                notifyItemChanged(oldHeaderIndex);
            } else {
                notifyDataSetChanged();
            }
        }

        expandedPlaylistId = playlistId;
        notifyItemChanged(headerIndex);
        if (listener != null) listener.onPlaylistClicked(playlistId);
    }

    public void showSongsForExpanded(long playlistId, List<SongResponse.Song> songs) {
        if (expandedPlaylistId == null || expandedPlaylistId != playlistId) return;
        int headerIndex = findPlaylistHeaderIndex(playlistId);
        if (headerIndex < 0) return;

        // clean old songs
        int removed = removeSongRowsAfter(headerIndex);
        if (removed > 0) notifyItemRangeRemoved(headerIndex + 1, removed);

        if (songs == null || songs.isEmpty()) {
            return;
        }

        List<Row> songRows = new ArrayList<>();
        for (SongResponse.Song s : songs) {
            songRows.add(Row.song(playlistId, s));
        }
        rows.addAll(headerIndex + 1, songRows);
        notifyItemRangeInserted(headerIndex + 1, songRows.size());
    }

    private int findPlaylistHeaderIndex(long playlistId) {
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            if (r.type == TYPE_PLAYLIST && r.playlistId == playlistId) return i;
        }
        return -1;
    }

    private int removeSongRowsAfter(int headerIndex) {
        int removed = 0;
        int i = headerIndex + 1;
        while (i < rows.size() && rows.get(i).type == TYPE_SONG) {
            rows.remove(i);
            removed++;
        }
        return removed;
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_PLAYLIST) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_playlist, parent, false);
            return new PlaylistVH(v);
        }
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_playlist_song, parent, false);
        return new SongVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);
        if (holder instanceof PlaylistVH) {
            PlaylistVH vh = (PlaylistVH) holder;
            vh.name.setText(row.playlistName != null ? row.playlistName : "");
            String meta = (row.playlistDesc != null && !row.playlistDesc.trim().isEmpty()) ? row.playlistDesc.trim() : "Playlist";
            vh.meta.setText(meta);
            boolean expanded = expandedPlaylistId != null && expandedPlaylistId == row.playlistId;
            vh.expand.setRotation(expanded ? 90f : 0f);
            vh.itemView.setOnClickListener(v -> togglePlaylist(row.playlistId));
        } else if (holder instanceof SongVH) {
            SongVH vh = (SongVH) holder;
            SongResponse.Song s = row.song;
            vh.title.setText(s != null && s.getTitle() != null ? s.getTitle() : "");
            vh.artist.setText(s != null && s.getArtist() != null ? s.getArtist() : "");
            vh.itemView.setOnClickListener(v -> {
                if (listener != null && s != null) listener.onSongClicked(s);
            });
        }
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class PlaylistVH extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView meta;
        final ImageView expand;

        PlaylistVH(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tvPlaylistName);
            meta = itemView.findViewById(R.id.tvPlaylistMeta);
            expand = itemView.findViewById(R.id.imgExpand);
        }
    }

    static class SongVH extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView artist;

        SongVH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvSongTitle);
            artist = itemView.findViewById(R.id.tvSongArtist);
        }
    }

    static class Row {
        final int type;
        final long playlistId;
        final String playlistName;
        final String playlistDesc;
        final SongResponse.Song song;

        private Row(int type, long playlistId, String playlistName, String playlistDesc, SongResponse.Song song) {
            this.type = type;
            this.playlistId = playlistId;
            this.playlistName = playlistName;
            this.playlistDesc = playlistDesc;
            this.song = song;
        }

        static Row playlist(long playlistId, String name, String desc) {
            return new Row(TYPE_PLAYLIST, playlistId, name, desc, null);
        }

        static Row song(long playlistId, SongResponse.Song song) {
            return new Row(TYPE_SONG, playlistId, null, null, song);
        }
    }
}

