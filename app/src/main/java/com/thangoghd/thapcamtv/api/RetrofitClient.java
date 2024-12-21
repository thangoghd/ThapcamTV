package com.thangoghd.thapcamtv.api;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String BASE_URL = "https://a.thapcamn.xyz/";
    private static final String SECOND_BASE_URL = "https://api.vebo.xyz/";
    private static final String GITHUB_API_BASE_URL = "https://api.github.com/";

    private static Retrofit githubRetrofit = null;
    private static GitHubApiService githubApiService = null;

    public static Retrofit getClient(boolean useSecondBaseUrl) {
        String url = useSecondBaseUrl ? SECOND_BASE_URL : BASE_URL;
        return new Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public static GitHubApiService getGitHubApiService() {
        if (githubApiService == null) {
            if (githubRetrofit == null) {
                OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
                githubRetrofit = new Retrofit.Builder()
                    .baseUrl(GITHUB_API_BASE_URL)
                    .client(httpClient.build())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            }
            githubApiService = githubRetrofit.create(GitHubApiService.class);
        }
        return githubApiService;
    }
}