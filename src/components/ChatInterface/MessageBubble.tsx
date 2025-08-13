import React from 'react';
import { View, Text, StyleSheet, Animated } from 'react-native';
import { Message } from '../../types/chat';

interface MessageBubbleProps {
  message: Message;
  isStreaming?: boolean;
  streamingText?: string;
}

const MessageBubble: React.FC<MessageBubbleProps> = ({ 
  message, 
  isStreaming = false, 
  streamingText = '' 
}) => {
  const displayText = isStreaming ? streamingText : message.text;
  
  return (
    <View
      style={[
        styles.messageContainer,
        message.isUser ? styles.userMessageContainer : styles.aiMessageContainer,
      ]}
    >
      <View
        style={[
          styles.messageBubble,
          message.isUser ? styles.userMessage : styles.aiMessage,
        ]}
      >
        <Text
          style={[
            styles.messageText,
            message.isUser ? styles.userMessageText : styles.aiMessageText,
          ]}
        >
          {displayText}
        </Text>
        {isStreaming && <StreamingIndicator />}
      </View>
      <Text style={styles.timestamp}>
        {message.timestamp.toLocaleTimeString([], { 
          hour: '2-digit', 
          minute: '2-digit' 
        })}
      </Text>
    </View>
  );
};

const StreamingIndicator: React.FC = () => {
  const [dot1] = React.useState(new Animated.Value(0));
  const [dot2] = React.useState(new Animated.Value(0));
  const [dot3] = React.useState(new Animated.Value(0));

  React.useEffect(() => {
    const animateDots = () => {
      const animationSequence = Animated.loop(
        Animated.sequence([
          Animated.timing(dot1, { toValue: 1, duration: 500, useNativeDriver: true }),
          Animated.timing(dot2, { toValue: 1, duration: 500, useNativeDriver: true }),
          Animated.timing(dot3, { toValue: 1, duration: 500, useNativeDriver: true }),
          Animated.parallel([
            Animated.timing(dot1, { toValue: 0, duration: 500, useNativeDriver: true }),
            Animated.timing(dot2, { toValue: 0, duration: 500, useNativeDriver: true }),
            Animated.timing(dot3, { toValue: 0, duration: 500, useNativeDriver: true }),
          ]),
        ])
      );
      animationSequence.start();
      return animationSequence;
    };

    const animation = animateDots();
    return () => animation.stop();
  }, [dot1, dot2, dot3]);

  return (
    <View style={styles.streamingContainer}>
      <Animated.View style={[styles.dot, { opacity: dot1 }]} />
      <Animated.View style={[styles.dot, { opacity: dot2 }]} />
      <Animated.View style={[styles.dot, { opacity: dot3 }]} />
    </View>
  );
};

const styles = StyleSheet.create({
  messageContainer: {
    marginBottom: 20,
  },
  userMessageContainer: {
    alignItems: 'flex-end',
  },
  aiMessageContainer: {
    alignItems: 'flex-start',
  },
  messageBubble: {
    maxWidth: '85%',
    paddingHorizontal: 18,
    paddingVertical: 12,
    borderRadius: 20,
  },
  userMessage: {
    backgroundColor: '#00ff88',
    borderBottomRightRadius: 6,
  },
  aiMessage: {
    backgroundColor: '#2a2a2a',
    borderBottomLeftRadius: 6,
  },
  messageText: {
    fontSize: 16,
    lineHeight: 24,
    fontWeight: '400',
  },
  userMessageText: {
    color: '#000000',
  },
  aiMessageText: {
    color: '#ffffff',
  },
  timestamp: {
    fontSize: 12,
    color: '#666666',
    marginHorizontal: 8,
    marginTop: 4,
  },
  streamingContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 8,
  },
  dot: {
    width: 4,
    height: 4,
    borderRadius: 2,
    backgroundColor: '#00ff88',
    marginHorizontal: 2,
  },
});

export default MessageBubble;
