import React, { useCallback, useEffect, useState, useRef, useMemo } from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  ActivityIndicator,
  Dimensions,
  TouchableOpacity,
  ScrollView,
  Image,
} from 'react-native';
import ImageZoom from 'react-native-image-pan-zoom';
import SafeScreen from '../common/SafeScreen';
import EncryptedStorage from 'react-native-encrypted-storage';
import RestApi from '../common/RestApi';
import { useFocusEffect } from '@react-navigation/native';
import RefreshContainer from '../common/RefreshContainer'; // ✨ 유틸 추가

const { width: SCREEN_WIDTH } = Dimensions.get('window');

const MARKER_SIZE = 30;
const X_SCALE = 4.9125;
const Y_SCALE = 4.775;
const ORIGIN_W = 1572;
const ORIGIN_H = 1146;

const MAP_IMAGES = {
  P1: require('../assets/P1.png'),
  P2_E: require('../assets/P2.png'),
  P2_W: require('../assets/P5.png'),
  P3_G: require('../assets/P3.png'),
  P3_B: require('../assets/P4.png'),
  default: require('../assets/P1.png'),
};

const InternalParkingMap = ({ deviceLoc, onInteractingChange }) => {
  const zoomRef = useRef(null);
  const [layoutSize, setLayoutSize] = useState({ width: 0, height: 0 });

  const bgImage = useMemo(() => {
    if (!deviceLoc?.floor) return MAP_IMAGES.default;
    return MAP_IMAGES[deviceLoc.floor] || MAP_IMAGES.default;
  }, [deviceLoc?.floor]);

  const onLayout = (e) => {
    const { width, height } = e.nativeEvent.layout;
    setLayoutSize({ width, height });
  };

  const calculateInfo = useMemo(() => {
    const { width, height } = layoutSize;
    if (width <= 0 || height <= 0 || !deviceLoc) return null;
    const rawX = deviceLoc.x != null ? Number(deviceLoc.x) : 0;
    const rawY = deviceLoc.y != null ? Number(deviceLoc.y) : 0;
    const scaledX = rawX * X_SCALE;
    const scaledY = rawY * Y_SCALE;
    const percentWidth = width / ORIGIN_W;
    const percentHeight = height / ORIGIN_H;
    const markerCx = scaledX * percentWidth;
    const markerCy = scaledY * percentHeight;
    return {
      styleLeft: markerCx - MARKER_SIZE / 2,
      styleTop: markerCy - MARKER_SIZE,
      targetX: markerCx,
      targetY: markerCy,
    };
  }, [deviceLoc, layoutSize]);

  useEffect(() => {
    if (calculateInfo && zoomRef.current && layoutSize.width > 0) {
      const timer = setTimeout(() => {
        const offsetX = layoutSize.width / 2 - calculateInfo.targetX;
        const offsetY = layoutSize.height / 2 - calculateInfo.targetY;
        zoomRef.current.centerOn({
          x: offsetX,
          y: offsetY,
          scale: 2.5,
          duration: 400,
        });
      }, 200);
      return () => clearTimeout(timer);
    }
  }, [calculateInfo]);

  return (
    <View style={styles.mapInnerContainer} onLayout={onLayout}>
      {layoutSize.width > 0 && calculateInfo && (
        <ImageZoom
          ref={zoomRef}
          cropWidth={layoutSize.width}
          cropHeight={layoutSize.height}
          imageWidth={layoutSize.width}
          imageHeight={layoutSize.height}
          minScale={1}
          maxScale={5}
          enableCenterFocus={false}
          panToMove={true}
          onMove={() => onInteractingChange?.(true)}
        >
          <View style={{ width: layoutSize.width, height: layoutSize.height }}>
            <Image source={bgImage} style={styles.mapImage} resizeMode="stretch" />
            <Image
              source={require('../assets/parking.png')}
              style={[
                styles.markerImg,
                { left: calculateInfo.styleLeft, top: calculateInfo.styleTop },
              ]}
            />
          </View>
        </ImageZoom>
      )}
    </View>
  );
};

