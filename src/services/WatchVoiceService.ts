import { NativeModules, NativeEventEmitter, EmitterSubscription } from 'react-native';
import { toByteArray } from 'base64-js'
import { whisperASRService } from './WhisperASRService';
import { opusDecoderService } from './OpusDecoderService';
import { zhSDKService } from './ZHSDKService';
import { watchAudioStore } from './WatchAudioStore';

const { ZHSDKModule } = NativeModules;

export interface WatchVoiceCommand {
  rawData: string;
  timestamp: string;
}

export interface WatchVoiceConfig {
  // AI Voice Command constants based on SDK documentation
  AI_VOICE_START: number;
  AI_VOICE_STOP: number;
  AI_VOICE_PAUSE: number;
  AI_VOICE_RESUME: number;
}

export interface WatchVoiceCallbacks {
  onVoiceCommand?: (command: WatchVoiceCommand) => void;
  onVoiceTranscription?: (text: string) => void;
  onVoiceError?: (error: string) => void;
}

// AI Voice Error Codes from SDK
export enum AiErrorCode {
  NORMAL = 0,
  NETWORK_ERROR = 1,
  NO_VOICE_TIMEOUT = 2,
  ASR_UNDERSTANDING_FAILURE = 3,
  SERVER_NO_RESPONSE = 4,
  RATE_LIMIT_EXCEEDED = 5,
}

// AI Voice Commands (these values need to be confirmed with actual SDK)
export const AI_VOICE_COMMANDS = {
  START_RECORDING: 1,
  STOP_RECORDING: 2,
  CANCEL_RECORDING: 3,
  AI_ASSISTANT_ACTIVATE: 4,
} as const;

class WatchVoiceService {
  private eventEmitter: NativeEventEmitter;
  private eventSubscriptions: Map<string, EmitterSubscription> = new Map();
  private callbacks: WatchVoiceCallbacks = {};
  private isWatchVoiceActive: boolean = false;
  private voiceResponseTimer: NodeJS.Timeout | null = null;
  private aiAudioFrameCount: number = 0;

  constructor() {
    console.log('🎤⌚ WatchVoiceService: constructor called')
    this.eventEmitter = new NativeEventEmitter(ZHSDKModule);
    console.log('🎤⌚ WatchVoiceService: eventEmitter created')
    this.setupEventListeners();
    console.log('🎤⌚ WatchVoiceService: event listeners set up')
  }

