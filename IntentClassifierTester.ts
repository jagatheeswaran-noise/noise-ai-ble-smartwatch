/**
 * Intent Classification Test Script
 * Use this to test the intent classifier with various queries
 */

import { intentClassifier } from './IntentClassifier';

// Test queries with expected outcomes
const testQueries = [
  // ACTION queries
  { query: "Remind me to take medicine", expectedType: "ACTION", expectedRouting: "device_action" },
  { query: "Set timer for 5 minutes", expectedType: "ACTION", expectedRouting: "device_action" },
  { query: "Track 200 calories", expectedType: "ACTION", expectedRouting: "device_action" },
  
  // QA - Health Data (local_db)
  { query: "What's my resting heart rate?", expectedType: "QA", expectedRouting: "local_db" },
  { query: "How did I sleep last night?", expectedType: "QA", expectedRouting: "local_db" },
  
  // QA - Health Coach (llm_health_coach)
  { query: "Compare my sleep last week and this week", expectedType: "QA", expectedRouting: "llm_health_coach" },
  { query: "Is my fitness improving?", expectedType: "QA", expectedRouting: "llm_health_coach" },
  { query: "What does my heart rate trend mean?", expectedType: "QA", expectedRouting: "llm_health_coach" },
  
  // QA - External API
  { query: "What's the weather today?", expectedType: "QA", expectedRouting: "external_api" },
  { query: "How's the temperature?", expectedType: "QA", expectedRouting: "external_api" },
  
  // QA - LLM (general/conversational)
  { query: "What is heart rate variability?", expectedType: "QA", expectedRouting: "llm" },
  { query: "How are you?", expectedType: "QA", expectedRouting: "llm" },
  { query: "Thank you", expectedType: "QA", expectedRouting: "llm" },
];

export class IntentClassifierTester {
  
  /**
   * Run all test queries and report results
   */
  public static runTests(): void {
    console.log('🧪 Running Intent Classification Tests...\n');
    
    let passed = 0;
    let failed = 0;
    
    testQueries.forEach((test, index) => {
      const result = intentClassifier.classifyIntent(test.query);
      
      const typeMatch = result.type === test.expectedType;
      const routingMatch = result.routing === test.expectedRouting;
      const success = typeMatch && routingMatch;
      
      if (success) {
        console.log(`✅ Test ${index + 1}: "${test.query}"`);
        passed++;
      } else {
        console.log(`❌ Test ${index + 1}: "${test.query}"`);
        console.log(`   Expected: ${test.expectedType} → ${test.expectedRouting}`);
        console.log(`   Got: ${result.type} → ${result.routing}`);
        failed++;
      }
    });
    
    console.log(`\n📊 Test Results: ${passed} passed, ${failed} failed`);
    console.log(`Success Rate: ${Math.round((passed / testQueries.length) * 100)}%`);
  }
  
  /**
   * Test a single query and show detailed results
   */
  public static testQuery(query: string): void {
    console.log(`🔍 Testing query: "${query}"`);
    
    const startTime = performance.now();
    const result = intentClassifier.classifyIntent(query);
    const endTime = performance.now();
    
    console.log('📋 Classification Result:');
    console.log(`   Type: ${result.type}`);
    console.log(`   QA Subtype: ${result.qa_subtype || 'N/A'}`);
    console.log(`   Intent: ${result.intent}`);
    console.log(`   Routing: ${result.routing}`);
    console.log(`   Requires Contextual Insight: ${result.requires_contextual_insight}`);
    console.log(`   Parameters:`, result.parameters);
    console.log(`   Confidence: ${result.meta.confidence_score}`);
    console.log(`   Processing Time: ${(endTime - startTime).toFixed(2)}ms`);
  }
  
  /**
   * Test multiple variations of a query type
   */
  public static testVariations(baseQuery: string, variations: string[]): void {
    console.log(`🔄 Testing variations of: "${baseQuery}"`);
    
    [baseQuery, ...variations].forEach((query, index) => {
      const result = intentClassifier.classifyIntent(query);
      console.log(`${index === 0 ? '📍' : '  '} "${query}" → ${result.type} → ${result.routing}`);
    });
  }
}

// Example usage:
// IntentClassifierTester.runTests();
// IntentClassifierTester.testQuery("Compare my sleep this week vs last week");
// IntentClassifierTester.testVariations("What's my heart rate?", [
//   "Show me my heart rate",
//   "How's my HR?",
//   "What was my pulse yesterday?"
// ]);

export default IntentClassifierTester;
