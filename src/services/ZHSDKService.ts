import { NativeModules, NativeEventEmitter, EmitterSubscription } from 'react-native';
import { 
  DeviceInfo, 
  ConnectionState, 
  SmartwatchEventType, 
  SmartwatchEventData,
  ScanStatus,
  ConnectionStatus
} from '../types/smartwatch';
import ConnectionPersistenceService from './ConnectionPersistenceService';
import { WatchVoiceCommand } from './WatchVoiceService';

const { ZHSDKModule } = NativeModules;

if (!ZHSDKModule) {
  throw new Error('ZHSDKModule is not available. Make sure the native module is properly linked.');
}

class ZHSDKService {
  private eventEmitter: NativeEventEmitter;
  private eventSubscriptions: Map<string, EmitterSubscription> = new Map();
  private scannedDevices: DeviceInfo[] = [];
  private scanStatus: ScanStatus = ScanStatus.IDLE;
  private connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED;
  private connectedDevice: DeviceInfo | null = null;
  private autoReconnectEnabled: boolean = true;
  private reconnectAttempts: number = 0;
  private maxReconnectAttempts: number = 3;

  constructor() {
    this.eventEmitter = new NativeEventEmitter(ZHSDKModule);
    this.setupEventListeners();
  }

  private setupEventListeners() {
    // Setup device found listener
    this.eventSubscriptions.set('onDeviceFound', 
      this.eventEmitter.addListener('onDeviceFound', (device: DeviceInfo) => {
        console.log('📱 Device found:', device);
        this.addScannedDevice(device);
      })
    );

    // Setup connection state listener
    this.eventSubscriptions.set('onConnectionStateChanged',
      this.eventEmitter.addListener('onConnectionStateChanged', (state: ConnectionState) => {
        console.log('🔗 Connection state changed:', state);
        this.updateConnectionStatus(state);
      })
    );

    // Setup other event listeners
    this.eventSubscriptions.set('onBindStateChecked',
      this.eventEmitter.addListener('onBindStateChecked', (result) => {
        console.log('📋 Bind state checked:', result);
      })
    );

    this.eventSubscriptions.set('onBindDeviceInfo',
      this.eventEmitter.addListener('onBindDeviceInfo', (result) => {
        console.log('📋 Bind device info:', result);
      })
    );

    this.eventSubscriptions.set('onUserIdVerified',
      this.eventEmitter.addListener('onUserIdVerified', (result) => {
        console.log('👤 User ID verified:', result);
      })
    );

    this.eventSubscriptions.set('onBindingResultSent',
      this.eventEmitter.addListener('onBindingResultSent', (result) => {
        console.log('📤 Binding result sent:', result);
      })
    );

    // Berry protocol specific events
    this.eventSubscriptions.set('onUserIdSetByBerry',
      this.eventEmitter.addListener('onUserIdSetByBerry', (result) => {
        console.log('🫐 User ID set by Berry:', result);
      })
    );

    this.eventSubscriptions.set('onDeviceBoundByBerry',
      this.eventEmitter.addListener('onDeviceBoundByBerry', (result) => {
        console.log('🫐 Device bound by Berry:', result);
      })
    );

    this.eventSubscriptions.set('onDeviceBindSuccessByBerry',
      this.eventEmitter.addListener('onDeviceBindSuccessByBerry', (result) => {
        console.log('🫐 Device bind success by Berry:', result);
        if (result.success) {
          console.log('✅ Updating connection status to BOUND (Berry)');
          this.connectionStatus = ConnectionStatus.BOUND;
          console.log('📊 Connection status is now:', this.connectionStatus);
          
          // Save the connection state for persistence
          if (this.connectedDevice) {
            ConnectionPersistenceService.saveConnectionState(
              this.connectedDevice,
              ConnectionStatus.BOUND,
              this.autoReconnectEnabled
            );
          }
        }
      })
    );

    this.eventSubscriptions.set('onOneKeyBindCompleted',
      this.eventEmitter.addListener('onOneKeyBindCompleted', (result) => {
        console.log('🫐 One-key bind completed:', result);
        if (result.success) {
          console.log('✅ Updating connection status to BOUND');
          this.connectionStatus = ConnectionStatus.BOUND;
          console.log('📊 Connection status is now:', this.connectionStatus);
          
          // Save the connection state for persistence
          if (this.connectedDevice) {
            ConnectionPersistenceService.saveConnectionState(
              this.connectedDevice,
              ConnectionStatus.BOUND,
              this.autoReconnectEnabled
            );
          }
        }
      })
    );

    // Device info and time sync events
    this.eventSubscriptions.set('onDeviceInfoReceived',
      this.eventEmitter.addListener('onDeviceInfoReceived', (result) => {
        console.log('📱 Device info received:', result);
      })
    );

    this.eventSubscriptions.set('onBatteryInfoReceived',
      this.eventEmitter.addListener('onBatteryInfoReceived', (result) => {
        console.log('🔋 Battery info received:', result);
      })
    );

    this.eventSubscriptions.set('onDeviceInfoRequestCompleted',
      this.eventEmitter.addListener('onDeviceInfoRequestCompleted', (result) => {
        console.log('📱 Device info request completed:', result);
      })
    );

    this.eventSubscriptions.set('onTimeSetCompleted',
      this.eventEmitter.addListener('onTimeSetCompleted', (result) => {
        console.log('⏰ Time set completed:', result);
      })
    );

    this.eventSubscriptions.set('onTimeFormatSetCompleted',
      this.eventEmitter.addListener('onTimeFormatSetCompleted', (result) => {
        console.log('⏰ Time format set completed:', result);
      })
    );

    this.eventSubscriptions.set('onBindDeviceInfo',
      this.eventEmitter.addListener('onBindDeviceInfo', (result) => {
        console.log('📱 Bind device info:', result);
      })
    );

    this.eventSubscriptions.set('onUserIdVerified',
      this.eventEmitter.addListener('onUserIdVerified', (result) => {
        console.log('👤 User ID verified:', result);
      })
    );

    this.eventSubscriptions.set('onBindingResultSent',
      this.eventEmitter.addListener('onBindingResultSent', (result) => {
        console.log('✅ Binding result sent:', result);
      })
    );
  }

