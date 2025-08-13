import { Platform } from 'react-native'
import RNFS from 'react-native-fs'
import { initWhisper } from 'whisper.rn'
// We'll integrate Opus decoder output here once available
import { whisperModelService } from './whisper/WhisperModelService'
import { fromByteArray as toBase64, toByteArray } from 'base64-js'

type PcmChunk = Uint8Array

export class WhisperASRService {
  private pcmBuffer: PcmChunk[] = []
  private isCapturing = false
  private modelPath: string | null = null
  private whisperContext: any | null = null
  private totalBytes = 0
  private chunkCount = 0

  async ensureModel(modelFilename?: string): Promise<string> {
    if (this.modelPath) return this.modelPath
    const path = await whisperModelService.getActiveModelPathOrDefault()
    const exists = await RNFS.exists(path.replace('file://', ''))
    if (!exists) throw new Error(`Active Whisper model not found. Please download it from AI Model Manager.`)
    this.modelPath = Platform.OS === 'ios' ? `file://${path}` : path
    return this.modelPath
  }

  startCapture() {
    this.pcmBuffer = []
    this.isCapturing = true
    this.totalBytes = 0
    this.chunkCount = 0
    console.log('🎤⌚ WhisperASR: startCapture')
  }

  stopCapture() {
    this.isCapturing = false
    console.log(`🎤⌚ WhisperASR: stopCapture; chunks=${this.chunkCount}, bytes=${this.totalBytes}`)
  }

  pushPcmBytes(base64Chunk: string) {
    if (!this.isCapturing) return
    // Incoming is base64 of raw bytes from BLE (likely 16-bit PCM). Keep as bytes; we will write WAV.
    try {
      const bytes = toByteArray(base64Chunk)
      this.pcmBuffer.push(bytes)
      this.totalBytes += bytes.length
      this.chunkCount += 1
      if (this.chunkCount % 10 === 0) {
        console.log(`🎤⌚ WhisperASR: buffered ${this.chunkCount} chunks, ${this.totalBytes} bytes`)
      }
    } catch (e) {
      console.warn('🎤⌚ WhisperASR: base64 decode failed', e)
    }
  }

  // New: Accept already-decoded PCM16 data (e.g., from Opus decoder)
  pushDecodedPcm(pcmChunk: Uint8Array) {
    if (!this.isCapturing) return
    
    // Analyze audio data for debugging
    this.analyzeAudioData(pcmChunk)
    
    this.pcmBuffer.push(pcmChunk)
    this.totalBytes += pcmChunk.length
    this.chunkCount += 1
  }

  private analyzeAudioData(pcm: Uint8Array) {
    if (this.chunkCount % 20 === 0) { // Log every 20th chunk to avoid spam
      // Convert bytes to 16-bit samples for analysis
      const samples: number[] = []
      for (let i = 0; i < Math.min(pcm.length, 16); i += 2) {
        const sample = (pcm[i + 1] << 8) | pcm[i] // Little endian
        samples.push(sample)
      }
      
      // Calculate basic audio statistics
      const max = Math.max(...samples.map(Math.abs))
      const avg = samples.reduce((a, b) => a + Math.abs(b), 0) / samples.length
      const nonZero = samples.filter(s => s !== 0).length
      
      console.log(`🎤⌚ Audio Analysis - Max: ${max}, Avg: ${avg.toFixed(1)}, NonZero: ${nonZero}/${samples.length}, First4: [${samples.slice(0, 4).join(', ')}]`)
    }
  }

  private concatPcm(): Uint8Array {
    const total = this.pcmBuffer.reduce((acc, cur) => acc + cur.length, 0)
    const out = new Uint8Array(total)
    let offset = 0
    for (const chunk of this.pcmBuffer) {
      out.set(chunk, offset)
      offset += chunk.length
    }
    
    // Analyze and potentially amplify the final audio
    this.analyzeFinalAudio(out)
    
    return out
  }

