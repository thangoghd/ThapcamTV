package com.thangoghd.thapcamtv;

import com.google.gson.JsonObject;
import com.thangoghd.thapcamtv.models.Match;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

import java.util.List;

public interface SportApi {
    @GET("api/match/tc/live")
    Call<ApiResponse> getLiveMatches();

    @GET("api/match/tc/{matchId}/no/meta")
    Call<JsonObject> getMatchStreamUrl(@Path("matchId") String matchId);
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