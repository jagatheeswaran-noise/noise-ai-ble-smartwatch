import RNFS from 'react-native-fs'
import AsyncStorage from '@react-native-async-storage/async-storage'

export type WhisperModelInfo = {
  name: string
  filename: string
  url: string
  size: number // bytes (approximate)
  description: string
}

export type WhisperDownloadProgress = {
  bytesWritten: number
  contentLength: number
  progress: number
}

const ACTIVE_MODEL_KEY = 'whisper_active_model_filename'

class WhisperModelService {
  private models: WhisperModelInfo[] = [
    {
      name: 'Whisper base.en (Q5_1)',
      filename: 'ggml-base.en-q5_1.bin',
      url: 'https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.en-q5_1.bin?download=true',
      size: 154_000_000, // ~154 MB
      description: 'English-only base model (quantized Q5_1) for on-device ASR',
    },
  ]

  getAvailableModels(): WhisperModelInfo[] {
    return this.models
  }

  async getModelsDir(): Promise<string> {
    const dir = `${RNFS.DocumentDirectoryPath}/models`
    const exists = await RNFS.exists(dir)
    if (!exists) await RNFS.mkdir(dir)
    return dir
  }

  async isModelDownloaded(model: WhisperModelInfo): Promise<boolean> {
    const dir = await this.getModelsDir()
    const path = `${dir}/${model.filename}`
    return RNFS.exists(path)
  }

  async getModelPath(modelOrFilename: WhisperModelInfo | string): Promise<string> {
    const dir = await this.getModelsDir()
    const filename = typeof modelOrFilename === 'string' ? modelOrFilename : modelOrFilename.filename
    return `${dir}/${filename}`
  }

  async downloadModel(
    model: WhisperModelInfo,
    onProgress?: (p: WhisperDownloadProgress) => void,
  ): Promise<boolean> {
    const toFile = await this.getModelPath(model)
    // If already exists, short-circuit
    if (await RNFS.exists(toFile)) {
      onProgress?.({ bytesWritten: model.size, contentLength: model.size, progress: 1 })
      return true
    }

    const job = RNFS.downloadFile({
      fromUrl: model.url,
      toFile,
      cacheable: false,
      progressInterval: 1000,
      progress: res => {
        onProgress?.({
          bytesWritten: res.bytesWritten,
          contentLength: res.contentLength,
          progress: res.bytesWritten / (res.contentLength || model.size),
        })
      },
    })

    const result = await job.promise
    return result.statusCode === 200
  }

  async deleteModel(model: WhisperModelInfo): Promise<boolean> {
    const path = await this.getModelPath(model)
    if (await RNFS.exists(path)) {
      await RNFS.unlink(path)
    }
    const active = await this.getActiveModelFilename()
    if (active === model.filename) {
      await AsyncStorage.removeItem(ACTIVE_MODEL_KEY)
    }
    return true
  }

  async setActiveModel(model: WhisperModelInfo): Promise<void> {
    await AsyncStorage.setItem(ACTIVE_MODEL_KEY, model.filename)
  }

  async getActiveModelFilename(): Promise<string | null> {
    return AsyncStorage.getItem(ACTIVE_MODEL_KEY)
  }

  async getActiveModelPathOrDefault(): Promise<string> {
    const active = (await this.getActiveModelFilename()) || this.models[0].filename
    return this.getModelPath(active)
  }
}

export const whisperModelService = new WhisperModelService()