  private addScannedDevice(device: DeviceInfo) {
    // Check if device already exists
    const existingIndex = this.scannedDevices.findIndex(d => d.address === device.address);
    if (existingIndex >= 0) {
      // Update existing device
      this.scannedDevices[existingIndex] = device;
    } else {
      // Add new device
      this.scannedDevices.push(device);
    }
  }

  private updateConnectionStatus(state: ConnectionState) {
    console.log('🔄 Updating connection status from state:', state);
    const oldStatus = this.connectionStatus;
    
    switch (state.stateString) {
      case 'connected':
        this.connectionStatus = ConnectionStatus.CONNECTED;
        break;
      case 'connecting':
        this.connectionStatus = ConnectionStatus.CONNECTING;
        break;
      case 'bound':
      case 'binding':
        this.connectionStatus = ConnectionStatus.BOUND;
        break;
      case 'disconnected':
        this.connectionStatus = ConnectionStatus.DISCONNECTED;
        this.connectedDevice = null;
        break;
      case 'timeout':
        this.connectionStatus = ConnectionStatus.DISCONNECTED;
        this.connectedDevice = null;
        break;
      default:
        this.connectionStatus = ConnectionStatus.DISCONNECTED;
        break;
    }
    
    console.log(`📊 Status changed: ${oldStatus} → ${this.connectionStatus}`);
  }

  // Public API methods
  async checkSDKStatus(): Promise<boolean> {
    try {
      return await ZHSDKModule.checkSDKStatus();
    } catch (error) {
      console.error('Error checking SDK status:', error);
      return false;
    }
  }

