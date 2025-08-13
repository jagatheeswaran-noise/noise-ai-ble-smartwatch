import React, { Component } from 'react';
import {
  StyleSheet,
  Text,
  View,
  TouchableHighlight,
  Alert,
  PermissionsAndroid,
  Platform,
  ScrollView,
  TouchableOpacity,
  StatusBar,
  Animated,
  TextInput,
  KeyboardAvoidingView,
  Keyboard,
} from 'react-native';

import Voice, {
  type SpeechRecognizedEvent,
  type SpeechResultsEvent,
  type SpeechErrorEvent,
} from '@react-native-voice/voice';

import { sessionStore, memoryStore } from './src/stores';

import { llamaService } from './LlamaService';
import ModelManager from './ModelManager';
import HealthDashboard from './HealthDashboard';
import SmartwatchManager from './src/components/smartwatch/SmartwatchManager';
import { intentClassifier } from './IntentClassifier';
import queryRouter from './QueryRouter';

type Language = {
  code: string;
  name: string;
};

const LANGUAGES: Language[] = [
  { code: 'en-IN', name: 'English' },
  { code: 'hi-IN', name: 'हिंदी' },
];

type Message = {
  id: string;
  text: string;
  isUser: boolean;
  timestamp: Date;
};

type Props = {};
type State = {
  recognized: string;
  pitch: string;
  error: string;
  end: string;
  started: string;
  results: string[];
  partialResults: string[];
  isRecording: boolean;
  isVoiceAvailable: boolean;
  selectedLanguage: Language;
  messages: Message[];
  currentTranscription: string;
  isAiProcessing: boolean;
  canStopAiGeneration: boolean;
  isModelLoaded: boolean;
  modelStatus: string;
  showModelManager: boolean;
  showHealthDashboard: boolean;
  showSmartwatchManager: boolean;
  streamingMessageId: string | null;
  currentStreamingText: string;
  showRetryButton: boolean;
  lastErrorCanRetry: boolean;
  textInput: string;
};

class VoiceTest extends Component<Props, State> {
  private scrollViewRef: ScrollView | null = null;
  private pulseAnim = new Animated.Value(1);
  private scrollTimeoutId: NodeJS.Timeout | null = null;
  private keyboardDidShowListener: any;
  private keyboardDidHideListener: any;

  state = {
    recognized: '',
    pitch: '',
    error: '',
    end: '',
    started: '',
    results: [],
    partialResults: [],
    isRecording: false,
    isVoiceAvailable: false,
    selectedLanguage: LANGUAGES[0],
    messages: [
      {
        id: '1',
        text: 'Hello! I\'m Noise AI, your assistant. You can speak to me or type your messages.',
        isUser: false,
        timestamp: new Date(),
      }
    ],
    currentTranscription: '',
    isAiProcessing: false,
    canStopAiGeneration: false,
    isModelLoaded: false,
    modelStatus: 'Initializing AI model...',
    showModelManager: false,
    showHealthDashboard: false,
    showSmartwatchManager: false,
    streamingMessageId: null,
    currentStreamingText: '',
    showRetryButton: false,
    lastErrorCanRetry: false,
    textInput: '',
  };

  // Animation values for thinking dots
  dotAnimation1 = new Animated.Value(0);
  dotAnimation2 = new Animated.Value(0);
  dotAnimation3 = new Animated.Value(0);

  constructor(props: Props) {
    super(props);
  }

  // Microphone Icon Component
  renderMicIcon = (size: number = 24, color: string = '#ffffff') => {
    return (
      <View style={{ width: size, height: size, alignItems: 'center', justifyContent: 'center' }}>
        <View style={{
          width: size * 0.4,
          height: size * 0.6,
          backgroundColor: 'transparent',
          borderWidth: 2,
          borderColor: color,
          borderRadius: size * 0.2,
          marginBottom: size * 0.1,
        }} />
        <View style={{
          width: size * 0.6,
          height: 2,
          backgroundColor: color,
          position: 'absolute',
          bottom: size * 0.15,
        }} />
        <View style={{
          width: 2,
          height: size * 0.2,
          backgroundColor: color,
          position: 'absolute',
          bottom: 0,
        }} />
      </View>
    );
  };

  // Throttled scroll to end to improve performance during streaming
  throttledScrollToEnd = () => {
    if (this.scrollTimeoutId) {
      clearTimeout(this.scrollTimeoutId);
    }
    
    this.scrollTimeoutId = setTimeout(() => {
      this.scrollViewRef?.scrollToEnd({ animated: true });
      this.scrollTimeoutId = null;
    }, 100); // Throttle to 100ms
  };

  // Stop Icon Component
  renderStopIcon = (size: number = 24, color: string = '#ffffff') => {
    return (
      <View style={{ width: size, height: size, alignItems: 'center', justifyContent: 'center' }}>
        <View style={{
          width: size * 0.6,
          height: size * 0.6,
          backgroundColor: color,
          borderRadius: 2,
        }} />
      </View>
    );
  };

  startPulseAnimation = () => {
    Animated.loop(
      Animated.sequence([
        Animated.timing(this.pulseAnim, {
          toValue: 1.2,
          duration: 1000,
          useNativeDriver: true,
        }),
        Animated.timing(this.pulseAnim, {
          toValue: 1,
          duration: 1000,
          useNativeDriver: true,
        }),
      ])
    ).start();
  };

  stopPulseAnimation = () => {
    this.pulseAnim.stopAnimation();
    this.pulseAnim.setValue(1);
  };

  // Keyboard event handlers - simplified
  keyboardDidShow = (event: any) => {
    // Just track that keyboard is showing, no mode switching
  };

  keyboardDidHide = () => {
    // Just track that keyboard is hiding, no mode switching
  };

  // Text input handlers
  handleTextInputChange = (text: string) => {
    this.setState({ textInput: text });
  };

