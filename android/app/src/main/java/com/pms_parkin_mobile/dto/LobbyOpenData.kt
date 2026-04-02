package com.pms_parkin_mobile.dto

import lombok.Data
data class LobbyOpenData(
    var minor: String? = null,
    var rssi: String? = null,
    var id: String? = null,
    var dong: String? = null,
    var ho: String? = null
)