/**
 * Enhanced Intent Classification System for Noise AI
 * Implements the hierarchical intent recognition flow:
 * Level 1: ACTION vs QA
 * Level 2: QA Subtypes (health_data, general_info, conversational)
 * Based on advanced fuzzy matching and pattern recognition
 */

export interface IntentResult {
  type: 'ACTION' | 'QA';
  qa_subtype?: 'health_data' | 'general_info' | 'conversational';
  routing: 'device_action' | 'local_db' | 'external_api' | 'llm' | 'llm_health_coach';
  intent: string;
  standard_intent: string;
  query: string;
  requires_contextual_insight: boolean;
  parameters: {
    metric?: string;
    aggregation?: string;
    timeframes?: string[];
    activity_type?: string;
    targets?: string;
    location?: string;
    language?: string;
    other_tags?: string[];
  };
  flags: {
    requires_data_fetch: boolean;
    requires_response_generation: boolean;
    requires_real_time_data: boolean;
  };
  meta: {
    confidence_score: number;
    source: string;
    version: string;
  };
}

// Primary classification labels
const PRIMARY_LABELS = [
  'ACTION:device_control',
  'ACTION:data_tracking', 
  'ACTION:reminder_timer',
  'QA:health_query',
  'QA:general_info',
  'QA:conversational'
];

// QA Subtypes for fallback classification
const QA_SUBTYPES = [
  'health_data:analysis',
  'health_data:comparison',
  'general_info:weather',
  'general_info:time',
  'general_info:knowledge',
  'conversational:greeting',
  'conversational:thanks',
  'conversational:casual'
];

// Comprehensive intent mapping with fuzzy matching support
const INTENT_MAP: { [key: string]: string } = {
  // Health Data Queries - Specific metrics
  'heart rate': 'heart_rate_query',
  'hr data': 'heart_rate_query',
  'pulse': 'heart_rate_query',
  'heartbeat': 'heart_rate_query',
  'cardiovascular': 'heart_rate_query',
  
  'sleep': 'sleep_analysis',
  'sleep score': 'sleep_analysis', 
  'sleep quality': 'sleep_analysis',
  'sleep pattern': 'sleep_analysis',
  'sleep trend': 'sleep_analysis',
  'bedtime': 'sleep_analysis',
  'wake up': 'sleep_analysis',
  'rest': 'sleep_analysis',
  
  'hrv': 'hrv_analysis',
  'heart rate variability': 'hrv_analysis',
  'stress recovery': 'hrv_analysis',
  
  'stress': 'stress_analysis',
  'stress level': 'stress_analysis',
  'anxiety': 'stress_analysis',
  'tension': 'stress_analysis',
  'relaxation': 'stress_analysis',
  
  'activity': 'activity_analysis',
  'exercise': 'activity_analysis',
  'workout': 'activity_analysis',
  'fitness': 'activity_analysis',
  'training': 'activity_analysis',
  'movement': 'activity_analysis',
  
  'steps': 'activity_analysis',
  'walking': 'activity_analysis',
  'distance': 'activity_analysis',
  
  'calories': 'calorie_query',
  'calorie burn': 'calorie_query',
  'energy expenditure': 'calorie_query',
  'metabolism': 'calorie_query',
  
  // Comparative and analytical queries
  'compare': 'health_comparison',
  'comparison': 'health_comparison',
  'versus': 'health_comparison',
  'vs': 'health_comparison',
  'better': 'health_comparison',
  'worse': 'health_comparison',
  'improve': 'health_comparison',
  'improving': 'health_comparison',
  'progress': 'health_comparison',
  
  'trend': 'health_trend',
  'trends': 'health_trend',
  'pattern': 'health_trend',
  'patterns': 'health_trend',
  'over time': 'health_trend',
  'historical': 'health_trend',
  'change': 'health_trend',
  'changes': 'health_trend',
  
  // Daily Assistant Actions
  'remind me': 'set_reminder',
  'reminder': 'set_reminder',
  'remember': 'set_reminder',
  'notify': 'set_reminder',
  'alert': 'set_reminder',
  
  'timer': 'set_timer',
  'countdown': 'set_timer',
  'stopwatch': 'set_timer',
  
  'alarm': 'set_alarm',
  'wake me': 'set_alarm',
  'wake up call': 'set_alarm',
  
  'weather': 'weather_info',
  'temperature': 'weather_info',
  'forecast': 'weather_info',
  'climate': 'weather_info',
  'rain': 'weather_info',
  'sunny': 'weather_info',
  'cloudy': 'weather_info',
  
  'time': 'time_query',
  'clock': 'time_query',
  'date': 'date_query',
  'today': 'date_query',
  'calendar': 'date_query',
  
  'track calories': 'track_calories',
  'log food': 'track_calories',
  'ate': 'track_calories',
  'eating': 'track_calories',
  'food': 'track_calories',
  'meal': 'track_calories',
  'diet': 'track_calories',
  
  // General Info and Knowledge
  'what is': 'general_knowledge',
  'what are': 'general_knowledge',
  'how to': 'general_knowledge',
  'how do': 'general_knowledge',
  'explain': 'health_explanation',
  'definition': 'health_explanation',
  'meaning': 'health_explanation',
  'why': 'health_explanation',
  'why do': 'health_explanation',
  'why does': 'health_explanation',
  'causes': 'health_explanation',
  'symptoms': 'health_explanation',
  
  // Conversational patterns
  'hello': 'conversational',
  'hi': 'conversational',
  'hey': 'conversational',
  'good morning': 'conversational',
  'good afternoon': 'conversational',
  'good evening': 'conversational',
  'how are you': 'conversational',
  'how are you doing': 'conversational',
  'whats up': 'conversational',
  'thank you': 'conversational',
  'thanks': 'conversational',
  'appreciate': 'conversational',
  'goodbye': 'conversational',
  'bye': 'conversational',
  'see you': 'conversational',
  'chat': 'conversational',
  'talk': 'conversational',
};

