import { initLlama, LlamaContext, ContextParams, CompletionParams } from 'llama.rn';
import AsyncStorage from '@react-native-async-storage/async-storage';
import RNFS from 'react-native-fs';
import { AppState, AppStateStatus } from 'react-native';
import healthDataManager from './HealthDataManager';
import { sessionStore, memoryStore } from './src/stores';
import { responseGenerator } from './ResponseGenerator';
import { databaseService } from './DatabaseService';
import EnhancedIntentClassifier, { enhancedIntentClassifier } from './EnhancedIntentClassifier';

interface LlamaConfig {
  modelPath?: string;
  isModelLoaded: boolean;
  context: string;
  autoOffloadEnabled: boolean;
  lastUsedTimestamp: number;
}

interface ModelDownloadProgress {
  bytesWritten: number;
  contentLength: number;
  progress: number;
}

interface ModelInfo {
  name: string;
  url: string;
  filename: string;
  size: number;
  description: string;
}

class LlamaService {
  private llamaContext: LlamaContext | null = null;
  private config: LlamaConfig = {
    isModelLoaded: false,
    context: 'You are Noise AI, a helpful health assistant. You provide accurate, supportive, and easy-to-understand health information. Always encourage users to consult healthcare professionals for serious concerns.',
    autoOffloadEnabled: true,
    lastUsedTimestamp: Date.now(),
  };
  private appStateSubscription: any = null;
  private offloadTimer: NodeJS.Timeout | null = null;
  private downloadInProgress: boolean = false;
  private currentStreamingInterval: NodeJS.Timeout | null = null;
  private shouldStopGeneration: boolean = false;
  private currentCompletionPromise: Promise<any> | null = null;

  // Available models
  private static readonly MODELS: ModelInfo[] = [
    {
      name: 'Llama-3.2-1B-Instruct',
      url: 'https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q8_0.gguf',
      filename: 'llama-3.2-1b-instruct-q8_0.gguf',
      size: 1342177280, // ~1.3GB
      description: 'Compact 1B parameter model optimized for health assistance'
    }
  ];

  constructor() {
    this.setupAppStateListener();
    // Initialize database when service is created
    this.initializeDatabase();
  }

  // Initialize database
  private async initializeDatabase() {
    try {
      await databaseService.initialize();
      console.log('Database initialized for LlamaService');
    } catch (error) {
      console.error('Error initializing database:', error);
    }
  }

  // Setup app state listener for auto offload/load
  private setupAppStateListener() {
    this.appStateSubscription = AppState.addEventListener(
      'change',
      this.handleAppStateChange
    );
  }

  // Handle app state changes for auto memory management
  private handleAppStateChange = (nextAppState: AppStateStatus) => {
    if (!this.config.autoOffloadEnabled) return;

    if (nextAppState === 'background') {
      console.log('App going to background, starting offload timer');
      this.startOffloadTimer();
    } else if (nextAppState === 'active') {
      console.log('App coming to foreground, cancelling offload timer');
      this.cancelOffloadTimer();
      // Auto-reload if model was previously loaded
      if (this.config.modelPath && !this.config.isModelLoaded) {
        this.initialize(this.config.modelPath);
      }
    }
  };

  // Start timer to offload model after inactivity
  private startOffloadTimer() {
    this.cancelOffloadTimer();
    // Offload after 5 minutes in background
    this.offloadTimer = setTimeout(() => {
      console.log('Auto-offloading model due to background state');
      this.offloadModel();
    }, 5 * 60 * 1000); // 5 minutes
  }

  // Stop current generation process
  stopGeneration(): void {
    console.log('🛑 STOP GENERATION CALLED');
    this.shouldStopGeneration = true;
    
    if (this.currentStreamingInterval) {
      console.log('🛑 Clearing streaming interval');
      clearInterval(this.currentStreamingInterval);
      this.currentStreamingInterval = null;
    }

    // Note: We can't directly abort the LLM completion, but the tokenCallback
    // will stop processing tokens and the response will be truncated
    if (this.currentCompletionPromise) {
      console.log('🛑 LLM completion in progress - will stop on next token');
    }
    
    console.log('🛑 Stop generation completed');
  }

  // Cancel offload timer
  private cancelOffloadTimer() {
    if (this.offloadTimer) {
      clearTimeout(this.offloadTimer);
      this.offloadTimer = null;
    }
  }

  // Offload model from memory
  async offloadModel(): Promise<void> {
    if (this.llamaContext) {
      console.log('Offloading model from memory');
      try {
        await this.llamaContext.release();
      } catch (releaseError) {
        console.warn('Context release method failed during offload, setting to null directly:', releaseError);
        // Fallback: Just set to null if release method is not available
      }
      this.llamaContext = null;
      this.config.isModelLoaded = false;
    }
  }

  // Get available models
  static getAvailableModels(): ModelInfo[] {
    return [...LlamaService.MODELS];
  }

  // Check if model file exists
  async isModelDownloaded(modelInfo?: ModelInfo): Promise<boolean> {
    const model = modelInfo || LlamaService.MODELS[0];
    const modelPath = `${RNFS.DocumentDirectoryPath}/${model.filename}`;
    return await RNFS.exists(modelPath);
  }

  // Get model file size
  async getModelFileSize(modelInfo?: ModelInfo): Promise<number> {
    const model = modelInfo || LlamaService.MODELS[0];
    const modelPath = `${RNFS.DocumentDirectoryPath}/${model.filename}`;
    
    try {
      if (await RNFS.exists(modelPath)) {
        const stat = await RNFS.stat(modelPath);
        return stat.size;
      }
    } catch (error) {
      console.error('Error getting model file size:', error);
    }
    return 0;
  }

