import React, { useState, useEffect } from 'react';
import { 
  StyleSheet, 
  View, 
  StatusBar, 
  Alert, 
  Text, 
  TouchableOpacity, 
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  Keyboard,
} from 'react-native';

// Import custom hooks
import {
  useVoiceRecognition,
  useChatState,
  useAIStreaming,
} from './src/hooks';
import { useWatchVoiceIntegration } from './src/hooks/useWatchVoiceIntegration';

// Import components
import { ChatContainer } from './src/components/ChatInterface';
import { VoiceButton, LanguageSelector } from './src/components/VoiceControls';
import { ChatTextInput } from './src/components/InputControls';
import { NavigationTabs, TabType } from './src/components/NavigationTabs';
import HeaderNavigation from './src/components/NavigationTabs/HeaderNavigation';

// Import existing components that we'll keep for now
import ModelManager from './ModelManager';
import HealthDashboard from './HealthDashboard';
import SmartwatchManager from './src/components/smartwatch/SmartwatchManager';

// Import services
import { chatService } from './src/services/chat/ChatService';
import { llamaService } from './LlamaService';
import queryRouter from './QueryRouter';
import { memoryStore } from './src/stores';

// Import types
import { AppState } from './src/types/chat';

// Import audio player
import Sound from 'react-native-sound';

type Language = {
  code: string;
  name: string;
};

const LANGUAGES: Language[] = [
  { code: 'en-IN', name: 'English' },
  { code: 'hi-IN', name: 'हिंदी' },
];