  private analyzeFinalAudio(pcm: Uint8Array) {
    // Sample a few points throughout the audio for analysis
    const samplePoints = Math.min(100, pcm.length / 2)
    let maxAmplitude = 0
    let totalAmplitude = 0
    let nonZeroSamples = 0
    
    for (let i = 0; i < samplePoints; i++) {
      const pos = Math.floor((i / samplePoints) * (pcm.length - 1)) & ~1 // Even position
      const sample = Math.abs((pcm[pos + 1] << 8) | pcm[pos])
      if (sample > 0) {
        nonZeroSamples++
        totalAmplitude += sample
        maxAmplitude = Math.max(maxAmplitude, sample)
      }
    }
    
    const avgAmplitude = nonZeroSamples > 0 ? totalAmplitude / nonZeroSamples : 0
    const dynamicRange = maxAmplitude / Math.max(avgAmplitude, 1)
    
    console.log(`🎤⌚ Final Audio Stats: MaxAmp=${maxAmplitude}, AvgAmp=${avgAmplitude.toFixed(1)}, NonZero=${nonZeroSamples}/${samplePoints}, Range=${dynamicRange.toFixed(2)}, TotalBytes=${pcm.length}`)
    
    // Check if audio is too quiet (max amplitude < 1000)
    if (maxAmplitude < 1000) {
      console.warn('🎤⌚ WARNING: Audio is very quiet (max amplitude < 1000). This might cause Whisper to fail.')
    }
    
    // Check if audio has enough non-zero samples
    if (nonZeroSamples < samplePoints * 0.1) {
      console.warn('🎤⌚ WARNING: Audio has very few non-zero samples. This might cause Whisper to fail.')
    }
  }

  // Assume 16-bit PCM, mono, 16kHz. If SDK differs, adjust SR/bit depth accordingly.
  private async writeWavFile(pcm: Uint8Array, sampleRate = 16000, numChannels = 1, bitsPerSample = 16): Promise<string> {
    const wav = this.pcmToWav(pcm, { sampleRate, numChannels, bitsPerSample })
    const filePath = `${RNFS.CachesDirectoryPath}/watch_audio_${Date.now()}.wav`
    const b64 = toBase64(wav)
    await RNFS.writeFile(filePath, b64, 'base64')
    return Platform.OS === 'ios' ? `file://${filePath}` : filePath
  }

  // Expose: write the currently buffered PCM to a WAV file and return path
  async saveBufferedPcmAsWav(sampleRate = 16000): Promise<string | null> {
    const pcm = this.concatPcm()
    if (pcm.length === 0) return null
    
    // Only create temporary cache file for transcription, don't save to Downloads
    try {
      // PRIMARY: Use byte-swapped format since watch audio is Big Endian
      console.log('🎤⌚ WhisperASR: Using byte-swapped format (watch is Big Endian)')
      const swapped = this.swapEndian16(pcm)
      const wavPath = await this.writeWavFile(swapped, sampleRate, 1, 16)
      console.log(`🎤⌚ WhisperASR: wrote temporary WAV for transcription ${wavPath}`)
      
      return wavPath
      
    } catch (e) {
      console.warn('🎤⌚ WhisperASR: failed to create temporary WAV:', e)
      return null
    }
  }

  private pcmToWav(pcm: Uint8Array, opts: { sampleRate: number; numChannels: number; bitsPerSample: number }): Uint8Array {
    const { sampleRate, numChannels, bitsPerSample } = opts
    const byteRate = (sampleRate * numChannels * bitsPerSample) / 8
    const blockAlign = (numChannels * bitsPerSample) / 8
    const dataSize = pcm.length
    const buffer = new ArrayBuffer(44 + dataSize)
    const view = new DataView(buffer)

    // RIFF header
    this.writeString(view, 0, 'RIFF')
    view.setUint32(4, 36 + dataSize, true)
    this.writeString(view, 8, 'WAVE')

    // fmt chunk
    this.writeString(view, 12, 'fmt ')
    view.setUint32(16, 16, true) // PCM
    view.setUint16(20, 1, true) // audio format = PCM
    view.setUint16(22, numChannels, true)
    view.setUint32(24, sampleRate, true)
    view.setUint32(28, byteRate, true)
    view.setUint16(32, blockAlign, true)
    view.setUint16(34, bitsPerSample, true)

    // data chunk
    this.writeString(view, 36, 'data')
    view.setUint32(40, dataSize, true)

    const out = new Uint8Array(buffer)
    out.set(pcm, 44)
    return out
  }

  private swapEndian16(inBytes: Uint8Array): Uint8Array {
    const out = new Uint8Array(inBytes.length)
    for (let i = 0; i + 1 < inBytes.length; i += 2) {
      out[i] = inBytes[i + 1]
      out[i + 1] = inBytes[i]
    }
    return out
  }

  private writeString(view: DataView, offset: number, str: string) {
    for (let i = 0; i < str.length; i++) {
      view.setUint8(offset + i, str.charCodeAt(i))
    }
  }

