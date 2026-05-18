package com.pms_parkin_mobile.api

import com.pms_parkin_mobile.dto.BeaconLocationResponse
import com.pms_parkin_mobile.dto.User
import com.woorisystem.domain.ParkingTotal
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query


//Web통신처리 API
interface RetrofitAPI {



    @POST("app/gateInfo")
    fun gyroinfo(@Query("userId") userId: String?, @Query("count") errorcode: Int): Call<Void?>?

    @POST("app/calcLocation")
    fun parking(
        @Query("userId") userId: String?,
        @Query("dong") dong: String?,
        @Query("ho") ho: String?,
        @Body total: ParkingTotal?
    ): Call<Void?>?

    @FormUrlEncoded
    @POST("app/openLobby")
    fun openLobby(
        @Field("id") id: String?,
        @Field("dong") dong: String?,
        @Field("ho") ho: String?,
        @Field("minor") minor: String?,
        @Field("rssi") rssi: String?
    ): Call<Map<String, Any>>?

    @POST("app/openLobby/check")
    fun openLobbyinit(@Query("userId") userId: String?): Call<Void?>?

    @POST("app/openLobby/null")
    fun openLobbyDataNull(@Query("userId") userId: String?): Call<Void?>?

    @POST("app/openLobby/rssi/check")
    fun openLobbyRssiFail(@Query("userId") userId: String?): Call<Void?>?

    @POST("app/error/message")
    fun errorMessage(
        @Query("userId") userId: String?,
        @Query("message") errorMessage: String?
    ): Call<Void?>?

    @POST("app/message")
    fun Message(
        @Query("userId") userId: String?,
        @Query("message") message: String?
    ): Call<Void?>?

    /**
     * 추정 좌표(x, y)를 서버로 전송하여 해당 범위에 속한 비컨 1개를 반환받는 API
     * 서버 구현: GET /app/beacon/findByLocation?x=...&y=...
     * 응답: { "beaconId": "00F5", "x": 123.0, "y": 456.0 }
     */
    @GET("app/beacon/findByLocation")
    fun findBeaconByLocation(
        @Query("x") x: Double,
        @Query("y") y: Double
    ): Call<BeaconLocationResponse?>?


}