  handleSendTextMessage = async () => {
    const { textInput } = this.state;
    if (!textInput.trim()) return;

    // Clear input
    this.setState({ textInput: '' });

    // Process the text message (reuse the same logic as voice)
    await this.processWithAI(textInput.trim());
  };

  // Simple focus handler for text input
  handleTextInputFocus = () => {
    // Just let the user type, no mode switching
  };

  async componentDidMount() {
    try {
      // Clear any system prompts from memory on app start
      try {
        await memoryStore.clearSystemPrompts();
        console.log('✅ Memory cleaned on app start');
      } catch (memError) {
        console.log('Memory clear error (ok if first run):', memError);
      }

      // Initialize health data if needed
      try {
        const { healthDataManager } = require('./HealthDataManager');
        const healthSummary = await healthDataManager.getHealthSummary();
        if (!healthSummary || healthSummary.includes("don't have recent health data")) {
          console.log('📊 Initializing health data on app start...');
          await healthDataManager.generateSampleData();
          console.log('✅ Health data initialized');
        } else {
          console.log('✅ Health data already exists');
        }
      } catch (healthError) {
        console.log('Health data initialization error:', healthError);
      }

      // Initialize Voice listeners
      Voice.onSpeechStart = this.onSpeechStart;
      Voice.onSpeechRecognized = this.onSpeechRecognized;
      Voice.onSpeechEnd = this.onSpeechEnd;
      Voice.onSpeechError = this.onSpeechError;
      Voice.onSpeechResults = this.onSpeechResults;
      Voice.onSpeechPartialResults = this.onSpeechPartialResults;
      Voice.onSpeechVolumeChanged = this.onSpeechVolumeChanged;

      // Initialize keyboard listeners
      this.keyboardDidShowListener = Keyboard.addListener('keyboardDidShow', this.keyboardDidShow);
      this.keyboardDidHideListener = Keyboard.addListener('keyboardDidHide', this.keyboardDidHide);

      // Start thinking dots animation
      this.startThinkingAnimation();

      // Check if Voice is available
      const isAvailable = await Voice.isAvailable();
      console.log('Voice availability:', isAvailable);
      
      this.setState({ isVoiceAvailable: !!isAvailable });
      
      if (isAvailable) {
        await this.requestMicrophonePermission();
      } else {
        Alert.alert('Voice Recognition', 'Voice recognition is not available on this device');
      }

      // Initialize AI model
      this.initializeAI();
    } catch (error) {
      console.error('Error initializing Voice:', error);
      this.setState({ error: 'Failed to initialize voice recognition' });
    }
  }

  // Thinking dots animation
  startThinkingAnimation = () => {
    const createAnimation = (animValue: Animated.Value, delay: number) => {
      return Animated.loop(
        Animated.sequence([
          Animated.timing(animValue, {
            toValue: 1,
            duration: 500,
            delay,
            useNativeDriver: true,
          }),
          Animated.timing(animValue, {
            toValue: 0.3,
            duration: 500,
            useNativeDriver: true,
          }),
        ])
      );
    };

    Animated.parallel([
      createAnimation(this.dotAnimation1, 0),
      createAnimation(this.dotAnimation2, 150),
      createAnimation(this.dotAnimation3, 300),
    ]).start();
  };

  initializeAI = async () => {
    try {
      this.setState({ modelStatus: 'Loading AI model...' });
      
      const isInitialized = await llamaService.initialize();
      
      if (isInitialized) {
        this.setState({ 
          isModelLoaded: true, 
          modelStatus: 'AI ready!' 
        });
        console.log('AI model loaded successfully');
      } else {
        this.setState({ 
          isModelLoaded: false, 
          modelStatus: 'AI model not available. Tap to download.' 
        });
        console.log('AI model not loaded');
      }
    } catch (error) {
      console.error('Error initializing AI:', error);
      this.setState({ 
        isModelLoaded: false, 
        modelStatus: 'AI initialization failed' 
      });
    }
  };

  handleModelStatusChange = (isLoaded: boolean) => {
    this.setState({ 
      isModelLoaded: isLoaded,
      modelStatus: isLoaded ? 'AI ready!' : 'AI model not loaded'
    });
  };

  openModelManager = () => {
    this.setState({ showModelManager: true });
  };

  closeModelManager = () => {
    this.setState({ showModelManager: false });
    // Refresh model status after closing manager
    this.initializeAI();
  };

  openSmartwatchManager = () => {
    this.setState({ showSmartwatchManager: true });
  };

  closeSmartwatchManager = () => {
    this.setState({ showSmartwatchManager: false });
  };

  componentWillUnmount() {
    Voice.destroy().then(Voice.removeAllListeners);
    
    // Cleanup keyboard listeners
    if (this.keyboardDidShowListener) {
      this.keyboardDidShowListener.remove();
    }
    if (this.keyboardDidHideListener) {
      this.keyboardDidHideListener.remove();
    }
    
    // Cleanup scroll timeout
    if (this.scrollTimeoutId) {
      clearTimeout(this.scrollTimeoutId);
    }
    
    // Cleanup AI resources
    llamaService.cleanup();
  }

  requestMicrophonePermission = async () => {
    if (Platform.OS === 'android') {
      try {
        const granted = await PermissionsAndroid.request(
          PermissionsAndroid.PERMISSIONS.RECORD_AUDIO,
          {
            title: 'Microphone Permission',
            message: 'Noise AI needs access to your microphone to recognize speech.',
            buttonNeutral: 'Ask Me Later',
            buttonNegative: 'Cancel',
            buttonPositive: 'OK',
          }
        );
        if (granted === PermissionsAndroid.RESULTS.GRANTED) {
          console.log('Microphone permission granted');
        } else {
          console.log('Microphone permission denied');
          Alert.alert(
            'Permission Required',
            'Microphone permission is required for voice recognition to work.'
          );
        }
      } catch (err) {
        console.warn(err);
      }
    }
  };

