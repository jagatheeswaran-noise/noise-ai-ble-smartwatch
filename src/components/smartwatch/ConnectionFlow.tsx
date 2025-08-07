import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  ActivityIndicator,
  StyleSheet,
  Alert,
  TextInput,
} from 'react-native';
import { zhSDKService } from '../../services/ZHSDKService';
import { DeviceInfo, ConnectionStatus } from '../../types/smartwatch';

interface ConnectionFlowProps {
  device: DeviceInfo;
  onComplete: (connected: boolean) => void;
  onCancel: () => void;
}

const ConnectionFlow: React.FC<ConnectionFlowProps> = ({
  device,
  onComplete,
  onCancel,
}) => {
  const [connectionStatus, setConnectionStatus] = useState<'connecting' | 'connected' | 'binding' | 'bound' | 'failed'>('connecting');
  const [connectionProgress, setConnectionProgress] = useState<string>('Initializing connection...');
  const [userId, setUserId] = useState<string>('');
  const [showUserIdInput, setShowUserIdInput] = useState<boolean>(false);

  useEffect(() => {
    connectToDevice();
    
    // Setup event listeners for binding events
    const unsubscribeOneKeyBind = zhSDKService.addEventListener(
      'onOneKeyBindCompleted',
      (result) => {
        console.log('🔑 One-key bind completed:', result);
        if (result.success) {
          setConnectionStatus('bound');
          setConnectionProgress('Device bound successfully!');
          setTimeout(() => {
            onComplete(true);
          }, 1500);
        } else {
          setConnectionStatus('failed');
          setConnectionProgress(`Binding failed: ${result.error || 'Unknown error'}`);
        }
      }
    );

    return () => {
      unsubscribeOneKeyBind.remove();
    };
  }, []);

  const connectToDevice = async () => {
    try {
      setConnectionProgress('Preparing to connect...');
      console.log('🔗 Starting connection to:', device.name);

      // Add a small delay for better UX
      await new Promise(resolve => setTimeout(resolve, 1000));

      setConnectionProgress('Connecting to device...');
      
      // Attempt to connect using the ZH SDK
      const connected = await zhSDKService.connectDevice(device);
      
      if (connected) {
        setConnectionProgress('Connection successful!');
        setConnectionStatus('connected');
        console.log('✅ Connected to device:', device.name);
        
        // For Berry protocol devices, show user ID input for binding
        if (device.protocol && device.protocol.toLowerCase().includes('berry')) {
          setConnectionProgress('Ready for binding...');
          setShowUserIdInput(true);
        } else {
          // For non-Berry devices, complete immediately
          setTimeout(() => {
            onComplete(true);
          }, 1500);
        }
      } else {
        throw new Error('Failed to connect to device');
      }
    } catch (error) {
      console.error('❌ Connection failed:', error);
      setConnectionStatus('failed');
      setConnectionProgress('Connection failed');
      
      Alert.alert(
        'Connection Failed',
        `Could not connect to ${device.name}. Please try again.`,
        [
          { text: 'Retry', onPress: connectToDevice },
          { text: 'Cancel', onPress: () => onComplete(false) }
        ]
      );
    }
  };

  const handleBind = async () => {
    if (!userId.trim()) {
      Alert.alert('Error', 'Please enter a User ID');
      return;
    }

    try {
      setConnectionStatus('binding');
      setConnectionProgress('Binding device...');
      setShowUserIdInput(false);

      console.log('🔑 Starting one-key bind with User ID:', userId);
      
      // Perform the one-key binding process for Berry protocol
      await zhSDKService.performOneKeyBindByBerry(userId.trim());
      
      // The result will be handled by the event listener
    } catch (error) {
      console.error('❌ Binding failed:', error);
      setConnectionStatus('failed');
      setConnectionProgress('Binding failed');
      
      Alert.alert(
        'Binding Failed',
        `Could not bind device ${device.name}. Please try again.`,
        [
          { text: 'Retry', onPress: () => setShowUserIdInput(true) },
          { text: 'Cancel', onPress: () => onComplete(false) }
        ]
      );
    }
  };

  const handleCancel = () => {
    console.log('🚫 Connection cancelled by user');
    onCancel();
  };

  return (
    <View style={styles.container}>
      <View style={styles.content}>
        <View style={styles.deviceInfo}>
          <Text style={styles.deviceName}>{device.name}</Text>
          <Text style={styles.deviceDetails}>
            {device.address} • {device.protocol || 'Unknown Protocol'}
          </Text>
          <Text style={styles.deviceSignal}>
            Signal: {device.rssi} dBm
          </Text>
        </View>

        <View style={styles.connectionContainer}>
          {connectionStatus === 'connecting' && (
            <>
              <ActivityIndicator size="large" color="#4ade80" />
              <Text style={styles.progressText}>{connectionProgress}</Text>
            </>
          )}

          {connectionStatus === 'connected' && !showUserIdInput && (
            <>
              <View style={styles.successIcon}>
                <Text style={styles.successText}>✅</Text>
              </View>
              <Text style={styles.progressText}>{connectionProgress}</Text>
            </>
          )}

          {connectionStatus === 'binding' && (
            <>
              <ActivityIndicator size="large" color="#3b82f6" />
              <Text style={styles.progressText}>{connectionProgress}</Text>
            </>
          )}

          {connectionStatus === 'bound' && (
            <>
              <View style={styles.successIcon}>
                <Text style={styles.successText}>🔗</Text>
              </View>
              <Text style={styles.progressText}>{connectionProgress}</Text>
            </>
          )}

          {connectionStatus === 'failed' && (
            <>
              <View style={styles.errorIcon}>
                <Text style={styles.errorText}>❌</Text>
              </View>
              <Text style={styles.errorProgressText}>{connectionProgress}</Text>
            </>
          )}
        </View>

        {showUserIdInput && (
          <View style={styles.userIdContainer}>
            <Text style={styles.userIdLabel}>Enter User ID for Binding:</Text>
            <TextInput
              style={styles.userIdInput}
              value={userId}
              onChangeText={setUserId}
              placeholder="e.g., user123"
              placeholderTextColor="#9ca3af"
              autoCapitalize="none"
              autoCorrect={false}
            />
            <TouchableOpacity style={styles.bindButton} onPress={handleBind}>
              <Text style={styles.bindButtonText}>Bind Device</Text>
            </TouchableOpacity>
          </View>
        )}

        <View style={styles.buttonContainer}>
          {connectionStatus === 'connecting' && (
            <TouchableOpacity style={styles.cancelButton} onPress={handleCancel}>
              <Text style={styles.cancelButtonText}>Cancel</Text>
            </TouchableOpacity>
          )}

          {connectionStatus === 'failed' && (
            <>
              <TouchableOpacity style={styles.retryButton} onPress={connectToDevice}>
                <Text style={styles.retryButtonText}>Retry</Text>
              </TouchableOpacity>
              <TouchableOpacity style={styles.cancelButton} onPress={handleCancel}>
                <Text style={styles.cancelButtonText}>Cancel</Text>
              </TouchableOpacity>
            </>
          )}
        </View>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#111827',
    padding: 20,
  },
  content: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  deviceInfo: {
    alignItems: 'center',
    marginBottom: 40,
    padding: 20,
    backgroundColor: '#1f2937',
    borderRadius: 12,
    width: '100%',
  },
  deviceName: {
    color: '#ffffff',
    fontSize: 20,
    fontWeight: 'bold',
    marginBottom: 8,
    textAlign: 'center',
  },
  deviceDetails: {
    color: '#9ca3af',
    fontSize: 14,
    textAlign: 'center',
    marginBottom: 4,
  },
  deviceSignal: {
    color: '#6b7280',
    fontSize: 12,
    textAlign: 'center',
  },
  connectionContainer: {
    alignItems: 'center',
    marginBottom: 40,
    minHeight: 100,
    justifyContent: 'center',
  },
  progressText: {
    color: '#d1d5db',
    fontSize: 16,
    marginTop: 16,
    textAlign: 'center',
  },
  errorProgressText: {
    color: '#ef4444',
    fontSize: 16,
    marginTop: 16,
    textAlign: 'center',
  },
  successIcon: {
    marginBottom: 8,
  },
  successText: {
    fontSize: 48,
  },
  errorIcon: {
    marginBottom: 8,
  },
  errorText: {
    fontSize: 48,
  },
  buttonContainer: {
    flexDirection: 'row',
    gap: 12,
    width: '100%',
  },
  cancelButton: {
    flex: 1,
    backgroundColor: '#374151',
    paddingVertical: 12,
    paddingHorizontal: 24,
    borderRadius: 8,
    alignItems: 'center',
  },
  cancelButtonText: {
    color: '#d1d5db',
    fontSize: 16,
    fontWeight: '600',
  },
  retryButton: {
    flex: 1,
    backgroundColor: '#4ade80',
    paddingVertical: 12,
    paddingHorizontal: 24,
    borderRadius: 8,
    alignItems: 'center',
  },
  retryButtonText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '600',
  },
  userIdContainer: {
    width: '100%',
    marginBottom: 20,
    padding: 20,
    backgroundColor: '#1f2937',
    borderRadius: 12,
  },
  userIdLabel: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 12,
    textAlign: 'center',
  },
  userIdInput: {
    backgroundColor: '#374151',
    borderRadius: 8,
    paddingVertical: 12,
    paddingHorizontal: 16,
    fontSize: 16,
    color: '#ffffff',
    marginBottom: 16,
    borderWidth: 1,
    borderColor: '#4b5563',
  },
  bindButton: {
    backgroundColor: '#3b82f6',
    paddingVertical: 12,
    paddingHorizontal: 24,
    borderRadius: 8,
    alignItems: 'center',
  },
  bindButtonText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '600',
  },
});

export default ConnectionFlow;
