import React, { useMemo, useState, useCallback } from 'react';
import { View, Image, StyleSheet } from 'react-native';
import ImageZoom from 'react-native-image-pan-zoom';

const MARKER_SIZE = 28;

// ✅ MapId에 따른 이미지 매핑 (안드로이드 리소스 이름 기준)
const MAP_IMAGES = {
  P1: require('../assets/P1.png'),
  P2_E: require('../assets/P2.png'),
  P2_W: require('../assets/P5.png'),
  P3_G: require('../assets/P3.png'),
  P3_B: require('../assets/P4.png'),
  default: require('../assets/P1.png'), // 예외 처리용
};

const ParkingLocation = ({ selectedCar, deviceLoc, onInteractingChange }) => {
  // ✅ 1. MapId에 따른 이미지 결정
  const bgImage = useMemo(() => {
    if (!deviceLoc?.floor) return MAP_IMAGES.default;
    return MAP_IMAGES[deviceLoc.floor] || MAP_IMAGES.default;
  }, [deviceLoc?.floor]);

  const [layoutSize, setLayoutSize] = useState({ width: 0, height: 0 });

  const onLayout = useCallback(e => {
    const { width, height } = e.nativeEvent.layout;
    setLayoutSize({ width, height });
  }, []);

  const setInteractingSafe = useCallback(
    v => {
      if (typeof onInteractingChange === 'function') onInteractingChange(v);
    },
    [onInteractingChange],
  );

  const calculateInfo = useMemo(() => {
    const { width, height } = layoutSize;
    if (width <= 0 || height <= 0 || !deviceLoc) {
      console.log('[Debug] Layout not ready or no deviceLoc');
      return null;
    }

    // 1. 원본 도면 해상도
    const ORIGINAL_IMAGE_W = 1572;
    const ORIGINAL_IMAGE_H = 1146;

    // 2. 스케일 계산 (contain 방식)
    const scale = Math.min(width / ORIGINAL_IMAGE_W, height / ORIGINAL_IMAGE_H);
    const renderW = ORIGINAL_IMAGE_W * scale;
    const renderH = ORIGINAL_IMAGE_H * scale;

    // 이미지 뷰 안에서 실제 이미지가 시작되는 지점 (중앙 정렬)
    const offsetX = (width - renderW) / 2;
    const offsetY = (height - renderH) / 2;
    // 3. 목표 비율 (실측 데이터 804, 651 기준)
    const xRatio = 804 / 1572; // 0.5114 -> 약 중앙 지점
    const yRatio = 651 / 1146; // 0.5681 -> 약 중앙 하단 지점

    // 4. 화면상의 마커 중심 좌표 (줌 전)
    const markerCx = xRatio * renderW + offsetX;
    const markerCy = yRatio * renderH + offsetY;

    // 5. 최종 마커 위치 (절대 좌표)
    const left = markerCx - MARKER_SIZE / 2;
    const top = markerCy - MARKER_SIZE;

    // 6. 줌 및 포지션 계산
    const zoom = 3.2;
    const posX = width / 2 - markerCx * zoom;
    const posY = height / 2 - markerCy * zoom;

    // 🔍 로그 출력 (이 로그들을 복사해서 보여주세요)
    console.log('--- Coordinate Debug Start ---');
    console.log(`1. Layout (Viewport): ${width} x ${height}`);
    console.log(
      `2. Rendered Img: ${renderW.toFixed(2)} x ${renderH.toFixed(
        2,
      )} (Scale: ${scale.toFixed(4)})`,
    );
    console.log(
      `3. Offset (Margin): X=${offsetX.toFixed(2)}, Y=${offsetY.toFixed(2)}`,
    );
    console.log(
      `4. Target Ratio: X=${xRatio.toFixed(4)}, Y=${yRatio.toFixed(4)}`,
    );
    console.log(
      `5. Marker Center: X=${markerCx.toFixed(2)}, Y=${markerCy.toFixed(2)}`,
    );
    console.log(
      `6. Final Style: left=${left.toFixed(2)}, top=${top.toFixed(2)}`,
    );
    console.log(
      `7. Zoom Position: posX=${posX.toFixed(2)}, posY=${posY.toFixed(2)}`,
    );
    console.log('--- Coordinate Debug End ---');

    return { left, top, zoom, posX, posY };
  }, [deviceLoc, layoutSize]);

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
            maxScale={10}
            scale={calculateInfo?.zoom || 1}
            positionX={calculateInfo?.posX || 0}
            positionY={calculateInfo?.posY || 0}
          >
            {width > 0 && calculateInfo ? (
              <View
                onTouchStart={() => setInteractingSafe(true)}
                onTouchEnd={() => setInteractingSafe(false)}
                onTouchCancel={() => setInteractingSafe(false)}
              >
                <Image
                  source={bgImage}
                  style={{ width, height }}
                  resizeMode="contain"
                />
                <Image
                  source={require('../assets/parking.png')}
                  style={[
                    styles.markerImg,
                    {
                      left: calculateInfo.left,
                      top: calculateInfo.top,
                      width: MARKER_SIZE,
                      height: MARKER_SIZE,
                    },
                  ]}
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
