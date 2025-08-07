import { intentClassifier, IntentResult } from './IntentClassifier';
import EnhancedIntentClassifier from './EnhancedIntentClassifier';
import { deviceController } from './DeviceController';
import { dailyAssistant } from './DailyAssistant';
import healthDataManager from './HealthDataManager';
import { sessionStore, memoryStore, UserMemory, ConversationContext } from './src/stores';

interface HealthMetrics {
  sleep_score?: number;
  heart_rate?: number;
  steps?: number;
  stress_level?: number;
  calories_burned?: number;
  timeframe?: string;
  raw_data?: any;
  enhanced_data?: any; // For storing complete database results
}

interface LLMService {
  generateResponse: (
    prompt: string, 
    context?: any, 
    onToken?: (token: string) => void,
    onComplete?: () => void
  ) => Promise<string>;
}

class EnhancedQueryRouter {
  private llamaService: LLMService | null = null;

  constructor() {
    // LlamaService will be injected when available
  }

  setLlamaService(service: LLMService) {
    this.llamaService = service;
  }
  
  async processQuery(
    userQuery: string,
    onToken?: (token: string) => void,
    onComplete?: () => void
  ): Promise<string> {
    console.log('🔍 Enhanced processing query:', userQuery);
    
    // Step 1: Get conversation context and user memory
    const conversationContext = memoryStore.getConversationContext();
    const relevantMemories = await memoryStore.getRelevantMemories(userQuery, undefined, 5);
    console.log('🧠 Retrieved conversation context:', conversationContext);
    console.log('💭 Retrieved relevant memories:', relevantMemories.length);
    
    // Step 2: Advanced Intent Classification
    const intent = intentClassifier.classifyIntent(userQuery);
    console.log('📊 Enhanced intent classified:', intent);
    
    // Step 3: Route based on advanced intent analysis with context
    switch (intent.routing) {
      case 'device_action':
        return await this.handleDeviceAction(userQuery, intent, conversationContext);
      
      case 'local_db':
        return await this.handleLocalDataQuery(userQuery, intent, conversationContext, relevantMemories);
      
      case 'llm_health_coach':
        return await this.handleAdvancedHealthCoaching(userQuery, intent, conversationContext, relevantMemories, onToken, onComplete);
      
      case 'external_api':
        return await this.handleExternalApiQuery(userQuery, intent, conversationContext);
      
      case 'llm':
        return await this.handleAdvancedLLMQuery(userQuery, intent, conversationContext, relevantMemories, onToken, onComplete);
      
      default:
        return await this.handleFallbackQuery(userQuery, intent, conversationContext, relevantMemories, onToken, onComplete);
    }
  }

  private async handleDeviceAction(
    query: string, 
    intent: IntentResult, 
    conversationContext: any
  ): Promise<string> {
    console.log('🔧 Enhanced device action handling with context');
    
    try {
      if (intent.intent.includes('alarm')) {
        return await deviceController.processNaturalLanguageCommand(query);
      } 
      
      if (intent.intent.includes('timer')) {
        const timerMatch = query.match(/(\d+)\s*(minute|min|hour|hr)/);
        if (timerMatch) {
          const duration = parseInt(timerMatch[1]);
          const unit = timerMatch[2];
          const minutes = unit.includes('hour') || unit.includes('hr') ? duration * 60 : duration;
          await dailyAssistant.createTimer(`Timer for ${duration} ${unit}`, minutes);
          
          // Update memory with timer preference
          await memoryStore.addMemory({
            type: 'interaction_pattern',
            content: `User frequently sets ${duration} ${unit} timers`,
            context: { timerDuration: minutes, timerType: unit },
            confidence: 0.8,
            tags: ['timer', 'productivity']
          });
          
          return `⏲️ Timer set for ${duration} ${unit}. I'll notify you when it's done!`;
        }
      }
      
      return "I can help with alarms, timers, and reminders. What would you like me to set up?";
      
    } catch (error) {
      console.error('Enhanced device action error:', error);
      return "I had trouble with that device command. Please try again.";
    }
  }

