// Test script for EnhancedHealthDataManager
import { enhancedHealthDataManager } from './EnhancedHealthDataManager.js';

async function testHealthDataManager() {
  console.log('🧪 Testing EnhancedHealthDataManager...');
  
  const manager = enhancedHealthDataManager;
  
  try {
    // Test generating data
    console.log('📊 Generating sample health data...');
    await manager.generateHealthData({ 
      days: 30, 
      userId: 'test-user',
      includeVariations: true,
      realisticPatterns: true
    });
    
    // Test external data download
    console.log('🌐 Testing external data download...');
    await manager.downloadExternalHealthData();
    
    // Test data source management
    console.log('🔧 Testing data source management...');
    
    // Toggle a data source
    await manager.toggleDataSource('github_health_datasets', true);
    
    // Check data status
    const status = await manager.getDataStatus();
    console.log('📋 Data status:', status);
    
    console.log('✅ All tests completed successfully!');
    
  } catch (error) {
    console.error('❌ Test failed:', error);
  }
}

testHealthDataManager();

// Run if called directly
if (require.main === module) {
  testHealthDataManager();
}

export { testHealthDataManager };
