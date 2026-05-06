import React from 'react';
import {
  Modal,
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  Linking,
} from 'react-native';

/**
 * 강제 업데이트 팝업
 * - 닫기 버튼 없음 (뒤로가기로도 닫히지 않음)
 * - "업데이트" 버튼 → Play Store 이동
 */
export default function ForceUpdateModal({ visible, storeUrl }) {
  const handleUpdate = () => {
    const url =
      storeUrl ||
      'https://play.google.com/store/apps/details?id=com.pms_parkin_mobile';
    Linking.openURL(url).catch(() => {
      Linking.openURL(
        'https://play.google.com/store/apps/details?id=com.pms_parkin_mobile',
      );
    });
  };

  return (
    <Modal
      visible={visible}
      transparent
      animationType="fade"
      onRequestClose={() => {}}  // 뒤로가기 무시
      statusBarTranslucent>
      <View style={styles.overlay}>
        <View style={styles.card}>
          {/* 아이콘 */}
          <Text style={styles.icon}>🔔</Text>

          {/* 제목 */}
          <Text style={styles.title}>업데이트 안내</Text>

          {/* 내용 */}
          <Text style={styles.message}>
            새로운 버전이 출시되었습니다.{'\n'}
            원활한 서비스 이용을 위해{'\n'}
            최신 버전으로 업데이트해 주세요.
          </Text>

          {/* 업데이트 버튼 */}
          <TouchableOpacity style={styles.updateBtn} onPress={handleUpdate}>
            <Text style={styles.updateBtnText}>업데이트</Text>
          </TouchableOpacity>
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.6)',
    alignItems: 'center',
    justifyContent: 'center',
    padding: 32,
  },
  card: {
    backgroundColor: '#FFFFFF',
    borderRadius: 16,
    padding: 28,
    width: '100%',
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.2,
    shadowRadius: 12,
    elevation: 8,
  },
  icon: {
    fontSize: 40,
    marginBottom: 16,
  },
  title: {
    fontSize: 20,
    fontWeight: '700',
    color: '#1E293B',
    marginBottom: 12,
  },
  message: {
    fontSize: 15,
    color: '#475569',
    textAlign: 'center',
    lineHeight: 24,
    marginBottom: 24,
  },
  updateBtn: {
    width: '100%',
    height: 50,
    backgroundColor: '#2563EB',
    borderRadius: 10,
    alignItems: 'center',
    justifyContent: 'center',
  },
  updateBtnText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '700',
  },
});
