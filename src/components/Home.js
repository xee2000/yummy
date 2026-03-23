import React, { useEffect, useState, useMemo, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  Alert,
  Modal,
  FlatList,
  ActivityIndicator,
  ScrollView,
  RefreshControl,
  NativeModules,
  Platform,
} from 'react-native';
import EncryptedStorage from 'react-native-encrypted-storage';
import SafeScreen from '../common/SafeScreen';
import RestApi from '../common/RestApi';
import PassParkingLocation from './PassParkingLocation';

const { AndroidModule } = NativeModules;

const Home = () => {
  const [refreshing, setRefreshing] = useState(false);
  const [passiveBusy, setPassiveBusy] = useState(false);
  const [countdown, setCountdown] = useState(0);

  const [cars, setCars] = useState([]);
  const [carsLoading, setCarsLoading] = useState(false);
  const [carsModalOpen, setCarsModalOpen] = useState(false);
  const [selectedCar, setSelectedCar] = useState(null);

  const [dong, setDong] = useState(null);
  const [ho, setHo] = useState(null);
  const [userId, setUserId] = useState(null);
  const [lobbyList, setLobbyList] = useState([]);

  const [confirmModalOpen, setConfirmModalOpen] = useState(false);
  const [parkingResult, setParkingResult] = useState(null);

  const selectedCarNumber = useMemo(() => {
    if (!selectedCar) return null;
    return selectedCar.carNumber || selectedCar;
  }, [selectedCar]);

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

  const fetchCars = useCallback(async (d, h) => {
    if (!d || !h) return;
    setCarsLoading(true);
    try {
      const res = await RestApi.get('/app/carInfo', {
        params: { dong: Number(d), ho: Number(h) },
      });
      const rawData = res?.data;
      let safeList = Array.isArray(rawData) ? rawData : (rawData ? [rawData] : []);
      const finalizedList = safeList
        .filter(item => item !== null)
        .map(item => (typeof item === 'string' ? { carNumber: item } : item));

      setCars(finalizedList);
      if (finalizedList.length > 0) {
        setSelectedCar(finalizedList[0]);
      }
    } catch (e) {
      console.warn('[Home] fetch carInfo error:', e.message);
    } finally {
      setCarsLoading(false);
    }
  }, []);

  // -----------------------------------------------------------
  // ✅ 1. 수동 주차위치 수집 프로세스
  // -----------------------------------------------------------
  const handleManualParkPosition = useCallback(async () => {
    if (!selectedCarNumber) {
      Alert.alert('알림', '차량을 먼저 선택해주세요.');
      return;
    }
    if (passiveBusy) return;

    try {
      if (Platform.OS === 'android' && AndroidModule?.passiveParking) {
        await AndroidModule.passiveParking(selectedCarNumber);
        setPassiveBusy(true);
        setCountdown(5);

        const timerInterval = setInterval(() => {
          setCountdown(prev => {
            if (prev <= 1) {
              clearInterval(timerInterval);
              return 0;
            }
            return prev - 1;
          });
        }, 1000);

        setTimeout(async () => {
          try {
            const resEnd = await AndroidModule.passiveParkingEnd();
            console.log('Native resEnd:', JSON.stringify(resEnd));

            if (resEnd && resEnd.beaconId) {
              const response = await RestApi.get('/app/findBybeaconId', {
                params: { beaconId: resEnd.beaconId }
              });

              console.log('Server Data:', JSON.stringify(response.data));

              if (response.data) {
                // 💡 에러 방지를 위해 response.data 값을 직접 변수에 담아 setParkingResult 처리
                const resultData = {
                  x: Number(response.data.x || 0),
                  y: Number(response.data.y || 0),
                  floor: response.data.mapId || 'P1',
                  cellName: response.data.cellName || '미지정',
                  beaconId: response.data.beaconId || resEnd.beaconId, // 서버 데이터 우선, 없으면 스캔값 사용
                };

                console.log('Setting Parking Result:', JSON.stringify(resultData));
                setParkingResult(resultData);
                setConfirmModalOpen(true);
              }
            } else {
              Alert.alert('알림', '주변에 감지된 비콘이 없습니다.');
            }
          } catch (err) {
            console.error('Fetch Error:', err);
            Alert.alert('오류', '위치 정보를 가져오는데 실패했습니다.');
          } finally {
            setPassiveBusy(false);
          }
        }, 5000);
      }
    } catch (e) {
      console.error('Native Error:', e);
      setPassiveBusy(false);
    }
  }, [selectedCarNumber, passiveBusy]);

  // -----------------------------------------------------------
  // ✅ 2. 위치 확정 (최신 parkingResult 참조 보장)
  // -----------------------------------------------------------
  const handleConfirmLocation = useCallback(async () => {
    // 💡 로그 확인용
    console.log('Confirming for User:', userId);
    console.log('Confirming for Car:', selectedCarNumber);
    console.log('Confirming Result Data:', JSON.stringify(parkingResult));

    if (!userId || !selectedCarNumber || !parkingResult?.beaconId) {
      Alert.alert('오류', '등록할 비콘 정보가 없습니다. 다시 스캔해주세요.');
      return;
    }

    try {
const response = await RestApi.put('/app/updateParkingLocation', null, {
      params: {
        userId: String(userId),     // 반드시 문자열로 변환
        dong: Number(dong),         // 숫자로 변환
        ho: Number(ho),
        carNumber: String(selectedCarNumber),
        beaconId: String(parkingResult.beaconId),
      },
    });

      if (response.status === 200 || response.status === 201) {
        Alert.alert('성공', '주차 위치가 성공적으로 등록되었습니다.');
        setConfirmModalOpen(false);
        setParkingResult(null); // 사용 완료 후 초기화
      } else {
        Alert.alert('실패', '서버 응답 오류가 발생했습니다.');
      }
    } catch (e) {
      console.error('[Home] Update API Error:', e);
      Alert.alert('오류', '서버 통신 중 에러가 발생했습니다.');
    }
  }, [userId, dong, ho, selectedCarNumber, parkingResult]); // ✨ 의존성 배열에 parkingResult 필수

  const loadInitialData = useCallback(async () => {
    try {
      const userData = await EncryptedStorage.getItem('user');
      if (userData) {
        const parsed = JSON.parse(userData);
        setDong(parsed?.dong); setHo(parsed?.ho); setUserId(parsed?.userId || parsed?.id);
        fetchLobbyInfo(parsed?.dong, parsed?.userId || parsed?.id);
        fetchCars(parsed?.dong, parsed?.ho);
      }
    } catch (e) {
      console.warn('[Home] load error:', e);
    }
  }, [fetchLobbyInfo, fetchCars]);

  useEffect(() => {
    loadInitialData();
  }, [loadInitialData]);

  return (
    <SafeScreen style={styles.container} backgroundColor="#FFFFFF" edges="top">
      <ScrollView 
        contentContainerStyle={styles.scrollContent} 
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={loadInitialData} />}
      >
        <View style={styles.banner}><Text style={styles.bannerText}>현재 자동 주차 서비스가 동작 중입니다.</Text></View>
        <View style={styles.inner}>
          <View style={styles.carSelectBox}>
            <Text style={styles.carSelectLabel}>선택 차량</Text>
            <TouchableOpacity style={styles.carSelectBtn} onPress={() => setCarsModalOpen(true)} disabled={passiveBusy}>
              <Text style={styles.carSelectText}>{selectedCarNumber || '차량을 선택하세요'}</Text>
            </TouchableOpacity>
          </View>

          <View style={styles.centerButtonArea}>
            <TouchableOpacity
              style={[styles.btn, styles.btnOutline, (passiveBusy || !selectedCarNumber) && styles.btnDisabled]}
              onPress={handleManualParkPosition}
              disabled={passiveBusy || !selectedCarNumber}
            >
              {passiveBusy ? (
                <View style={{ flexDirection: 'row', alignItems: 'center' }}>
                  <ActivityIndicator size="small" color="#0A84FF" style={{ marginRight: 8 }} />
                  <Text style={styles.btnOutlineText}>수집 중... ({countdown}s)</Text>
                </View>
              ) : <Text style={styles.btnOutlineText}>수동 주차위치 수집</Text>}
            </TouchableOpacity>
          </View>

          <View style={styles.lobbyArea}>
            <Text style={styles.sectionTitle}>공동현관 제어</Text>
            {lobbyList.map(item => (
              <TouchableOpacity key={item.id} style={[styles.btn, styles.btnPrimary, styles.lobbyBtn]} onPress={() => {}}>
                <Text style={styles.btnPrimaryText}>{item.floor}층 {item.line}라인 문열기</Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>
      </ScrollView>

      {/* 차량 선택 모달 */}
      <Modal visible={carsModalOpen} transparent={true} animationType="slide">
        <TouchableOpacity style={styles.modalOverlay} activeOpacity={1} onPress={() => setCarsModalOpen(false)}>
          <View style={styles.modalContent}>
            <View style={styles.modalHeader}><Text style={styles.modalTitle}>차량 선택</Text></View>
            <FlatList
              data={cars}
              keyExtractor={(item, index) => index.toString()}
              renderItem={({ item }) => (
                <TouchableOpacity style={styles.carItem} onPress={() => { setSelectedCar(item); setCarsModalOpen(false); }}>
                  <Text style={[styles.carItemText, selectedCarNumber === (item.carNumber || item) && styles.selectedCarText]}>
                    {item.carNumber || item}
                  </Text>
                </TouchableOpacity>
              )}
            />
          </View>
        </TouchableOpacity>
      </Modal>

      {/* 위치 확인 모달 (parkingResult가 있을 때만 내용 표시) */}
      <Modal visible={confirmModalOpen} animationType="fade" transparent={true}>
        <View style={styles.modalFullOverlay}>
          {parkingResult && (
            <View style={styles.confirmModalContent}>
              <Text style={styles.confirmTitle}>주차 위치 확인</Text>
              <Text style={styles.confirmSubTitle}>인식된 위치: {parkingResult.cellName}</Text>
              <View style={styles.mapContainer}>
                <PassParkingLocation deviceLoc={parkingResult} visible={confirmModalOpen} />
              </View>
              <View style={styles.confirmBtnRow}>
                <TouchableOpacity style={[styles.confirmBtn, styles.btnRetry]} onPress={() => {setConfirmModalOpen(false); setParkingResult(null); handleManualParkPosition();}}>
                  <Text style={styles.btnRetryText}>다시 스캔</Text>
                </TouchableOpacity>
                <TouchableOpacity style={[styles.confirmBtn, styles.btnConfirm]} onPress={handleConfirmLocation}>
                  <Text style={styles.btnConfirmText}>위치 확정</Text>
                </TouchableOpacity>
              </View>
            </View>
          )}
        </View>
      </Modal>
    </SafeScreen>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#FFFFFF' },
  scrollContent: { flexGrow: 1, paddingBottom: 20 },
  banner: { padding: 12, backgroundColor: '#FFF5CC', alignItems: 'center' },
  bannerText: { color: '#7A5D00', fontSize: 13, fontWeight: '500' },
  inner: { flex: 1, padding: 20 },
  carSelectBox: { borderWidth: 1, borderColor: '#E5E7EB', borderRadius: 12, padding: 14, backgroundColor: '#F9FAFB', marginBottom: 15 },
  carSelectLabel: { fontSize: 14, fontWeight: '700', color: '#374151' },
  carSelectBtn: { marginTop: 10, height: 48, borderRadius: 10, borderWidth: 1, borderColor: '#D1D5DB', backgroundColor: '#FFFFFF', paddingHorizontal: 12, justifyContent: 'center' },
  carSelectText: { fontSize: 16, fontWeight: '700', color: '#111827' },
  centerButtonArea: { marginBottom: 35, alignItems: 'center' },
  lobbyArea: { marginTop: 10 },
  sectionTitle: { fontSize: 18, fontWeight: '800', marginBottom: 15, color: '#111' },
  btn: { height: 54, borderRadius: 12, alignItems: 'center', justifyContent: 'center', width: '100%' },
  btnPrimary: { backgroundColor: '#0A84FF' },
  btnPrimaryText: { color: '#FFFFFF', fontWeight: '700', fontSize: 16 },
  btnOutline: { borderWidth: 1.5, borderColor: '#0A84FF', backgroundColor: '#FFFFFF' },
  btnOutlineText: { color: '#0A84FF', fontWeight: '700', fontSize: 16 },
  btnDisabled: { opacity: 0.5, borderColor: '#D1D5DB' },
  lobbyBtn: { marginBottom: 12 },
  modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.4)', justifyContent: 'flex-end' },
  modalContent: { backgroundColor: '#FFFFFF', borderTopLeftRadius: 20, borderTopRightRadius: 20, paddingBottom: 40, maxHeight: '50%' },
  modalHeader: { padding: 20, borderBottomWidth: 1, borderBottomColor: '#F3F4F6', alignItems: 'center' },
  modalTitle: { fontSize: 18, fontWeight: '800' },
  carItem: { padding: 20, borderBottomWidth: 1, borderBottomColor: '#F3F4F6' },
  carItemText: { fontSize: 16, color: '#333' },
  selectedCarText: { color: '#0A84FF', fontWeight: '800' },
  modalFullOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.6)', justifyContent: 'center', alignItems: 'center' },
  confirmModalContent: { width: '92%', backgroundColor: '#FFF', borderRadius: 20, padding: 20, alignItems: 'center' },
  confirmTitle: { fontSize: 20, fontWeight: '900', color: '#333', marginBottom: 5 },
  confirmSubTitle: { fontSize: 15, color: '#666', marginBottom: 15 },
  mapContainer: { width: '100%', height: 300, marginBottom: 20 },
  confirmBtnRow: { flexDirection: 'row', justifyContent: 'space-between', width: '100%' },
  confirmBtn: { flex: 1, height: 50, borderRadius: 10, justifyContent: 'center', alignItems: 'center', marginHorizontal: 5 },
  btnRetry: { backgroundColor: '#F3F4F6', borderWidth: 1, borderColor: '#D1D5DB' },
  btnRetryText: { color: '#4B5563', fontWeight: '700' },
  btnConfirm: { backgroundColor: '#0A84FF' },
  btnConfirmText: { color: '#FFF', fontWeight: '700' },
});

export default Home;