  private async handleLocalDataQuery(
    query: string, 
    intent: IntentResult,
    conversationContext: any,
    relevantMemories: any[]
  ): Promise<string> {
    console.log('📊 Enhanced local data query handling with context and memory');
    
    try {
      const metrics = await this.fetchEnhancedHealthMetrics(intent.parameters);
      
      // Check for user preferences in memory
      const healthPreferences = relevantMemories.filter(m => m.type === 'health_preference');
      console.log('💡 Found health preferences:', healthPreferences.length);
      
      const response = this.formatStructuredHealthResponse(metrics, intent, query);
      
      // Store health query pattern for future personalization
      await memoryStore.addMemory({
        type: 'interaction_pattern',
        content: `User regularly asks about ${intent.parameters.metric || 'health metrics'}`,
        context: { metric: intent.parameters.metric, query_type: intent.intent },
        confidence: 0.7,
        tags: ['health_query', intent.parameters.metric || 'general']
      });
      
      return response;
      
    } catch (error) {
      console.error('Enhanced local data query error:', error);
      return "I had trouble accessing your health data. Please try again.";
    }
  }

  private async handleAdvancedHealthCoaching(
    query: string, 
    intent: IntentResult,
    conversationContext: any,
    relevantMemories: any[],
    onToken?: (token: string) => void,
    onComplete?: () => void
  ): Promise<string> {
    console.log('🏥 Advanced health coaching with LLM, context, and memory');
    
    try {
      const metrics = await this.fetchEnhancedHealthMetrics(intent.parameters);
      const coachingPrompt = this.buildHealthCoachingPrompt(query, metrics, intent, conversationContext, relevantMemories);
      
      if (this.llamaService) {
        const response = await this.llamaService.generateResponse(
          coachingPrompt,
          { metrics, intent, conversationContext, relevantMemories },
          onToken,
          onComplete
        );
        
        // Store health coaching interaction
        await memoryStore.addMemory({
          type: 'health_preference',
          content: `User seeks coaching on ${intent.parameters.metric || 'health topics'}`,
          context: { coaching_area: intent.parameters.metric, query, response_type: 'llm_coaching' },
          confidence: 0.9,
          tags: ['health_coaching', intent.parameters.metric || 'general']
        });
        
        return response;
      } else {
        return this.generateStructuredHealthCoachResponse(query, metrics, intent, relevantMemories);
      }
      
    } catch (error) {
      console.error('Advanced health coaching error:', error);
      return "I had trouble analyzing your health data. Please try again.";
    }
  }

  private async handleExternalApiQuery(
    query: string, 
    intent: IntentResult,
    conversationContext: any
  ): Promise<string> {
    console.log('🌐 Enhanced external API query handling with context');
    
    if (intent.intent === 'weather_info') {
      try {
        const weather = await dailyAssistant.getWeatherInfo();
        
        // Store weather preference
        await memoryStore.addMemory({
          type: 'interaction_pattern',
          content: 'User frequently checks weather',
          context: { query_type: 'weather', location: weather.location },
          confidence: 0.6,
          tags: ['weather', 'daily_info']
        });
        
        return `🌤️ Current weather: ${weather.temperature}°C and ${weather.condition.toLowerCase()} in ${weather.location}. Humidity is ${weather.humidity}% with winds at ${weather.windSpeed} km/h.`;
      } catch (error) {
        return "I'm having trouble getting weather information right now.";
      }
    }
    
    if (intent.intent === 'time_query') {
      const now = new Date();
      return `🕐 Current time: ${now.toLocaleTimeString()} on ${now.toLocaleDateString()}`;
    }
    
    return "I can help with weather and time information. What would you like to know?";
  }

