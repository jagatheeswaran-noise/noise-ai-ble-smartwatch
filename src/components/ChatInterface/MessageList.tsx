import React from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { Message } from '../../types/chat';
import MessageBubble from './MessageBubble';

interface MessageListProps {
  messages: Message[];
  streamingMessageId: string | null;
  currentStreamingText: string;
  onScrollViewRef: (ref: ScrollView | null) => void;
}

const MessageList: React.FC<MessageListProps> = ({
  messages,
  streamingMessageId,
  currentStreamingText,
  onScrollViewRef,
}) => {
  // Check if we only have the intro message
  const isIntroOnly = messages.length === 1 && !messages[0].isUser;
  
  return (
    <ScrollView
      ref={onScrollViewRef}
      style={styles.container}
      contentContainerStyle={[
        styles.contentContainer,
        isIntroOnly && styles.introOnlyContainer
      ]}
      keyboardShouldPersistTaps="handled"
      showsVerticalScrollIndicator={false}
    >
      {isIntroOnly && <View style={styles.spacer} />}
      {messages.map((message) => (
        <MessageBubble
          key={message.id}
          message={message}
          isStreaming={message.id === streamingMessageId}
          streamingText={message.id === streamingMessageId ? currentStreamingText : undefined}
        />
      ))}
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0a0a0a',
  },
  contentContainer: {
    padding: 24,
    paddingBottom: 20,
    flexGrow: 1,
    justifyContent: 'flex-end',
  },
  introOnlyContainer: {
    justifyContent: 'flex-end',
    paddingBottom: 0, // No spacing - positioned directly above AI Model Status
  },
  spacer: {
    height: 50, // Fixed height instead of flex
  },
});

export default MessageList;
