package com.pms_parkin_mobile.api;

import android.util.Log;


import com.pms_parkin_mobile.dto.Total;
import com.pms_parkin_mobile.dto.User;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;

public class RestController {

    private RetrofitAPI retrofitAPI;
    private static final String TAG = "RestController";
    /** 싱글톤 인스턴스 (volatile로 가시성 보장) */
    private static volatile RestController INSTANCE;


    private RestController() {
        this.retrofitAPI = RetropitClient.getApiService();
    }


    public static RestController getInstance() {
        RestController local = INSTANCE;
        if (local == null) {
            synchronized (RestController.class) {
                local = INSTANCE;
                if (local == null) {
                    local = new RestController();
                    INSTANCE = local;
                }
            }
        }
        return local;
    }


    public void getUserId(Integer user_id, Callback<User> callback) {
        Log.d("RestController", "getUserId called with user_id: " + user_id);
        Call<User> call = retrofitAPI.getUserId(user_id);
        call.enqueue(callback);
    }


    public void gyroinfo(String userId, int errorcode,  Callback<Void> callback) {
        Log.d("RestController", "getUserId called with user_id: " + userId);
        Call<Void> call = retrofitAPI.gyroinfo(userId, errorcode);
        call.enqueue(callback);
    }

    public void parking(String userId, String dong, String ho, Total total, Callback<Void> callback) {
        Log.d("RestController", "parking : " + userId);
        Call<Void> call = retrofitAPI.parking(userId,dong,ho, total);
        call.enqueue(callback);
    }
}
