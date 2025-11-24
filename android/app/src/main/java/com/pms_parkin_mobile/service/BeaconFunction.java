package com.pms_parkin_mobile.service;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.pms_parkin_mobile.dataManager.SaveArrayListValue;
import com.pms_parkin_mobile.dto.Beacon;
import com.pms_parkin_mobile.util.Hex;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.TreeSet;

public class BeaconFunction {
    private final TimerSingleton mTimerSingleton;
    private final Context mContext;

    public BeaconFunction(Context context) {
        this.mContext = context;

        mTimerSingleton = TimerSingleton.getInstance();
    }

    // 입구 비컨

    /**
     * ** 종료 준비 **
     * -> 전재 조건 : 3번 비컨(엘리베이터)이 컨저 들어와선 안된다
     * : 1번 비컨(로비)을 받은 후 3번 비컨(엘리베이터)을 받을 수 있도록 처리하여야 한다.
     * : 1번 비컨(로비)를 초기화 시키기 전까지는 한번 받은 이후로 받을 수 없다.
     * : 들어온 이후로는 계속 받는지만 Check
     * <p>
     * -> 종료
     * : 로비 -> 엘리베이터 순으로 비컨이 들어와야한다.
     * : 로비 들어온 이후 엘리베이터를 받으면 종료 모드 시작
     * : 종료 모드 시작되면 일정 시간동안 로비.엘리베이터 비컨을 계속 받는지 Check -> 안받기 시작한 시점을 기준으로 3~5초간 안들어오면 종료
     * <p>
     * -> 시작      : 엘리베이터 -> 로비 순으로 비컨이 들어와야한다.
     * : 로비가 들어온 이후 T 상태로 Gyro 가 발생할 경우 시작
     * <p>
     * -> 1층 로비 : 3번째 자리가 1이다.
     **/
    // 로비 비컨


    // 엘리베이터 비컨


    public void ChangeBeacon(int major, int minor, double rssi, SaveArrayListValue saveArrayListValue) {


        if (App.getInstance().isStartFlag()) {
            String id;

            if (minor > 32768) {
                id = String.valueOf(minor - 32768);
                String hexValue = String.format("%04X", Integer.valueOf(id));

                Beacon beacon = new Beacon();
                beacon.setBeaconId(hexValue);
                beacon.setRssi(String.valueOf(rssi));
                beacon.setState(String.valueOf(major));
                beacon.setDelay(String.valueOf(App.getInstance().getmWholeTimerDelay()));
                beacon.setSeq(String.valueOf(App.getInstance().getmBeaconSequence()));

                App.getInstance().getmBeaconArrayList().add(beacon);

                Log.d("BeaconFunction", "TAG_BEACON_CHANGE - BEACON VALUE : " + hexValue  + " : " + rssi);
                int beaconSequence = App.getInstance().getmBeaconSequence() + 1;
                App.getInstance().setmBeaconSequence(beaconSequence);
            } else {
                id = String.valueOf(minor);
            }

            App.getInstance().setmCollectAccelBeaconStart(true);
            if (App.getInstance().ismCollectAccelBeaconStart()) {
                String hexValue = String.format("%04X", Integer.valueOf(id));
                saveArrayListValue.SaveAccelBeacon(hexValue, String.valueOf(rssi), String.valueOf(App.getInstance().getmWholeTimerDelay()));
                // 2025.02.17 by jhlee
                AddAccelDelay(hexValue, rssi);
            }
        }
    }

//    public void OnlyOpenLobby(String uuid, int minor, double rssi) {
//        UserDataSingleton userDataSingleton = UserDataSingleton.getInstance();
//        ArrayList<OnepassBeacon> onepassList = userDataSingleton.getOnepassList();
//
//        final TimerSingleton timerSingleton = TimerSingleton.getInstance();
//
//        for (int i = 0; i < onepassList.size(); i++) {
//            OnepassBeacon onepassBeacon = onepassList.get(i);
//            if (!uuid.equals(onepassBeacon.getUuid())) continue;
//
//            String lobbyMinor = onepassBeacon.getMinor();
//            double lobbyRssi = onepassBeacon.getRssi();
//            String minorHex = String.format("%04X", minor);
//
//            if (lobbyMinor.equals(minorHex) && rssi >= lobbyRssi) {
//                if (!timerSingleton.isLobbyTimerStart()) {
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                        // only for gingerbread and newer versions
//                        timerSingleton.StartLobbyTimer();
//                    } else {
//                        Handler handler = new Handler(Looper.getMainLooper());
//                        handler.post(timerSingleton::StartLobbyTimer);
//                    }
//                }
//
//                if (userDataSingleton.getPurpose().contains(BluetoothServiceUsage.BLE_ONEPASS)) {
//                    Timber.v("ONLY_LOBBY_BEACON - OPEN 요청 : %s, %s", timerSingleton.isLobbyTimerStart(), rssi);
//                    ParkingServiceApi.getInstance().OpenLobby(mContext, onepassBeacon, rssi);
//                }
//            }
//        }
//        //endregion
//    }
//
//    // jhlee 2024-12-27 시작 : 주차장에서 신호가 약한 비컨이 들어오면 서비스를 멈추면 안됨
//    public void stayParkingService() {
//        if (mTimerSingleton.isCOLLECT_START_BEACON_CALC()) {
//            int value = mDataManagerSingleton.getCollectStartCalcBeacon();
//            mDataManagerSingleton.setCollectStartCalcBeacon(++value);
//        }
//    }
    // jhlee 2024-12-27 끝

