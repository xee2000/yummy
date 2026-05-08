package com.pms_parkin_mobile.service

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPOutputStream

object TotalCompressor {
    private const val TAG = "TotalCompressor"

    private val gson: Gson = GsonBuilder()
        .excludeFieldsWithoutExposeAnnotation()
        .create()

    /**
     * obj → JSON → GZIP 압축 바이트 배열
     *
     * ⚠️ 기존: gson.toJson(obj) 로 중간 String 전체를 힙에 올린 뒤 압축
     *    → 대용량 리스트(sensorList, gyroList 등)가 많을 경우 OOM 발생
     *
     * ✅ 개선: gson.toJson(obj, writer) 스트리밍 방식
     *    → JSON 문자열을 메모리에 통째로 올리지 않고 GZIPOutputStream 에 직접 기록
     *    → 힙 사용량 대폭 감소
     */
    @JvmStatic
    fun toGzipBytes(obj: Any?): ByteArray {
        if (obj == null) return ByteArray(0)

        return try {
            val bos = ByteArrayOutputStream()
            GZIPOutputStream(bos).use { gzip ->
                OutputStreamWriter(gzip, StandardCharsets.UTF_8).use { writer ->
                    // 스트리밍 직렬화: 중간 String 없이 writer 에 직접 기록
                    gson.toJson(obj, writer)
                }
                // GZIPOutputStream 은 use 블록 종료 시 자동 close/finish
            }
            val result = bos.toByteArray()
            Log.d(TAG, "toGzipBytes 완료: ${result.size} bytes")
            result
        } catch (e: Exception) {
            Log.e(TAG, "toGzipBytes 실패", e)
            ByteArray(0)
        }
    }

    private fun toKb(bytes: Int): String = String.format("%.2f", bytes / 1024.0)
}