  async transcribeBufferedAudio(modelFilename?: string): Promise<string> {
    const pcm = this.concatPcm()
    if (pcm.length === 0) return ''
    console.log(`🎤⌚ WhisperASR: total raw bytes=${pcm.length}`)

    if (!this.whisperContext) {
      const modelPath = await this.ensureModel(modelFilename)
      this.whisperContext = await initWhisper({ filePath: modelPath })
      console.log('🎤⌚ WhisperASR: whisper context initialized')
    }

    // PRIMARY: Use byte-swapped format since watch audio is Big Endian
    console.log('🎤⌚ WhisperASR: Using byte-swapped format (watch is Big Endian)')
    const swapped = this.swapEndian16(pcm)
    let wavPath = await this.writeWavFile(swapped, 16000, 1, 16)
    console.log(`🎤⌚ WhisperASR: wrote WAV (16k BE->LE corrected) ${wavPath}`)
    
    // Check WAV file size
    try {
      const wavStats = await RNFS.stat(wavPath.replace('file://', ''))
      console.log(`🎤⌚ WhisperASR: WAV file size: ${wavStats.size} bytes`)
    } catch (e) {
      console.warn('🎤⌚ WhisperASR: Could not get WAV file stats:', e)
    }
    
    let { promise } = this.whisperContext.transcribe(wavPath, { language: 'en' })
    let { result } = await promise
    let text = (result || '').trim()
    console.log('🎤⌚ WhisperASR: result (16k corrected):', text)
    if (text && text !== '[BLANK_AUDIO]') return text

    // Fallback 1: Original format (Little Endian) 
    wavPath = await this.writeWavFile(pcm, 16000, 1, 16)
    console.log(`🎤⌚ WhisperASR: wrote WAV (16k original) ${wavPath}`)
    ;({ promise } = this.whisperContext.transcribe(wavPath, { language: 'en' }))
    ;({ result } = await promise)
    text = (result || '').trim()
    console.log('🎤⌚ WhisperASR: result (16k original):', text)
    if (text && text !== '[BLANK_AUDIO]') return text

    // Fallback 2: 8 kHz with corrected endianness
    wavPath = await this.writeWavFile(swapped, 8000, 1, 16)
    console.log(`🎤⌚ WhisperASR: wrote WAV (8k corrected) ${wavPath}`)
    ;({ promise } = this.whisperContext.transcribe(wavPath, { language: 'en' }))
    ;({ result } = await promise)
    text = (result || '').trim()
    console.log('🎤⌚ WhisperASR: result (8k corrected):', text)
    return text
  }

  // NEW: Transcribe a specific WAV file (for OpusBridge output)
  async transcribeWavFile(wavPath: string, modelFilename?: string): Promise<string> {
    console.log(`🎤⌚ WhisperASR: transcribing WAV file: ${wavPath}`)

    if (!this.whisperContext) {
      const modelPath = await this.ensureModel(modelFilename)
      this.whisperContext = await initWhisper({ filePath: modelPath })
      console.log('🎤⌚ WhisperASR: whisper context initialized')
    }

    try {
      // Check if file exists
      const fileExists = await RNFS.exists(wavPath.replace('file://', ''))
      if (!fileExists) {
        throw new Error(`WAV file not found: ${wavPath}`)
      }

      // Get file stats
      const wavStats = await RNFS.stat(wavPath.replace('file://', ''))
      console.log(`🎤⌚ WhisperASR: WAV file size: ${wavStats.size} bytes`)

      // Transcribe the WAV file directly
      let { promise } = this.whisperContext.transcribe(wavPath, { language: 'en' })
      let { result } = await promise
      let text = (result || '').trim()
      
      console.log('🎤⌚ WhisperASR: transcription result:', text)
      return text

    } catch (error) {
      console.error('🎤⌚ WhisperASR: WAV file transcription failed:', error)
      throw error
    }
  }

  async saveCompletePcmAsWav(pcmData: number[], sampleRate = 16000): Promise<string | null> {
    if (pcmData.length === 0) return null
    
    // Only create temporary cache file for transcription, don't save to Downloads
    try {
      // Convert number array to Uint8Array
      const pcmBytes = new Uint8Array(pcmData.length * 2)
      for (let i = 0; i < pcmData.length; i++) {
        const sample = pcmData[i]
        pcmBytes[i * 2] = (sample & 0xff)
        pcmBytes[i * 2 + 1] = ((sample >> 8) & 0xff)
      }
      
      // Save COMPLETE CORRECTED WAV (byte-swapped) to cache only
      const correctedPcm = this.swapEndian16(pcmBytes)
      const correctedCachePath = await this.writeWavFile(correctedPcm, sampleRate, 1, 16)
      console.log(`🎤⌚ WhisperASR: created temporary WAV for transcription: ${correctedCachePath}`)
      
      return correctedCachePath
      
    } catch (e) {
      console.warn('🎤⌚ WhisperASR: failed to create temporary WAV:', e)
      return null
    }
  }
}

export const whisperASRService = new WhisperASRService()


