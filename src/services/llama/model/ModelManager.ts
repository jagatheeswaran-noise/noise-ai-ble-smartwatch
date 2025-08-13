import { initLlama, LlamaContext, ContextParams } from 'llama.rn';
import { ModelInfo, LlamaConfig } from '../utils/LlamaTypes';
import { LlamaConfigManager } from '../utils/LlamaConfig';
import { cacheManager } from '../storage/CacheManager';
import { modelDownloader } from './ModelDownloader';

export class ModelManager {
  private llamaContext: LlamaContext | null = null;
  private config: LlamaConfig;
  private isInitializing: boolean = false;

  constructor() {
    this.config = LlamaConfigManager.createDefaultConfig();
  }

  async loadModel(modelPath: string): Promise<LlamaContext> {
    if (this.isInitializing) {
      throw new Error('Model is already being initialized');
    }

    if (this.llamaContext) {
      console.log('🤖 Model already loaded, returning existing context');
      return this.llamaContext;
    }

    this.isInitializing = true;

    try {
      console.log(`🤖 Loading model from: ${modelPath}`);

      // Verify model file exists
      const modelExists = await cacheManager.modelExists(modelPath.split('/').pop() || '');
      if (!modelExists) {
        throw new Error(`Model file not found: ${modelPath}`);
      }

      const contextParams: ContextParams = {
        model: modelPath,
        use_mlock: true,
        n_ctx: 2048,
        n_threads: 4,
        use_mmap: true,
      };

      this.llamaContext = await initLlama(contextParams);
      this.config.modelPath = modelPath;
      this.config.isModelLoaded = true;
      this.config.lastUsedTimestamp = Date.now();

      console.log('✅ Model loaded successfully');
      return this.llamaContext;

    } catch (error) {
      console.error('❌ Failed to load model:', error);
      this.config.isModelLoaded = false;
      this.llamaContext = null;
      throw error;
    } finally {
      this.isInitializing = false;
    }
  }

  async unloadModel(): Promise<void> {
    if (!this.llamaContext) {
      console.log('🤖 No model to unload');
      return;
    }

    try {
      console.log('🤖 Unloading model...');
      
      // The llama.rn library handles cleanup automatically
      // We just need to clear our references
      this.llamaContext = null;
      this.config.isModelLoaded = false;
      this.config.lastUsedTimestamp = Date.now();

      console.log('✅ Model unloaded successfully');
    } catch (error) {
      console.error('❌ Error unloading model:', error);
      // Force cleanup even if there was an error
      this.llamaContext = null;
      this.config.isModelLoaded = false;
    }
  }

  async reloadModel(): Promise<LlamaContext> {
    console.log('🔄 Reloading model...');
    
    if (this.config.modelPath) {
      await this.unloadModel();
      return await this.loadModel(this.config.modelPath);
    } else {
      throw new Error('No model path configured for reload');
    }
  }

  isModelLoaded(): boolean {
    return this.config.isModelLoaded && this.llamaContext !== null;
  }

  getLoadedModel(): LlamaContext | null {
    return this.llamaContext;
  }

  getModelInfo(): { 
    isLoaded: boolean; 
    modelPath?: string; 
    lastUsed: number;
    autoOffloadEnabled: boolean;
  } {
    return {
      isLoaded: this.config.isModelLoaded,
      modelPath: this.config.modelPath,
      lastUsed: this.config.lastUsedTimestamp,
      autoOffloadEnabled: this.config.autoOffloadEnabled,
    };
  }

  async validateModel(modelPath: string): Promise<boolean> {
    try {
      const filename = modelPath.split('/').pop() || '';
      const modelExists = await cacheManager.modelExists(filename);
      
      if (!modelExists) {
        return false;
      }

      // Additional validation could be added here
      // For now, just check if file exists and has reasonable size
      const modelSize = await cacheManager.getModelSize(filename);
      return modelSize > 1024 * 1024; // At least 1MB
      
    } catch (error) {
      console.error('❌ Error validating model:', error);
      return false;
    }
  }

  updateLastUsed(): void {
    this.config.lastUsedTimestamp = Date.now();
  }

  getConfig(): LlamaConfig {
    return { ...this.config };
  }

  updateConfig(updates: Partial<LlamaConfig>): void {
    this.config = { ...this.config, ...updates };
  }

  setAutoOffload(enabled: boolean): void {
    this.config.autoOffloadEnabled = enabled;
    console.log(`🤖 Auto-offload ${enabled ? 'enabled' : 'disabled'}`);
  }

  isAutoOffloadEnabled(): boolean {
    return this.config.autoOffloadEnabled;
  }

  async getAvailableModels(): Promise<ModelInfo[]> {
    const downloadedModels = await modelDownloader.getAllDownloadedModels();
    const availableModels: ModelInfo[] = [];

    for (const model of LlamaConfigManager.MODELS) {
      const isDownloaded = downloadedModels.some(dm => dm.filename === model.filename);
      if (isDownloaded) {
        availableModels.push(model);
      }
    }

    return availableModels;
  }

  async deleteModel(model: ModelInfo): Promise<boolean> {
    // Unload if currently loaded
    if (this.config.modelPath?.includes(model.filename)) {
      await this.unloadModel();
    }

    return await cacheManager.deleteModel(model.filename);
  }

  getModelLoadDuration(): number {
    // This would need to be tracked during loading
    // For now, return 0 as placeholder
    return 0;
  }

  async resetToDefaults(): Promise<void> {
    await this.unloadModel();
    this.config = LlamaConfigManager.createDefaultConfig();
    console.log('🔄 Model manager reset to defaults');
  }

  cleanup(): void {
    this.unloadModel();
  }
}

// Singleton instance
export const modelManager = new ModelManager();
