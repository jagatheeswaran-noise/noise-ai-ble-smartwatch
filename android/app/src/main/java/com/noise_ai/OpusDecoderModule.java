package com.noise_ai;

import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import org.concentus.OpusDecoder;

public class OpusDecoderModule extends ReactContextBaseJavaModule {
    private static final String TAG = "OpusDecoderModule";
    private OpusDecoder decoder;
    private final int sampleRate = 16000;
    private final int channels = 1;
    // Use a generous max frame size to allow decoder to output variable sizes
    private final int maxFrameSize = 1920; // samples per channel (up to 60 ms at 16 kHz)
    private int frameCount = 0;
    private final int headerFramesToSkip = 5; // firmware note

    public OpusDecoderModule(ReactApplicationContext reactContext) {
        super(reactContext);
        tryInit();
    }

    @NonNull
    @Override
    public String getName() {
        return "OpusDecoderModule";
    }

    private void tryInit() {
        try {
            if (decoder == null) {
                decoder = new OpusDecoder(sampleRate, channels);
                frameCount = 0;
                Log.d(TAG, "Initialized OpusDecoder SR=" + sampleRate + ", ch=" + channels);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to init OpusDecoder", e);
            decoder = null;
        }
    }

    @ReactMethod
    public void reset(Promise promise) {
        frameCount = 0;
        accumulatedPcm.clear();
        totalOpusBytes = 0;
        totalDecodedSamples = 0;
        
        // Clear raw packet storage
        rawOpusPackets.clear();
        expectedPacketCount = 0;
        receivedPacketCount = 0;
        firstPacketTime = 0;
        lastPacketTime = 0;
        
        promise.resolve(true);
    }

    @ReactMethod
    public void getCompleteDecodedAudio(Promise promise) {
        try {
            if (accumulatedPcm.isEmpty()) {
                promise.resolve("");
                return;
            }
            
            // Convert accumulated PCM to byte array
            int totalSamples = accumulatedPcm.size();
            short[] pcmShorts = new short[totalSamples];
            for (int i = 0; i < totalSamples; i++) {
                pcmShorts[i] = accumulatedPcm.get(i);
            }
            
            byte[] pcmBytes = shortsToLittleEndianBytes(pcmShorts, totalSamples);
            String base64Pcm = Base64.encodeToString(pcmBytes, Base64.NO_WRAP);
            
            Log.d(TAG, "Complete decoded audio: " + totalSamples + " samples, " + 
                      pcmBytes.length + " bytes (Opus: " + totalOpusBytes + " bytes)");
            
            promise.resolve(base64Pcm);
        } catch (Exception e) {
            Log.e(TAG, "Error getting complete decoded audio", e);
            promise.reject("PCM_EXPORT_ERROR", e);
        }
    }

    @ReactMethod
    public void saveRawOpusPackets(String filePath, Promise promise) {
        try {
            if (rawOpusPackets.isEmpty()) {
                promise.reject("NO_PACKETS", "No raw Opus packets available");
                return;
            }
            
            // Calculate total size
            int totalSize = 0;
            for (byte[] packet : rawOpusPackets) {
                totalSize += packet.length;
            }
            
            // Create combined buffer
            byte[] combinedBuffer = new byte[totalSize];
            int offset = 0;
            for (byte[] packet : rawOpusPackets) {
                System.arraycopy(packet, 0, combinedBuffer, offset, packet.length);
                offset += packet.length;
            }
            
            // Write to file
            java.io.File file = new java.io.File(filePath);
            file.getParentFile().mkdirs();
            
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
                fos.write(combinedBuffer);
            }
            
            Log.d(TAG, "Raw Opus packets saved: " + rawOpusPackets.size() + " packets, " + totalSize + " bytes to " + filePath);
            promise.resolve(filePath);
            
        } catch (Exception e) {
            Log.e(TAG, "Error saving raw Opus packets", e);
            promise.reject("SAVE_ERROR", e);
        }
    }

