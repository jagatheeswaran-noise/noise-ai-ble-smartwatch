import { useState, useEffect, useCallback, useRef } from 'react';
import { watchVoiceService, WatchVoiceCommand, AiErrorCode } from '../services/WatchVoiceService';
import { zhSDKService } from '../services/ZHSDKService';

export interface WatchVoiceState {
  isWatchVoiceActive: boolean;
  isProcessingWatchVoice: boolean;
  watchVoiceError: string | null;
  lastWatchCommand: WatchVoiceCommand | null;
}

export interface WatchVoiceIntegrationCallbacks {
  onWatchVoiceResult: (transcription: string) => Promise<void>;
  onWatchVoiceError: (error: string) => void;
}

export const useWatchVoiceIntegration = (callbacks: WatchVoiceIntegrationCallbacks) => {
  const [state, setState] = useState<WatchVoiceState>({
    isWatchVoiceActive: false,
    isProcessingWatchVoice: false,
    watchVoiceError: null,
    lastWatchCommand: null,
  });

  // Use refs to maintain stable callback references
  const callbacksRef = useRef(callbacks);
  callbacksRef.current = callbacks;

  // Handle watch voice commands
  const handleWatchVoiceCommand = useCallback(async (command: WatchVoiceCommand) => {
    console.log('🎤⌚ Processing watch voice command:', command);
    
    setState(prev => ({
      ...prev,
      lastWatchCommand: command,
      isProcessingWatchVoice: true,
      watchVoiceError: null,
    }));

    try {
      // Parse the command to determine if it's a start/stop recording command
      const { rawData } = command;
      
      if (rawData.includes('start') || rawData.includes('activate')) {
        setState(prev => ({ ...prev, isWatchVoiceActive: true }));
        console.log('🎤⌚ Watch voice recording started');
      } else if (rawData.includes('stop') || rawData.includes('end')) {
        setState(prev => ({ ...prev, isWatchVoiceActive: false }));
        console.log('🎤⌚ Watch voice recording stopped');
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Unknown error';
      setState(prev => ({ ...prev, watchVoiceError: errorMessage }));
      callbacksRef.current.onWatchVoiceError(errorMessage);
    } finally {
      setState(prev => ({ ...prev, isProcessingWatchVoice: false }));
    }
  }, []);

  // Handle transcribed voice from watch
  const handleWatchVoiceTranscription = useCallback(async (transcription: string) => {
    console.log('🎤⌚ Watch voice transcription received:', transcription);
    
    setState(prev => ({ ...prev, isProcessingWatchVoice: true }));

    try {
      // Send the transcription to the AI pipeline
      await callbacksRef.current.onWatchVoiceResult(transcription);
      
      // Send confirmation to watch that we received the transcription
      await watchVoiceService.sendTranscribedText(transcription);
      
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Transcription processing error';
      console.error('🎤⌚ Error processing watch transcription:', error);
      
      setState(prev => ({ ...prev, watchVoiceError: errorMessage }));
      callbacksRef.current.onWatchVoiceError(errorMessage);
      
      // Send error to watch
      await watchVoiceService.sendErrorCode(AiErrorCode.ASR_UNDERSTANDING_FAILURE);
    } finally {
      setState(prev => ({ ...prev, isProcessingWatchVoice: false }));
    }
  }, []);

  // Handle watch voice errors
  const handleWatchVoiceError = useCallback(async (error: string) => {
    console.error('🎤⌚ Watch voice error:', error);
    
    setState(prev => ({
      ...prev,
      watchVoiceError: error,
      isWatchVoiceActive: false,
      isProcessingWatchVoice: false,
    }));

    callbacksRef.current.onWatchVoiceError(error);
    
    // Send appropriate error code to watch
    if (error.includes('network')) {
      await watchVoiceService.sendErrorCode(AiErrorCode.NETWORK_ERROR);
    } else if (error.includes('timeout')) {
      await watchVoiceService.sendErrorCode(AiErrorCode.NO_VOICE_TIMEOUT);
    } else {
      await watchVoiceService.sendErrorCode(AiErrorCode.ASR_UNDERSTANDING_FAILURE);
    }
  }, []);

  // Send AI response back to watch
  const sendAiResponseToWatch = useCallback(async (response: string) => {
    try {
      console.log('🎤⌚ Sending AI response to watch:', response);
      await watchVoiceService.handleAiResponse(response);
    } catch (error) {
      console.error('🎤⌚ Error sending AI response to watch:', error);
      await watchVoiceService.sendErrorCode(AiErrorCode.SERVER_NO_RESPONSE);
    }
  }, []);

  // Initialize watch voice integration
  useEffect(() => {
    console.log('🎤⌚ Initializing watch voice integration');

    // Set up watch voice callbacks
    watchVoiceService.setCallbacks({
      onVoiceCommand: handleWatchVoiceCommand,
      onVoiceTranscription: handleWatchVoiceTranscription,
      onVoiceError: handleWatchVoiceError,
    });

    return () => {
      console.log('🎤⌚ Cleaning up watch voice integration');
      // Only clear callbacks, don't cleanup event listeners (they're singleton)
      watchVoiceService.clearCallbacks();
    };
  }, []); // Empty dependency array to prevent re-initialization loops

  // Manual trigger methods for testing
  const triggerWatchVoiceStart = useCallback(async () => {
    try {
      await watchVoiceService.sendAiVoiceCommand(1); // Assuming 1 is start command
      setState(prev => ({ ...prev, isWatchVoiceActive: true }));
    } catch (error) {
      console.error('🎤⌚ Error triggering watch voice start:', error);
    }
  }, []);

  const triggerWatchVoiceStop = useCallback(async () => {
    try {
      await watchVoiceService.sendAiVoiceCommand(2); // Assuming 2 is stop command
      setState(prev => ({ ...prev, isWatchVoiceActive: false }));
    } catch (error) {
      console.error('🎤⌚ Error triggering watch voice stop:', error);
    }
  }, []);

  // Simulate watch voice interaction for testing
  const simulateWatchVoiceInput = useCallback(async (text: string) => {
    try {
      console.log('🎤⌚ Simulating watch voice input:', text);
      await watchVoiceService.simulateWatchVoiceInteraction(text);
    } catch (error) {
      console.error('🎤⌚ Error simulating watch voice input:', error);
    }
  }, []);

  return {
    watchVoiceState: state,
    sendAiResponseToWatch,
    triggerWatchVoiceStart,
    triggerWatchVoiceStop,
    simulateWatchVoiceInput,
    isWatchConnected: zhSDKService.currentConnectionStatus === 'bound',
  };
};