  async checkBluetoothPermissions(): Promise<boolean> {
    try {
      return await ZHSDKModule.checkBluetoothPermissions();
    } catch (error) {
      console.error('Error checking Bluetooth permissions:', error);
      return false;
    }
  }

  async requestBluetoothPermissions(): Promise<boolean> {
    try {
      return await ZHSDKModule.requestBluetoothPermissions();
    } catch (error) {
      console.error('Error requesting Bluetooth permissions:', error);
      return false;
    }
  }

  async isBluetoothEnabled(): Promise<boolean> {
    try {
      return await ZHSDKModule.isBluetoothEnabled();
    } catch (error) {
      console.error('Error checking Bluetooth status:', error);
      return false;
    }
  }

  async startDeviceScan(): Promise<boolean> {
    try {
      this.scannedDevices = [];
      this.scanStatus = ScanStatus.SCANNING;
      
      const result = await ZHSDKModule.startDeviceScan();
      if (!result) {
        this.scanStatus = ScanStatus.IDLE;
      }
      return result;
    } catch (error) {
      console.error('Error starting device scan:', error);
      this.scanStatus = ScanStatus.IDLE;
      return false;
    }
  }

  async stopDeviceScan(): Promise<boolean> {
    try {
      this.scanStatus = ScanStatus.STOPPED;
      return await ZHSDKModule.stopDeviceScan();
    } catch (error) {
      console.error('Error stopping device scan:', error);
      return false;
    }
  }

  async getScannedDevices(): Promise<DeviceInfo[]> {
    try {
      const devices = await ZHSDKModule.getScannedDevices();
      return devices || [];
    } catch (error) {
      console.error('Error getting scanned devices:', error);
      return [];
    }
  }

  async connectDevice(device: DeviceInfo): Promise<boolean> {
    try {
      console.log(`🔗 Connecting to device: ${device.name} (${device.address})`);
      this.connectedDevice = device;
      this.connectionStatus = ConnectionStatus.CONNECTING;
      
      // Start the connection process
      const connectionInitiated = await ZHSDKModule.connectDevice(device.name, device.address, device.protocol);
      
      if (!connectionInitiated) {
        console.error('❌ Failed to initiate connection');
        this.connectedDevice = null;
        this.connectionStatus = ConnectionStatus.DISCONNECTED;
        return false;
      }
      
      // Wait for connection state to change to 'connected' with timeout
      return new Promise((resolve) => {
        const timeout = setTimeout(() => {
          console.error('❌ Connection timeout');
          this.connectedDevice = null;
          this.connectionStatus = ConnectionStatus.DISCONNECTED;
          resolve(false);
        }, 30000); // 30 second timeout
        
        const checkConnection = () => {
          if (this.connectionStatus === ConnectionStatus.CONNECTED) {
            clearTimeout(timeout);
            console.log('✅ Connection confirmed via state change');
            resolve(true);
          } else if (this.connectionStatus === ConnectionStatus.DISCONNECTED) {
            clearTimeout(timeout);
            console.log('❌ Connection failed via state change');
            this.connectedDevice = null;
            resolve(false);
          } else {
            // Still connecting, check again in 500ms
            setTimeout(checkConnection, 500);
          }
        };
        
        // Start checking after a small delay
        setTimeout(checkConnection, 500);
      });
    } catch (error) {
      console.error('Error connecting to device:', error);
      this.connectedDevice = null;
      this.connectionStatus = ConnectionStatus.DISCONNECTED;
      return false;
    }
  }

  async disconnectDevice(): Promise<boolean> {
    try {
      const result = await ZHSDKModule.disconnectDevice();
      this.connectedDevice = null;
      this.connectionStatus = ConnectionStatus.DISCONNECTED;
      return result;
    } catch (error) {
      console.error('Error disconnecting device:', error);
      return false;
    }
  }

  async isDeviceConnected(): Promise<boolean> {
    try {
      return await ZHSDKModule.isDeviceConnected();
    } catch (error) {
      console.error('Error checking device connection:', error);
      return false;
    }
  }