  private async handleAdvancedLLMQuery(
    query: string, 
    intent: IntentResult,
    conversationContext: any,
    relevantMemories: any[],
    onToken?: (token: string) => void,
    onComplete?: () => void
  ): Promise<string> {
    console.log('🤖 Advanced LLM query handling with context and memory');
    
    if (intent.qa_subtype === 'conversational') {
      return await this.handleAdvancedConversationalQuery(query, conversationContext, relevantMemories, onToken, onComplete);
    }
    
    if (this.llamaService) {
      const contextPrompt = this.buildGeneralKnowledgePrompt(query, intent, conversationContext, relevantMemories);
      return await this.llamaService.generateResponse(contextPrompt, { intent, conversationContext, relevantMemories }, onToken, onComplete);
    }
    
    return this.handleConversationalFallback(query);
  }

  private async handleFallbackQuery(
    query: string, 
    intent: IntentResult,
    conversationContext: any,
    relevantMemories: any[],
    onToken?: (token: string) => void,
    onComplete?: () => void
  ): Promise<string> {
    console.log('🔄 Fallback query handling with context and memory');
    
    if (this.llamaService) {
      const fallbackPrompt = `As a health and wellness assistant, respond to this query: "${query}". 
      
User context: ${JSON.stringify(conversationContext, null, 2)}
Relevant memories: ${JSON.stringify(relevantMemories.slice(0, 3), null, 2)}

Provide helpful, accurate information while encouraging the user to consult healthcare professionals for medical concerns.`;
      return await this.llamaService.generateResponse(fallbackPrompt, { intent, conversationContext, relevantMemories }, onToken, onComplete);
    }
    
    return "I'm a health and wellness assistant. I can help you with health data, alarms, timers, weather, and basic questions. Could you rephrase your question?";
  }

  private async fetchEnhancedHealthMetrics(parameters: any): Promise<HealthMetrics> {
    const metrics: HealthMetrics = {};
    
    try {
      // Use Enhanced Intent Classifier for database-driven health data
      const enhancedIntentClassifier = new EnhancedIntentClassifier();
      
      // Build a proper intent for the health query
      const category = parameters.metric === 'sleep_score' ? 'sleep' : 
                      parameters.metric === 'heart_rate' ? 'heart_rate' :
                      parameters.metric === 'activity' ? 'activity' :
                      parameters.metric === 'stress' ? 'stress' : 'general';
      
      const timeframe = parameters.timeframes?.[0] || 'recent';
      
      const mockIntent = {
        category: category as 'heart_rate' | 'sleep' | 'activity' | 'stress' | 'nutrition' | 'general',
        timeframe: timeframe as 'today' | 'yesterday' | 'this_week' | 'last_week' | 'this_month' | 'last_month' | 'recent',
        comparison: false,
        specificMetric: undefined,
        action: 'analyze' as 'analyze' | 'compare' | 'trend' | 'advice' | 'summary'
      };
      
      console.log('🔍 ROUTER: Building health query for intent:', mockIntent);
      const healthQuery = await enhancedIntentClassifier.buildHealthDataQuery(mockIntent);
      console.log('🔍 ROUTER: Health query built:', healthQuery);
      
      const relevantHealthData = await enhancedIntentClassifier.fetchRelevantHealthData(healthQuery);
      console.log('🔍 ROUTER: Fetched health data:', relevantHealthData);
      
      if (relevantHealthData && relevantHealthData.primary) {
        const data = relevantHealthData.primary;
        
        // Convert database data to metrics format
        if (mockIntent.category === 'sleep' && data.sleep) {
          metrics.sleep_score = data.sleep.averageQuality;
          metrics.raw_data = data.sleep;
        }
        
        if (mockIntent.category === 'heart_rate' && data.heartRate) {
          metrics.heart_rate = data.heartRate.average;
          metrics.raw_data = data.heartRate;
        }
        
        if (mockIntent.category === 'activity' && data.activity) {
          metrics.steps = data.activity.averageSteps;
          metrics.raw_data = data.activity;
        }
        
        if (mockIntent.category === 'stress' && data.stress) {
          metrics.stress_level = data.stress.averageLevel;
          metrics.raw_data = data.stress;
        }
        
        // Store the complete relevant health data for the LLM
        metrics.enhanced_data = relevantHealthData;
      } else {
        console.log('🔍 ROUTER: No health data found, using fallback');
        // Fallback to old system only if no database data
        const latestData = await healthDataManager.getLatestMetrics();
        
        if (parameters.metric === 'sleep_score' && latestData?.sleep) {
          metrics.sleep_score = latestData.sleep.sleepScore;
          metrics.raw_data = latestData.sleep;
        }
        
        if (parameters.metric === 'heart_rate') {
          const timeframe = parameters.timeframes?.[0] || 'week';
          const hrData = await healthDataManager.getAverageHeartRate(timeframe);
          metrics.heart_rate = hrData.average;
          metrics.raw_data = hrData;
        }
        
        if (parameters.metric === 'activity' && latestData) {
          metrics.steps = 8500; // placeholder
          metrics.raw_data = { steps: metrics.steps, date: new Date() };
        }
        
        if (parameters.metric === 'stress' && latestData) {
          metrics.stress_level = latestData.stressLevel;
          metrics.raw_data = { stress: latestData.stressLevel };
        }
        
        if (parameters.metric === 'calories' && latestData?.calories) {
          metrics.calories_burned = latestData.calories.burned;
          metrics.raw_data = latestData.calories;
        }
      }
      
    } catch (error) {
      console.error('Error fetching enhanced health metrics:', error);
    }
    
    return metrics;
  }

