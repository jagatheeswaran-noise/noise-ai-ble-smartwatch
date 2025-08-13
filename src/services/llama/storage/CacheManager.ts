import RNFS from 'react-native-fs';
import { storageMonitor } from './StorageMonitor';

export class CacheManager {
  private readonly TEMP_DIR = `${RNFS.DocumentDirectoryPath}/temp`;
  private readonly MODELS_DIR = `${RNFS.DocumentDirectoryPath}/models`;
  private readonly MAX_TEMP_AGE_MS = 24 * 60 * 60 * 1000; // 24 hours

  constructor() {
    this.ensureDirectoriesExist();
  }

  private async ensureDirectoriesExist(): Promise<void> {
    try {
      await RNFS.mkdir(this.TEMP_DIR);
      await RNFS.mkdir(this.MODELS_DIR);
    } catch (error) {
      // Directories might already exist, that's fine
      console.log('📁 Directories already exist or created');
    }
  }

  async cleanupTempFiles(): Promise<number> {
    try {
      const files = await RNFS.readDir(this.TEMP_DIR);
      let deletedCount = 0;
      const now = Date.now();

      for (const file of files) {
        const fileAge = file.mtime ? now - new Date(file.mtime).getTime() : 0;
        
        if (fileAge > this.MAX_TEMP_AGE_MS) {
          try {
            await RNFS.unlink(file.path);
            deletedCount++;
            console.log(`🗑️ Deleted old temp file: ${file.name}`);
          } catch (deleteError) {
            console.error(`❌ Failed to delete temp file ${file.name}:`, deleteError);
          }
        }
      }

      console.log(`🧹 Cleanup complete: ${deletedCount} temp files deleted`);
      return deletedCount;
    } catch (error) {
      console.error('❌ Error during temp file cleanup:', error);
      return 0;
    }
  }

  async cleanupOldDownloads(): Promise<number> {
    try {
      const files = await RNFS.readDir(this.TEMP_DIR);
      let deletedCount = 0;

      // Look for partial download files (usually have .part or .tmp extensions)
      const partialFiles = files.filter(file => 
        file.name.endsWith('.part') || 
        file.name.endsWith('.tmp') ||
        file.name.includes('download_')
      );

      for (const file of partialFiles) {
        try {
          await RNFS.unlink(file.path);
          deletedCount++;
          console.log(`🗑️ Deleted partial download: ${file.name}`);
        } catch (deleteError) {
          console.error(`❌ Failed to delete partial download ${file.name}:`, deleteError);
        }
      }

      return deletedCount;
    } catch (error) {
      console.error('❌ Error cleaning up old downloads:', error);
      return 0;
    }
  }

  async getTotalCacheSize(): Promise<number> {
    try {
      const tempFiles = await RNFS.readDir(this.TEMP_DIR);
      let totalSize = 0;

      for (const file of tempFiles) {
        if (file.isFile()) {
          totalSize += file.size;
        }
      }

      return totalSize;
    } catch (error) {
      console.error('❌ Error calculating cache size:', error);
      return 0;
    }
  }

  async clearAllCache(): Promise<void> {
    try {
      const files = await RNFS.readDir(this.TEMP_DIR);
      
      for (const file of files) {
        try {
          await RNFS.unlink(file.path);
          console.log(`🗑️ Deleted cache file: ${file.name}`);
        } catch (deleteError) {
          console.error(`❌ Failed to delete cache file ${file.name}:`, deleteError);
        }
      }

      console.log('🧹 All cache files cleared');
    } catch (error) {
      console.error('❌ Error clearing cache:', error);
    }
  }

  async getModelPath(filename: string): Promise<string> {
    return `${this.MODELS_DIR}/${filename}`;
  }

  async getTempPath(filename: string): Promise<string> {
    return `${this.TEMP_DIR}/${filename}`;
  }

  async modelExists(filename: string): Promise<boolean> {
    try {
      const modelPath = await this.getModelPath(filename);
      const exists = await RNFS.exists(modelPath);
      return exists;
    } catch (error) {
      console.error('❌ Error checking if model exists:', error);
      return false;
    }
  }

  async getModelSize(filename: string): Promise<number> {
    try {
      const modelPath = await this.getModelPath(filename);
      const stat = await RNFS.stat(modelPath);
      return stat.size;
    } catch (error) {
      console.error('❌ Error getting model size:', error);
      return 0;
    }
  }

  async deleteModel(filename: string): Promise<boolean> {
    try {
      const modelPath = await this.getModelPath(filename);
      const exists = await RNFS.exists(modelPath);
      
      if (exists) {
        await RNFS.unlink(modelPath);
        console.log(`🗑️ Deleted model: ${filename}`);
        return true;
      }
      
      return false;
    } catch (error) {
      console.error(`❌ Error deleting model ${filename}:`, error);
      return false;
    }
  }

  async performMaintenanceCleanup(): Promise<{
    tempFilesDeleted: number;
    partialDownloadsDeleted: number;
    spaceFreed: number;
  }> {
    console.log('🧹 Starting maintenance cleanup...');
    
    const initialCacheSize = await this.getTotalCacheSize();
    const tempFilesDeleted = await this.cleanupTempFiles();
    const partialDownloadsDeleted = await this.cleanupOldDownloads();
    const finalCacheSize = await this.getTotalCacheSize();
    const spaceFreed = initialCacheSize - finalCacheSize;

    console.log(`🧹 Maintenance cleanup complete:
      - Temp files deleted: ${tempFilesDeleted}
      - Partial downloads deleted: ${partialDownloadsDeleted}
      - Space freed: ${storageMonitor.formatBytes(spaceFreed)}`);

    return {
      tempFilesDeleted,
      partialDownloadsDeleted,
      spaceFreed,
    };
  }

  async getStorageReport(): Promise<{
    cacheSize: string;
    modelCount: number;
    tempFileCount: number;
    recommendations: string[];
  }> {
    try {
      const cacheSize = await this.getTotalCacheSize();
      const tempFiles = await RNFS.readDir(this.TEMP_DIR);
      const modelFiles = await RNFS.readDir(this.MODELS_DIR);
      
      const recommendations: string[] = [];
      
      if (cacheSize > 100 * 1024 * 1024) { // 100MB
        recommendations.push('Consider clearing cache to free up space');
      }
      
      if (tempFiles.length > 10) {
        recommendations.push('Multiple temporary files found, cleanup recommended');
      }

      const storageHealth = await storageMonitor.getHealthStatusFromUsage();
      if (storageHealth !== 'healthy') {
        recommendations.push('Storage space is running low, consider freeing up space');
      }

      return {
        cacheSize: storageMonitor.formatBytes(cacheSize),
        modelCount: modelFiles.filter(f => f.isFile()).length,
        tempFileCount: tempFiles.filter(f => f.isFile()).length,
        recommendations,
      };
    } catch (error) {
      console.error('❌ Error generating storage report:', error);
      throw error;
    }
  }
}

// Singleton instance
export const cacheManager = new CacheManager();
