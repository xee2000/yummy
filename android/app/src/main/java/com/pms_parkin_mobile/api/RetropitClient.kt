package com.pms_parkin_mobile.api

import com.google.gson.GsonBuilder
import com.pms_parkin_mobile.dto.Network
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


//웹서버로 전송하기 위한 url구성
object RetropitClient {
    private val network = Network()
    private val retrofit: Retrofit? = null
    private val BASE_URL = network.ip + ":" + network.port+ "/pms-dongtan/"

    val apiService: RetrofitAPI
        get() = instance.create<RetrofitAPI>(RetrofitAPI::class.java)

    private val instance: Retrofit
        get() {
            val gson = GsonBuilder().setLenient().create()
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
        }
}
