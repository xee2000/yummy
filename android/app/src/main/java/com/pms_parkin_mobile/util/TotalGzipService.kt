package com.pms_parkin_mobile.util

import com.google.gson.GsonBuilder
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPOutputStream

/**
 * TotalGzipService
 *
 * ParkingTotal 객체를 JSON 직렬화 후 Gzip 압축하여 byte[] 반환
 * bigDataSend 모드에서 Total.file 필드에 첨부해 서버로 전송
 *
 * 기존 TotalGzipService.java → Kotlin object로 변환
 */
object TotalGzipService {
    private val gson = GsonBuilder()
        .excludeFieldsWithoutExposeAnnotation()
        .create()

    fun toGzipBytes(obj: Any?): ByteArray {
        if (obj == null) return ByteArray(0)
        return try {
            val json  = gson.toJson(obj)
            val input = json.toByteArray(StandardCharsets.UTF_8)
            val bos   = ByteArrayOutputStream()
            GZIPOutputStream(bos).use { it.write(input) }
            bos.toByteArray()
        } catch (e: Exception) {
            Timber.e("TotalGzipService: 압축 실패 - ${e.message}")
            ByteArray(0)
        }
    }
}
