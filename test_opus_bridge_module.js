/**
 * Test script for OpusBridge module
 * Run this in your React Native app to test the native module
 */

import { NativeModules } from 'react-native';
import RNFS from 'react-native-fs';

const { OpusBridge } = NativeModules;

console.log('🎵 Testing OpusBridge Module...');
console.log('Available methods:', Object.keys(OpusBridge));

// Test 1: Check if module is available
if (!OpusBridge) {
  console.error('❌ OpusBridge module not found!');
} else {
  console.log('✅ OpusBridge module found');
  
  // Test 2: Test with a small .bin file
  const testBinFile = '/storage/emulated/0/Download/NoiseAI_WatchRecordings/watch_raw_opus_packets_2025-08-12T07-18-17-588Z.bin';
  const outputWavFile = '/storage/emulated/0/Download/NoiseAI_WatchRecordings/test_decoded_native.wav';
  
  console.log('📁 Input file:', testBinFile);
  console.log('📁 Output file:', outputWavFile);
  
  // Test 3: Check if input file exists
  RNFS.exists(testBinFile)
    .then((exists) => {
      if (exists) {
        console.log('✅ Input .bin file exists');
        
        // Test 4: Get file size
        return RNFS.stat(testBinFile);
      } else {
        console.error('❌ Input .bin file not found');
        throw new Error('Input file not found');
      }
    })
    .then((stats) => {
      console.log(`📊 Input file size: ${stats.size} bytes`);
      
      // Test 5: Call OpusBridge.decodeBinToWav
      console.log('🔄 Calling OpusBridge.decodeBinToWav...');
      return OpusBridge.decodeBinToWav(testBinFile, outputWavFile);
    })
    .then((result) => {
      console.log('✅ OpusBridge.decodeBinToWav completed successfully!');
      console.log('📊 Result:', result);
      
      // Test 6: Check if output file was created
      return RNFS.exists(outputWavFile);
    })
    .then((exists) => {
      if (exists) {
        console.log('✅ Output WAV file created successfully!');
        
        // Test 7: Get output file size
        return RNFS.stat(outputWavFile);
      } else {
        console.error('❌ Output WAV file not created');
        throw new Error('Output file not created');
      }
    })
    .then((stats) => {
      console.log(`📊 Output WAV file size: ${stats.size} bytes`);
      
      // Calculate compression ratio
      const inputSize = 56400; // Known size of the test file
      const outputSize = stats.size;
      const compressionRatio = (outputSize / inputSize).toFixed(2);
      
      console.log(`🎯 Compression ratio: ${compressionRatio}x (expected: ~8x)`);
      
      if (compressionRatio > 6 && compressionRatio < 10) {
        console.log('🎉 SUCCESS: OpusBridge is working correctly!');
        console.log('🎵 You should now have clear audio instead of noise!');
      } else {
        console.log('⚠️  WARNING: Compression ratio seems unusual');
        console.log('   This might indicate an issue with the decoding');
      }
    })
    .catch((error) => {
      console.error('❌ Test failed:', error);
      console.error('Stack trace:', error.stack);
    });
}

// Test 8: Test with custom packet size
console.log('\n🧪 Testing with custom packet size...');
const customOutputFile = '/storage/emulated/0/Download/NoiseAI_WatchRecordings/test_custom_packet.wav';

OpusBridge.decodeBinToWav(
  '/storage/emulated/0/Download/NoiseAI_WatchRecordings/watch_raw_opus_packets_2025-08-12T07-18-17-588Z.bin',
  customOutputFile,
  { bytesPerPacket: 80 }
)
.then((result) => {
  console.log('✅ Custom packet size test successful:', result);
})
.catch((error) => {
  console.error('❌ Custom packet size test failed:', error);
});

console.log('\n🎯 Test completed! Check the logs above for results.');
