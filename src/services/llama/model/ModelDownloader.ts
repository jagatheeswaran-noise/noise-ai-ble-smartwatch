import RNFS from 'react-native-fs';
import { ModelInfo, ModelDownloadProgress, DownloadError } from '../utils/LlamaTypes';
import { cacheManager } from '../storage/CacheManager';
import { storageMonitor } from '../storage/StorageMonitor';

export class ModelDownloader {
  private activeDownloads: Map<string, { jobId: number; promise: Promise<RNFS.DownloadResult> }> = new Map();
  private downloadProgress: Map<string, ModelDownloadProgress> = new Map();

  async downloadModel(
    model: ModelInfo, 
    onProgress?: (progress: ModelDownloadProgress) => void
  ): Promise<string> {
    const { name, url, filename, size } = model;
    
    console.log(`📥 Starting download: ${name}`);

    // Check if already downloading
    if (this.activeDownloads.has(filename)) {
      throw new DownloadError(`Download already in progress for ${name}`);
    }

    // Check storage space
    const hasSpace = await storageMonitor.hasEnoughSpaceForFile(size);
    if (!hasSpace) {
      throw new DownloadError(`Insufficient storage space for ${name}. Need ${storageMonitor.formatBytes(size)}`);
    }

    // Check if model already exists
    const modelExists = await cacheManager.modelExists(filename);
    if (modelExists) {
      const existingSize = await cacheManager.getModelSize(filename);
      if (existingSize === size) {
        console.log(`✅ Model ${name} already exists with correct size`);
        return await cacheManager.getModelPath(filename);
      } else {
        console.log(`⚠️ Model ${name} exists but size mismatch, re-downloading`);
        await cacheManager.deleteModel(filename);
      }
    }

    const tempPath = await cacheManager.getTempPath(`${filename}.part`);
    const finalPath = await cacheManager.getModelPath(filename);

    try {
      // Check for partial download
      let resumeFrom = 0;
      const partialExists = await RNFS.exists(tempPath);
      if (partialExists) {
        const partialStat = await RNFS.stat(tempPath);
        resumeFrom = partialStat.size;
        console.log(`📥 Resuming download from ${storageMonitor.formatBytes(resumeFrom)}`);
      }

      const downloadOptions: RNFS.DownloadFileOptions = {
        fromUrl: url,
        toFile: tempPath,
        headers: resumeFrom > 0 ? { Range: `bytes=${resumeFrom}-` } : {},
        progress: (res) => {
          const progress: ModelDownloadProgress = {
            bytesWritten: res.bytesWritten + resumeFrom,
            contentLength: res.contentLength + resumeFrom,
            progress: (res.bytesWritten + resumeFrom) / (res.contentLength + resumeFrom),
          };

          this.downloadProgress.set(filename, progress);
          onProgress?.(progress);

          console.log(`📥 Download progress: ${(progress.progress * 100).toFixed(1)}%`);
        },
      };

      const downloadResult = RNFS.downloadFile(downloadOptions);
      this.activeDownloads.set(filename, downloadResult);

      const result = await downloadResult.promise;

      if (result.statusCode === 200 || result.statusCode === 206) {
        // Verify file size
        const downloadedStat = await RNFS.stat(tempPath);
        const expectedSize = resumeFrom > 0 ? size : size;

        if (downloadedStat.size >= expectedSize * 0.95) { // Allow 5% tolerance
          // Move from temp to final location
          await RNFS.moveFile(tempPath, finalPath);
          console.log(`✅ Download completed: ${name}`);
          
          // Cleanup
          this.activeDownloads.delete(filename);
          this.downloadProgress.delete(filename);
          
          return finalPath;
        } else {
          throw new DownloadError(
            `Downloaded file size (${downloadedStat.size}) doesn't match expected size (${expectedSize})`
          );
        }
      } else {
        throw new DownloadError(`Download failed with status code: ${result.statusCode}`);
      }

    } catch (error) {
      console.error(`❌ Download failed for ${name}:`, error);
      
      // Cleanup on error
      this.activeDownloads.delete(filename);
      this.downloadProgress.delete(filename);
      
      // Don't delete partial file, allow resume next time
      if (error instanceof Error) {
        const downloadError = new DownloadError(error.message);
        downloadError.retryable = true;
        throw downloadError;
      }
      throw error;
    }
  }

  async resumeDownload(
    model: ModelInfo,
    onProgress?: (progress: ModelDownloadProgress) => void
  ): Promise<string> {
    console.log(`🔄 Attempting to resume download: ${model.name}`);
    return this.downloadModel(model, onProgress);
  }

  async cancelDownload(filename: string): Promise<void> {
    const activeDownload = this.activeDownloads.get(filename);
    
    if (activeDownload) {
      try {
        RNFS.stopDownload(activeDownload.jobId);
        console.log(`❌ Download cancelled: ${filename}`);
      } catch (error) {
        console.error('Error cancelling download:', error);
      }
      
      this.activeDownloads.delete(filename);
      this.downloadProgress.delete(filename);
    }
  }

  getDownloadProgress(filename: string): ModelDownloadProgress | null {
    return this.downloadProgress.get(filename) || null;
  }

  isDownloading(filename: string): boolean {
    return this.activeDownloads.has(filename);
  }

  async cleanupFailedDownloads(): Promise<void> {
    try {
      const tempDir = await cacheManager.getTempPath('');
      const files = await RNFS.readDir(tempDir);
      
      for (const file of files) {
        if (file.name.endsWith('.part')) {
          try {
            await RNFS.unlink(file.path);
            console.log(`🗑️ Cleaned up failed download: ${file.name}`);
          } catch (deleteError) {
            console.error(`Error deleting ${file.name}:`, deleteError);
          }
        }
      }
    } catch (error) {
      console.error('Error cleaning up failed downloads:', error);
    }
  }

  async getAllDownloadedModels(): Promise<{ filename: string; size: number; path: string }[]> {
    try {
      const modelsDir = await cacheManager.getModelPath('');
      const files = await RNFS.readDir(modelsDir);
      
      const models = [];
      for (const file of files) {
        if (file.isFile() && file.name.endsWith('.gguf')) {
          models.push({
            filename: file.name,
            size: file.size,
            path: file.path,
          });
        }
      }
      
      return models;
    } catch (error) {
      console.error('Error getting downloaded models:', error);
      return [];
    }
  }

  async validateModelIntegrity(model: ModelInfo): Promise<boolean> {
    try {
      const modelPath = await cacheManager.getModelPath(model.filename);
      const exists = await RNFS.exists(modelPath);
      
      if (!exists) {
        return false;
      }

      const stat = await RNFS.stat(modelPath);
      const sizeDifference = Math.abs(stat.size - model.size);
      const sizeToleranceBytes = model.size * 0.01; // 1% tolerance

      return sizeDifference <= sizeToleranceBytes;
    } catch (error) {
      console.error('Error validating model integrity:', error);
      return false;
    }
  }

  cleanup(): void {
    // Cancel all active downloads
    for (const [filename] of this.activeDownloads) {
      this.cancelDownload(filename);
    }
    
    this.activeDownloads.clear();
    this.downloadProgress.clear();
  }
}

// Singleton instance
export const modelDownloader = new ModelDownloader();
