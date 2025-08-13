import RNFS from 'react-native-fs';
import { StorageInfo, StorageHealth } from '../utils/LlamaTypes';

export class StorageMonitor {
  private isMonitoring: boolean = false;
  private monitoringInterval: NodeJS.Timeout | null = null;
  private warningCallbacks: ((info: StorageInfo) => void)[] = [];
  private readonly WARNING_THRESHOLD = 0.85; // 85%
  private readonly CRITICAL_THRESHOLD = 0.95; // 95%

  async getCurrentUsage(): Promise<StorageInfo> {
    try {
      const freeSpace = await RNFS.getFSInfo();
      const total = freeSpace.totalSpace;
      const available = freeSpace.freeSpace;
      const used = total - available;
      const usagePercentage = used / total;

      return {
        available,
        used,
        total,
        usagePercentage,
      };
    } catch (error) {
      console.error('💾 Error getting storage info:', error);
      throw new Error('Failed to get storage information');
    }
  }

  getHealthStatus(usagePercentage: number): StorageHealth {
    if (usagePercentage >= this.CRITICAL_THRESHOLD) {
      return 'critical';
    } else if (usagePercentage >= this.WARNING_THRESHOLD) {
      return 'warning';
    }
    return 'healthy';
  }

  async getHealthStatusFromUsage(): Promise<StorageHealth> {
    const storageInfo = await this.getCurrentUsage();
    return this.getHealthStatus(storageInfo.usagePercentage);
  }

  onStorageWarning(callback: (info: StorageInfo) => void): void {
    this.warningCallbacks.push(callback);
  }

  removeStorageWarningCallback(callback: (info: StorageInfo) => void): void {
    const index = this.warningCallbacks.indexOf(callback);
    if (index > -1) {
      this.warningCallbacks.splice(index, 1);
    }
  }

  private async checkStorageAndNotify(): Promise<void> {
    try {
      const storageInfo = await this.getCurrentUsage();
      const health = this.getHealthStatus(storageInfo.usagePercentage);

      if (health === 'warning' || health === 'critical') {
        console.warn(`💾 Storage ${health}:`, `${(storageInfo.usagePercentage * 100).toFixed(1)}% used`);
        
        // Notify all registered callbacks
        this.warningCallbacks.forEach(callback => {
          try {
            callback(storageInfo);
          } catch (error) {
            console.error('💾 Error in storage warning callback:', error);
          }
        });
      }
    } catch (error) {
      console.error('💾 Error checking storage:', error);
    }
  }

  startMonitoring(intervalMs: number = 30000): void { // Default 30 seconds
    if (this.isMonitoring) {
      console.log('💾 Storage monitoring already active');
      return;
    }

    console.log('💾 Starting storage monitoring');
    this.isMonitoring = true;
    
    // Initial check
    this.checkStorageAndNotify();
    
    // Set up periodic monitoring
    this.monitoringInterval = setInterval(() => {
      this.checkStorageAndNotify();
    }, intervalMs);
  }

  stopMonitoring(): void {
    if (!this.isMonitoring) {
      return;
    }

    console.log('💾 Stopping storage monitoring');
    this.isMonitoring = false;
    
    if (this.monitoringInterval) {
      clearInterval(this.monitoringInterval);
      this.monitoringInterval = null;
    }
  }

  async hasEnoughSpaceForFile(fileSize: number): Promise<boolean> {
    try {
      const storageInfo = await this.getCurrentUsage();
      // Keep some buffer space (500MB minimum)
      const requiredSpace = fileSize + (500 * 1024 * 1024);
      return storageInfo.available >= requiredSpace;
    } catch (error) {
      console.error('💾 Error checking space for file:', error);
      return false;
    }
  }

  formatBytes(bytes: number): string {
    if (bytes === 0) return '0 Bytes';

    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));

    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }

  async getFormattedStorageInfo(): Promise<{
    available: string;
    used: string;
    total: string;
    usagePercentage: string;
    health: StorageHealth;
  }> {
    const info = await this.getCurrentUsage();
    const health = this.getHealthStatus(info.usagePercentage);

    return {
      available: this.formatBytes(info.available),
      used: this.formatBytes(info.used),
      total: this.formatBytes(info.total),
      usagePercentage: `${(info.usagePercentage * 100).toFixed(1)}%`,
      health,
    };
  }

  cleanup(): void {
    this.stopMonitoring();
    this.warningCallbacks = [];
  }
}

// Singleton instance
export const storageMonitor = new StorageMonitor();
