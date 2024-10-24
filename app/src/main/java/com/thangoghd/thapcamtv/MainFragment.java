package com.thangoghd.thapcamtv;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HorizontalGridView;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.RowHeaderPresenter;
import androidx.leanback.widget.RowPresenter;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.thangoghd.thapcamtv.iconheader.IconHeaderItem;
import com.thangoghd.thapcamtv.iconheader.IconHeaderItemPresenterSelector;

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
        setHeaderPresenterSelector(new IconHeaderItemPresenterSelector());

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
            for (Map.Entry<String, List<Match>> entry : matchesBySport.entrySet()) {
                String sportKey = entry.getKey();
                List<Match> matches = entry.getValue();

                SportType sportType = SportType.fromKey(sportKey);
                IconHeaderItem header = new IconHeaderItem(sportType.ordinal(), sportType.getVietnameseName(), sportType.getIconResourceId());

                ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new MatchPresenter());
                for (Match match : matches) {
                    listRowAdapter.add(match);
                }

                rowsAdapter.add(new ListRow(header, listRowAdapter));
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