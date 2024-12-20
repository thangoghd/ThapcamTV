package com.thangoghd.thapcamtv.api;

import com.google.gson.JsonObject;
import com.thangoghd.thapcamtv.response.ReplayLinkResponse;
import com.thangoghd.thapcamtv.response.MatchResponse;
import com.thangoghd.thapcamtv.response.ReplayResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface SportApi {
    @GET("api/match/featured")
    Call<MatchResponse> getLiveMatches();

    @GET("api/match/tc/{matchId}/no/meta")
    Call<JsonObject> getThapcamStreamUrl(@Path("matchId") String matchId);

    @GET("api/match/{matchId}/meta")
    Call<JsonObject> getVeboStreamUrl(@Path("matchId") String matchId);

    @GET("api/news/vebotv/list/{link}/{page}")
    Call<ReplayResponse> getReplays(@Path("link") String link, @Path("page") int page);

    @GET("api/news/vebotv/detail/{id}")
    Call<ReplayLinkResponse> getReplayDetails(@Path("id") String id);

    @GET("api/news/vebotv/search/{link}/{query}")
    Call<ReplayResponse> searchReplays(@Path("link") String link, @Path("query") String query);
}



