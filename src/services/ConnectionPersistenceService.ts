import AsyncStorage from '@react-native-async-storage/async-storage';
import { DeviceInfo, ConnectionStatus } from '../types/smartwatch';

interface PersistedConnectionState {
  lastConnectedDevice: DeviceInfo | null;
  lastConnectionTime: number;
  lastConnectionStatus: ConnectionStatus;
  isReconnectEnabled: boolean;
}

class ConnectionPersistenceService {
  private static readonly STORAGE_KEY = 'smartwatch_connection_state';
  private static readonly MAX_RECONNECT_AGE_MS = 24 * 60 * 60 * 1000; // 24 hours

  /**
   * Save the current connection state to persistent storage
   */
  static async saveConnectionState(
    device: DeviceInfo | null,
    status: ConnectionStatus,
    enableReconnect: boolean = true
  ): Promise<void> {
    try {
      const state: PersistedConnectionState = {
        lastConnectedDevice: device,
        lastConnectionTime: Date.now(),
        lastConnectionStatus: status,
        isReconnectEnabled: enableReconnect,
      };

      await AsyncStorage.setItem(this.STORAGE_KEY, JSON.stringify(state));
      console.log('💾 Connection state saved:', {
        device: device?.name,
        status,
        enableReconnect,
      });
    } catch (error) {
      console.error('❌ Failed to save connection state:', error);
    }
  }

  /**
   * Load the persisted connection state from storage
   */
  static async loadConnectionState(): Promise<PersistedConnectionState | null> {
    try {
      const storedData = await AsyncStorage.getItem(this.STORAGE_KEY);
      if (!storedData) {
        console.log('📱 No persisted connection state found');
        return null;
      }

      const state: PersistedConnectionState = JSON.parse(storedData);
      
      // Check if the stored state is too old
      const age = Date.now() - state.lastConnectionTime;
      if (age > this.MAX_RECONNECT_AGE_MS) {
        console.log('⏰ Persisted connection state is too old, ignoring');
        await this.clearConnectionState();
        return null;
      }

      console.log('📱 Loaded persisted connection state:', {
        device: state.lastConnectedDevice?.name,
        status: state.lastConnectionStatus,
        age: Math.round(age / 60000) + ' minutes ago',
      });

      return state;
    } catch (error) {
      console.error('❌ Failed to load connection state:', error);
      return null;
    }
  }

  /**
   * Clear the persisted connection state
   */
  static async clearConnectionState(): Promise<void> {
    try {
      await AsyncStorage.removeItem(this.STORAGE_KEY);
      console.log('🗑️ Connection state cleared');
    } catch (error) {
      console.error('❌ Failed to clear connection state:', error);
    }
  }

  /**
   * Check if we should attempt to reconnect based on the persisted state
   */
  static async shouldAttemptReconnect(): Promise<boolean> {
    const state = await this.loadConnectionState();
    
    if (!state || !state.isReconnectEnabled || !state.lastConnectedDevice) {
      return false;
    }

    // Only reconnect if the last status was connected or bound
    return state.lastConnectionStatus === ConnectionStatus.CONNECTED || 
           state.lastConnectionStatus === ConnectionStatus.BOUND;
  }

  /**
   * Get the last connected device if it should be reconnected
   */
  static async getDeviceForReconnect(): Promise<DeviceInfo | null> {
    const state = await this.loadConnectionState();
    
    if (!state || !await this.shouldAttemptReconnect()) {
      return null;
    }

    return state.lastConnectedDevice;
  }

  /**
   * Update reconnect preference
   */
  static async setReconnectEnabled(enabled: boolean): Promise<void> {
    try {
      const state = await this.loadConnectionState();
      if (state) {
        state.isReconnectEnabled = enabled;
        await AsyncStorage.setItem(this.STORAGE_KEY, JSON.stringify(state));
        console.log('⚙️ Reconnect preference updated:', enabled);
      }
    } catch (error) {
      console.error('❌ Failed to update reconnect preference:', error);
    }
  }
}

export default ConnectionPersistenceService;