  // Download model with progress tracking
  async downloadModel(
    progressCallback?: (progress: ModelDownloadProgress) => void,
    modelInfo?: ModelInfo
  ): Promise<boolean> {
    if (this.downloadInProgress) {
      console.log('Download already in progress');
      return false;
    }

    const model = modelInfo || LlamaService.MODELS[0];
    const modelPath = `${RNFS.DocumentDirectoryPath}/${model.filename}`;
    
    try {
      this.downloadInProgress = true;
      
      // Check if model already exists
      if (await RNFS.exists(modelPath)) {
        console.log('Model already exists');
        progressCallback?.({
          bytesWritten: model.size,
          contentLength: model.size,
          progress: 1.0
        });
        return true;
      }

      console.log(`Starting download of ${model.name} from ${model.url}`);

      // Create download job
      const downloadJob = RNFS.downloadFile({
        fromUrl: model.url,
        toFile: modelPath,
        progress: (res) => {
          const progress: ModelDownloadProgress = {
            bytesWritten: res.bytesWritten,
            contentLength: res.contentLength,
            progress: res.bytesWritten / res.contentLength
          };
          progressCallback?.(progress);
        },
        progressInterval: 1000, // Update every second
        cacheable: false,
      });

      const result = await downloadJob.promise;
      
      if (result.statusCode === 200) {
        console.log('Model downloaded successfully');
        
        // Verify file size
        const stat = await RNFS.stat(modelPath);
        const downloadedSize = stat.size;
        
        if (downloadedSize < model.size * 0.95) { // Allow 5% variance
          console.error('Downloaded file size is too small, removing incomplete file');
          await RNFS.unlink(modelPath);
          return false;
        }
        
        return true;
      } else {
        console.error('Download failed with status:', result.statusCode);
        return false;
      }
      
    } catch (error) {
      console.error('Error downloading model:', error);
      
      // Clean up partial download
      try {
        if (await RNFS.exists(modelPath)) {
          await RNFS.unlink(modelPath);
        }
      } catch (cleanupError) {
        console.error('Error cleaning up partial download:', cleanupError);
      }
      
      return false;
    } finally {
      this.downloadInProgress = false;
    }
  }

  // Delete model file
  async deleteModel(modelInfo?: ModelInfo): Promise<boolean> {
    const model = modelInfo || LlamaService.MODELS[0];
    const modelPath = `${RNFS.DocumentDirectoryPath}/${model.filename}`;
    
    try {
      // Offload from memory first
      await this.offloadModel();
      
      // Delete file
      if (await RNFS.exists(modelPath)) {
        await RNFS.unlink(modelPath);
        console.log('Model file deleted successfully');
        this.config.modelPath = undefined;
        return true;
      }
      return false;
    } catch (error) {
      console.error('Error deleting model:', error);
      return false;
    }
  }

  // Initialize Llama with the model
  async initialize(modelPath?: string): Promise<boolean> {
    try {
      if (this.llamaContext) {
        await this.cleanup();
      }

      // Use provided path or default model
      let defaultModelPath: string;
      if (modelPath) {
        defaultModelPath = modelPath;
      } else {
        const defaultModel = LlamaService.MODELS[0];
        defaultModelPath = `${RNFS.DocumentDirectoryPath}/${defaultModel.filename}`;
      }
      
      console.log('Initializing Llama with model:', defaultModelPath);
      
      // Check if model file exists
      const modelExists = await RNFS.exists(defaultModelPath);
      if (!modelExists) {
        console.log('Model file not found, will use fallback responses');
        this.config.isModelLoaded = false;
        this.config.modelPath = undefined;
        return false;
      }

      // Initialize Llama
      const contextParams: ContextParams = {
        model: defaultModelPath,
        n_ctx: 2048,
        n_gpu_layers: 0, // CPU-only for better compatibility
        n_threads: 4,
      };
      
      this.llamaContext = await initLlama(contextParams);

      this.config.isModelLoaded = true;
      this.config.modelPath = defaultModelPath;
      this.config.lastUsedTimestamp = Date.now();
      
      console.log('Llama initialized successfully');
      return true;
    } catch (error) {
      console.error('Failed to initialize Llama:', error);
      this.config.isModelLoaded = false;
      return false;
    }
  }

  // Check if model is ready
  isReady(): boolean {
    return this.config.isModelLoaded && this.llamaContext !== null;
  }

