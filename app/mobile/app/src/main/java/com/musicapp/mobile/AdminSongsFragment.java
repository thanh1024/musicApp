package com.musicapp.mobile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.musicapp.mobile.api.ApiService;
import com.musicapp.mobile.api.RetrofitClient;
import org.json.JSONArray;
import org.json.JSONObject;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminSongsFragment extends Fragment {
    private RecyclerView recyclerViewSongs;
    private ApiService apiService;
    private List<Map<String, Object>> songsList = new ArrayList<>();
    private List<Map<String, Object>> filteredSongs = new ArrayList<>();
    private SongsAdapter adapter;
    private View dialogView;
    private final List<String> artistOptions = new ArrayList<>();
    private final List<String> genreOptions = new ArrayList<>();
    private String genreFilter = ""; // empty = all

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_songs, container, false);

        recyclerViewSongs = view.findViewById(R.id.recyclerViewAdminSongs);
        recyclerViewSongs.setLayoutManager(new LinearLayoutManager(getContext()));

        apiService = RetrofitClient.getApiService(getContext());

        Button buttonRefresh = view.findViewById(R.id.buttonRefreshSongs);
        buttonRefresh.setOnClickListener(v -> loadSongs());

        Button buttonAddSong = view.findViewById(R.id.buttonAddSong);
        buttonAddSong.setOnClickListener(v -> showAddSongDialog());
        EditText search = view.findViewById(R.id.editSearchSongs);
        if (search != null) {
            search.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterSongs(s.toString()); }
                @Override public void afterTextChanged(android.text.Editable s) { }
            });
        }

        adapter = new SongsAdapter();
        recyclerViewSongs.setAdapter(adapter);

        bindGenreChips(view);
        loadSongs();

        return view;
    }

    private void bindGenreChips(View view) {
        TextView all = view.findViewById(R.id.chipSongsAll);
        TextView pop = view.findViewById(R.id.chipSongsPop);
        TextView piano = view.findViewById(R.id.chipSongsPiano);
        TextView hiphop = view.findViewById(R.id.chipSongsHipHop);
        TextView chill = view.findViewById(R.id.chipSongsChill);
        TextView rock = view.findViewById(R.id.chipSongsRock);

        if (all != null) all.setOnClickListener(v -> { genreFilter = ""; updateGenreChipUI(view); filterSongs(getSearchText(view)); });
        if (pop != null) pop.setOnClickListener(v -> { genreFilter = "Pop"; updateGenreChipUI(view); filterSongs(getSearchText(view)); });
        if (piano != null) piano.setOnClickListener(v -> { genreFilter = "Piano"; updateGenreChipUI(view); filterSongs(getSearchText(view)); });
        if (hiphop != null) hiphop.setOnClickListener(v -> { genreFilter = "Hip Hop"; updateGenreChipUI(view); filterSongs(getSearchText(view)); });
        if (chill != null) chill.setOnClickListener(v -> { genreFilter = "Chill"; updateGenreChipUI(view); filterSongs(getSearchText(view)); });
        if (rock != null) rock.setOnClickListener(v -> { genreFilter = "Rock"; updateGenreChipUI(view); filterSongs(getSearchText(view)); });

        updateGenreChipUI(view);
    }

    private String getSearchText(View root) {
        EditText ed = root != null ? root.findViewById(R.id.editSearchSongs) : null;
        return ed != null ? ed.getText().toString() : "";
    }

    private void updateGenreChipUI(View view) {
        TextView all = view.findViewById(R.id.chipSongsAll);
        TextView pop = view.findViewById(R.id.chipSongsPop);
        TextView piano = view.findViewById(R.id.chipSongsPiano);
        TextView hiphop = view.findViewById(R.id.chipSongsHipHop);
        TextView chill = view.findViewById(R.id.chipSongsChill);
        TextView rock = view.findViewById(R.id.chipSongsRock);

        setChip(all, genreFilter.isEmpty());
        setChip(pop, "Pop".equals(genreFilter));
        setChip(piano, "Piano".equals(genreFilter));
        setChip(hiphop, "Hip Hop".equals(genreFilter));
        setChip(chill, "Chill".equals(genreFilter));
        setChip(rock, "Rock".equals(genreFilter));
    }

    private void setChip(TextView tv, boolean selected) {
        if (tv == null) return;
        tv.setBackgroundResource(selected ? R.drawable.bg_chip_active : R.drawable.bg_chip_inactive);
        tv.setTextColor(getResources().getColor(selected ? R.color.text_primary : R.color.text_secondary));
    }

    private void loadSongs() {
        Call<ResponseBody> call = apiService.getAdminSongs();
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String json = response.body().string();
                        JSONObject jsonResponse = new JSONObject(json);
                        if (jsonResponse.getBoolean("success")) {
                            JSONArray songs = jsonResponse.getJSONArray("data");
                            songsList.clear();
                            for (int i = 0; i < songs.length(); i++) {
                                JSONObject song = songs.getJSONObject(i);
                                Map<String, Object> songMap = new HashMap<>();
                                songMap.put("id", song.getLong("id"));
                                songMap.put("title", song.getString("title"));
                                songMap.put("artist", song.getString("artist"));
                                songMap.put("album", song.optString("album", ""));
                                songMap.put("genre", song.getString("genre"));
                                songMap.put("mood", song.optString("mood", ""));
                                songMap.put("duration", song.getInt("duration"));
                                songMap.put("fileUrl", song.getString("fileUrl"));
                                songMap.put("thumbnailUrl", song.optString("thumbnailUrl", ""));
                                songsList.add(songMap);
                            }
                            filteredSongs.clear();
                            filteredSongs.addAll(songsList);
                            adapter.notifyDataSetChanged();
                        } else {
                            Toast.makeText(getContext(), "Lỗi: " + jsonResponse.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Lỗi xử lý dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String errorMsg = "Lỗi kết nối";
                    if (response.code() == 401) {
                        errorMsg = "Chưa đăng nhập hoặc token hết hạn";
                        SessionManager.clearSession(requireContext());
                        startActivity(new android.content.Intent(getActivity(), LoginActivity.class));
                        if (getActivity() != null) getActivity().finish();
                    } else if (response.code() == 403) {
                        errorMsg = "Không có quyền admin. Vui lòng đăng nhập với tài khoản admin.";
                    } else if (response.code() >= 500) {
                        errorMsg = "Lỗi server";
                    }
                    Toast.makeText(getContext(), errorMsg + " (Code: " + response.code() + ")", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                String errorMsg = "Lỗi kết nối: " + t.getMessage();
                if (t.getMessage() != null && t.getMessage().contains("Unable to resolve host")) {
                    errorMsg = "Không thể kết nối đến server. Vui lòng kiểm tra kết nối mạng.";
                } else if (t.getMessage() != null && t.getMessage().contains("timeout")) {
                    errorMsg = "Kết nối timeout. Vui lòng thử lại.";
                }
                Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showAddSongDialog() {
        loadArtistAndGenreOptions(() -> {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
            LayoutInflater inflater = LayoutInflater.from(getContext());
            dialogView = inflater.inflate(R.layout.dialog_add_song, null);
            builder.setView(dialogView);
            builder.setTitle("Thêm bài hát mới");

            Spinner spArtist = dialogView.findViewById(R.id.spinnerArtist);
            Spinner spGenre = dialogView.findViewById(R.id.spinnerGenre);
            if (spArtist != null) {
                android.widget.ArrayAdapter<String> ad = new android.widget.ArrayAdapter<>(
                        requireContext(), android.R.layout.simple_spinner_dropdown_item, artistOptions
                );
                spArtist.setAdapter(ad);
            }
            if (spGenre != null) {
                android.widget.ArrayAdapter<String> ad = new android.widget.ArrayAdapter<>(
                        requireContext(), android.R.layout.simple_spinner_dropdown_item, genreOptions
                );
                spGenre.setAdapter(ad);
            }

            builder.setPositiveButton("Thêm", (dialog, which) -> {
                EditText editTitle = dialogView.findViewById(R.id.editTextTitle);
                EditText editFileUrl = dialogView.findViewById(R.id.editTextFileUrl);
                EditText editThumb = dialogView.findViewById(R.id.editTextThumbnailUrl);
                EditText editAlbum = dialogView.findViewById(R.id.editTextAlbum);
                EditText editMood = dialogView.findViewById(R.id.editTextMood);

                String artist = spArtist != null && spArtist.getSelectedItem() != null ? spArtist.getSelectedItem().toString() : "";
                String genre = spGenre != null && spGenre.getSelectedItem() != null ? spGenre.getSelectedItem().toString() : "";

                Map<String, Object> songData = new HashMap<>();
                songData.put("title", editTitle != null ? editTitle.getText().toString().trim() : "");
                songData.put("artist", artist);
                songData.put("genre", genre);
                songData.put("fileUrl", editFileUrl != null ? editFileUrl.getText().toString().trim() : "");
                if (editThumb != null) songData.put("thumbnailUrl", editThumb.getText().toString().trim());
                if (editAlbum != null) songData.put("album", editAlbum.getText().toString().trim());
                if (editMood != null) songData.put("mood", editMood.getText().toString().trim());

                createSong(songData);
            });
            builder.setNegativeButton("Hủy", null);
            builder.show();
        });
    }

    private void loadArtistAndGenreOptions(Runnable onDone) {
        artistOptions.clear();
        genreOptions.clear();

        // default placeholders
        artistOptions.add("Chọn nghệ sĩ");
        genreOptions.add("Chọn thể loại");

        final int[] done = new int[]{0};
        Runnable tick = () -> {
            done[0]++;
            if (done[0] >= 2 && onDone != null) onDone.run();
        };

        apiService.getAdminArtists().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject obj = new JSONObject(response.body().string());
                        JSONArray arr = obj.optJSONArray("data");
                        if (arr != null) {
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject a = arr.optJSONObject(i);
                                if (a == null) continue;
                                String name = a.optString("name", "").trim();
                                if (!name.isEmpty()) artistOptions.add(name);
                            }
                        }
                    } catch (Exception ignored) {}
                }
                tick.run();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                tick.run();
            }
        });

        apiService.getAdminGenres().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject obj = new JSONObject(response.body().string());
                        JSONArray arr = obj.optJSONArray("data");
                        if (arr != null) {
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject a = arr.optJSONObject(i);
                                if (a == null) continue;
                                String name = a.optString("name", "").trim();
                                if (!name.isEmpty()) genreOptions.add(name);
                            }
                        }
                    } catch (Exception ignored) {}
                }
                tick.run();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                tick.run();
            }
        });
    }

    private void createSong(Map<String, Object> songData) {
        Call<ResponseBody> call = apiService.createSong(songData);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject jsonResponse = new JSONObject(response.body().string());
                        if (jsonResponse.getBoolean("success")) {
                            Toast.makeText(getContext(), "Đã thêm bài hát", Toast.LENGTH_SHORT).show();
                            loadSongs();
                        } else {
                            Toast.makeText(getContext(), "Lỗi: " + jsonResponse.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(getContext(), "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class SongsAdapter extends RecyclerView.Adapter<SongsAdapter.ViewHolder> {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_admin_song, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Map<String, Object> song = filteredSongs.get(position);
            holder.textViewTitle.setText((String) song.get("title"));
            holder.textViewArtist.setText((String) song.get("artist"));
            holder.textViewGenre.setText((String) song.get("genre"));

            Long songId = (Long) song.get("id");
            holder.buttonView.setOnClickListener(v -> showSongDetail(song));
            holder.buttonEdit.setOnClickListener(v -> showEditSongDialog(songId, song));
            holder.buttonDelete.setOnClickListener(v -> deleteSong(songId, position));
        }

        @Override
        public int getItemCount() {
            return filteredSongs.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textViewTitle, textViewArtist, textViewGenre;
            Button buttonDelete, buttonView, buttonEdit;

            ViewHolder(View itemView) {
                super(itemView);
                textViewTitle = itemView.findViewById(R.id.textViewSongTitle);
                textViewArtist = itemView.findViewById(R.id.textViewSongArtist);
                textViewGenre = itemView.findViewById(R.id.textViewSongGenre);
                buttonDelete = itemView.findViewById(R.id.buttonDeleteSong);
                buttonView = itemView.findViewById(R.id.buttonViewSongDetail);
                buttonEdit = itemView.findViewById(R.id.buttonEditSong);
            }
        }
    }

    private void deleteSong(Long songId, int position) {
        Call<ResponseBody> call = apiService.deleteSong(songId);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject jsonResponse = new JSONObject(response.body().string());
                        if (jsonResponse.getBoolean("success")) {
                            songsList.remove(position);
                            filteredSongs.remove(position);
                            adapter.notifyDataSetChanged();
                            Toast.makeText(getContext(), "Đã xóa bài hát", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(getContext(), "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterSongs(String keyword) {
        String q = keyword != null ? keyword.trim().toLowerCase() : "";
        filteredSongs.clear();
        for (Map<String, Object> s : songsList) {
            String title = String.valueOf(s.get("title")).toLowerCase();
            String artist = String.valueOf(s.get("artist")).toLowerCase();
            String genre = String.valueOf(s.get("genre")).toLowerCase();

            boolean matchText = q.isEmpty() || title.contains(q) || artist.contains(q) || genre.contains(q);
            boolean matchGenre = genreFilter.isEmpty() || genre.contains(genreFilter.toLowerCase());
            if (matchText && matchGenre) filteredSongs.add(s);
        }
        adapter.notifyDataSetChanged();
    }

    private void showSongDetail(Map<String, Object> song) {
        String msg = "ID: " + song.get("id") + "\n"
                + "Title: " + song.get("title") + "\n"
                + "Artist: " + song.get("artist") + "\n"
                + "Genre: " + song.get("genre") + "\n"
                + "Album: " + song.get("album") + "\n"
                + "Duration: " + song.get("duration") + "s\n"
                + "File URL: " + song.get("fileUrl");
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Chi tiết bài hát")
                .setMessage(msg)
                .setPositiveButton("Đóng", null)
                .show();
    }

    private void showEditSongDialog(Long songId, Map<String, Object> song) {
        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_song, null);
        EditText editTitle = v.findViewById(R.id.editTextTitle);
        Spinner spArtist = v.findViewById(R.id.spinnerArtist);
        Spinner spGenre = v.findViewById(R.id.spinnerGenre);
        EditText editFileUrl = v.findViewById(R.id.editTextFileUrl);
        EditText editThumb = v.findViewById(R.id.editTextThumbnailUrl);
        EditText editAlbum = v.findViewById(R.id.editTextAlbum);
        EditText editMood = v.findViewById(R.id.editTextMood);
        editTitle.setText(String.valueOf(song.get("title")));
        editFileUrl.setText(String.valueOf(song.get("fileUrl")));
        if (editThumb != null) editThumb.setText(String.valueOf(song.get("thumbnailUrl")));
        editAlbum.setText(String.valueOf(song.get("album")));
        editMood.setText(String.valueOf(song.get("mood")));

        // Load dropdown options then preselect current values
        loadArtistAndGenreOptions(() -> {
            if (spArtist != null) {
                android.widget.ArrayAdapter<String> ad = new android.widget.ArrayAdapter<>(
                        requireContext(), android.R.layout.simple_spinner_dropdown_item, artistOptions
                );
                spArtist.setAdapter(ad);
                String current = String.valueOf(song.get("artist"));
                int idx = artistOptions.indexOf(current);
                if (idx >= 0) spArtist.setSelection(idx);
            }
            if (spGenre != null) {
                android.widget.ArrayAdapter<String> ad = new android.widget.ArrayAdapter<>(
                        requireContext(), android.R.layout.simple_spinner_dropdown_item, genreOptions
                );
                spGenre.setAdapter(ad);
                String current = String.valueOf(song.get("genre"));
                int idx = genreOptions.indexOf(current);
                if (idx >= 0) spGenre.setSelection(idx);
            }
        });

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Cập nhật bài hát")
                .setView(v)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", (d, w) -> {
                    Map<String, Object> body = new HashMap<>();
                    body.put("title", editTitle.getText().toString().trim());
                    String artist = spArtist != null && spArtist.getSelectedItem() != null ? spArtist.getSelectedItem().toString() : "";
                    String genre = spGenre != null && spGenre.getSelectedItem() != null ? spGenre.getSelectedItem().toString() : "";
                    body.put("artist", artist);
                    body.put("genre", genre);
                    body.put("fileUrl", editFileUrl.getText().toString().trim());
                    if (editThumb != null) body.put("thumbnailUrl", editThumb.getText().toString().trim());
                    body.put("album", editAlbum.getText().toString().trim());
                    body.put("mood", editMood.getText().toString().trim());
                    apiService.updateSong(songId, body).enqueue(new Callback<ResponseBody>() {
                        @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(getContext(), "Đã cập nhật bài hát", Toast.LENGTH_SHORT).show();
                                loadSongs();
                            } else {
                                Toast.makeText(getContext(), "Cập nhật thất bại (code " + response.code() + ")", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Toast.makeText(getContext(), "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }).show();
    }
}
