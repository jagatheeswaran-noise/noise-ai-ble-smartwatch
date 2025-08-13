import AsyncStorage from '@react-native-async-storage/async-storage';
import { Message } from '../../types/chat';

class ChatService {
  private static readonly CHAT_HISTORY_KEY = 'chat_history';
  private static readonly MAX_MESSAGES = 100; // Limit stored messages

  async saveMessageHistory(messages: Message[]): Promise<void> {
    try {
      // Keep only the last MAX_MESSAGES
      const messagesToSave = messages.slice(-ChatService.MAX_MESSAGES);
      await AsyncStorage.setItem(
        ChatService.CHAT_HISTORY_KEY, 
        JSON.stringify(messagesToSave)
      );
      console.log('💾 Chat history saved:', messagesToSave.length, 'messages');
    } catch (error) {
      console.error('💾 Error saving chat history:', error);
    }
  }

  async loadMessageHistory(): Promise<Message[]> {
    try {
      const historyJson = await AsyncStorage.getItem(ChatService.CHAT_HISTORY_KEY);
      if (historyJson) {
        const messages = JSON.parse(historyJson);
        // Convert timestamp strings back to Date objects
        const parsedMessages = messages.map((msg: any) => ({
          ...msg,
          timestamp: new Date(msg.timestamp),
        }));
        console.log('💾 Chat history loaded:', parsedMessages.length, 'messages');
        return parsedMessages;
      }
    } catch (error) {
      console.error('💾 Error loading chat history:', error);
    }
    return [];
  }

  async clearChatHistory(): Promise<void> {
    try {
      await AsyncStorage.removeItem(ChatService.CHAT_HISTORY_KEY);
      console.log('💾 Chat history cleared');
    } catch (error) {
      console.error('💾 Error clearing chat history:', error);
    }
  }

  formatMessage(text: string, isUser: boolean): Omit<Message, 'id' | 'timestamp'> {
    return {
      text: text.trim(),
      isUser,
    };
  }

  async exportChatHistory(): Promise<string> {
    try {
      const messages = await this.loadMessageHistory();
      const exportData = {
        exportDate: new Date().toISOString(),
        messageCount: messages.length,
        messages: messages,
      };
      return JSON.stringify(exportData, null, 2);
    } catch (error) {
      console.error('💾 Error exporting chat history:', error);
      throw error;
    }
  }

  validateMessage(text: string): { isValid: boolean; error?: string } {
    if (!text || typeof text !== 'string') {
      return { isValid: false, error: 'Message must be a non-empty string' };
    }

    const trimmedText = text.trim();
    if (trimmedText.length === 0) {
      return { isValid: false, error: 'Message cannot be empty' };
    }

    if (trimmedText.length > 10000) {
      return { isValid: false, error: 'Message is too long (max 10,000 characters)' };
    }

    return { isValid: true };
  }

  async getMessageStats(): Promise<{
    totalMessages: number;
    userMessages: number;
    aiMessages: number;
    averageMessageLength: number;
    oldestMessage?: Date;
    newestMessage?: Date;
  }> {
    try {
      const messages = await this.loadMessageHistory();
      
      if (messages.length === 0) {
        return {
          totalMessages: 0,
          userMessages: 0,
          aiMessages: 0,
          averageMessageLength: 0,
        };
      }

      const userMessages = messages.filter(msg => msg.isUser);
      const aiMessages = messages.filter(msg => !msg.isUser);
      const totalLength = messages.reduce((sum, msg) => sum + msg.text.length, 0);
      const averageLength = Math.round(totalLength / messages.length);

      const timestamps = messages.map(msg => msg.timestamp);
      const oldestMessage = new Date(Math.min(...timestamps.map(d => d.getTime())));
      const newestMessage = new Date(Math.max(...timestamps.map(d => d.getTime())));

      return {
        totalMessages: messages.length,
        userMessages: userMessages.length,
        aiMessages: aiMessages.length,
        averageMessageLength: averageLength,
        oldestMessage,
        newestMessage,
      };
    } catch (error) {
      console.error('💾 Error getting message stats:', error);
      throw error;
    }
  }
}

export const chatService = new ChatService();
