import RNFS from 'react-native-fs';
import OpusBridge from './OpusBridge';

export async function decodeWatchAudioBin() {
  try {
    // Input: Raw Opus packets from watch
    const inPath = `${RNFS.DownloadDirectoryPath}/NoiseAI_WatchRecordings/watch_raw_opus_packets_2025-08-12T07-18-17-588Z.bin`;
    
    // Output: Decoded WAV file
    const outPath = `${RNFS.DownloadDirectoryPath}/NoiseAI_WatchRecordings/watch_decoded_native.wav`;
    
    console.log('🎵 Starting native Opus decode...');
    console.log('📁 Input:', inPath);
    console.log('📁 Output:', outPath);
    
    // Decode with default 80-byte packets
    const result = await OpusBridge.decodeBinToWav(inPath, outPath);
    
    console.log('✅ Native decode successful:', result);
    console.log('🎵 WAV file saved at:', result.wavPath);
    
    return result;
    
  } catch (error) {
    console.error('❌ Native decode failed:', error);
    throw error;
  }
}

export async function decodeWithCustomPacketSize(binPath: string, wavPath: string, bytesPerPacket: number = 80) {
  try {
    console.log(`🎵 Decoding with ${bytesPerPacket}-byte packets...`);
    
    const result = await OpusBridge.decodeBinToWav(binPath, wavPath, { 
      bytesPerPacket 
    });
    
    console.log('✅ Custom decode successful:', result);
    return result;
    
  } catch (error) {
    console.error('❌ Custom decode failed:', error);
    throw error;
  }
}
