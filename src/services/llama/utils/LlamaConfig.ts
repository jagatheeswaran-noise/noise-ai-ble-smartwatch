import { LlamaConfig, ModelInfo } from './LlamaTypes';

export class LlamaConfigManager {
  private static readonly DEFAULT_CONFIG: LlamaConfig = {
    isModelLoaded: false,
    context: 'You are Noise AI, a helpful health assistant. You provide accurate, supportive, and easy-to-understand health information. Always encourage users to consult healthcare professionals for serious concerns.',
    autoOffloadEnabled: true,
    lastUsedTimestamp: Date.now(),
  };

  // Available models
  static readonly MODELS: ModelInfo[] = [
    {
      name: 'Llama-3.2-1B-Instruct',
      url: 'https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q8_0.gguf',
      filename: 'llama-3.2-1b-instruct-q8_0.gguf',
      size: 1342177280, // ~1.3GB
      description: 'Compact 1B parameter model optimized for health assistance'
    }
  ];

  static createDefaultConfig(): LlamaConfig {
    return { ...this.DEFAULT_CONFIG };
  }

  static getDefaultModel(): ModelInfo {
    return this.MODELS[0];
  }

  static validateConfig(config: Partial<LlamaConfig>): LlamaConfig {
    return {
      ...this.DEFAULT_CONFIG,
      ...config,
    };
  }

  static getModelByName(name: string): ModelInfo | undefined {
    return this.MODELS.find(model => model.name === name);
  }

  static getModelByFilename(filename: string): ModelInfo | undefined {
    return this.MODELS.find(model => model.filename === filename);
  }
}
