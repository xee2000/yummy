package com.pms_parkin_mobile.dataManager;

import android.util.Log;

import com.pms_parkin_mobile.dto.AccelBeacon;
import com.pms_parkin_mobile.dto.GyroSensor;
import com.pms_parkin_mobile.service.App;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

public class SaveArrayListValue {

    // 2025.02.17 by jhlee 시작
    public void SaveAccelBeacon() {
        if (App.getInstance().getmAccelBeaconMap() != null) {
            for (String key : App.getInstance().getmAccelBeaconMap().keySet()) {
                AccelBeacon value = App.getInstance().getmAccelBeaconMap().get(key);
                if (value == null) continue;

                String id = value.getBeaconId();
                String rssi = value.getRssi();
                String delay = value.getDelay();
                String count = value.getCount();

                // 기존 delayList (Set 혹은 ArrayList로 저장됨)
                LinkedHashSet<String> accelDelay = App.getInstance().getmAccelBeaconDelayMap().get(id);
                Log.d("TEST","TAG_ACCEL_VALUE_INFORM2 - ACCEL VALUE : " + accelDelay + " , " + App.getInstance().getmAccelBeaconMap());

                // 동일 delay 기준으로 최고 rssi만 남기도록 처리
                // key: delay, value: 전체 문자열 ("rssi_delay")
                Map<String, String> delayToEntry = new HashMap<>();
                if (accelDelay != null) {
                    for (String entry : accelDelay) {
                        String[] parts = entry.split("_", 2);
                        if (parts.length == 2) {
                            String entryDelay = parts[1];
                            double entryRssi = Double.parseDouble(parts[0]);
                            if (delayToEntry.containsKey(entryDelay)) {
                                String existingEntry = delayToEntry.get(entryDelay);
                                double existingRssi = Double.parseDouble(existingEntry.split("_", 2)[0]);
                                if (entryRssi > existingRssi) {
                                    delayToEntry.put(entryDelay, entry);
                                }
                            } else {
                                delayToEntry.put(entryDelay, entry);
                            }
                        }
                    }
                }
                // Map의 값들을 새로운 LinkedHashSet에 저장 (필요 시 순서를 유지)
                LinkedHashSet<String> filteredDelay = new LinkedHashSet<>(delayToEntry.values());

                // 필요에 따라, delay 기준 오름차순 정렬을 적용하려면 아래처럼 ArrayList로 변환 후 정렬 후 다시 set에 넣을 수 있습니다.
                ArrayList<String> sortedList = new ArrayList<>(filteredDelay);
                Collections.sort(sortedList, (s1, s2) -> {
                    int d1 = Integer.parseInt(s1.split("_", 2)[1]);
                    int d2 = Integer.parseInt(s2.split("_", 2)[1]);
                    return Integer.compare(d1, d2);
                });
                filteredDelay = new LinkedHashSet<>(sortedList);

                // 새 AccelBeacon 객체 생성 후 업데이트된 delayList 적용
                AccelBeacon accelBeacon = new AccelBeacon();
                accelBeacon.setBeaconId(id);
                accelBeacon.setRssi(rssi);
                accelBeacon.setDelay(delay);
                accelBeacon.setCount(count);
                accelBeacon.setDelayList(filteredDelay);

                App.getInstance().getmAccelBeaconArrayList().add(accelBeacon);
            }
        }
    }


    public String AccelSensorResult() {
        int resultCount = App.getInstance().getmAccelCount();

        String result;

        if (resultCount < 3 && resultCount >= 0) {
            result = "T";
        } else if (resultCount < 12 && resultCount >= 3) {
            result = "S";
        } else {
            result = "W";
        }

        return result;
    }

    public void SaveGyro() {

        GyroSensor gyroSensor = new GyroSensor();
        gyroSensor.setDelay(String.valueOf(App.getInstance().getmWholeTimerDelay()));
        gyroSensor.setX(String.valueOf(App.getInstance().getmSaveCountRoll()));
        gyroSensor.setY(String.valueOf(App.getInstance().getmSaveCountPitch()));
        gyroSensor.setZ(String.valueOf(App.getInstance().getmSaveCountYaw()));

        App.getInstance().getmGyroSensorArrayList().add(gyroSensor);
    }

    public void SaveAccelBeacon(String id, String rssi, String delay) {
        AccelBeacon accelBeacon = App.getInstance().getmAccelBeaconMap().get(id);

        if (accelBeacon == null) {
            String LogValue = String.format("ID : %s , RSSI : %s , DELAY : %s", id, rssi, delay);
            Log.d("TEST","TEST_LOG_190221_AB - 엑셀 비컨 저장 - 값 없음 -> 추가 // %s" +  LogValue);

            AccelBeacon saveAccelBeacon = new AccelBeacon();
            saveAccelBeacon.setBeaconId(id);
            saveAccelBeacon.setRssi(rssi);
            saveAccelBeacon.setDelay(delay);
            saveAccelBeacon.setCount("1");
            LinkedHashSet<String>  delayList = App.getInstance().SetDelayList(rssi + "_" + delay);
            saveAccelBeacon.setDelayList(delayList);

            App.getInstance().getmAccelBeaconMap().put(id, saveAccelBeacon);
        } else {
            if (Float.parseFloat(rssi) >= Float.parseFloat(accelBeacon.getRssi())) {
                String LogValue = String.format("ID : %s , RSSI : %s , DELAY : %s", id, rssi, delay);
                Log.d("TEST","TEST_LOG_190221_AB - 엑셀 비컨 저장 - 값 있음 -> 변경 // %s" + LogValue);

                int accelCount = Integer.parseInt(accelBeacon.getCount());
                accelCount++;

                AccelBeacon saveAccelBeacon = new AccelBeacon();
                saveAccelBeacon.setBeaconId(id);
                saveAccelBeacon.setRssi(rssi);
                saveAccelBeacon.setDelay(delay);
                saveAccelBeacon.setCount(String.valueOf(accelCount));
                LinkedHashSet<String> delayList = App.getInstance().SetDelayList(rssi + "_" + delay);
                saveAccelBeacon.setDelayList(delayList);

                App.getInstance().getmAccelBeaconMap().put(id, saveAccelBeacon);
            } else {
                int accelCount = Integer.parseInt(accelBeacon.getCount());
                accelCount++;

                String LogValue = String.format("ID : %s , RSSI : %s , DELAY : %s", accelBeacon.getBeaconId(), accelBeacon.getRssi(), accelBeacon.getDelay());

                AccelBeacon saveAccelBeacon = new AccelBeacon();
                saveAccelBeacon.setBeaconId(accelBeacon.getBeaconId());
                saveAccelBeacon.setRssi(accelBeacon.getRssi());
                saveAccelBeacon.setDelay(accelBeacon.getDelay());
                saveAccelBeacon.setCount(String.valueOf(accelCount));
                LinkedHashSet<String> delayList = App.getInstance().SetDelayList(rssi + "_" + delay);
                accelBeacon.setDelayList(delayList);

                App.getInstance().getmAccelBeaconMap().put(id, saveAccelBeacon);
            }
        }
        // 2025.02.17 by jhlee 끝
    }
}

