import React, { useEffect, useRef } from 'react';
import { TouchableOpacity, Animated, StyleSheet, View } from 'react-native';

interface VoiceButtonProps {
  isRecording: boolean;
  onPress: () => void;
  disabled?: boolean;
  size?: number;
}

const VoiceButton: React.FC<VoiceButtonProps> = ({
  isRecording,
  onPress,
  disabled = false,
  size = 50,
}) => {
  const pulseAnim = useRef(new Animated.Value(1)).current;
  const ringAnim = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    if (isRecording) {
      // Start pulsing animation
      const pulseAnimation = Animated.loop(
        Animated.sequence([
          Animated.timing(pulseAnim, {
            toValue: 1.1,
            duration: 800,
            useNativeDriver: true,
          }),
          Animated.timing(pulseAnim, {
            toValue: 1,
            duration: 800,
            useNativeDriver: true,
          }),
        ])
      );

      // Start ring animation
      const ringAnimation = Animated.loop(
        Animated.sequence([
          Animated.timing(ringAnim, {
            toValue: 1,
            duration: 1000,
            useNativeDriver: true,
          }),
          Animated.timing(ringAnim, {
            toValue: 0,
            duration: 1000,
            useNativeDriver: true,
          }),
        ])
      );

      pulseAnimation.start();
      ringAnimation.start();

      return () => {
        pulseAnimation.stop();
        ringAnimation.stop();
      };
    } else {
      pulseAnim.setValue(1);
      ringAnim.setValue(0);
    }
  }, [isRecording, pulseAnim, ringAnim]);

  const renderMicIcon = (iconSize: number = size * 0.5, color: string = '#ffffff') => {
    return (
      <View style={{ width: iconSize, height: iconSize, alignItems: 'center', justifyContent: 'center' }}>
        <View style={{
          width: iconSize * 0.4,
          height: iconSize * 0.6,
          backgroundColor: 'transparent',
          borderWidth: 2,
          borderColor: color,
          borderRadius: iconSize * 0.2,
          marginBottom: iconSize * 0.1,
        }} />
        <View style={{
          width: iconSize * 0.6,
          height: 2,
          backgroundColor: color,
          position: 'absolute',
          bottom: iconSize * 0.15,
        }} />
        <View style={{
          width: 2,
          height: iconSize * 0.2,
          backgroundColor: color,
          position: 'absolute',
          bottom: 0,
        }} />
      </View>
    );
  };

  const renderStopIcon = () => (
    <View style={{
      width: size * 0.3,
      height: size * 0.3,
      backgroundColor: '#ffffff',
      borderRadius: 2,
    }} />
  );

  return (
    <Animated.View style={{ transform: [{ scale: pulseAnim }] }}>
      {isRecording && (
        <Animated.View
          style={[
            styles.recordingRing,
            {
              width: size + 10,
              height: size + 10,
              borderRadius: (size + 10) / 2,
              opacity: ringAnim,
            },
          ]}
        />
      )}
      <TouchableOpacity
        style={[
          styles.micButton,
          {
            width: size,
            height: size,
            borderRadius: size / 2,
          },
          isRecording && styles.micButtonRecording,
          disabled && styles.micButtonDisabled,
        ]}
        onPress={onPress}
        disabled={disabled}
        activeOpacity={0.8}
      >
        {isRecording ? renderStopIcon() : renderMicIcon()}
      </TouchableOpacity>
    </Animated.View>
  );
};

const styles = StyleSheet.create({
  micButton: {
    backgroundColor: '#00ff88',
    justifyContent: 'center',
    alignItems: 'center',
    position: 'relative',
    elevation: 4,
    shadowColor: '#00ff88',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.3,
    shadowRadius: 4,
  },
  micButtonRecording: {
    backgroundColor: '#ff4444',
  },
  micButtonDisabled: {
    backgroundColor: '#555555',
    opacity: 0.5,
  },
  recordingRing: {
    position: 'absolute',
    borderWidth: 2,
    borderColor: '#ff4444',
    top: -5,
    left: -5,
  },
  iconText: {
    color: '#ffffff',
  },
  stopIcon: {
    backgroundColor: '#ffffff',
    borderRadius: 2,
  },
});

export default VoiceButton;