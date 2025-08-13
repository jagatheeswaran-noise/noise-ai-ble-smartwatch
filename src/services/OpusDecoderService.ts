import { NativeModules } from 'react-native'
import base64 from 'base64-js'
const { OpusDecoderModule } = NativeModules

export class OpusDecoderService {
  private headerFramesToSkip = 5
  private frameCount = 0
  private sampleRate = 16000
  private channels = 1
  private isInitialized = false

  async init(): Promise<boolean> {
    try {
      if (!OpusDecoderModule) {
        console.error('🎤⌚ OpusDecoderModule not available')
        return false
      }
      
      // Reset counters and native decoder state
      await OpusDecoderModule.reset?.()
      this.frameCount = 0
      this.isInitialized = true
      console.log('🎤⌚ OpusDecoderService initialized successfully')
      return true
    } catch (error) {
      console.error('🎤⌚ Failed to initialize OpusDecoderService:', error)
      this.isInitialized = false
      return false
    }
  }

  reset() {
    this.frameCount = 0
    if (OpusDecoderModule?.reset) {
      OpusDecoderModule.reset().catch(() => {})
    }
  }

  async decodeFrame(opusFrame: Uint8Array): Promise<Uint8Array | null> {
    // Auto-initialize if needed
    if (!this.isInitialized) {
      const initialized = await this.init()
      if (!initialized) return null
    }
    
    this.frameCount += 1
    if (this.frameCount <= this.headerFramesToSkip) {
      console.log(`🎤⌚ Skipping header frame ${this.frameCount}/${this.headerFramesToSkip}`)
      return null
    }
    
    if (!OpusDecoderModule?.decodeFrame) {
      console.error('🎤⌚ OpusDecoderModule.decodeFrame not available')
      return null
    }
    
    try {
      // Convert Uint8Array to base64 using base64-js
      const base64Opus = base64.fromByteArray(opusFrame)
      const base64Pcm: string = await OpusDecoderModule.decodeFrame(base64Opus)
      if (!base64Pcm || base64Pcm.length === 0) {
        console.log('🎤⌚ No PCM data returned from decoder')
        return null
      }
      
      // Convert base64 back to Uint8Array using base64-js
      const pcmArray = base64.toByteArray(base64Pcm)
      console.log(`🎤⌚ Decoded frame ${this.frameCount}: ${opusFrame.length} opus bytes → ${pcmArray.length} PCM bytes`)
      return new Uint8Array(pcmArray)
    } catch (error) {
      console.warn(`🎤⌚ Opus decode failed for frame ${this.frameCount}:`, error)
      return null
    }
  }

  async getCompleteDecodedAudio(): Promise<Uint8Array | null> {
    if (!this.isInitialized || !OpusDecoderModule?.getCompleteDecodedAudio) {
      console.error('🎤⌚ OpusDecoderService not initialized or method not available')
      return null
    }
    
    try {
      const base64Pcm: string = await OpusDecoderModule.getCompleteDecodedAudio()
      if (!base64Pcm || base64Pcm.length === 0) {
        console.log('🎤⌚ No complete PCM data available')
        return null
      }
      
      // Convert base64 back to Uint8Array
      const pcmArray = base64.toByteArray(base64Pcm)
      console.log(`🎤⌚ Complete decoded audio: ${pcmArray.length} PCM bytes`)
      return new Uint8Array(pcmArray)
    } catch (error) {
      console.warn('🎤⌚ Failed to get complete decoded audio:', error)
      return null
    }
  }

  async saveRawOpusPackets(filePath: string): Promise<string | null> {
    if (!this.isInitialized || !OpusDecoderModule?.saveRawOpusPackets) {
      console.error('🎤⌚ OpusDecoderService not initialized or method not available')
      return null
    }
    
    try {
      const savedPath: string = await OpusDecoderModule.saveRawOpusPackets(filePath)
      if (savedPath) {
        console.log(`🎤⌚ Raw Opus packets saved to: ${savedPath}`)
        return savedPath
      }
      return null
    } catch (error) {
      console.warn('🎤⌚ Failed to save raw Opus packets:', error)
      return null
    }
  }
}

export const opusDecoderService = new OpusDecoderService()


