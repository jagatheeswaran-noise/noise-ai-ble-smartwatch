# Enhanced Context & Memory Management Features

## Overview

The health assistant now features robust session-based context management and intelligent memory capabilities, inspired by PocketPal AI's architecture. This system provides persistent conversation history, adaptive learning, and personalized responses based on user interactions and health data.

## Architecture

### Session Management (SessionStore)
- **Persistent Sessions**: Chat sessions are automatically saved and restored using MobX persistence
- **Session Grouping**: Conversations are organized by date (Today, Yesterday, This Week, etc.)
- **Message History**: Complete conversation history with metadata tracking
- **Context Continuity**: Each session maintains its own user context and conversation flow

### Memory Management (MemoryStore)
- **Intelligent Memory Storage**: Automatically captures user preferences, health goals, and interaction patterns
- **Relevance Scoring**: Advanced algorithm to retrieve most relevant memories based on context, confidence, recency, and usage
- **Memory Types**: Health preferences, goal settings, user facts, and interaction patterns
- **Automatic Cleanup**: Old, low-confidence memories are automatically pruned

### Enhanced LlamaService Integration
- **Context-Aware Prompts**: AI responses include session history, relevant memories, and health data
- **Adaptive Response Length**: Learns user preferences for brief, moderate, or detailed responses
- **Session Continuity**: Maintains conversation flow across interactions
- **Memory Learning**: Automatically extracts and stores meaningful information from conversations

## Key Features

### 1. Session-Based Conversations
```typescript
// Create new session
await sessionStore.createNewSession('Health Goals Discussion');

// Add messages with metadata
await sessionStore.addMessageToCurrentSession({
  author: 'user',
  text: 'I want to improve my sleep quality',
  createdAt: Date.now(),
  type: 'text',
  metadata: { healthQuery: true }
});

// Access session context
const context = sessionStore.getSessionContext();
```

### 2. Intelligent Memory System
```typescript
// Store health preferences
await memoryStore.addHealthPreference(
  'User prefers morning workouts',
  { timestamp: Date.now(), confidence: 0.8 }
);

// Retrieve relevant memories
const memories = await memoryStore.getRelevantMemories(
  'exercise routine', 
  'health_preference', 
  5
);

// Update conversation context
await memoryStore.updateConversationContext({
  currentFocus: 'fitness',
  adaptiveLength: 'detailed'
});
```

### 3. Enhanced AI Responses
- **Personalized Context**: Responses include user's health data, goals, and preferences
- **Memory Integration**: Leverages past conversations for consistent, personalized advice
- **Adaptive Length**: Automatically adjusts response detail based on user feedback
- **Session Awareness**: Maintains conversation continuity and references previous topics

### 4. Health Data Integration
- **Real-time Health Context**: Current health metrics are included in AI responses
- **Goal Progress Tracking**: AI responses reference user's progress toward health goals
- **Personalized Recommendations**: Advice tailored to user's specific health profile and history

## Usage Examples

### Basic Session Management
```typescript
// Initialize session for new conversation
if (!sessionStore.activeSessionId) {
  await sessionStore.createNewSession('Daily Health Check');
}

// Process user input with full context
await llamaService.generateHealthAdvice(
  userInput,
  onTokenCallback,
  onCompleteCallback
);
```

### Memory-Enhanced Responses
The AI will now include relevant context like:
- "Based on your previous goal to exercise more..."
- "Since you mentioned preferring morning workouts..."
- "Following up on our discussion about sleep quality..."

### Adaptive Learning
The system automatically:
- Learns user's preferred response length
- Identifies common health topics of interest
- Remembers user's goals and preferences
- Adapts advice based on interaction patterns

## Data Persistence

### Session Storage
- Sessions are persisted using MobX + AsyncStorage
- Automatic serialization/deserialization of session data
- Cross-app-restart persistence

### Memory Storage
- Memories stored with confidence scores and metadata
- Automatic relevance scoring for retrieval
- Configurable memory limits and cleanup policies

### Health Data Integration
- Seamless integration with HealthDataManager
- Real-time health context in conversations
- Goal progress tracking and motivation

## Benefits

1. **Continuity**: Conversations feel natural and connected across sessions
2. **Personalization**: Responses become more tailored to individual users over time
3. **Context Awareness**: AI understands user's health journey and goals
4. **Learning**: System improves recommendations based on user feedback and patterns
5. **Efficiency**: Users don't need to repeat information or preferences
6. **Engagement**: More meaningful interactions lead to better health outcomes

## Technical Implementation

### Store Architecture
- **MobX Observables**: Reactive state management for real-time UI updates
- **Persistence Layer**: Automatic save/restore using mobx-persist-store
- **Type Safety**: Full TypeScript support for all store interfaces

### Memory Algorithm
- **Relevance Scoring**: Combines content matching, confidence, recency, and usage frequency
- **Context Matching**: Semantic matching between user input and stored memories
- **Dynamic Weighting**: Adjusts scoring based on memory type and age

### Session Management
- **Automatic Grouping**: Sessions organized by time periods for easy navigation
- **Message Metadata**: Rich metadata for health queries, goals, and context
- **Context Hooks**: Integration points for health data and user preferences

This enhanced system transforms the health assistant from a stateless question-answering tool into an intelligent, context-aware health companion that learns and adapts to each user's unique needs and preferences.