const Parking = () => {
  const [dong, setDong] = useState(null);
  const [ho, setHo] = useState(null);
  const [list, setList] = useState([]);
  const [loading, setLoading] = useState(false);
  const [refreshEnabled, setRefreshEnabled] = useState(true);
  const [currentIndex, setCurrentIndex] = useState(0);

  const flatListRef = useRef(null);

  const fetchParkingList = useCallback(async (opts = { silent: false }) => {
    const userData = await EncryptedStorage.getItem('user');
    if (!userData) return;
    const parsed = JSON.parse(userData);
    const d = parsed?.dong ?? parsed?.DONG;
    const h = parsed?.ho ?? parsed?.HO;
    
    setDong(d);
    setHo(h);

    if (d == null || h == null) return;
    if (!opts.silent) setLoading(true);

    try {
      const res = await RestApi.get('/app/findbyParkingLocationList', {
        params: { dong: Number(d), ho: Number(h) },
      });
      const data = res?.data ?? [];
      const rows = Array.isArray(data) ? data : (data?.result ?? []);
      setList(rows.filter(r => r?.carNumber));
    } catch (e) {
      console.error('[Parking] Fetch Error:', e);
    } finally {
      if (!opts.silent) setLoading(false);
    }
  }, []);

  useFocusEffect(
    useCallback(() => {
      fetchParkingList({ silent: true });
    }, [fetchParkingList])
  );

  const scrollToVehicle = (index) => {
    flatListRef.current?.scrollToIndex({ index, animated: true });
    setCurrentIndex(index);
  };

  const renderItem = ({ item }) => {
    const deviceLoc = {
      floor: item.mapId ?? null,
      x: item.x != null ? Number(item.x) : null,
      y: item.y != null ? Number(item.y) : null,
    };

    return (
      <View style={styles.slideItem}>
        <View style={styles.card}>
          <View style={styles.compactHeader}>
            <Text style={styles.carNumber}>{item.carNumber}</Text>
            <View style={styles.infoRow}>
              <View style={styles.labelGroup}>
                <Text style={styles.labelText}>구역</Text>
                <Text style={styles.valueText}>{item.mapImageName || '-'}</Text>
              </View>
              <View style={styles.verticalDivider} />
              <View style={styles.labelGroup}>
                <Text style={styles.labelText}>위치</Text>
                <Text style={[styles.valueText, styles.orangeText]}>{item.cellName || '-'}</Text>
              </View>
            </View>
          </View>

          <View style={styles.mapWrapper}>
            <InternalParkingMap
              deviceLoc={deviceLoc}
              onInteractingChange={isInteracting => setRefreshEnabled(!isInteracting)}
            />
          </View>
          <Text style={styles.zoomGuide}>지도를 벌려서 상세 위치를 확인하세요</Text>
        </View>
      </View>
    );
  };

  return (
    <SafeScreen style={styles.container} backgroundColor="#F2F5F8" edges="top">
      {/* ✨ 전체 영역에 새로고침 유틸 적용 */}
      <RefreshContainer onRefresh={() => fetchParkingList({ silent: true })}>
        <View style={styles.topBar}>
          <Text style={styles.topBarTitle}>주차 위치 찾기</Text>
          <Text style={styles.topBarInfo}>{dong}동 {ho}호</Text>
        </View>

        <View style={styles.carTabContainer}>
          <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.carTabScroll}>
            {list.map((car, index) => (
              <TouchableOpacity 
                key={car.carNumber} 
                onPress={() => scrollToVehicle(index)}
                style={[styles.carTab, currentIndex === index && styles.activeCarTab]}
              >
                <Text style={[styles.carTabText, currentIndex === index && styles.activeCarTabText]}>
                  {car.carNumber}
                </Text>
              </TouchableOpacity>
            ))}
          </ScrollView>
        </View>

        <View style={styles.listWrapper}>
          <FlatList
            ref={flatListRef}
            data={list}
            keyExtractor={(item) => item.carNumber}
            renderItem={renderItem}
            horizontal
            pagingEnabled
            scrollEnabled={refreshEnabled} // 지도 조작 중엔 슬라이드 방지
            showsHorizontalScrollIndicator={false}
            onMomentumScrollEnd={(e) => {
              const newIndex = Math.round(e.nativeEvent.contentOffset.x / SCREEN_WIDTH);
              setCurrentIndex(newIndex);
            }}
            ListEmptyComponent={
              loading ? <ActivityIndicator style={{marginTop: 50}} /> : <View style={styles.emptyContainer}><Text style={styles.emptyText}>주차 정보가 없습니다.</Text></View>
            }
          />
        </View>
      </RefreshContainer>
    </SafeScreen>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#F2F5F8' },
  topBar: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 20, paddingVertical: 12, backgroundColor: '#FFF' },
  topBarTitle: { fontSize: 17, fontWeight: '800', color: '#1A1A1A' },
  topBarInfo: { fontSize: 13, color: '#007AFF', fontWeight: '700' },
  carTabContainer: { backgroundColor: '#FFF', paddingVertical: 10, borderBottomWidth: 1, borderBottomColor: '#F0F0F0' },
  carTabScroll: { paddingHorizontal: 15 },
  carTab: { paddingHorizontal: 15, paddingVertical: 6, borderRadius: 20, backgroundColor: '#F0F4F8', marginRight: 10, borderWidth: 1, borderColor: '#E0E6ED' },
  activeCarTab: { backgroundColor: '#007AFF', borderColor: '#007AFF' },
  carTabText: { fontSize: 13, fontWeight: '600', color: '#666' },
  activeCarTabText: { color: '#FFF' },
  listWrapper: { flex: 1, height: 500 }, // 슬라이드 영역 높이 확보
  slideItem: { width: SCREEN_WIDTH, height: '100%', paddingHorizontal: 10, paddingTop: 10, paddingBottom: 10 },
  card: { flex: 1, backgroundColor: '#FFF', borderRadius: 24, padding: 15, elevation: 4, shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.1, shadowRadius: 10 },
  compactHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 },
  carNumber: { fontSize: 18, fontWeight: '800', color: '#333' },
  infoRow: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#F8FAFC', paddingHorizontal: 10, paddingVertical: 6, borderRadius: 8 },
  labelGroup: { flexDirection: 'row', alignItems: 'baseline' },
  labelText: { fontSize: 11, color: '#718096', fontWeight: '600', marginRight: 4 },
  valueText: { fontSize: 14, fontWeight: '800', color: '#1A202C' },
  orangeText: { color: '#E67E22' },
  verticalDivider: { width: 1, height: 12, backgroundColor: '#CBD5E0', marginHorizontal: 10 },
  mapWrapper: { flex: 1, borderRadius: 14, overflow: 'hidden', borderWidth: 1, borderColor: '#E2E8F0', backgroundColor: '#FFF', marginVertical: 8 },
  mapInnerContainer: { width: '100%', height: '100%' },
  mapImage: { width: '100%', height: '100%', position: 'absolute' },
  markerImg: { position: 'absolute', width: MARKER_SIZE, height: MARKER_SIZE, zIndex: 999 },
  zoomGuide: { textAlign: 'center', fontSize: 11, color: '#A0AEC0', marginTop: 4, fontWeight: '500', marginBottom: 5 },
  emptyContainer: { width: SCREEN_WIDTH, height: 200, justifyContent: 'center', alignItems: 'center' },
  emptyText: { color: '#999', fontSize: 15 },
});

export default Parking;