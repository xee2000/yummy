package com.pms_parkin_mobile.service;

import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

public final class TotalCompressor {

    private static final String TAG = "TotalCompressor";

    // @Expose 붙은 필드만 쓰고 싶으면 excludeFieldsWithoutExposeAnnotation() 유지
    private static final Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    private TotalCompressor() {
        // static 유틸 클래스
    }

    public static byte[] toGzipBytes(Object obj) {


        if (obj == null) return new byte[0];

        try {
            String json = gson.toJson(obj);

            Log.d("TEST", "json : " + json);
            byte[] input = json.getBytes(StandardCharsets.UTF_8);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            GZIPOutputStream gzip = new GZIPOutputStream(bos);
            gzip.write(input);
            gzip.close();

            return bos.toByteArray();
        } catch (Exception e) {
            return new byte[0];
        }
    }


    // KB로 보기 좋게 변환
    private static String toKb(int bytes) {
        return String.format("%.2f", bytes / 1024.0);
    }
}