  private formatStructuredHealthResponse(metrics: HealthMetrics, intent: IntentResult, query: string): string {
    const { parameters } = intent;
    
    if (parameters.metric === 'sleep_score' && metrics.sleep_score !== undefined) {
      return `😴 Your sleep score: ${metrics.sleep_score}/100`;
    }
    
    if (parameters.metric === 'heart_rate' && metrics.heart_rate !== undefined) {
      return `❤️ Your average heart rate: ${Math.round(metrics.heart_rate)} BPM`;
    }
    
    if (parameters.metric === 'activity' && metrics.steps !== undefined) {
      return `👟 Your steps today: ${metrics.steps.toLocaleString()} steps`;
    }
    
    if (parameters.metric === 'stress' && metrics.stress_level !== undefined) {
      return `😌 Your stress level: ${metrics.stress_level}/10`;
    }
    
    return "Here's your health data summary. What specific information would you like to know?";
  }

  private buildHealthCoachingPrompt(
    query: string, 
    metrics: HealthMetrics, 
    intent: IntentResult, 
    conversationContext: any, 
    relevantMemories: any[]
  ): string {
    // Extract actual user question if query contains system prompt
    let actualUserQuery = query;
    
    // If query contains system prompt, extract just the user question
    if (query.includes('You are Noise AI') || query.includes('A user has asked:')) {
      // Try to extract from "A user has asked:" pattern
      const userAskedMatch = query.match(/A user has asked: "([^"]+)"/);
      if (userAskedMatch) {
        actualUserQuery = userAskedMatch[1];
      } else {
        // Try to extract from Question: pattern
        const questionMatch = query.match(/Question: ([^\n]+)/);
        if (questionMatch) {
          actualUserQuery = questionMatch[1];
        } else {
          // Look for common query patterns at the end
          const lines = query.split('\n');
          for (let i = lines.length - 1; i >= 0; i--) {
            const line = lines[i].trim();
            if (line && !line.includes('Noise AI') && !line.includes('User Health Data') && 
                !line.includes('Provide') && !line.includes('Keep') && 
                !line.includes('Analyze') && line.length < 200) {
              actualUserQuery = line;
              break;
            }
          }
        }
      }
    }
    
    console.log('🧹 CLEANED USER QUERY:', actualUserQuery);
    console.log('🧹 ORIGINAL QUERY LENGTH:', query.length);
    console.log('🧹 CLEANED QUERY LENGTH:', actualUserQuery.length);
    
    // Build clean health context from enhanced data
    let healthContext = '';
    
