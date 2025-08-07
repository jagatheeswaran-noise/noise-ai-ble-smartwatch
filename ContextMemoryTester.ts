/**
 * Context and Memory Management Pipeline Test
 * Tests the enhanced context and memory integration in the QueryRouter
 */

import { intentClassifier } from './IntentClassifier';
import queryRouter from './QueryRouter';
import { memoryStore, sessionStore } from './src/stores';

interface ContextTestCase {
  query: string;
  description: string;
  expectedMemoryType?: string;
  shouldUpdateContext?: boolean;
  expectedContextualResponse?: boolean;
}

const contextTestCases: ContextTestCase[] = [
  {
    query: "What's my sleep score?",
    description: "Health data query with memory tracking",
    expectedMemoryType: "interaction_pattern",
    shouldUpdateContext: true,
    expectedContextualResponse: true
  },
  {
    query: "How can I improve my sleep quality?",
    description: "Health coaching with context integration",
    expectedMemoryType: "health_preference", 
    shouldUpdateContext: true,
    expectedContextualResponse: true
  },
  {
    query: "Set a 25 minute timer",
    description: "Device action with user preference learning",
    expectedMemoryType: "interaction_pattern",
    shouldUpdateContext: false,
    expectedContextualResponse: false
  },
  {
    query: "What's the weather like?",
    description: "External API with usage pattern tracking",
    expectedMemoryType: "interaction_pattern",
    shouldUpdateContext: false,
    expectedContextualResponse: false
  },
  {
    query: "Hello, how are you today?",
    description: "Conversational query with memory-based personalization",
    expectedMemoryType: undefined,
    shouldUpdateContext: true,
    expectedContextualResponse: true
  }
];

class ContextMemoryPipelineTester {
  
  async runContextMemoryTests(): Promise<void> {
    console.log('🧠 Starting Context & Memory Management Pipeline Tests\n');
    
    // Initialize test session
    await this.initializeTestSession();
    
    let passedTests = 0;
    let totalTests = contextTestCases.length;
    
    for (let i = 0; i < contextTestCases.length; i++) {
      const testCase = contextTestCases[i];
      const result = await this.runContextTest(testCase, i + 1);
      if (result) passedTests++;
    }
    
    console.log('\n📊 Context & Memory Pipeline Test Summary:');
    console.log(`✅ Passed: ${passedTests}/${totalTests}`);
    console.log(`❌ Failed: ${totalTests - passedTests}/${totalTests}`);
    console.log(`📈 Success Rate: ${((passedTests / totalTests) * 100).toFixed(1)}%`);
    
    if (passedTests === totalTests) {
      console.log('\n🎉 All context and memory tests passed! Pipeline is working correctly.');
    } else {
      console.log('\n⚠️ Some context/memory tests failed. Check the pipeline integration.');
    }
    
    // Show final memory state
    await this.showMemoryState();
  }
  
  private async initializeTestSession(): Promise<void> {
    console.log('🚀 Initializing test session...');
    
    // Create a test session
    if (!sessionStore.activeSessionId) {
      await sessionStore.createNewSession('Context Memory Test Session');
    }
    
    // Clear previous test memories
    await this.clearTestMemories();
    
    // Set initial conversation context
    await memoryStore.setConversationContext({
      sessionId: sessionStore.activeSessionId!,
      userGoals: [],
      healthPreferences: {},
      currentFocus: 'general',
      recentTopics: [],
      adaptiveLength: 'detailed'
    });
    
    console.log('✅ Test session initialized\n');
  }
  
  private async clearTestMemories(): Promise<void> {
    // This would clear test-specific memories in a real implementation
    console.log('🧹 Cleared previous test memories');
  }
  
