import { useState, useCallback, useRef } from 'react';
import { ScrollView } from 'react-native';
import { Message } from '../types/chat';

interface UseChatStateProps {
  initialMessages?: Message[];
}

interface UseChatStateReturn {
  messages: Message[];
  addMessage: (message: Omit<Message, 'id' | 'timestamp'>) => string;
  updateMessage: (id: string, updates: Partial<Message>) => void;
  updateStreamingMessage: (id: string, text: string) => void;
  clearChat: () => void;
  scrollToBottom: () => void;
  setScrollViewRef: (ref: ScrollView | null) => void;
  generateMessageId: () => string;
}

export const useChatState = ({ 
  initialMessages = [
    {
      id: '1',
      text: 'Hello! I\'m Noise AI, your assistant. You can speak to me or type your messages.',
      isUser: false,
      timestamp: new Date(),
    }
  ] 
}: UseChatStateProps = {}): UseChatStateReturn => {
  const [messages, setMessages] = useState<Message[]>(initialMessages);
  const scrollViewRef = useRef<ScrollView | null>(null);
  const scrollTimeoutId = useRef<NodeJS.Timeout | null>(null);

  const generateMessageId = useCallback(() => {
    return `msg_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }, []);

  const addMessage = useCallback((message: Omit<Message, 'id' | 'timestamp'>): string => {
    const newMessage: Message = {
      ...message,
      id: generateMessageId(),
      timestamp: new Date(),
    };

    setMessages(prev => [...prev, newMessage]);
    
    // Auto-scroll to bottom after adding message
    setTimeout(() => scrollToBottom(), 100);
    
    return newMessage.id;
  }, [generateMessageId]);

  const updateMessage = useCallback((id: string, updates: Partial<Message>) => {
    setMessages(prev => 
      prev.map(msg => 
        msg.id === id ? { ...msg, ...updates } : msg
      )
    );
  }, []);

  const updateStreamingMessage = useCallback((id: string, text: string) => {
    setMessages(prev => 
      prev.map(msg => 
        msg.id === id ? { ...msg, text } : msg
      )
    );
    
    // Throttled scroll to bottom during streaming
    if (scrollTimeoutId.current) {
      clearTimeout(scrollTimeoutId.current);
    }
    
    scrollTimeoutId.current = setTimeout(() => {
      scrollViewRef.current?.scrollToEnd({ animated: true });
      scrollTimeoutId.current = null;
    }, 100);
  }, []);

  const clearChat = useCallback(() => {
    setMessages(initialMessages);
  }, [initialMessages]);

  const scrollToBottom = useCallback(() => {
    scrollViewRef.current?.scrollToEnd({ animated: true });
  }, []);

  const setScrollViewRef = useCallback((ref: ScrollView | null) => {
    scrollViewRef.current = ref;
  }, []);

  return {
    messages,
    addMessage,
    updateMessage,
    updateStreamingMessage,
    clearChat,
    scrollToBottom,
    setScrollViewRef,
    generateMessageId,
  };
};
