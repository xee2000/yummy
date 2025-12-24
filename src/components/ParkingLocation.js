import React, { useMemo, useState, useCallback } from 'react';
import { View, Image, StyleSheet } from 'react-native';
import ImageZoom from 'react-native-image-pan-zoom';

const MARKER_SIZE = 28;

const CAR_CONFIG = {
  test1234: { image: require('../assets/B1.png'), marker: { x: 0.3, y: 0.35 } },
  test5678: {
    image: require('../assets/B1.png'),
    marker: { x: 0.62, y: 0.48 },
  },
  test9012: { image: require('../assets/B1.png'), marker: { x: 0.78, y: 0.7 } },
};

/**
 * props:
 * - selectedCar
 * - onInteractingChange?: (isInteracting: boolean) => void
 *   ✅ 도면 터치/드래그/핀치 중이면 true, 손 떼면 false (부모에서 새로고침 비활성화에 사용)
 */
const ParkingLocation = ({ selectedCar, onInteractingChange }) => {
  const cfg = useMemo(() => {
    if (selectedCar && CAR_CONFIG[selectedCar]) return CAR_CONFIG[selectedCar];
    return { image: require('../assets/B1.png'), marker: { x: 0.5, y: 0.5 } };
  }, [selectedCar]);

  const [layoutSize, setLayoutSize] = useState({ width: 0, height: 0 });

  const onLayout = useCallback(e => {
    const { width, height } = e.nativeEvent.layout;
    setLayoutSize({ width, height });
  }, []);

  // ✅ 도면 터치 중인지 (부모 RefreshControl 끄는 용도)
  const [interacting, setInteracting] = useState(false);
  const setInteractingSafe = useCallback(
    v => {
      setInteracting(v);
      if (typeof onInteractingChange === 'function') onInteractingChange(v);
    },
    [onInteractingChange],
  );

  // ✅ contain 기준으로 실제 렌더된 이미지 영역 계산
  const containInfo = useMemo(() => {
    const { width, height } = layoutSize;
    if (width <= 0 || height <= 0) return null;

    const src = Image.resolveAssetSource(cfg.image);
    const imgW = src?.width || 1;
    const imgH = src?.height || 1;

    const scale = Math.min(width / imgW, height / imgH);
    const renderW = imgW * scale;
    const renderH = imgH * scale;
    const offsetX = (width - renderW) / 2;
    const offsetY = (height - renderH) / 2;

    return { imgW, imgH, scale, renderW, renderH, offsetX, offsetY };
  }, [cfg.image, layoutSize]);

  // ✅ 마커 위치(픽셀) + 초기 줌/이동값 계산
  const { markerStyle, initialScale, initialPosition } = useMemo(() => {
    const { width, height } = layoutSize;

    if (!containInfo || width <= 0 || height <= 0) {
      return {
        markerStyle: [styles.markerImg, { left: 0, top: 0 }],
        initialScale: 1,
        initialPosition: { x: 0, y: 0 },
      };
    }

    const zoom = 1.5; // ✅ 50% 줌인
    const { renderW, renderH, offsetX, offsetY } = containInfo;
    const { x, y } = cfg.marker;

    const markerCx = offsetX + x * renderW;
    const markerCy = offsetY + y * renderH;

    const left = markerCx - MARKER_SIZE / 2;
    const top = markerCy - MARKER_SIZE / 2;

    const posX = width / 2 - markerCx * zoom;
    const posY = height / 2 - markerCy * zoom;

    return {
      markerStyle: [
        styles.markerImg,
        { left, top, width: MARKER_SIZE, height: MARKER_SIZE },
      ],
      initialScale: zoom,
      initialPosition: { x: posX, y: posY },
    };
  }, [cfg.marker, containInfo, layoutSize]);

  const { width, height } = layoutSize;

  return (
    <View style={styles.container}>
      <View style={styles.viewport} onLayout={onLayout}>
        <View style={styles.borderWrapper}>
          <ImageZoom
            cropWidth={width || 1}
            cropHeight={height || 1}
            imageWidth={width || 1}
            imageHeight={height || 1}
            minScale={1}
            maxScale={5}
            enableCenterFocus
            pinchToZoom
            enableDoubleClickZoom
            scale={initialScale}
            positionX={initialPosition.x}
            positionY={initialPosition.y}
          >
            {width > 0 && height > 0 ? (
              <View
                // ✅ 도면 제스처 중에는 부모 pull-to-refresh 끄기 용도
                onTouchStart={() => setInteractingSafe(true)}
                onTouchEnd={() => setInteractingSafe(false)}
                onTouchCancel={() => setInteractingSafe(false)}
              >
                <Image
                  source={cfg.image}
                  style={{ width, height }}
                  resizeMode="contain"
                />
                <Image
                  source={require('../assets/parking.png')}
                  style={markerStyle}
                  resizeMode="contain"
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

export default ParkingLocation;
