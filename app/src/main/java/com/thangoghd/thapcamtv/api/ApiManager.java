package com.thangoghd.thapcamtv;

public class ApiManager {
    public static SportApi getSportApi(boolean isSecondUrl) {
        return RetrofitClient.getClient(isSecondUrl).create(SportApi.class);
    }
}
