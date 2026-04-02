package com.pms_parkin_mobile.service

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPOutputStream

object TotalCompressor {
    private const val TAG = "TotalCompressor"

    // @Expose 붙은 필드만 쓰고 싶으면 excludeFieldsWithoutExposeAnnotation() 유지
    private val gson: Gson = GsonBuilder()
        .excludeFieldsWithoutExposeAnnotation()
        .create()

    @JvmStatic
    fun toGzipBytes(obj: Any?): ByteArray {
        if (obj == null) return ByteArray(0)

        try {
            val json = gson.toJson(obj)

            Log.d("TEST", "json : " + json)
            val input: ByteArray? = json.toByteArray(StandardCharsets.UTF_8)

            val bos = ByteArrayOutputStream()
            val gzip = GZIPOutputStream(bos)
            gzip.write(input)
            gzip.close()

            return bos.toByteArray()
        } catch (e: Exception) {
            return ByteArray(0)
        }
    }


    // KB로 보기 좋게 변환
    private fun toKb(bytes: Int): String {
        return String.format("%.2f", bytes / 1024.0)
    }
}