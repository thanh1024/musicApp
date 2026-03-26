package com.musicapp.mobile;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.musicapp.mobile.api.ApiService;
import com.musicapp.mobile.api.RetrofitClient;
import com.musicapp.mobile.api.SongResponse;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchFragment extends Fragment {
    private ApiService apiService;
    private final List<SongResponse.Song> searchResults = new ArrayList<>();
    private SearchSongsAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        EditText editTextSearch = view.findViewById(R.id.editTextSearch);
        ImageButton buttonSearch = view.findViewById(R.id.btnVoiceSearch);
        RecyclerView recyclerView = view.findViewById(R.id.recyclerSearchResults);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SearchSongsAdapter();
        recyclerView.setAdapter(adapter);

        apiService = RetrofitClient.getApiService(getContext());

        buttonSearch.setOnClickListener(v -> {
            String keyword = editTextSearch.getText().toString().trim();
            if (!keyword.isEmpty()) {
                searchSongs(keyword);
            }
        });

        editTextSearch.setOnEditorActionListener((v, actionId, event) -> {
            boolean isEnter = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER;
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH || isEnter) {
                String keyword = editTextSearch.getText().toString().trim();
                if (!keyword.isEmpty()) {
                    searchSongs(keyword);
                }
                return true;
            }
            return false;
        });

        return view;
    }

    private void searchSongs(String keyword) {
        Call<SongResponse> call = apiService.searchSongs(keyword);
        call.enqueue(new Callback<SongResponse>() {
            @Override
            public void onResponse(Call<SongResponse> call, Response<SongResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    SongResponse songResponse = response.body();
                    if (songResponse.isSuccess() && songResponse.getData() != null) {
                        searchResults.clear();
                        searchResults.addAll(songResponse.getData());
                        adapter.notifyDataSetChanged();
                        int count = searchResults.size();
                        Toast.makeText(getContext(), "Tìm thấy " + count + " bài hát", Toast.LENGTH_SHORT).show();
                    } else {
                        String message = songResponse.getMessage();
                        Toast.makeText(getContext(), message != null ? message : "Không tìm thấy kết quả", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Lỗi tìm kiếm", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SongResponse> call, Throwable t) {
                Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class SearchSongsAdapter extends RecyclerView.Adapter<SearchSongsAdapter.ViewHolder> {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_search_song, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            SongResponse.Song song = searchResults.get(position);
            holder.titleView.setText(song.getTitle() != null ? song.getTitle() : "Chưa có tiêu đề");
            holder.artistView.setText(song.getArtist() != null ? song.getArtist() : "Chưa có nghệ sĩ");
            holder.metaView.setText(buildMeta(song));
            holder.itemView.setOnClickListener(v ->
                    Toast.makeText(getContext(), "Chọn: " + holder.titleView.getText(), Toast.LENGTH_SHORT).show());
        }

        @Override
        public int getItemCount() {
            return searchResults.size();
        }

        private String buildMeta(SongResponse.Song song) {
            String genre = song.getGenre() != null ? song.getGenre() : "Không rõ thể loại";
            String mood = song.getMood() != null ? song.getMood() : "Không rõ tâm trạng";
            return genre + " • " + mood;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView titleView;
            TextView artistView;
            TextView metaView;

            ViewHolder(View itemView) {
                super(itemView);
                titleView = itemView.findViewById(R.id.textSearchSongTitle);
                artistView = itemView.findViewById(R.id.textSearchSongArtist);
                metaView = itemView.findViewById(R.id.textSearchSongMeta);
            }
        }
    }
}
