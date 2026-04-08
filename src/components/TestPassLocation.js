import React, { useMemo, useState, useCallback, useRef, useEffect } from 'react';
import { View, Image, StyleSheet, InteractionManager } from 'react-native';
import ImageZoom from 'react-native-image-pan-zoom';

// ----------------------------------------------------------------
// [설정값] 고정 맵 1장 + 좌표 스케일
// ----------------------------------------------------------------
const DOT_SIZE = 18;
const ORIGIN_W = 1572;
const ORIGIN_H = 1146;

const MAP_IMAGE = require('../assets/B1.png');

const calcDotStyle = (loc, layoutSize, color) => {
  const { width, height } = layoutSize;
  if (width <= 0 || height <= 0 || !loc) return null;
  // CSV 좌표는 이미 1572×1146 기준 픽셀 좌표 → 스케일 없이 비율만 적용
  const cx = Number(loc.x) * (width / ORIGIN_W);
  const cy = Number(loc.y) * (height / ORIGIN_H);
  return {
    cx,
    cy,
    dotStyle: {
      position: 'absolute',
      left: cx - DOT_SIZE / 2,
      top: cy - DOT_SIZE / 2,
      width: DOT_SIZE,
      height: DOT_SIZE,
      borderRadius: DOT_SIZE / 2,
      backgroundColor: color,
      zIndex: 999,
      borderWidth: 2,
      borderColor: '#fff',
    },
  };
};

// actualLoc  : 내가 실제 서있는 비콘 위치 (빨간 점)
// estimatedLoc : 알고리즘 추정 좌표 (파란 점)
const TestParkingLocation = ({ actualLoc, estimatedLoc, visible, focusKey }) => {
  const zoomRef = useRef(null);
  const [layoutSize, setLayoutSize] = useState({ width: 0, height: 0 });

  const onLayout = useCallback(e => {
    const { width, height } = e.nativeEvent.layout;
    setLayoutSize({ width, height });
  }, []);

  const actualDot     = useMemo(() => calcDotStyle(actualLoc, layoutSize, '#FF3B30'),     [actualLoc, layoutSize]);
  const estimatedDot  = useMemo(() => calcDotStyle(estimatedLoc, layoutSize, '#007AFF'),  [estimatedLoc, layoutSize]);

  // 모달 오픈 시 추정 좌표 중심으로 자동 줌인
  useEffect(() => {
    if (!visible || !zoomRef.current || !estimatedDot) return;
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
          const { cx, cy } = estimatedDot;

          zoomRef.current?.centerOn?.({
            x: (width / 2 - cx) * 0.9,
            y: (height / 2 - cy) * 0.9,
            scale: 0.9,
            duration: 0,
          });

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
  }, [visible, focusKey, estimatedDot, layoutSize]);

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
                  source={MAP_IMAGE}
                  style={styles.mapImage}
                  resizeMode="stretch"
                />
                {actualDot && (
                  <View style={actualDot.dotStyle} />
                )}
                {estimatedDot && (
                  <View style={estimatedDot.dotStyle} />
                )}
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
});

export default TestParkingLocation;
