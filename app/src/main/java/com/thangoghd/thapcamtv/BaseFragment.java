package com.thangoghd.thapcamtv;

import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.thangoghd.thapcamtv.adapters.ReplayAdapter;
import com.thangoghd.thapcamtv.cache.ReplayCache;
import com.thangoghd.thapcamtv.response.ReplayResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public abstract class BaseFragment extends Fragment implements ReplayAdapter.OnHighlightClickListener {
    protected RecyclerView recyclerView;
    protected ReplayAdapter replayAdapter;
    protected int currentPage = 1;
    protected int maxPages;
    protected abstract String getLink();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_replay, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 3);
        recyclerView.setLayoutManager(layoutManager);

        recyclerView.setFocusable(true);
        recyclerView.setFocusableInTouchMode(true);
        recyclerView.requestFocus();

        // Add scroll listener to handle focus
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

        setupPaginationButtons(view);
        setupSearchButton(view);
        fetchHighlights(currentPage, view);
        return view;
    }

    private void setupSearchButton(View view) {
        Button searchButton = view.findViewById(R.id.searchButton);
        Button deleteButton = view.findViewById(R.id.deleteButton);
        EditText searchInput = view.findViewById(R.id.searchInput);

        searchButton.setOnClickListener(v -> {
            String query = searchInput.getText().toString().trim();
            if (!query.isEmpty()) {
                searchHighlights(query);
                hideKeyboard();
            } else {
                fetchHighlights(currentPage, view);
            }
        });

        deleteButton.setOnClickListener(v -> {
            searchInput.setText("");
            fetchHighlights(currentPage, view);
        });

        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String query = searchInput.getText().toString().trim();
                if (!query.isEmpty()) {
                    searchHighlights(query);
                    hideKeyboard();
                } else {
                    fetchHighlights(currentPage, view);
                }
                return true;
            }
            return false;
        });
    }

    protected void searchHighlights(String query) {
        ApiManager.getSportApi(true).searchReplays(getLink(), query).enqueue(new Callback<ReplayResponse>() {
            @Override
            public void onResponse(Call<ReplayResponse> call, Response<ReplayResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    replayAdapter = new ReplayAdapter(response.body().getData().getList(), BaseFragment.this);
                    recyclerView.setAdapter(replayAdapter);
                    maxPages = (int) Math.ceil((double) response.body().getData().getTotal() / response.body().getData().getLimit());
                    updatePaginationViews(getView());
                } else {
                    recyclerView.setAdapter(null);
                }
            }

            @Override
            public void onFailure(Call<ReplayResponse> call, Throwable t) {
                Toast.makeText(getContext(), "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                recyclerView.setAdapter(null);
            }
        });
    }

    protected void fetchHighlights(int page, View view) {
        // Check cache
        ReplayResponse cachedResponse = ReplayCache.getList(getLink(), page);
        if (cachedResponse != null) {
            replayAdapter = new ReplayAdapter(cachedResponse.getData().getList(), this);
            recyclerView.setAdapter(replayAdapter);
            maxPages = (int) Math.ceil((double) cachedResponse.getData().getTotal() / cachedResponse.getData().getLimit());
            updatePaginationViews(view);
            return;
        }
        // If not cached, fetch
        ApiManager.getSportApi(true).getReplays(getLink(), page).enqueue(new Callback<ReplayResponse>() {
            @Override
            public void onResponse(Call<ReplayResponse> call, Response<ReplayResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ReplayCache.putList(getLink(), page, response.body());
                    replayAdapter = new ReplayAdapter(response.body().getData().getList(), BaseFragment.this);
                    recyclerView.setAdapter(replayAdapter);
                    maxPages = (int) Math.ceil((double) response.body().getData().getTotal() / response.body().getData().getLimit());
                    updatePaginationViews(view);
                } else {
                    Toast.makeText(getContext(), "Lỗi không thể tải video", Toast.LENGTH_SHORT).show();
                    recyclerView.setAdapter(null);
                }
            }

            @Override
            public void onFailure(Call<ReplayResponse> call, Throwable t) {
                Toast.makeText(getContext(), "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                recyclerView.setAdapter(null);
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

    private void hideKeyboard() {
        try {
            if (getActivity() != null && getActivity().getCurrentFocus() != null) {
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
                }
            }
        } catch (Exception e) {
            // Handle any potential exceptions on older Android versions
        }
    }

    private void goToPage(int page) {
        if (page < 1 || page > maxPages) {
            Toast.makeText(getContext(), "Trang không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }
        currentPage = page;
        fetchHighlights(currentPage, getView());
    }

    private void updatePaginationViews(View view) {
        TextView currentPageTextView = view.findViewById(R.id.currentPage);
        TextView totalPagesTextView = view.findViewById(R.id.totalPages);
        currentPageTextView.setText(String.valueOf(currentPage));
        totalPagesTextView.setText("/ " + maxPages);
    }

    @Override
    public abstract void onHighlightClick(String id);
}