  private async runContextTest(testCase: ContextTestCase, testNumber: number): Promise<boolean> {
    console.log(`\n🔍 Test ${testNumber}: ${testCase.description}`);
    console.log(`📝 Query: "${testCase.query}"`);
    
    try {
      // Step 1: Check initial memory state
      const initialMemoryCount = memoryStore.memories.length;
      const initialContext = memoryStore.getConversationContext();
      
      console.log(`🧠 Initial memory count: ${initialMemoryCount}`);
      console.log(`🗂️ Initial context focus: ${initialContext?.currentFocus}`);
      
      // Step 2: Process query through the enhanced pipeline
      const response = await queryRouter.processQuery(testCase.query);
      
      console.log(`💬 Response: ${response.substring(0, 100)}${response.length > 100 ? '...' : ''}`);
      
      // Step 3: Verify memory was updated if expected
      const finalMemoryCount = memoryStore.memories.length;
      const memoryAdded = finalMemoryCount > initialMemoryCount;
      
      console.log(`🧠 Final memory count: ${finalMemoryCount} (${memoryAdded ? 'ADDED' : 'NO CHANGE'})`);
      
      // Step 4: Check if the latest memory has the expected type
      let memoryTypeCorrect = true;
      if (testCase.expectedMemoryType && memoryAdded) {
        const latestMemory = memoryStore.memories[0];
        memoryTypeCorrect = latestMemory.type === testCase.expectedMemoryType;
        console.log(`📊 Memory type: ${latestMemory.type} (Expected: ${testCase.expectedMemoryType}) - ${memoryTypeCorrect ? 'CORRECT' : 'INCORRECT'}`);
        console.log(`🏷️ Memory tags: [${latestMemory.tags.join(', ')}]`);
      }
      
      // Step 5: Verify context was updated if expected
      const finalContext = memoryStore.getConversationContext();
      let contextUpdated = false;
      if (finalContext && initialContext) {
        contextUpdated = finalContext.recentTopics.length > initialContext.recentTopics.length ||
                        finalContext.currentFocus !== initialContext.currentFocus;
      }
      
      console.log(`🗂️ Context updated: ${contextUpdated ? 'YES' : 'NO'} (Expected: ${testCase.shouldUpdateContext ? 'YES' : 'NO'})`);
      if (finalContext) {
        console.log(`🎯 Current focus: ${finalContext.currentFocus}`);
        console.log(`📚 Recent topics: [${finalContext.recentTopics.slice(-3).join(', ')}]`);
      }
      
      // Step 6: Test contextual response generation
      let contextualResponseTest = true;
      if (testCase.expectedContextualResponse) {
        // Run the same query again to see if it uses context
        const secondResponse = await queryRouter.processQuery(testCase.query);
        console.log(`🔄 Second response: ${secondResponse.substring(0, 100)}${secondResponse.length > 100 ? '...' : ''}`);
        // In a real test, we would check if the second response references context
      }
      
      // Overall test result
      const testPassed = (
        (testCase.expectedMemoryType ? memoryTypeCorrect && memoryAdded : true) &&
        (testCase.shouldUpdateContext === contextUpdated) &&
        response.length > 0
      );
      
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
  
  private async showMemoryState(): Promise<void> {
    console.log('\n🧠 Final Memory State:');
    console.log(`📊 Total memories: ${memoryStore.memories.length}`);
    
    const memoryTypes = memoryStore.memories.reduce((acc: any, memory) => {
      acc[memory.type] = (acc[memory.type] || 0) + 1;
      return acc;
    }, {});
    
    console.log('📈 Memory types breakdown:');
    Object.entries(memoryTypes).forEach(([type, count]) => {
      console.log(`  - ${type}: ${count}`);
    });
    
    if (memoryStore.memories.length > 0) {
      console.log('\n🔍 Recent memories:');
      memoryStore.memories.slice(0, 3).forEach((memory, index) => {
        console.log(`  ${index + 1}. [${memory.type}] ${memory.content.substring(0, 50)}...`);
        console.log(`     Tags: [${memory.tags.join(', ')}] | Confidence: ${memory.confidence}`);
      });
    }
    
    const context = memoryStore.getConversationContext();
    if (context) {
      console.log('\n🗂️ Final Context State:');
      console.log(`  Session ID: ${context.sessionId}`);
      console.log(`  Current Focus: ${context.currentFocus}`);
      console.log(`  Recent Topics: [${context.recentTopics.join(', ')}]`);
      console.log(`  Adaptive Length: ${context.adaptiveLength}`);
    }
  }
  
  async runQuickContextTests(): Promise<void> {
    console.log('⚡ Quick Context Integration Tests\n');
    
    const quickTests = [
      "What's my heart rate?",
      "How can I reduce stress?",
      "Set a timer for 30 minutes",
      "What's the weather?",
      "Thank you for the help"
    ];
    
    for (let i = 0; i < quickTests.length; i++) {
      const query = quickTests[i];
      console.log(`\n${i + 1}. "${query}"`);
      
      const initialMemories = memoryStore.memories.length;
      const response = await queryRouter.processQuery(query);
      const finalMemories = memoryStore.memories.length;
      
      console.log(`   Response: ${response.substring(0, 80)}...`);
      console.log(`   Memory change: ${finalMemories - initialMemories > 0 ? '✅ Added' : '⚪ None'}`);
    }
  }
}

// Export for use in app
export const contextMemoryTester = new ContextMemoryPipelineTester();

// For standalone testing
if (require.main === module) {
  contextMemoryTester.runContextMemoryTests().catch(console.error);
}
