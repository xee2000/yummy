import React, {
  useEffect,
  useRef,
  useState,
  useMemo,
  useCallback,
} from 'react';
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
  ScrollView,
  RefreshControl,
  Platform,
} from 'react-native';
import EncryptedStorage from 'react-native-encrypted-storage';
import SafeScreen from '../common/SafeScreen';
import RestApi from '../common/RestApi';
import PassParkingLocation from './PassParkingLocation';

const { AndroidModule } = NativeModules;

const Home = () => {
  const timerRef = useRef(null);

  const [refreshing, setRefreshing] = useState(false);
  const [passiveBusy, setPassiveBusy] = useState(false);

  // 데이터 상태
  const [cars, setCars] = useState([]);
  const [carsLoading, setCarsLoading] = useState(false);
  const [carsModalOpen, setCarsModalOpen] = useState(false);
  const [selectedCar, setSelectedCar] = useState(null);

  const [dong, setDong] = useState(null);
  const [ho, setHo] = useState(null);
  const [userId, setUserId] = useState(null);

  // ✅ 로비(공동현관) 리스트 데이터
  const [lobbyList, setLobbyList] = useState([]);

  // 주차 위치 관련
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [lastBeacon, setLastBeacon] = useState(null);
  const [deviceLocLoading, setDeviceLocLoading] = useState(false);
  const [deviceLoc, setDeviceLoc] = useState(null);

  const selectedCarNumber = useMemo(() => {
    if (!selectedCar) return null;
    return typeof selectedCar === 'object'
      ? selectedCar.carNumber
      : selectedCar;
  }, [selectedCar]);

  // -----------------------------
  // ✅ 1. 로비 정보 조회 (홈 진입 시 호출)
  // -----------------------------
  const fetchLobbyInfo = useCallback(async (targetDong, targetUserId) => {
    if (!targetDong || !targetUserId) return;
    try {
      const res = await RestApi.post('/app/openLobby/findByDong', null, {
        params: { dong: Number(targetDong), userId: String(targetUserId) },
      });
      setLobbyList(Array.isArray(res?.data) ? res.data : []);
    } catch (e) {
      console.warn('[Lobby] fetch error:', e.message);
    }
  }, []);

  // -----------------------------
  // ✅ 2. 차량 정보 조회 (홈 진입 시 호출)
  // -----------------------------
  const fetchCars = useCallback(async (d, h) => {
    if (!d || !h) return;
    setCarsLoading(true);
    try {
      const res = await RestApi.get('/app/carInfo', {
        params: { dong: Number(d), ho: Number(h) },
      });
      const safeList = Array.isArray(res?.data)
        ? res.data.map(item =>
            typeof item === 'string' ? { carNumber: item } : item,
          )
        : [];
      setCars(safeList);
      if (safeList.length === 1) setSelectedCar(safeList[0]);
    } catch (e) {
      console.warn('[Home] fetch carInfo error:', e.message);
    } finally {
      setCarsLoading(false);
    }
  }, []);

  // -----------------------------
  // ✅ 3. 수동 문열림 실행 (ModelAttribute 전송)
  // -----------------------------
  const handleOpenLobby = async item => {
    try {
      // 1. 데이터 준비 (Key 이름을 Retrofit @Field와 정확히 일치시켜야 함)
      const details = new URLSearchParams();
      details.append('id', String(userId)); // 서버 @Field("id")
      details.append('dong', String(dong)); // 서버 @Field("dong")
      details.append('ho', String(ho)); // 서버 @Field("ho")
      details.append('minor', String(item.minorNumber)); // 서버 @Field("minor")
      details.append('rssi', '-70'); // 서버 @Field("rssi")

      console.log('[Lobby] 전송 데이터:', details.toString());

      // 2. API 호출
      // ✅ 포인트 1: details.toString()으로 문자열화해서 보냄
      // ✅ 포인트 2: config 위치를 확인 (url, data, config)
      const res = await RestApi.post(
        '/app/pass/openLobby',
        details.toString(),
        {
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            Accept: 'application/json',
          },
        },
      );

      // 3. 응답 처리
      if (res.status === 200 || res.status === 201) {
        Alert.alert(
          '성공',
          `${item.floor}층 ${item.line}라인 문이 열렸습니다.`,
        );
      } else {
        console.log('[Lobby] 서버 응답 상태:', res.status);
      }
    } catch (e) {
      console.warn('[Lobby] 요청 에러:', e.response?.data || e.message);
      Alert.alert('실패', '문열림 요청 중 오류가 발생했습니다.');
    }
  };

  // -----------------------------
  // ✅ 4. 초기 데이터 로드 로직
  // -----------------------------
  const loadInitialData = useCallback(async () => {
    try {
      const userData = await EncryptedStorage.getItem('user');
      if (userData) {
        const parsed = JSON.parse(userData);
        const d = parsed?.dong;
        const h = parsed?.ho;
        const uid = parsed?.userId || parsed?.id;

        setDong(d);
        setHo(h);
        setUserId(uid);

        // ✅ 홈 들어오는 순간 모든 API 호출
        fetchLobbyInfo(d, uid);
        fetchCars(d, h);
      }
    } catch (e) {
      console.warn('[Home] loadInitialData error:', e);
    }
  }, [fetchLobbyInfo, fetchCars]);

  useEffect(() => {
    loadInitialData();
  }, [loadInitialData]);

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    await loadInitialData();
    setRefreshing(false);
  }, [loadInitialData]);

  // ... (주차 위치 관련 기존 함수들 유지)

  return (
    <SafeScreen style={styles.container} backgroundColor="#FFFFFF">
      <ScrollView
        contentContainerStyle={{ flexGrow: 1 }}
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
        }
      >
        <View style={styles.banner}>
          <Text style={styles.bannerText}>
            현재 자동 주차 서비스가 동작 중입니다.
          </Text>
        </View>

        <View style={styles.inner}>
          {/* 차량 선택 영역 */}
          <View style={styles.carSelectBox}>
            <Text style={styles.carSelectLabel}>선택 차량</Text>
            <TouchableOpacity
              style={styles.carSelectBtn}
              onPress={() => setCarsModalOpen(true)}
            >
              <Text style={styles.carSelectText}>
                {carsLoading
                  ? '로딩 중...'
                  : selectedCarNumber || '차량을 선택하세요'}
              </Text>
            </TouchableOpacity>
          </View>

          {/* <View style={styles.centerButtonArea}> */}
          {/* <TouchableOpacity */}
          {/* style={[ */}
          {/* styles.btn, */}
          {/* styles.btnOutline, */}
          {/* (passiveBusy || !selectedCarNumber) && styles.btnDisabled, */}
          {/* ]} */}
          {/* onPress={() => { */}
          {/* /* 기존 handleManualParkPosition 호출 */}
          {/* }} */}
          {/* disabled={passiveBusy || !selectedCarNumber} */}
          {/* > */}
          {/* <Text style={styles.btnOutlineText}> */}
          {/* {passiveBusy ? '수집중...' : '수동 주차위치'} */}
          {/* </Text> */}
          {/* </TouchableOpacity> */}
          {/* </View> */}

          {/* 2. 동적 로비 버튼 영역 (아래로 배치) */}
          <View style={styles.lobbyArea}>
            <Text style={styles.sectionTitle}>공동현관 제어</Text>
            {lobbyList.length === 0 ? (
              <Text style={styles.emptyText}>로비 정보가 없습니다.</Text>
            ) : (
              lobbyList.map(item => (
                <TouchableOpacity
                  key={item.id}
                  style={[styles.btn, styles.btnPrimary, styles.lobbyBtn]}
                  onPress={() => handleOpenLobby(item)}
                >
                  <Text style={styles.btnPrimaryText}>
                    {item.floor}층 {item.line}라인 수동 문열기
                  </Text>
                </TouchableOpacity>
              ))
            )}
          </View>
        </View>
      </ScrollView>

      {/* 차량 선택 모달 등 기존 모달 유지... */}
    </SafeScreen>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#FFFFFF' },
  banner: { padding: 12, backgroundColor: '#FFF5CC' },
  bannerText: { color: '#7A5D00', fontSize: 13 },
  inner: { flex: 1, padding: 20 },
  carSelectBox: {
    borderWidth: 1,
    borderColor: '#E5E7EB',
    borderRadius: 12,
    padding: 14,
    backgroundColor: '#F9FAFB',
    marginBottom: 20,
  },
  carSelectLabel: { fontSize: 14, fontWeight: '700' },
  carSelectBtn: {
    marginTop: 10,
    height: 44,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: '#D1D5DB',
    backgroundColor: '#FFFFFF',
    paddingHorizontal: 12,
    justifyContent: 'center',
  },
  carSelectText: { fontSize: 15, fontWeight: '700' },

  centerButtonArea: { alignItems: 'center', marginBottom: 30 },
  lobbyArea: { marginTop: 10 },
  sectionTitle: {
    fontSize: 16,
    fontWeight: '800',
    marginBottom: 15,
    color: '#333',
  },
  emptyText: { textAlign: 'center', color: '#999', marginTop: 20 },

  btn: {
    height: 52,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
    width: '100%',
  },
  btnPrimary: { backgroundColor: '#0A84FF' },
  btnPrimaryText: { color: '#FFFFFF', fontWeight: '700', fontSize: 15 },
  btnOutline: { borderWidth: 1, borderColor: '#0A84FF' },
  btnOutlineText: { color: '#0A84FF', fontWeight: '700' },
  btnDisabled: { opacity: 0.5 },
  lobbyBtn: { marginBottom: 12 }, // 버튼 사이 간격
  carNo: { fontSize: 16, fontWeight: '600' },
});

export default Home;