  // Generate health advice based on user input with streaming support
  async generateHealthAdvice(
    userInput: string, 
    onToken?: (token: string) => void,
    onComplete?: (fullResponse: string) => void
  ): Promise<string> {
    // Reset stop flag for new generation
    this.shouldStopGeneration = false;
    
    // Update last used timestamp
    this.config.lastUsedTimestamp = Date.now();
    
    // Initialize user context if this is first interaction
    await this.initializeUserContext();
    
    console.log('🔍 PROCESSING USER QUERY:', userInput);
    
    // Use enhanced intent classification to understand the query
    // Enhanced intent processing
    const enhancedIntentClassifier = new EnhancedIntentClassifier();
    const intent = await enhancedIntentClassifier.classifyIntent(userInput);
    console.log('🎯 CLASSIFIED INTENT:', intent);
    
    const healthQuery = await enhancedIntentClassifier.buildHealthDataQuery(intent);
    console.log('📅 HEALTH QUERY:', healthQuery);
    
    // Debug: Check database contents
    try {
      await databaseService.initialize();
      const dbContents = await databaseService.checkDatabaseContents();
      console.log('📊 CHECKING DATABASE BEFORE QUERY');
    } catch (error) {
      console.error('❌ Error checking database:', error);
    }
    
    const relevantHealthData = await enhancedIntentClassifier.fetchRelevantHealthData(healthQuery);
    console.log('💾 FETCHED HEALTH DATA:', relevantHealthData);
    
    // Track this interaction for learning
    await this.trackUserInteraction(userInput, intent.category);

    if (!this.isReady()) {
      console.log('🟠 AI model not ready - using database or fallback response');
      console.log('🟠 Model state:', {
        llamaContext: !!this.llamaContext,
        config: this.config
      });
      
      // Use enhanced database response based on intent and data
      const response = await this.generateEnhancedFallbackResponse(userInput, intent, relevantHealthData);
      if (onToken) {
        this.simulateStreamingResponse(response, onToken, onComplete);
      }
      return response;
    }

    try {
      console.log('🟢 Using real LLM for response generation');
      const healthPrompt = await this.buildEnhancedHealthPrompt(userInput, intent, relevantHealthData);
      let fullResponse = '';
      
      const completionParams: CompletionParams = {
        prompt: healthPrompt,
        n_predict: 512,
        temperature: 0.7,
        top_p: 0.9,
        top_k: 40,
        penalty_repeat: 1.1,
        stop: ['\n\nUser:', '\n\nHuman:', 'User:', 'Human:'],
      };      // Use streaming callback if provided
      const tokenCallback = onToken ? (data: any) => {
        // Check if generation should be stopped
        if (this.shouldStopGeneration) {
          console.log('🛑 LLM TOKEN CALLBACK - STOP REQUESTED');
          return;
        }
        
        if (data.token) {
          console.log('🔤 LLM TOKEN:', data.token);
          fullResponse += data.token;
          onToken(data.token);
        }
      } : undefined;

      console.log('🟢 Starting LLM completion...');
      console.log('🔍 PROMPT BEING SENT TO LLM:');
      console.log('================================');
      console.log(healthPrompt);
      console.log('================================');
      
      this.currentCompletionPromise = this.llamaContext!.completion(completionParams, tokenCallback);
      const response = await this.currentCompletionPromise;
      this.currentCompletionPromise = null;

      console.log('🤖 RAW LLM RESPONSE:');
      console.log('================================');
      console.log('Response object:', JSON.stringify(response, null, 2));
      console.log('Response text:', response.text);
      console.log('================================');

      // Check if we were stopped during completion
      if (this.shouldStopGeneration) {
        console.log('🛑 LLM completion was stopped');
        return 'Generation stopped by user.';
      }

      const processedResponse = this.processResponse(response.text);
      console.log('🔧 PROCESSED RESPONSE:');
      console.log('================================');
      console.log(processedResponse);
      console.log('================================');
      
      // If we weren't streaming, simulate it for consistent UX
      if (onToken && !tokenCallback) {
        this.simulateStreamingResponse(processedResponse, onToken, onComplete);
      } else if (onComplete) {
        onComplete(processedResponse);
      }

      return processedResponse;
    } catch (error) {
      console.error('Error generating response:', error);
      // Fallback to enhanced response with intent and data
      const fallbackResponse = await this.generateEnhancedFallbackResponse(userInput, intent, relevantHealthData);
      if (onToken) {
        this.simulateStreamingResponse(fallbackResponse, onToken, onComplete);
      }
      return fallbackResponse;
    }
  }

  // Generate response from pre-built prompt (used by QueryRouter)
  async generateResponse(
    prompt: string,
    context?: any,
    onToken?: (token: string) => void,
    onComplete?: (fullResponse: string) => void
  ): Promise<string> {
    console.log('🎯 GENERATE RESPONSE CALLED WITH PRE-BUILT PROMPT');
    
    // Reset stop flag for new generation
    this.shouldStopGeneration = false;
    
    // Update last used timestamp
    this.config.lastUsedTimestamp = Date.now();
    
    if (!this.isReady()) {
      console.log('🟠 AI model not ready for pre-built prompt - using fallback');
      const fallbackMsg = 'I apologize, but the AI model is not currently available. Please try again later.';
      if (onToken) {
        this.simulateStreamingResponse(fallbackMsg, onToken, onComplete);
      }
      return fallbackMsg;
    }

    try {
      console.log('🟢 Using real LLM for pre-built prompt');
      console.log('🔍 PRE-BUILT PROMPT:');
      console.log('================================');
      console.log(prompt);
      console.log('================================');
      
      let fullResponse = '';
      
      const completionParams: CompletionParams = {
        prompt: prompt,
        n_predict: 512,
        temperature: 0.7,
        top_p: 0.9,
        top_k: 40,
        penalty_repeat: 1.1,
        stop: ['\n\nUser:', '\n\nHuman:', 'User:', 'Human:'],
      };

      // Use streaming callback if provided
      const tokenCallback = onToken ? (data: any) => {
        if (this.shouldStopGeneration) {
          console.log('🛑 PRE-BUILT PROMPT TOKEN CALLBACK - STOP REQUESTED');
          return;
        }
        
        if (data.token) {
          console.log('🔤 PRE-BUILT PROMPT TOKEN:', data.token);
          fullResponse += data.token;
          onToken(data.token);
        }
      } : undefined;

      this.currentCompletionPromise = this.llamaContext!.completion(completionParams, tokenCallback);
      const response = await this.currentCompletionPromise;
      this.currentCompletionPromise = null;

      console.log('🤖 PRE-BUILT PROMPT RAW LLM RESPONSE:');
      console.log('================================');
      console.log('Response text:', response.text);
      console.log('================================');

      if (this.shouldStopGeneration) {
        console.log('🛑 Pre-built prompt completion was stopped');
        return 'Generation stopped by user.';
      }

      const processedResponse = this.processResponse(response.text);
      console.log('🔧 PRE-BUILT PROMPT PROCESSED RESPONSE:');
      console.log('================================');
      console.log(processedResponse);
      console.log('================================');
      
      if (onToken && !tokenCallback) {
        this.simulateStreamingResponse(processedResponse, onToken, onComplete);
      } else if (onComplete) {
        onComplete(processedResponse);
      }

      return processedResponse;
    } catch (error) {
      console.error('Error with pre-built prompt:', error);
      const errorMsg = 'Sorry, I encountered an error while processing your request. Please try again.';
      if (onToken) {
        this.simulateStreamingResponse(errorMsg, onToken, onComplete);
      }
      return errorMsg;
    }
  }