  addMessage = (text: string, isUser: boolean) => {
    const newMessage: Message = {
      id: Date.now().toString(),
      text,
      isUser,
      timestamp: new Date(),
    };
    
    this.setState(prevState => ({
      messages: [...prevState.messages, newMessage]
    }), () => {
      // Auto-scroll to bottom
      setTimeout(() => {
        this.scrollViewRef?.scrollToEnd({ animated: true });
      }, 100);
    });
  };

  onSpeechStart = (e: any) => {
    console.log('onSpeechStart: ', e);
    this.setState({
      started: '√',
      isRecording: true,
      error: '',
      currentTranscription: '',
      showRetryButton: false,
      lastErrorCanRetry: false,
    });
    this.startPulseAnimation();
  };

  onSpeechRecognized = (e: SpeechRecognizedEvent) => {
    console.log('onSpeechRecognized: ', e);
    this.setState({
      recognized: '√',
    });
  };

  onSpeechEnd = (e: any) => {
    console.log('onSpeechEnd: ', e);
    this.setState({
      end: '√',
      isRecording: false,
    });
    this.stopPulseAnimation();
  };

  // Map speech error codes to user-friendly messages
  getSpeechErrorMessage = (errorCode: string): { message: string; canRetry: boolean; severity: 'info' | 'warning' | 'error' } => {
    const errorMap: Record<string, { message: string; canRetry: boolean; severity: 'info' | 'warning' | 'error' }> = {
      '1': { message: 'Network connection error. Please check your internet connection.', canRetry: true, severity: 'error' },
      '2': { message: 'Network timeout. Please try again.', canRetry: true, severity: 'warning' },
      '3': { message: 'Audio recording error. Please check microphone permissions.', canRetry: true, severity: 'error' },
      '4': { message: 'Server error. Please try again later.', canRetry: true, severity: 'error' },
      '5': { message: 'Client error. Please restart the app.', canRetry: false, severity: 'error' },
      '6': { message: 'Speech timeout. Please speak more clearly.', canRetry: true, severity: 'info' },
      '7': { message: 'Could not understand speech. Please try speaking again.', canRetry: true, severity: 'info' },
      '8': { message: 'Speech recognition not available.', canRetry: false, severity: 'error' },
      '9': { message: 'Insufficient permissions. Please grant microphone access.', canRetry: false, severity: 'error' },
    };

    return errorMap[errorCode] || { 
      message: `Unknown error (${errorCode}). Please try again.`, 
      canRetry: true, 
      severity: 'warning' 
    };
  };

  onSpeechError = (e: SpeechErrorEvent) => {
    console.log('onSpeechError: ', e);
    
    const errorCode = e.error?.code?.toString() || 'unknown';
    const errorInfo = this.getSpeechErrorMessage(errorCode);
    
    // Handle specific error cases elegantly
    if (errorCode === '7') {
      // "No match" - this is common and not really an error
      this.setState({
        error: '',
        isRecording: false,
        currentTranscription: 'Could not understand speech. Please try again.',
        showRetryButton: true,
        lastErrorCanRetry: true,
      });
      
      // Auto-clear the message after 5 seconds, but keep retry button
      setTimeout(() => {
        this.setState({
          currentTranscription: '',
        });
      }, 5000);
      
    } else if (errorCode === '6') {
      // Speech timeout - also common
      this.setState({
        error: '',
        isRecording: false,
        currentTranscription: 'Speech timeout. Please try speaking again.',
        showRetryButton: true,
        lastErrorCanRetry: true,
      });
      
      // Auto-clear the message after 5 seconds, but keep retry button
      setTimeout(() => {
        this.setState({
          currentTranscription: '',
        });
      }, 5000);
      
    } else if (errorInfo.severity === 'error') {
      // Serious errors - show to user and potentially disable functionality
      this.setState({
        error: errorInfo.message,
        isRecording: false,
        currentTranscription: '',
        showRetryButton: errorInfo.canRetry,
        lastErrorCanRetry: errorInfo.canRetry,
      });
      
      if (errorCode === '9') {
        // Permission error - could prompt user to grant permissions
        Alert.alert(
          'Microphone Permission Required',
          'Please grant microphone permission in your device settings to use voice recognition.',
          [{ text: 'OK' }]
        );
      }
      
    } else {
      // Warning or info level errors - show briefly
      this.setState({
        error: '',
        isRecording: false,
        currentTranscription: errorInfo.message,
        showRetryButton: errorInfo.canRetry,
        lastErrorCanRetry: errorInfo.canRetry,
      });
      
      // Auto-clear warning messages
      setTimeout(() => {
        this.setState({
          currentTranscription: '',
        });
      }, 5000);
    }
    
    this.stopPulseAnimation();
  };

  onSpeechResults = async (e: SpeechResultsEvent) => {
    console.log('onSpeechResults: ', e);
    const results = e.value && e.value?.length > 0 ? e.value : [];
    this.setState({
      results,
      currentTranscription: '',
      showRetryButton: false,
      lastErrorCanRetry: false,
    });
    
    if (results.length > 0) {
      const transcription = results[0];
      this.addMessage(transcription, true);
      
      // Process with AI (using fallback if model not loaded)
      this.processWithAI(transcription);
    }
  };

