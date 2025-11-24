package com.pms_parkin_mobile.dto;

import org.json.JSONException;
import org.json.JSONObject;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class User {

    private int id;
    private String userId;
    private String userPwd;
    private String phone;
    private String userName;
    private String dong;
    private String ho;
    private int buildingId;
    private double latitude;
    private double longitude;

    public User(String jsonString) throws JSONException {
        JSONObject jsonObject = new JSONObject(jsonString);
        this.id = jsonObject.getInt("id");
        this.userId = jsonObject.getString("user_id");
        this.userPwd = jsonObject.getString("user_pwd");
        this.phone = jsonObject.getString("phone");
        this.userName = jsonObject.getString("user_name");
        this.dong = jsonObject.getString("dong");
        this.ho = jsonObject.getString("ho");
        this.buildingId = jsonObject.getInt("building_id");
    }

    // Getter 메서드
    public int getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserPwd() {
        return userPwd;
    }

    public String getPhone() {
        return phone;
    }

    public String getUserName() {
        return userName;
    }

    public String getDong() {
        return dong;
    }

    public String getHo() {
        return ho;
    }

    public int getBuildingId() {
        return buildingId;
    }
}