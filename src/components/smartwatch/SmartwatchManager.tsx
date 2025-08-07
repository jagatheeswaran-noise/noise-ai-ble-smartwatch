import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  Modal,
  StatusBar,
  Alert,
} from 'react-native';
import DeviceScanner from './DeviceScanner';
import DeviceControl from './DeviceControl';
import { zhSDKService } from '../../services/ZHSDKService';
import { ConnectionStatus, DeviceInfo } from '../../types/smartwatch';

interface SmartwatchManagerProps {
  visible: boolean;
  onClose: () => void;
}

const SmartwatchManager: React.FC<SmartwatchManagerProps> = ({ visible, onClose }) => {
  const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus>(ConnectionStatus.DISCONNECTED);
  const [connectedDevice, setConnectedDevice] = useState<DeviceInfo | null>(null);
  const [isBluetoothEnabled, setIsBluetoothEnabled] = useState<boolean>(false);
  const [hasPermissions, setHasPermissions] = useState<boolean>(false);
  const [currentView, setCurrentView] = useState<'scanner' | 'control'>('scanner');

  useEffect(() => {
    if (visible) {
      checkInitialState();
    }
  }, [visible]);

  const checkInitialState = async () => {
    try {
      // Check Bluetooth status
      const bluetoothEnabled = await zhSDKService.isBluetoothEnabled();
      setIsBluetoothEnabled(bluetoothEnabled);

      // Check permissions
      const permissions = await zhSDKService.checkBluetoothPermissions();
      setHasPermissions(permissions);

      // Update connection status
      setConnectionStatus(zhSDKService.currentConnectionStatus);
      setConnectedDevice(zhSDKService.currentConnectedDevice);

      // Switch to control view if connected
      if (zhSDKService.currentConnectionStatus === ConnectionStatus.CONNECTED || 
          zhSDKService.currentConnectionStatus === ConnectionStatus.BOUND) {
        setCurrentView('control');
      }

      if (!bluetoothEnabled) {
        Alert.alert(
          'Bluetooth Required',
          'Please enable Bluetooth to scan for devices.',
          [{ text: 'OK' }]
        );
        return;
      }

      if (!permissions) {
        Alert.alert(
          'Permissions Required',
          'This app needs Bluetooth and Location permissions to scan for devices.',
          [
            { text: 'Cancel', style: 'cancel' },
            { 
              text: 'Grant Permissions', 
              onPress: () => requestPermissions() 
            }
          ]
        );
      }
    } catch (error) {
      console.error('Error checking initial state:', error);
    }
  };

  const handleConnectionSuccess = (device: DeviceInfo) => {
    console.log('✅ Device connected successfully:', device);
    setConnectedDevice(device);
    setConnectionStatus(zhSDKService.currentConnectionStatus);
    setCurrentView('control');
  };

  const handleBackToScanner = () => {
    setCurrentView('scanner');
  };

  const handleDisconnect = async () => {
    try {
      await zhSDKService.disconnectDevice();
      setConnectedDevice(null);
      setConnectionStatus(ConnectionStatus.DISCONNECTED);
      setCurrentView('scanner');
    } catch (error) {
      console.error('Error disconnecting:', error);
    }
  };

  const requestPermissions = async () => {
    try {
      const granted = await zhSDKService.requestBluetoothPermissions();
      setHasPermissions(granted);
      
      if (!granted) {
        Alert.alert(
          'Permissions Denied',
          'Bluetooth and Location permissions are required to scan for devices.',
          [{ text: 'OK' }]
        );
      }
    } catch (error) {
      console.error('Error requesting permissions:', error);
    }
  };

  const getConnectionStatusText = () => {
    switch (connectionStatus) {
      case ConnectionStatus.CONNECTED:
        return 'Connected';
      case ConnectionStatus.CONNECTING:
        return 'Connecting...';
      case ConnectionStatus.BINDING:
        return 'Binding...';
      case ConnectionStatus.BOUND:
        return 'Bound Successfully';
      default:
        return 'Disconnected';
    }
  };

  const getConnectionStatusColor = () => {
    switch (connectionStatus) {
      case ConnectionStatus.CONNECTED:
      case ConnectionStatus.BOUND:
        return '#4ade80'; // Green
      case ConnectionStatus.CONNECTING:
      case ConnectionStatus.BINDING:
        return '#f59e0b'; // Orange
      default:
        return '#ef4444'; // Red
    }
  };

  const handleClose = async () => {
    // Stop any ongoing scan when closing
    if (zhSDKService.currentScanStatus === 'scanning') {
      await zhSDKService.stopDeviceScan();
    }
    onClose();
  };

  return (
    <Modal
      visible={visible}
      animationType="slide"
      presentationStyle="fullScreen"
      onRequestClose={handleClose}
    >
      <StatusBar barStyle="light-content" backgroundColor="#1f2937" />
      <View style={styles.container}>
        <View style={styles.header}>
          <TouchableOpacity onPress={handleClose} style={styles.closeButton}>
            <Text style={styles.closeButtonText}>✕</Text>
          </TouchableOpacity>
          <Text style={styles.title}>
            {currentView === 'control' ? 'Device Control' : 'ZH SDK Device Scanner'}
          </Text>
          {currentView === 'control' && connectedDevice && (
            <TouchableOpacity onPress={handleBackToScanner} style={styles.backButton}>
              <Text style={styles.backButtonText}>←</Text>
            </TouchableOpacity>
          )}
          {currentView === 'scanner' && (
            <View style={styles.placeholder} />
          )}
        </View>

        <View style={styles.statusContainer}>
          <Text style={styles.statusLabel}>Connection Status:</Text>
          <Text style={[styles.statusText, { color: getConnectionStatusColor() }]}>
            {getConnectionStatusText()}
          </Text>
        </View>

        {!isBluetoothEnabled ? (
          <View style={styles.errorContainer}>
            <Text style={styles.errorText}>
              Bluetooth is not enabled. Please enable Bluetooth in your device settings.
            </Text>
          </View>
        ) : !hasPermissions ? (
          <View style={styles.errorContainer}>
            <Text style={styles.errorText}>
              Bluetooth and Location permissions are required.
            </Text>
            <TouchableOpacity style={styles.permissionButton} onPress={requestPermissions}>
              <Text style={styles.permissionButtonText}>Grant Permissions</Text>
            </TouchableOpacity>
          </View>
        ) : currentView === 'control' ? (
          <DeviceControl 
            device={connectedDevice}
          />
        ) : (
          <DeviceScanner 
            onConnectionStatusChange={setConnectionStatus}
            onConnectionSuccess={handleConnectionSuccess}
          />
        )}
      </View>
    </Modal>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#1f2937',
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 20,
    paddingTop: 60,
    paddingBottom: 20,
    backgroundColor: '#374151',
    borderBottomWidth: 1,
    borderBottomColor: '#4b5563',
  },
  closeButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: '#4b5563',
    alignItems: 'center',
    justifyContent: 'center',
  },
  closeButtonText: {
    color: '#ffffff',
    fontSize: 18,
    fontWeight: 'bold',
  },
  title: {
    color: '#ffffff',
    fontSize: 18,
    fontWeight: 'bold',
    textAlign: 'center',
  },
  placeholder: {
    width: 40,
  },
  statusContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingVertical: 12,
    backgroundColor: '#374151',
  },
  statusLabel: {
    color: '#d1d5db',
    fontSize: 14,
    marginRight: 8,
  },
  statusText: {
    fontSize: 14,
    fontWeight: '600',
  },
  errorContainer: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 20,
  },
  errorText: {
    color: '#ef4444',
    fontSize: 16,
    textAlign: 'center',
    marginBottom: 20,
    lineHeight: 24,
  },
  permissionButton: {
    backgroundColor: '#4ade80',
    paddingHorizontal: 24,
    paddingVertical: 12,
    borderRadius: 8,
    marginTop: 10,
  },
  permissionButtonText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '600',
  },
  backButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: '#374151',
    alignItems: 'center',
    justifyContent: 'center',
  },
  backButtonText: {
    color: '#ffffff',
    fontSize: 18,
    fontWeight: 'bold',
  },
});

export default SmartwatchManager;
