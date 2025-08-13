import React, { useState, useEffect } from 'react';
import { StyleSheet, View, StatusBar, Alert } from 'react-native';

// Import custom hooks
import {
  useVoiceRecognition,
  useChatState,
  useAIStreaming,
  useKeyboardHandling,
} from './src/hooks';

// Import components
import { ChatContainer } from './src/components/ChatInterface';
import { VoiceButton, LanguageSelector } from './src/components/VoiceControls';
import { ChatTextInput } from './src/components/InputControls';
import { NavigationTabs, TabType } from './src/components/NavigationTabs';

// Import existing components that we'll keep for now
import ModelManager from './ModelManager';
import HealthDashboard from './HealthDashboard';
import SmartwatchManager from './src/components/smartwatch/SmartwatchManager';

// Import services
import { chatService } from './src/services/chat/ChatService';
import { llamaService } from './LlamaService';
import { memoryStore } from './src/stores';

// Import types
import { AppState } from './src/types/chat';

const VoiceTest: React.FC = () => {
  // App state
  const [appState, setAppState] = useState<AppState>({
    isModelLoaded: false,
    modelStatus: 'Initializing AI model...',
    showModelManager: false,
    showHealthDashboard: false,
    showSmartwatchManager: false,
  });

  const [activeTab, setActiveTab] = useState<TabType>('chat');
  const [textInput, setTextInput] = useState('');

  // Initialize hooks
  const {
    messages,
    addMessage,
    updateStreamingMessage,
    clearChat,
    scrollToBottom,
    setScrollViewRef,
  } = useChatState();

  const {
    isAiProcessing,
    canStopAiGeneration,
    streamingMessageId,
    currentStreamingText,
    sendMessage,
    stopGeneration,
    retryLastMessage,
  } = useAIStreaming({
    onAddMessage: addMessage,
    onUpdateStreamingMessage: updateStreamingMessage,
  });

  const {
    isRecording,
    isVoiceAvailable,
    selectedLanguage,
    error: voiceError,
    currentTranscription,
    startRecording,
    stopRecording,
    setLanguage,
  } = useVoiceRecognition({
    onSpeechResult: async (result: string) => {
      console.log('🎤 Voice result:', result);
      await sendMessage(result);
    },
    onSpeechErrorCallback: (error: string) => {
      console.error('🎤 Voice error:', error);
      Alert.alert('Voice Error', error);
    },
    onPartialResult: (result: string) => {
      console.log('🎤 Partial result:', result);
    },
  });

  const { dismissKeyboard } = useKeyboardHandling({
    autoScrollOnShow: true,
    scrollToBottom,
  });

  // Initialize app
  useEffect(() => {
    initializeApp();
  }, []);

  // Save chat history when messages change
  useEffect(() => {
    if (messages.length > 1) {
      chatService.saveMessageHistory(messages);
    }
  }, [messages]);

  // Monitor AI model status
  useEffect(() => {
    const checkModelStatus = async () => {
      try {
        const isLoaded = llamaService.isReady();
        setAppState(prev => ({
          ...prev,
          isModelLoaded: isLoaded,
          modelStatus: isLoaded ? 'AI Model Ready' : 'AI Model Not Loaded',
        }));
      } catch (error) {
        console.error('Error checking model status:', error);
      }
    };

    const interval = setInterval(checkModelStatus, 5000);
    checkModelStatus(); // Initial check

    return () => clearInterval(interval);
  }, []);

  const initializeApp = async () => {
    try {
      console.log('🚀 Initializing Noise AI app...');

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

      // Load chat history
      try {
        const savedMessages = await chatService.loadMessageHistory();
        if (savedMessages.length > 0) {
          // We'll need to update the chat state with saved messages
          console.log('💾 Loaded chat history:', savedMessages.length, 'messages');
        }
      } catch (historyError) {
        console.log('Chat history load error:', historyError);
      }

      // Initialize AI model
      initializeAI();

      console.log('✅ App initialization complete');
    } catch (error) {
      console.error('❌ Error initializing app:', error);
    }
  };

  const initializeAI = async () => {
    try {
      console.log('🤖 Initializing AI model...');
      setAppState(prev => ({ ...prev, modelStatus: 'Loading AI model...' }));

      const isLoaded = llamaService.isReady();
      if (isLoaded) {
        setAppState(prev => ({
          ...prev,
          isModelLoaded: true,
          modelStatus: 'AI Model Ready',
        }));
        console.log('✅ AI model already loaded');
      } else {
        setAppState(prev => ({
          ...prev,
          isModelLoaded: false,
          modelStatus: 'AI Model needs to be downloaded',
        }));
        console.log('⚠️ AI model not loaded');
      }
    } catch (error) {
      console.error('❌ Error initializing AI:', error);
      setAppState(prev => ({
        ...prev,
        isModelLoaded: false,
        modelStatus: 'AI Model initialization failed',
      }));
    }
  };

  const handleVoiceButtonPress = async () => {
    if (isRecording) {
      await stopRecording();
    } else {
      if (!isVoiceAvailable) {
        Alert.alert('Voice Recognition', 'Voice recognition is not available on this device');
        return;
      }
      await startRecording();
    }
  };

  const handleSendTextMessage = async () => {
    if (!textInput.trim()) return;

    const message = textInput.trim();
    setTextInput('');
    dismissKeyboard();
    
    await sendMessage(message);
  };

  const handleStopGeneration = () => {
    stopGeneration();
  };

  const handleTabChange = (tab: TabType) => {
    setActiveTab(tab);
    
    // Update legacy state for compatibility
    setAppState(prev => ({
      ...prev,
      showModelManager: tab === 'model',
      showHealthDashboard: tab === 'health',
      showSmartwatchManager: tab === 'smartwatch',
    }));
  };

  const renderContent = () => {
    switch (activeTab) {
      case 'model':
        return (
          <ModelManager
            visible={true}
            onClose={() => setActiveTab('chat')}
          />
        );
      case 'health':
        return (
          <HealthDashboard />
        );
      case 'smartwatch':
        return (
          <SmartwatchManager
            visible={true}
            onClose={() => setActiveTab('chat')}
          />
        );
      default:
        return (
          <>
            <LanguageSelector
              selectedLanguage={selectedLanguage}
              onLanguageSelect={setLanguage}
              style={styles.languageSelector}
            />
            
            <ChatContainer
              messages={messages}
              streamingMessageId={streamingMessageId}
              currentStreamingText={currentStreamingText}
              onScrollViewRef={setScrollViewRef}
            >
              <View style={styles.inputContainer}>
                <ChatTextInput
                  value={textInput}
                  onChangeText={setTextInput}
                  onSend={handleSendTextMessage}
                  disabled={isAiProcessing}
                  placeholder={isRecording ? 'Listening...' : 'Type your message...'}
                />
                
                <View style={styles.voiceControlContainer}>
                  <VoiceButton
                    isRecording={isRecording}
                    onPress={handleVoiceButtonPress}
                    disabled={isAiProcessing}
                    size={50}
                  />
                  
                  {canStopAiGeneration && (
                    <View style={styles.stopButtonContainer}>
                      <VoiceButton
                        isRecording={false}
                        onPress={handleStopGeneration}
                        disabled={false}
                        size={40}
                      />
                    </View>
                  )}
                </View>
              </View>
            </ChatContainer>
          </>
        );
    }
  };

  return (
    <View style={styles.container}>
      <StatusBar barStyle="light-content" backgroundColor="#000" />
      
      {renderContent()}
      
      <NavigationTabs
        activeTab={activeTab}
        onTabChange={handleTabChange}
        modelStatus={appState.modelStatus}
        isModelLoaded={appState.isModelLoaded}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#000',
  },
  languageSelector: {
    borderBottomWidth: 1,
    borderBottomColor: '#333',
  },
  inputContainer: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    paddingHorizontal: 16,
    paddingVertical: 8,
    backgroundColor: '#000',
    borderTopWidth: 1,
    borderTopColor: '#333',
  },
  voiceControlContainer: {
    marginLeft: 8,
    alignItems: 'center',
  },
  stopButtonContainer: {
    marginTop: 8,
  },
});

export default VoiceTest;