  processWithAI = async (userInput: string) => {
    try {
      this.setState({ 
        isAiProcessing: true,
        canStopAiGeneration: true 
      });
      
      // Initialize session if needed
      if (!sessionStore.activeSessionId) {
        await sessionStore.createNewSession('Health Chat');
      }

      // Add user message to session
      const userMessageId = await sessionStore.addMessageToCurrentSession({
        author: 'user',
        text: userInput,
        createdAt: Date.now(),
        type: 'text',
      });

      // 🚀 ENHANCED: Advanced Intent Classification
      console.log('🧠 Analyzing user intent for:', userInput);
      const intent = intentClassifier.classifyIntent(userInput);
      console.log('📊 Intent classification result:', intent);

      // Update conversation context with intent analysis
      await memoryStore.setConversationContext({
        sessionId: sessionStore.activeSessionId!,
        userGoals: [],
        healthPreferences: {},
        currentFocus: intent.standard_intent === 'health_explanation' || intent.standard_intent.includes('health') ? 'health' : 'general',
        recentTopics: [intent.intent],
        adaptiveLength: intent.qa_subtype === 'conversational' ? 'short' : 'detailed',
      });
      
      // Create a thinking message with animated loader
      const thinkingMessageId = Date.now().toString() + '_thinking';
      const thinkingMessage: Message = {
        id: thinkingMessageId,
        text: '',
        isUser: false,
        timestamp: new Date(),
      };
      
      this.setState(prevState => ({
        messages: [...prevState.messages, thinkingMessage]
      }));

      // Create the streaming response message
      const streamingMessageId = Date.now().toString() + '_streaming';
      const streamingMessage: Message = {
        id: streamingMessageId,
        text: '',
        isUser: false,
        timestamp: new Date(),
      };

      // Set up streaming state
      this.setState({
        streamingMessageId,
        currentStreamingText: '',
      });

      let fullAiResponse = '';

      // 🚀 ENHANCED: Use the new QueryRouter for intelligent response routing
      console.log('🔀 Routing query through enhanced system...');
      
      // Set up LlamaService in QueryRouter for streaming
      if (queryRouter.setLlamaService) {
        queryRouter.setLlamaService({
          generateResponse: async (prompt: string, context?: any, onToken?: (token: string) => void, onComplete?: () => void) => {
            return await llamaService.generateResponse(
              prompt,
              context,
              onToken || ((token: string) => {
                fullAiResponse += token;
                this.setState(prevState => {
                  const newStreamingText = prevState.currentStreamingText + token;
                  
                  // Remove thinking message and add/update streaming message
                  const updatedMessages = prevState.messages
                    .filter(msg => msg.id !== thinkingMessageId)
                    .filter(msg => msg.id !== streamingMessageId);
                  
                  const updatedStreamingMessage = {
                    ...streamingMessage,
                    text: newStreamingText,
                  };

                  return {
                    messages: [...updatedMessages, updatedStreamingMessage],
                    currentStreamingText: newStreamingText,
                    isAiProcessing: true,
                    canStopAiGeneration: true,
                  };
                }, () => {
                  this.throttledScrollToEnd();
                });
              }),
              onComplete || (async (fullResponse: string) => {
                // Save AI response to session store with intent metadata
                await sessionStore.addMessageToCurrentSession({
                  author: 'assistant',
                  text: fullResponse,
                  createdAt: Date.now(),
                  type: 'text',
                  metadata: {
                    healthQuery: intent.standard_intent.includes('health'),
                    goalQuery: intent.standard_intent.includes('goal'),
                  },
                });

                this.setState(prevState => ({
                  streamingMessageId: null,
                  currentStreamingText: '',
                  canStopAiGeneration: false,
                  messages: prevState.messages.map(msg => 
                    msg.id === streamingMessageId 
                      ? { ...msg, text: fullResponse }
                      : msg
                  ),
                }), () => {
                  setTimeout(() => {
                    this.scrollViewRef?.scrollToEnd({ animated: true });
                  }, 100);
                });
              })
            );
          }
        });
      }

      // Process query through enhanced routing system
      const response = await queryRouter.processQuery(
        userInput,
        // Token callback for streaming
        (token: string) => {
          fullAiResponse += token;
          this.setState(prevState => {
            const newStreamingText = prevState.currentStreamingText + token;
            
            // Remove thinking message and add/update streaming message
            const updatedMessages = prevState.messages
              .filter(msg => msg.id !== thinkingMessageId)
              .filter(msg => msg.id !== streamingMessageId);
            
            const updatedStreamingMessage = {
              ...streamingMessage,
              text: newStreamingText,
            };

            return {
              messages: [...updatedMessages, updatedStreamingMessage],
              currentStreamingText: newStreamingText,
              isAiProcessing: true,
              canStopAiGeneration: true,
            };
          }, () => {
            this.throttledScrollToEnd();
          });
        },
        // Complete callback
        async () => {
          // Save AI response to session store with enhanced metadata
          await sessionStore.addMessageToCurrentSession({
            author: 'assistant',
            text: fullAiResponse,
            createdAt: Date.now(),
            type: 'text',
            metadata: {
              healthQuery: intent.standard_intent.includes('health'),
              goalQuery: intent.standard_intent.includes('goal'),
            },
          });

          this.setState(prevState => ({
            streamingMessageId: null,
            currentStreamingText: '',
            canStopAiGeneration: false,
            isAiProcessing: false,
            messages: prevState.messages.map(msg => 
              msg.id === streamingMessageId 
                ? { ...msg, text: fullAiResponse }
                : msg
            ),
          }), () => {
            setTimeout(() => {
              this.scrollViewRef?.scrollToEnd({ animated: true });
            }, 100);
          });
        }
      );

      // Handle non-streaming responses (device actions, simple queries, etc.)
      if (response && !fullAiResponse) {
        // Remove thinking message
        this.setState(prevState => ({
          messages: prevState.messages.filter(msg => msg.id !== thinkingMessageId),
          streamingMessageId: null,
          currentStreamingText: '',
          canStopAiGeneration: false,
          isAiProcessing: false,
        }));

        // Add the response message
        this.addMessage(response, false);

        // Save to session store
        await sessionStore.addMessageToCurrentSession({
          author: 'assistant',
          text: response,
          createdAt: Date.now(),
          type: 'text',
          metadata: {
            healthQuery: intent.standard_intent.includes('health'),
            goalQuery: intent.standard_intent.includes('goal'),
          },
        });
      }
      
    } catch (error) {
      console.error('Error processing with enhanced AI:', error);
      this.setState({ 
        isAiProcessing: false,
        canStopAiGeneration: false,
        streamingMessageId: null,
        currentStreamingText: '',
      });
      this.addMessage('Sorry, I encountered an error processing your request. Please try again.', false);
    }
  };

