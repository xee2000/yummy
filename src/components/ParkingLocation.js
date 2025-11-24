import React, { useMemo, useState, useCallback } from 'react';
import { View, Image, StyleSheet } from 'react-native';
import ImageZoom from 'react-native-image-pan-zoom';

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

  const markerStyle = useMemo(() => {
    const { x, y } = cfg.marker;
    return [
      styles.markerImg,
      {
        left: `${x * 100}%`,
        top: `${y * 100}%`,
        transform: [{ translateX: -14 }, { translateY: -14 }],
      },
    ];
  }, [cfg.marker]);

  const { width, height } = layoutSize;

  return (
    <View style={styles.container}>
      <View style={styles.viewport} onLayout={onLayout}>
        <View style={styles.borderWrapper}>
          {/* ✅ 항상 ImageZoom은 호출되지만, 내부에서 렌더링 제어 */}
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
              // 👇 처음 layout 잡히기 전엔 빈 뷰만 보여줌
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
  markerImg: { position: 'absolute', width: 28, height: 28 },
});

export default ParkingLocation;
