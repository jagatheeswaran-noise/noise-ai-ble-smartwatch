import React from 'react';
import { View, TouchableOpacity, Text, StyleSheet } from 'react-native';

export type TabType = 'chat' | 'model' | 'health' | 'smartwatch';

interface NavigationTabsProps {
  activeTab: TabType;
  onTabChange: (tab: TabType) => void;
  modelStatus?: string;
  isModelLoaded?: boolean;
}

const NavigationTabs: React.FC<NavigationTabsProps> = ({
  activeTab,
  onTabChange,
  modelStatus,
  isModelLoaded = false,
}) => {
  const tabs = [
    { id: 'chat' as TabType, label: 'Chat', icon: '💬' },
    { id: 'model' as TabType, label: 'AI Model', icon: '🤖', status: isModelLoaded },
    { id: 'health' as TabType, label: 'Health', icon: '📊' },
    { id: 'smartwatch' as TabType, label: 'Watch', icon: '⌚' },
  ];

  const renderStatusDot = (status?: boolean) => {
    if (status === undefined) return null;
    
    return (
      <View style={[
        styles.statusDot,
        { backgroundColor: status ? '#4CAF50' : '#FF5722' }
      ]} />
    );
  };

  return (
    <View style={styles.container}>
      {tabs.map((tab) => (
        <TouchableOpacity
          key={tab.id}
          style={[
            styles.tab,
            activeTab === tab.id && styles.activeTab,
          ]}
          onPress={() => onTabChange(tab.id)}
        >
          <View style={styles.tabContent}>
            <Text style={styles.tabIcon}>{tab.icon}</Text>
            {renderStatusDot(tab.status)}
          </View>
          <Text style={[
            styles.tabLabel,
            activeTab === tab.id && styles.activeTabLabel,
          ]}>
            {tab.label}
          </Text>
        </TouchableOpacity>
      ))}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    backgroundColor: '#111',
    borderTopWidth: 1,
    borderTopColor: '#333',
    paddingVertical: 8,
  },
  tab: {
    flex: 1,
    alignItems: 'center',
    paddingVertical: 8,
    paddingHorizontal: 4,
  },
  activeTab: {
    backgroundColor: '#222',
    borderRadius: 8,
    marginHorizontal: 4,
  },
  tabContent: {
    position: 'relative',
    alignItems: 'center',
    justifyContent: 'center',
  },
  tabIcon: {
    fontSize: 20,
    marginBottom: 4,
  },
  tabLabel: {
    color: '#888',
    fontSize: 12,
    fontWeight: '500',
  },
  activeTabLabel: {
    color: '#007AFF',
    fontWeight: 'bold',
  },
  statusDot: {
    position: 'absolute',
    top: -2,
    right: -2,
    width: 8,
    height: 8,
    borderRadius: 4,
    borderWidth: 1,
    borderColor: '#000',
  },
});

export default NavigationTabs;
