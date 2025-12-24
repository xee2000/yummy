// src/components/Parking.js
import React, { useCallback, useEffect, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  ActivityIndicator,
  Alert,
  RefreshControl,
} from 'react-native';
import SafeScreen from '../common/SafeScreen';
import ParkingLocation from './ParkingLocation';
import EncryptedStorage from 'react-native-encrypted-storage';
import RestApi from '../common/RestApi';
import { useFocusEffect } from '@react-navigation/native';

// ✅ ISO -> KST 포맷(안깨지게)
// - Intl 있으면 ko-KR + Asia/Seoul
// - 없으면 수동으로 +9h 처리
const formatKST = iso => {
  if (!iso) return '-';
  try {
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) return '-';

    if (typeof Intl !== 'undefined' && Intl.DateTimeFormat) {
      return new Intl.DateTimeFormat('ko-KR', {
        timeZone: 'Asia/Seoul',
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false,
      }).format(d);
    }

    // fallback: UTC 기준 +9h
    const kst = new Date(d.getTime() + 9 * 60 * 60 * 1000);
    const pad = n => String(n).padStart(2, '0');
    return `${kst.getUTCFullYear()}-${pad(kst.getUTCMonth() + 1)}-${pad(
      kst.getUTCDate(),
    )} ${pad(kst.getUTCHours())}:${pad(kst.getUTCMinutes())}:${pad(
      kst.getUTCSeconds(),
    )}`;
  } catch {
    return '-';
  }
};

const floorText = mapId => (mapId ? `${mapId}층` : '-');
const pillarText = area => (area ? `${area} 기둥` : '-');

const Parking = () => {
  const [dong, setDong] = useState(null);
  const [ho, setHo] = useState(null);

  const [list, setList] = useState([]); // ParkingResult[]
  const [loading, setLoading] = useState(false);
  const [refreshing, setRefreshing] = useState(false);

  const loadDongHo = useCallback(async () => {
    try {
      const userData = await EncryptedStorage.getItem('user');
      if (!userData) return;

      const parsed = JSON.parse(userData);
      const d =
        parsed?.dong ?? parsed?.DONG ?? parsed?.Dong ?? parsed?.D ?? null;
      const h = parsed?.ho ?? parsed?.HO ?? parsed?.Ho ?? parsed?.H ?? null;

      setDong(d);
      setHo(h);
    } catch (e) {
      console.warn('[Parking] loadDongHo error:', e);
    }
  }, []);

  const fetchParkingList = useCallback(
    async (opts = { silent: false }) => {
      if (dong == null || ho == null) return;

      if (!opts.silent) setLoading(true);

      try {
        const res = await RestApi.get('/app/findbyParkingLocationList', {
          params: { dong: Number(dong), ho: Number(ho) },
        });

        const data = res?.data ?? {};
        console.log('[Parking] data:', JSON.stringify(data));

        const rows = Array.isArray(data?.result) ? data.result : [];

        const safe = rows
          .filter(r => r?.carNumber)
          .map(r => ({
            carNumber: r.carNumber,
            x: r.x, // String
            y: r.y, // String
            mapId: r.mapId, // e.g. "B3"
            area: r.area, // e.g. "A01"
            lastParkingTime: r.lastParkingTime, // ISO string (UTC)
          }));

        setList(safe);
      } catch (e) {
        console.warn('[Parking] fetchParkingList error:', {
          message: e?.message,
          status: e?.response?.status,
          data: e?.response?.data,
        });
        Alert.alert('오류', '주차 위치 목록을 불러오지 못했습니다.');
      } finally {
        if (!opts.silent) setLoading(false);
      }
    },
    [dong, ho],
  );

  // 1) 최초 dong/ho 로드
  useEffect(() => {
    loadDongHo();
  }, [loadDongHo]);

  // 2) dong/ho 준비되면 최초 1회 호출
  useEffect(() => {
    if (dong == null || ho == null) return;
    fetchParkingList();
  }, [dong, ho, fetchParkingList]);

  // 3) ✅ 화면 진입/재진입(포커스) 할 때마다 호출
  useFocusEffect(
    useCallback(() => {
      if (dong == null || ho == null) {
        loadDongHo();
        return;
      }
      fetchParkingList({ silent: true });
    }, [dong, ho, loadDongHo, fetchParkingList]),
  );

  const onRefresh = async () => {
    setRefreshing(true);
    await fetchParkingList({ silent: true });
    setRefreshing(false);
  };

  const renderItem = ({ item }) => {
    const deviceLoc = {
      floor: item.mapId ?? null,
      x: item.x != null ? Number(item.x) : null,
      y: item.y != null ? Number(item.y) : null,
      area: item.area ?? null,
      lastParkingTime: item.lastParkingTime ?? null,
    };

    // ✅ 훅 쓰지 말고 그냥 계산
    const timeKST = formatKST(item.lastParkingTime);

    return (
      <View style={styles.itemWrapper}>
        <Text style={styles.carName}>{item.carNumber}</Text>

        <Text style={styles.metaText}>
          위치: {floorText(item.mapId)} / {pillarText(item.area)}
        </Text>

        <Text style={styles.timeText}>마지막 주차: {timeKST}</Text>

        <ParkingLocation selectedCar={item.carNumber} deviceLoc={deviceLoc} />
      </View>
    );
  };

  return (
    <SafeScreen style={styles.container} backgroundColor="#FFFFFF">
      <Text style={styles.title}>차량별 주차 위치</Text>

      {loading && (
        <View style={{ paddingVertical: 10 }}>
          <ActivityIndicator />
        </View>
      )}

      <FlatList
        data={list}
        keyExtractor={(item, idx) => `${item.carNumber}-${idx}`}
        showsVerticalScrollIndicator={false}
        renderItem={renderItem}
        contentContainerStyle={styles.listContent}
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
        }
        ListEmptyComponent={
          !loading ? (
            <Text style={styles.emptyText}>표시할 주차 위치가 없습니다.</Text>
          ) : null
        }
      />
    </SafeScreen>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#FFF' },
  title: {
    fontSize: 20,
    fontWeight: '700',
    color: '#111',
    textAlign: 'center',
    marginVertical: 20,
  },
  listContent: { paddingBottom: 50 },
  itemWrapper: { marginBottom: 40, alignItems: 'center' },
  carName: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333',
    marginBottom: 6,
  },
  metaText: {
    fontSize: 12,
    color: '#374151',
    marginBottom: 4,
  },
  timeText: {
    fontSize: 12,
    color: '#6B7280',
    marginBottom: 10,
  },
  emptyText: {
    textAlign: 'center',
    color: '#6B7280',
    marginTop: 40,
  },
});

export default Parking;