// Enhanced standard intent mapping
const STANDARD_INTENT_MAP: { [key: string]: string } = {
  'heart_rate_query': 'heart_rate_analysis',
  'sleep_analysis': 'sleep_analysis',
  'hrv_analysis': 'hrv_analysis',
  'stress_analysis': 'stress_analysis',
  'activity_analysis': 'activity_analysis',
  'calorie_query': 'activity_analysis',
  'health_comparison': 'health_data_comparison',
  'health_trend': 'health_data_comparison',
  'set_reminder': 'daily_assistant_action',
  'set_timer': 'daily_assistant_action',
  'set_alarm': 'daily_assistant_action',
  'weather_info': 'weather_info',
  'time_query': 'general_info',
  'date_query': 'general_info',
  'track_calories': 'daily_assistant_action',
  'general_knowledge': 'general_knowledge',
  'health_explanation': 'health_explanation',
  'conversational': 'conversational',
};

export class IntentClassifier {
  
  /**
   * Main intent classification function - enhanced version
   */
  public classifyIntent(query: string): IntentResult {
    const result: IntentResult = {
      type: 'QA',
      routing: 'llm',
      intent: '',
      standard_intent: '',
      query: query,
      requires_contextual_insight: false,
      parameters: {},
      flags: {
        requires_data_fetch: false,
        requires_response_generation: true,
        requires_real_time_data: false,
      },
      meta: {
        confidence_score: 0.8,
        source: 'Noise-AI',
        version: 'v1.0',
      },
    };

    // Step 1: Primary Classification (ACTION vs QA)
    const primaryResult = this.classifyPrimaryType(query);
    result.type = primaryResult.type;
    result.meta.confidence_score = primaryResult.confidence;

    // Step 2: Intent Detection (enhanced fuzzy matching)
    const detectedIntent = this.getBestIntent(query);
    result.intent = detectedIntent;

    // Step 3: Assign Standardized Intent Category
    const standardIntent = STANDARD_INTENT_MAP[detectedIntent] || 'other';
    result.standard_intent = standardIntent;

    // Step 4: Routing and Subtype Decision
    if (result.type === 'ACTION') {
      result.routing = 'device_action';
      result.qa_subtype = undefined;
    } else {
      // Enhanced QA flow routing
      this.assignAdvancedQARouting(result, standardIntent, query);
    }

    // Step 5: Extract Parameters (enhanced)
    result.parameters = this.extractEnhancedParameters(query);

    // Step 6: Set Context Flag
    result.requires_contextual_insight = result.routing === 'llm_health_coach';

    // Step 7: Set Flags & Metadata
    this.setAdvancedFlags(result);

    return result;
  }