  async verifyUserId(userId: string): Promise<boolean> {
    try {
      return await ZHSDKModule.verifyUserId(userId);
    } catch (error) {
      console.error('Error verifying user ID:', error);
      return false;
    }
  }

  async requestDeviceBindState(): Promise<boolean> {
    try {
      return await ZHSDKModule.requestDeviceBindState();
    } catch (error) {
      console.error('Error requesting bind state:', error);
      return false;
    }
  }

  async bindDevice(): Promise<boolean> {
    try {
      this.connectionStatus = ConnectionStatus.BINDING;
      return await ZHSDKModule.bindDevice();
    } catch (error) {
      console.error('Error binding device:', error);
      return false;
    }
  }

  async sendBindingResult(userId: string): Promise<boolean> {
    try {
      const result = await ZHSDKModule.sendBindingResult(userId);
      if (result) {
        this.connectionStatus = ConnectionStatus.BOUND;
      }
      return result;
    } catch (error) {
      console.error('Error sending binding result:', error);
      return false;
    }
  }

  // Berry Protocol specific binding methods
  async setUserIdByBerryProtocol(userId: string, deviceModel: string = 'React Native', sdkVersion: string = '1.0.0'): Promise<boolean> {
    try {
      return await ZHSDKModule.setUserIdByBerryProtocol(userId, deviceModel, sdkVersion);
    } catch (error) {
      console.error('Error setting user ID by Berry protocol:', error);
      return false;
    }
  }

  async bindDeviceByBerryProtocol(): Promise<boolean> {
    try {
      return await ZHSDKModule.bindDeviceByBerryProtocol();
    } catch (error) {
      console.error('Error binding device by Berry protocol:', error);
      return false;
    }
  }

  async bindDeviceSuccessByBerryProtocol(): Promise<boolean> {
    try {
      return await ZHSDKModule.bindDeviceSuccessByBerryProtocol();
    } catch (error) {
      console.error('Error completing bind device by Berry protocol:', error);
      return false;
    }
  }

  async performOneKeyBindByBerry(userId: string, deviceModel: string = 'React Native', sdkVersion: string = '1.0.0'): Promise<boolean> {
    try {
      this.connectionStatus = ConnectionStatus.BINDING;
      const result = await ZHSDKModule.performOneKeyBindByBerry(userId, deviceModel, sdkVersion);
      return result;
    } catch (error) {
      console.error('Error performing one-key bind by Berry protocol:', error);
      this.connectionStatus = ConnectionStatus.CONNECTED;
      return false;
    }
  }

  // Device status and time sync methods
  async getDeviceInfo(): Promise<boolean> {
    try {
      return await ZHSDKModule.getDeviceInfo();
    } catch (error) {
      console.error('Error getting device info:', error);
      return false;
    }
  }

  async setTime(): Promise<boolean> {
    try {
      return await ZHSDKModule.setTime();
    } catch (error) {
      console.error('Error setting time:', error);
      return false;
    }
  }

  async setTimeFormat(is24HourFormat: boolean): Promise<boolean> {
    try {
      return await ZHSDKModule.setTimeFormat(is24HourFormat);
    } catch (error) {
      console.error('Error setting time format:', error);
      return false;
    }
  }

  // AI Voice Integration Methods
  async sendAiVoiceCommand(command: number): Promise<boolean> {
    try {
      return await ZHSDKModule.sendAiVoiceCommand(command);
    } catch (error) {
      console.error('Error sending AI voice command:', error);
      return false;
    }
  }

  async sendAiTranslatedText(text: string): Promise<boolean> {
    try {
      return await ZHSDKModule.sendAiTranslatedText(text);
    } catch (error) {
      console.error('Error sending AI translated text:', error);
      return false;
    }
  }

  async sendAiAnswerText(text: string): Promise<boolean> {
    try {
      return await ZHSDKModule.sendAiAnswerText(text);
    } catch (error) {
      console.error('Error sending AI answer text:', error);
      return false;
    }
  }

