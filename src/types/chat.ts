// Shared types for chat functionality

export type Language = {
  code: string;
  name: string;
};

export type Message = {
  id: string;
  text: string;
  isUser: boolean;
  timestamp: Date;
};

export type VoiceState = {
  recognized: string;
  pitch: string;
  error: string;
  end: string;
  started: string;
  results: string[];
  partialResults: string[];
  isRecording: boolean;
  isVoiceAvailable: boolean;
  currentTranscription: string;
};

export type AIState = {
  isAiProcessing: boolean;
  canStopAiGeneration: boolean;
  streamingMessageId: string | null;
  currentStreamingText: string;
  showRetryButton: boolean;
  lastErrorCanRetry: boolean;
};

export type AppState = {
  isModelLoaded: boolean;
  modelStatus: string;
  showModelManager: boolean;
  showHealthDashboard: boolean;
  showSmartwatchManager: boolean;
};

export const LANGUAGES: Language[] = [
  { code: 'en-IN', name: 'English' },
  { code: 'hi-IN', name: 'हिंदी' },
];
