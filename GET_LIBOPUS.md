# 🎵 Getting libopus.so for Android ARM64

## Current Status
✅ **OpusBridge module structure** - Complete  
✅ **Build configuration** - Complete  
✅ **Stub implementation** - Complete (generates silence for testing)  
⚠️ **Real libopus.so** - Need to obtain  

## 🚀 **Option 1: Download Prebuilt (Recommended)**

### **Source A: Official Opus Website**
1. Visit: https://opus-codec.org/downloads/
2. Look for "Android" or "ARM64" versions
3. Download and extract
4. Place `libopus.so` in: `android/app/src/main/jniLibs/arm64-v8a/`

### **Source B: Android NDK Samples**
1. Install Android NDK
2. Look in: `$ANDROID_NDK/samples/opus/`
3. Copy the generated `.so` file

### **Source C: Third-party Projects**
- FFmpeg Android builds
- WebRTC Android builds
- Other open-source audio projects

## 🔨 **Option 2: Build from Source**

### **Prerequisites**
```bash
# Install Android NDK
# Download from: https://developer.android.com/ndk/downloads
export ANDROID_NDK=/path/to/your/android-ndk
```

### **Build Steps**
```bash
# Clone Opus source
git clone https://github.com/xiph/opus.git
cd opus

# Configure for Android ARM64
./autogen.sh
./configure \
  --host=aarch64-linux-android \
  --prefix=/tmp/opus-android \
  --disable-shared \
  --enable-static \
  --disable-doc \
  --disable-extra-programs

# Build
make -j$(nproc)
make install

# Copy the library
cp /tmp/opus-android/lib/libopus.a android/app/src/main/jniLibs/arm64-v8a/
```

## 🔧 **Option 3: Use Android Studio**

1. Open Android Studio
2. Go to **SDK Manager** → **SDK Tools**
3. Install **NDK (Side by side)**
4. Use **CMake** to build libopus as part of your project

## 📁 **File Placement**

Once you have `libopus.so`, place it here:
```
android/app/src/main/jniLibs/arm64-v8a/libopus.so
```

## 🔄 **Switch from Stub to Real Library**

### **Step 1: Update CMakeLists.txt**
```cmake
# Comment out the stub
# add_library(opus_stub STATIC
#   opus_stub.cpp
# )

# Uncomment and use real libopus
add_library(opus SHARED IMPORTED)
set_target_properties(opus PROPERTIES
  IMPORTED_LOCATION
    ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libopus.so
)

# Update target_link_libraries
target_link_libraries(opusbridge 
  opus  # Instead of opus_stub
  ${log-lib}
)
```

### **Step 2: Clean and Rebuild**
```bash
cd android
./gradlew clean
cd ..
npm run android
```

## 🧪 **Testing the Module**

### **With Stub (Current)**
- Module will build and run
- Decoding will generate silence (all zeros)
- Good for testing the module structure

### **With Real libopus.so**
- Full Opus decoding functionality
- Proper audio output
- Production-ready performance

## 📊 **Expected Results**

### **File Sizes**
- **Stub library:** ~10-50 KB
- **Real libopus.so:** ~200-500 KB

### **Functionality**
- **Stub:** Generates silence, logs operations
- **Real:** Decodes Opus packets to PCM audio

## 🆘 **Troubleshooting**

### **Build Errors**
```bash
# Check NDK version
android/app/build.gradle: ndkVersion "26.1.10909125"

# Verify CMake
android/app/build.gradle: externalNativeBuild { cmake { ... } }
```

### **Runtime Errors**
```bash
# Check logs
adb logcat | grep -E "(OpusBridge|OpusStub|libopus)"

# Verify library loading
adb shell ls -la /data/app/*/lib/arm64/
```

## 🎯 **Next Steps**

1. **Get real libopus.so** (choose one option above)
2. **Update CMakeLists.txt** to use real library
3. **Clean and rebuild** the project
4. **Test with your .bin files**
5. **Verify 1:8 compression ratio** is working

## 📞 **Need Help?**

- Check Android NDK documentation
- Look at Opus project issues
- Search for "Android libopus.so" on GitHub
- Consider using a different audio codec if needed

---

**Note:** The stub implementation allows you to test the module structure immediately. Replace it with the real library when you're ready for production use.