  private setupEventListeners() {
    console.log('🎤⌚ WatchVoiceService: setting up event listeners')
    
    // Listen for watch AI voice commands
    this.eventSubscriptions.set('onWatchAiVoiceCommand',
      this.eventEmitter.addListener('onWatchAiVoiceCommand', (command: WatchVoiceCommand) => {
        console.log('🎤⌚ Watch AI voice command received:', command);
        this.handleWatchVoiceCommand(command);
      })
    );
    console.log('🎤⌚ WatchVoiceService: onWatchAiVoiceCommand listener added')

    // Listen for raw AI voice data (v2.2.0+)
    this.eventSubscriptions.set('onWatchAiVoiceData',
      this.eventEmitter.addListener('onWatchAiVoiceData', (result: { base64: string; length: number }) => {
        console.log('🎤⌚ onWatchAiVoiceData event received:', result)
        if (result?.base64) {
          // If we somehow didn't see voiceState=1, start capture on first audio
          if (!this.isWatchVoiceActive) {
            console.log('🎤⌚ Received AI voice data without active state. Starting capture now.')
            this.isWatchVoiceActive = true
            try {
              whisperASRService.startCapture()
              opusDecoderService.reset()
              console.log('🎤⌚ Reset Opus decoder buffers for fallback recording session')
              opusDecoderService.init()?.catch(()=>{})
              // Start raw opus store with unknown session id fallback
              watchAudioStore.start('unknown').catch(()=>{})
            } catch {}
          }
          try {
            // Convert base64 to Uint8Array using base64-js
            const bytes = toByteArray(result.base64)
            // store raw opus for analysis
            watchAudioStore.addOpusBase64(result.base64).catch(()=>{})
            this.aiAudioFrameCount += 1
            if (this.aiAudioFrameCount <= 3) {
              const preview = Array.from(bytes.slice(0, 12)).map(b=>(b as number).toString(16).padStart(2,'0')).join(' ')
              console.log(`🎤⌚ onWatchAiVoiceData frame#${this.aiAudioFrameCount} len=${bytes.length} head=${preview}`)
            }
            // Store frame for later complete decoding
            opusDecoderService
              .decodeFrame(bytes)
              .then((decoded) => {
                if (decoded) {
                  // Still push individual frames for immediate processing
                  whisperASRService.pushDecodedPcm(decoded)
                  console.log(`🎤⌚ Decoded PCM chunk bytes=${decoded.length}`)
                }
              })
              .catch((e: any) => {
                console.warn('🎤⌚ Opus decode failed', e)
              })
          } catch (e) {
            console.warn('🎤⌚ Opus decode failed (bytes)', e)
          }
        } else {
          console.warn('🎤⌚ onWatchAiVoiceData received invalid result:', result)
        }
      })
    );
    console.log('🎤⌚ WatchVoiceService: onWatchAiVoiceData listener added')

    // Listen for AI command responses
    this.eventSubscriptions.set('onAiVoiceCommandSent',
      this.eventEmitter.addListener('onAiVoiceCommandSent', (result) => {
        console.log('🎤⌚ AI voice command sent result:', result);
      })
    );

    this.eventSubscriptions.set('onAiTranslatedTextSent',
      this.eventEmitter.addListener('onAiTranslatedTextSent', (result) => {
        console.log('🎤⌚ AI translated text sent result:', result);
      })
    );

    this.eventSubscriptions.set('onAiAnswerTextSent',
      this.eventEmitter.addListener('onAiAnswerTextSent', (result) => {
        console.log('🎤⌚ AI answer text sent result:', result);
      })
    );

    // Listen for AI error codes from watch
    this.eventSubscriptions.set('onWatchAiErrorCode',
      this.eventEmitter.addListener('onWatchAiErrorCode', (result) => {
        console.log('🎤⌚ AI error code received from watch:', result);
        if (this.callbacks.onVoiceError) {
          const errorMessage = this.getErrorMessage(result.errorCode);
          this.callbacks.onVoiceError(errorMessage);
        }
      })
    );
  }

