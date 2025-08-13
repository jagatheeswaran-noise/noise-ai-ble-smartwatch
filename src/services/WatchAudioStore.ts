import RNFS from 'react-native-fs'
import { toByteArray, fromByteArray as toBase64 } from 'base64-js'

export class WatchAudioStore {
  private sessionId: string | null = null
  private startedAt: number = 0
  private opusCachePath: string | null = null
  private opusFrameCount: number = 0
  private opusFrames: Uint8Array[] = []

  async start(sessionId: string) {
    this.sessionId = sessionId
    this.startedAt = Date.now()
    this.opusFrameCount = 0
    this.opusFrames = [] // Clear frames array
    const cacheDir = RNFS.CachesDirectoryPath
    this.opusCachePath = `${cacheDir}/watch_opus_${sessionId}_${this.startedAt}.opus`
    // Ensure file is fresh
    try {
      await RNFS.unlink(this.opusCachePath).catch(() => {})
    } catch {}
    console.log(`🎤⌚ WatchAudioStore: started session=${sessionId}, opus=${this.opusCachePath}`)
  }

  async addOpusBase64(base64Data: string) {
    try {
      // Convert base64 to raw bytes and store in memory
      const rawBytes = toByteArray(base64Data)
      this.opusFrames.push(rawBytes)
      this.opusFrameCount += 1
      
      console.log(`🎤⌚ WatchAudioStore: stored opus frame#${this.opusFrameCount}, bytes=${rawBytes.length}, totalFrames=${this.opusFrames.length}`)
      
      if (this.opusFrameCount <= 3) {
        console.log(`🎤⌚ WatchAudioStore: frame preview: ${Array.from(rawBytes.slice(0, 8)).map(b => b.toString(16).padStart(2, '0')).join(' ')}`)
      }
    } catch (e) {
      console.warn('🎤⌚ WatchAudioStore: failed to process opus frame', e)
    }
  }

  async finalize(): Promise<string | null> {
    console.log(`🎤⌚ WatchAudioStore: finalize called - sessionId=${this.sessionId}, frames=${this.opusFrames.length}, count=${this.opusFrameCount}`)
    
    if (!this.sessionId || this.opusFrames.length === 0) {
      console.warn(`🎤⌚ WatchAudioStore: cannot finalize - sessionId=${this.sessionId}, frames=${this.opusFrames.length}`)
      return null
    }
    
    try {
      // Concatenate all frames into one buffer
      const totalSize = this.opusFrames.reduce((sum, frame) => sum + frame.length, 0)
      const combinedBuffer = new Uint8Array(totalSize)
      let offset = 0
      
      console.log(`🎤⌚ WatchAudioStore: concatenating ${this.opusFrames.length} frames into ${totalSize} bytes`)
      
      for (const frame of this.opusFrames) {
        combinedBuffer.set(frame, offset)
        offset += frame.length
      }
      
      // Save to Downloads folder so user can easily access
      const outDir = `${RNFS.DownloadDirectoryPath}/NoiseAI_WatchRecordings`
      await RNFS.mkdir(outDir).catch(() => {}) // Create if doesn't exist
      const timestamp = new Date().toISOString().replace(/[:.]/g, '-')
      const outPath = `${outDir}/watch_opus_${this.sessionId}_${timestamp}.opus`
      
      console.log(`🎤⌚ WatchAudioStore: writing to ${outPath}`)
      
      // Convert Uint8Array to base64 string for RNFS compatibility
      const base64String = toBase64(combinedBuffer)
      await RNFS.writeFile(outPath, base64String, 'base64')
      console.log(`🎤⌚ WatchAudioStore: saved opus at ${outPath} (frames=${this.opusFrameCount}, totalBytes=${totalSize})`)
      return outPath
      
    } catch (e) {
      console.warn('🎤⌚ WatchAudioStore: failed to persist opus file to Downloads, trying Documents', e)
      // Fallback to Documents directory
      try {
        const outDir = `${RNFS.DocumentDirectoryPath}/watch_recordings`
        await RNFS.mkdir(outDir)
        const timestamp = new Date().toISOString().replace(/[:.]/g, '-')
        const outPath = `${outDir}/watch_opus_${this.sessionId}_${timestamp}.opus`
        
        // Concatenate frames again for fallback
        const totalSize = this.opusFrames.reduce((sum, frame) => sum + frame.length, 0)
        const combinedBuffer = new Uint8Array(totalSize)
        let offset = 0
        for (const frame of this.opusFrames) {
          combinedBuffer.set(frame, offset)
          offset += frame.length
        }
        
        // Convert Uint8Array to base64 string for RNFS compatibility
        const base64String = toBase64(combinedBuffer)
        await RNFS.writeFile(outPath, base64String, 'base64')
        console.log(`🎤⌚ WatchAudioStore: saved opus at ${outPath} (fallback) (frames=${this.opusFrameCount}, totalBytes=${totalSize})`)
        return outPath
      } catch (e2) {
        console.warn('🎤⌚ WatchAudioStore: fallback also failed', e2)
        return null
      }
    } finally {
      // Clean up
      this.opusFrames = []
      this.sessionId = null
      this.opusFrameCount = 0
    }
  }
}

export const watchAudioStore = new WatchAudioStore()


