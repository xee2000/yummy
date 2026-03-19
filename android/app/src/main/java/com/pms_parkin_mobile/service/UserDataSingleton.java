package com.pms_parkin_mobile.service;

import com.pms_parkin_mobile.dto.LobbyOpenData;

import java.util.ArrayList;

import lombok.Data;

@Data
public class UserDataSingleton {

    private String Dong;
    private String Ho;
    private String UserName;
    private String Cel;
    private String ID;
    private boolean IsDriver;
    private String userId;
    private ArrayList<LobbyOpenData> OpenMINOR;


    private static final UserDataSingleton ourInstance = new UserDataSingleton();

    public static UserDataSingleton getInstance() {
        return ourInstance;
    }

    private UserDataSingleton() {
        OpenMINOR=new ArrayList<>();
    }
}