  private handleWatchVoiceCommand(command: WatchVoiceCommand) {
    try {
      console.log('🎤⌚ Processing watch voice command:', command);
      console.log('🎤⌚ Available callbacks:', Object.keys(this.callbacks));
      
      // Parse the raw data to understand the command
      const { rawData } = command;
      
      // Notify callback
      if (this.callbacks.onVoiceCommand) {
        console.log('🎤⌚ Calling onVoiceCommand callback');
        this.callbacks.onVoiceCommand(command);
      } else {
        console.warn('🎤⌚ No onVoiceCommand callback available');
      }

      // Parse voice state from AiVoiceCmdBean format
      if (rawData && typeof rawData === 'string') {
        const voiceStateMatch = rawData.match(/voiceState=(\d+)/);
        if (voiceStateMatch) {
          const voiceState = parseInt(voiceStateMatch[1]);
          
          if (voiceState === 1) {
            this.isWatchVoiceActive = true;
            console.log('🎤⌚ Watch voice recording started (voiceState=1)');
            
            // NEW: Reset Opus decoder buffers to prevent audio accumulation between recordings
            opusDecoderService.reset();
            console.log('🎤⌚ Reset Opus decoder buffers for new recording session');
            
            // Send acknowledgment command 3 (ready to receive audio) - CORRECT FLOW
            console.log('🎤⌚ Sending acknowledgment command 3 (ready to receive audio)');
            this.sendAiVoiceCommand(3);
            
            // No longer using auto-response timer - will rely on real Whisper transcription

            // Start buffering PCM for Whisper
            whisperASRService.startCapture();
            // Start storing raw opus too
            const sid = this.extractSessionId(rawData)
            watchAudioStore.start(sid).catch(()=>{})
            
          } else if (voiceState === 2) {
            this.isWatchVoiceActive = false;
            console.log('🎤⌚ Watch voice recording stopped (voiceState=2)');
            
            // No auto-response timer to clear - using real transcription
            
            // Stop buffer and run Whisper transcription
            whisperASRService.stopCapture();
            
            // Save RAW Opus packets (zero-loss binary capture)
            const timestamp = new Date().toISOString().replace(/[:.]/g, '-')
            const rawOpusPath = `/storage/emulated/0/Download/NoiseAI_WatchRecordings/watch_raw_opus_packets_${timestamp}.bin`
            
            opusDecoderService.saveRawOpusPackets(rawOpusPath)
              .then((savedPath) => {
                if (savedPath) {
                  console.log('🎤⌚ Saved RAW Opus packets (zero-loss) at:', savedPath)
                }
              })
              .catch((error) => {
                console.warn('🎤⌚ Error saving raw Opus packets:', error)
              })
            
            // Get the complete decoded audio from Opus decoder
            opusDecoderService.getCompleteDecodedAudio()
              .then((completePcm) => {
                if (completePcm && completePcm.length > 0) {
                  console.log(`🎤⌚ Complete decoded audio: ${completePcm.length} bytes (expected: ~${this.aiAudioFrameCount * 400 * 8} bytes)`)
                  
                  // Save the complete decoded audio
                  const completePcmArray = Array.from(completePcm)
                  whisperASRService.saveCompletePcmAsWav(completePcmArray, 16000)
                    .then((wavPath) => {
                      if (wavPath) {
                        console.log('🎤⌚ Saved COMPLETE decoded WAV at:', wavPath)
                      }
                    })
                    .catch((error) => {
                      console.warn('🎤⌚ Error saving complete WAV:', error)
                    })
                } else {
                  console.warn('🎤⌚ No complete decoded audio available')
                }
              })
              .catch((error) => {
                console.warn('🎤⌚ Error getting complete decoded audio:', error)
              })
            
            // Save decoded PCM to a WAV so you can listen and verify
            whisperASRService
              .saveBufferedPcmAsWav(16000)
              .then((wav) => {
                if (wav) {
                  console.log('🎤⌚ Saved decoded WAV at:', wav)
                }
              })
              .catch((e) => {
                console.warn('🎤⌚ Failed to save decoded WAV', e)
              })
            // Save both raw Opus and decoded audio for manual comparison
            Promise.all([
              watchAudioStore.finalize(),
              whisperASRService.saveBufferedPcmAsWav(16000)
            ]).then(async ([opusPath, wavPath]) => {
              if (opusPath) {
                console.log('🎤⌚ Saved raw OPUS at:', opusPath)
              }
              if (wavPath) {
                console.log('🎤⌚ Saved decoded WAV at:', wavPath)
              }
              
              // NEW: Use OpusBridge to convert raw BIN to high-quality WAV for transcription
              try {
                // Find the most recent BIN file instead of using current timestamp
                const RNFS = require('react-native-fs')
                if (!RNFS || !RNFS.DownloadDirectoryPath) {
                  throw new Error('RNFS not available')
                }
                
                const downloadsDir = `${RNFS.DownloadDirectoryPath}/NoiseAI_WatchRecordings`
                const files = await RNFS.readDir(downloadsDir)
                const binFiles = files
                  .filter((file: any) => file.isFile() && file.name.endsWith('.bin'))
                  .sort((a: any, b: any) => b.mtime.getTime() - a.mtime.getTime())
                
                if (binFiles.length > 0) {
                  const latestBinFile = binFiles[0]
                  console.log('🎤⌚ Latest BIN file found:', latestBinFile.path)
                  
                  // Convert using OpusBridge for best quality
                  const { NativeModules } = require('react-native')
                  const { OpusBridge } = NativeModules
                  
                  if (OpusBridge) {
                    const timestamp = new Date().toISOString().replace(/[:.]/g, '-')
                    const sandboxBinPath = `${RNFS.DocumentDirectoryPath}/opus_input_${timestamp}.bin`
                    const sandboxWavPath = `${RNFS.DocumentDirectoryPath}/opus_output_${timestamp}.wav`
                    
                    // Copy BIN to sandbox
                    await RNFS.copyFile(latestBinFile.path, sandboxBinPath)
                    
                    // Convert to WAV using OpusBridge
                    const result = await OpusBridge.decodeBinToWav(sandboxBinPath, sandboxWavPath, { bytesPerPacket: 80 })
                    
                    if (result && result.wavPath) {
                      console.log('🎤⌚ OpusBridge conversion successful:', result.wavPath)
                      console.log('🎤⌚ WAV file size:', result.outputSize, 'bytes')
                      console.log('🎤⌚ Input BIN size:', result.inputSize, 'bytes')
                      
                      // NEW: Copy WAV file to Downloads for easy access and debugging
                      try {
                        const downloadsWavPath = `${RNFS.DownloadDirectoryPath}/NoiseAI_WatchRecordings/watch_decoded_${timestamp}.wav`
                        await RNFS.copyFile(result.wavPath, downloadsWavPath)
                        console.log('🎤⌚ WAV file copied to Downloads for debugging:', downloadsWavPath)
                      } catch (copyError) {
                        console.warn('🎤⌚ Failed to copy WAV to Downloads:', copyError)
                      }
                      
                      // Use the high-quality WAV for transcription
                      await this.processWhisperTranscriptionWithFile(result.wavPath, rawData)
                      return
                    } else {
                      console.warn('🎤⌚ OpusBridge conversion failed, falling back to buffered audio')
                    }
                  } else {
                    console.warn('🎤⌚ OpusBridge not available, falling back to buffered audio')
                  }
                } else {
                  console.log('🎤⌚ No BIN files found, using buffered audio')
                }
              } catch (error) {
                console.warn('🎤⌚ OpusBridge integration failed, falling back to buffered audio:', error)
              }
              
              // Fallback to original buffered audio transcription
              this.processWhisperTranscription(rawData)
            }).catch((error) => {
              console.warn('🎤⌚ Error saving audio files:', error)
              // Still try to process transcription
              this.processWhisperTranscription(rawData);
            });
            this.isWatchVoiceActive = false;
          }
        }
      } else {
        console.warn('🎤⌚ Invalid rawData received:', rawData);
      }

      // Transcription extraction is now handled above when voiceState=2

    } catch (error) {
      console.error('🎤⌚ Error handling watch voice command:', error);
      if (this.callbacks.onVoiceError) {
        this.callbacks.onVoiceError(error instanceof Error ? error.message : 'Unknown error');
      }
    }
  }

