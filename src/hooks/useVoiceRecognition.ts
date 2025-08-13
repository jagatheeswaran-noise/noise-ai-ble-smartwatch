import { useState, useEffect, useCallback } from 'react';
import { PermissionsAndroid, Platform, Alert } from 'react-native';
import Voice, {
  type SpeechRecognizedEvent,
  type SpeechResultsEvent,
  type SpeechErrorEvent,
} from '@react-native-voice/voice';
import { Language, VoiceState, LANGUAGES } from '../types/chat';

interface UseVoiceRecognitionProps {
  onSpeechResult?: (result: string) => void;
  onSpeechErrorCallback?: (error: string) => void;
  onPartialResult?: (result: string) => void;
}

interface UseVoiceRecognitionReturn extends VoiceState {
  selectedLanguage: Language;
  startRecording: () => Promise<void>;
  stopRecording: () => void;
  setLanguage: (language: Language) => void;
  requestPermissions: () => Promise<boolean>;
}

export const useVoiceRecognition = ({
  onSpeechResult,
  onSpeechErrorCallback,
  onPartialResult,
}: UseVoiceRecognitionProps = {}): UseVoiceRecognitionReturn => {
  const [voiceState, setVoiceState] = useState<VoiceState>({
    recognized: '',
    pitch: '',
    error: '',
    end: '',
    started: '',
    results: [],
    partialResults: [],
    isRecording: false,
    isVoiceAvailable: false,
    currentTranscription: '',
  });

  const [selectedLanguage, setSelectedLanguage] = useState<Language>(LANGUAGES[0]);

  // Voice event handlers
  const onSpeechStart = useCallback((_e: any) => {
    console.log('🎤 Speech recognition started');
    setVoiceState(prev => ({
      ...prev,
      started: 'Started',
      isRecording: true,
      error: '',
    }));
  }, []);

  const onSpeechRecognized = useCallback((_e: SpeechRecognizedEvent) => {
    console.log('🎤 Speech recognized');
    setVoiceState(prev => ({
      ...prev,
      recognized: 'Recognized',
    }));
  }, []);

  const onSpeechEnd = useCallback((_e: any) => {
    console.log('🎤 Speech recognition ended');
    setVoiceState(prev => ({
      ...prev,
      end: 'Ended',
      isRecording: false,
    }));
  }, []);

  const onSpeechError = useCallback((e: SpeechErrorEvent) => {
    console.log('🎤 Speech recognition error:', e.error);
    const errorMessage = e.error?.message || 'Speech recognition error';
    setVoiceState(prev => ({
      ...prev,
      error: errorMessage,
      isRecording: false,
    }));
    onSpeechErrorCallback?.(errorMessage);
  }, [onSpeechErrorCallback]);

  const onSpeechResults = useCallback((e: SpeechResultsEvent) => {
    console.log('🎤 Speech results:', e.value);
    const result = e.value?.[0] || '';
    setVoiceState(prev => ({
      ...prev,
      results: e.value || [],
      currentTranscription: result,
    }));
    if (result) {
      onSpeechResult?.(result);
    }
  }, [onSpeechResult]);

  const onSpeechPartialResults = useCallback((e: SpeechResultsEvent) => {
    console.log('🎤 Partial results:', e.value);
    const partialResult = e.value?.[0] || '';
    setVoiceState(prev => ({
      ...prev,
      partialResults: e.value || [],
      currentTranscription: partialResult,
    }));
    if (partialResult) {
      onPartialResult?.(partialResult);
    }
  }, [onPartialResult]);

  const onSpeechVolumeChanged = useCallback((e: any) => {
    setVoiceState(prev => ({
      ...prev,
      pitch: e.value,
    }));
  }, []);

  // Initialize voice recognition
  useEffect(() => {
    const initializeVoice = async () => {
      try {
        const available = await Voice.isAvailable();
        console.log('🎤 Voice recognition available:', available);
        
        setVoiceState(prev => ({
          ...prev,
          isVoiceAvailable: !!available,
        }));
      } catch (error) {
        console.error('🎤 Error initializing voice recognition:', error);
        setVoiceState(prev => ({
          ...prev,
          error: 'Failed to initialize voice recognition',
          isVoiceAvailable: false,
        }));
      }
    };

    initializeVoice();

    // Cleanup
    return () => {
      Voice.destroy().then(Voice.removeAllListeners);
    };
  }, []); // Empty dependency array - only run once on mount

  // Update event listeners when callbacks change
  useEffect(() => {
    Voice.onSpeechStart = onSpeechStart;
    Voice.onSpeechRecognized = onSpeechRecognized;
    Voice.onSpeechEnd = onSpeechEnd;
    Voice.onSpeechError = onSpeechError;
    Voice.onSpeechResults = onSpeechResults;
    Voice.onSpeechPartialResults = onSpeechPartialResults;
    Voice.onSpeechVolumeChanged = onSpeechVolumeChanged;
  }, [onSpeechStart, onSpeechRecognized, onSpeechEnd, onSpeechError, onSpeechResults, onSpeechPartialResults, onSpeechVolumeChanged]);

  const requestPermissions = useCallback(async (): Promise<boolean> => {
    if (Platform.OS === 'android') {
      try {
        const grants = await PermissionsAndroid.requestMultiple([
          PermissionsAndroid.PERMISSIONS.RECORD_AUDIO,
        ]);

        const audioGranted = grants[PermissionsAndroid.PERMISSIONS.RECORD_AUDIO] === PermissionsAndroid.RESULTS.GRANTED;
        
        if (!audioGranted) {
          Alert.alert(
            'Permissions Required',
            'Audio recording permission is required for voice input.',
            [{ text: 'OK' }]
          );
          return false;
        }
        return true;
      } catch (err) {
        console.error('🎤 Error requesting permissions:', err);
        return false;
      }
    }
    return true; // iOS permissions handled automatically
  }, []);

  const startRecording = useCallback(async () => {
    try {
      console.log('🎤 Starting voice recognition...');
      
      if (!voiceState.isVoiceAvailable) {
        Alert.alert('Error', 'Voice recognition is not available on this device.');
        return;
      }

      const hasPermission = await requestPermissions();
      if (!hasPermission) {
        return;
      }

      // Clear previous state
      setVoiceState(prev => ({
        ...prev,
        recognized: '',
        pitch: '',
        error: '',
        started: '',
        results: [],
        partialResults: [],
        currentTranscription: '',
      }));

      await Voice.start(selectedLanguage.code);
    } catch (error) {
      console.error('🎤 Error starting voice recognition:', error);
      setVoiceState(prev => ({
        ...prev,
        error: 'Failed to start voice recognition',
        isRecording: false,
      }));
    }
  }, [voiceState.isVoiceAvailable, selectedLanguage.code, requestPermissions]);

  const stopRecording = useCallback(async () => {
    try {
      console.log('🎤 Stopping voice recognition...');
      await Voice.stop();
    } catch (error) {
      console.error('🎤 Error stopping voice recognition:', error);
    }
  }, []);

  const setLanguage = useCallback((language: Language) => {
    console.log('🎤 Setting language to:', language.name);
    setSelectedLanguage(language);
  }, []);

  return {
    ...voiceState,
    selectedLanguage,
    startRecording,
    stopRecording,
    setLanguage,
    requestPermissions,
  };
};