    // Buffer to accumulate all decoded PCM data
    private final java.util.List<Short> accumulatedPcm = new java.util.ArrayList<>();
    private int totalOpusBytes = 0;
    private int totalDecodedSamples = 0;
    
    // Raw packet storage for zero-loss capture
    private final java.util.List<byte[]> rawOpusPackets = new java.util.ArrayList<>();
    private int expectedPacketCount = 0;
    private int receivedPacketCount = 0;
    private long firstPacketTime = 0;
    private long lastPacketTime = 0;

    @ReactMethod
    public void decodeFrame(String base64Opus, Promise promise) {
        tryInit();
        if (decoder == null) {
            promise.reject("OPUS_INIT_ERROR", "Decoder not initialized");
            return;
        }
        if (base64Opus == null || base64Opus.isEmpty()) {
            promise.resolve("");
            return;
        }
        frameCount += 1;
        if (frameCount <= headerFramesToSkip) {
            // skip header frames
            promise.resolve("");
            return;
        }
        
        byte[] opusBytes = Base64.decode(base64Opus, Base64.NO_WRAP);
        totalOpusBytes += opusBytes.length;
        
        // Store raw packet for zero-loss capture
        rawOpusPackets.add(opusBytes.clone());
        receivedPacketCount++;
        
        // Track timing for packet analysis
        long currentTime = System.currentTimeMillis();
        if (firstPacketTime == 0) {
            firstPacketTime = currentTime;
        }
        lastPacketTime = currentTime;
        
        // Log packet details
        Log.d(TAG, "Packet " + receivedPacketCount + ": " + opusBytes.length + " bytes, " +
                  "total: " + totalOpusBytes + " bytes, time: " + (currentTime - firstPacketTime) + "ms");
        
        short[] pcmShorts = new short[maxFrameSize * channels];
        try {
            int decodedSamples = decoder.decode(opusBytes, 0, opusBytes.length, pcmShorts, 0, maxFrameSize, false);
            Log.d(TAG, "Frame " + frameCount + ": decodedSamples=" + decodedSamples + ", inLen=" + opusBytes.length);
            
            if (decodedSamples <= 0) {
                Log.w(TAG, "No samples decoded for frame " + frameCount);
                promise.resolve("");
                return;
            }
            
            // Accumulate decoded PCM data
            for (int i = 0; i < decodedSamples * channels; i++) {
                accumulatedPcm.add(pcmShorts[i]);
            }
            totalDecodedSamples += decodedSamples;
            
            // Log compression ratio
            double compressionRatio = (double) totalOpusBytes / (totalDecodedSamples * channels * 2);
            Log.d(TAG, "Compression ratio: " + String.format("%.2f:1", compressionRatio) + 
                      " (Opus: " + totalOpusBytes + " bytes, PCM: " + (totalDecodedSamples * channels * 2) + " bytes)");
            
            // Log first few samples for debugging
            if (frameCount <= 8) {
                StringBuilder samples = new StringBuilder("First samples: ");
                for (int i = 0; i < Math.min(8, decodedSamples); i++) {
                    samples.append(pcmShorts[i]).append(" ");
                }
                Log.d(TAG, samples.toString());
            }
            
            // Return the frame's PCM data for immediate processing
            byte[] pcmBytes = shortsToLittleEndianBytes(pcmShorts, decodedSamples * channels);
            String base64Pcm = Base64.encodeToString(pcmBytes, Base64.NO_WRAP);
            promise.resolve(base64Pcm);
        } catch (Exception e) {
            Log.e(TAG, "decode error; opusLen=" + opusBytes.length, e);
            promise.reject("OPUS_DECODE_ERROR", e);
        }
    }

    private static byte[] shortsToLittleEndianBytes(short[] data, int length) {
        int outLen = length * 2;
        byte[] out = new byte[outLen];
        for (int i = 0; i < length; i++) {
            short v = data[i];
            out[i * 2] = (byte) (v & 0xff);
            out[i * 2 + 1] = (byte) ((v >> 8) & 0xff);
        }
        return out;
    }
}