  /**
   * Enhanced Level 1: Classify as ACTION or QA with confidence scoring
   */
  private classifyPrimaryType(query: string): { type: 'ACTION' | 'QA', confidence: number } {
    const queryLower = query.toLowerCase();
    
    // Enhanced action detection patterns
    const actionPatterns = [
      // Direct command patterns
      { pattern: /^(set|create|start|stop|pause|resume)/, weight: 0.9 },
      { pattern: /(remind me|set a reminder|reminder for)/i, weight: 0.95 },
      { pattern: /(set.*timer|timer for|countdown)/i, weight: 0.9 },
      { pattern: /(set.*alarm|wake me|alarm for)/i, weight: 0.9 },
      { pattern: /(track|log|record|save|add).*\b(food|meal|calories|exercise)/i, weight: 0.85 },
      
      // Action verbs with high confidence
      { pattern: /\b(turn on|turn off|enable|disable|activate|deactivate)\b/i, weight: 0.8 },
      { pattern: /\b(schedule|plan|organize)\b/i, weight: 0.7 },
    ];

    // Question patterns (QA indicators)
    const qaPatterns = [
      { pattern: /^(what|how|when|where|why|which|who)/, weight: 0.9 },
      { pattern: /\b(tell me|show me|explain|describe)\b/i, weight: 0.8 },
      { pattern: /\?(.*)?$/, weight: 0.95 }, // Ends with question mark
      { pattern: /\b(compare|analysis|trend|pattern|insight)\b/i, weight: 0.8 },
    ];

    let actionScore = 0;
    let qaScore = 0;

    // Calculate action score
    for (const { pattern, weight } of actionPatterns) {
      if (pattern.test(queryLower)) {
        actionScore = Math.max(actionScore, weight);
      }
    }

    // Calculate QA score
    for (const { pattern, weight } of qaPatterns) {
      if (pattern.test(queryLower)) {
        qaScore = Math.max(qaScore, weight);
      }
    }

    // Default bias towards QA if unclear
    if (actionScore === 0 && qaScore === 0) {
      qaScore = 0.6; // Default QA confidence
    }

    return actionScore > qaScore 
      ? { type: 'ACTION', confidence: actionScore }
      : { type: 'QA', confidence: Math.max(qaScore, 0.6) };
  }

