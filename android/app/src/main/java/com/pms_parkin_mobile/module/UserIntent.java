package com.pms_parkin_mobile.module;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.Nullable;

import com.pms_parkin_mobile.dto.LobbyOpenData;
import com.pms_parkin_mobile.dto.User;
import com.pms_parkin_mobile.service.App;
import com.pms_parkin_mobile.service.UserDataSingleton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

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

                    // 1. JSON 객체 생성
                    JSONObject jsonObject = new JSONObject(data);

                    // 2. 싱글톤 인스턴스 가져오기 및 데이터 세팅
                    UserDataSingleton userStore = UserDataSingleton.getInstance();

                    userStore.setDong(jsonObject.optString("dong"));
                    userStore.setHo(jsonObject.optString("ho"));
                    userStore.setUserName(jsonObject.optString("name"));
                    userStore.setCel(jsonObject.optString("cel"));
                    userStore.setUserId(jsonObject.optString("userId"));
                    // 3. minorList 파싱 및 ArrayList 저장
                    if (jsonObject.has("minorList")) {
                        JSONArray jsonArray = jsonObject.getJSONArray("minorList");
                        ArrayList<LobbyOpenData> minorList = new ArrayList<>();

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject item = jsonArray.getJSONObject(i);
                            LobbyOpenData lobbyData = new LobbyOpenData();
                            lobbyData.setMinor(item.optString("minor"));
                            lobbyData.setRssi(item.optString("rssi"));
                            lobbyData.setId(jsonObject.optString("userId"));
                            lobbyData.setDong(jsonObject.optString("dong"));
                            lobbyData.setHo(jsonObject.optString("ho"));
                            minorList.add(lobbyData);
                        }
                        Log.d("Ble" , "minorList : " + minorList);
                        userStore.setOpenMINOR(minorList);
                    }
                } else {
                    Log.e("UserIntent", "Received data is null");
                }
            } catch (JSONException e) {
                Log.e("UserIntent", "JSON Parsing Error", e);
            }
        }

    }
}

