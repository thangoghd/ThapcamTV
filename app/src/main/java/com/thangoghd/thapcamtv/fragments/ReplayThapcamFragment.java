package com.thangoghd.thapcamtv.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.thangoghd.thapcamtv.PlayerActivity;
import com.thangoghd.thapcamtv.R;
import com.thangoghd.thapcamtv.adapters.ReplayAdapter;
import com.thangoghd.thapcamtv.api.ApiManager;
import com.thangoghd.thapcamtv.models.Replay;
import com.thangoghd.thapcamtv.response.ReplayLinkResponse;
import com.thangoghd.thapcamtv.response.ReplayResponse;

import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReplayThapcamFragment extends Fragment implements ReplayAdapter.OnHighlightClickListener {
    private RecyclerView recyclerView;
    private ReplayAdapter replayAdapter;
    private int currentPage = 1;
    private int maxPages;
    private Spinner categorySpinner;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_review, container, false);
        
        TextView titleText = view.findViewById(R.id.titleText);
        titleText.setText("Xem lại (Môn khác)");
        
        setupSpinner(view);
        setupRecyclerView(view);
        setupPaginationButtons(view);
        setupSearchButton(view);
        fetchReplays(currentPage);
        
        return view;
    }

    private void setupSpinner(View view) {
        categorySpinner = view.findViewById(R.id.spinner);
        List<String> categories = Arrays.asList(
            "Tất cả",
            "Sẽ phát triển sau"
        );
        
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categories
        ) {
            @Override
            public boolean isEnabled(int position) {
                // Chỉ cho phép chọn "Tất cả"
                return position == 0;
            }
        };
        
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
    }

    private void setupRecyclerView(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 3);
        recyclerView.setLayoutManager(layoutManager);

        recyclerView.setFocusable(true);
        recyclerView.setFocusableInTouchMode(true);
        recyclerView.requestFocus();

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (recyclerView.getLayoutManager() != null && recyclerView.getLayoutManager().getFocusedChild() == null) {
                        View firstChild = recyclerView.getLayoutManager().getChildAt(0);
                        if (firstChild != null) {
                            firstChild.requestFocus();
                        }
                    }
                }
            }
        });
    }

    private void setupPaginationButtons(View view) {
        view.findViewById(R.id.firstPage).setOnClickListener(v -> goToPage(1));
        view.findViewById(R.id.previousPage).setOnClickListener(v -> goToPage(currentPage - 1));
        view.findViewById(R.id.nextPage).setOnClickListener(v -> goToPage(currentPage + 1));
        view.findViewById(R.id.lastPage).setOnClickListener(v -> goToPage(maxPages));

        view.findViewById(R.id.goToPage).setOnClickListener(v -> {
            EditText pageInput = view.findViewById(R.id.pageInput);
            String pageText = pageInput.getText().toString();
            if (!pageText.isEmpty()) {
                int page = Integer.parseInt(pageText);
                if (page > 0 && page <= maxPages) {
                    goToPage(page);
                } else {
                    Toast.makeText(getContext(), "Số trang không hợp lệ", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext(), "Vui lòng nhập số trang", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSearchButton(View view) {
        Button searchButton = view.findViewById(R.id.searchButton);
        Button deleteButton = view.findViewById(R.id.deleteButton);
        EditText searchInput = view.findViewById(R.id.searchInput);

        searchButton.setOnClickListener(v -> {
            String query = searchInput.getText().toString().trim();
            if (!query.isEmpty()) {
                searchReplays(query);
                hideKeyboard();
            } else {
                fetchReplays(currentPage);
            }
        });

        deleteButton.setOnClickListener(v -> {
            searchInput.setText("");
            fetchReplays(currentPage);
        });

        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String query = searchInput.getText().toString().trim();
                if (!query.isEmpty()) {
                    searchReplays(query);
                    hideKeyboard();
                } else {
                    fetchReplays(currentPage);
                }
                return true;
            }
            return false;
        });
    }

    private void fetchReplays(int page) {
        ApiManager.getSportApi(false).getFullMatchesThapcam(page).enqueue(new Callback<ReplayResponse>() {
            @Override
            public void onResponse(@NonNull Call<ReplayResponse> call, @NonNull Response<ReplayResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ReplayResponse replayResponse = response.body();
                    List<Replay> replays = replayResponse.getData().getList();
                    replayAdapter = new ReplayAdapter(replays, ReplayThapcamFragment.this);
                    recyclerView.setAdapter(replayAdapter);
                    maxPages = (int) Math.ceil((double) replayResponse.getData().getTotal() / replayResponse.getData().getLimit());
                    updatePaginationUI();
                } else {
                    Toast.makeText(getContext(), "Không thể tải dữ liệu", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ReplayResponse> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void searchReplays(String query) {
        ApiManager.getSportApi(false).searchReplaysFromThapcam(query).enqueue(new Callback<ReplayResponse>() {
            @Override
            public void onResponse(@NonNull Call<ReplayResponse> call, @NonNull Response<ReplayResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Replay> replays = response.body().getData().getList();
                    replayAdapter = new ReplayAdapter(replays, ReplayThapcamFragment.this);
                    recyclerView.setAdapter(replayAdapter);
                    maxPages = 1;
                    currentPage = 1;
                    updatePaginationUI();
                } else {
                    Toast.makeText(getContext(), "Không tìm thấy kết quả", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ReplayResponse> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), "Lỗi tìm kiếm: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void goToPage(int page) {
        if (page < 1 || page > maxPages) {
            Toast.makeText(getContext(), "Trang không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }
        currentPage = page;
        fetchReplays(currentPage);
    }

    private void updatePaginationUI() {
        View view = getView();
        if (view == null) return;  // Kiểm tra view null
        
        TextView currentPageTextView = view.findViewById(R.id.currentPage);
        TextView totalPagesTextView = view.findViewById(R.id.totalPages);
        if (currentPageTextView != null && totalPagesTextView != null) {
            currentPageTextView.setText(String.valueOf(currentPage));
            totalPagesTextView.setText("/ " + maxPages);
        }
    }

    private void hideKeyboard() {
        if (getActivity() != null && getActivity().getCurrentFocus() != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
        }
    }

    @Override
    public void onHighlightClick(String id) {
        ApiManager.getSportApi(false).getFullMatchesDetailsFromThapcam(id).enqueue(new Callback<ReplayLinkResponse>() {
            @Override
            public void onResponse(@NonNull Call<ReplayLinkResponse> call, @NonNull Response<ReplayLinkResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String videoUrl = response.body().getData().getVideoUrl();
                    Intent intent = new Intent(getActivity(), PlayerActivity.class);
                    intent.putExtra("replay_url", videoUrl);
                    intent.putExtra("show_quality_spinner", false);
                    intent.putExtra("source_type", "replay");
                    startActivity(intent);
                } else {
                    Toast.makeText(getContext(), "Không thể lấy được luồng video", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ReplayLinkResponse> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}