    if (metrics.enhanced_data && metrics.enhanced_data.primary) {
      const data = metrics.enhanced_data.primary;
      
      // Format the health data in a clean, readable way
      if (data.sleep) {
        const hours = Math.floor(data.sleep.averageDuration / 60);
        const minutes = data.sleep.averageDuration % 60;
        healthContext += `\n- Sleep: ${hours}h ${minutes}m average duration`;
        if (data.sleep.averageQuality) {
          healthContext += `\n- Quality: ${data.sleep.averageQuality}/100 average`;
        }
        if (data.sleep.averageEfficiency) {
          healthContext += `\n- Efficiency: ${data.sleep.averageEfficiency}% average`;
        }
        if (data.sleep.nightsTracked) {
          healthContext += `\n- Nights tracked: ${data.sleep.nightsTracked}`;
        }
      }
      
      if (data.heartRate) {
        healthContext += `\n- Heart Rate: ${data.heartRate.average} BPM average`;
        if (data.heartRate.min && data.heartRate.max) {
          healthContext += ` (Range: ${data.heartRate.min}-${data.heartRate.max} BPM)`;
        }
        if (data.heartRate.recordCount) {
          healthContext += `\n- Records: ${data.heartRate.recordCount} measurements`;
        }
        if (data.heartRate.types && Array.isArray(data.heartRate.types)) {
          healthContext += `\n- Types tracked: ${data.heartRate.types.join(', ')}`;
        }
      }
      
      if (data.activity) {
        if (data.activity.averageSteps) {
          healthContext += `\n- Activity: ${data.activity.averageSteps} steps daily average`;
        }
        if (data.activity.averageCalories) {
          healthContext += `\n- Calories: ${data.activity.averageCalories} calories burned average`;
        }
      }
      
      if (data.stress) {
        healthContext += `\n- Stress Level: ${data.stress.averageLevel}/100 average`;
      }
      
      // Add timeframe
      if (metrics.enhanced_data.timeframe) {
        healthContext += `\n- Time period: ${metrics.enhanced_data.timeframe.start} to ${metrics.enhanced_data.timeframe.end}`;
      }
    } else {
      healthContext = '\n- No specific health data available for this timeframe';
    }
    
    // Get user focus areas from conversation context
    let focusAreas = '';
    if (conversationContext && conversationContext.currentFocus && conversationContext.currentFocus !== 'general') {
      focusAreas = `\n\nUser Focus Areas: ${conversationContext.currentFocus}`;
    }
    
    // Build memory context (keep it simple)
    let memoryContext = '';
    if (relevantMemories && relevantMemories.length > 0) {
      const recentMemory = relevantMemories[0];
      if (recentMemory.context && recentMemory.context.coaching_area) {
        memoryContext = `\n\nNote: User has previously shown interest in ${recentMemory.context.coaching_area} insights.`;
      }
    }
    
