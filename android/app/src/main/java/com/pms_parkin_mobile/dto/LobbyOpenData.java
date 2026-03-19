package com.pms_parkin_mobile.dto;

import lombok.Data;

@Data
public class LobbyOpenData {

    private String minor;
    private String rssi;
    private String id;
    private String dong;
    private String ho;

    public LobbyOpenData(){}
}