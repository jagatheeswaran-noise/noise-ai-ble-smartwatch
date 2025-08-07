import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import { DeviceInfo } from '../../types/smartwatch';

interface DeviceCardProps {
  device: DeviceInfo;
  onSelect: () => void;
  isSelected: boolean;
}

const DeviceCard: React.FC<DeviceCardProps> = ({ device, onSelect, isSelected }) => {
  const getSignalStrength = (rssi: number): string => {
    if (rssi >= -50) return 'Excellent';
    if (rssi >= -60) return 'Good';
    if (rssi >= -70) return 'Fair';
    return 'Weak';
  };

  const getProtocolDisplay = (protocol: string): string => {
    if (!protocol) return 'Generic BLE';
    return protocol;
  };

  return (
    <TouchableOpacity 
      style={[
        styles.deviceCard, 
        isSelected && styles.selectedCard
      ]} 
      onPress={onSelect}
    >
      <View style={styles.deviceHeader}>
        <Text style={[styles.deviceName, isSelected && styles.selectedText]}>
          {device.name || 'Unknown Device'}
        </Text>
        <View style={[styles.protocolBadge, device.protocol === 'Berry' && styles.berryBadge]}>
          <Text style={styles.protocolText}>
            {getProtocolDisplay(device.protocol)}
          </Text>
        </View>
      </View>
      
      <View style={styles.deviceDetails}>
        <Text style={[styles.deviceAddress, isSelected && styles.selectedText]}>
          {device.address}
        </Text>
        <View style={styles.deviceStatus}>
          <Text style={[styles.signalText, isSelected && styles.selectedText]}>
            Signal: {getSignalStrength(device.rssi)} ({device.rssi} dBm)
          </Text>
          {device.isBind && (
            <View style={styles.bindBadge}>
              <Text style={styles.bindText}>Paired</Text>
            </View>
          )}
        </View>
      </View>
      
      {isSelected && (
        <View style={styles.selectionIndicator}>
          <Text style={styles.selectionText}>Selected</Text>
        </View>
      )}
    </TouchableOpacity>
  );
};

const styles = StyleSheet.create({
  deviceCard: {
    backgroundColor: '#2c2c2c',
    borderRadius: 12,
    padding: 16,
    marginVertical: 6,
    marginHorizontal: 16,
    borderWidth: 2,
    borderColor: 'transparent',
    elevation: 3,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.25,
    shadowRadius: 3.84,
  },
  selectedCard: {
    borderColor: '#4CAF50',
    backgroundColor: '#1e3d2f',
  },
  deviceHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  deviceName: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#ffffff',
    flex: 1,
    marginRight: 8,
  },
  selectedText: {
    color: '#4CAF50',
  },
  protocolBadge: {
    backgroundColor: '#555555',
    borderRadius: 12,
    paddingHorizontal: 8,
    paddingVertical: 4,
  },
  berryBadge: {
    backgroundColor: '#ff6b35',
  },
  protocolText: {
    fontSize: 12,
    color: '#ffffff',
    fontWeight: '500',
  },
  deviceDetails: {
    gap: 8,
  },
  deviceAddress: {
    fontSize: 14,
    color: '#cccccc',
    fontFamily: 'monospace',
  },
  deviceStatus: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  signalText: {
    fontSize: 12,
    color: '#aaaaaa',
  },
  bindBadge: {
    backgroundColor: '#2196F3',
    borderRadius: 8,
    paddingHorizontal: 6,
    paddingVertical: 2,
  },
  bindText: {
    fontSize: 10,
    color: '#ffffff',
    fontWeight: 'bold',
  },
  selectionIndicator: {
    marginTop: 12,
    padding: 8,
    backgroundColor: 'rgba(76, 175, 80, 0.2)',
    borderRadius: 8,
    alignItems: 'center',
  },
  selectionText: {
    color: '#4CAF50',
    fontSize: 12,
    fontWeight: 'bold',
  },
});

export default DeviceCard;
