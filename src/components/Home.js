// src/components/Home.js
import React, { useEffect, useRef, useState, useMemo } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  Alert,
  NativeModules,
  Modal,
  FlatList,
  ActivityIndicator,
} from 'react-native';
import EncryptedStorage from 'react-native-encrypted-storage';
import SafeScreen from '../common/SafeScreen';
import RestApi from '../common/RestApi';
import PassParkingLocation from './PassParkingLocation';

const { AndroidModule } = NativeModules;

const Home = () => {
  const timerRef = useRef(null);

  const [passiveBusy, setPassiveBusy] = useState(false);

  // 차량 목록/선택
  const [cars, setCars] = useState([]);
  const [carsLoading, setCarsLoading] = useState(false);
  const [carsModalOpen, setCarsModalOpen] = useState(false);
  const [selectedCar, setSelectedCar] = useState(null);

  // dong/ho
  const [dong, setDong] = useState(null);
  const [ho, setHo] = useState(null);

  // 결과 확인 모달
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [lastBeacon, setLastBeacon] = useState(null);

  // beaconId로 조회한 device 위치
  const [deviceLocLoading, setDeviceLocLoading] = useState(false);
  const [deviceLoc, setDeviceLoc] = useState(null); // { floor, x, y }

  const selectedCarNumber = useMemo(() => {
    if (!selectedCar) return null;
    return selectedCar.car_number ?? selectedCar.carNumber ?? null;
  }, [selectedCar]);

  // -----------------------------
  // ✅ beaconId -> device 조회 (raw x/y 그대로)
  // -----------------------------
  const fetchDeviceByBeaconId = async beaconId => {
    if (!beaconId) return null;

    setDeviceLocLoading(true);
    try {
      let res;

      // 1차: /app/findBybeaconId
      try {
        res = await RestApi.get('/app/findBybeaconId', {
          params: { beaconId: String(beaconId) },
        });
      } catch (e1) {
        // 2차: /findBybeaconId
        res = await RestApi.get('/findBybeaconId', {
          params: { beaconId: String(beaconId) },
        });
      }

      const device = res?.data ?? null;
      if (!device) return null;

      // 필요한 값만
      const floor = device?.floor ?? null;

      // ✅ raw 숫자 그대로 (정규화 금지)
      const x = Number(device?.x);
      const y = Number(device?.y);

      const loc = { floor, x, y };
      setDeviceLoc(loc);

      return loc;
    } catch (e) {
      console.warn('[Home] fetchDeviceByBeaconId error:', {
        message: e?.message,
        status: e?.response?.status,
        data: e?.response?.data,
      });
      return null;
    } finally {
      setDeviceLocLoading(false);
    }
  };

  const getUserIdFromStorage = async () => {
    try {
      const userData = await EncryptedStorage.getItem('user');
      if (!userData) return null;
      const parsed = JSON.parse(userData);

      // userId 키 후보들 (프로젝트마다 달라서 안전하게 여러개)
      return (
        parsed?.userId ??
        parsed?.user_id ??
        parsed?.id ??
        parsed?.userid ??
        parsed?.USERID ??
        null
      );
    } catch (e) {
      console.warn('[Home] getUserIdFromStorage error:', e);
      return null;
    }
  };

  const confirmParkingLocation = async () => {
    if (!lastBeacon?.beaconId) {
      Alert.alert('오류', 'beaconId가 없습니다.');
      return;
    }
    if (!selectedCarNumber) {
      Alert.alert('오류', '차량 번호가 없습니다.');
      return;
    }
    if (dong == null || ho == null) {
      Alert.alert('오류', 'dong/ho 정보가 없습니다.');
      return;
    }

    const userId = await getUserIdFromStorage();
    if (!userId) {
      Alert.alert('오류', 'userId를 찾을 수 없습니다.');
      return;
    }

    try {
      // ✅ PUT /app/updateParkingLocation (RequestParam)
      await RestApi.put('/app/updateParkingLocation', null, {
        params: {
          userId: String(userId),
          dong: Number(dong),
          ho: Number(ho),
          carNumber: String(selectedCarNumber),
          beaconId: String(lastBeacon.beaconId),
        },
      });

      Alert.alert(
        '확정 완료',
        `차량: ${selectedCarNumber}\nbeaconId: ${lastBeacon.beaconId}`,
      );
    } catch (e) {
      console.warn('[Home] updateParkingLocation error:', {
        message: e?.message,
        status: e?.response?.status,
        data: e?.response?.data,
      });
      Alert.alert('확정 실패', '주차 위치 저장에 실패했습니다.');
    }
  };

  // 앱 진입 시 서비스 상태 확인 + dong/ho 로드
  useEffect(() => {
    const bootstrap = async () => {
      // 1) 서비스 확인
      try {
        const running = await AndroidModule?.ServiceRunningCheck?.();
        console.log('[Home] ServiceRunningCheck:', running);

        const startAppFn =
          AndroidModule?.startApplication ?? AndroidModule?.StartApplication;

        if (!running && startAppFn) {
          await startAppFn();
        }
      } catch (e) {
        console.warn('[Home] ensureService error:', e);
      }

      // 2) dong/ho 로드
      try {
        const userData = await EncryptedStorage.getItem('user');
        if (userData) {
          const parsed = JSON.parse(userData);
          const d =
            parsed?.dong ?? parsed?.DONG ?? parsed?.Dong ?? parsed?.D ?? null;
          const h = parsed?.ho ?? parsed?.HO ?? parsed?.Ho ?? parsed?.H ?? null;
          setDong(d);
          setHo(h);
        }
      } catch (e) {
        console.warn('[Home] load user error:', e);
      }
    };

    bootstrap();

    return () => {
      if (timerRef.current) clearTimeout(timerRef.current);
      timerRef.current = null;
    };
  }, []);

  // dong/ho 준비되면 차량목록 로드
  useEffect(() => {
    if (dong == null || ho == null) return;

    const fetchCars = async () => {
      setCarsLoading(true);
      try {
        let res;

        // 1차: /app/carInfo
        try {
          res = await RestApi.get('/app/carInfo', {
            params: { dong: Number(dong), ho: Number(ho) },
          });
        } catch (err1) {
          // 2차: /carInfo
          res = await RestApi.get('/carInfo', {
            params: { dong: Number(dong), ho: Number(ho) },
          });
        }

        const list = res?.data;
        const safeList = Array.isArray(list) ? list : [];
        setCars(safeList);

        if (safeList.length === 1) setSelectedCar(safeList[0]);
      } catch (e) {
        console.warn('[Home] fetch carInfo error:', {
          message: e?.message,
          status: e?.response?.status,
          data: e?.response?.data,
        });
        Alert.alert('차량 목록 오류', '차량 목록을 불러오지 못했습니다.');
      } finally {
        setCarsLoading(false);
      }
    };

    fetchCars();
  }, [dong, ho]);

  const handleManualGateOpen = () => {
    Alert.alert('수동 문열림', '수동 문열림을 요청했어요.');
  };

  // 수동 주차위치 수집
  const handleManualParkPosition = async () => {
    if (passiveBusy) return;

    if (!selectedCarNumber) {
      Alert.alert(
        '차량 선택',
        '수동 주차위치를 수행할 차량을 먼저 선택해 주세요.',
      );
      return;
    }

    setPassiveBusy(true);

    // ✅ 이전 결과 초기화
    setLastBeacon(null);
    setDeviceLoc(null);

    try {
      Alert.alert(
        '수동 주차위치',
        '지금부터 5초간 수동 주차위치를 수집합니다.',
      );

      // ✅ 차번호 무조건 넘기기
      if (AndroidModule?.passiveParkingStart) {
        await AndroidModule.passiveParkingStart(String(selectedCarNumber));
      } else if (AndroidModule?.passiveParking) {
        AndroidModule.passiveParking(String(selectedCarNumber));
      } else {
        Alert.alert('오류', '네이티브 수동 주차위치 시작 함수가 없습니다.');
        setPassiveBusy(false);
        return;
      }

      timerRef.current = setTimeout(async () => {
        try {
          const beacon = await AndroidModule?.passiveParkingEnd?.();

          if (!beacon) {
            Alert.alert(
              '수동 주차위치 결과',
              '비콘을 찾지 못했어요. 다시 시도해 주세요.',
            );
            return;
          }

          const fixedBeacon = {
            ...beacon,
            carNumber: beacon?.carNumber ?? selectedCarNumber,
          };
          setLastBeacon(fixedBeacon);

          // ✅ beaconId로 device 조회해서 floor/x/y raw 세팅
          const beaconId = fixedBeacon?.beaconId;
          const loc = await fetchDeviceByBeaconId(beaconId);

          if (!loc) {
            Alert.alert(
              '위치 조회 실패',
              '비콘 위치(Device)를 서버에서 찾지 못했습니다.\n(그래도 확인 화면은 열어둘게요)',
            );
          }

          setConfirmOpen(true);
        } catch (e) {
          console.warn('[Home] passiveParkingEnd error:', e);
          Alert.alert('오류', '수동 주차위치 종료 중 오류가 발생했어요.');
        } finally {
          setPassiveBusy(false);
          timerRef.current = null;
        }
      }, 5000);
    } catch (e) {
      console.warn('[Home] passiveParking start error:', e);
      Alert.alert('오류', '수동 주차위치 시작 중 오류가 발생했어요.');
      setPassiveBusy(false);
    }
  };

  const openCarPicker = () => {
    if (carsLoading) return;
    if (dong == null || ho == null) {
      Alert.alert('정보 부족', 'dong/ho 정보를 확인할 수 없습니다.');
      return;
    }
    if (!cars?.length) {
      Alert.alert('차량 없음', '등록된 차량이 없습니다.');
      return;
    }
    setCarsModalOpen(true);
  };

  const renderCarItem = ({ item }) => {
    const carNo = item?.car_number ?? item?.carNumber ?? '';
    const isSelected = selectedCarNumber === carNo;

    return (
      <TouchableOpacity
        style={[styles.carItem, isSelected && styles.carItemSelected]}
        onPress={() => {
          setSelectedCar(item);
          setCarsModalOpen(false);
        }}
        activeOpacity={0.85}
      >
        <Text style={styles.carNo}>{carNo}</Text>
        <Text style={styles.carSub}>
          {item?.dong != null && item?.ho != null
            ? `동/호: ${item.dong} / ${item.ho}`
            : ''}
        </Text>
      </TouchableOpacity>
    );
  };

  return (
    <SafeScreen style={styles.container} backgroundColor="#FFFFFF">
      <View style={styles.banner}>
        <Text style={styles.bannerText}>
          현재 자동 주차위치 서비스가 동작 중이므로 자동 방식은 취소될 수
          있습니다.
        </Text>
      </View>

      <View style={styles.inner}>
        {/* 차량 선택 */}
        <View style={styles.carSelectBox}>
          <Text style={styles.carSelectLabel}>선택 차량</Text>

          <TouchableOpacity
            style={styles.carSelectBtn}
            onPress={openCarPicker}
            activeOpacity={0.85}
          >
            <Text style={styles.carSelectText}>
              {carsLoading
                ? '차량 불러오는 중...'
                : selectedCarNumber
                ? selectedCarNumber
                : '차량을 선택하세요'}
            </Text>
            {carsLoading && <ActivityIndicator style={{ marginLeft: 10 }} />}
          </TouchableOpacity>

          {!!selectedCarNumber && (
            <Text style={styles.carHint}>
              이 차량으로 수동 주차위치를 수집합니다.
            </Text>
          )}
        </View>

        <View style={styles.buttonsRow}>
          <TouchableOpacity
            style={[styles.btn, styles.btnPrimary]}
            onPress={handleManualGateOpen}
            activeOpacity={0.85}
          >
            <Text style={styles.btnPrimaryText}>수동 문열림</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[
              styles.btn,
              styles.btnOutline,
              (passiveBusy || !selectedCarNumber) && styles.btnDisabled,
            ]}
            onPress={handleManualParkPosition}
            disabled={passiveBusy || !selectedCarNumber}
            activeOpacity={0.85}
          >
            <Text style={styles.btnOutlineText}>
              {!selectedCarNumber
                ? '차량 선택 필요'
                : passiveBusy
                ? '수집중...'
                : '수동 주차위치'}
            </Text>
          </TouchableOpacity>
        </View>
      </View>

      {/* 차량 선택 모달 */}
      <Modal
        visible={carsModalOpen}
        transparent
        animationType="fade"
        onRequestClose={() => setCarsModalOpen(false)}
      >
        <View style={styles.modalDim}>
          <View style={styles.modalCard}>
            <Text style={styles.modalTitle}>차량 선택</Text>

            <FlatList
              data={cars}
              keyExtractor={(item, idx) =>
                String(item?.car_number ?? item?.carNumber ?? idx)
              }
              renderItem={renderCarItem}
              ItemSeparatorComponent={() => <View style={{ height: 8 }} />}
              contentContainerStyle={{ paddingVertical: 10 }}
            />

            <TouchableOpacity
              style={styles.modalClose}
              onPress={() => setCarsModalOpen(false)}
              activeOpacity={0.85}
            >
              <Text style={styles.modalCloseText}>닫기</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>

      {/* 결과 확인 모달 */}
      <Modal
        visible={confirmOpen}
        transparent
        animationType="fade"
        onRequestClose={() => setConfirmOpen(false)}
      >
        <View style={styles.modalDim}>
          <View style={styles.confirmCard}>
            <Text style={styles.confirmTitle}>주차 위치 확인</Text>
            <Text style={styles.confirmSub}>
              표시된 위치가 맞는지 확인해 주세요.{'\n'}(확대/축소 및 이동 가능)
            </Text>

            {deviceLocLoading ? (
              <View style={{ paddingVertical: 14 }}>
                <ActivityIndicator />
                <Text
                  style={{
                    marginTop: 8,
                    textAlign: 'center',
                    color: '#6B7280',
                  }}
                >
                  위치 정보를 불러오는 중...
                </Text>
              </View>
            ) : (
              <>
                <Text
                  style={{ fontSize: 12, color: '#374151', marginBottom: 8 }}
                >
                  beaconId: {lastBeacon?.beaconId ?? '-'} / x:
                  {String(deviceLoc?.x ?? '-')} y:
                  {String(deviceLoc?.y ?? '-')}
                </Text>

                {/* ✅ raw 좌표(deviceLoc) 그대로 넘김 */}
                <PassParkingLocation
                  selectedCar={selectedCarNumber}
                  deviceLoc={deviceLoc}
                  visible={confirmOpen}
                  focusKey={`${lastBeacon?.beaconId ?? ''}-${
                    deviceLoc?.x ?? ''
                  }-${deviceLoc?.y ?? ''}`}
                />
              </>
            )}

            <View style={styles.confirmBtnRow}>
              <TouchableOpacity
                style={[styles.confirmBtn, styles.retryBtn]}
                onPress={() => {
                  setConfirmOpen(false);
                  setLastBeacon(null);
                  setDeviceLoc(null);
                  setTimeout(() => handleManualParkPosition(), 150);
                }}
                activeOpacity={0.85}
              >
                <Text style={styles.retryText}>재시도</Text>
              </TouchableOpacity>

              <TouchableOpacity
                style={[styles.confirmBtn, styles.okBtn]}
                onPress={async () => {
                  setConfirmOpen(false); // 모달 먼저 닫고
                  await confirmParkingLocation(); // ✅ 서버 저장 호출
                }}
                activeOpacity={0.85}
              >
                <Text style={styles.okText}>확정</Text>
              </TouchableOpacity>
            </View>

            <TouchableOpacity
              style={styles.confirmCloseX}
              onPress={() => setConfirmOpen(false)}
              activeOpacity={0.85}
            >
              <Text style={{ fontSize: 18, fontWeight: '900' }}>×</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>
    </SafeScreen>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#FFFFFF' },
  banner: {
    paddingHorizontal: 16,
    paddingVertical: 12,
    backgroundColor: '#FFF5CC',
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: '#E5D58A',
  },
  bannerText: { color: '#7A5D00', fontSize: 13, lineHeight: 18 },
  inner: { flex: 1, paddingHorizontal: 20, paddingTop: 18 },

  carSelectBox: {
    borderWidth: 1,
    borderColor: '#E5E7EB',
    borderRadius: 12,
    padding: 14,
    backgroundColor: '#F9FAFB',
  },
  carSelectLabel: { fontSize: 14, fontWeight: '700', color: '#111827' },
  carSelectBtn: {
    marginTop: 10,
    height: 44,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: '#D1D5DB',
    backgroundColor: '#FFFFFF',
    paddingHorizontal: 12,
    alignItems: 'center',
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  carSelectText: { fontSize: 15, fontWeight: '700', color: '#111827' },
  carHint: { marginTop: 8, color: '#6B7280', fontSize: 12 },

  buttonsRow: { flexDirection: 'row', gap: 12, marginTop: 14 },
  btn: {
    flex: 1,
    height: 48,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
  },
  btnPrimary: { backgroundColor: '#0A84FF' },
  btnPrimaryText: { color: '#FFFFFF', fontSize: 16, fontWeight: '600' },
  btnOutline: {
    backgroundColor: '#FFFFFF',
    borderWidth: 1,
    borderColor: '#CCCCCC',
  },
  btnOutlineText: { color: '#111111', fontSize: 16, fontWeight: '600' },
  btnDisabled: { opacity: 0.55 },

  modalDim: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.35)',
    justifyContent: 'center',
    padding: 18,
  },

  modalCard: {
    backgroundColor: '#FFFFFF',
    borderRadius: 14,
    padding: 16,
    maxHeight: '75%',
  },
  modalTitle: {
    fontSize: 18,
    fontWeight: '800',
    color: '#111827',
    marginBottom: 10,
  },
  carItem: {
    borderWidth: 1,
    borderColor: '#E5E7EB',
    borderRadius: 12,
    padding: 12,
    backgroundColor: '#FFFFFF',
  },
  carItemSelected: { borderColor: '#0A84FF' },
  carNo: { fontSize: 16, fontWeight: '800', color: '#111827' },
  carSub: { marginTop: 4, fontSize: 12, color: '#6B7280' },
  modalClose: {
    marginTop: 10,
    height: 44,
    borderRadius: 10,
    backgroundColor: '#111827',
    alignItems: 'center',
    justifyContent: 'center',
  },
  modalCloseText: { color: '#FFFFFF', fontSize: 15, fontWeight: '800' },

  confirmCard: {
    backgroundColor: '#FFFFFF',
    borderRadius: 16,
    padding: 14,
  },
  confirmTitle: {
    fontSize: 18,
    fontWeight: '900',
    color: '#111827',
  },
  confirmSub: {
    marginTop: 6,
    marginBottom: 10,
    color: '#6B7280',
    fontSize: 12,
    lineHeight: 16,
  },
  confirmBtnRow: {
    flexDirection: 'row',
    gap: 10,
    marginTop: 12,
  },
  confirmBtn: {
    flex: 1,
    height: 44,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
  },
  retryBtn: { backgroundColor: '#F3F4F6' },
  okBtn: { backgroundColor: '#111827' },
  retryText: { color: '#111827', fontWeight: '800', fontSize: 15 },
  okText: { color: '#FFFFFF', fontWeight: '900', fontSize: 15 },
  confirmCloseX: {
    position: 'absolute',
    right: 10,
    top: 8,
    width: 34,
    height: 34,
    borderRadius: 17,
    alignItems: 'center',
    justifyContent: 'center',
  },
});

export default Home;
