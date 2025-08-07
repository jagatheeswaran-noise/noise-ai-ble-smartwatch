# Enhanced Health Assistant Demo Script

## Session & Memory Management Features Demo

### Step 1: Session Management
1. **Open the app** - A new session should automatically be created when you send your first message
2. **Send first message**: "I want to improve my overall health"
   - Verify session is created
   - Check that user message is stored

### Step 2: Memory Learning
3. **Share personal health info**: "I am 28 years old and prefer swimming over running"
   - This should be automatically stored as user facts and health preferences
4. **Set a health goal**: "My goal is to sleep 8 hours per night"
   - This should be stored as a goal-setting memory

### Step 3: Context-Aware Responses
5. **Ask for personalized advice**: "What exercise routine would you recommend for me?"
   - Response should reference your age and swimming preference
   - Should feel personalized and specific

### Step 4: Session Continuity
6. **Continue the conversation**: "How can I track my progress?"
   - Response should reference the previous exercise discussion
   - Should maintain conversation flow

### Step 5: Health Data Integration
7. **Ask about health metrics**: "What's my current heart rate trend?"
   - Response should include actual health data if available
   - Should provide personalized analysis

### Step 6: Memory Recall
8. **Test memory recall**: "What did I say about my exercise preferences earlier?"
   - Should recall your swimming preference
   - Should demonstrate memory retrieval

### Step 7: Adaptive Responses
9. **Request brief answers**: "Give me a short answer - what should I eat for breakfast?"
   - System should provide a concise response
10. **Request detailed answers**: "Give me a detailed explanation of sleep hygiene"
    - System should adapt and provide comprehensive information

### Step 8: Session Persistence
11. **Close and reopen the app**
    - Previous conversation should be restored
    - Session should be available in session list
12. **Continue conversation**: "Following up on our sleep discussion..."
    - Should maintain context from before app restart

## Expected Behaviors

### Session Management
- ✅ New sessions created automatically
- ✅ Messages stored with timestamps and metadata
- ✅ Sessions persist across app restarts
- ✅ Session list organized by date groups

### Memory System
- ✅ Health preferences automatically detected and stored
- ✅ Personal facts remembered (age, preferences, etc.)
- ✅ Goals and targets tracked over time
- ✅ Interaction patterns learned and applied

### AI Enhancement
- ✅ Responses include relevant personal context
- ✅ Health data integrated into advice
- ✅ Conversation continuity maintained
- ✅ Adaptive response length based on user preference

### Data Persistence
- ✅ All data survives app restarts
- ✅ No loss of conversation history
- ✅ Memories maintained with confidence scores
- ✅ Health data context always current

## Testing Checklist

- [ ] Session automatically created on first message
- [ ] Personal information automatically stored as memories
- [ ] AI responses reference stored memories and preferences
- [ ] Conversation maintains natural flow and context
- [ ] Health data integrated into responses when relevant
- [ ] Response length adapts to user requests
- [ ] App restart preserves all conversation and memory data
- [ ] New conversations reference previous sessions when relevant
- [ ] Memory system learns and improves over time
- [ ] Performance remains smooth with stored data

## Troubleshooting

### If session not created:
- Check console for store initialization errors
- Verify AsyncStorage permissions
- Check network connectivity for persistence

### If memories not working:
- Look for memory store errors in console
- Verify user input triggers memory detection logic
- Check memory confidence thresholds

### If context missing from AI responses:
- Verify LlamaService integration with stores
- Check health data availability
- Ensure memory retrieval is working

### If data not persisting:
- Check MobX persistence configuration
- Verify AsyncStorage write permissions
- Look for serialization errors in console

This enhanced system transforms the health assistant from a simple Q&A tool into an intelligent health companion that learns, remembers, and provides increasingly personalized advice over time.
