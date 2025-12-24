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

const ParkingLocation = ({ selectedCar }) => {
  const cfg = useMemo(() => {
    if (selectedCar && CAR_CONFIG[selectedCar]) return CAR_CONFIG[selectedCar];
    return { image: require('../assets/B1.png'), marker: { x: 0.5, y: 0.5 } };
  }, [selectedCar]);

  const [layoutSize, setLayoutSize] = useState({ width: 0, height: 0 });
  const onLayout = useCallback(e => {
    const { width, height } = e.nativeEvent.layout;
    setLayoutSize({ width, height });
  }, []);

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

    // 기본값
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

    // 마커 "컨테이너 좌표(px)" (contain 적용된 위치)
    const markerCx = offsetX + x * renderW; // center X
    const markerCy = offsetY + y * renderH; // center Y

    // 마커 이미지 표시(left/top)
    const left = markerCx - MARKER_SIZE / 2;
    const top = markerCy - MARKER_SIZE / 2;

    // ImageZoom의 positionX/Y는 "확대된 콘텐츠를 얼마나 이동시킬지"
    // 마커가 crop 중앙에 오도록 이동값 계산:
    // markerCx * zoom + posX = cropW/2  => posX = cropW/2 - markerCx*zoom
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
            // ✅ 초기 줌 + 이동(마커 센터링)
            scale={initialScale}
            positionX={initialPosition.x}
            positionY={initialPosition.y}
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
