package com.thangoghd.thapcamtv.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.thangoghd.thapcamtv.BaseFragment;
import com.thangoghd.thapcamtv.PlayerActivity;
import com.thangoghd.thapcamtv.R;
import com.thangoghd.thapcamtv.api.ApiManager;
import com.thangoghd.thapcamtv.response.ReplayLinkResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FullMatchFragment extends BaseFragment {
    @Override
    protected String getLink() {
        return "xemlai";
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        TextView titleText = view.findViewById(R.id.titleText);
        titleText.setText("Xem lại");
        return view;
    }

    @Override
    public void onHighlightClick(String id) {
        fetchHighlightDetails(id);
    }

    private void fetchHighlightDetails(String id) {
        ApiManager.getSportApi(true).getReplayDetails(id).enqueue(new Callback<ReplayLinkResponse>() {
            @Override
            public void onResponse(Call<ReplayLinkResponse> call, Response<ReplayLinkResponse> response) {
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
            public void onFailure(Call<ReplayLinkResponse> call, Throwable t) {
                Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}