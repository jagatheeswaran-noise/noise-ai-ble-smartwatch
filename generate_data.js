#!/usr/bin/env node

// Simple data generation script
// Usage: node generate_data.js

const path = require('path');

console.log('🏥 Health Data Generation Script');
console.log('==================================');

async function generateData() {
  try {
    // For React Native projects, you'll typically run this through your app
    console.log('📱 This script should be run within your React Native app');
    console.log('');
    console.log('💡 To generate data:');
    console.log('');
    console.log('1. Add the DataGenerationComponent to your app');
    console.log('2. Use the buttons to generate local or external data');
    console.log('3. Or call the methods directly in your code:');
    console.log('');
    console.log('   // Generate local data');
    console.log('   await enhancedHealthDataManager.generateHealthData({');
    console.log('     days: 30,');
    console.log('     userId: "your-user-id",');
    console.log('     includeVariations: true,');
    console.log('     realisticPatterns: true');
    console.log('   });');
    console.log('');
    console.log('   // Download external data');
    console.log('   await enhancedHealthDataManager.downloadExternalHealthData();');
    console.log('');
    console.log('✅ Ready to generate health data!');
    
  } catch (error) {
    console.error('❌ Error:', error.message);
  }
}

generateData();
