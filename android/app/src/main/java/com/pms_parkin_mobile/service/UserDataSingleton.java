package com.pms_parkin_mobile.service;

import android.content.Context;
import android.content.SharedPreferences;

import com.pms_parkin_mobile.dto.LobbyOpenData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class UserDataSingleton {

    private String Dong;
    private String Ho;
    private String UserName;
    private String Cel;
    private String ID;
    private boolean IsDriver;
    private String userId;
    private ArrayList<LobbyOpenData> OpenMINOR;

    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "user_data_pref";

    private static final UserDataSingleton ourInstance = new UserDataSingleton();

    public static UserDataSingleton getInstance() {
        return ourInstance;
    }

    private UserDataSingleton() {
        OpenMINOR = new ArrayList<>();
    }

    /** App.init() 에서 호출 — 프로세스 재시작 후에도 데이터 복원 */
    public void init(Context context) {
        sharedPreferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        loadFromPrefs();
    }

    private void loadFromPrefs() {
        if (sharedPreferences == null) return;
        Dong     = sharedPreferences.getString("dong",      null);
        Ho       = sharedPreferences.getString("ho",        null);
        UserName = sharedPreferences.getString("user_name", null);
        Cel      = sharedPreferences.getString("cel",       null);
        ID       = sharedPreferences.getString("id",        null);
        IsDriver = sharedPreferences.getBoolean("is_driver", false);
        userId   = sharedPreferences.getString("user_id",   null);

        String jsonStr = sharedPreferences.getString("open_minor", null);
        if (jsonStr != null) {
            try {
                OpenMINOR = new ArrayList<>();
                JSONArray arr = new JSONArray(jsonStr);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    LobbyOpenData data = new LobbyOpenData();
                    data.setMinor(obj.optString("minor"));
                    data.setRssi(obj.optString("rssi"));
                    data.setId(obj.optString("id"));
                    data.setDong(obj.optString("dong"));
                    data.setHo(obj.optString("ho"));
                    OpenMINOR.add(data);
                }
            } catch (JSONException e) {
                OpenMINOR = new ArrayList<>();
            }
        }
    }

    // ======== Getters ========
    public String getDong()                        { return Dong; }
    public String getHo()                          { return Ho; }
    public String getUserName()                    { return UserName; }
    public String getCel()                         { return Cel; }
    public String getID()                          { return ID; }
    public boolean isIsDriver()                    { return IsDriver; }
    public String getUserId()                      { return userId; }
    public ArrayList<LobbyOpenData> getOpenMINOR() { return OpenMINOR; }

    // ======== Setters (SharedPreferences 동시 저장) ========
    public void setDong(String dong) {
        this.Dong = dong;
        save("dong", dong);
    }

    public void setHo(String ho) {
        this.Ho = ho;
        save("ho", ho);
    }

    public void setUserName(String userName) {
        this.UserName = userName;
        save("user_name", userName);
    }

    public void setCel(String cel) {
        this.Cel = cel;
        save("cel", cel);
    }

    public void setID(String id) {
        this.ID = id;
        save("id", id);
    }

    public void setIsDriver(boolean isDriver) {
        this.IsDriver = isDriver;
        if (sharedPreferences != null) {
            sharedPreferences.edit().putBoolean("is_driver", isDriver).apply();
        }
    }

    public void setUserId(String userId) {
        this.userId = userId;
        save("user_id", userId);
    }

    public void setOpenMINOR(ArrayList<LobbyOpenData> openMINOR) {
        this.OpenMINOR = openMINOR;
        if (sharedPreferences == null || openMINOR == null) return;
        try {
            JSONArray arr = new JSONArray();
            for (LobbyOpenData data : openMINOR) {
                JSONObject obj = new JSONObject();
                obj.put("minor", data.getMinor());
                obj.put("rssi",  data.getRssi());
                obj.put("id",    data.getId());
                obj.put("dong",  data.getDong());
                obj.put("ho",    data.getHo());
                arr.put(obj);
            }
            sharedPreferences.edit().putString("open_minor", arr.toString()).apply();
        } catch (JSONException ignore) {}
    }

    private void save(String key, String value) {
        if (sharedPreferences != null) {
            sharedPreferences.edit().putString(key, value).apply();
        }
    }
}
