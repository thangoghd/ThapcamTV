package com.thangoghd.thapcamtv.api;

import com.google.gson.JsonObject;
import com.thangoghd.thapcamtv.models.Provider;
import com.thangoghd.thapcamtv.response.ReplayLinkResponse;
import com.thangoghd.thapcamtv.response.MatchResponse;
import com.thangoghd.thapcamtv.response.ReplayResponse;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.Path;

public interface SportApi {
    @GET("api/match/featured")
    Call<MatchResponse> getLiveMatches();

    @GET("api/match/tc/{matchId}/{p}/meta/{token}")
    Call<JsonObject> getThapcamStreamUrl(
        @Path("matchId") String matchId,
        @Path("p") String p,
        @Path("token") String token
    );

    @GET("api/match/{matchId}/meta")
    Call<JsonObject> getVeboStreamUrl(@Path("matchId") String matchId);

    @GET("api/news/vebotv/list/{link}/{page}")
    Call<ReplayResponse> getReplays(@Path("link") String link, @Path("page") int page);

    @GET("api/news/vebotv/detail/{id}")
    Call<ReplayLinkResponse> getReplayDetails(@Path("id") String id);

    @GET("api/news/vebotv/search/{link}/{query}")
    Call<ReplayResponse> searchReplays(@Path("link") String link, @Path("query") String query);

    @GET("api/news/thapcam/list/xemlai/{page}")
    Call<ReplayResponse> getFullMatchesThapcam(@Path("page") int page);

    @GET("api/news/thapcam/detail/{id}")
    Call<ReplayLinkResponse> getFullMatchesDetailsFromThapcam(@Path("id") String id);

    @GET("api/news/thapcam/search/xemlai/{query}")
    Call<ReplayResponse> searchReplaysFromThapcam(@Path("query") String query);

    @GET("providers")
    Call<Provider> getProviders();
}