const VoiceTest: React.FC = () => {
  // State for tabs and navigation
  const [currentTab, setCurrentTab] = useState<TabType>('chat');
  const [showLanguageSelector, setShowLanguageSelector] = useState(false);

  // App state
  const [appState, setAppState] = useState<AppState>({
    isModelLoaded: false,
    modelStatus: 'Initializing AI model...',
    showModelManager: false,
    showHealthDashboard: false,
    showSmartwatchManager: false,
  });

  const [textInput, setTextInput] = useState('');
  const [partialResults, setPartialResults] = useState<string[]>([]);
  const [currentTranscription, setCurrentTranscription] = useState('');
  const [isKeyboardVisible, setIsKeyboardVisible] = useState(false);

  // NEW: Watch audio player state
  const [latestWatchAudio, setLatestWatchAudio] = useState<string | null>(null);
  const [isPlayingAudio, setIsPlayingAudio] = useState(false);
  const [audioPlayer, setAudioPlayer] = useState<Sound | null>(null);

  // Initialize hooks
  const {
    messages,
    addMessage,
    updateStreamingMessage,
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
  } = useAIStreaming({
    onAddMessage: addMessage,
    onUpdateStreamingMessage: updateStreamingMessage,
  });

  // Watch voice integration
  const {
    watchVoiceState,
    sendAiResponseToWatch,
    triggerWatchVoiceStart,
    triggerWatchVoiceStop,
    simulateWatchVoiceInput,
    isWatchConnected,
  } = useWatchVoiceIntegration({
    onWatchVoiceResult: async (transcription: string) => {
      console.log('🎤⌚ Watch voice result received:', transcription);
      // Process the transcription through the existing AI pipeline
      await sendMessage(transcription);
      
      // NEW: Check for latest decoded WAV file
      try {
        const { RNFS } = require('react-native-fs');
        const downloadsDir = `${RNFS.DownloadDirectoryPath}/NoiseAI_WatchRecordings`;
        const files = await RNFS.readDir(downloadsDir);
        const wavFiles = files
          .filter((file: any) => file.isFile() && file.name.endsWith('.wav'))
          .sort((a: any, b: any) => b.mtime.getTime() - a.mtime.getTime());
        
        if (wavFiles.length > 0) {
          const latestWav = wavFiles[0];
          setLatestWatchAudio(latestWav.path);
          console.log('🎵 Latest watch audio file found:', latestWav.path);
        }
      } catch (error) {
        console.log('🎵 No watch audio files found:', error);
      }
    },
    onWatchVoiceError: (error: string) => {
      console.error('🎤⌚ Watch voice error:', error);
      Alert.alert('Watch Voice Error', error);
    },
  });

  // NEW: Audio player functions for watch recordings
  const playWatchAudio = async (audioPath: string) => {
    try {
      // Stop any currently playing audio
      if (audioPlayer) {
        audioPlayer.stop();
        audioPlayer.release();
      }

      // Set up audio category
      Sound.setCategory('Playback');
      
      // Create new sound instance
      const sound = new Sound(audioPath, '', (err) => {
        if (err) {
          console.error('🎵 Error loading audio:', err);
          Alert.alert('Audio Error', 'Failed to load audio file');
          return;
        }
        
        console.log('🎵 Audio loaded successfully, duration:', sound.getDuration(), 'seconds');
        
        // Play the audio
        sound.play((success) => {
          if (success) {
            console.log('🎵 Audio playback completed');
          } else {
            console.log('🎵 Audio playback failed');
          }
          setIsPlayingAudio(false);
          sound.release();
        });
        
        setIsPlayingAudio(true);
        setAudioPlayer(sound);
      });
      
    } catch (error) {
      console.error('🎵 Error playing audio:', error);
      Alert.alert('Audio Error', 'Failed to play audio file');
    }
  };

  const stopWatchAudio = () => {
    if (audioPlayer) {
      audioPlayer.stop();
      audioPlayer.release();
      setAudioPlayer(null);
      setIsPlayingAudio(false);
    }
  };

  // NEW: Function to manually check for latest audio files
  const checkForLatestAudio = async () => {
    try {
      const { RNFS } = require('react-native-fs');
      const downloadsDir = `${RNFS.DownloadDirectoryPath}/NoiseAI_WatchRecordings`;
      const files = await RNFS.readDir(downloadsDir);
      const wavFiles = files
        .filter((file: any) => file.isFile() && file.name.endsWith('.wav'))
        .sort((a: any, b: any) => b.mtime.getTime() - a.mtime.getTime());
      
      if (wavFiles.length > 0) {
        const latestWav = wavFiles[0];
        setLatestWatchAudio(latestWav.path);
        console.log('🎵 Latest watch audio file found:', latestWav.path);
        Alert.alert('Audio Found', `Found latest audio: ${latestWav.name}`);
      } else {
        console.log('🎵 No WAV files found');
        Alert.alert('No Audio', 'No WAV files found in Downloads/NoiseAI_WatchRecordings');
      }
    } catch (error) {
      console.log('🎵 Error checking for audio files:', error);
      Alert.alert('Error', 'Failed to check for audio files');
    }
  };

  const handleSendMessage = async () => {
    if (textInput.trim()) {
      await sendMessage(textInput);
      setTextInput('');
    }
  };

  const {
    isRecording,
    isVoiceAvailable,
    selectedLanguage,
    startRecording,
    stopRecording,
    setLanguage,
  } = useVoiceRecognition({
    onSpeechResult: async (result: string) => {
      console.log('🎤 Voice result:', result);
      setCurrentTranscription('');
      setPartialResults([]);
      await sendMessage(result);
    },
    onSpeechErrorCallback: (error: string) => {
      console.error('🎤 Voice error:', error);
      setCurrentTranscription('');
      setPartialResults([]);
      Alert.alert('Voice Error', error);
    },
    onPartialResult: (result: string) => {
      console.log('🎤 Partial result:', result);
      setCurrentTranscription(result);
      setPartialResults([result]);
    },
  });

  // Initialize app
  useEffect(() => {
    const init = async () => {
      console.log('🚀 VoiceTest initialized');
      await initializeApp();
    };
    init();
  }, []);

  // Save chat history when messages change
  useEffect(() => {
    if (messages.length > 1) {
      chatService.saveMessageHistory(messages);
    }
  }, [messages]);

  // Keyboard visibility listeners
  useEffect(() => {
    const keyboardDidShowListener = Keyboard.addListener('keyboardDidShow', () => {
      setIsKeyboardVisible(true);
    });
    const keyboardDidHideListener = Keyboard.addListener('keyboardDidHide', () => {
      setIsKeyboardVisible(false);
    });

    return () => {
      keyboardDidShowListener.remove();
      keyboardDidHideListener.remove();
    };
  }, []);

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

  // Monitor AI response completion and send to watch
  useEffect(() => {
    if (!isAiProcessing && streamingMessageId && currentStreamingText && isWatchConnected) {
      // AI response is complete, send it to the watch
      console.log('🎤⌚ AI response completed, sending to watch:', currentStreamingText);
      sendAiResponseToWatch(currentStreamingText);
    }
  }, [isAiProcessing, streamingMessageId, currentStreamingText, isWatchConnected, sendAiResponseToWatch]);

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

      // Try to initialize the AI model
      const isInitialized = await llamaService.initialize();
      
      if (isInitialized) {
        // 🔧 FIX: Inject LlamaService into QueryRouter for real AI responses
        queryRouter.setLlamaService(llamaService);
        console.log('✅ QueryRouter configured with LlamaService');
        
        setAppState(prev => ({
          ...prev,
          isModelLoaded: true,
          modelStatus: 'AI ready!',
        }));
        console.log('✅ AI model loaded successfully');
      } else {
        setAppState(prev => ({
          ...prev,
          isModelLoaded: false,
          modelStatus: 'AI model not available. Tap to download.',
        }));
        console.log('⚠️ AI model not loaded');
      }
    } catch (error) {
      console.error('❌ Error initializing AI:', error);
      setAppState(prev => ({
        ...prev,
        isModelLoaded: false,
        modelStatus: 'AI initialization failed',
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
    Keyboard.dismiss();
    
    await sendMessage(message);
  };

  const handleStopGeneration = () => {
    stopGeneration();
  };

  const handleTabChange = (tab: TabType) => {
    setCurrentTab(tab);
    
    // Update legacy state for compatibility
    setAppState(prev => ({
      ...prev,
      showModelManager: tab === 'model',
      showHealthDashboard: tab === 'health',
      showSmartwatchManager: tab === 'smartwatch',
    }));
  };

  const renderContent = () => {
    switch (currentTab) {
      case 'model':
        return (
          <ModelManager
            visible={true}
            onClose={() => setCurrentTab('chat')}
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
            onClose={() => setCurrentTab('chat')}
          />
        );
      default:
        return (
          <ChatContainer
            messages={messages}
            streamingMessageId={streamingMessageId}
            currentStreamingText={currentStreamingText}
            onScrollViewRef={setScrollViewRef}
          />
        );
    }
  };

  return (
    <KeyboardAvoidingView 
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : 'padding'}
      keyboardVerticalOffset={Platform.OS === 'ios' ? 0 : 10}
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
              onPress={() => setShowLanguageSelector(!showLanguageSelector)}
            >
              <Text style={styles.languageText}>
                {selectedLanguage.name}
              </Text>
              <View style={styles.languageIndicator} />
            </TouchableOpacity>
            <TouchableOpacity 
              style={styles.modelManagerButton}
              onPress={() => handleTabChange('model')}
            >
              <Text style={styles.modelManagerButtonText}>AI</Text>
            </TouchableOpacity>
            <TouchableOpacity 
              style={styles.smartwatchButton}
              onPress={() => handleTabChange('smartwatch')}
            >
              <Text style={styles.smartwatchButtonText}>⌚</Text>
            </TouchableOpacity>
            {/* Watch AI Status Indicator (Development Only) */}
            {__DEV__ && (
              <>
                <TouchableOpacity 
                  style={[styles.smartwatchButton, { 
                    backgroundColor: watchVoiceState.isWatchVoiceActive ? '#4ade80' : '#6b7280',
                    opacity: watchVoiceState.isWatchVoiceActive ? 1 : 0.6 
                  }]}
                  onPress={() => {
                    Alert.alert(
                      'Watch AI Status', 
                      `AI Commands: ${watchVoiceState.isWatchVoiceActive ? 'Active' : 'Waiting'}\n\nPress the AI button on your watch to send voice commands.`
                    );
                  }}
                >
                  <Text style={styles.smartwatchButtonText}>🎤</Text>
                </TouchableOpacity>
                
                {/* Debug Connection Button */}
                <TouchableOpacity 
                  style={[styles.smartwatchButton, { backgroundColor: '#ff9500' }]}
                  onPress={async () => {
                    try {
                      console.log('🔍 Testing AI voice connection...');
                      const result = await zhSDKService.testAiVoiceConnection();
                      console.log('🔍 Connection test result:', result);
                      Alert.alert(
                        'AI Voice Connection Test',
                        `Connected: ${result.isConnected}
SDK Init: ${result.sdkInitialized}
Callback: ${result.callbackRegistered}
Status: ${result.testStatus || 'N/A'}
${result.error ? `Error: ${result.error}` : ''}`
                      );
                    } catch (error) {
                      console.error('🔍 Connection test failed:', error);
                      Alert.alert('Test Failed', error instanceof Error ? error.message : 'Unknown error');
                    }
                  }}
                >
                  <Text style={[styles.smartwatchButtonText, { fontSize: 14 }]}>🔍</Text>
                </TouchableOpacity>
                
                {/* Initialize Watch AI Button */}
                <TouchableOpacity 
                  style={[styles.smartwatchButton, { backgroundColor: '#16a34a' }]}
                  onPress={async () => {
                    try {
                      console.log('🎤⌚ Initializing watch AI...');
                      const result = await zhSDKService.initializeWatchAI();
                      console.log('🎤⌚ Initialize AI result:', result);
                      Alert.alert(
                        'Initialize Watch AI', 
                        result 
                          ? 'AI initialized! Now start listening mode.' 
                          : 'Failed to initialize AI on watch'
                      );
                    } catch (error) {
                      console.error('🎤⌚ Initialize AI failed:', error);
                      Alert.alert('Initialize AI Failed', error instanceof Error ? error.message : 'Unknown error');
                    }
                  }}
                >
                  <Text style={[styles.smartwatchButtonText, { fontSize: 14 }]}>🔧</Text>
                </TouchableOpacity>

                {/* Start AI Listening Button */}
                <TouchableOpacity 
                  style={[styles.smartwatchButton, { backgroundColor: '#dc2626' }]}
                  onPress={async () => {
                    try {
                      console.log('🎤⌚ Starting AI listening...');
                      const result = await zhSDKService.startWatchAIListening();
                      console.log('🎤⌚ Start listening result:', result);
                      Alert.alert(
                        'Start AI Listening', 
                        result 
                          ? 'AI listening started! Press AI button on watch and speak.' 
                          : 'Failed to start AI listening'
                      );
                    } catch (error) {
                      console.error('🎤⌚ Start listening failed:', error);
                      Alert.alert('Start Listening Failed', error instanceof Error ? error.message : 'Unknown error');
                    }
                  }}
                >
                  <Text style={[styles.smartwatchButtonText, { fontSize: 14 }]}>🎙️</Text>
                </TouchableOpacity>

                {/* Stop AI Listening Button */}
                <TouchableOpacity 
                  style={[styles.smartwatchButton, { backgroundColor: '#6b7280' }]}
                  onPress={async () => {
                    try {
                      console.log('🎤⌚ Stopping AI listening...');
                      const result = await zhSDKService.stopWatchAIListening();
                      console.log('🎤⌚ Stop listening result:', result);
                      Alert.alert(
                        'Stop AI Listening', 
                        result 
                          ? 'AI listening stopped.' 
                          : 'Failed to stop AI listening'
                      );
                    } catch (error) {
                      console.error('🎤⌚ Stop listening failed:', error);
                      Alert.alert('Stop Listening Failed', error instanceof Error ? error.message : 'Unknown error');
                    }
                  }}
                >
                  <Text style={[styles.smartwatchButtonText, { fontSize: 14 }]}>⏹️</Text>
                </TouchableOpacity>
              </>
            )}
          </View>
        </View>
      </View>

      {/* NEW: Watch Audio Player */}
      {latestWatchAudio && (
        <View style={styles.audioPlayerContainer}>
          <View style={styles.audioPlayerContent}>
            <Text style={styles.audioPlayerTitle}>🎵 Latest Watch Recording</Text>
            <Text style={styles.audioPlayerFile} numberOfLines={1}>
              {latestWatchAudio.split('/').pop() || 'Unknown file'}
            </Text>
            <View style={styles.audioPlayerControls}>
              {isPlayingAudio ? (
                <TouchableOpacity 
                  style={[styles.audioButton, styles.stopButton]} 
                  onPress={stopWatchAudio}
                >
                  <Text style={styles.audioButtonText}>⏹️ Stop</Text>
                </TouchableOpacity>
              ) : (
                <TouchableOpacity 
                  style={[styles.audioButton, styles.playButton]} 
                  onPress={() => playWatchAudio(latestWatchAudio)}
                >
                  <Text style={styles.audioButtonText}>▶️ Play</Text>
                </TouchableOpacity>
              )}
              <TouchableOpacity 
                style={[styles.audioButton, styles.infoButton]} 
                onPress={() => {
                  Alert.alert(
                    'Audio File Info', 
                    `Path: ${latestWatchAudio}\n\nThis is the latest decoded WAV file from your smartwatch recording. Use it to verify audio quality and debug transcription issues.`
                  );
                }}
              >
                <Text style={styles.audioButtonText}>ℹ️ Info</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      )}

      {/* NEW: Manual Audio Check Button */}
      {/* 
      <View style={styles.audioCheckContainer}>
        <TouchableOpacity 
          style={styles.audioCheckButton} 
          onPress={checkForLatestAudio}
        >
          <Text style={styles.audioCheckButtonText}>🔍 Check for Audio Files</Text>
        </TouchableOpacity>
        <Text style={styles.audioCheckHint}>
          Use this button to manually check for new watch recordings
        </Text>
      </View>
      */}

      {/* Language Selection */}
      {showLanguageSelector && (
        <View style={styles.languageOptions}>
          {LANGUAGES.map((language: Language) => (
            <TouchableOpacity
              key={language.code}
              style={[
                styles.languageOption,
                selectedLanguage.code === language.code && styles.selectedLanguageOption
              ]}
              onPress={() => {
                setLanguage(language);
                setShowLanguageSelector(false);
              }}
            >
              <Text style={[
                styles.languageOptionText,
                selectedLanguage.code === language.code && styles.selectedLanguageOptionText
              ]}>
                {language.name}
              </Text>
              {selectedLanguage.code === language.code && (
                <View style={styles.activeLanguageDot} />
              )}
            </TouchableOpacity>
          ))}
        </View>
      )}

      {renderContent()}

      {/* AI Model Status Bar */}
      <TouchableOpacity 
        style={styles.modelStatusContainer}
        onPress={() => handleTabChange('model')}
      >
        <View style={[
          styles.modelStatusIndicator, 
          appState.isModelLoaded ? styles.modelLoaded : styles.modelNotLoaded
        ]} />
        <Text style={styles.modelStatusText}>{appState.modelStatus}</Text>
        <Text style={styles.modelStatusHint}>Tap to manage</Text>
      </TouchableOpacity>

      {/* Enhanced Bottom Input Area - ChatGPT Style */}
      <View style={[
        styles.inputContainer,
        { paddingBottom: 40 }
      ]}>
        {/* Partial Results Display - Beautiful Bubble Style */}
        {(isRecording && currentTranscription) && (
          <View style={styles.partialResultsContainer}>
            <View style={styles.partialResultsBubble}>
              <View style={styles.recordingPulse} />
              <View style={styles.partialResultsContent}>
                <Text style={styles.partialResultsLabel}>Listening...</Text>
                <Text style={styles.partialResultsText}>{currentTranscription}</Text>
              </View>
            </View>
          </View>
        )}

        {/* Watch Voice Status Display */}
        {watchVoiceState.isWatchVoiceActive && (
          <View style={styles.partialResultsContainer}>
            <View style={[styles.partialResultsBubble, styles.watchVoiceBubble]}>
              <View style={[styles.recordingPulse, styles.watchRecordingPulse]} />
              <View style={styles.partialResultsContent}>
                <Text style={[styles.partialResultsLabel, styles.watchVoiceLabel]}>⌚ Watch Voice Active...</Text>
                {watchVoiceState.lastWatchCommand && (
                  <Text style={styles.partialResultsText}>Listening from smartwatch</Text>
                )}
              </View>
            </View>
          </View>
        )}

        {/* Watch Voice Processing Display */}
        {watchVoiceState.isProcessingWatchVoice && (
          <View style={styles.partialResultsContainer}>
            <View style={[styles.partialResultsBubble, styles.processingBubble]}>
              <View style={[styles.recordingPulse, styles.processingPulse]} />
              <View style={styles.partialResultsContent}>
                <Text style={[styles.partialResultsLabel, styles.processingLabel]}>⌚ Processing watch voice...</Text>
              </View>
            </View>
          </View>
        )}

        <View style={styles.chatInputArea}>
          <View style={styles.inputWrapper}>
            <ChatTextInput
              value={textInput}
              onChangeText={setTextInput}
              onSend={handleSendMessage}
              placeholder="Message Noise AI..."
              disabled={isAiProcessing}
              multiline={true}
            />
            
            <View style={styles.inputActions}>
              {textInput.trim() ? (
                <TouchableOpacity 
                  style={styles.sendButton}
                  onPress={() => handleSendMessage()}
                >
                  <Text style={styles.sendButtonText}>➤</Text>
                </TouchableOpacity>
              ) : (
                <View style={styles.voiceControlContainer}>
                  <VoiceButton
                    isRecording={isRecording || canStopAiGeneration}
                    onPress={canStopAiGeneration ? handleStopGeneration : startRecording}
                    disabled={!isVoiceAvailable}
                    size={32}
                  />
                </View>
              )}
            </View>
          </View>
        </View>
      </View>
    </KeyboardAvoidingView>
  );
};

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
  headerActions: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
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
  recordingPulse: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: '#00ff88',
    marginRight: 12,
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
  inputContainer: {
    backgroundColor: '#1a1a1a',
    borderTopWidth: 1,
    borderTopColor: '#2a2a2a',
    paddingHorizontal: 24,
    paddingVertical: 16,
  },
  chatInputArea: {
    flexDirection: 'row',
    alignItems: 'flex-end',
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
  voiceControlContainer: {
    alignItems: 'center',
  },
  stopButtonContainer: {
    marginTop: 8,
  },
  partialResultsContainer: {
    alignItems: 'center',
    marginBottom: 12,
  },
  partialResultsBubble: {
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
    minWidth: 200,
  },
  partialResultsContent: {
    flex: 1,
  },
  partialResultsLabel: {
    color: '#00ff88',
    fontSize: 12,
    fontWeight: '600',
    marginBottom: 4,
  },
  partialResultsText: {
    color: '#ffffff',
    fontSize: 14,
    lineHeight: 18,
  },
  // Watch Voice Styles
  watchVoiceBubble: {
    backgroundColor: '#1a2d3d',
    borderColor: '#4ade80',
  },
  watchRecordingPulse: {
    backgroundColor: '#4ade80',
  },
  watchVoiceLabel: {
    color: '#4ade80',
  },
  processingBubble: {
    backgroundColor: '#2d1a3d',
    borderColor: '#a855f7',
  },
  processingPulse: {
    backgroundColor: '#a855f7',
  },
  processingLabel: {
    color: '#a855f7',
  },
  audioPlayerContainer: {
    backgroundColor: '#1a1a1a',
    paddingHorizontal: 24,
    paddingVertical: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#2a2a2a',
  },
  audioPlayerContent: {
    backgroundColor: '#2a2a2a',
    borderRadius: 15,
    padding: 20,
    alignItems: 'center',
  },
  audioPlayerTitle: {
    color: '#ffffff',
    fontSize: 18,
    fontWeight: 'bold',
    marginBottom: 10,
  },
  audioPlayerFile: {
    color: '#00ff88',
    fontSize: 14,
    fontWeight: '500',
    marginBottom: 15,
  },
  audioPlayerControls: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    width: '100%',
  },
  audioButton: {
    backgroundColor: '#00ff88',
    paddingVertical: 10,
    paddingHorizontal: 20,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: '#00ff88',
  },
  audioButtonText: {
    color: '#000000',
    fontSize: 14,
    fontWeight: 'bold',
  },
  playButton: {
    backgroundColor: '#00ff88',
  },
  stopButton: {
    backgroundColor: '#ff6b6b',
  },
  infoButton: {
    backgroundColor: '#6b7280',
  },
  // audioCheckContainer: {
  //   alignItems: 'center',
  //   marginTop: 10,
  //   marginBottom: 10,
  //   paddingHorizontal: 24,
  // },
  // audioCheckButton: {
  //   backgroundColor: '#2a2a2a',
  //   paddingVertical: 12,
  //   paddingHorizontal: 25,
  //   borderRadius: 20,
  //   borderWidth: 1,
  //   borderColor: '#00ff88',
  // },
  // audioCheckButtonText: {
  //   color: '#00ff88',
  //   fontSize: 14,
  //   fontWeight: '600',
  // },
  // audioCheckHint: {
  //   color: '#666666',
  //   fontSize: 12,
  //   marginTop: 5,
  //   textAlign: 'center',
  // },
});

export default VoiceTest;
