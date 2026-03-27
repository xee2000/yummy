package com.pms_parkin_mobile.api;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;


import com.pms_parkin_mobile.dataManager.DataManagerSingleton;
import com.pms_parkin_mobile.dto.GyroSensor2;
import com.pms_parkin_mobile.dto.LobbyOpenData;
import com.pms_parkin_mobile.dto.Total;
import com.pms_parkin_mobile.dto.User;
import com.pms_parkin_mobile.foreground.OpenLobbyAlarm;
import com.pms_parkin_mobile.foreground.ParkingSuccessAlarm;
import com.pms_parkin_mobile.service.App;
import com.pms_parkin_mobile.service.TotalCompressor;
import com.pms_parkin_mobile.service.UserDataSingleton;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RestController {

    private RetrofitAPI retrofitAPI;
    private static final String TAG = "RestController";
    /** 싱글톤 인스턴스 (volatile로 가시성 보장) */
    private static volatile RestController INSTANCE;
    private boolean isRetryingNow = false;
    private ConnectivityManager.NetworkCallback networkCallback;
    private CountDownTimer networkLodingTimer;
    private RestController() {
        this.retrofitAPI = RetropitClient.getApiService();
    }


    public static RestController getInstance() {
        RestController local = INSTANCE;
        if (local == null) {
            synchronized (RestController.class) {
                local = INSTANCE;
                if (local == null) {
                    local = new RestController();
                    INSTANCE = local;
                }
            }
        }
        return local;
    }

    private boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        Network network = cm.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }



    public void getUserId(Integer user_id) {
        Log.d(TAG, "getUserId 전송 시작 - id: " + user_id);
        Call<User> call = retrofitAPI.getUserId(user_id);

        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "getUserId 조회 성공: " + response.body().toString());
                    // 필요 시 여기에 DataManager 저장 로직 추가 가능
                } else {
                    Log.e(TAG, "getUserId 조회 실패 (코드): " + response.code());
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Log.e(TAG, "getUserId 네트워크 오류: " + t.getMessage());
            }
        });
    }

    public void gyroinfo(String userId, int errorcode) {
        Log.d(TAG, "gyroinfo 전송 시작 - User: " + userId + ", Code: " + errorcode);
        Call<Void> call = retrofitAPI.gyroinfo(userId, errorcode);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "gyroinfo 전송 성공");
                } else {
                    Log.e(TAG, "gyroinfo 전송 실패 (코드): " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "gyroinfo 네트워크 오류: " + t.getMessage());
            }
        });
    }

    public void errorMessage(String userId, String errorMessage) {
        // 1. 호출 시점 로그 출력
        Log.d(TAG, "errorMessage 전송 시작 - User: " + userId + ", Msg: " + errorMessage);

        // 2. API 호출 (Retrofit)
        Call<Void> call = retrofitAPI.errorMessage(userId, errorMessage);

        // 3. 내부에서 콜백을 직접 구현하여 결과 로그까지 출력
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, retrofit2.Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "서버 에러 로그 전송 성공 [User: " + userId + "]");
                } else {
                    Log.e(TAG, "서버 에러 로그 전송 실패 (코드: " + response.code() + ")");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "서버 에러 로그 전송 중 네트워크 오류: " + t.getMessage());
            }
        });
    }


    public synchronized void Parking(Context context, Total total) {
        Log.d("TEST", "🌀 [PARKING 진입]");

        if (isRetryingNow) {
            Log.d("TEST", "🚫 이미 재시도 중 - 무시");
            return;
        }

        if (!isNetworkConnected(context)) {
            Log.d("TEST", "📴 네트워크 없음 - 연결 감지 대기 중");
            waitForNetwork(context, total);
            return;
        }

        sendParking(context, total);
    }

    public void openLobby(LobbyOpenData lobbyOpenData) {
        Log.d(TAG, "openLobby 요청 시작 - Minor: " + lobbyOpenData.getMinor());
        Call<Void> call = retrofitAPI.openLobby(
                lobbyOpenData.getId(),
                lobbyOpenData.getDong(),
                lobbyOpenData.getHo(),
                lobbyOpenData.getMinor(),
                lobbyOpenData.getRssi()
        );

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "openLobby 성공 [Minor: " + lobbyOpenData.getMinor() + "]");
                    Intent intent = new Intent(App.getInstance().getContext(), OpenLobbyAlarm.class);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        // 안드로이드 8.0 이상은 startForegroundService를 호출해야 함
                        App.getInstance().getContext().startForegroundService(intent);
                    } else {
                        App.getInstance().getContext().startService(intent);
                    }

                } else {
                    Log.e(TAG, "openLobby 실패 (코드): " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "openLobby 네트워크 오류: " + t.getMessage());
            }
        });
    }

    public void openLobbyinit(String id) {
        Log.d(TAG, "openLobbyinit 요청 시작 - ID: " + id);
        Call<Void> call = retrofitAPI.openLobbyinit(id);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "openLobbyinit 성공");
                } else {
                    Log.e(TAG, "openLobbyinit 실패 (코드): " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "openLobbyinit 네트워크 오류: " + t.getMessage());
            }
        });
    }

    /**
     * 5. 로비 데이터 없음 전송 (내부 콜백 처리)
     */
    public void openLobbyDataNull(String id) {
        Log.d(TAG, "openLobbyDataNull 요청 시작 - ID: " + id);
        Call<Void> call = retrofitAPI.openLobbyDataNull(id);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "openLobbyDataNull 전송 성공");
                } else {
                    Log.e(TAG, "openLobbyDataNull 전송 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "openLobbyDataNull 네트워크 오류: " + t.getMessage());
            }
        });
    }

    /**
     * 6. 로비 RSSI 실패 전송 (내부 콜백 처리)
     */
    public void openLobbyRssiFail(String id) {
        Log.d(TAG, "openLobbyRssiFail 요청 시작 - ID: " + id);
        Call<Void> call = retrofitAPI.openLobbyRssiFail(id);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "openLobbyRssiFail 전송 성공");
                } else {
                    Log.e(TAG, "openLobbyRssiFail 전송 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "openLobbyRssiFail 네트워크 오류: " + t.getMessage());
            }
        });
    }

    public void GyroInformation(Context context, String message) {
        // 1. 필요한 사용자 정보 가져오기 (예: App 인스턴스나 SharedPreference 이용)
        // 여기서는 기존 코드 패턴에 맞춰 임의로 userId를 가져오는 로직을 넣었습니다.
        String userId = App.getInstance().getUserId();

        Log.d(TAG, "GateInformation - message: " + message);

        // 2. API 호출
        Call<Void> call = retrofitAPI.GateInformation(userId, message);

        // 3. 내부에서 콜백 처리 (호출부에서 Callback을 안 넘겨도 되게끔)
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, retrofit2.Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "GateInformation 전송 성공: " + message);
                } else {
                    Log.e(TAG, "GateInformation 전송 실패 (코드): " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "GateInformation 네트워크 오류: " + t.getMessage());
            }
        });
    }

    private void sendParking(Context context, Total total) {
        isRetryingNow = true;

        String userId = "test";
        String dong = "999";
        String ho = "999";


        if(!UserDataSingleton.getInstance().getBigDataSend()){
            Log.d("TEST", "normalFlag : "+ UserDataSingleton.getInstance().getBigDataSend());
            Call<Void> call = retrofitAPI.Parking(total, userId, dong, ho);
            call.enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    isRetryingNow = false;
                    if (response.isSuccessful()) {
                        Log.d("TEST", "✅ Parking 요청 성공");
                        if(networkLodingTimer != null){
                            Log.d("TEST", "⏳ 타이머 중단");
                            networkLodingTimer.cancel();
                            networkLodingTimer = null;
                        }
                        DataConnectSucceessForeground(context);

                        DataManagerSingleton.getInstance().Reset();
                    } else {
                        Log.e("TEST", "❌ 응답 실패 - code: " + response.code());
                        waitForNetwork(context, total);
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    isRetryingNow = false;
                    Log.e("TEST", "🔥 요청 실패 - 에러: " + t.getMessage());
                    Log.e("TEST", "🔥 요청 실패 URL = " + call.request().url());
                    Log.e("TEST", "🔥 Throwable = " + t);                 // toString()
                    Log.e("TEST", "🔥 Class = " + t.getClass().getName()); // 예외 타입
                    Log.e("TEST", "🔥 Cause = " + t.getCause());          // 원인 체인
                    Log.e("TEST", "🔥 Stack = ", t);
                    parkingApiTimer(context, total);
                }
            });
        }else{
            Log.d("TEST", "bigFlag : "+ UserDataSingleton.getInstance().getBigDataSend());
            List<GyroSensor2> tempGyroList2 = total.getGyroList2();
            total.setGyroList2(null);
            byte[] file =  TotalCompressor.toGzipBytes(total);
            total.setFile(file);
            total.setGyroList2(tempGyroList2);

            Log.d("TEST" ,"total : " + total.getGyroList());
            Log.d("TEST" ,"total2 : " + total.getGyroList2());
            Call<Void> call = retrofitAPI.Parking(total, userId, dong, ho);
            call.enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    isRetryingNow = false;
                    if (response.isSuccessful()) {
                        Log.d("TEST", "✅ Parking 요청 성공");
                        if(networkLodingTimer != null){
                            Log.d("TEST", "⏳ 타이머 중단");
                            networkLodingTimer.cancel();
                            networkLodingTimer = null;
                        }
                        DataConnectSucceessForeground(context);

                        DataManagerSingleton.getInstance().Reset();
                    } else {
                        Log.e("TEST", "❌ 응답 실패 - code: " + response.code());
                        waitForNetwork(context, total);
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    isRetryingNow = false;
                    Log.e("TEST", "🔥 요청 실패 - 에러: " + t.getMessage());
                    Log.e("TEST", "🔥 요청 실패 URL = " + call.request().url());
                    Log.e("TEST", "🔥 Throwable = " + t);                 // toString()
                    Log.e("TEST", "🔥 Class = " + t.getClass().getName()); // 예외 타입
                    Log.e("TEST", "🔥 Cause = " + t.getCause());          // 원인 체인
                    Log.e("TEST", "🔥 Stack = ", t);
                    parkingApiTimer(context, total);
                }
            });
        }
    }

    private void waitForNetwork(Context context, Total total) {
        // 이미 콜백이 등록되어 있다면 중복 등록 방지
        if (networkCallback != null) {
            Log.d("TEST", "⏳ 이미 네트워크 콜백 등록됨 - 중복 방지");
            return;
        }

        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            Log.e("TEST", "❌ ConnectivityManager 가 null - 타이머 방식으로 대체");
            parkingApiTimer(context, total);   // fallback
            return;
        }

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        // ✅ 여기서 콜백 생성
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                Log.d("TEST", "🌐 네트워크 복구 감지 - Parking 재전송 시도");

                // 더 이상 필요 없으니 콜백 해제
                try {
                    cm.unregisterNetworkCallback(this);
                } catch (Exception e) {
                    Log.w("TEST", "unregisterNetworkCallback 실패/중복 해제 가능", e);
                }
                networkCallback = null;

                // 메인 스레드에서 재요청
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (!isRetryingNow) {
                        sendParking(context, total);
                    } else {
                        Log.d("TEST", "이미 재시도 중이어서 중복 전송은 하지 않음");
                    }
                });
            }

            @Override
            public void onLost(Network network) {
                Log.d("TEST", "📴 네트워크 다시 끊김");
            }
        };

        // ✅ 실제로 콜백 등록
        cm.registerNetworkCallback(request, networkCallback);
        Log.d("TEST", "📡 네트워크 감지 콜백 등록 완료");
    }

    private void DataConnectSucceessForeground(Context context) {
        Intent serviceIntent = new Intent(context, ParkingSuccessAlarm.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            }
        } else {
            context.startService(serviceIntent);

        }
    }

    private void parkingApiTimer(Context context, Total total) {
        if (networkLodingTimer != null) {
            Log.d("TEST", "⏳ 타이머 이미 동작 중 - 중복 방지");
            return;
        }

        networkLodingTimer = new CountDownTimer(Long.MAX_VALUE, 5000) {
            @Override
            public void onTick(long millisUntilFinished) {
                ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
                boolean isConnected = activeNetwork != null && activeNetwork.isConnected();

                Log.d("TEST", "🕒 5초마다 네트워크 체크");

                if (isConnected) {
                    Log.d("TEST", "🌐 네트워크 연결됨 - API 전송 및 타이머 종료");

                    sendParking(context, total);
                    cancel(); // 타이머 중단
                    networkLodingTimer = null;
                } else {
                    Log.d("TEST", "📴 아직 네트워크 연결 안 됨");
                }
            }

            @Override
            public void onFinish() {
                // do nothing
            }
        };

        networkLodingTimer.start();
    }

}
