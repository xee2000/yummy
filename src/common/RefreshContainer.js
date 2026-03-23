import React, { useState, useCallback } from 'react';
import { ScrollView, RefreshControl, StyleSheet, View } from 'react-native';

const RefreshContainer = ({ onRefresh, children, style, contentContainerStyle }) => {
  const [refreshing, setRefreshing] = useState(false);

  const handleRefresh = useCallback(async () => {
    setRefreshing(true);
    try {
      if (onRefresh) await onRefresh();
    } catch (e) {
      console.error('[RefreshContainer] Error:', e);
    } finally {
      setRefreshing(false);
    }
  }, [onRefresh]);

  return (
    <ScrollView
      style={[styles.container, style]}
      contentContainerStyle={[{ flexGrow: 1 }, contentContainerStyle]}
      showsVerticalScrollIndicator={false}
      refreshControl={
        <RefreshControl
          refreshing={refreshing}
          onRefresh={handleRefresh}
          tintColor="#007AFF"
          colors={['#007AFF']}
        />
      }
    >
      {children}
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1 },
});

export default RefreshContainer;