  async sendAiViewUi(title: string, content: string): Promise<boolean> {
    try {
      console.log('🎤⌚ Sending AI view UI:', { title, content });
      return await ZHSDKModule.sendAiViewUi(title, content);
    } catch (error) {
      console.error('🎤⌚ Error sending AI view UI:', error);
      return false;
    }
  }

  async sendAiErrorCode(errorCode: number): Promise<boolean> {
    try {
      return await ZHSDKModule.sendAiErrorCode(errorCode);
    } catch (error) {
      console.error('Error sending AI error code:', error);
      return false;
    }
  }

  async testAiVoiceConnection(): Promise<any> {
    try {
      return await ZHSDKModule.testAiVoiceConnection();
    } catch (error) {
      console.error('Error testing AI voice connection:', error);
      return { 
        isConnected: false, 
        sdkInitialized: false, 
        callbackRegistered: false,
        error: error instanceof Error ? error.message : 'Unknown error'
      };
    }
  }

  async simulateWatchVoiceCommand(): Promise<boolean> {
    try {
      return await ZHSDKModule.simulateWatchVoiceCommand();
    } catch (error) {
      console.error('Error simulating watch voice command:', error);
      return false;
    }
  }

  async initializeWatchAI(): Promise<boolean> {
    try {
      console.log('🎤⌚ Initializing AI functionality on watch...');
      return await ZHSDKModule.initializeWatchAI();
    } catch (error) {
      console.error('Error initializing watch AI:', error);
      return false;
    }
  }

  async startWatchAIListening(): Promise<boolean> {
    try {
      console.log('🎤⌚ Starting AI listening mode on watch...');
      return await ZHSDKModule.startWatchAIListening();
    } catch (error) {
      console.error('Error starting AI listening:', error);
      return false;
    }
  }

  async stopWatchAIListening(): Promise<boolean> {
    try {
      console.log('🎤⌚ Stopping AI listening mode on watch...');
      return await ZHSDKModule.stopWatchAIListening();
    } catch (error) {
      console.error('Error stopping AI listening:', error);
      return false;
    }
  }

  // Event subscription methods
  addEventListener<T extends SmartwatchEventType>(
    eventType: T,
    listener: (data: SmartwatchEventData[T]) => void
  ): EmitterSubscription {
    return this.eventEmitter.addListener(eventType, listener);
  }

  removeEventListener(subscription: EmitterSubscription) {
    subscription.remove();
  }

  // Getters for current state
  get currentScanStatus(): ScanStatus {
    return this.scanStatus;
  }

  get currentConnectionStatus(): ConnectionStatus {
    return this.connectionStatus;
  }

  get currentConnectedDevice(): DeviceInfo | null {
    return this.connectedDevice;
  }

  // Force refresh connection status from native module
  async refreshConnectionStatus(): Promise<void> {
    try {
      const isConnected = await ZHSDKModule.isDeviceConnected();
      console.log('🔄 Force refresh - Native connection check:', isConnected);
      
      if (!isConnected) {
        console.log('📊 Force updating status to DISCONNECTED');
        this.connectionStatus = ConnectionStatus.DISCONNECTED;
        this.connectedDevice = null;
      } else if (this.connectedDevice && this.connectionStatus === ConnectionStatus.CONNECTING) {
        // If native says connected but we're still in connecting state, update to connected
        console.log('📊 Force updating status to CONNECTED');
        this.connectionStatus = ConnectionStatus.CONNECTED;
      }
    } catch (error) {
      console.error('Error refreshing connection status:', error);
    }
  }

  get currentScannedDevices(): DeviceInfo[] {
    return [...this.scannedDevices];
  }

  // Connection persistence and auto-reconnection methods
  
