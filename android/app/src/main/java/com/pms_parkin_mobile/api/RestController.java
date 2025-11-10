package com.pms_parkin_mobile.api;

import android.util.Log;


import com.pms_parkin_mobile.dto.User;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;

public class RestController {

    private RetrofitAPI retrofitAPI;

    public RestController() {
        this.retrofitAPI = RetropitClient.getApiService();
    }

    public void getUserId(Integer user_id, Callback<User> callback) {
        Log.d("RestController", "getUserId called with user_id: " + user_id);
        Call<User> call = retrofitAPI.getUserId(user_id);
        call.enqueue(callback);
    }


}