  /**
   * Enhanced intent detection with fuzzy matching and similarity scoring
   */
  private getBestIntent(query: string): string {
    const queryLower = query.toLowerCase();

    // Step 1: Exact substring matching (highest priority)
    const exactMatches: { intent: string, score: number }[] = [];
    for (const phrase in INTENT_MAP) {
      if (queryLower.includes(phrase)) {
        // Calculate match quality based on phrase length and position
        const matchLength = phrase.length;
        const matchIndex = queryLower.indexOf(phrase);
        const score = (matchLength / queryLower.length) * 
                     (matchIndex === 0 ? 1.2 : 1.0); // Boost for beginning matches
        exactMatches.push({ intent: INTENT_MAP[phrase], score });
      }
    }

    if (exactMatches.length > 0) {
      // Return the best exact match
      exactMatches.sort((a, b) => b.score - a.score);
      return exactMatches[0].intent;
    }

    // Step 2: Enhanced pattern matching with word boundaries
    const patterns = [
      { regex: /\b(heart rate|hr|pulse|cardiovascular)\b/i, intent: 'heart_rate_query' },
      { regex: /\b(sleep|sleeping|rest|bedtime|wake up)\b/i, intent: 'sleep_analysis' },
      { regex: /\b(hrv|heart rate variability|recovery)\b/i, intent: 'hrv_analysis' },
      { regex: /\b(stress|anxiety|tension|relaxation)\b/i, intent: 'stress_analysis' },
      { regex: /\b(activity|exercise|workout|fitness|training)\b/i, intent: 'activity_analysis' },
      { regex: /\b(steps|walking|distance|movement)\b/i, intent: 'activity_analysis' },
      { regex: /\b(calorie|calories|energy|metabolism)\b/i, intent: 'calorie_query' },
      { regex: /\b(compare|comparison|versus|vs|better|worse)\b/i, intent: 'health_comparison' },
      { regex: /\b(trend|pattern|over time|historical|change)\b/i, intent: 'health_trend' },
      { regex: /\b(weather|temperature|forecast|rain|sunny)\b/i, intent: 'weather_info' },
      { regex: /\b(time|clock|date|today|calendar)\b/i, intent: 'time_query' },
      { regex: /\b(remind|reminder|notify|alert)\b/i, intent: 'set_reminder' },
      { regex: /\b(timer|countdown|stopwatch)\b/i, intent: 'set_timer' },
      { regex: /\b(alarm|wake me)\b/i, intent: 'set_alarm' },
      { regex: /\b(hello|hi|hey|good morning|good afternoon)\b/i, intent: 'conversational' },
      { regex: /\b(thank you|thanks|appreciate)\b/i, intent: 'conversational' },
      { regex: /\b(what is|how to|explain|why|definition)\b/i, intent: 'general_knowledge' },
    ];

    for (const { regex, intent } of patterns) {
      if (regex.test(queryLower)) {
        return intent;
      }
    }

    // Step 3: Fuzzy matching with similarity calculation
    const fuzzyMatches = this.getFuzzyMatches(queryLower, Object.keys(INTENT_MAP), 0.6);
    if (fuzzyMatches.length > 0) {
      return INTENT_MAP[fuzzyMatches[0]];
    }

    // Step 4: Context-based fallback
    return this.getContextualFallback(queryLower);
  }

  /**
   * Simple fuzzy matching implementation
   */
  private getFuzzyMatches(query: string, candidates: string[], threshold: number): string[] {
    const matches: { phrase: string, similarity: number }[] = [];
    
    for (const candidate of candidates) {
      const similarity = this.calculateSimilarity(query, candidate);
      if (similarity >= threshold) {
        matches.push({ phrase: candidate, similarity });
      }
    }
    
    matches.sort((a, b) => b.similarity - a.similarity);
    return matches.slice(0, 3).map(m => m.phrase); // Return top 3 matches
  }

  /**
   * Calculate string similarity (simplified Levenshtein distance)
   */
  private calculateSimilarity(str1: string, str2: string): number {
    const maxLength = Math.max(str1.length, str2.length);
    if (maxLength === 0) return 1.0;
    
    const distance = this.levenshteinDistance(str1, str2);
    return (maxLength - distance) / maxLength;
  }

  /**
   * Levenshtein distance calculation
   */
  private levenshteinDistance(str1: string, str2: string): number {
    const matrix = [];
    
    for (let i = 0; i <= str2.length; i++) {
      matrix[i] = [i];
    }
    
    for (let j = 0; j <= str1.length; j++) {
      matrix[0][j] = j;
    }
    
    for (let i = 1; i <= str2.length; i++) {
      for (let j = 1; j <= str1.length; j++) {
        if (str2.charAt(i - 1) === str1.charAt(j - 1)) {
          matrix[i][j] = matrix[i - 1][j - 1];
        } else {
          matrix[i][j] = Math.min(
            matrix[i - 1][j - 1] + 1,
            matrix[i][j - 1] + 1,
            matrix[i - 1][j] + 1
          );
        }
      }
    }
    
    return matrix[str2.length][str1.length];
  }

