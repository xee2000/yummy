package com.pms_parkin_mobile.api;

import android.util.Log;

import retrofit2.Call;
import retrofit2.Callback;

public abstract class SimpleCallback<T> implements Callback<T> {
    private final String tag;

    public SimpleCallback(String tag) {
        this.tag = tag;
    }

    @Override
    public void onFailure(Call<T> call, Throwable t) {
        Log.e(tag, "Network Error: " + t.getMessage());
    }
}
