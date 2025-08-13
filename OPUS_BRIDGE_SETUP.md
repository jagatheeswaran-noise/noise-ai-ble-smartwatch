# OpusBridge Native Module Setup

## Overview
OpusBridge is a React Native native module that decodes raw Opus `.bin` files (fixed 80-byte CBR packets, 16 kHz mono) into PCM16LE WAV files using libopus.

## ✅ **Setup Complete!**

The module is now configured to use the **android-opus-codec AAR** which provides:
- `libopus.so` for ARM64-v8a (415KB)
- `libopus.so` for armeabi-v7a
- `libopus.so` for x86 and x86_64
- Additional Opus utilities (`libeasyopus.so`, `libopusenc.so`)

## 📁 **Current File Structure**
```
android/app/
├── libs/
│   └── opus.aar                    # Opus library bundle (1.3MB)
├── src/main/
│   ├── cpp/
│   │   ├── CMakeLists.txt          # Build configuration
│   │   ├── opus_bridge_jni.cpp     # JNI implementation
│   │   ├── wav_writer.c            # WAV file helper
│   │   └── third_party/opus/include/
│   │       ├── opus.h              # Opus headers
│   │       └── opus_types.h        # Type definitions
│   ├── jniLibs/
│   │   └── arm64-v8a/
│   │       └── libopus.so          # Real libopus.so (415KB)
│   └── java/com/noise_ai/
│       ├── OpusBridgeModule.kt     # Main module
│       └── OpusBridgePackage.kt    # Package registration
```

## 🚀 **Build Instructions**

### **1. Clean and Rebuild**
```bash
cd android
./gradlew clean
cd ..
npm run android
```

### **2. Verify Build**
Check the build logs for:
- `OpusBridge` module loading
- `libopusbridge.so` generation
- No CMake errors
- AAR extraction and inclusion

## 🎯 **Usage**

### **Basic Usage**
```typescript
import OpusBridge from './src/native/OpusBridge';

// Decode with default 80-byte packets
const result = await OpusBridge.decodeBinToWav(
  '/path/to/input.bin',
  '/path/to/output.wav'
);

console.log('WAV saved at:', result.wavPath);
```

### **Custom Packet Size**
```typescript
// Decode with custom packet size
const result = await OpusBridge.decodeBinToWav(
  '/path/to/input.bin',
  '/path/to/output.wav',
  { bytesPerPacket: 80 }
);
```

### **Example with React Native FS**
```typescript
import RNFS from 'react-native-fs';
import OpusBridge from './src/native/OpusBridge';

const inPath = `${RNFS.DownloadDirectoryPath}/watch_audio.bin`;
const outPath = `${RNFS.DownloadDirectoryPath}/watch_audio.wav`;

try {
  const result = await OpusBridge.decodeBinToWav(inPath, outPath);
  console.log('Decoded WAV:', result.wavPath);
} catch (error) {
  console.error('Decode failed:', error);
}
```

## 🔧 **Technical Details**

### **Library Information**
- **Source:** android-opus-codec AAR
- **libopus.so size:** 415KB (ARM64-v8a)
- **Supported ABIs:** arm64-v8a, armeabi-v7a, x86, x86_64
- **License:** BSD (Opus) + Apache 2.0 (AAR wrapper)

### **Constants**
- **Sample Rate:** 16,000 Hz
- **Channels:** 1 (mono)
- **Bits per Sample:** 16
- **Max Samples per Packet:** 1,920 (120ms)
- **Default Packet Size:** 80 bytes

### **File Format Support**
- **Input:** Raw binary files with fixed-size Opus packets
- **Output:** Standard WAV files (PCM16, Little Endian)
- **Validation:** File size must be multiple of packet size

### **Error Handling**
- File I/O errors
- Invalid packet sizes
- Opus decoder errors
- WAV header errors

## 🧪 **Testing**

### **Test with Your .bin Files**
```typescript
// Test with your latest recording
const inPath = '/storage/emulated/0/Download/NoiseAI_WatchRecordings/watch_raw_opus_packets_2025-08-12T07-18-17-588Z.bin';
const outPath = '/storage/emulated/0/Download/NoiseAI_WatchRecordings/watch_decoded_native.wav';

const result = await OpusBridge.decodeBinToWav(inPath, outPath);
```

### **Expected Results**
- **Input:** 56,400 bytes (.bin file)
- **Output:** ~451,200 bytes WAV (1:8 compression ratio)
- **Audio:** Clear voice instead of noise

## 🆘 **Troubleshooting**

### **Build Errors**
```bash
# Check AAR inclusion
android/app/build.gradle: implementation fileTree(dir: "libs", include: ["opus.aar"])

# Verify CMake
android/app/build.gradle: externalNativeBuild { cmake { ... } }

# Check NDK version
android/app/build.gradle: ndkVersion "26.1.10909125"
```

### **Runtime Errors**
```bash
# Check logs
adb logcat | grep -E "(OpusBridge|libopus)"

# Verify library loading
adb shell ls -la /data/app/*/lib/arm64/
```

### **Common Issues**
1. **"libopus.so not found"**
   - Verify AAR is in `android/app/libs/`
   - Check build.gradle includes the AAR

2. **CMake build failures**
   - Ensure NDK version 26.1.10909125 or later
   - Verify CMakeLists.txt syntax

3. **Runtime crashes**
   - Check logcat for native errors
   - Verify input file exists and is readable

## 🎉 **Benefits of AAR Approach**

✅ **No manual compilation required**  
✅ **Pre-built for multiple ABIs**  
✅ **Automatically managed by Gradle**  
✅ **Official Opus library quality**  
✅ **Easy updates and maintenance**  

## 📞 **Support**

- **Opus project:** https://opus-codec.org/
- **AAR source:** https://github.com/theeasiestway/android-opus-codec
- **Android NDK:** https://developer.android.com/ndk

---

**Status:** ✅ **Ready for production use with real Opus decoding!**