  /**
   * Contextual fallback based on query characteristics
   */
  private getContextualFallback(query: string): string {
    // Questions without specific intent - likely general knowledge
    if (query.includes('?') || query.startsWith('what') || query.startsWith('how')) {
      return 'general_knowledge';
    }
    
    // Greetings and social interactions
    if (query.length < 20 && (query.includes('hello') || query.includes('hi'))) {
      return 'conversational';
    }
    
    // Numbers and measurements might be health-related
    if (/\d+/.test(query) && (query.includes('bpm') || query.includes('score') || query.includes('%'))) {
      return 'health_comparison';
    }
    
    return 'general_knowledge';
  }

  /**
   * Enhanced QA routing and subtype assignment
   */
  private assignAdvancedQARouting(result: IntentResult, standardIntent: string, query: string): void {
    const queryLower = query.toLowerCase();
    
    switch (standardIntent) {
      case 'health_explanation':
        result.qa_subtype = 'general_info';
        result.routing = 'llm';
        break;

      case 'sleep_analysis':
      case 'hrv_analysis':
      case 'heart_rate_analysis':
      case 'activity_analysis':
      case 'stress_analysis':
        // Advanced routing logic for health data
        if (this.requiresAdvancedAnalysis(queryLower)) {
          result.qa_subtype = 'health_data';
          result.routing = 'llm_health_coach';
          result.requires_contextual_insight = true;
        } else if (this.isSimpleDataRequest(queryLower)) {
          result.qa_subtype = 'health_data';
          result.routing = 'local_db';
        } else {
          // Default to health coach for ambiguous health queries
          result.qa_subtype = 'health_data';
          result.routing = 'llm_health_coach';
          result.requires_contextual_insight = true;
        }
        break;

      case 'health_data_comparison':
        result.qa_subtype = 'health_data';
        result.routing = 'llm_health_coach';
        result.requires_contextual_insight = true;
        break;

      case 'weather_info':
        result.qa_subtype = 'general_info';
        result.routing = 'external_api';
        break;

      case 'daily_assistant_action':
        // These are actually actions, update type
        result.type = 'ACTION';
        result.routing = 'device_action';
        result.qa_subtype = undefined;
        break;

      case 'conversational':
        result.qa_subtype = 'conversational';
        result.routing = 'llm';
        break;

      case 'general_knowledge':
      case 'general_info':
        result.qa_subtype = 'general_info';
        result.routing = 'llm';
        break;

      default:
        result.qa_subtype = 'general_info';
        result.routing = 'llm';
        break;
    }
  }

  /**
   * Determine if query requires advanced analysis (coaching/insights)
   */
  private requiresAdvancedAnalysis(query: string): boolean {
    const advancedKeywords = [
      'compare', 'comparison', 'versus', 'vs', 'better', 'worse', 'improve', 'improving',
      'progress', 'trend', 'trends', 'pattern', 'patterns', 'over time', 'historical',
      'change', 'changes', 'analysis', 'analyze', 'insight', 'insights', 'advice',
      'recommend', 'recommendation', 'should i', 'how can i', 'help me', 'optimize',
      'enhancement', 'correlation', 'relationship', 'why is', 'what does this mean'
    ];
    
    return advancedKeywords.some(keyword => query.includes(keyword));
  }

  /**
   * Determine if query is asking for simple data retrieval
   */
  private isSimpleDataRequest(query: string): boolean {
    const simplePatterns = [
      /^(what is|what was|show me) my \w+ (today|yesterday|this week|last week)$/i,
      /^my \w+ (score|rate|level|count)$/i,
      /^(current|latest) \w+ (data|reading|value)$/i,
    ];
    
    return simplePatterns.some(pattern => pattern.test(query.trim()));
  }