  private getErrorMessage(errorCode: number): string {
    switch (errorCode) {
      case AiErrorCode.NORMAL:
        return 'Normal operation';
      case AiErrorCode.NETWORK_ERROR:
        return 'Network connection error';
      case AiErrorCode.NO_VOICE_TIMEOUT:
        return 'No voice input detected (timeout)';
      case AiErrorCode.ASR_UNDERSTANDING_FAILURE:
        return 'Voice recognition failed';
      case AiErrorCode.SERVER_NO_RESPONSE:
        return 'Server not responding';
      case AiErrorCode.RATE_LIMIT_EXCEEDED:
        return 'Rate limit exceeded';
      default:
        return `Unknown error code: ${errorCode}`;
    }
  }

  private async sendAiVoiceCommand(command: number): Promise<void> {
    try {
      console.log('🎤⌚ Sending AI voice command:', command);
      
      // Use React Native bridge directly to avoid circular dependencies
      const { NativeModules } = require('react-native');
      const { ZHSDKModule } = NativeModules;
      
      if (!ZHSDKModule) {
        throw new Error('ZHSDKModule not available');
      }
      
      // Send AI voice command (1=enable, 2=start listening, 3=acknowledgment)
      await ZHSDKModule.sendAiVoiceCmd(command);
      
      console.log('🎤⌚ AI voice command sent successfully');
    } catch (error) {
      console.error('🎤⌚ Error sending AI voice command:', error);
    }
  }

