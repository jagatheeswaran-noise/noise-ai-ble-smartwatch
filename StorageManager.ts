import RNFS from 'react-native-fs';

export interface StorageInfo {
  availableSpace: number;
  totalSpace: number;
  usedSpace: number;
  usedPercentage: number;
}

export class StorageManager {
  
  static async getStorageInfo(): Promise<StorageInfo> {
    try {
      const fsInfo = await RNFS.getFSInfo();
      
      const totalSpace = fsInfo.totalSpace;
      const freeSpace = fsInfo.freeSpace;
      const usedSpace = totalSpace - freeSpace;
      const usedPercentage = (usedSpace / totalSpace) * 100;

      return {
        availableSpace: freeSpace,
        totalSpace: totalSpace,
        usedSpace: usedSpace,
        usedPercentage: usedPercentage,
      };
    } catch (error) {
      console.error('Error getting storage info:', error);
      // Return placeholder values if unable to get real storage info
      return {
        availableSpace: 5000000000, // 5GB
        totalSpace: 64000000000, // 64GB
        usedSpace: 59000000000, // 59GB
        usedPercentage: 92.2,
      };
    }
  }

  static formatBytes(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }

  static async getDirectorySize(dirPath: string): Promise<number> {
    try {
      const items = await RNFS.readdir(dirPath);
      let totalSize = 0;

      for (const item of items) {
        const itemPath = `${dirPath}/${item}`;
        const stat = await RNFS.stat(itemPath);
        
        if (stat.isDirectory()) {
          totalSize += await this.getDirectorySize(itemPath);
        } else {
          totalSize += stat.size;
        }
      }

      return totalSize;
    } catch (error) {
      console.error('Error calculating directory size:', error);
      return 0;
    }
  }

  static async getModelStorageUsage(): Promise<{ totalSize: number; fileCount: number }> {
    try {
      const modelDir = RNFS.DocumentDirectoryPath;
      const files = await RNFS.readdir(modelDir);
      
      let totalSize = 0;
      let fileCount = 0;

      for (const file of files) {
        if (file.endsWith('.gguf')) {
          const filePath = `${modelDir}/${file}`;
          const stat = await RNFS.stat(filePath);
          totalSize += stat.size;
          fileCount++;
        }
      }

      return { totalSize, fileCount };
    } catch (error) {
      console.error('Error getting model storage usage:', error);
      return { totalSize: 0, fileCount: 0 };
    }
  }

  static async cleanupTempFiles(): Promise<number> {
    try {
      const tempDir = RNFS.TemporaryDirectoryPath;
      const files = await RNFS.readdir(tempDir);
      
      let cleanedSize = 0;

      for (const file of files) {
        const filePath = `${tempDir}/${file}`;
        const stat = await RNFS.stat(filePath);
        
        // Delete files older than 24 hours
        const ageHours = (Date.now() - new Date(stat.mtime).getTime()) / (1000 * 60 * 60);
        
        if (ageHours > 24) {
          await RNFS.unlink(filePath);
          cleanedSize += stat.size;
        }
      }

      return cleanedSize;
    } catch (error) {
      console.error('Error cleaning temp files:', error);
      return 0;
    }
  }

  static async checkStorageHealth(): Promise<{
    isHealthy: boolean;
    warnings: string[];
    recommendations: string[];
  }> {
    const storageInfo = await this.getStorageInfo();
    const modelUsage = await this.getModelStorageUsage();
    
    const warnings: string[] = [];
    const recommendations: string[] = [];
    let isHealthy = true;

    // Check if storage is critically low
    if (storageInfo.usedPercentage > 95) {
      isHealthy = false;
      warnings.push('Storage is critically low (>95% used)');
      recommendations.push('Delete unused files or apps to free up space');
    } else if (storageInfo.usedPercentage > 85) {
      warnings.push('Storage is running low (>85% used)');
      recommendations.push('Consider freeing up some space for optimal performance');
    }

    // Check if there's enough space for model downloads
    const largestModelSize = 1342177280; // ~1.3GB for Llama-3.2-1B
    if (storageInfo.availableSpace < largestModelSize * 1.5) {
      warnings.push('Insufficient space for downloading AI models');
      recommendations.push('Free up at least 2GB of space to download AI models');
    }

    // Check model storage usage
    if (modelUsage.totalSize > storageInfo.totalSpace * 0.1) {
      warnings.push('AI models are using more than 10% of total storage');
      recommendations.push('Consider removing unused AI models');
    }

    return {
      isHealthy,
      warnings,
      recommendations,
    };
  }
}
