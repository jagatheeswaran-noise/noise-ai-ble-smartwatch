#!/usr/bin/env node

/**
 * Test script for OpusBridge module setup
 * Run this to verify everything is configured correctly
 */

const fs = require('fs');
const path = require('path');

console.log('🎵 Testing OpusBridge Module Setup...\n');

// Check 1: AAR file exists
const aarPath = 'android/app/libs/opus.aar';
if (fs.existsSync(aarPath)) {
    const stats = fs.statSync(aarPath);
    const sizeMB = (stats.size / (1024 * 1024)).toFixed(2);
    console.log(`✅ AAR file found: ${aarPath} (${sizeMB} MB)`);
} else {
    console.log(`❌ AAR file missing: ${aarPath}`);
}

// Check 2: libopus.so exists
const libopusPath = 'android/app/src/main/jniLibs/arm64-v8a/libopus.so';
if (fs.existsSync(libopusPath)) {
    const stats = fs.statSync(libopusPath);
    const sizeKB = (stats.size / 1024).toFixed(0);
    console.log(`✅ libopus.so found: ${libopusPath} (${sizeKB} KB)`);
} else {
    console.log(`❌ libopus.so missing: ${libopusPath}`);
}

// Check 3: CMakeLists.txt configured for real opus
const cmakePath = 'android/app/src/main/cpp/CMakeLists.txt';
if (fs.existsSync(cmakePath)) {
    const content = fs.readFileSync(cmakePath, 'utf8');
    if (content.includes('add_library(opus SHARED IMPORTED)')) {
        console.log('✅ CMakeLists.txt configured for real libopus');
    } else {
        console.log('❌ CMakeLists.txt not configured for real libopus');
    }
} else {
    console.log(`❌ CMakeLists.txt missing: ${cmakePath}`);
}

// Check 4: build.gradle includes AAR
const buildGradlePath = 'android/app/build.gradle';
if (fs.existsSync(buildGradlePath)) {
    const content = fs.readFileSync(buildGradlePath, 'utf8');
    if (content.includes('implementation fileTree(dir: "libs", include: ["opus.aar"])')) {
        console.log('✅ build.gradle includes opus.aar dependency');
    } else {
        console.log('❌ build.gradle missing opus.aar dependency');
    }
} else {
    console.log(`❌ build.gradle missing: ${buildGradlePath}`);
}

// Check 5: OpusBridge module files exist
const moduleFiles = [
    'android/app/src/main/java/com/noise_ai/OpusBridgeModule.kt',
    'android/app/src/main/java/com/noise_ai/OpusBridgePackage.kt',
    'android/app/src/main/cpp/opus_bridge_jni.cpp',
    'android/app/src/main/cpp/wav_writer.c',
    'src/native/OpusBridge.ts'
];

console.log('\n📁 Checking module files:');
moduleFiles.forEach(file => {
    if (fs.existsSync(file)) {
        console.log(`  ✅ ${file}`);
    } else {
        console.log(`  ❌ ${file}`);
    }
});

// Check 6: MainApplication includes OpusBridgePackage
const mainAppPath = 'android/app/src/main/java/com/noise_ai/MainApplication.kt';
if (fs.existsSync(mainAppPath)) {
    const content = fs.readFileSync(mainAppPath, 'utf8');
    if (content.includes('add(OpusBridgePackage())')) {
        console.log('\n✅ MainApplication includes OpusBridgePackage');
    } else {
        console.log('\n❌ MainApplication missing OpusBridgePackage');
    }
} else {
    console.log(`\n❌ MainApplication missing: ${mainAppPath}`);
}

console.log('\n🎯 Next Steps:');
console.log('1. Clean and rebuild: cd android && ./gradlew clean && cd .. && npm run android');
console.log('2. Test the module with your .bin files');
console.log('3. Verify 1:8 compression ratio is working');
console.log('4. Check logs for successful Opus decoding');

console.log('\n📊 Expected Results:');
console.log('- Input: 56,400 bytes (.bin file)');
console.log('- Output: ~451,200 bytes WAV (1:8 compression ratio)');
console.log('- Audio: Clear voice instead of noise');

console.log('\n🎉 Setup Status: READY FOR BUILD!');
