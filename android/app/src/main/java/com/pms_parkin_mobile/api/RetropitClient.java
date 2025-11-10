package com.pms_parkin_mobile.api;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pms_parkin_mobile.dto.Network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

//웹서버로 전송하기 위한 url구성
public class RetropitClient {

    private static Network network = new Network();
    private static Retrofit retrofit = null;
    private static final String BASE_URL = network.getIp() + ":" + network.getPort();


    public static RetrofitAPI getApiService(){return getInstance().create(RetrofitAPI.class);}

    private static Retrofit getInstance(){
        Gson gson = new GsonBuilder().setLenient().create();
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }
}
