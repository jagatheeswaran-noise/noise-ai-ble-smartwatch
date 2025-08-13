import React from 'react';
import { View, TextInput, TouchableOpacity, StyleSheet, Text } from 'react-native';

interface ChatTextInputProps {
  value: string;
  onChangeText: (text: string) => void;
  onSend: () => void;
  placeholder?: string;
  disabled?: boolean;
  multiline?: boolean;
}

const ChatTextInput: React.FC<ChatTextInputProps> = ({
  value,
  onChangeText,
  onSend,
  placeholder = "Type your message...",
  disabled = false,
  multiline = true,
}) => {
  const handleSend = () => {
    if (value.trim() && !disabled) {
      onSend();
    }
  };

  return (
    <View style={styles.container}>
      <TextInput
        style={[
          styles.textInput,
          disabled && styles.disabledInput,
        ]}
        value={value}
        onChangeText={onChangeText}
        placeholder={placeholder}
        placeholderTextColor="#666666"
        multiline={multiline}
        maxLength={1000}
        editable={!disabled}
        keyboardAppearance="dark"
        returnKeyType="send"
        onSubmitEditing={handleSend}
        blurOnSubmit={!multiline}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: 'transparent',
  },
  textInput: {
    flex: 1,
    color: '#ffffff',
    fontSize: 16,
    maxHeight: 100,
    paddingVertical: 0,
    paddingHorizontal: 0,
    backgroundColor: 'transparent',
    textAlignVertical: 'center',
  },
  disabledInput: {
    opacity: 0.6,
  },
});

export default ChatTextInput;