    return `You are Noise AI, a helpful health assistant. You provide accurate, supportive, and easy-to-understand health information. Always encourage users to consult healthcare professionals for serious concerns.

User Health Data:${healthContext}${focusAreas}

Question: ${actualUserQuery}

${memoryContext}

Provide a personalized, insightful response that:
1. Directly answers their question using the provided data
2. Offers actionable health insights and recommendations
3. Explains patterns or trends in their data if relevant
4. Maintains an encouraging, supportive tone
5. Suggests areas for improvement with specific, achievable steps

Keep the response conversational, informative, and focused on their specific query.

Analyze the specific data above and provide a focused response about what the user asked.

Noise AI: `;
  }

  private generateStructuredHealthCoachResponse(
    query: string, 
    metrics: HealthMetrics, 
    intent: IntentResult, 
    relevantMemories: any[]
  ): string {
    const queryLower = query.toLowerCase();
    
    // Check for previous coaching preferences in memory
    const coachingMemories = relevantMemories.filter(m => m.type === 'health_preference');
    const userPreferences = coachingMemories.length > 0 ? 
      ` Based on your previous interactions, I know you're interested in ${coachingMemories[0].content}.` : '';
    
    if (queryLower.includes('sleep') && metrics.sleep_score !== undefined) {
      if (metrics.sleep_score >= 80) {
        return `🌟 Excellent sleep quality! Your score of ${metrics.sleep_score}/100 shows you're getting restorative rest. Keep maintaining your consistent sleep schedule and bedtime routine!${userPreferences}`;
      } else if (metrics.sleep_score >= 60) {
        return `😊 Your sleep score of ${metrics.sleep_score}/100 is decent, but there's room for improvement. Try going to bed 30 minutes earlier or creating a more relaxing bedtime routine.${userPreferences}`;
      } else {
        return `😴 Your sleep score of ${metrics.sleep_score}/100 suggests significant improvement opportunities. Consider limiting screen time before bed, keeping a consistent sleep schedule, and creating a comfortable sleep environment.${userPreferences}`;
      }
    }
    
    if (queryLower.includes('heart rate') && metrics.heart_rate !== undefined) {
      return `❤️ Your average heart rate of ${Math.round(metrics.heart_rate)} BPM indicates good cardiovascular health. Regular exercise and stress management can help maintain healthy heart rate patterns.${userPreferences}`;
    }
    
    return `Based on your health data, you're making progress! Keep up the great work with your wellness routine.${userPreferences}`;
  }

  private buildGeneralKnowledgePrompt(
    query: string, 
    intent: IntentResult, 
    conversationContext: any, 
    relevantMemories: any[]
  ): string {
    // Extract only user question from the query, avoid system prompts
    const userQuestion = query.includes('You are Noise AI') 
      ? query.split('"')[1] || query
      : query;
    
    return `You are Noise AI, a knowledgeable health and wellness assistant. Answer this question: "${userQuestion}"

Provide accurate, helpful information while:
1. Focusing on health and wellness topics when relevant
2. Being encouraging and supportive
3. Suggesting practical applications
4. Always encouraging users to consult healthcare professionals for medical advice
5. Keeping responses conversational and accessible

Answer the question directly and helpfully.`;
  }

  private async handleAdvancedConversationalQuery(
    query: string,
    conversationContext: any,
    relevantMemories: any[],
    onToken?: (token: string) => void,
    onComplete?: () => void
  ): Promise<string> {
    if (this.llamaService) {
      const memoryData = JSON.stringify(relevantMemories.slice(0, 3), null, 2);
      const conversationData = JSON.stringify(conversationContext, null, 2);
      
      const conversationalPrompt = `You are Noise AI, a friendly health and wellness assistant. Respond naturally to: "${query}"

Conversation context:
${conversationData}

User memories and interaction patterns:
${memoryData}

Be conversational, helpful, and personable while:
1. Maintaining your role as a health assistant
2. Being warm and engaging based on their conversation style
3. Referencing their past interactions and preferences when relevant
4. Offering to help with health data, wellness tips, or daily tasks
5. Keeping responses concise but meaningful
6. Adapting your communication style to their preferences`;
      return await this.llamaService.generateResponse(conversationalPrompt, { conversationContext, relevantMemories }, onToken, onComplete);
    }
    
    return this.handleConversationalFallback(query);
  }

  private handleConversationalFallback(query: string): string {
    const lowerQuery = query.toLowerCase();
    
    if (lowerQuery.includes('hello') || lowerQuery.includes('hi')) {
      return "Hello! I'm your AI health assistant. I can help you track your health data, set alarms and reminders, check the weather, and answer wellness questions. How can I help you today?";
    }
    
    if (lowerQuery.includes('how are you')) {
      return "I'm doing great, thank you for asking! I'm here and ready to help you with your health and wellness. How are you feeling today?";
    }
    
    if (lowerQuery.includes('thank you') || lowerQuery.includes('thanks')) {
      return "You're very welcome! I'm always happy to help. Is there anything else you'd like to know about your health or wellness?";
    }
    
    if (lowerQuery.includes('goodbye') || lowerQuery.includes('bye')) {
      return "Goodbye! Take care of yourself and remember to stay healthy. I'm here whenever you need me!";
    }
    
    return "I'm your AI health assistant! I can help you with health data, alarms, timers, weather, and wellness questions. What would you like to know?";
  }
}

const queryRouter = new EnhancedQueryRouter();
export default queryRouter;
