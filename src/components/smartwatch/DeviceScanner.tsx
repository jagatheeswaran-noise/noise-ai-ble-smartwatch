import React, { useState, useEffect, useCallback } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  FlatList,
  StyleSheet,
  Alert,
  ActivityIndicator,
} from 'react-native';
import { zhSDKService } from '../../services/ZHSDKService';
import { DeviceInfo, ConnectionStatus, ScanStatus } from '../../types/smartwatch';
import DeviceCard from './DeviceCard';
import ConnectionFlow from './ConnectionFlow';

interface DeviceScannerProps {
  onConnectionStatusChange: (status: ConnectionStatus) => void;
  onDeviceSelect?: (device: DeviceInfo) => void;
  onConnectionSuccess?: (device: DeviceInfo) => void;
}

const DeviceScanner: React.FC<DeviceScannerProps> = ({ 
  onConnectionStatusChange, 
  onDeviceSelect,
  onConnectionSuccess
}) => {
  const [scanStatus, setScanStatus] = useState<ScanStatus>(ScanStatus.IDLE);
  const [scannedDevices, setScannedDevices] = useState<DeviceInfo[]>([]);
  const [filteredDevices, setFilteredDevices] = useState<DeviceInfo[]>([]);
  const [searchText, setSearchText] = useState<string>('');
  const [scanTimer, setScanTimer] = useState<NodeJS.Timeout | null>(null);
  
  const SCAN_TIMEOUT = 15000; // 15 seconds auto-stop
  const [selectedDevice, setSelectedDevice] = useState<DeviceInfo | null>(null);
  const [showConnectionFlow, setShowConnectionFlow] = useState<boolean>(false);

  useEffect(() => {
    let deviceUpdateTimer: NodeJS.Timeout | null = null;
    
    // Throttled device list update - only update every 1000ms to prevent performance issues
    const throttledUpdate = () => {
      if (deviceUpdateTimer) clearTimeout(deviceUpdateTimer);
      deviceUpdateTimer = setTimeout(() => {
        updateDeviceList();
      }, 1000);
    };
    
    // Setup event listeners
    const deviceFoundSubscription = zhSDKService.addEventListener('onDeviceFound', (device: DeviceInfo) => {
      console.log('📱 New device found:', device);
      throttledUpdate(); // Use throttled update instead of immediate
    });

    const connectionStateSubscription = zhSDKService.addEventListener('onConnectionStateChanged', (state: any) => {
      console.log('🔗 Connection state changed:', state);
      onConnectionStatusChange(zhSDKService.currentConnectionStatus);
    });

    return () => {
      if (deviceUpdateTimer) clearTimeout(deviceUpdateTimer);
      deviceFoundSubscription.remove();
      connectionStateSubscription.remove();
    };
  }, [onConnectionStatusChange]);

  useEffect(() => {
    // Filter devices based on search text
    if (!searchText.trim()) {
      setFilteredDevices(scannedDevices);
    } else {
      const filtered = scannedDevices.filter(device => 
        device.name.toLowerCase().includes(searchText.toLowerCase()) ||
        device.address.toLowerCase().includes(searchText.toLowerCase())
      );
      setFilteredDevices(filtered);
    }
  }, [searchText, scannedDevices]);

  const updateDeviceList = useCallback(async () => {
    try {
      const devices = await zhSDKService.getScannedDevices();
      setScannedDevices(devices);
    } catch (error) {
      console.error('Error updating device list:', error);
    }
  }, []);

  const handleStartScan = async () => {
    try {
      setScanStatus(ScanStatus.SCANNING);
      setScannedDevices([]);
      setFilteredDevices([]);
      
      // Check SDK status first
      console.log('🔍 Checking SDK status...');
      const sdkAvailable = await zhSDKService.checkSDKStatus();
      console.log('📱 SDK Status:', sdkAvailable);
      
      if (!sdkAvailable) {
        setScanStatus(ScanStatus.IDLE);
        Alert.alert('Error', 'ZH SDK is not available. Please restart the app.');
        return;
      }
      
      // Check permissions
      console.log('🔍 Checking Bluetooth permissions...');
      const hasPermissions = await zhSDKService.checkBluetoothPermissions();
      console.log('🔐 Permissions:', hasPermissions);
      
      if (!hasPermissions) {
        setScanStatus(ScanStatus.IDLE);
        Alert.alert('Permissions Required', 'Bluetooth permissions are required to scan for devices.');
        return;
      }
      
      console.log('🔍 Starting device scan...');
      const started = await zhSDKService.startDeviceScan();
      console.log('📡 Scan started:', started);
      
      if (!started) {
        setScanStatus(ScanStatus.IDLE);
        Alert.alert('Error', 'Failed to start device scan. Please try again.');
      } else {
        // Auto-stop scan after 15 seconds
        const timer = setTimeout(() => {
          console.log('⏰ Auto-stopping scan after timeout');
          handleStopScan();
        }, SCAN_TIMEOUT);
        setScanTimer(timer);
      }
    } catch (error) {
      console.error('Error starting scan:', error);
      setScanStatus(ScanStatus.IDLE);
      Alert.alert('Error', 'Failed to start device scan.');
    }
  };

  const handleStopScan = async () => {
    try {
      console.log('🛑 Stopping device scan...');
      
      // Clear auto-stop timer
      if (scanTimer) {
        clearTimeout(scanTimer);
        setScanTimer(null);
      }
      
      setScanStatus(ScanStatus.STOPPED);
      const stopped = await zhSDKService.stopDeviceScan();
      console.log('🛑 Scan stopped:', stopped);
    } catch (error) {
      console.error('Error stopping scan:', error);
    }
  };

  const handleDeviceSelect = (device: DeviceInfo) => {
    // Just select the device, don't start connection yet
    setSelectedDevice(device);
    onDeviceSelect?.(device); // Notify parent component if needed
  };

  const handleConnectToDevice = () => {
    if (!selectedDevice) return;
    
    if (scanStatus === ScanStatus.SCANNING) {
      handleStopScan();
    }
    
    setShowConnectionFlow(true);
  };

  const handleConnectionComplete = (connected: boolean) => {
    setShowConnectionFlow(false);
    if (connected && selectedDevice && onConnectionSuccess) {
      onConnectionSuccess(selectedDevice);
    }
    setSelectedDevice(null);
  };

  const handleConnectionCancel = () => {
    setShowConnectionFlow(false);
    setSelectedDevice(null);
  };

  const renderDevice = ({ item }: { item: DeviceInfo }) => (
    <DeviceCard
      device={item}
      onSelect={() => handleDeviceSelect(item)}
      isSelected={selectedDevice?.address === item.address}
    />
  );

  const renderScanButton = () => {
    if (scanStatus === ScanStatus.SCANNING) {
      return (
        <TouchableOpacity style={[styles.scanButton, styles.stopButton]} onPress={handleStopScan}>
          <ActivityIndicator size="small" color="#ffffff" style={{ marginRight: 8 }} />
          <Text style={styles.scanButtonText}>Stop Scan</Text>
        </TouchableOpacity>
      );
    } else {
      return (
        <TouchableOpacity style={[styles.scanButton, styles.startButton]} onPress={handleStartScan}>
          <Text style={styles.scanButtonText}>Start Scan</Text>
        </TouchableOpacity>
      );
    }
  };

  if (showConnectionFlow && selectedDevice) {
    return (
      <ConnectionFlow
        device={selectedDevice}
        onComplete={handleConnectionComplete}
        onCancel={handleConnectionCancel}
      />
    );
  }

  return (
    <View style={styles.container}>
      <View style={styles.searchContainer}>
        <TextInput
          style={styles.searchInput}
          placeholder="Search device name or MAC address..."
          placeholderTextColor="#9ca3af"
          value={searchText}
          onChangeText={setSearchText}
          autoCapitalize="none"
          autoCorrect={false}
        />
      </View>

      <View style={styles.buttonContainer}>
        {renderScanButton()}
      </View>

      <View style={styles.deviceListContainer}>
        <Text style={styles.deviceListTitle}>
          {scanStatus === ScanStatus.SCANNING ? 'Scanning for devices...' : `Found ${filteredDevices.length} devices`}
        </Text>
        
        <FlatList
          data={filteredDevices}
          renderItem={renderDevice}
          keyExtractor={(item) => item.address}
          style={styles.deviceList}
          showsVerticalScrollIndicator={false}
          ListEmptyComponent={
            <View style={styles.emptyContainer}>
              <Text style={styles.emptyText}>
                {scanStatus === ScanStatus.SCANNING 
                  ? 'Looking for devices...' 
                  : 'No devices found. Tap "Start Scan" to search for devices.'
                }
              </Text>
            </View>
          }
        />
      </View>
      
      {/* Connect Button - only show when device is selected */}
      {selectedDevice && (
        <View style={styles.connectButtonContainer}>
          <TouchableOpacity 
            style={styles.connectButton} 
            onPress={handleConnectToDevice}
          >
            <Text style={styles.connectButtonText}>
              Connect to {selectedDevice.name}
            </Text>
          </TouchableOpacity>
        </View>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#1f2937',
  },
  searchContainer: {
    paddingHorizontal: 20,
    paddingVertical: 16,
  },
  searchInput: {
    backgroundColor: '#374151',
    borderRadius: 8,
    paddingHorizontal: 16,
    paddingVertical: 12,
    fontSize: 16,
    color: '#ffffff',
    borderWidth: 1,
    borderColor: '#4b5563',
  },
  buttonContainer: {
    paddingHorizontal: 20,
    paddingBottom: 16,
  },
  scanButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 12,
    paddingHorizontal: 24,
    borderRadius: 8,
  },
  startButton: {
    backgroundColor: '#4ade80',
  },
  stopButton: {
    backgroundColor: '#ef4444',
  },
  scanButtonText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '600',
  },
  deviceListContainer: {
    flex: 1,
    paddingHorizontal: 20,
  },
  deviceListTitle: {
    color: '#d1d5db',
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 12,
  },
  deviceList: {
    flex: 1,
  },
  emptyContainer: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 40,
  },
  emptyText: {
    color: '#9ca3af',
    fontSize: 16,
    textAlign: 'center',
    lineHeight: 24,
  },
  connectButtonContainer: {
    paddingHorizontal: 20,
    paddingVertical: 16,
    backgroundColor: '#111827',
    borderTopWidth: 1,
    borderTopColor: '#374151',
  },
  connectButton: {
    backgroundColor: '#4ade80',
    paddingVertical: 14,
    paddingHorizontal: 24,
    borderRadius: 8,
    alignItems: 'center',
  },
  connectButtonText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '600',
  },
});

export default DeviceScanner;
