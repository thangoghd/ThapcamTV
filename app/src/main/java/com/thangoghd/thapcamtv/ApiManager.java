package com.thangoghd.thapcamtv.api;

import com.thangoghd.thapcamtv.RetrofitClient;
import com.thangoghd.thapcamtv.SportApi;

public class ApiManager {
    private static SportApi sportApi = null;

    public static SportApi getSportApi() {
        if (sportApi == null) {
            sportApi = RetrofitClient.getClient().create(SportApi.class);
        }
        return sportApi;
    }
}