  onSpeechPartialResults = (e: SpeechResultsEvent) => {
    console.log('onSpeechPartialResults: ', e);
    const partialResults = e.value && e.value?.length > 0 ? e.value : [];
    this.setState({
      partialResults,
      currentTranscription: partialResults.length > 0 ? partialResults[0] : '',
    });
  };

  onSpeechVolumeChanged = (e: any) => {
    console.log('onSpeechVolumeChanged: ', e);
    this.setState({
      pitch: e.value,
    });
  };

  selectLanguage = (language: Language) => {
    this.setState({ selectedLanguage: language });
    this.addMessage(`Language switched to ${language.name}`, false);
  };

  _startRecognizing = async () => {
    console.log('🟡 BUTTON PRESSED');
    console.log('🟡 Current state - isAiProcessing:', this.state.isAiProcessing);
    console.log('🟡 Current state - canStopAiGeneration:', this.state.canStopAiGeneration);
    console.log('🟡 Current state - isRecording:', this.state.isRecording);
    
    // If AI is currently processing, stop it instead of starting recognition
    if (this.state.isAiProcessing && this.state.canStopAiGeneration) {
      console.log('🟡 CONDITIONS MET: Calling _stopAiGeneration');
      await this._stopAiGeneration();
      return;
    }

    // If currently recording, stop recording
    if (this.state.isRecording) {
      console.log('🟡 STOP RECORDING');
      await this._stopRecognizing();
      return;
    }

    if (!this.state.isVoiceAvailable) {
      Alert.alert('Error', 'Voice recognition is not available on this device');
      return;
    }

    console.log('🟡 STARTING VOICE RECOGNITION');

    this.setState({
      recognized: '',
      pitch: '',
      error: '',
      started: '',
      results: [],
      partialResults: [],
      end: '',
      showRetryButton: false,
      lastErrorCanRetry: false,
    });

    try {
      const isRecognizing = await Voice.isRecognizing();
      if (isRecognizing) {
        await Voice.stop();
      }
      
      await Voice.start(this.state.selectedLanguage.code);
    } catch (e) {
      console.error('Voice start error:', e);
      this.setState({ 
        error: `Failed to start: ${e}`,
        isRecording: false,
        showRetryButton: true,
        lastErrorCanRetry: true,
      });
      Alert.alert('Error', 'Failed to start voice recognition. Please try again.');
    }
  };

  _stopRecognizing = async () => {
    try {
      await Voice.stop();
    } catch (e) {
      console.error(e);
      Alert.alert('Error', 'Failed to stop voice recognition');
    }
  };

  _retryVoiceRecognition = async () => {
    console.log('🔄 RETRY VOICE RECOGNITION');
    
    // Clear any existing error state
    this.setState({
      error: '',
      currentTranscription: '',
      showRetryButton: false,
      lastErrorCanRetry: false,
    });
    
    // Start recognition again
    await this._startRecognizing();
  };

  _stopAiGeneration = async () => {
    try {
      console.log('🔴 STOP AI GENERATION CALLED');
      console.log('🔴 LlamaService available:', !!llamaService);
      console.log('🔴 stopGeneration method available:', !!(llamaService && llamaService.stopGeneration));
      
      // Stop the AI generation process in LlamaService
      if (llamaService && llamaService.stopGeneration) {
        console.log('🔴 Calling llamaService.stopGeneration()');
        llamaService.stopGeneration();
      } else {
        console.log('🔴 ERROR: LlamaService or stopGeneration method not available');
      }

      console.log('🔴 Updating UI state');
      // Stop the AI generation process
      this.setState({ 
        isAiProcessing: false,
        canStopAiGeneration: false,
        streamingMessageId: null,
        currentStreamingText: '',
      });

      // Remove any thinking or streaming messages
      this.setState(prevState => ({
        messages: prevState.messages.filter(msg => 
          !msg.text.includes('thinking') && 
          msg.id !== prevState.streamingMessageId
        )
      }));

      console.log('AI generation stopped by user');
    } catch (error) {
      console.error('Error stopping AI generation:', error);
    }
  };

  _cancelRecognizing = async () => {
    try {
      await Voice.cancel();
    } catch (e) {
      console.error(e);
      Alert.alert('Error', 'Failed to cancel voice recognition');
    }
  };

  _destroyRecognizer = async () => {
    try {
      await Voice.destroy();
    } catch (e) {
      console.error(e);
    }
    this.setState({
      recognized: '',
      pitch: '',
      error: '',
      started: '',
      results: [],
      partialResults: [],
      end: '',
      isRecording: false,
    });
  };

