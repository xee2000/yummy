package com.pms_parkin_mobile.module;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.Nullable;

import com.pms_parkin_mobile.dto.User;
import com.pms_parkin_mobile.service.App;

import org.json.JSONException;

public class UserIntent extends IntentService {

    public UserIntent() {
        super("UserIntent");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        Log.d("UserIntent", "Service started and running in the background.");


        if (intent != null) {
            try {
                String data = intent.getStringExtra("user");
                if (data != null) {
                    Log.d("UserIntent", "Received data: " + data);
                    User user = new User(data);
                    App.getInstance().setUser(user);


                } else {
                    Log.e("UserIntent", "Received data is null");
                    // 적절한 예외 처리나 기본 처리 로직 추가
                }
            } catch (JSONException e) {
                Log.e("UserIntent", "JSON parsing error", e);
                throw new RuntimeException(e);
            }

            // 데이터 처리 로직 추가
        }

    }
}

