package com.thangoghd.thapcamtv;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;
import java.util.Map;

public class MainViewModel extends ViewModel {
    private SportRepository repository;
    private MutableLiveData<Map<String, List<Match>>> matches = new MutableLiveData<>();

    public MainViewModel(SportRepository repository) {
        this.repository = repository;
    }

    public LiveData<Map<String, List<Match>>> getMatches() {
        return matches;
    }

    public void fetchMatches() {
        repository.getLiveMatches(new RepositoryCallback<List<Match>>() {
            @Override
            public void onSuccess(List<Match> result) {
                matches.postValue(repository.getMatchesBySportType(result));
            }

            @Override
            public void onError(Exception e) {
                // Handle error
                Log.e("MainViewModel", "Error fetching matches", e);
            }
        });
    }
}