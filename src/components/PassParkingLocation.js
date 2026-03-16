import React, {
  useMemo,
  useState,
  useCallback,
  useRef,
  useEffect,
} from 'react';
import { View, Image, StyleSheet, InteractionManager } from 'react-native';
import ImageZoom from 'react-native-image-pan-zoom';

const MARKER_SIZE = 28;

const ZOOM_OUT_SCALE = 0.9; // ✅ 처음엔 살짝 줌아웃
const ZOOM_IN_SCALE = 1.5; // ✅ 50% 줌인
const ZOOM_IN_DURATION = 280; // ✅ 줌인 애니메이션 시간(ms)

const FLOOR_CALIB = {
  B1: {
    minX: 1480,
    minY: 1420,
    maxX: 6000,
    maxY: 4000,
    invertY: true,
  },
};

const CAR_CONFIG = {
  test1234: { image: require('../assets/B1.png') },
  test5678: { image: require('../assets/B1.png') },
  test9012: { image: require('../assets/B1.png') },
};

function getFloorKey(floor) {
  return (
    floor?.code ||
    floor?.name ||
    floor?.floorName ||
    floor?.id ||
    floor?.floor_id ||
    'B1'
  );
}

function clamp01(v) {
  return Math.max(0, Math.min(1, v));
}

function mapServerTo01({ x, y, floor }) {
  const key = getFloorKey(floor);
  const cal = FLOOR_CALIB[key];

  if (!cal || !Number.isFinite(x) || !Number.isFinite(y)) {
    return { nx: 0.5, ny: 0.5 };
  }

  const nx = (x - cal.minX) / (cal.maxX - cal.minX);
  let ny = (y - cal.minY) / (cal.maxY - cal.minY);

  if (cal.invertY) ny = 1 - ny;

  return { nx: clamp01(nx), ny: clamp01(ny) };
}

const PassParkingLocation = ({ selectedCar, deviceLoc, visible, focusKey }) => {
  const zoomRef = useRef(null);

  const cfg = useMemo(() => {
    if (selectedCar && CAR_CONFIG[selectedCar]) return CAR_CONFIG[selectedCar];
    return { image: require('../assets/B1.png') };
  }, [selectedCar]);

  const [layoutSize, setLayoutSize] = useState({ width: 0, height: 0 });
  const onLayout = useCallback(e => {
    const { width, height } = e.nativeEvent.layout;
    setLayoutSize({ width, height });
  }, []);

  // ✅ 마커 중심점(cx, cy) 계산 + 마커 스타일
  const markerCalc = useMemo(() => {
    const { width, height } = layoutSize;
    if (width <= 0 || height <= 0) return null;

    const { nx, ny } = mapServerTo01({
      x: deviceLoc?.x,
      y: deviceLoc?.y,
      floor: deviceLoc?.floor,
    });

    const src = Image.resolveAssetSource(cfg.image);
    const imgW = src?.width || 1;
    const imgH = src?.height || 1;

    const scale = Math.min(width / imgW, height / imgH);
    const renderW = imgW * scale;
    const renderH = imgH * scale;
    const offsetX = (width - renderW) / 2;
    const offsetY = (height - renderH) / 2;

    const cx = offsetX + nx * imgW * scale; // ✅ crop 영역 기준 마커 중심 X
    const cy = offsetY + ny * imgH * scale; // ✅ crop 영역 기준 마커 중심 Y

    const left = cx - MARKER_SIZE / 2;
    const top = cy - MARKER_SIZE / 2;

    return {
      cx,
      cy,
      markerStyle: [
        styles.markerImg,
        { left, top, width: MARKER_SIZE, height: MARKER_SIZE },
      ],
    };
  }, [cfg.image, layoutSize, deviceLoc]);

  // ✅ 팝업 열릴 때(visible=true) “줌아웃 → 마커기준 줌인” 강제 실행
  useEffect(() => {
    if (!visible) return;
    if (!zoomRef.current) return;
    if (!markerCalc) return;

    const { width, height } = layoutSize;
    if (width <= 0 || height <= 0) return;

    let cancelled = false;
    let t1, t2;

    const run = async () => {
      // ✅ 모달 애니메이션/레이아웃 끝난 뒤 실행 (씹힘 방지)
      await new Promise(resolve =>
        InteractionManager.runAfterInteractions(resolve),
      );
      if (cancelled) return;

      // requestAnimationFrame 2번 정도 기다리면 안정적
      requestAnimationFrame(() => {
        requestAnimationFrame(() => {
          if (cancelled) return;

          const cx = markerCalc.cx;
          const cy = markerCalc.cy;

          // (w/2,h/2)에 (cx,cy)가 오도록 translate 계산
          const dxOut = (width / 2 - cx) * ZOOM_OUT_SCALE;
          const dyOut = (height / 2 - cy) * ZOOM_OUT_SCALE;

          const dxIn = (width / 2 - cx) * ZOOM_IN_SCALE;
          const dyIn = (height / 2 - cy) * ZOOM_IN_SCALE;

          // 1) 먼저 줌아웃 상태로 "마커 중심" 맞춰놓기 (즉시)
          zoomRef.current?.centerOn?.({
            x: dxOut,
            y: dyOut,
            scale: ZOOM_OUT_SCALE,
            duration: 0,
          });

          // 2) 살짝 뒤에 줌인 애니메이션
          t1 = setTimeout(() => {
            if (cancelled) return;
            zoomRef.current?.centerOn?.({
              x: dxIn,
              y: dyIn,
              scale: ZOOM_IN_SCALE,
              duration: ZOOM_IN_DURATION,
            });
          }, 80);
        });
      });
    };

    run();

    return () => {
      cancelled = true;
      if (t1) clearTimeout(t1);
      if (t2) clearTimeout(t2);
    };
  }, [visible, focusKey, markerCalc, layoutSize]);

  const { width, height } = layoutSize;

  return (
    <View style={styles.container}>
      <View style={styles.viewport} onLayout={onLayout}>
        <View style={styles.borderWrapper}>
          <ImageZoom
            ref={zoomRef}
            cropWidth={width || 1}
            cropHeight={height || 1}
            imageWidth={width || 1}
            imageHeight={height || 1}
            minScale={0.6}
            maxScale={5}
            enableCenterFocus
            pinchToZoom
            enableDoubleClickZoom
          >
            {width > 0 && height > 0 ? (
              <View>
                <Image
                  source={cfg.image}
                  style={{ width, height }}
                  resizeMode="contain"
                />
                <Image
                  source={require('../assets/parking.png')}
                  style={markerCalc?.markerStyle}
                />
              </View>
            ) : (
              <View
                style={{
                  width: '100%',
                  height: '100%',
                  backgroundColor: '#fff',
                }}
              />
            )}
          </ImageZoom>
        </View>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: { alignItems: 'center', backgroundColor: '#FFFFFF' },
  viewport: { width: '92%', aspectRatio: 3 / 2, alignSelf: 'center' },
  borderWrapper: {
    flex: 1,
    borderWidth: 2,
    borderColor: '#000',
    borderRadius: 8,
    overflow: 'hidden',
    backgroundColor: '#fff',
  },
  markerImg: { position: 'absolute' },
});

export default PassParkingLocation;
