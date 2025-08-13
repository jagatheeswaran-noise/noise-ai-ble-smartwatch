// Llama service type definitions

export interface LlamaConfig {
  modelPath?: string;
  isModelLoaded: boolean;
  context: string;
  autoOffloadEnabled: boolean;
  lastUsedTimestamp: number;
}

export interface ModelDownloadProgress {
  bytesWritten: number;
  contentLength: number;
  progress: number;
}

export interface ModelInfo {
  name: string;
  url: string;
  filename: string;
  size: number;
  description: string;
}

export interface StorageInfo {
  available: number;
  used: number;
  total: number;
  usagePercentage: number;
}

export interface MemoryInfo {
  used: number;
  total: number;
  available: number;
}

export interface StreamingStatus {
  isStreaming: boolean;
  isPaused: boolean;
  canStop: boolean;
  tokensGenerated: number;
}

export type StorageHealth = 'healthy' | 'warning' | 'critical';

export class DownloadError extends Error {
  code?: string;
  retryable?: boolean;

  constructor(message: string, code?: string, retryable: boolean = true) {
    super(message);
    this.name = 'DownloadError';
    this.code = code;
    this.retryable = retryable;
  }
}
