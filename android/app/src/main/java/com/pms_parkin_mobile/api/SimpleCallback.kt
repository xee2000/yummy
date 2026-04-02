package com.pms_parkin_mobile.api

import android.util.Log
import retrofit2.Call
import retrofit2.Callback

abstract class SimpleCallback<T>(private val tag: String?) : Callback<T?> {
    override fun onFailure(call: Call<T?>, t: Throwable) {
        Log.e(tag, "Network Error: " + t.message)
    }
}