  private async sendWatchTranslatedText(message: string): Promise<void> {
    try {
      console.log('🎤⌚ Sending translated text to watch:', message);
      
      // Use React Native bridge directly to avoid circular dependencies
      const { NativeModules } = require('react-native');
      const { ZHSDKModule } = NativeModules;
      
      if (!ZHSDKModule) {
        throw new Error('ZHSDKModule not available');
      }
      
      // Send AI translated text (for voice-to-text result)
      await ZHSDKModule.sendAiTranslatedText(message);
      
      console.log('🎤⌚ Translated text sent successfully');
    } catch (error) {
      console.error('🎤⌚ Error sending translated text to watch:', error);
    }
  }

  private async sendWatchAnswerText(message: string): Promise<void> {
    try {
      console.log('🎤⌚ Sending answer text to watch:', message);
      
      // Use React Native bridge directly to avoid circular dependencies
      const { NativeModules } = require('react-native');
      const { ZHSDKModule } = NativeModules;
      
      if (!ZHSDKModule) {
        throw new Error('ZHSDKModule not available');
      }
      
      // Send AI answer text (for voiceState=2 final response)
      await ZHSDKModule.sendAiAnswerText(message);
      
      console.log('🎤⌚ Answer text sent successfully');
    } catch (error) {
      console.error('🎤⌚ Error sending answer text to watch:', error);
    }
  }

  private async sendWatchViewUi(title: string, content: string): Promise<void> {
    try {
      console.log('🎤⌚ Sending view UI to watch:', { title, content });
      
      // Use React Native bridge directly to avoid circular dependencies
      const { NativeModules } = require('react-native');
      const { ZHSDKModule } = NativeModules;
      
      if (!ZHSDKModule) {
        throw new Error('ZHSDKModule not available');
      }
      
      // Send AI view UI (for visual display on watch)
      await ZHSDKModule.sendAiViewUi(title, content);
      
      console.log('🎤⌚ View UI sent successfully');
    } catch (error) {
      console.error('🎤⌚ Error sending view UI to watch:', error);
    }
  }

