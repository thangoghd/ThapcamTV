package com.thangoghd.thapcamtv;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.HorizontalGridView;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.RowHeaderPresenter;
import androidx.leanback.widget.RowPresenter;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.util.List;
import java.util.Map;

public class MainFragment extends BrowseSupportFragment {
    private MainViewModel viewModel;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                SportApi api = ApiManager.getSportApi();
                SportRepository repository = new SportRepository(api);
                return (T) new MainViewModel(repository);
            }
        }).get(MainViewModel.class);

        setupUIElements();
        loadRows();

        viewModel.fetchMatches();
    }

    private void setupUIElements() {
        setTitle("Live Sports");
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);
    }

    private void loadRows() {
        ArrayObjectAdapter rowsAdapter = new ArrayObjectAdapter(new CustomListRowPresenter());
    
        viewModel.getMatches().observe(getViewLifecycleOwner(), matchesBySport -> {
            rowsAdapter.clear();
            int index = 0;
            for (Map.Entry<String, List<Match>> entry : matchesBySport.entrySet()) {
                String sportType = entry.getKey();
                List<Match> matches = entry.getValue();
    
                ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new MatchPresenter());
                for (Match match : matches) {
                    listRowAdapter.add(match);
                }
    
                HeaderItem header = new HeaderItem(sportType);
                rowsAdapter.add(new ListRow(header, listRowAdapter));
                index++;
            }
        });
    
        setAdapter(rowsAdapter);
    }

    private class CustomListRowPresenter extends ListRowPresenter {
        public CustomListRowPresenter() {
            super();
            setHeaderPresenter(new RowHeaderPresenter());
            setSelectEffectEnabled(false);
        }
    
        @Override
        protected void initializeRowViewHolder(RowPresenter.ViewHolder holder) {
            super.initializeRowViewHolder(holder);
            // Customize the ListRowPresenter here
            ListRowPresenter.ViewHolder listRowViewHolder = (ListRowPresenter.ViewHolder) holder;
            HorizontalGridView gridView = listRowViewHolder.getGridView();
            gridView.setItemSpacing(20); // Set the spacing between items
        }
    }
}