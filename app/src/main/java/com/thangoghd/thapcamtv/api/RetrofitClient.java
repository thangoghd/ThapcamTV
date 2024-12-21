package com.thangoghd.thapcamtv.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String BASE_URL = "https://a.thapcamn.xyz/";
    private static final String SECOND_BASE_URL = "https://api.vebo.xyz/";

    public static Retrofit getClient(boolean useSecondBaseUrl) {
        String url = useSecondBaseUrl ? SECOND_BASE_URL : BASE_URL;
        return new Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }
}