  /**
   * Enhanced parameter extraction with more sophisticated parsing
   */
  private extractEnhancedParameters(query: string): any {
    const params: any = {};
    const queryLower = query.toLowerCase();

    // Enhanced time frame extraction with more patterns
    const timeframes = [];
    const timePatterns = [
      { pattern: /\b(today|this morning|this afternoon|this evening)\b/, value: 'today' },
      { pattern: /\b(yesterday|last night)\b/, value: 'yesterday' },
      { pattern: /\b(this week|current week)\b/, value: 'this_week' },
      { pattern: /\b(last week|previous week)\b/, value: 'last_week' },
      { pattern: /\b(this month|current month)\b/, value: 'this_month' },
      { pattern: /\b(last month|previous month)\b/, value: 'last_month' },
      { pattern: /\b(past \d+ days?|last \d+ days?)\b/, value: 'custom_days' },
      { pattern: /\b(past week|recent)\b/, value: 'recent' },
    ];
    
    for (const { pattern, value } of timePatterns) {
      if (pattern.test(queryLower)) {
        timeframes.push(value);
      }
    }
    if (timeframes.length > 0) params.timeframes = timeframes;

    // Enhanced metric extraction with synonyms
    const metricPatterns = [
      { patterns: ['heart rate', 'hr', 'pulse', 'heartbeat', 'bpm'], metric: 'heart_rate' },
      { patterns: ['sleep', 'sleep score', 'sleep quality', 'rest'], metric: 'sleep_score' },
      { patterns: ['hrv', 'heart rate variability', 'recovery'], metric: 'hrv' },
      { patterns: ['stress', 'stress level', 'anxiety', 'tension'], metric: 'stress' },
      { patterns: ['steps', 'walking', 'activity', 'movement'], metric: 'activity' },
      { patterns: ['exercise', 'workout', 'fitness', 'training'], metric: 'exercise' },
      { patterns: ['calories', 'calorie', 'energy', 'burn'], metric: 'calories' },
      { patterns: ['weight', 'body weight', 'mass'], metric: 'weight' },
    ];
    
    for (const { patterns, metric } of metricPatterns) {
      if (patterns.some(pattern => queryLower.includes(pattern))) {
        params.metric = metric;
        break;
      }
    }

    // Enhanced aggregation type detection
    const aggregationPatterns = [
      { patterns: ['compare', 'comparison', 'versus', 'vs'], aggregation: 'compare' },
      { patterns: ['average', 'avg', 'mean'], aggregation: 'avg' },
      { patterns: ['trend', 'trends', 'pattern', 'over time'], aggregation: 'trend' },
      { patterns: ['total', 'sum', 'cumulative'], aggregation: 'sum' },
      { patterns: ['maximum', 'max', 'highest', 'peak'], aggregation: 'max' },
      { patterns: ['minimum', 'min', 'lowest'], aggregation: 'min' },
      { patterns: ['range', 'variation', 'difference'], aggregation: 'range' },
    ];
    
    for (const { patterns, aggregation } of aggregationPatterns) {
      if (patterns.some(pattern => queryLower.includes(pattern))) {
        params.aggregation = aggregation;
        break;
      }
    }

    // Extract activity types
    const activityTypes = [];
    const activities = ['running', 'walking', 'cycling', 'swimming', 'yoga', 'gym', 'workout'];
    for (const activity of activities) {
      if (queryLower.includes(activity)) {
        activityTypes.push(activity);
      }
    }
    if (activityTypes.length > 0) params.activity_type = activityTypes[0];

    // Extract numerical values and units
    const numbers = queryLower.match(/\d+/g);
    if (numbers) {
      params.numerical_values = numbers.map(n => parseInt(n));
    }

    // Extract goals/targets
    if (queryLower.includes('goal') || queryLower.includes('target')) {
      params.targets = 'goal_related';
    }

    return params;
  }

  /**
   * Enhanced flag setting with more sophisticated logic
   */
  private setAdvancedFlags(result: IntentResult): void {
    // Data fetch requirements
    result.flags.requires_data_fetch = [
      'local_db', 
      'external_api', 
      'llm_health_coach'
    ].includes(result.routing);

    // Real-time data requirements
    result.flags.requires_real_time_data = result.routing === 'external_api' ||
      (result.routing === 'llm_health_coach' && 
       (result.parameters?.timeframes?.includes('today') ?? false));

    // Response generation (always true unless it's a pure action)
    result.flags.requires_response_generation = result.type !== 'ACTION' || 
      result.routing !== 'device_action';

    // Additional contextual flags
    if (result.requires_contextual_insight) {
      result.flags.requires_data_fetch = true;
    }
  }
}

// Singleton instance
export const intentClassifier = new IntentClassifier();
