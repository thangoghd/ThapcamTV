package com.thangoghd.thapcamtv.channels;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.AsyncTask;
import android.util.Log;

import com.thangoghd.thapcamtv.api.ApiManager;
import com.thangoghd.thapcamtv.models.Replay;
import com.thangoghd.thapcamtv.response.ReplayResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HighlightChannelUpdateService extends JobService {
    private UpdateTask updateTask;

    @Override
    public boolean onStartJob(JobParameters params) {
        updateTask = new UpdateTask(this);
        updateTask.execute(params);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        if (updateTask != null) {
            updateTask.cancel(true);
        }
        return true;
    }

    private static class UpdateTask extends AsyncTask<JobParameters, Void, Void> {
        private final JobService jobService;

        UpdateTask(JobService jobService) {
            this.jobService = jobService;
        }

        @Override
        protected Void doInBackground(JobParameters... params) {
            final JobParameters jobParams = params[0];

            // Get channel ID
            long channelId = HighlightChannelHelper.createOrGetChannel(jobService);
            if (channelId == -1) {
                jobService.jobFinished(jobParams, false);
                return null;
            }

            // Fetch highlights
            ApiManager.getSportApi(true).getReplays("highlight", 1).enqueue(new Callback<ReplayResponse>() {
                @Override
                public void onResponse(Call<ReplayResponse> call, Response<ReplayResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        List<Replay> replays = response.body().getData().getList();
                        if (replays != null) {
                            for (Replay replay : replays) {
                                HighlightChannelHelper.addProgramToChannel(jobService, channelId, replay);
                            }
                            Log.d("HighlightChannel", "Updated channel with " + replays.size() + " programs");
                        }
                    }
                    jobService.jobFinished(jobParams, false);
                }

                @Override
                public void onFailure(Call<ReplayResponse> call, Throwable t) {
                    Log.e("HighlightChannel", "Failed to update channel", t);
                    jobService.jobFinished(jobParams, true);
                }
            });

            return null;
        }
    }
}
