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
  TextInput,
} from 'react-native';
import EncryptedStorage from 'react-native-encrypted-storage';
import SafeScreen from '../common/SafeScreen';
import RestApi from '../common/RestApi';
import PassParkingLocation from './PassParkingLocation';
import TestParkingLocation from './TestPassLocation';

const { AndroidModule } = NativeModules;

const TestHome = () => {
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

  // 테스트용 상태
  const [testBeaconId, setTestBeaconId] = useState('');
  const [testBusy, setTestBusy] = useState(false);
  const [testCountdown, setTestCountdown] = useState(0);
  const [testModalOpen, setTestModalOpen] = useState(false);
  const [testResult, setTestResult] = useState(null); // { estimatedX, estimatedY, actualX, actualY, actualFound }

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
              // ✅ 서버 호출 없이 OS에서 받은 좌표 바로 사용
              const resultData = {
                x: Number(resEnd.x ?? 0),
                y: Number(resEnd.y ?? 0),
                beaconX: Number(resEnd.beaconX ?? 0),  // 매칭된 비컨 좌표 (참고용)
                beaconY: Number(resEnd.beaconY ?? 0),
                beaconId: resEnd.beaconId,
                cellName: resEnd.beaconId,              // cellName 없으면 beaconId로 표시
              };

              console.log('Setting Parking Result:', JSON.stringify(resultData));
              setParkingResult(resultData);
              setConfirmModalOpen(true);
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
  // ✅ 테스트: 비콘 ID 입력 → 스캔 → 실제/추정 위치 비교
  // -----------------------------------------------------------
  const handleTestScan = useCallback(async () => {
    const trimmedId = testBeaconId.trim();
    if (!trimmedId) {
      Alert.alert('알림', '비콘 ID를 입력해주세요.');
      return;
    }
    if (testBusy) return;

    try {
      if (Platform.OS === 'android' && AndroidModule?.TestpassiveParking) {
        AndroidModule.TestpassiveParking('');
        setTestBusy(true);
        setTestCountdown(5);

        const timerInterval = setInterval(() => {
          setTestCountdown(prev => {
            if (prev <= 1) {
              clearInterval(timerInterval);
              return 0;
            }
            return prev - 1;
          });
        }, 1000);

        setTimeout(async () => {
          try {
            const res = await AndroidModule.TestpassiveParkingEnd(trimmedId);
            if (res) {
              setTestResult(res);
              setTestModalOpen(true);
            } else {
              Alert.alert('알림', '주변에 감지된 비콘이 없습니다.');
            }
          } catch (err) {
            console.error('Test Scan Error:', err);
            Alert.alert('오류', '스캔 중 오류가 발생했습니다.');
          } finally {
            setTestBusy(false);
          }
        }, 5000);
      }
    } catch (e) {
      console.error('Native Error:', e);
      setTestBusy(false);
    }
  }, [testBeaconId, testBusy]);

  // -----------------------------------------------------------
  // ✅ 2. 위치 확정
  // -----------------------------------------------------------
  const handleConfirmLocation = useCallback(async () => {
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
          userId: String(userId),
          dong: Number(dong),
          ho: Number(ho),
          carNumber: String(selectedCarNumber),
          beaconId: String(parkingResult.beaconId),
        },
      });

      if (response.status === 200 || response.status === 201) {
        Alert.alert('성공', '주차 위치가 성공적으로 등록되었습니다.');
        setConfirmModalOpen(false);
        setParkingResult(null);
      } else {
        Alert.alert('실패', '서버 응답 오류가 발생했습니다.');
      }
    } catch (e) {
      console.error('[Home] Update API Error:', e);
      Alert.alert('오류', '서버 통신 중 에러가 발생했습니다.');
    }
  }, [userId, dong, ho, selectedCarNumber, parkingResult]);

  const loadInitialData = useCallback(async () => {
    try {
      const userData = await EncryptedStorage.getItem('user');
      if (userData) {
        const parsed = JSON.parse(userData);
        setDong(parsed?.dong);
        setHo(parsed?.ho);
        setUserId(parsed?.userId || parsed?.id);
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
        <View style={styles.inner}>
          <View style={styles.carSelectBox}>
            <Text style={styles.carSelectLabel}>선택 차량</Text>
            <TouchableOpacity
              style={styles.carSelectBtn}
              onPress={() => setCarsModalOpen(true)}
              disabled={passiveBusy}
            >
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
              ) : (
                <Text style={styles.btnOutlineText}>수동 주차위치 수집</Text>
              )}
            </TouchableOpacity>
          </View>

          {/* 테스트: 비콘 위치 정확도 확인 */}
          <View style={styles.testArea}>
            <Text style={styles.sectionTitle}>위치 정확도 테스트</Text>
            <TextInput
              style={styles.beaconInput}
              placeholder="현재 서있는 비콘 ID 입력 (예: 00F5)"
              placeholderTextColor="#9CA3AF"
              value={testBeaconId}
              onChangeText={setTestBeaconId}
              autoCapitalize="characters"
              editable={!testBusy}
            />
            <TouchableOpacity
              style={[styles.btn, styles.btnTest, testBusy && styles.btnDisabled]}
              onPress={handleTestScan}
              disabled={testBusy}
            >
              {testBusy ? (
                <View style={{ flexDirection: 'row', alignItems: 'center' }}>
                  <ActivityIndicator size="small" color="#FFFFFF" style={{ marginRight: 8 }} />
                  <Text style={styles.btnTestText}>스캔 중... ({testCountdown}s)</Text>
                </View>
              ) : (
                <Text style={styles.btnTestText}>테스트 스캔 (5초)</Text>
              )}
            </TouchableOpacity>
          </View>

          <View style={styles.lobbyArea}>
            <Text style={styles.sectionTitle}>공동현관 제어</Text>
            {lobbyList.map(item => (
              <TouchableOpacity
                key={item.id}
                style={[styles.btn, styles.btnPrimary, styles.lobbyBtn]}
                onPress={() => {}}
              >
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
            <View style={styles.modalHeader}>
              <Text style={styles.modalTitle}>차량 선택</Text>
            </View>
            <FlatList
              data={cars}
              keyExtractor={(item, index) => index.toString()}
              renderItem={({ item }) => (
                <TouchableOpacity
                  style={styles.carItem}
                  onPress={() => { setSelectedCar(item); setCarsModalOpen(false); }}
                >
                  <Text style={[styles.carItemText, selectedCarNumber === (item.carNumber || item) && styles.selectedCarText]}>
                    {item.carNumber || item}
                  </Text>
                </TouchableOpacity>
              )}
            />
          </View>
        </TouchableOpacity>
      </Modal>

      {/* 테스트 결과 모달 */}
      <Modal visible={testModalOpen} animationType="fade" transparent={true} onRequestClose={() => { setTestModalOpen(false); setTestResult(null); }}>
        <View style={styles.modalFullOverlay}>
          {testResult && (
            <View style={styles.confirmModalContent}>
              <View style={styles.modalTitleRow}>
                <Text style={styles.confirmTitle}>위치 정확도 확인</Text>
                <TouchableOpacity
                  style={styles.closeBtn}
                  onPress={() => { setTestModalOpen(false); setTestResult(null); }}
                  hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
                >
                  <Text style={styles.closeBtnText}>✕</Text>
                </TouchableOpacity>
              </View>
              <View style={styles.legendRow}>
                <View style={[styles.legendDot, { backgroundColor: '#FF3B30' }]} />
                <Text style={styles.legendText}>실제 위치 (입력 비콘: {testBeaconId.trim().toUpperCase()})</Text>
              </View>
              <View style={styles.legendRow}>
                <View style={[styles.legendDot, { backgroundColor: '#007AFF' }]} />
                <Text style={styles.legendText}>추정 위치</Text>
              </View>
              {!testResult.actualFound && (
                <Text style={styles.warnText}>* CSV에서 입력한 비콘 ID를 찾을 수 없습니다.</Text>
              )}
              <View style={styles.mapContainer}>
                <TestParkingLocation
                  actualLoc={testResult.actualFound ? { x: testResult.actualX, y: testResult.actualY } : null}
                  estimatedLoc={{ x: testResult.estimatedX, y: testResult.estimatedY }}
                  visible={testModalOpen}
                />
              </View>
              <TouchableOpacity
                style={[styles.confirmBtn, styles.btnRetry, { width: '100%' }]}
                onPress={() => { setTestModalOpen(false); setTestResult(null); }}
              >
                <Text style={styles.btnRetryText}>닫기</Text>
              </TouchableOpacity>
            </View>
          )}
        </View>
      </Modal>

      {/* 위치 확인 모달 */}
      <Modal visible={confirmModalOpen} animationType="fade" transparent={true}>
        <View style={styles.modalFullOverlay}>
          {parkingResult && (
            <View style={styles.confirmModalContent}>
              <Text style={styles.confirmTitle}>주차 위치 확인</Text>
              <Text style={styles.confirmSubTitle}>인식된 위치: {parkingResult.cellName}</Text>
              <View style={styles.mapContainer}>
                <PassParkingLocation
                  deviceLoc={{ x: parkingResult.x, y: parkingResult.y }}
                  visible={confirmModalOpen}
                />
              </View>
              <View style={styles.confirmBtnRow}>
                <TouchableOpacity
                  style={[styles.confirmBtn, styles.btnRetry]}
                  onPress={() => {
                    setConfirmModalOpen(false);
                    setParkingResult(null);
                    handleManualParkPosition();
                  }}
                >
                  <Text style={styles.btnRetryText}>다시 스캔</Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={[styles.confirmBtn, styles.btnConfirm]}
                  onPress={handleConfirmLocation}
                >
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
  testArea: { marginBottom: 30, borderWidth: 1, borderColor: '#E5E7EB', borderRadius: 12, padding: 16, backgroundColor: '#F9FAFB' },
  beaconInput: { height: 48, borderWidth: 1, borderColor: '#D1D5DB', borderRadius: 10, paddingHorizontal: 14, fontSize: 15, color: '#111827', backgroundColor: '#FFF', marginBottom: 12 },
  btnTest: { backgroundColor: '#10B981' },
  btnTestText: { color: '#FFF', fontWeight: '700', fontSize: 16 },
  legendRow: { flexDirection: 'row', alignItems: 'center', marginBottom: 6, alignSelf: 'flex-start' },
  legendDot: { width: 14, height: 14, borderRadius: 7, marginRight: 8, borderWidth: 1.5, borderColor: '#fff' },
  legendText: { fontSize: 13, color: '#374151' },
  warnText: { fontSize: 12, color: '#EF4444', marginBottom: 8 },
  modalTitleRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', width: '100%', marginBottom: 10 },
  closeBtn: { padding: 4 },
  closeBtnText: { fontSize: 18, color: '#6B7280', fontWeight: '700' },
});

export default TestHome;