  // Simulate streaming for fallback responses or when model doesn't support streaming
  private simulateStreamingResponse(
    text: string, 
    onToken: (token: string) => void, 
    onComplete?: (fullResponse: string) => void
  ) {
    const words = text.split(' ');
    let currentIndex = 0;
    this.shouldStopGeneration = false; // Reset stop flag

    this.currentStreamingInterval = setInterval(() => {
      // Check if generation should be stopped
      if (this.shouldStopGeneration) {
        console.log('🛑 STREAMING STOPPED BY USER');
        clearInterval(this.currentStreamingInterval!);
        this.currentStreamingInterval = null;
        console.log('🛑 Streaming stopped by user');
        return;
      }

      if (currentIndex < words.length) {
        const token = currentIndex === 0 ? words[currentIndex] : ' ' + words[currentIndex];
        onToken(token);
        currentIndex++;
      } else {
        clearInterval(this.currentStreamingInterval!);
        this.currentStreamingInterval = null;
        onComplete?.(text);
      }
    }, 50 + Math.random() * 100); // Variable delay for realistic typing effect
  }

  // Generate fallback responses when AI model is not available
  private async generateFallbackResponse(userInput: string): Promise<string> {
    console.log('🟡 GENERATING FALLBACK RESPONSE for:', userInput);
    
    try {
      // Try to get response from database first
      const databaseResponse = await responseGenerator.generateResponse(userInput);
      if (databaseResponse && !databaseResponse.includes('data not available')) {
        return databaseResponse;
      }
    } catch (error) {
      console.error('Error getting database response for fallback:', error);
    }

    // If database fails, provide minimal generic responses
    const input = userInput.toLowerCase();
    
    if (input.includes('heart rate') || input.includes('hr') || input.includes('bpm')) {
      return "I can help you understand heart rate patterns and trends based on your health data. Please ensure your health metrics are being tracked for personalized insights.";
    }
    
    if (input.includes('sleep')) {
      return "Sleep quality is crucial for your health and recovery. I can analyze your sleep patterns, duration, and efficiency when your sleep data is available. Make sure your sleep tracking is enabled for detailed insights.";
    }
    
    if (input.includes('stress') || input.includes('anxiety')) {
      return "Stress management is important for overall wellness. I can provide personalized stress insights based on your heart rate variability and other health metrics. Ensure your stress tracking is active for detailed analysis.";
    }
    
    if (input.includes('activity') || input.includes('exercise') || input.includes('steps')) {
      return "Physical activity is essential for maintaining good health. I can analyze your activity patterns, steps, and calorie burn when your fitness data is being tracked. Enable activity tracking for comprehensive insights.";
    }
    
    if (input.includes('diet') || input.includes('nutrition')) {
      return "Nutrition plays a vital role in your health and wellness. I can provide personalized dietary recommendations based on your health metrics and goals. Ensure your health data is being tracked for tailored advice.";
    }
    
    // Generic health response
    return "I'm here to provide personalized health insights based on your data. I can help with sleep analysis, heart rate trends, stress management, activity tracking, and nutrition guidance. Please ensure your health metrics are being tracked for the most accurate and helpful responses.";
  }

  // Build comprehensive health prompt with context, memory, and session awareness
  private async buildHealthPrompt(userInput: string, databaseResponse?: string | null): Promise<string> {
    let healthContext = '';
    let userContext = '';
    
    // Clean up user input - extract just the actual question
    let actualUserQuery = userInput;
    
    // If userInput contains system prompt, extract the actual user question
    if (userInput.includes('A user has asked:')) {
      const match = userInput.match(/A user has asked: "([^"]+)"/);
      if (match) {
        actualUserQuery = match[1];
        console.log('🧹 EXTRACTED ACTUAL USER QUERY:', actualUserQuery);
      }
    }
    
    try {
      // Get health metrics from database
      const healthMetrics = await responseGenerator.getHealthMetrics();
      
      if (healthMetrics) {
        healthContext = `\n\nYour Recent Health Data:
- Heart Rate: ${healthMetrics.heartRate.average} BPM average (Resting: ${healthMetrics.heartRate.resting} BPM)
- Sleep: ${Math.floor(healthMetrics.sleep.duration / 60)}h ${healthMetrics.sleep.duration % 60}m average (${healthMetrics.sleep.score}/100 quality)
- Activity: ${healthMetrics.activity.steps} steps daily average
- Stress Level: ${healthMetrics.stress.level}/100
- Recovery Score: ${healthMetrics.stress.recoveryScore}/100`;
      } else {
        // Fallback if no health data is available
        healthContext = `\n\nYour Health Data: Currently gathering your health metrics. Please ensure your health tracking is enabled for personalized insights.`;
      }

      // If we have a database response, include it as context
      if (databaseResponse && !databaseResponse.includes('data not available')) {
        healthContext += `\n\nRelevant Information: ${databaseResponse}`;
      }

      // Get basic user preferences (simplified)
      const userProfile = await healthDataManager.getUserContext();
      if (userProfile) {
        const focusAreas = userProfile.preferences.focusAreas || [];
        if (focusAreas.length > 0) {
          userContext = `\n\nYour Focus Areas: ${focusAreas.join(', ')}`;
        }
      }
    } catch (error) {
      console.error('Error fetching health context:', error);
      healthContext = `\n\nHealth Data: Unable to retrieve current health metrics. Please ensure your health tracking is active.`;
    }

    // Build a clean, simple prompt
    const finalPrompt = `${this.config.context}${healthContext}${userContext}

Question: ${actualUserQuery}

Analyze the specific data above and provide a focused response about what the user asked.

Noise AI: `;

