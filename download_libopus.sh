#!/bin/bash

# Script to download libopus.so for Android ARM64
# Run this script to get the real libopus library

echo "🎵 Downloading libopus.so for Android ARM64..."

# Create directory if it doesn't exist
mkdir -p android/app/src/main/jniLibs/arm64-v8a

# Try multiple sources for libopus.so
echo "📥 Trying source 1: Prebuilt Android library..."

# Source 1: Try to download from a known Android project
curl -L -o android/app/src/main/jniLibs/arm64-v8a/libopus.so \
  "https://github.com/illuspas/Android-MediaCodec-Encoder/raw/master/libs/arm64-v8a/libopus.so" \
  --fail --silent --show-error

if [ $? -eq 0 ]; then
    echo "✅ Successfully downloaded from source 1"
else
    echo "❌ Source 1 failed, trying source 2..."
    
    # Source 2: Try to download from another source
    curl -L -o android/app/src/main/jniLibs/arm64-v8a/libopus.so \
      "https://github.com/illuspas/Android-MediaCodec-Encoder/raw/master/libs/arm64-v8a/libopus.so" \
      --fail --silent --show-error
    
    if [ $? -eq 0 ]; then
        echo "✅ Successfully downloaded from source 2"
    else
        echo "❌ Source 2 failed"
        echo ""
        echo "🔧 Manual download required:"
        echo "1. Visit: https://opus-codec.org/downloads/"
        echo "2. Download Android ARM64 version"
        echo "3. Extract and place libopus.so in: android/app/src/main/jniLibs/arm64-v8a/"
        echo ""
        echo "Or build from source:"
        echo "1. Install Android NDK"
        echo "2. Clone: git clone https://github.com/xiph/opus.git"
        echo "3. Build for ARM64: ./configure --host=aarch64-linux-android"
        echo "4. Copy the generated .so file"
        exit 1
    fi
fi

# Verify the file
if [ -f "android/app/src/main/jniLibs/arm64-v8a/libopus.so" ]; then
    file_size=$(stat -f%z "android/app/src/main/jniLibs/arm64-v8a/libopus.so" 2>/dev/null || stat -c%s "android/app/src/main/jniLibs/arm64-v8a/libopus.so" 2>/dev/null)
    echo "📁 File downloaded: android/app/src/main/jniLibs/arm64-v8a/libopus.so"
    echo "📏 File size: $file_size bytes"
    
    # Check if it's a valid shared library
    if file "android/app/src/main/jniLibs/arm64-v8a/libopus.so" | grep -q "ELF\|shared object\|ARM\|aarch64"; then
        echo "✅ Valid shared library detected"
    else
        echo "⚠️  Warning: File may not be a valid shared library"
        echo "   You may need to manually download the correct file"
    fi
else
    echo "❌ Download failed"
    exit 1
fi

echo ""
echo "🎯 Next steps:"
echo "1. Update CMakeLists.txt to use 'opus' instead of 'opus_stub'"
echo "2. Clean and rebuild: cd android && ./gradlew clean && cd .. && npm run android"
echo "3. Test the OpusBridge module"
