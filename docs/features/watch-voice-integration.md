# Smartwatch Voice Integration Feature

## Overview

This feature enables voice streaming from Noise smartwatches to the mobile app via Bluetooth Low Energy (BLE), integrating seamlessly with the existing AI pipeline for conversational health assistance.

## Architecture

```
Smartwatch AI Button Press → BLE Voice Command → WatchVoiceService → AI Pipeline → Response Back to Watch
```

## Key Components

### 1. **ZHSDKModule (Android Native)**
- **Location**: `android/app/src/main/java/com/noise_ai/ZHSDKModule.java`
- **Purpose**: Extends the native SDK module to handle AI voice commands
- **Key Methods**:
  - `sendAiVoiceCommand(command)` - Send voice commands to watch
  - `sendAiTranslatedText(text)` - Send transcribed text to watch
  - `sendAiAnswerText(text)` - Send AI response to watch
  - `sendAiErrorCode(errorCode)` - Send error codes to watch

### 2. **WatchVoiceService (TypeScript)**
- **Location**: `src/services/WatchVoiceService.ts`
- **Purpose**: Manages voice streaming and integration between watch and app
- **Key Features**:
  - Event-driven architecture with native event emitters
  - AI error code management (network, timeout, ASR failures)
  - Transcription processing and validation
  - Watch-to-app communication bridge

### 3. **useWatchVoiceIntegration Hook**
- **Location**: `src/hooks/useWatchVoiceIntegration.ts`
- **Purpose**: React hook for managing watch voice state and callbacks
- **Features**:
  - Watch voice state management
  - AI pipeline integration
  - Error handling and recovery
  - Testing utilities for development

### 4. **Enhanced UI Components**
- **Location**: `VoiceTest.tsx`
- **Features**:
  - Watch voice status indicators
  - Processing state visualization
  - Real-time feedback bubbles
  - Development testing controls

## Voice Flow Process

### 1. **Voice Initiation**
```typescript
User presses AI button on watch → Watch sends voice command via BLE → App receives onWatchAiVoiceCommand event
```

### 2. **Voice Processing**
```typescript
WatchVoiceService processes command → Extracts transcription → Triggers onWatchVoiceResult callback
```

### 3. **AI Integration**
```typescript
Transcription sent to existing AI pipeline → AI generates response → Response sent back to watch
```

### 4. **Error Handling**
```typescript
Any errors trigger appropriate error codes → Sent back to watch → User feedback provided
```

## Error Codes

Based on ZH SDK documentation:

```typescript
enum AiErrorCode {
  NORMAL = 0,                    // Normal operation
  NETWORK_ERROR = 1,             // Network connectivity issues
  NO_VOICE_TIMEOUT = 2,          // No voice input detected within timeout
  ASR_UNDERSTANDING_FAILURE = 3,  // Speech recognition failed
  SERVER_NO_RESPONSE = 4,        // AI server not responding
  RATE_LIMIT_EXCEEDED = 5,       // Too many requests
}
```

## Implementation Details

### Watch Voice Commands (Estimated Values)
```typescript
const AI_VOICE_COMMANDS = {
  START_RECORDING: 1,    // Start voice recording
  STOP_RECORDING: 2,     // Stop voice recording
  CANCEL_RECORDING: 3,   // Cancel current recording
  AI_ASSISTANT_ACTIVATE: 4, // Activate AI assistant
};
```

### Event Types
```typescript
// Native events from watch
onWatchAiVoiceCommand      // Raw voice command from watch
onAiVoiceCommandSent       // Confirmation of command sent to watch
onAiTranslatedTextSent     // Confirmation of text sent to watch
onAiAnswerTextSent         // Confirmation of response sent to watch
onAiErrorCodeSent          // Confirmation of error sent to watch
```

## Usage Examples

### Basic Integration
```typescript
const {
  watchVoiceState,
  sendAiResponseToWatch,
  simulateWatchVoiceInput,
  isWatchConnected,
} = useWatchVoiceIntegration({
  onWatchVoiceResult: async (transcription: string) => {
    // Process transcription through AI pipeline
    await sendMessage(transcription);
  },
  onWatchVoiceError: (error: string) => {
    console.error('Watch voice error:', error);
  },
});
```

### Development Testing
```typescript
// Simulate watch voice input for testing
await simulateWatchVoiceInput("What's my heart rate today?");

// Check watch connection status
if (isWatchConnected) {
  // Watch is connected and bound
}
```

## UI Components

### Watch Voice Status Indicators
- **Active Recording**: Green pulsing bubble with watch emoji
- **Processing**: Purple bubble showing processing state
- **Error State**: Red indicators with error messages

### Development Controls
- **Test Button**: Simulates watch voice input (dev builds only)
- **Status Display**: Shows current watch voice state
- **Connection Indicator**: Shows watch connection status

## Integration Points

### 1. **AI Pipeline Integration**
The watch voice transcription is processed through the existing AI pipeline:
```typescript
// Transcription flows through the same path as phone voice input
await sendMessage(transcription); // Existing AI pipeline method
```

### 2. **Response Delivery**
AI responses are automatically sent back to the watch:
```typescript
// Monitors AI completion and sends response to watch
useEffect(() => {
  if (!isAiProcessing && currentStreamingText && isWatchConnected) {
    sendAiResponseToWatch(currentStreamingText);
  }
}, [isAiProcessing, currentStreamingText]);
```

### 3. **Error Handling**
Comprehensive error handling with appropriate feedback:
```typescript
// Network errors, timeouts, and processing failures are handled
// with specific error codes sent back to the watch
```

## Configuration

### Required SDK Version
- **ZH SDK**: 2.1.7 or later
- **Feature Support**: Berry protocol with AI voice integration

### Permissions
- Bluetooth LE permissions (already handled by existing SDK integration)
- Microphone permissions (handled by existing voice recognition)

## Testing

### Development Mode Features
- **Simulation**: `simulateWatchVoiceInput()` for testing without physical watch
- **Debug Logging**: Comprehensive console logging for development
- **Visual Indicators**: Clear UI feedback for all states
- **Test Button**: Quick testing in development builds

### Production Considerations
- All debug features are hidden in production builds
- Error handling provides user-friendly feedback
- Graceful degradation when watch is not connected

## Future Enhancements

1. **Advanced Voice Commands**: Support for voice shortcuts and custom commands
2. **Voice Preprocessing**: Local voice processing on the watch before transmission
3. **Offline Mode**: Basic voice commands that work without AI pipeline
4. **Voice Profiles**: User-specific voice recognition tuning
5. **Multi-language Support**: Extended language support for watch voice input

## Troubleshooting

### Common Issues
1. **Watch Not Responding**: Check Bluetooth connection and binding status
2. **Voice Not Processed**: Verify AI pipeline is functional and model is loaded
3. **Timeouts**: Check network connectivity for AI processing
4. **Permission Issues**: Ensure all Bluetooth and microphone permissions are granted

### Debug Commands
```typescript
// Check watch connection
console.log('Watch connected:', isWatchConnected);

// Check watch voice state
console.log('Watch voice state:', watchVoiceState);

// Test voice simulation
await simulateWatchVoiceInput("Test message");
```

This implementation provides a robust foundation for smartwatch voice integration while maintaining compatibility with the existing AI pipeline and user experience.