    // jhlee 2025-2-20 delayList 중복 제거 및 오름차순
    public void AddAccelDelay(String HexValue, double rssi) {
        int delay = App.getInstance().getmWholeTimerDelay();
        String delayStr = String.valueOf(delay);
        // "rssi_delay" 형식 (rssi가 앞, delay가 뒤)
        String newEntry = ((int)rssi) + "_" + delayStr;

        // 기존에 저장된 Set을 가져오거나 새로 생성 (맵의 타입을 LinkedHashSet<String>으로 가정)
        LinkedHashSet<String> set  = App.getInstance().getmAccelBeaconDelayMap().get(HexValue);
        if (set == null) {
            set = new LinkedHashSet<>();
        }

        String toUpdate = null;
        // set의 각 항목에서 뒤의 delay 값을 기준으로 중복 검사
        for (String entry : set) {
            String[] parts = entry.split("_", 2); // parts[0]: rssi, parts[1]: delay
            if (parts.length == 2 && parts[1].equals(delayStr)) {
                double storedRssi = Double.parseDouble(parts[0]);
                if (rssi > storedRssi) {
                    toUpdate = entry;
                }
                break;
            }
        }
        if (toUpdate != null) {
            set.remove(toUpdate);
        }
        set.add(newEntry);

        // TreeSet을 사용하여 delay(두 번째 부분) 기준으로 오름차순 정렬
        TreeSet<String> sortedTreeSet = new TreeSet<>((s1, s2) -> {
            // NULL이거나 "_"를 포함하지 않을 경우 Return 0
            if (s1 == null || s2 == null || !s1.contains("_") || !s2.contains("_")) {
                return 0;
            }
            String[] parts1 = s1.split("_", 2);
            String[] parts2 = s2.split("_", 2);
            int delay1 = Integer.parseInt(parts1[1]);
            int delay2 = Integer.parseInt(parts2[1]);
            return Integer.compare(delay1, delay2);
        });
        sortedTreeSet.addAll(set);

        // 정렬된 결과를 다시 LinkedHashSet에 담아 순서를 유지
        LinkedHashSet<String> sortedSet = new LinkedHashSet<>(sortedTreeSet);

        // 맵에 정렬된 Set을 저장 (맵의 값 타입이 LinkedHashSet<String>이어야 함)
        App.getInstance().getmAccelBeaconDelayMap().put(HexValue, sortedSet);
    }
}
