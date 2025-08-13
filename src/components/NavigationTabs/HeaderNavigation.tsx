import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';

export interface HeaderNavigationProps {
  activeTab: string;
  onTabChange: (tab: string) => void;
  modelStatus: string;
  isModelLoaded: boolean;
}

const HeaderNavigation: React.FC<HeaderNavigationProps> = ({
  activeTab,
  onTabChange,
  modelStatus,
  isModelLoaded,
}) => {
  return (
    <>
      {/* Elegant Header */}
      <View style={styles.header}>
        <View style={styles.headerContent}>
          <View style={styles.brandContainer}>
            <View style={styles.brandIcon} />
            <Text style={styles.brandText}>Noise AI</Text>
          </View>
          <View style={styles.headerActions}>
            <TouchableOpacity
              style={styles.healthDashboardButton}
              onPress={() => onTabChange('health')}
            >
              <Text style={styles.healthDashboardButtonText}>💚</Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={styles.smartwatchButton}
              onPress={() => onTabChange('smartwatch')}
            >
              <Text style={styles.smartwatchButtonText}>⌚</Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={styles.modelManagerButton}
              onPress={() => onTabChange('models')}
            >
              <Text style={styles.modelManagerButtonText}>🤖</Text>
            </TouchableOpacity>
          </View>
        </View>
      </View>

      {/* Model Status Bar */}
      <View style={styles.modelStatusContainer}>
        <View style={[
          styles.modelStatusIndicator,
          isModelLoaded ? styles.modelLoaded : styles.modelNotLoaded,
        ]} />
        <Text style={styles.modelStatusText}>
          {modelStatus || (isModelLoaded ? 'AI Model Ready' : 'AI Model Loading...')}
        </Text>
      </View>
    </>
  );
};

const styles = StyleSheet.create({
  header: {
    backgroundColor: '#1a1a1a',
    paddingTop: 50,
    paddingBottom: 20,
    paddingHorizontal: 24,
    borderBottomWidth: 1,
    borderBottomColor: '#2a2a2a',
  },
  headerContent: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  brandContainer: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  brandIcon: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: '#00ff88',
    marginRight: 12,
  },
  brandText: {
    fontSize: 22,
    fontWeight: '300',
    color: '#ffffff',
    letterSpacing: 1,
  },
  headerActions: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  modelManagerButton: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: '#00ff88',
    justifyContent: 'center',
    alignItems: 'center',
  },
  modelManagerButtonText: {
    color: '#000000',
    fontSize: 12,
    fontWeight: 'bold',
  },
  smartwatchButton: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: '#4ade80',
    justifyContent: 'center',
    alignItems: 'center',
  },
  smartwatchButtonText: {
    color: '#000000',
    fontSize: 16,
    fontWeight: 'bold',
  },
  healthDashboardButton: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: '#2a2a2a',
    justifyContent: 'center',
    alignItems: 'center',
  },
  healthDashboardButtonText: {
    fontSize: 16,
  },
  modelStatusContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 24,
    paddingVertical: 8,
    backgroundColor: '#1a1a1a',
  },
  modelStatusIndicator: {
    width: 8,
    height: 8,
    borderRadius: 4,
    marginRight: 8,
  },
  modelLoaded: {
    backgroundColor: '#00ff88',
  },
  modelNotLoaded: {
    backgroundColor: '#ff6b6b',
  },
  modelStatusText: {
    color: '#cccccc',
    fontSize: 12,
    fontWeight: '400',
    flex: 1,
  },
});

export default HeaderNavigation;
