// Simple health data generation script using CommonJS
const { execSync } = require('child_process');

console.log('🏥 Generating 30 Days of Realistic Health Data');
console.log('=============================================');

// Since this is a React Native project, let's create a simple script
// that shows how to integrate the enhanced health data generation

const healthDataGenerationCode = `
import { enhancedHealthDataManager } from './EnhancedHealthDataManager';

async function generateRealisticHealthData() {
  try {
    console.log('📊 Starting health data generation...');
    
    // Generate 30 days of realistic health data
    const config = {
      days: 30,
      userId: 'demo-user-' + Date.now(),
      includeVariations: true,
      realisticPatterns: true
    };
    
    console.log('🔧 Configuration:', config);
    console.log('⏳ Generating data... This may take a moment...');
    
    const success = await enhancedHealthDataManager.generateHealthData(config);
    
    if (success) {
      console.log('✅ Successfully generated 30 days of realistic health data!');
      
      // Check the data status
      const status = await enhancedHealthDataManager.getDataStatus();
      console.log('📋 Data Status:', {
        hasData: status.hasData,
        recordCount: status.recordCount,
        daysCovered: status.daysCovered,
        lastGenerated: status.lastGenerated
      });
      
      console.log('🎯 Data includes:');
      console.log('  • Heart rate patterns (resting, active, recovery)');
      console.log('  • Sleep cycles (light, deep, REM sleep)');
      console.log('  • Activity levels (steps, calories, exercise)');
      console.log('  • Stress indicators (daily variations)');
      console.log('  • Nutrition data (calories, macros, hydration)');
      console.log('  • General health metrics (weight, mood, etc.)');
      
      return true;
    } else {
      console.log('❌ Failed to generate health data');
      return false;
    }
    
  } catch (error) {
    console.error('❌ Error generating health data:', error.message);
    return false;
  }
}

// Export for use in React Native app
export { generateRealisticHealthData };
`;

console.log('📝 Here\'s how to generate 30 days of realistic health data:');
console.log('');
console.log('1. Add this to your React Native component:');
console.log('');
console.log(healthDataGenerationCode);
console.log('');
console.log('2. Or call it directly in your app:');
console.log('');
console.log('```typescript');
console.log('await enhancedHealthDataManager.generateHealthData({');
console.log('  days: 30,');
console.log('  userId: "your-user-id",');
console.log('  includeVariations: true,');
console.log('  realisticPatterns: true');
console.log('});');
console.log('```');
console.log('');
console.log('✨ This will create:');
console.log('  • 30 days of realistic health patterns');
console.log('  • Heart rate variations throughout the day');
console.log('  • Natural sleep cycles and quality scores');
console.log('  • Activity patterns (weekday vs weekend)');
console.log('  • Stress level fluctuations');
console.log('  • Nutrition and hydration tracking');
console.log('  • General health metrics');
console.log('');
console.log('🚀 All data will be stored in SQLite for fast, bias-free responses!');
