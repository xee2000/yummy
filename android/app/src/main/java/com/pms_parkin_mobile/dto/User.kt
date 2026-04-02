package com.pms_parkin_mobile.dto

import lombok.Data
import lombok.RequiredArgsConstructor
import org.json.JSONObject

@Data
@RequiredArgsConstructor
class User(jsonString: String) {
    // Getter 메서드
    var id: Int
        private set
    var userId: String?
        private set
    val userPwd: String? = null
    var phone: String?
        private set
    var userName: String?
        private set
    var dong: String?
        private set
    var ho: String?
        private set
    val buildingId: Int = 0
    val latitude = 0.0
    val longitude = 0.0

    init {
        val jsonObject = JSONObject(jsonString)
        this.id = jsonObject.getInt("id")
        this.userId = jsonObject.getString("userId")
        this.phone = jsonObject.getString("phone")
        this.userName = jsonObject.getString("alias")
        this.dong = jsonObject.getString("dong")
        this.ho = jsonObject.getString("ho")
    }
}