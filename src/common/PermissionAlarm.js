// src/screens/PermissionAlarm.js
import React from 'react';
import { View, Text, StyleSheet, ScrollView, Pressable } from 'react-native';
import Icon from 'react-native-vector-icons/Ionicons';
import {
  widthPercentageToDP as wp,
  heightPercentageToDP as hp,
} from 'react-native-responsive-screen';

const COLORS = {
  surface: '#FFFFFF',
  text: '#0A0A0A',
  sub: '#2E3A46',
  brand: '#1B5FFF',
};

const CTA_HEIGHT = 56;

const PERMISSION_ITEMS = [
  {
    code: 'POST_NOTIFICATIONS',
    icon: 'notifications-outline',
    title: '알림 권한',
    desc: '앱의 주요 안내 및 상태 알림을 받을 수 있어요.',
  },
  {
    code: 'LOCATION',
    icon: 'location-outline',
    title: '위치 권한',
    desc: '주차 위치 확인, 배경 위치 기반 기능을 제공하기 위해 필요해요.',
  },
  {
    code: 'BLUETOOTH',
    icon: 'bluetooth-outline',
    title: '블루투스 권한',
    desc: '주변 기기를 감지하고 스마트 연동 기능을 사용하기 위해 필요해요.',
  },
];
/** 팝업 카드(배경 반투명). 부모에서 <Modal transparent>로 감싸 사용.
 * props:
 * - onConfirm(): "확인했어요" 눌렀을 때 (권한요청 트리거)
 * - onClose(): 바깥/닫기버튼으로 닫기
 */
export default function PermissionAlarm({ onConfirm, onClose }) {
  return (
    <View style={styles.overlay} pointerEvents="box-none">
      <Pressable style={StyleSheet.absoluteFill} onPress={onClose} />

      <View style={styles.card}>
        {/* 헤더 */}
        <View style={styles.header}>
          <Text style={styles.title}>앱 이용을 위한 권한 안내</Text>
          <Pressable hitSlop={12} onPress={onClose} style={styles.closeBtn}>
            <Icon name="close" size={20} color={COLORS.sub} />
          </Pressable>
        </View>

        <Text style={styles.helper}>
          아래 권한은 서비스 제공을 위해 꼭 필요해요. 자세한 안내를 확인한 뒤
          다음 단계에서 권한을 허용해 주세요.
        </Text>

        {/* 본문(카드 내부 스크롤) */}
        <ScrollView
          style={styles.scroll}
          contentContainerStyle={{ paddingBottom: 12 }}
          showsVerticalScrollIndicator={false}
        >
          {PERMISSION_ITEMS.map(item => (
            <View key={item.code} style={styles.permRow}>
              <View style={styles.permIconWrap}>
                <Icon name={item.icon} size={hp('2.6%')} color={COLORS.brand} />
              </View>
              <View style={{ flex: 1 }}>
                <Text style={styles.permTitle}>{item.title}</Text>
                <Text style={styles.permDesc}>{item.desc}</Text>
              </View>
            </View>
          ))}
        </ScrollView>

        {/* 하단 버튼 */}
        <Pressable
          onPress={onConfirm}
          style={({ pressed }) => [
            styles.cta,
            pressed && { opacity: 0.95, transform: [{ scale: 0.998 }] },
          ]}
          accessibilityRole="button"
          accessibilityLabel="권한 안내 확인"
          hitSlop={8}
        >
          <Icon
            name="checkmark-circle-outline"
            size={18}
            color="#FFF"
            style={{ marginRight: 8 }}
          />
          <Text style={styles.ctaText}>확인했어요</Text>
        </Pressable>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  overlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(0,0,0,0.35)',
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 18,
  },
  card: {
    width: '100%',
    maxWidth: 480,
    maxHeight: '78%',
    backgroundColor: COLORS.surface,
    borderRadius: 18,
    paddingHorizontal: 16,
    paddingTop: 14,
    paddingBottom: 14,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: '#E5E7EB',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 10 },
    shadowOpacity: 0.12,
    shadowRadius: 20,
    elevation: 8,
  },
  header: {
    paddingRight: 28,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 6,
  },
  title: { fontSize: hp('2.6%'), fontWeight: '900', color: COLORS.text },
  closeBtn: { position: 'absolute', right: 0, top: 2 },
  helper: {
    fontSize: hp('1.9%'),
    color: COLORS.sub,
    marginBottom: 8,
    textAlign: 'center',
  },
  scroll: { flexGrow: 0, marginTop: 6, marginBottom: 10 },
  permRow: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    paddingVertical: 9,
    gap: 10,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: '#E5E7EB',
  },
  permIconWrap: {
    width: 36,
    height: 36,
    borderRadius: 10,
    backgroundColor: '#EEF3FF',
    alignItems: 'center',
    justifyContent: 'center',
  },
  permTitle: { fontSize: hp('2.0%'), fontWeight: '800', color: COLORS.text },
  permDesc: {
    fontSize: hp('1.9%'),
    color: COLORS.sub,
    marginTop: 2,
    lineHeight: 20,
  },
  cta: {
    height: CTA_HEIGHT,
    borderRadius: 14,
    backgroundColor: COLORS.brand,
    alignItems: 'center',
    justifyContent: 'center',
    flexDirection: 'row',
    shadowColor: COLORS.brand,
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.18,
    shadowRadius: 8,
    elevation: 3,
    marginTop: 6,
  },
  ctaText: { color: '#FFF', fontSize: hp('2.3%'), fontWeight: '900' },
});
