import React, {
  useMemo,
  useState,
  useCallback,
  useRef,
  useEffect,
} from 'react';
import { View, Image, StyleSheet, Dimensions } from 'react-native';
import ImageZoom from 'react-native-image-pan-zoom';

// 안드로이드 25dp 크기와 유사하게 설정
const MARKER_SIZE = 30;

// 🚨 [중요] 안드로이드 자바 코드의 기준값과 동일하게 설정
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

const ParkingLocation = ({ deviceLoc, onInteractingChange }) => {
  const zoomRef = useRef(null);
  const [layoutSize, setLayoutSize] = useState({ width: 0, height: 0 });

  const bgImage = useMemo(() => {
    if (!deviceLoc?.floor) return MAP_IMAGES.default;
    return MAP_IMAGES[deviceLoc.floor] || MAP_IMAGES.default;
  }, [deviceLoc?.floor]);

  // 뷰가 그려질 때 안드로이드의 CurrentWidth, CurrentHeight를 획득
  const onLayout = useCallback(e => {
    const { width, height } = e.nativeEvent.layout;
    console.log(`[Layout Log] Current View Size: ${width} x ${height}`);
    setLayoutSize({ width, height });
  }, []);

  const calculateInfo = useMemo(() => {
    const { width, height } = layoutSize;
    if (width <= 0 || height <= 0 || !deviceLoc) return null;

    // 1. 서버 원본 좌표 (Java: X, Y 동일)
    const rawX = deviceLoc.x != null ? Number(deviceLoc.x) : 0;
    const rawY = deviceLoc.y != null ? Number(deviceLoc.y) : 0;

    // 2. Java의 PercentWidth/PercentHeight 와 동일
    const percentWidth = width / ORIGIN_W;
    const percentHeight = height / ORIGIN_H;

    // 3. 뷰포트 기준 마커 픽셀 좌표
    //    Java: ChangeX = X * PercentWidth + ImgLocationX - IconWidth/2
    //    RN:   ImgLocationX = 0 (마커가 같은 컨테이너 자식이므로)
    const markerCx = rawX * percentWidth;
    const markerCy = rawY * percentHeight;

    // 4. 마커 스타일 (X: 중앙 정렬, Y: 하단 끝을 좌표에 맞춤 — Java와 동일)
    const styleLeft = markerCx - MARKER_SIZE / 2;
    const styleTop = markerCy - MARKER_SIZE;

    // 5. centerOn 좌표 — 이미지 좌상단(0,0) 기준 절대 픽셀 좌표를 그대로 넘김
    //    라이브러리 내부: positionX = cropW/2 - x, 실제 translation = positionX * scale
    //    따라서 x = markerCx (절대좌표), y = markerCy (절대좌표)
    const targetX = markerCx;
    const targetY = markerCy;

    console.log('--- [Coordinate Debug] ---');
    console.log(`Raw(server): x=${rawX}, y=${rawY}`);
    console.log(
      `Viewport: ${width} x ${height}  |  percent: ${percentWidth.toFixed(
        4,
      )}, ${percentHeight.toFixed(4)}`,
    );
    console.log(
      `Marker px: cx=${markerCx.toFixed(1)}, cy=${markerCy.toFixed(1)}`,
    );
    console.log(
      `style: left=${styleLeft.toFixed(1)}, top=${styleTop.toFixed(1)}`,
    );
    console.log(`centerOn: x=${targetX.toFixed(1)}, y=${targetY.toFixed(1)}`);

    return { styleLeft, styleTop, targetX, targetY };
  }, [deviceLoc, layoutSize]);

  // 데이터가 로드되거나 변경되면 해당 위치로 줌인
  useEffect(() => {
    if (!calculateInfo) return;
    // ImageZoom 내부 초기화 완료 후 centerOn 호출
    const timer = setTimeout(() => {
      zoomRef.current?.centerOn({
        x: calculateInfo.targetX,
        y: calculateInfo.targetY,
        scale: 2.5,
        duration: 300,
      });
    }, 50);
    return () => clearTimeout(timer);
  }, [calculateInfo]);

  return (
    <View style={styles.container}>
      {/* 뷰포트의 비율을 안드로이드 이미지뷰(1572/1146)와 강제로 맞춤 */}
      <View style={styles.viewport} onLayout={onLayout}>
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
            onMove={() => onInteractingChange?.(true)}
          >
            {/* 안드로이드의 match_parent RelativeLayout 역할을 하는 캔버스 */}
            <View
              style={{
                width: layoutSize.width,
                height: layoutSize.height,
                backgroundColor: '#FFFFFF',
              }}
            >
              <Image
                source={bgImage}
                style={styles.mapImage}
                resizeMode="stretch" // 🚨 핵심: 누끼 이미지여도 배경이 있는 것처럼 꽉 채움(fitXY)
              />
              <Image
                source={require('../assets/parking.png')} // 마커 이미지
                style={[
                  styles.markerImg,
                  {
                    left: calculateInfo.styleLeft,
                    top: calculateInfo.styleTop,
                  },
                ]}
              />
            </View>
          </ImageZoom>
        )}
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    width: '100%',
    alignItems: 'center',
    backgroundColor: '#FFFFFF',
  },
  viewport: {
    // 화면 너비의 95%를 사용하되, 높이는 원본 도면 비율에 따라 자동 결정
    width: Dimensions.get('window').width * 0.95,
    aspectRatio: 1572 / 1146,
    backgroundColor: '#FFFFFF',
    overflow: 'hidden',
    borderWidth: 1,
    borderColor: '#E5E7EB',
    borderRadius: 8,
  },
  mapImage: {
    width: '100%',
    height: '100%',
    position: 'absolute',
  },
  markerImg: {
    position: 'absolute',
    width: MARKER_SIZE,
    height: MARKER_SIZE,
    zIndex: 99,
  },
});

export default ParkingLocation;
