/**
 * Script to clear system prompts from memory store
 */

import AsyncStorage from '@react-native-async-storage/async-storage';

async function clearMemory() {
  try {
    // Clear the memory store
    await AsyncStorage.removeItem('MemoryStore');
    console.log('✅ Cleared memory store - system prompts removed');
    
    // Also clear any other potential storage
    await AsyncStorage.removeItem('SessionStore');
    console.log('✅ Cleared session store');
    
    console.log('🧹 Memory cleanup complete!');
  } catch (error) {
    console.error('❌ Error clearing memory:', error);
  }
}

clearMemory();
