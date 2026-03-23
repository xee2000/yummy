import React, {
  useMemo,
  useState,
  useCallback,
  useRef,
  useEffect,
} from 'react';
import { View, Image, StyleSheet, InteractionManager } from 'react-native';
import ImageZoom from 'react-native-image-pan-zoom';

// ----------------------------------------------------------------
// [설정값] Parking.js와 동일한 상수 세팅
// ----------------------------------------------------------------
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

const PassParkingLocation = ({ deviceLoc, visible, focusKey }) => {
  const zoomRef = useRef(null);
  const [layoutSize, setLayoutSize] = useState({ width: 0, height: 0 });

  // 1. 서버 mapId에 따른 배경 이미지 선택
  const floorImage = useMemo(() => {
    const mapId = deviceLoc?.floor;
    if (mapId && MAP_IMAGES[mapId]) {
      return MAP_IMAGES[mapId];
    }
    return MAP_IMAGES.default;
  }, [deviceLoc?.floor]);

  const onLayout = useCallback(e => {
    const { width, height } = e.nativeEvent.layout;
    setLayoutSize({ width, height });
  }, []);

  // 2. [중요] Parking.js와 동일한 좌표 계산 수식 적용
  const markerCalc = useMemo(() => {
    const { width, height } = layoutSize;
    if (width <= 0 || height <= 0 || !deviceLoc) return null;

    // (1) 서버 원본 좌표 로드
    const rawX = deviceLoc.x != null ? Number(deviceLoc.x) : 0;
    const rawY = deviceLoc.y != null ? Number(deviceLoc.y) : 0;

    // (2) 고정 스케일 적용 (Parking.js 방식)
    const scaledX = rawX * X_SCALE;
    const scaledY = rawY * Y_SCALE;

    // (3) 현재 레이아웃 해상도 대비 비율 계산
    const percentWidth = width / ORIGIN_W;
    const percentHeight = height / ORIGIN_H;

    // (4) 최종 마커 중심점 좌표
    const cx = scaledX * percentWidth;
    const cy = scaledY * percentHeight;

    // (5) 스타일 반환 (마커의 중앙/하단 보정)
    return {
      cx,
      cy,
      markerStyle: [
        styles.markerImg,
        { 
          left: cx - MARKER_SIZE / 2, 
          top: cy - MARKER_SIZE, // 핀 끝이 좌표에 오도록 설정
          width: MARKER_SIZE, 
          height: MARKER_SIZE 
        },
      ],
    };
  }, [layoutSize, deviceLoc]);

  // 3. 모달 오픈 시 자동 줌인 애니메이션
  useEffect(() => {
    if (!visible || !zoomRef.current || !markerCalc) return;

    const { width, height } = layoutSize;
    if (width <= 0 || height <= 0) return;

    let cancelled = false;
    let t1;

    const run = async () => {
      await new Promise(resolve => InteractionManager.runAfterInteractions(resolve));
      if (cancelled) return;

      requestAnimationFrame(() => {
        requestAnimationFrame(() => {
          if (cancelled) return;

          const { cx, cy } = markerCalc;

          // 줌아웃 상태에서 마커 위치 잡기
          zoomRef.current?.centerOn?.({
            x: (width / 2 - cx) * 0.9,
            y: (height / 2 - cy) * 0.9,
            scale: 0.9,
            duration: 0,
          });

          // 1.8배 정도로 마커 중심 줌인
          t1 = setTimeout(() => {
            if (cancelled) return;
            zoomRef.current?.centerOn?.({
              x: width / 2 - cx,
              y: height / 2 - cy,
              scale: 1.8,
              duration: 300,
            });
          }, 100);
        });
      });
    };

    run();
    return () => {
      cancelled = true;
      if (t1) clearTimeout(t1);
    };
  }, [visible, focusKey, markerCalc, layoutSize]);

  return (
    <View style={styles.container}>
      <View style={styles.viewport} onLayout={onLayout}>
        <View style={styles.borderWrapper}>
          <ImageZoom
            ref={zoomRef}
            cropWidth={layoutSize.width || 1}
            cropHeight={layoutSize.height || 1}
            imageWidth={layoutSize.width || 1}
            imageHeight={layoutSize.height || 1}
            minScale={0.5}
            maxScale={5}
            enableCenterFocus={false}
          >
            {layoutSize.width > 0 && (
              <View style={{ width: layoutSize.width, height: layoutSize.height }}>
                <Image
                  source={floorImage}
                  style={styles.mapImage}
                  resizeMode="stretch" // Parking.js와 동일하게 stretch 권장
                />
                <Image
                  source={require('../assets/parking.png')}
                  style={markerCalc?.markerStyle}
                />
              </View>
            )}
          </ImageZoom>
        </View>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: { width: '100%', height: '100%', backgroundColor: '#FFFFFF' },
  viewport: { flex: 1, alignSelf: 'center', width: '100%' },
  borderWrapper: {
    flex: 1,
    borderWidth: 1,
    borderColor: '#E2E8F0',
    borderRadius: 16,
    overflow: 'hidden',
    backgroundColor: '#F8FAFC',
  },
  mapImage: { width: '100%', height: '100%', position: 'absolute' },
  markerImg: { position: 'absolute', zIndex: 999 },
});

export default PassParkingLocation;