  /**
   * Initialize the service and attempt auto-reconnection if enabled
   */
  async initializeWithPersistence(): Promise<boolean> {
    try {
      console.log('🔄 Initializing service with persistence...');
      
      // Check if we should attempt to reconnect
      const shouldReconnect = await ConnectionPersistenceService.shouldAttemptReconnect();
      if (!shouldReconnect) {
        console.log('📱 No reconnection needed');
        return false;
      }

      const deviceToReconnect = await ConnectionPersistenceService.getDeviceForReconnect();
      if (!deviceToReconnect) {
        console.log('📱 No device found for reconnection');
        return false;
      }

      console.log('🔄 Attempting to reconnect to:', deviceToReconnect.name);
      
      // Check SDK status and permissions first
      const sdkReady = await this.checkSDKStatus();
      const permissionsGranted = await this.checkBluetoothPermissions();
      if (!sdkReady || !permissionsGranted) {
        console.log('❌ SDK not ready or permissions missing, cannot reconnect');
        return false;
      }

      // Attempt reconnection
      return await this.attemptAutoReconnect(deviceToReconnect);
      
    } catch (error) {
      console.error('❌ Error during initialization with persistence:', error);
      return false;
    }
  }

  /**
   * Attempt to automatically reconnect to a device
   */
  private async attemptAutoReconnect(device: DeviceInfo): Promise<boolean> {
    try {
      this.reconnectAttempts++;
      console.log(`🔄 Auto-reconnect attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts} to ${device.name}`);

      // First check if device is already connected at native level
      const isAlreadyConnected = await ZHSDKModule.isDeviceConnected();
      if (isAlreadyConnected) {
        console.log('✅ Device already connected at native level');
        this.connectedDevice = device;
        this.connectionStatus = ConnectionStatus.BOUND; // Assume bound if already connected
        return true;
      }

      // Attempt to connect
      const connected = await this.connectDevice(device);
      if (connected) {
        console.log('✅ Auto-reconnection successful');
        this.reconnectAttempts = 0; // Reset counter on success
        return true;
      } else {
        console.log('❌ Auto-reconnection failed');
        
        // If we've exceeded max attempts, clear the persisted state
        if (this.reconnectAttempts >= this.maxReconnectAttempts) {
          console.log('❌ Max reconnection attempts reached, clearing persisted state');
          await ConnectionPersistenceService.clearConnectionState();
          this.reconnectAttempts = 0;
        }
        
        return false;
      }
      
    } catch (error) {
      console.error('❌ Error during auto-reconnection:', error);
      return false;
    }
  }

  /**
   * Manually trigger reconnection to last connected device
   */
  async reconnectToLastDevice(): Promise<boolean> {
    const device = await ConnectionPersistenceService.getDeviceForReconnect();
    if (!device) {
      console.log('📱 No device available for reconnection');
      return false;
    }

    console.log('🔄 Manual reconnection triggered for:', device.name);
    return await this.attemptAutoReconnect(device);
  }

  /**
   * Enable or disable auto-reconnection
   */
  async setAutoReconnectEnabled(enabled: boolean): Promise<void> {
    this.autoReconnectEnabled = enabled;
    await ConnectionPersistenceService.setReconnectEnabled(enabled);
    console.log('⚙️ Auto-reconnect set to:', enabled);
  }

  /**
   * Get the last connected device from persistence
   */
  async getLastConnectedDevice(): Promise<DeviceInfo | null> {
    return await ConnectionPersistenceService.getDeviceForReconnect();
  }

  /**
   * Clear connection persistence (disconnect permanently)
   */
  async clearConnectionPersistence(): Promise<void> {
    await ConnectionPersistenceService.clearConnectionState();
    this.connectedDevice = null;
    this.connectionStatus = ConnectionStatus.DISCONNECTED;
    console.log('🗑️ Connection persistence cleared');
  }

  // Cleanup method
  cleanup() {
    this.eventSubscriptions.forEach(subscription => subscription.remove());
    this.eventSubscriptions.clear();
    this.scannedDevices = [];
    this.connectedDevice = null;
    this.scanStatus = ScanStatus.IDLE;
    this.connectionStatus = ConnectionStatus.DISCONNECTED;
  }
}

// Export singleton instance
export const zhSDKService = new ZHSDKService();
export default zhSDKService;