  private extractSessionId(rawData: string): string {
    if (!rawData || typeof rawData !== 'string') {
      return 'unknown';
    }
    
    const voiceNameMatch = rawData.match(/voiceName='([^']+)'/);
    return voiceNameMatch ? voiceNameMatch[1] : 'unknown';
  }

  private extractTranscriptionFromCommand(rawData: string): string | null {
    // Parse the AiVoiceCmdBean format: AiVoiceCmdBean{voiceState=X, voiceName='Y'}
    
    if (!rawData || typeof rawData !== 'string') {
      console.warn('🎤⌚ Invalid rawData for transcription extraction:', rawData);
      return null;
    }
    
    try {
      // Look for voiceState patterns in the AiVoiceCmdBean
      const voiceStateMatch = rawData.match(/voiceState=(\d+)/);
      const voiceNameMatch = rawData.match(/voiceName='([^']+)'/);
      
      if (voiceStateMatch && voiceNameMatch) {
        const voiceState = parseInt(voiceStateMatch[1]);
        const voiceName = voiceNameMatch[1];
        
        console.log(`🎤⌚ Parsed voice command - State: ${voiceState}, Name: ${voiceName}`);
        
        // voiceState=2 means voice recording ended
        if (voiceState === 2) {
          console.log(`🎤⌚ Voice recording completed for session: ${voiceName}`);
          console.log(`🎤⌚ Raw voice data: ${rawData}`);
          
          // The ZH SDK should provide transcribed text in the AiVoiceCmdBean
          // Let's try to extract actual transcription data from the bean
          
          // Look for any text field in the raw data
          const textMatch = rawData.match(/text['":]?\s*['":]([^'"]+)['"]/i);
          if (textMatch) {
            const transcription = textMatch[1].trim();
            console.log(`🎤⌚ Found transcribed text: "${transcription}"`);
            return transcription;
          }
          
          // Look for transcription field
          const transcriptionMatch = rawData.match(/transcription['":]?\s*['":]([^'"]+)['"]/i);
          if (transcriptionMatch) {
            const transcription = transcriptionMatch[1].trim();
            console.log(`🎤⌚ Found transcription field: "${transcription}"`);
            return transcription;
          }
          
          // If no transcription found in the data, the SDK might handle it differently
          console.warn(`🎤⌚ No transcription found in voice data. Raw data: ${rawData}`);
          console.warn(`🎤⌚ The watch may be expecting the app to handle raw audio data instead.`);
          
          // Return null - no automatic fake transcription
          return null;
        }
      }
      
      return null;
    } catch (error) {
      console.warn('🎤⌚ Error extracting transcription:', error);
      return null;
    }
  }

  // Public API methods
  async sendAiVoiceCommand(command: number): Promise<boolean> {
    try {
      return await ZHSDKModule.sendAiVoiceCommand(command);
    } catch (error) {
      console.error('🎤⌚ Error sending AI voice command:', error);
      return false;
    }
  }

  async sendTranscribedText(text: string): Promise<boolean> {
    try {
      return await ZHSDKModule.sendAiTranslatedText(text);
    } catch (error) {
      console.error('🎤⌚ Error sending transcribed text:', error);
      return false;
    }
  }

  async sendAiResponse(text: string): Promise<boolean> {
    try {
      return await ZHSDKModule.sendAiAnswerText(text);
    } catch (error) {
      console.error('🎤⌚ Error sending AI response:', error);
      return false;
    }
  }

  async sendErrorCode(errorCode: AiErrorCode): Promise<boolean> {
    try {
      return await ZHSDKModule.sendAiErrorCode(errorCode);
    } catch (error) {
      console.error('🎤⌚ Error sending error code:', error);
      return false;
    }
  }

  // Callback management
  setCallbacks(callbacks: WatchVoiceCallbacks) {
    this.callbacks = { ...this.callbacks, ...callbacks };
    console.log('🎤⌚ Callbacks updated:', Object.keys(this.callbacks));
  }

  clearCallbacks() {
    console.log('🎤⌚ Clearing callbacks');
    this.callbacks = {};
  }

  // Check if callbacks are properly set
  hasCallbacks(): boolean {
    return Object.keys(this.callbacks).length > 0;
  }

  // State getters
  get isActive(): boolean {
    return this.isWatchVoiceActive;
  }

  // Simulate voice interaction flow
  async simulateWatchVoiceInteraction(transcribedText: string): Promise<void> {
    try {
      console.log('🎤⌚ Simulating watch voice interaction with text:', transcribedText);
      
      // Send the transcribed text to the watch
      await this.sendTranscribedText(transcribedText);
      
      // Trigger the voice transcription callback to integrate with AI pipeline
      if (this.callbacks.onVoiceTranscription) {
        this.callbacks.onVoiceTranscription(transcribedText);
      }
    } catch (error) {
      console.error('🎤⌚ Error in simulated interaction:', error);
      if (this.callbacks.onVoiceError) {
        this.callbacks.onVoiceError(error instanceof Error ? error.message : 'Simulation error');
      }
    }
  }

  // Integration with existing AI pipeline
  async handleAiResponse(response: string): Promise<void> {
    try {
      // Send the AI response back to the watch
      await this.sendAiResponse(response);
      console.log('🎤⌚ AI response sent to watch:', response);
    } catch (error) {
      console.error('🎤⌚ Error sending AI response to watch:', error);
      await this.sendErrorCode(AiErrorCode.SERVER_NO_RESPONSE);
    }
  }

  // Cleanup
  cleanup() {
    this.clearAutoResponseTimer();
    this.eventSubscriptions.forEach(subscription => subscription.remove());
    this.eventSubscriptions.clear();
    this.callbacks = {};
    this.isWatchVoiceActive = false;
  }

  // Auto-response timer removed - now using real Whisper transcription only

  // clearAutoResponseTimer removed - no longer using auto-response timer

  private processVoiceResponse(rawData: string): void {
    // Extract transcription when recording stops
    const transcription = this.extractTranscriptionFromCommand(rawData);
    const sessionId = this.extractSessionId(rawData);
    
    if (transcription) {
      console.log(`🎤⌚ Extracted transcription: "${transcription}"`);
      
      // Step 1: Send transcribed text (voice-to-text result)
      console.log('🎤⌚ Sending transcribed text to watch');
      this.sendWatchTranslatedText(transcription);
      
      // Step 2: Generate AI response and send answer text
      const aiResponse = `AI processed: "${transcription}". Session: ${sessionId.replace('noise_ai_', '')}`;
      console.log('🎤⌚ Sending AI response:', aiResponse);
      this.sendWatchAnswerText(aiResponse);
      this.sendWatchViewUi("AI Assistant", aiResponse);
      
      if (this.callbacks.onVoiceTranscription) {
        console.log('🎤⌚ Calling onVoiceTranscription callback');
        this.callbacks.onVoiceTranscription(transcription);
      } else {
        console.warn('🎤⌚ No onVoiceTranscription callback available');
      }
    } else {
      console.warn('🎤⌚ No transcription extracted from command');
      
      // Since no transcription is available, simulate the flow
      const simulatedTranscription = `"What's my heart rate?"`;
      const aiResponse = `Your current heart rate is 72 BPM. Session: ${sessionId.replace('noise_ai_', '')}`;
      
      console.log('🎤⌚ Sending simulated transcription:', simulatedTranscription);
      this.sendWatchTranslatedText(simulatedTranscription);
      
      console.log('🎤⌚ Sending AI response:', aiResponse);
      this.sendWatchAnswerText(aiResponse);
      this.sendWatchViewUi("AI Assistant", aiResponse);
    }
  }

  private async processWhisperTranscription(rawData: string): Promise<void> {
    try {
      console.log('🎤⌚ [DEBUG] Running Whisper transcription for buffered audio');
      const transcript = await whisperASRService.transcribeBufferedAudio('ggml-base.en-q5_1.bin');
      const sessionId = this.extractSessionId(rawData);
      const text = transcript && transcript.length > 0 ? transcript : '...';

      console.log('🎤⌚ [DEBUG] Buffered audio transcription result:', text);
      console.log('🎤⌚ [DEBUG] Raw transcript length:', transcript?.length || 0);

      // Step 1: send transcribed text to watch
      await this.sendWatchTranslatedText(text);

      // Step 2: generate AI response using existing pipeline (simplified here)
      const aiResponse = `AI processed: "${text}". Session: ${sessionId.replace('noise_ai_', '')}`;
      await this.sendWatchAnswerText(aiResponse);
      await this.sendWatchViewUi('AI Assistant', aiResponse);

      if (this.callbacks.onVoiceTranscription) {
        console.log('🎤⌚ [DEBUG] Calling onVoiceTranscription callback with:', text);
        this.callbacks.onVoiceTranscription(text);
      }
    } catch (error) {
      console.error('🎤⌚ Whisper transcription failed:', error);
      await this.sendErrorCode(AiErrorCode.ASR_UNDERSTANDING_FAILURE);
    }
  }

  private async processWhisperTranscriptionWithFile(wavPath: string, rawData: string): Promise<void> {
    try {
      console.log('🎤⌚ [DEBUG] Running Whisper transcription for high-quality WAV file:', wavPath);
      const transcript = await whisperASRService.transcribeWavFile(wavPath, 'ggml-base.en-q5_1.bin');
      const sessionId = this.extractSessionId(rawData);
      const text = transcript && transcript.length > 0 ? transcript : '...';

      console.log('🎤⌚ [DEBUG] WAV file transcription result:', text);
      console.log('🎤⌚ [DEBUG] Raw transcript length:', transcript?.length || 0);

      // Step 1: send transcribed text to watch
      await this.sendWatchTranslatedText(text);

      // Step 2: generate AI response using existing pipeline (simplified here)
      const aiResponse = `AI processed: "${text}". Session: ${sessionId.replace('noise_ai_', '')}`;
      await this.sendWatchAnswerText(aiResponse);
      await this.sendWatchViewUi('AI Assistant', aiResponse);

      if (this.callbacks.onVoiceTranscription) {
        console.log('🎤⌚ [DEBUG] Calling onVoiceTranscription callback with:', text);
        this.callbacks.onVoiceTranscription(text);
      }
    } catch (error) {
      console.error('🎤⌚ Whisper transcription failed for high-quality WAV:', error);
      await this.sendErrorCode(AiErrorCode.ASR_UNDERSTANDING_FAILURE);
    }
  }
}

// Export singleton instance
export const watchVoiceService = new WatchVoiceService();
export default watchVoiceService;
