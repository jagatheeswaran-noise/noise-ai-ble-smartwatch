import { useState, useCallback } from 'react';
import { AIState } from '../types/chat';
import { llamaService } from '../../LlamaService';
import queryRouter from '../../QueryRouter';

interface UseAIStreamingProps {
  onAddMessage: (message: { text: string; isUser: boolean }) => string;
  onUpdateStreamingMessage: (id: string, text: string) => void;
}

interface UseAIStreamingReturn extends AIState {
  sendMessage: (text: string) => Promise<void>;
  stopGeneration: () => void;
  retryLastMessage: () => void;
  resetAIState: () => void;
}

export const useAIStreaming = ({
  onAddMessage,
  onUpdateStreamingMessage,
}: UseAIStreamingProps): UseAIStreamingReturn => {
  const [aiState, setAIState] = useState<AIState>({
    isAiProcessing: false,
    canStopAiGeneration: false,
    streamingMessageId: null,
    currentStreamingText: '',
    showRetryButton: false,
    lastErrorCanRetry: false,
  });

  const [lastUserMessage, setLastUserMessage] = useState<string>('');

  const sendMessage = useCallback(async (text: string) => {
    if (!text.trim()) return;

    console.log('🤖 Sending message:', text);
    setLastUserMessage(text);

    // Add user message
    onAddMessage({ text: text.trim(), isUser: true });

    // Set AI processing state
    setAIState(prev => ({
      ...prev,
      isAiProcessing: true,
      canStopAiGeneration: false,
      showRetryButton: false,
      lastErrorCanRetry: false,
      currentStreamingText: '',
    }));

    try {
      // Create AI response message
      const aiMessageId = onAddMessage({ text: '', isUser: false });
      
      setAIState(prev => ({
        ...prev,
        streamingMessageId: aiMessageId,
        canStopAiGeneration: true,
      }));

      let fullResponse = '';

      // Use query router for processing
      const response = await queryRouter.processQuery(
        text,
        // onToken callback for streaming
        (token: string) => {
          fullResponse += token;
          setAIState(prev => ({
            ...prev,
            currentStreamingText: fullResponse,
          }));
          onUpdateStreamingMessage(aiMessageId, fullResponse);
        },
        // onComplete callback
        () => {
          console.log('🤖 AI response completed');
          setAIState(prev => ({
            ...prev,
            isAiProcessing: false,
            canStopAiGeneration: false,
            streamingMessageId: null,
            currentStreamingText: '',
          }));
        }
      );

      // If no streaming occurred, set the final response
      if (!fullResponse && response) {
        onUpdateStreamingMessage(aiMessageId, response);
      }

      setAIState(prev => ({
        ...prev,
        isAiProcessing: false,
        canStopAiGeneration: false,
        streamingMessageId: null,
        currentStreamingText: '',
      }));

    } catch (error) {
      console.error('🤖 Error generating AI response:', error);
      
      const errorMessage = error instanceof Error ? error.message : 'An error occurred while generating response';
      const canRetry = !errorMessage.includes('network') && !errorMessage.includes('connection');
      
      // Add error message
      onAddMessage({ 
        text: `I apologize, but I encountered an error: ${errorMessage}. ${canRetry ? 'Please try again.' : ''}`, 
        isUser: false 
      });

      setAIState(prev => ({
        ...prev,
        isAiProcessing: false,
        canStopAiGeneration: false,
        streamingMessageId: null,
        currentStreamingText: '',
        showRetryButton: canRetry,
        lastErrorCanRetry: canRetry,
      }));
    }
  }, [onAddMessage, onUpdateStreamingMessage]);

  const stopGeneration = useCallback(() => {
    console.log('🤖 Stopping AI generation');
    
    // Stop the LlamaService generation
    if (llamaService && typeof llamaService.stopGeneration === 'function') {
      llamaService.stopGeneration();
    }

    setAIState(prev => ({
      ...prev,
      isAiProcessing: false,
      canStopAiGeneration: false,
      streamingMessageId: null,
      currentStreamingText: '',
    }));
  }, []);

  const retryLastMessage = useCallback(async () => {
    if (lastUserMessage && aiState.lastErrorCanRetry) {
      console.log('🤖 Retrying last message:', lastUserMessage);
      await sendMessage(lastUserMessage);
    }
  }, [lastUserMessage, aiState.lastErrorCanRetry, sendMessage]);

  const resetAIState = useCallback(() => {
    setAIState({
      isAiProcessing: false,
      canStopAiGeneration: false,
      streamingMessageId: null,
      currentStreamingText: '',
      showRetryButton: false,
      lastErrorCanRetry: false,
    });
  }, []);

  return {
    ...aiState,
    sendMessage,
    stopGeneration,
    retryLastMessage,
    resetAIState,
  };
};
