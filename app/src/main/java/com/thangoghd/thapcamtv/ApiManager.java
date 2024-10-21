package com.thangoghd.thapcamtv;

public class ApiManager {
    private static SportApi sportApi = null;

    public static SportApi getSportApi() {
        if (sportApi == null) {
            sportApi = RetrofitClient.getClient().create(SportApi.class);
        }
        return sportApi;
    }
}
