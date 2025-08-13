import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  Alert,
  ScrollView,
} from 'react-native';
import RNFS from 'react-native-fs';
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
  const [availableBinFiles, setAvailableBinFiles] = useState<string[]>([]);
  const [selectedBinFile, setSelectedBinFile] = useState<string>('');
  const [isScanningFiles, setIsScanningFiles] = useState<boolean>(false);

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

  const scanForBinFiles = async () => {
    try {
      setIsScanningFiles(true);
      console.log('🔍 Scanning for BIN files...');
      
      // Use the same path as ModelManager for consistency
      const downloadsDir = `${RNFS.DownloadDirectoryPath}/NoiseAI_WatchRecordings`;
      const documentsDir = `${RNFS.DocumentDirectoryPath}/watch_recordings`;
      
      let binFiles: string[] = [];
      
      try {
        // Check Downloads/NoiseAI_WatchRecordings directory
        const downloadsExists = await RNFS.exists(downloadsDir);
        if (downloadsExists) {
          const files = await RNFS.readDir(downloadsDir);
          binFiles = files
            .filter(file => file.isFile() && file.name.endsWith('.bin'))
            .map(file => file.path);
          console.log('📁 Found BIN files in Downloads:', binFiles);
        }
      } catch (error) {
        console.log('📁 Downloads/NoiseAI_WatchRecordings directory not accessible:', error);
      }
      
      // Also check Documents/watch_recordings directory
      try {
        const documentsExists = await RNFS.exists(documentsDir);
        if (documentsExists) {
          const files = await RNFS.readDir(documentsDir);
          const docBinFiles = files
            .filter(file => file.isFile() && file.name.endsWith('.bin'))
            .map(file => file.path);
          binFiles = [...binFiles, ...docBinFiles];
          console.log('📁 Found BIN files in Documents:', docBinFiles);
        }
      } catch (error) {
        console.log('📁 Documents/watch_recordings directory not accessible:', error);
      }
      
      // Sort by modification time (newest first)
      binFiles.sort((a, b) => {
        try {
          const statA = RNFS.stat(a);
          const statB = RNFS.stat(b);
          return statB.mtime.getTime() - statA.mtime.getTime();
        } catch (error) {
          return 0; // If we can't get stats, don't change order
        }
      });
      
      console.log('📋 Total BIN files found:', binFiles);
      setAvailableBinFiles(binFiles);
      
      if (binFiles.length > 0) {
        setSelectedBinFile(binFiles[0]); // Select the first (newest) file
        Alert.alert('Success', `Found ${binFiles.length} BIN file(s)`);
      } else {
        Alert.alert('No Files', 'No BIN files found in NoiseAI_WatchRecordings or watch_recordings folders');
      }
      
    } catch (error) {
      console.error('❌ Error scanning for BIN files:', error);
      Alert.alert('Error', `Failed to scan for BIN files: ${error.message}`);
    } finally {
      setIsScanningFiles(false);
    }
  };

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

  const handleTestOpusBridge = async () => {
    try {
      if (!selectedBinFile) {
        Alert.alert('No File Selected', 'Please scan for BIN files and select one first');
        return;
      }
      
      console.log('🎵 Testing OpusBridge module...');
      
      // Import NativeModules dynamically to avoid build issues
      const { NativeModules } = require('react-native');
      const { OpusBridge } = NativeModules;
      
      if (!OpusBridge) {
        Alert.alert('Error', 'OpusBridge module not found!');
        return;
      }
      
      console.log('✅ OpusBridge module found');
      console.log('Available methods:', Object.keys(OpusBridge));
      
      // Source file (user selected)
      const srcBinFile = selectedBinFile;
      
      // Destination files in app's sandbox (guaranteed accessible)
      const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
      const dstBinFile = `${RNFS.DocumentDirectoryPath}/opus_input_${timestamp}.bin`;
      const dstWavFile = `${RNFS.DocumentDirectoryPath}/opus_output_${timestamp}.wav`;
      
      console.log('📁 Source file:', srcBinFile);
      console.log('📁 Sandbox BIN:', dstBinFile);
      console.log('📁 Sandbox WAV:', dstWavFile);
      
      // Step 1: Verify source exists and is accessible
      let sourceStat;
      try {
        sourceStat = await RNFS.stat(srcBinFile);
        if (!sourceStat || !sourceStat.isFile()) {
          throw new Error('Source BIN not found or not accessible');
        }
        console.log('✅ Source file accessible, size:', sourceStat.size, 'bytes');
      } catch (statError) {
        console.error('❌ Source file access failed:', statError);
        Alert.alert('File Access Error', 
          'Cannot access the selected .bin file.\n\n' +
          'This is likely due to Android scoped storage restrictions.\n\n' +
          'Try selecting a different file or copying it to a different location.'
        );
        return;
      }
      
      // Step 2: Copy into app's sandbox (guaranteed readable by native fopen)
      try {
        console.log('📋 Copying file to sandbox...');
        await RNFS.copyFile(srcBinFile, dstBinFile);
        console.log('✅ File copied to sandbox');
      } catch (copyError) {
        console.error('❌ File copy failed:', copyError);
        Alert.alert('Copy Error', `Failed to copy file to sandbox: ${copyError.message}`);
        return;
      }
      
      // Step 3: Decode using the sandbox file (guaranteed accessible)
      try {
        console.log('🎵 Decoding with OpusBridge...');
        const result = await OpusBridge.decodeBinToWav(dstBinFile, dstWavFile, { bytesPerPacket: 80 });
        
        if (result.wavPath) {
          Alert.alert(
            'Success! 🎉', 
            `OpusBridge test completed successfully!\n\n` +
            `Input: ${srcBinFile}\n` +
            `Output: ${result.wavPath}\n` +
            `Input Size: ${sourceStat.size} bytes\n` +
            `Output Size: ${result.outputSize || 'Unknown'} bytes\n` +
            `Bytes per packet: ${result.bytesPerPacket || 80}`
          );
          console.log('🎵 OpusBridge test result:', result);
        } else {
          Alert.alert('Error', `OpusBridge test failed: ${result}`);
          console.log('❌ OpusBridge test failed:', result);
        }
      } catch (decodeError) {
        console.error('❌ Decode failed:', decodeError);
        Alert.alert('Decode Error', `OpusBridge decode failed: ${decodeError.message}`);
      }
      
    } catch (error) {
      console.error('❌ OpusBridge test error:', error);
      Alert.alert('Error', `OpusBridge test failed: ${error.message}`);
    }
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

  // Helper function to safely decode Opus files with sandbox copy
  const decodeOpusFileSafely = async (sourcePath: string, bytesPerPacket: number = 80) => {
    try {
      const RNFS = require('react-native-fs');
      
      // Generate unique filenames in app's sandbox
      const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
      const dstBinFile = `${RNFS.DocumentDirectoryPath}/opus_input_${timestamp}.bin`;
      const dstWavFile = `${RNFS.DocumentDirectoryPath}/opus_output_${timestamp}.wav`;
      
      console.log('📁 Source:', sourcePath);
      console.log('📁 Sandbox BIN:', dstBinFile);
      console.log('📁 Sandbox WAV:', dstWavFile);
      
      // Step 1: Verify source exists and is accessible
      const sourceStat = await RNFS.stat(sourcePath);
      if (!sourceStat || !sourceStat.isFile()) {
        throw new Error('Source file not found or not accessible');
      }
      console.log('✅ Source accessible, size:', sourceStat.size, 'bytes');
      
      // Step 2: Copy into app's sandbox
      await RNFS.copyFile(sourcePath, dstBinFile);
      console.log('✅ File copied to sandbox');
      
      // Step 3: Decode using sandbox file
      const { NativeModules } = require('react-native');
      const { OpusBridge } = NativeModules;
      
      const result = await OpusBridge.decodeBinToWav(dstBinFile, dstWavFile, { bytesPerPacket });
      console.log('✅ Decode successful:', result);
      
      return {
        success: true,
        inputPath: dstBinFile,
        outputPath: result.wavPath,
        bytesPerPacket,
        sourceSize: sourceStat.size
      };
      
    } catch (error) {
      console.error('❌ Safe decode failed:', error);
      return {
        success: false,
        error: error.message
      };
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

          {/* File Picker Section */}
          <View style={styles.infoContainer}>
            <Text style={styles.infoTitle}>📁 BIN File Picker</Text>
            <Text style={styles.infoText}>
              Scan for available .bin files and select one to convert
            </Text>
            
            <TouchableOpacity 
              style={[styles.primaryButton, { backgroundColor: '#059669' }]} 
              onPress={scanForBinFiles}
              disabled={isScanningFiles}
            >
              <Text style={styles.buttonText}>
                {isScanningFiles ? '🔍 Scanning...' : '🔍 Scan for BIN Files'}
              </Text>
            </TouchableOpacity>
            
            {availableBinFiles.length > 0 && (
              <View style={styles.fileListContainer}>
                <Text style={styles.fileListTitle}>Available Files:</Text>
                {availableBinFiles.map((filePath, index) => {
                  const fileName = filePath.split('/').pop() || 'Unknown';
                  const isSelected = filePath === selectedBinFile;
                  
                  return (
                    <TouchableOpacity
                      key={index}
                      style={[
                        styles.fileItem,
                        isSelected && styles.selectedFileItem
                      ]}
                      onPress={() => setSelectedBinFile(filePath)}
                    >
                      <Text style={[
                        styles.fileName,
                        isSelected && styles.selectedFileName
                      ]}>
                        {fileName}
                      </Text>
                      <Text style={styles.filePath}>{filePath}</Text>
                    </TouchableOpacity>
                  );
                })}
              </View>
            )}
          </View>

          {/* OpusBridge Test Section */}
          <View style={styles.infoContainer}>
            <Text style={styles.infoTitle}>🎵 OpusBridge Test</Text>
            <Text style={styles.infoText}>
              Test the native OpusBridge module with your selected .bin file
            </Text>
            
            {selectedBinFile ? (
              <View style={styles.selectedFileInfo}>
                <Text style={styles.selectedFileLabel}>Selected:</Text>
                <Text style={styles.selectedFilePath}>{selectedBinFile.split('/').pop()}</Text>
              </View>
            ) : (
              <Text style={styles.noFileSelected}>No file selected</Text>
            )}
            
            <TouchableOpacity 
              style={[
                styles.primaryButton, 
                { backgroundColor: '#8b5cf6' },
                !selectedBinFile && styles.disabledButton
              ]} 
              onPress={handleTestOpusBridge}
              disabled={!selectedBinFile}
            >
              <Text style={styles.buttonText}>
                {selectedBinFile ? 'Test OpusBridge' : 'Select a file first'}
              </Text>
            </TouchableOpacity>
          </View>


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
  fileListContainer: {
    marginTop: 16,
    padding: 12,
    backgroundColor: '#1f2937',
    borderRadius: 8,
  },
  fileListTitle: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 12,
  },
  fileItem: {
    padding: 12,
    backgroundColor: '#374151',
    borderRadius: 8,
    marginBottom: 8,
    borderWidth: 2,
    borderColor: 'transparent',
  },
  selectedFileItem: {
    borderColor: '#8b5cf6',
    backgroundColor: '#4c1d95',
  },
  fileName: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 4,
  },
  selectedFileName: {
    color: '#e9d5ff',
  },
  filePath: {
    color: '#9ca3af',
    fontSize: 12,
    fontFamily: 'monospace',
  },
  selectedFileInfo: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 12,
    padding: 12,
    backgroundColor: '#1f2937',
    borderRadius: 8,
  },
  selectedFileLabel: {
    color: '#9ca3af',
    fontSize: 14,
    marginRight: 8,
  },
  selectedFilePath: {
    color: '#10b981',
    fontSize: 14,
    fontWeight: '600',
    fontFamily: 'monospace',
  },
  noFileSelected: {
    color: '#6b7280',
    fontSize: 14,
    fontStyle: 'italic',
    textAlign: 'center',
    marginBottom: 12,
  },
});

export default DeviceControl;
