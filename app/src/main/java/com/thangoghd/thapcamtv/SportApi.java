package com.thangoghd.thapcamtv;

import retrofit2.Call;
import retrofit2.http.GET;
import java.util.List;

public interface SportApi {
    @GET("api/match/tc/live")
    Call<ApiResponse> getLiveMatches();
}

class ApiResponse {
    private int status;
    private List<Match> data;

    // Getters and setters
    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public List<Match> getData() {
        return data;
    }

    public void setData(List<Match> data) {
        this.data = data;
    }
}