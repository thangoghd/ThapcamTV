package com.thangoghd.thapcamtv;

import com.google.gson.JsonObject;
import com.thangoghd.thapcamtv.response.ReplayLinkResponse;
import com.thangoghd.thapcamtv.response.MatchResponse;
import com.thangoghd.thapcamtv.response.ReplayResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface SportApi {
    @GET("api/match/tc/live")
    Call<MatchResponse> getLiveMatches();

    @GET("api/match/tc/{matchId}/no/meta")
    Call<JsonObject> getMatchStreamUrl(@Path("matchId") String matchId);

    @GET("api/news/vebotv/list/{link}/{page}")
    Call<ReplayResponse> getHighlights(@Path("link") String link, @Path("page") int page);

    @GET("api/news/vebotv/detail/{id}")
    Call<ReplayLinkResponse> getHighlightDetails(@Path("id") String id);

    @GET("api/news/vebotv/search/{link}/{query}")
    Call<ReplayResponse> searchHighlights(@Path("link") String link, @Path("query") String query);
}



