import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  Alert,
  ScrollView,
} from 'react-native';
import { zhSDKService } from '../../services/ZHSDKService';
import { DeviceInfo, ConnectionStatus } from '../../types/smartwatch';

interface DeviceControlProps {
  device: DeviceInfo | null;
}

const DeviceControl: React.FC<DeviceControlProps> = ({ device }) => {
  const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus>(ConnectionStatus.DISCONNECTED);
  const [deviceInfo, setDeviceInfo] = useState<any>(null);
  const [batteryInfo, setBatteryInfo] = useState<any>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);

  useEffect(() => {
    // Setup event listeners
    const unsubscribeConnectionState = zhSDKService.addEventListener(
      'onConnectionStateChanged',
      (state) => {
        console.log('🔗 Connection state:', state);
        const serviceStatus = zhSDKService.currentConnectionStatus;
        console.log('📊 Service status updated to:', serviceStatus);
        setConnectionStatus(serviceStatus); // Keep local state in sync for re-renders
      }
    );

    const unsubscribeDeviceInfo = zhSDKService.addEventListener(
      'onDeviceInfoReceived',
      (info) => {
        console.log('📱 Device info:', info);
        setDeviceInfo(info);
        setIsLoading(false);
      }
    );

    const unsubscribeBatteryInfo = zhSDKService.addEventListener(
      'onBatteryInfoReceived',
      (info) => {
        console.log('🔋 Battery info:', info);
        setBatteryInfo(info);
      }
    );

    const unsubscribeTimeSet = zhSDKService.addEventListener(
      'onTimeSetCompleted',
      (result) => {
        console.log('⏰ Time set result:', result);
        setIsLoading(false);
        if (result.success) {
          Alert.alert('Success', 'Time synchronized successfully!');
        } else {
          Alert.alert('Error', 'Failed to synchronize time');
        }
      }
    );

    // Listen for binding completion events to update status
    const unsubscribeBindingComplete = zhSDKService.addEventListener(
      'onOneKeyBindCompleted',
      (result) => {
        console.log('🔗 Binding completed, updating status:', result);
        const serviceStatus = zhSDKService.currentConnectionStatus;
        console.log('📊 Service status after binding:', serviceStatus);
        setConnectionStatus(serviceStatus);
      }
    );

    const unsubscribeBindingSuccess = zhSDKService.addEventListener(
      'onDeviceBindSuccessByBerry',
      (result) => {
        console.log('🔗 Binding success, updating status:', result);
        const serviceStatus = zhSDKService.currentConnectionStatus;
        console.log('📊 Service status after binding success:', serviceStatus);
        setConnectionStatus(serviceStatus);
      }
    );

    // Initialize connection status
    setConnectionStatus(zhSDKService.currentConnectionStatus);

    return () => {
      unsubscribeConnectionState.remove();
      unsubscribeDeviceInfo.remove();
      unsubscribeBatteryInfo.remove();
      unsubscribeTimeSet.remove();
      unsubscribeBindingComplete.remove();
      unsubscribeBindingSuccess.remove();
    };
  }, []);

  const handleCheckConnection = async () => {
    try {
      // First refresh the service status
      await zhSDKService.refreshConnectionStatus();
      
      const isConnected = await zhSDKService.isDeviceConnected();
      const serviceStatus = zhSDKService.currentConnectionStatus;
      const connectedDevice = zhSDKService.currentConnectedDevice;
      const localStatus = connectionStatus; // Local component state
      
      const statusText = isConnected ? 'Connected' : 'Disconnected';
      const detailText = `Native check: ${statusText}\nService status: ${serviceStatus}\nLocal status: ${localStatus}\nDevice: ${connectedDevice?.name || 'None'}`;
      
      console.log('🔍 Connection check details:', {
        isConnected,
        serviceStatus,
        localStatus,
        connectedDevice: connectedDevice?.name
      });
      
      // Force sync local state with service state
      setConnectionStatus(serviceStatus);
      
      Alert.alert('Connection Status', detailText);
    } catch (error) {
      Alert.alert('Error', 'Failed to check connection status');
    }
  };

  const handleGetDeviceInfo = async () => {
    // Check both local state and service status
    const serviceStatus = zhSDKService.currentConnectionStatus;
    const connectedDevice = zhSDKService.currentConnectedDevice;
    
    console.log('🔍 Device info check - Status:', serviceStatus, 'Device:', connectedDevice?.name);
    
    if (serviceStatus !== ConnectionStatus.CONNECTED && serviceStatus !== ConnectionStatus.BOUND) {
      Alert.alert('Error', `Device is not connected. Current status: ${serviceStatus}`);
      return;
    }

    if (!connectedDevice) {
      Alert.alert('Error', 'No device is currently connected');
      return;
    }

    setIsLoading(true);
    try {
      await zhSDKService.getDeviceInfo();
      // Response will be handled by event listener
    } catch (error) {
      setIsLoading(false);
      Alert.alert('Error', 'Failed to get device information');
    }
  };

  const handleSetTime = async () => {
    // Check both local state and service status
    const serviceStatus = zhSDKService.currentConnectionStatus;
    const connectedDevice = zhSDKService.currentConnectedDevice;
    
    console.log('⏰ Time sync check - Status:', serviceStatus, 'Device:', connectedDevice?.name);
    
    if (serviceStatus !== ConnectionStatus.CONNECTED && serviceStatus !== ConnectionStatus.BOUND) {
      Alert.alert('Error', `Device is not connected. Current status: ${serviceStatus}`);
      return;
    }

    if (!connectedDevice) {
      Alert.alert('Error', 'No device is currently connected');
      return;
    }

    setIsLoading(true);
    try {
      await zhSDKService.setTime();
      // Response will be handled by event listener
    } catch (error) {
      setIsLoading(false);
      Alert.alert('Error', 'Failed to set time');
    }
  };

  const handleSetTimeFormat = async (is24Hour: boolean) => {
    // Check both local state and service status
    const serviceStatus = zhSDKService.currentConnectionStatus;
    const connectedDevice = zhSDKService.currentConnectedDevice;
    
    if (serviceStatus !== ConnectionStatus.CONNECTED && serviceStatus !== ConnectionStatus.BOUND) {
      Alert.alert('Error', `Device is not connected. Current status: ${serviceStatus}`);
      return;
    }

    if (!connectedDevice) {
      Alert.alert('Error', 'No device is currently connected');
      return;
    }

    try {
      await zhSDKService.setTimeFormat(is24Hour);
      Alert.alert('Success', `Time format set to ${is24Hour ? '24-hour' : '12-hour'} format`);
    } catch (error) {
      Alert.alert('Error', 'Failed to set time format');
    }
  };

  const handleReconnect = async () => {
    setIsLoading(true);
    try {
      console.log('🔄 Manual reconnection triggered by user');
      const success = await zhSDKService.reconnectToLastDevice();
      
      if (success) {
        Alert.alert('Success', 'Reconnected to device successfully');
      } else {
        Alert.alert('Error', 'Failed to reconnect to device. Try scanning for devices again.');
      }
    } catch (error) {
      console.error('❌ Reconnection error:', error);
      Alert.alert('Error', 'Failed to reconnect to device');
    } finally {
      setIsLoading(false);
    }
  };

  const handleClearPersistence = async () => {
    Alert.alert(
      'Forget Device',
      'This will clear the saved connection and the app will not automatically reconnect to this device. Continue?',
      [
        { text: 'Cancel', style: 'cancel' },
        { 
          text: 'Clear', 
          style: 'destructive',
          onPress: async () => {
            try {
              await zhSDKService.clearConnectionPersistence();
              Alert.alert('Success', 'Device connection cleared. The app will no longer auto-reconnect to this device.');
            } catch (error) {
              console.error('❌ Clear persistence error:', error);
              Alert.alert('Error', 'Failed to clear device connection');
            }
          }
        }
      ]
    );
  };

  const getConnectionStatusColor = () => {
    // Use real-time status from service instead of local state
    const currentStatus = zhSDKService.currentConnectionStatus;
    switch (currentStatus) {
      case ConnectionStatus.CONNECTED:
      case ConnectionStatus.BOUND:
        return '#10b981';
      case ConnectionStatus.CONNECTING:
      case ConnectionStatus.BINDING:
        return '#f59e0b';
      default:
        return '#ef4444';
    }
  };

  const getConnectionStatusText = () => {
    // Use real-time status from service instead of local state
    const currentStatus = zhSDKService.currentConnectionStatus;
    switch (currentStatus) {
      case ConnectionStatus.CONNECTED:
        return 'Connected';
      case ConnectionStatus.BOUND:
        return 'Connected & Bound';
      case ConnectionStatus.CONNECTING:
        return 'Connecting...';
      case ConnectionStatus.BINDING:
        return 'Binding...';
      default:
        return 'Disconnected';
    }
  };

  const getBatteryStatusText = (chargeStatus: number) => {
    switch (chargeStatus) {
      case 1:
        return 'Charging';
      case 2:
        return 'Not Charging';
      case 3:
        return 'Full';
      default:
        return 'Unknown';
    }
  };

  if (!device) {
    return (
      <View style={styles.container}>
        <Text style={styles.noDeviceText}>No device connected</Text>
      </View>
    );
  }

  return (
    <ScrollView style={styles.container}>
      <View style={styles.content}>
        {/* Device Status */}
        <View style={styles.statusCard}>
          <Text style={styles.cardTitle}>Device Status</Text>
          <View style={styles.statusRow}>
            <Text style={styles.label}>Device:</Text>
            <Text style={styles.value}>{device.name}</Text>
          </View>
          <View style={styles.statusRow}>
            <Text style={styles.label}>Address:</Text>
            <Text style={styles.value}>{device.address}</Text>
          </View>
          <View style={styles.statusRow}>
            <Text style={styles.label}>Status:</Text>
            <View style={[styles.statusIndicator, { backgroundColor: getConnectionStatusColor() }]}>
              <Text style={styles.statusText}>{getConnectionStatusText()}</Text>
            </View>
          </View>
        </View>

        {/* Device Information */}
        {deviceInfo && (
          <View style={styles.infoContainer}>
            <Text style={styles.infoTitle}>Device Information</Text>
            <Text style={styles.infoText}>Equipment Number: {deviceInfo.equipmentNumber || 'Unknown'}</Text>
            <Text style={styles.infoText}>Firmware Version: {deviceInfo.firmwareVersion || 'Unknown'}</Text>
            <Text style={styles.infoText}>Serial Number: {deviceInfo.serialNumber || 'Unknown'}</Text>
            <Text style={styles.infoText}>MAC Address: {deviceInfo.macAddress || 'Unknown'}</Text>
          </View>
        )}

        {/* Battery Information */}
        {batteryInfo && (
          <View style={styles.batteryCard}>
            <Text style={styles.cardTitle}>Battery Status</Text>
            <View style={styles.batteryRow}>
              <Text style={styles.label}>Capacity:</Text>
              <Text style={styles.batteryValue}>{batteryInfo.capacity}%</Text>
            </View>
            <View style={styles.batteryRow}>
              <Text style={styles.label}>Status:</Text>
              <Text style={styles.value}>{getBatteryStatusText(batteryInfo.chargeStatus)}</Text>
            </View>
          </View>
        )}

        {/* Control Buttons */}
        <View style={styles.buttonContainer}>
          <TouchableOpacity style={styles.primaryButton} onPress={handleCheckConnection}>
            <Text style={styles.buttonText}>Check Connection</Text>
          </TouchableOpacity>

          <TouchableOpacity 
            style={[styles.primaryButton, styles.reconnectButton, isLoading && styles.disabledButton]} 
            onPress={handleReconnect}
            disabled={isLoading}
          >
            <Text style={styles.buttonText}>
              {isLoading ? 'Reconnecting...' : 'Reconnect'}
            </Text>
          </TouchableOpacity>

          <TouchableOpacity 
            style={[styles.primaryButton, isLoading && styles.disabledButton]} 
            onPress={handleGetDeviceInfo}
            disabled={isLoading}
          >
            <Text style={styles.buttonText}>
              {isLoading ? 'Getting Info...' : 'Get Device Info'}
            </Text>
          </TouchableOpacity>

          <TouchableOpacity 
            style={[styles.primaryButton, styles.timeButton, isLoading && styles.disabledButton]} 
            onPress={handleSetTime}
            disabled={isLoading}
          >
            <Text style={styles.buttonText}>
              {isLoading ? 'Setting Time...' : 'Sync Time'}
            </Text>
          </TouchableOpacity>

          <View style={styles.timeFormatContainer}>
            <Text style={styles.sectionTitle}>Time Format</Text>
            <View style={styles.timeFormatButtons}>
              <TouchableOpacity 
                style={styles.formatButton} 
                onPress={() => handleSetTimeFormat(false)}
              >
                <Text style={styles.formatButtonText}>12-Hour</Text>
              </TouchableOpacity>
              <TouchableOpacity 
                style={styles.formatButton} 
                onPress={() => handleSetTimeFormat(true)}
              >
                <Text style={styles.formatButtonText}>24-Hour</Text>
              </TouchableOpacity>
            </View>
          </View>

          <TouchableOpacity 
            style={[styles.primaryButton, styles.forgetButton]} 
            onPress={handleClearPersistence}
          >
            <Text style={styles.buttonText}>Forget Device</Text>
          </TouchableOpacity>
        </View>
      </View>
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0f172a',
  },
  content: {
    padding: 20,
  },
  noDeviceText: {
    color: '#9ca3af',
    fontSize: 16,
    textAlign: 'center',
    marginTop: 50,
  },
  statusCard: {
    backgroundColor: '#1e293b',
    borderRadius: 12,
    padding: 16,
    marginBottom: 16,
  },
  infoCard: {
    backgroundColor: '#1e293b',
    borderRadius: 12,
    padding: 16,
    marginBottom: 16,
  },
  batteryCard: {
    backgroundColor: '#1e293b',
    borderRadius: 12,
    padding: 16,
    marginBottom: 20,
  },
  cardTitle: {
    color: '#ffffff',
    fontSize: 18,
    fontWeight: 'bold',
    marginBottom: 12,
  },
  statusRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  infoRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 8,
  },
  batteryRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  label: {
    color: '#9ca3af',
    fontSize: 14,
  },
  value: {
    color: '#ffffff',
    fontSize: 14,
    fontWeight: '500',
  },
  batteryValue: {
    color: '#10b981',
    fontSize: 16,
    fontWeight: 'bold',
  },
  statusIndicator: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 12,
  },
  statusText: {
    color: '#ffffff',
    fontSize: 12,
    fontWeight: '600',
  },
  buttonContainer: {
    gap: 12,
  },
  primaryButton: {
    backgroundColor: '#3b82f6',
    paddingVertical: 14,
    paddingHorizontal: 20,
    borderRadius: 8,
    alignItems: 'center',
  },
  timeButton: {
    backgroundColor: '#10b981',
  },
  reconnectButton: {
    backgroundColor: '#f59e0b',
  },
  forgetButton: {
    backgroundColor: '#ef4444',
  },
  disabledButton: {
    backgroundColor: '#6b7280',
    opacity: 0.6,
  },
  buttonText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '600',
  },
  timeFormatContainer: {
    marginTop: 8,
  },
  sectionTitle: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 12,
  },
  timeFormatButtons: {
    flexDirection: 'row',
    gap: 12,
  },
  formatButton: {
    flex: 1,
    backgroundColor: '#374151',
    paddingVertical: 12,
    borderRadius: 8,
    alignItems: 'center',
  },
  formatButtonText: {
    color: '#d1d5db',
    fontSize: 14,
    fontWeight: '600',
  },
  infoContainer: {
    backgroundColor: '#374151',
    borderRadius: 12,
    padding: 16,
    marginBottom: 16,
  },
  infoTitle: {
    color: '#ffffff',
    fontSize: 18,
    fontWeight: 'bold',
    marginBottom: 12,
    textAlign: 'center',
  },
  infoText: {
    color: '#d1d5db',
    fontSize: 14,
    marginBottom: 8,
  },
});

export default DeviceControl;
