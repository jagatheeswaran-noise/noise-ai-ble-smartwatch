package com.noise_ai

import com.facebook.react.bridge.*
import org.concentus.OpusDecoder
import java.io.*
import java.nio.ByteOrder

class OpusBridgeModule(private val reactCtx: ReactApplicationContext)
  : ReactContextBaseJavaModule(reactCtx) {

  companion object {
    private const val SR = 16000
    private const val CH = 1
    private const val BYTES_PER_PKT = 80
    private const val FRAME_SAMPLES_MAX = (0.12 * SR).toInt() // 120 ms safety
  }

  override fun getName() = "OpusBridge"

  @ReactMethod
  fun decodeBinToWav(inPath: String, outPath: String, opts: ReadableMap?, promise: Promise) {
    try {
      val bpp = opts?.getInt("bytesPerPacket") ?: BYTES_PER_PKT
      
      // Validate input file
      val inFile = File(inPath)
      if (!inFile.exists()) {
        promise.reject("FILE_NOT_FOUND", "Input file does not exist: $inPath")
        return
      }
      
      if (inFile.length() % bpp != 0L) {
        promise.reject("INVALID_FILE", "File size is not a multiple of $bpp bytes")
        return
      }
      
      // Create output directory if it doesn't exist
      val outFile = File(outPath)
      outFile.parentFile?.mkdirs()
      
      // Decode using pure Java implementation
      val success = decodeBinToWavPureJava(inFile, outFile, bpp)
      
      if (success) {
        val res = Arguments.createMap()
        res.putString("wavPath", outPath)
        res.putInt("bytesPerPacket", bpp)
        res.putInt("inputSize", inFile.length().toInt())
        res.putInt("outputSize", outFile.length().toInt())
        promise.resolve(res)
      } else {
        promise.reject("DECODE_FAILED", "Failed to decode Opus file")
      }
      
    } catch (t: Throwable) {
      promise.reject("DECODE_EXCEPTION", t)
    }
  }

  private fun decodeBinToWavPureJava(inFile: File, outFile: File, bytesPerPkt: Int): Boolean {
    return try {
      require(inFile.length() % bytesPerPkt == 0L) { "BIN not ${bytesPerPkt}B-aligned" }

      val dec = OpusDecoder(SR, CH)
      val pcmFrame = ShortArray(FRAME_SAMPLES_MAX)
      val pcmOut = ByteArrayOutputStream()

      FileInputStream(inFile).use { fi ->
        val pkt = ByteArray(bytesPerPkt)
        while (true) {
          val read = fi.read(pkt)
          if (read <= 0) break
          val samples = dec.decode(pkt, 0, read, pcmFrame, 0, FRAME_SAMPLES_MAX, false)
          // write little-endian PCM16
          for (i in 0 until samples) {
            val s = pcmFrame[i].toInt()
            pcmOut.write(s and 0xFF)
            pcmOut.write((s ushr 8) and 0xFF)
          }
        }
      }
      
      val pcmBytes = pcmOut.toByteArray()
      writeWavHeaderAndData(outFile, pcmBytes, SR, CH, 16)
      
      true
    } catch (e: Exception) {
      android.util.Log.e("OpusBridge", "Decode failed: ${e.message}", e)
      false
    }
  }

  private fun writeWavHeaderAndData(outFile: File, pcm: ByteArray, sr: Int, ch: Int, bps: Int) {
    val byteRate = sr * ch * (bps / 8)
    val blockAlign = ch * (bps / 8)
    val dataSize = pcm.size
    val riffSize = 36 + dataSize
    
    DataOutputStream(BufferedOutputStream(FileOutputStream(outFile))).use { w ->
      w.writeBytes("RIFF")
      w.writeIntLE(riffSize)
      w.writeBytes("WAVE")
      w.writeBytes("fmt ")
      w.writeIntLE(16)
      w.writeShortLE(1) // PCM
      w.writeShortLE(ch.toShort().toInt())
      w.writeIntLE(sr)
      w.writeIntLE(byteRate)
      w.writeShortLE(blockAlign.toShort().toInt())
      w.writeShortLE(bps.toShort().toInt())
      w.writeBytes("data")
      w.writeIntLE(dataSize)
      w.write(pcm)
    }
  }

  private fun DataOutputStream.writeIntLE(v: Int) {
    writeByte(v)
    writeByte(v ushr 8)
    writeByte(v ushr 16)
    writeByte(v ushr 24)
  }

  private fun DataOutputStream.writeShortLE(v: Int) {
    writeByte(v)
    writeByte(v ushr 8)
  }

  private fun DataOutputStream.writeBytes(s: String) {
    write(s.toByteArray(Charsets.US_ASCII))
  }

  @ReactMethod
  fun addListener(eventName: String) {
    // Keep: Required for RN built in Event Emitter
  }

  @ReactMethod
  fun removeListeners(count: Int) {
    // Keep: Required for RN built in Event Emitter
  }
}
