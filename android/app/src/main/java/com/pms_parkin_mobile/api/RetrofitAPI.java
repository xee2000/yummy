package com.pms_parkin_mobile.api;




import com.pms_parkin_mobile.dto.Total;
import com.pms_parkin_mobile.dto.User;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Query;

//Web통신처리 API
public interface RetrofitAPI {


    @GET("app/user/get")
    Call<User> getUserId(@Query("id") Integer userId);

    @POST("app/gyroInfo")
    Call<Void> gyroinfo(@Query("userId") String userId,@Query("count") int errorcode);

    @POST("app/calcLocation")
    Call<Void> parking(@Query("userId")String userId,@Query("dong")String dong, @Query("ho")String ho, @Body Total total);
}