  formatTime = (date: Date) => {
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  renderMessage = (message: Message) => {
    return (
      <View key={message.id} style={[
        styles.messageContainer,
        message.isUser ? styles.userMessageContainer : styles.aiMessageContainer
      ]}>
        <View style={[
          styles.messageBubble,
          message.isUser ? styles.userMessage : styles.aiMessage
        ]}>
          {message.text ? (
            <Text style={[
              styles.messageText,
              message.isUser ? styles.userMessageText : styles.aiMessageText
            ]}>
              {message.text}
            </Text>
          ) : (
            // Render animated loader for thinking state
            <View style={styles.loaderContainer}>
              <Text style={styles.aiMessageText}>AI is thinking</Text>
              <View style={styles.dotsContainer}>
                <Animated.View style={[styles.dot, { opacity: this.dotAnimation1 }]} />
                <Animated.View style={[styles.dot, { opacity: this.dotAnimation2 }]} />
                <Animated.View style={[styles.dot, { opacity: this.dotAnimation3 }]} />
              </View>
            </View>
          )}
        </View>
      </View>
    );
  };

  render() {
    return (
      <KeyboardAvoidingView 
        style={styles.container}
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
        keyboardVerticalOffset={Platform.OS === 'ios' ? 0 : 20}
      >
        <StatusBar barStyle="light-content" backgroundColor="#0a0a0a" />
        
        {/* Header */}
        <View style={styles.header}>
          <View style={styles.headerContent}>
            <View style={styles.brandContainer}>
              <View style={styles.brandIcon} />
              <Text style={styles.brandText}>Noise AI</Text>
            </View>
            <View style={styles.headerActions}>
              <TouchableOpacity 
                style={styles.languageSelector}
                onPress={() => {/* Language selector logic if needed */}}
              >
                <Text style={styles.languageText}>{this.state.selectedLanguage.name}</Text>
                <View style={styles.languageIndicator} />
              </TouchableOpacity>
              {/* Health Dashboard Button - Commented out for now */}
              {/* <TouchableOpacity 
                style={styles.healthDashboardButton}
                onPress={() => this.setState({ showHealthDashboard: true })}
              >
                <Text style={styles.healthDashboardButtonText}>📊</Text>
              </TouchableOpacity> */}
              <TouchableOpacity 
                style={styles.modelManagerButton}
                onPress={this.openModelManager}
              >
                <Text style={styles.modelManagerButtonText}>AI</Text>
              </TouchableOpacity>
              <TouchableOpacity 
                style={styles.smartwatchButton}
                onPress={this.openSmartwatchManager}
              >
                <Text style={styles.smartwatchButtonText}>⌚</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>

        {/* Language Selection */}
        <View style={styles.languageOptions}>
          {LANGUAGES.map((language) => (
            <TouchableOpacity
              key={language.code}
              style={[
                styles.languageOption,
                this.state.selectedLanguage.code === language.code && styles.selectedLanguageOption
              ]}
              onPress={() => this.selectLanguage(language)}
            >
              <Text style={[
                styles.languageOptionText,
                this.state.selectedLanguage.code === language.code && styles.selectedLanguageOptionText
              ]}>
                {language.name}
              </Text>
              {this.state.selectedLanguage.code === language.code && (
                <View style={styles.activeLanguageDot} />
              )}
            </TouchableOpacity>
          ))}
        </View>

        {/* Chat Messages */}
        <ScrollView 
          ref={(ref) => { this.scrollViewRef = ref; }}
          style={styles.chatContainer} 
          showsVerticalScrollIndicator={false}
          contentContainerStyle={styles.chatContent}
          keyboardShouldPersistTaps="handled"
          nestedScrollEnabled={true}
        >
          {this.state.messages.map(this.renderMessage)}
          
          {/* Current transcription (live) */}
          {this.state.currentTranscription && (
            <View style={[styles.messageContainer, styles.userMessageContainer]}>
              <View style={[styles.messageBubble, styles.transcribingMessage]}>
                <Text style={styles.transcribingText}>
                  {this.state.currentTranscription}
                </Text>
                <View style={styles.typingIndicator}>
                  <Animated.View style={[styles.typingDot, { opacity: this.pulseAnim }]} />
                  <Animated.View style={[styles.typingDot, { opacity: this.pulseAnim }]} />
                  <Animated.View style={[styles.typingDot, { opacity: this.pulseAnim }]} />
                </View>
              </View>
            </View>
          )}
        </ScrollView>

        {/* Recording Status */}
        {this.state.isRecording && (
          <View style={styles.recordingStatus}>
            <View style={styles.recordingStatusBubble}>
              <Animated.View style={[styles.recordingPulse, { transform: [{ scale: this.pulseAnim }] }]} />
              <Text style={styles.recordingText}>Listening...</Text>
            </View>
          </View>
        )}

        {/* Error Display */}
        {this.state.error && (
          <View style={styles.errorContainer}>
            <View style={styles.errorIcon} />
            <Text style={styles.errorText}>{this.state.error}</Text>
          </View>
        )}

        {/* Try Again Button */}
        {this.state.showRetryButton && this.state.lastErrorCanRetry && (
          <View style={styles.retryContainer}>
            <TouchableOpacity
              style={styles.retryButton}
              onPress={this._retryVoiceRecognition}
              activeOpacity={0.8}
            >
              <Text style={styles.retryButtonText}>🔄 Try Again</Text>
            </TouchableOpacity>
          </View>
        )}

        {/* AI Model Status */}
        <TouchableOpacity 
          style={styles.modelStatusContainer}
          onPress={this.openModelManager}
        >
          <View style={[styles.modelStatusIndicator, this.state.isModelLoaded ? styles.modelLoaded : styles.modelNotLoaded]} />
          <Text style={styles.modelStatusText}>{this.state.modelStatus}</Text>
          <Text style={styles.modelStatusHint}>Tap to manage</Text>
        </TouchableOpacity>

        {/* Enhanced Bottom Input Area - ChatGPT Style */}
        <View style={styles.inputContainer}>
          <View style={styles.chatInputArea}>
            {/* Unified Text Input with Mic */}
            <View style={styles.inputWrapper}>
              <TextInput
                style={styles.chatTextInput}
                value={this.state.textInput}
                onChangeText={this.handleTextInputChange}
                onFocus={this.handleTextInputFocus}
                placeholder="Message Noise AI..."
                placeholderTextColor="#666"
                multiline
                maxLength={500}
                returnKeyType="send"
                onSubmitEditing={this.handleSendTextMessage}
                blurOnSubmit={false}
              />
              
              {/* Right side buttons */}
              <View style={styles.inputActions}>
                {/* Send button (when text is present) */}
                {this.state.textInput.trim() ? (
                  <TouchableOpacity 
                    style={styles.sendButton}
                    onPress={this.handleSendTextMessage}
                  >
                    <Text style={styles.sendButtonText}>➤</Text>
                  </TouchableOpacity>
                ) : (
                  /* Mic button (when no text) */
                  <Animated.View style={{ transform: [{ scale: (this.state.isRecording || this.state.canStopAiGeneration) ? this.pulseAnim : 1 }] }}>
                    <TouchableOpacity 
                      onPress={this._startRecognizing} 
                      style={[
                        styles.micButton,
                        this.state.isRecording && styles.micButtonRecording,
                        this.state.canStopAiGeneration && styles.micButtonStopping,
                        !this.state.isVoiceAvailable && styles.micButtonDisabled
                      ]}
                      disabled={!this.state.isVoiceAvailable}
                    >
                      {this.state.canStopAiGeneration 
                        ? this.renderStopIcon(20, '#ffffff')
                        : this.renderMicIcon(20, this.state.isRecording ? '#ffffff' : '#ffffff')
                      }
                      {this.state.isRecording && <View style={styles.recordingRing} />}
                      {this.state.canStopAiGeneration && <View style={styles.stoppingRing} />}
                    </TouchableOpacity>
                  </Animated.View>
                )}
              </View>
            </View>
          </View>
        </View>

        {/* Model Manager Modal */}
        <ModelManager
          visible={this.state.showModelManager}
          onClose={this.closeModelManager}
          onModelStatusChange={this.handleModelStatusChange}
        />

        {/* Smartwatch Manager Modal */}
        <SmartwatchManager
          visible={this.state.showSmartwatchManager}
          onClose={this.closeSmartwatchManager}
        />

        {/* Health Dashboard Modal - Commented out for now
        {this.state.showHealthDashboard && (
          <View style={styles.modalOverlay}>
            <View style={styles.modalContainer}>
              <View style={styles.modalHeader}>
                <TouchableOpacity 
                  style={styles.modalCloseButton}
                  onPress={() => this.setState({ showHealthDashboard: false })}
                >
                  <Text style={styles.modalCloseButtonText}>✕</Text>
                </TouchableOpacity>
              </View>
              <HealthDashboard />
            </View>
          </View>
        )}
        */}
      </KeyboardAvoidingView>
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0a0a0a',
  },
  header: {
    backgroundColor: '#1a1a1a',
    paddingTop: 50,
    paddingBottom: 20,
    paddingHorizontal: 24,
    borderBottomWidth: 1,
    borderBottomColor: '#2a2a2a',
  },
  headerContent: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  brandContainer: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  brandIcon: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: '#00ff88',
    marginRight: 12,
  },
  brandText: {
    fontSize: 22,
    fontWeight: '300',
    color: '#ffffff',
    letterSpacing: 1,
  },
  languageSelector: {
    backgroundColor: '#2a2a2a',
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 20,
    flexDirection: 'row',
    alignItems: 'center',
  },
  languageText: {
    fontSize: 14,
    color: '#00ff88',
    fontWeight: '400',
    marginRight: 8,
  },
  languageIndicator: {
    width: 6,
    height: 6,
    borderRadius: 3,
    backgroundColor: '#00ff88',
  },
  languageOptions: {
    backgroundColor: '#1a1a1a',
    flexDirection: 'row',
    paddingHorizontal: 24,
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#2a2a2a',
  },
  languageOption: {
    backgroundColor: '#2a2a2a',
    paddingHorizontal: 20,
    paddingVertical: 10,
    borderRadius: 20,
    marginRight: 12,
    flexDirection: 'row',
    alignItems: 'center',
  },
  selectedLanguageOption: {
    backgroundColor: '#003d1f',
    borderWidth: 1,
    borderColor: '#00ff88',
  },
  languageOptionText: {
    fontSize: 14,
    color: '#cccccc',
    fontWeight: '400',
  },
  selectedLanguageOptionText: {
    color: '#00ff88',
  },
  activeLanguageDot: {
    width: 6,
    height: 6,
    borderRadius: 3,
    backgroundColor: '#00ff88',
    marginLeft: 8,
  },
  chatContainer: {
    flex: 1,
  },
  chatContent: {
    padding: 24,
    paddingBottom: 150, // Increased bottom padding to prevent cutoff
  },
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
  transcribingMessage: {
    backgroundColor: '#1a3d2a',
    borderBottomRightRadius: 6,
    borderWidth: 1,
    borderColor: '#00ff88',
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
  transcribingText: {
    color: '#00ff88',
    fontStyle: 'italic',
  },
  typingIndicator: {
    flexDirection: 'row',
    marginTop: 8,
    alignItems: 'center',
  },
  typingDot: {
    width: 4,
    height: 4,
    borderRadius: 2,
    backgroundColor: '#00ff88',
    marginHorizontal: 2,
  },
  recordingStatus: {
    position: 'absolute',
    top: 100,
    left: 0,
    right: 0,
    alignItems: 'center',
    zIndex: 1000,
  },
  recordingStatusBubble: {
    backgroundColor: '#1a3d2a',
    paddingHorizontal: 20,
    paddingVertical: 12,
    borderRadius: 25,
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#00ff88',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.3,
    shadowRadius: 4,
    elevation: 5,
  },
  recordingPulse: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: '#00ff88',
    marginRight: 12,
  },
  recordingText: {
    color: '#00ff88',
    fontSize: 14,
    fontWeight: '500',
  },
  errorContainer: {
    backgroundColor: '#3d1a1a',
    borderTopWidth: 1,
    borderTopColor: '#5a2a2a',
    paddingHorizontal: 24,
    paddingVertical: 16,
    flexDirection: 'row',
    alignItems: 'center',
  },
  errorIcon: {
    width: 6,
    height: 6,
    borderRadius: 3,
    backgroundColor: '#ff4757',
    marginRight: 12,
  },
  errorText: {
    color: '#ff6b6b',
    fontSize: 14,
    flex: 1,
  },
  retryContainer: {
    paddingHorizontal: 24,
    paddingVertical: 12,
    alignItems: 'center',
  },
  retryButton: {
    backgroundColor: '#2a4d3a',
    paddingHorizontal: 20,
    paddingVertical: 12,
    borderRadius: 24,
    borderWidth: 1,
    borderColor: '#00ff88',
    shadowColor: '#00ff88',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.2,
    shadowRadius: 4,
    elevation: 3,
  },
  retryButtonText: {
    color: '#00ff88',
    fontSize: 16,
    fontWeight: '600',
    textAlign: 'center',
  },
  inputContainer: {
    backgroundColor: '#1a1a1a',
    borderTopWidth: 1,
    borderTopColor: '#2a2a2a',
    paddingHorizontal: 24,
    paddingVertical: 20,
    paddingBottom: 40,
  },
  inputArea: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#2a2a2a',
    borderRadius: 25,
    paddingHorizontal: 20,
    paddingVertical: 16,
  },
  inputTextContainer: {
    flex: 1,
    marginRight: 16,
  },
  inputPlaceholder: {
    fontSize: 16,
    color: '#666666',
    fontWeight: '400',
  },
  modelStatusContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 24,
    paddingVertical: 8,
    backgroundColor: '#1a1a1a',
  },
  modelStatusIndicator: {
    width: 8,
    height: 8,
    borderRadius: 4,
    marginRight: 8,
  },
  modelLoaded: {
    backgroundColor: '#00ff88',
  },
  modelNotLoaded: {
    backgroundColor: '#ff6b6b',
  },
  modelStatusText: {
    color: '#cccccc',
    fontSize: 12,
    fontWeight: '400',
    flex: 1,
  },
  modelStatusHint: {
    color: '#666666',
    fontSize: 10,
    fontStyle: 'italic',
  },
  headerActions: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  modelManagerButton: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: '#00ff88',
    justifyContent: 'center',
    alignItems: 'center',
  },
  modelManagerButtonText: {
    color: '#000000',
    fontSize: 12,
    fontWeight: 'bold',
  },
  smartwatchButton: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: '#4ade80',
    justifyContent: 'center',
    alignItems: 'center',
  },
  smartwatchButtonText: {
    color: '#000000',
    fontSize: 16,
    fontWeight: 'bold',
  },
  healthDashboardButton: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: '#2a2a2a',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 8,
  },
  healthDashboardButtonText: {
    fontSize: 16,
  },
  loaderContainer: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  dotsContainer: {
    flexDirection: 'row',
    marginLeft: 8,
  },
  dot: {
    width: 4,
    height: 4,
    borderRadius: 2,
    backgroundColor: '#00ff88',
    marginHorizontal: 1,
  },
  modalOverlay: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(0, 0, 0, 0.9)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  modalContainer: {
    width: '100%',
    height: '100%',
    backgroundColor: '#0a0a0a',
  },
  modalHeader: {
    position: 'absolute',
    top: 60,
    right: 20,
    zIndex: 1000,
  },
  modalCloseButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: '#2a2a2a',
    justifyContent: 'center',
    alignItems: 'center',
  },
  modalCloseButtonText: {
    color: '#ffffff',
    fontSize: 18,
    fontWeight: 'bold',
  },
  
  // ChatGPT-Style Input Styles
  chatInputArea: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    paddingHorizontal: 16,
    paddingVertical: 12,
  },
  inputWrapper: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'flex-end',
    backgroundColor: '#2a2a2a',
    borderRadius: 25,
    paddingHorizontal: 16,
    paddingVertical: 12,
    minHeight: 50,
  },
  chatTextInput: {
    flex: 1,
    color: '#ffffff',
    fontSize: 16,
    maxHeight: 100,
    paddingVertical: 0,
    paddingHorizontal: 0,
    backgroundColor: 'transparent',
    textAlignVertical: 'center',
  },
  inputActions: {
    marginLeft: 12,
    justifyContent: 'center',
    alignItems: 'center',
  },
  sendButton: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: '#00ff88',
    justifyContent: 'center',
    alignItems: 'center',
  },
  sendButtonText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: 'bold',
  },
  micButton: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: '#00ff88',
    justifyContent: 'center',
    alignItems: 'center',
    position: 'relative',
  },
  micButtonRecording: {
    backgroundColor: '#ff4444',
  },
  micButtonStopping: {
    backgroundColor: '#ff8800',
  },
  micButtonDisabled: {
    backgroundColor: '#555555',
    opacity: 0.5,
  },
  recordingRing: {
    position: 'absolute',
    width: 40,
    height: 40,
    borderRadius: 20,
    borderWidth: 2,
    borderColor: '#ff4444',
    opacity: 0.6,
  },
  stoppingRing: {
    position: 'absolute',
    width: 40,
    height: 40,
    borderRadius: 20,
    borderWidth: 2,
    borderColor: '#ff8800',
    opacity: 0.6,
  },
  
  // Legacy styles - keeping for compatibility
  inputContainerExpanded: {
    backgroundColor: '#1a1a1a',
    paddingHorizontal: 20,
    paddingVertical: 15,
    borderTopWidth: 1,
    borderTopColor: '#2a2a2a',
    minHeight: 80,
  },
  textInputArea: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    backgroundColor: '#2a2a2a',
    borderRadius: 25,
    paddingHorizontal: 15,
    paddingVertical: 10,
    minHeight: 50,
  },
  textInput: {
    flex: 1,
    color: '#ffffff',
    fontSize: 16,
    maxHeight: 100,
    paddingVertical: 8,
    paddingHorizontal: 12,
    backgroundColor: 'transparent',
  },
  textInputActions: {
    flexDirection: 'row',
    alignItems: 'center',
    marginLeft: 10,
  },
  toggleInputButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: '#3a3a3a',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 8,
  },
  toggleInputButtonText: {
    fontSize: 18,
  },
  sendButtonDisabled: {
    backgroundColor: '#333333',
    opacity: 0.5,
  },
  textInputToggle: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: '#2a2a2a',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 12,
  },
  textInputToggleText: {
    fontSize: 20,
  },
});

export default VoiceTest;
