/**
 * Test script for Enhanced Intent Classification and Query Routing System
 * Run this to validate the end-to-end functionality
 */

import { intentClassifier } from './IntentClassifier';
import queryRouter from './QueryRouter';

interface TestCase {
  query: string;
  expectedIntent?: string;
  expectedRouting?: string;
  description: string;
}

const testCases: TestCase[] = [
  // Health Data Queries
  {
    query: "What's my sleep score?",
    expectedIntent: "health_data_query",
    expectedRouting: "local_db",
    description: "Sleep score query"
  },
  {
    query: "How many steps did I take today?",
    expectedIntent: "health_data_query", 
    expectedRouting: "local_db",
    description: "Steps query"
  },
  {
    query: "What's my heart rate?",
    expectedIntent: "health_data_query",
    expectedRouting: "local_db", 
    description: "Heart rate query"
  },
  
  // Health Coaching
  {
    query: "How can I improve my sleep quality?",
    expectedIntent: "health_advice",
    expectedRouting: "llm_health_coach",
    description: "Sleep improvement advice"
  },
  {
    query: "What should I do about my high stress levels?",
    expectedIntent: "health_advice",
    expectedRouting: "llm_health_coach",
    description: "Stress management advice"
  },
  
  // Device Actions
  {
    query: "Set an alarm for 7 AM",
    expectedIntent: "alarm_management",
    expectedRouting: "device_action",
    description: "Alarm setting"
  },
  {
    query: "Start a 25 minute timer",
    expectedIntent: "timer_management", 
    expectedRouting: "device_action",
    description: "Timer setting"
  },
  
  // External Queries
  {
    query: "What's the weather like?",
    expectedIntent: "weather_info",
    expectedRouting: "external_api",
    description: "Weather query"
  },
  {
    query: "What time is it?",
    expectedIntent: "time_query",
    expectedRouting: "external_api", 
    description: "Time query"
  },
  
  // Conversational
  {
    query: "Hello, how are you?",
    expectedIntent: "general_qa",
    expectedRouting: "llm",
    description: "Greeting"
  },
  {
    query: "Thank you for your help",
    expectedIntent: "general_qa",
    expectedRouting: "llm",
    description: "Gratitude expression"
  },
  
  // General Knowledge
  {
    query: "What are the benefits of regular exercise?",
    expectedIntent: "general_qa",
    expectedRouting: "llm",
    description: "General health knowledge"
  }
];

class EnhancedIntentTester {
  
  async runAllTests(): Promise<void> {
    console.log('🧪 Starting Enhanced Intent Classification & Routing Tests\n');
    
    let passedTests = 0;
    let totalTests = testCases.length;
    
    for (let i = 0; i < testCases.length; i++) {
      const testCase = testCases[i];
      const result = await this.runSingleTest(testCase, i + 1);
      if (result) passedTests++;
    }
    
    console.log('\n📊 Test Summary:');
    console.log(`✅ Passed: ${passedTests}/${totalTests}`);
    console.log(`❌ Failed: ${totalTests - passedTests}/${totalTests}`);
    console.log(`📈 Success Rate: ${((passedTests / totalTests) * 100).toFixed(1)}%`);
    
    if (passedTests === totalTests) {
      console.log('\n🎉 All tests passed! Enhanced system is working correctly.');
    } else {
      console.log('\n⚠️ Some tests failed. Check the output above for details.');
    }
  }
  
  private async runSingleTest(testCase: TestCase, testNumber: number): Promise<boolean> {
    console.log(`\n🔍 Test ${testNumber}: ${testCase.description}`);
    console.log(`📝 Query: "${testCase.query}"`);
    
    try {
      // Test Intent Classification
      const intentResult = intentClassifier.classifyIntent(testCase.query);
      console.log(`🎯 Classified Intent: ${intentResult.intent}`);
      console.log(`🔀 Routing: ${intentResult.routing}`);
      console.log(`📊 Confidence: ${(intentResult.meta.confidence_score * 100).toFixed(1)}%`);
      
      // Check if intent matches expected
      const intentMatch = !testCase.expectedIntent || intentResult.intent === testCase.expectedIntent;
      const routingMatch = !testCase.expectedRouting || intentResult.routing === testCase.expectedRouting;
      
      if (!intentMatch) {
        console.log(`❌ Intent mismatch. Expected: ${testCase.expectedIntent}, Got: ${intentResult.intent}`);
      }
      
      if (!routingMatch) {
        console.log(`❌ Routing mismatch. Expected: ${testCase.expectedRouting}, Got: ${intentResult.routing}`);
      }
      
      // Test Query Processing (without LLM to avoid delays)
      console.log(`🤖 Testing query processing...`);
      const response = await queryRouter.processQuery(testCase.query);
      console.log(`💬 Response: ${response.substring(0, 100)}${response.length > 100 ? '...' : ''}`);
      
      const testPassed = intentMatch && routingMatch && response.length > 0;
      
      if (testPassed) {
        console.log(`✅ Test ${testNumber} PASSED`);
      } else {
        console.log(`❌ Test ${testNumber} FAILED`);
      }
      
      return testPassed;
      
    } catch (error) {
      console.log(`💥 Test ${testNumber} ERROR:`, error);
      return false;
    }
  }
  
  async runQuickIntentTests(): Promise<void> {
    console.log('⚡ Quick Intent Classification Tests\n');
    
    const quickTests = [
      "What's my sleep score?",
      "Set alarm for 6 AM", 
      "How's the weather?",
      "Hello there!",
      "Help me improve my fitness"
    ];
    
    quickTests.forEach((query, index) => {
      console.log(`\n${index + 1}. "${query}"`);
      const result = intentClassifier.classifyIntent(query);
      console.log(`   Intent: ${result.intent} | Routing: ${result.routing} | Confidence: ${(result.meta.confidence_score * 100).toFixed(1)}%`);
    });
  }
}

// Export for use in app
export const enhancedIntentTester = new EnhancedIntentTester();

// For standalone testing
if (require.main === module) {
  enhancedIntentTester.runAllTests().catch(console.error);
}
