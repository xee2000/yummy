package com.pms_parkin_mobile.api;




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


    @GET("/balem_web/api/user/get")
    Call<User> getUserId(@Query("id") Integer userId);
}