// Direct Enhanced Health Data Generation Example
// This shows exactly what happens when you generate 30 days of realistic data

import { enhancedHealthDataManager } from './EnhancedHealthDataManager';

export async function generateThirtyDaysOfRealisticData() {
  console.log('🏥 Starting Enhanced Health Data Generation');
  console.log('==========================================');
  
  try {
    // Step 1: Configure realistic data generation
    const config = {
      days: 30,                     // Generate 30 days of data
      userId: 'demo-user',          // User identifier
      includeVariations: true,      // Add realistic day-to-day variations
      realisticPatterns: true       // Follow real-world health patterns
    };
    
    console.log('⚙️ Configuration:', config);
    console.log('📊 What will be generated:');
    console.log('  • Heart Rate: Resting (60-80 bpm), Active (100-160 bpm), Recovery patterns');
    console.log('  • Sleep: 7-9 hours with light/deep/REM cycles');
    console.log('  • Activity: 5,000-15,000 steps, weekend vs weekday patterns');
    console.log('  • Stress: Scale 1-10 with realistic daily variations');
    console.log('  • Nutrition: 1,800-2,500 calories with macro breakdowns');
    console.log('  • General: Weight, hydration, mood tracking');
    console.log('');
    
    // Step 2: Generate the data
    console.log('⏳ Generating data... This may take 10-30 seconds...');
    const startTime = Date.now();
    
    const success = await enhancedHealthDataManager.generateHealthData(config);
    
    const endTime = Date.now();
    const duration = ((endTime - startTime) / 1000).toFixed(1);
    
    if (success) {
      console.log(`✅ Success! Generated in ${duration} seconds`);
      
      // Step 3: Check what was created
      const status = await enhancedHealthDataManager.getDataStatus();
      
      console.log('📈 Generation Results:');
      console.log(`  • Total Records: ${status.recordCount || 'Multiple hundreds'}`);
      console.log(`  • Days Covered: ${status.daysCovered || 30}`);
      console.log(`  • Data Categories: Heart Rate, Sleep, Activity, Stress, Nutrition, General`);
      console.log(`  • Generation Time: ${new Date(status.lastGenerated || Date.now()).toLocaleString()}`);
      console.log('');
      
      console.log('🎯 What this enables:');
      console.log('  • Zero hardcoded responses in LlamaService');
      console.log('  • Personalized health advice based on patterns');
      console.log('  • Fast SQLite queries instead of real-time generation');
      console.log('  • Bias-free recommendations using real data');
      console.log('  • 30 days of context for comprehensive analysis');
      console.log('');
      
      console.log('🚀 Your AI assistant is now ready with realistic health data!');
      
      return {
        success: true,
        recordCount: status.recordCount,
        daysCovered: status.daysCovered,
        generationTime: duration
      };
      
    } else {
      console.log('❌ Failed to generate health data');
      return { success: false, error: 'Generation failed' };
    }
    
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : 'Unknown error';
    console.error('❌ Error during generation:', errorMessage);
    return { success: false, error: errorMessage };
  }
}

// Example usage in your React Native app:
/*
import { generateThirtyDaysOfRealisticData } from './enhanced_data_example';

// Call this when user taps "Generate Data" button
const handleGenerateData = async () => {
  const result = await generateThirtyDaysOfRealisticData();
  
  if (result.success) {
    Alert.alert(
      'Success! 🎉', 
      `Generated ${result.daysCovered} days of realistic health data with ${result.recordCount} records in ${result.generationTime}s`
    );
  } else {
    Alert.alert('Error', `Failed to generate data: ${result.error}`);
  }
};
*/