    console.log('🔄 SIMPLIFIED PROMPT LENGTH:', finalPrompt.length);
    console.log('🔄 HEALTH CONTEXT LENGTH:', healthContext.length);
    return finalPrompt;
  }

  // Process and clean the AI response
  private processResponse(response: string): string {
    // Clean up the response
    let cleaned = response.trim();
    
    // Remove any prompt echoing
    cleaned = cleaned.replace(/^(Noise AI:|AI:)/i, '').trim();
    
    // Remove common LLM end tokens and artifacts
    cleaned = cleaned.replace(/<\|eot_id\|>/g, '');
    cleaned = cleaned.replace(/<\|end_of_text\|>/g, '');
    cleaned = cleaned.replace(/<\|endoftext\|>/g, '');
    cleaned = cleaned.replace(/\[END\]/g, '');
    cleaned = cleaned.replace(/\[DONE\]/g, '');
    cleaned = cleaned.replace(/<\/s>/g, '');
    
    // Remove duplicate responses (split by "Noise AI:" and take first)
    if (cleaned.includes('Noise AI:')) {
      cleaned = cleaned.split('Noise AI:')[0].trim();
    }
    
    // Remove any trailing incomplete sentences
    const sentences = cleaned.split(/[.!?]/);
    if (sentences.length > 1 && sentences[sentences.length - 1].trim().length < 10) {
      sentences.pop(); // Remove incomplete last sentence
      cleaned = sentences.join('.') + '.';
    }
    
    // Clean up any leftover formatting and whitespace
    cleaned = cleaned.trim();
    
    // Ensure it's not too long but preserve complete thoughts
    if (cleaned.length > 400) {
      const cutoff = cleaned.lastIndexOf('.', 400);
      if (cutoff > 200) {
        cleaned = cleaned.substring(0, cutoff + 1);
      } else {
        cleaned = cleaned.substring(0, 400) + '...';
      }
    }

    return cleaned || 'I apologize, but I couldn\'t generate a proper response. Could you please rephrase your question?';
  }

  // Enhanced prompt builder with intent-based data
  private async buildEnhancedHealthPrompt(userInput: string, intent: any, relevantHealthData: any): Promise<string> {
    let healthContext = '';
    let userContext = '';
    
    try {
      // Build context based on actual fetched data
      if (relevantHealthData && relevantHealthData.primary) {
        healthContext = this.buildHealthContextFromData(relevantHealthData, intent);
      } else {
        // Fallback to general metrics if no specific data found
        const generalMetrics = await responseGenerator.getHealthMetrics();
        if (generalMetrics) {
          healthContext = this.buildGeneralHealthContext(generalMetrics);
        } else {
          healthContext = `\n\nUser Health Data: Currently gathering health metrics. Please use the Health Data Generator in the AI Model Configurator to create health data for personalized insights.`;
        }
      }

      // Get user preferences
      const userProfile = await healthDataManager.getUserContext();
      if (userProfile) {
        const focusAreas = userProfile.preferences.focusAreas || [];
        if (focusAreas.length > 0) {
          userContext = `\n\nUser Focus Areas: ${focusAreas.join(', ')}`;
        }
      }
    } catch (error) {
      console.error('Error building enhanced health context:', error);
      healthContext = `\n\nUser Health Data: Unable to retrieve current health metrics. Please ensure health tracking is active.`;
    }

    const finalPrompt = `${this.config.context}${healthContext}${userContext}

Question: ${userInput}

Analyze the specific data above and provide a focused response about what the user asked.

Noise AI: `;

    console.log('🔄 ENHANCED PROMPT LENGTH:', finalPrompt.length);
    console.log('🎯 INTENT:', intent);
    console.log('📊 DATA SUMMARY:', relevantHealthData?.primary ? 'Real data available' : 'No specific data');
    return finalPrompt;
  }

  // Generate enhanced fallback response with actual data
  private async generateEnhancedFallbackResponse(userInput: string, intent: any, relevantHealthData: any): Promise<string> {
    try {
      if (relevantHealthData && relevantHealthData.primary) {
        return this.buildDataDrivenResponse(userInput, intent, relevantHealthData);
      }
      
      // Try template-based response
      const templateResponse = await responseGenerator.generateResponse(userInput);
      if (templateResponse && !templateResponse.includes('data not available')) {
        return templateResponse;
      }
      
      // Final fallback with guidance
      if (!relevantHealthData || !relevantHealthData.primary) {
        return `I understand you're asking about your ${intent.category} ${intent.timeframe}. To provide personalized insights, I need health data first. 

Please use the Health Data Generator in the AI Model Configurator to:
• Generate realistic sample data for testing
• Import your own health data
• Enable data tracking

Once you have health data, I'll be able to provide detailed analysis of your ${intent.category} patterns, trends, and personalized recommendations.`;
      }
      
      return this.generateBasicFallbackResponse(userInput, intent);
    } catch (error) {
      console.error('Error generating enhanced fallback:', error);
      return `I understand you're asking about ${intent.category}. While I don't have specific data available right now, I recommend consulting with a healthcare professional for personalized advice about your ${intent.category} health.`;
    }
  }

  // Build health context from actual database data
  private buildHealthContextFromData(relevantHealthData: any, intent: any): string {
    if (!relevantHealthData || !relevantHealthData.primary) {
      return '\n\nUser Health Data: No data available for the requested timeframe. Please use the Health Data Generator in the AI Model Configurator to create health data first.';
    }

    const data = relevantHealthData.primary;
    let context = '\n\nUser Health Data:';

    // Handle case where no specific data is available
    if (data.message) {
      return `\n\nUser Health Data: ${data.message}. You can generate health data using the Health Data Generator in the AI Model Configurator.`;
    }

    switch (intent.category) {
      case 'heart_rate':
        if (data.heartRate && typeof data.heartRate === 'object') {
          context += `\n- Heart Rate: ${data.heartRate.average || 'N/A'} BPM average`;
          if (data.heartRate.min && data.heartRate.max) {
            context += ` (Range: ${data.heartRate.min}-${data.heartRate.max} BPM)`;
          }
          if (data.heartRate.recordCount) {
            context += `\n- Records: ${data.heartRate.recordCount} measurements`;
          }
          if (data.heartRate.types && Array.isArray(data.heartRate.types)) {
            context += `\n- Types tracked: ${data.heartRate.types.join(', ')}`;
          }
        }
        break;
      case 'sleep':
        if (data.sleep && typeof data.sleep === 'object') {
          if (data.sleep.averageDuration) {
            const hours = Math.floor(data.sleep.averageDuration / 60);
            const minutes = data.sleep.averageDuration % 60;
            context += `\n- Sleep: ${hours}h ${minutes}m average duration`;
          }
          if (data.sleep.averageQuality) {
            context += `\n- Quality: ${data.sleep.averageQuality}/100 average`;
          }
          if (data.sleep.averageEfficiency) {
            context += `\n- Efficiency: ${data.sleep.averageEfficiency}% average`;
          }
          if (data.sleep.nightsTracked) {
            context += `\n- Nights tracked: ${data.sleep.nightsTracked}`;
          }
          if (data.sleep.types && Array.isArray(data.sleep.types)) {
            context += `\n- Data types: ${data.sleep.types.join(', ')}`;
          }
        }
        break;
      case 'activity':
        if (data.activity && typeof data.activity === 'object') {
          if (data.activity.averageSteps) {
            context += `\n- Activity: ${data.activity.averageSteps} steps daily average`;
          }
          if (data.activity.averageCalories) {
            context += `\n- Calories: ${data.activity.averageCalories} calories burned average`;
          }
          if (data.activity.daysTracked) {
            context += `\n- Days tracked: ${data.activity.daysTracked}`;
          }
        }
        break;
      case 'stress':
        if (data.stress && typeof data.stress === 'object') {
          if (data.stress.averageLevel) {
            context += `\n- Stress Level: ${data.stress.averageLevel}/100 average`;
          }
          if (data.stress.min && data.stress.max) {
            context += `\n- Range: ${data.stress.min}-${data.stress.max}/100`;
          }
          if (data.stress.recordCount) {
            context += `\n- Records: ${data.stress.recordCount} measurements`;
          }
        }
        break;
      default:
        context += `\n- Category: ${intent.category}`;
        context += `\n- Records available: ${data.recordCount || 0}`;
    }

    // Add timeframe information safely
    if (relevantHealthData.timeframe && relevantHealthData.timeframe.start && relevantHealthData.timeframe.end) {
      context += `\n- Time period: ${relevantHealthData.timeframe.start} to ${relevantHealthData.timeframe.end}`;
    } else if (intent.timeframe) {
      context += `\n- Time period: ${intent.timeframe}`;
    }

    // Add comparison data if available
    if (relevantHealthData.comparison) {
      context += '\n\nComparison Data (Previous Period):';
      const compData = relevantHealthData.comparison;
      
      switch (intent.category) {
        case 'sleep':
          if (compData.sleep && typeof compData.sleep === 'object') {
            if (compData.sleep.averageDuration) {
              const hours = Math.floor(compData.sleep.averageDuration / 60);
              const minutes = compData.sleep.averageDuration % 60;
              context += `\n- Sleep: ${hours}h ${minutes}m average duration`;
            }
            if (compData.sleep.averageQuality) {
              context += `\n- Quality: ${compData.sleep.averageQuality}/100 average`;
            }
            if (compData.sleep.averageEfficiency) {
              context += `\n- Efficiency: ${compData.sleep.averageEfficiency}% average`;
            }
            if (compData.sleep.nightsTracked) {
              context += `\n- Nights tracked: ${compData.sleep.nightsTracked}`;
            }
          }
          break;
        case 'heart_rate':
          if (compData.heartRate && typeof compData.heartRate === 'object') {
            context += `\n- Heart Rate: ${compData.heartRate.average || 'N/A'} BPM average`;
            if (compData.heartRate.min && compData.heartRate.max) {
              context += ` (Range: ${compData.heartRate.min}-${compData.heartRate.max} BPM)`;
            }
          }
          break;
        case 'activity':
          if (compData.activity && typeof compData.activity === 'object') {
            if (compData.activity.averageSteps) {
              context += `\n- Activity: ${compData.activity.averageSteps} steps daily average`;
            }
            if (compData.activity.averageCalories) {
              context += `\n- Calories: ${compData.activity.averageCalories} calories burned average`;
            }
          }
          break;
      }
    }

    return context;
  }

  // Build general health context when no specific data is available
  private buildGeneralHealthContext(metrics: any): string {
    return `\n\nUser Health Data:
- Heart Rate: ${metrics.heartRate.average} BPM average (Resting: ${metrics.heartRate.resting} BPM)
- Sleep: ${Math.floor(metrics.sleep.duration / 60)}h ${metrics.sleep.duration % 60}m average (${metrics.sleep.score}/100 quality)
- Activity: ${metrics.activity.steps} steps daily average
- Stress Level: ${metrics.stress.level}/100
- Recovery Score: ${metrics.stress.recoveryScore}/100`;
  }

  // Build response using actual data
  private buildDataDrivenResponse(userInput: string, intent: any, relevantHealthData: any): string {
    const data = relevantHealthData.primary;
    
    if (!data || data.message) {
      return `I don't have ${intent.category} data available for the requested timeframe (${intent.timeframe}). Please ensure your health tracking is active and try asking about a different time period.`;
    }

    let response = `Based on your ${intent.category} data for ${intent.timeframe}:\n\n`;

    switch (intent.category) {
      case 'heart_rate':
        if (data.heartRate) {
          response += `Your heart rate averaged ${data.heartRate.average} BPM, ranging from ${data.heartRate.min} to ${data.heartRate.max} BPM across ${data.heartRate.recordCount} measurements. `;
          if (data.heartRate.average >= 60 && data.heartRate.average <= 100) {
            response += 'This is within the normal resting heart rate range. ';
          }
          response += 'Continue monitoring your heart rate trends and consult a healthcare professional if you notice any concerning patterns.';
        }
        break;
      case 'sleep':
        if (data.sleep) {
          const hours = Math.floor(data.sleep.averageDuration / 60);
          const minutes = data.sleep.averageDuration % 60;
          response += `You averaged ${hours} hours and ${minutes} minutes of sleep with a quality score of ${data.sleep.averageQuality}/100 across ${data.sleep.nightsTracked} nights. `;
          if (data.sleep.averageDuration >= 420) { // 7 hours
            response += 'You\'re meeting the recommended sleep duration. ';
          } else {
            response += 'Consider aiming for 7-9 hours of sleep for optimal health. ';
          }
          response += 'Focus on maintaining consistent sleep schedules and creating a relaxing bedtime routine.';
        }
        break;
      case 'activity':
        if (data.activity) {
          response += `You averaged ${data.activity.averageSteps} steps daily and burned approximately ${data.activity.averageCalories} calories across ${data.activity.daysTracked} days. `;
          if (data.activity.averageSteps >= 8000) {
            response += 'Great job staying active! ';
          } else {
            response += 'Consider gradually increasing your daily activity. ';
          }
          response += 'Regular physical activity supports overall cardiovascular health and well-being.';
        }
        break;
      case 'stress':
        if (data.stress) {
          response += `Your stress levels averaged ${data.stress.averageLevel}/100, ranging from ${data.stress.min} to ${data.stress.max} across ${data.stress.recordCount} measurements. `;
          if (data.stress.averageLevel <= 40) {
            response += 'Your stress levels appear to be well-managed. ';
          } else {
            response += 'Consider stress management techniques like meditation, exercise, or deep breathing. ';
          }
          response += 'Continue monitoring your stress and seek support if levels remain consistently high.';
        }
        break;
      default:
        response += `I found ${data.recordCount} records for this timeframe. `;
        response += 'For more specific insights, please ask about particular aspects of your health data.';
    }

    return response;
  }

  // Basic fallback when no data is available
  private generateBasicFallbackResponse(userInput: string, intent: any): string {
    const categoryAdvice: { [key: string]: string } = {
      heart_rate: 'Monitor your heart rate regularly and maintain cardiovascular health through regular exercise and a balanced diet.',
      sleep: 'Aim for 7-9 hours of quality sleep each night. Establish a consistent sleep schedule and create a relaxing bedtime routine.',
      activity: 'Try to get at least 150 minutes of moderate-intensity exercise per week, including both cardio and strength training.',
      stress: 'Practice stress management techniques like meditation, deep breathing, or regular exercise. Consider talking to a healthcare professional if stress becomes overwhelming.',
      nutrition: 'Focus on a balanced diet with plenty of fruits, vegetables, lean proteins, and whole grains. Stay hydrated throughout the day.',
      general: 'Maintain a healthy lifestyle with regular exercise, balanced nutrition, adequate sleep, and stress management.'
    };

    const advice = categoryAdvice[intent.category] || categoryAdvice.general;
    return `I understand you're asking about your ${intent.category}. ${advice} For personalized advice based on your specific health data, please ensure your health tracking is active and try asking again later.`;
  }

  // Track user behavior and learning patterns with enhanced memory integration
  async trackUserInteraction(userInput: string, responseType: string = 'general'): Promise<void> {
    try {
      const timestamp = Date.now();
      
      // Analyze input for memory storage
      const isHealthQuery = userInput.toLowerCase().match(/health|heart|sleep|stress|calories|fitness/);
      const isGoalQuery = userInput.toLowerCase().match(/goal|target|progress|plan/);
      const isPersonalInfo = userInput.toLowerCase().match(/my|i am|i have|i feel|i want/);
      
      // Store relevant memories
      if (isHealthQuery && isPersonalInfo) {
        await memoryStore.addHealthPreference(
          userInput,
          { responseType, timestamp, sessionId: sessionStore.activeSessionId }
        );
      }
      
      if (isGoalQuery && isPersonalInfo) {
        await memoryStore.addGoalSetting(
          userInput,
          { responseType, timestamp, sessionId: sessionStore.activeSessionId }
        );
      }
      
      // Track interaction patterns
      const patternInfo = `User typically asks ${responseType} questions`;
      await memoryStore.addInteractionPattern(
        patternInfo,
        { 
          responseType, 
          timestamp, 
          userInputLength: userInput.length,
          sessionId: sessionStore.activeSessionId 
        }
      );
      
      // Update conversation context
      const currentContext = memoryStore.getConversationContext();
      const recentTopics = currentContext?.recentTopics || [];
      recentTopics.unshift(responseType);
      
      await memoryStore.updateConversationContext({
        sessionId: sessionStore.activeSessionId || 'unknown',
        currentFocus: responseType as any,
        recentTopics: recentTopics.slice(0, 5), // Keep last 5 topics
      });

      // Store interaction in AsyncStorage for persistent learning
      const interactions = await AsyncStorage.getItem('user_interactions');
      let interactionHistory = [];
      if (interactions) {
        interactionHistory = JSON.parse(interactions);
      }

      interactionHistory.push({
        input: userInput.substring(0, 100), // Limit storage size
        type: responseType,
        timestamp,
        sessionId: sessionStore.activeSessionId,
      });

      // Keep only last 100 interactions
      if (interactionHistory.length > 100) {
        interactionHistory = interactionHistory.slice(-100);
      }

      await AsyncStorage.setItem('user_interactions', JSON.stringify(interactionHistory));

      // Continue with original health data context updates
      const context = await healthDataManager.getUserContext();
      const currentTime = new Date();
      const timeSlot = `${currentTime.getHours()}:00`;
      
      if (context) {
        const updatedBehaviorPatterns = {
          ...context.behaviorPatterns,
          commonQueries: [
            ...context.behaviorPatterns.commonQueries.slice(-9), // Keep last 10
            userInput.toLowerCase()
          ],
          queryTimes: {
            ...context.behaviorPatterns.queryTimes,
            [responseType]: [
              ...(context.behaviorPatterns.queryTimes[responseType] || []).slice(-4), // Keep last 5
              timeSlot
            ]
          },
          lastInteractionDate: currentTime
        };

        await healthDataManager.updateUserContext({
          behaviorPatterns: updatedBehaviorPatterns
        });
      }
    } catch (error) {
      console.error('Error tracking user interaction:', error);
    }
  }

  // Update user preferences based on usage patterns
  async adaptUserPreferences(feedback?: 'too_brief' | 'too_detailed' | 'just_right'): Promise<void> {
    try {
      const context = await healthDataManager.getUserContext();
      if (!context) return;

      let updatedPreferences = { ...context.preferences };

      // Adapt response length based on feedback
      if (feedback === 'too_brief' && context.preferences.responseLength === 'brief') {
        updatedPreferences.responseLength = 'moderate';
      } else if (feedback === 'too_brief' && context.preferences.responseLength === 'moderate') {
        updatedPreferences.responseLength = 'detailed';
      } else if (feedback === 'too_detailed' && context.preferences.responseLength === 'detailed') {
        updatedPreferences.responseLength = 'moderate';
      } else if (feedback === 'too_detailed' && context.preferences.responseLength === 'moderate') {
        updatedPreferences.responseLength = 'brief';
      }

      // Analyze query patterns to suggest focus areas
      const commonQueries = context.behaviorPatterns.commonQueries;
      const sleepQueries = commonQueries.filter(q => 
        q.includes('sleep') || q.includes('rest') || q.includes('tired')
      ).length;
      const fitnessQueries = commonQueries.filter(q => 
        q.includes('exercise') || q.includes('workout') || q.includes('fitness') || q.includes('calories')
      ).length;
      const stressQueries = commonQueries.filter(q => 
        q.includes('stress') || q.includes('anxiety') || q.includes('mood')
      ).length;

      // Auto-suggest focus areas based on query frequency
      const suggestedFocusAreas = [];
      if (sleepQueries >= 3) suggestedFocusAreas.push('sleep');
      if (fitnessQueries >= 3) suggestedFocusAreas.push('fitness');
      if (stressQueries >= 3) suggestedFocusAreas.push('stress');

      if (suggestedFocusAreas.length > 0) {
        const uniqueFocusAreas = Array.from(new Set([
          ...context.preferences.focusAreas,
          ...suggestedFocusAreas
        ]));
        updatedPreferences.focusAreas = uniqueFocusAreas;
      }

      await healthDataManager.updateUserContext({
        preferences: updatedPreferences
      });
    } catch (error) {
      console.error('Error adapting user preferences:', error);
    }
  }

  // Initialize default user context if none exists
  async initializeUserContext(): Promise<void> {
    try {
      const existingContext = await healthDataManager.getUserContext();
      if (!existingContext) {
        await healthDataManager.updateUserContext({
          preferences: {
            responseLength: 'moderate',
            focusAreas: ['sleep', 'fitness'],
            timePreferences: {
              wakeTime: '07:00',
              bedTime: '23:00',
              workoutTime: '18:00'
            },
            sleepGoal: 8,
            exerciseGoal: 30,
            calorieGoal: 2000,
            stressManagement: 'moderate'
          },
          behaviorPatterns: {
            commonQueries: [],
            queryTimes: {},
            engagementLevel: 'moderate',
            lastInteractionDate: new Date()
          },
          lifestyle: {
            dietType: 'balanced',
            activityLevel: 'moderate',
            sleepSchedule: 'regular',
            workSchedule: 'standard'
          },
          healthHistory: {
            chronicConditions: [],
            allergies: [],
            medications: [],
            injuries: []
          }
        });
        console.log('Initialized default user context');
      }
    } catch (error) {
      console.error('Error initializing user context:', error);
    }
  }

  // Cleanup resources
  async cleanup(): Promise<void> {
    try {
      // Cancel any pending timers
      this.cancelOffloadTimer();
      
      // Remove app state listener
      if (this.appStateSubscription) {
        this.appStateSubscription.remove();
        this.appStateSubscription = null;
      }
      
      if (this.llamaContext) {
        try {
          await this.llamaContext.release();
        } catch (releaseError) {
          console.warn('Context release method failed, setting to null directly:', releaseError);
          // Fallback: Just set to null if release method is not available
        }
        this.llamaContext = null;
      }
      this.config.isModelLoaded = false;
      console.log('Llama cleanup completed');
    } catch (error) {
      console.error('Error during cleanup:', error);
    }
  }

  // Auto offload settings
  setAutoOffloadEnabled(enabled: boolean) {
    this.config.autoOffloadEnabled = enabled;
    if (!enabled) {
      this.cancelOffloadTimer();
    }
  }

  getAutoOffloadEnabled(): boolean {
    return this.config.autoOffloadEnabled;
  }

  // Get model status
  getStatus(): { isLoaded: boolean; modelPath?: string } {
    return {
      isLoaded: this.config.isModelLoaded,
      modelPath: this.config.modelPath,
    };
  }
}

// Export singleton instance
export const llamaService = new LlamaService();
export default LlamaService;
export type { ModelInfo, ModelDownloadProgress };
