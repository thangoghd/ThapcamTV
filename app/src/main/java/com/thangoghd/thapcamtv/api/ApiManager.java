package com.thangoghd.thapcamtv.api;

public class ApiManager {
    public static SportApi getSportApi(boolean isSecondUrl) {
        return RetrofitClient.getClient(isSecondUrl).create(SportApi.class);
    }
}
