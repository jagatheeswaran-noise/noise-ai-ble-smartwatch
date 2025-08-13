import React from 'react';
import { StyleSheet, View } from 'react-native';
import { Message } from '../../types/chat';
import MessageList from './MessageList';

interface ChatContainerProps {
  messages: Message[];
  streamingMessageId: string | null;
  currentStreamingText: string;
  onScrollViewRef: (ref: any) => void;
  children?: React.ReactNode;
}

const ChatContainer: React.FC<ChatContainerProps> = ({
  messages,
  streamingMessageId,
  currentStreamingText,
  onScrollViewRef,
  children,
}) => {
  return (
    <View style={styles.container}>
      <MessageList
        messages={messages}
        streamingMessageId={streamingMessageId}
        currentStreamingText={currentStreamingText}
        onScrollViewRef={onScrollViewRef}
      />
      {children}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0a0a0a',
  },
});

export default ChatContainer;
