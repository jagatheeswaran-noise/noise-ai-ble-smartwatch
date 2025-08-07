import { databaseService } from './DatabaseService';

export interface QueryIntent {
  category: 'heart_rate' | 'sleep' | 'activity' | 'stress' | 'nutrition' | 'general';
  timeframe: 'today' | 'yesterday' | 'this_week' | 'last_week' | 'this_month' | 'last_month' | 'recent';
  comparison?: boolean; // true if comparing timeframes
  specificMetric?: string;
  action: 'analyze' | 'compare' | 'trend' | 'advice' | 'summary';
}

export interface HealthDataQuery {
  startDate: string;
  endDate: string;
  category: string;
  specificTypes?: string[];
  comparison?: {
    startDate: string;
    endDate: string;
  };
}

class EnhancedIntentClassifier {
  private timeframePatterns = {
    today: ['today', 'current day', 'right now'],
    yesterday: ['yesterday', 'last day'],
    this_week: ['this week', 'current week', 'past week', 'past 7 days'],
    last_week: ['last week', 'previous week', 'week before'],
    this_month: ['this month', 'current month', 'past month', 'past 30 days'],
    last_month: ['last month', 'previous month', 'month before'],
    recent: ['recent', 'lately', 'recently', 'past few days']
  };

  private categoryPatterns = {
    heart_rate: ['heart rate', 'hr', 'bpm', 'pulse', 'cardiac', 'resting heart', 'active heart'],
    sleep: ['sleep', 'sleeping', 'slept', 'rest', 'bed', 'bedtime', 'wake', 'rem', 'deep sleep'],
    activity: ['steps', 'walking', 'exercise', 'workout', 'activity', 'calories', 'active', 'fitness'],
    stress: ['stress', 'stressed', 'anxiety', 'pressure', 'tension', 'recovery', 'hrv'],
    nutrition: ['food', 'eating', 'calories', 'nutrition', 'diet', 'meal', 'hydration', 'water'],
    general: ['health', 'wellness', 'overall', 'weight', 'mood', 'energy']
  };

  private actionPatterns = {
    analyze: ['how was', 'how is', 'what about', 'analyze', 'tell me about', 'show me'],
    compare: ['compare', 'difference', 'vs', 'versus', 'between', 'than'],
    trend: ['trend', 'pattern', 'over time', 'progress', 'improving', 'getting better'],
    advice: ['advice', 'recommend', 'suggest', 'improve', 'help', 'what should'],
    summary: ['summary', 'overview', 'report', 'status', 'overall']
  };

  async classifyIntent(userInput: string): Promise<QueryIntent> {
    const input = userInput.toLowerCase();
    
    // Determine category
    let category: QueryIntent['category'] = 'general';
    for (const [cat, patterns] of Object.entries(this.categoryPatterns)) {
      if (patterns.some(pattern => input.includes(pattern))) {
        category = cat as QueryIntent['category'];
        break;
      }
    }

    // Determine timeframe
    let timeframe: QueryIntent['timeframe'] = 'recent';
    for (const [time, patterns] of Object.entries(this.timeframePatterns)) {
      if (patterns.some(pattern => input.includes(pattern))) {
        timeframe = time as QueryIntent['timeframe'];
        break;
      }
    }

    // Determine action
    let action: QueryIntent['action'] = 'analyze';
    for (const [act, patterns] of Object.entries(this.actionPatterns)) {
      if (patterns.some(pattern => input.includes(pattern))) {
        action = act as QueryIntent['action'];
        break;
      }
    }

    // Check for comparison
    const comparison = this.actionPatterns.compare.some(pattern => input.includes(pattern));

    // Extract specific metrics
    const specificMetric = this.extractSpecificMetric(input, category);

    console.log('🎯 INTENT CLASSIFICATION:', {
      category,
      timeframe,
      comparison,
      action,
      specificMetric,
      originalQuery: userInput
    });

    return {
      category,
      timeframe,
      comparison,
      specificMetric,
      action
    };
  }

  private extractSpecificMetric(input: string, category: string): string | undefined {
    const lowercaseInput = input.toLowerCase();
    
    const metricMap: { [key: string]: { [key: string]: string } } = {
      heart_rate: {
        'resting heart rate': 'resting_hr',
        'resting hr': 'resting_hr',
        'active heart rate': 'active_hr',
        'maximum heart rate': 'max_hr',
        'max heart rate': 'max_hr',
        'recovery heart rate': 'recovery_hr'
      },
      sleep: {
        'sleep duration': 'duration',
        'sleep time': 'duration',
        'sleep quality': 'score',
        'sleep score': 'score',
        'sleep efficiency': 'efficiency'
        // Don't match general terms like "sleep" to specific metrics
      },
      activity: {
        'step count': 'steps',
        'calories': 'calories_burned',
        'distance': 'distance',
        'active minutes': 'active_minutes'
      },
      stress: {
        'stress level': 'stress_level',
        'hrv score': 'hrv',
        'recovery score': 'recovery_score'
      }
    };

    const categoryMetrics = metricMap[category];
    if (!categoryMetrics) return undefined;

    // Use more specific phrase matching to avoid false positives
    for (const [keyword, metric] of Object.entries(categoryMetrics)) {
      if (lowercaseInput.includes(keyword)) {
        return metric;
      }
    }

    return undefined;
  }

  async buildHealthDataQuery(intent: QueryIntent): Promise<HealthDataQuery> {
    const now = new Date();
    let startDate: Date, endDate: Date;

    // Calculate date ranges based on timeframe
    switch (intent.timeframe) {
      case 'today':
        startDate = new Date(now.getFullYear(), now.getMonth(), now.getDate());
        endDate = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1);
        break;
      case 'yesterday':
        startDate = new Date(now.getFullYear(), now.getMonth(), now.getDate() - 1);
        endDate = new Date(now.getFullYear(), now.getMonth(), now.getDate());
        break;
      case 'this_week':
        const thisWeekStart = new Date(now);
        thisWeekStart.setDate(now.getDate() - now.getDay());
        startDate = thisWeekStart;
        endDate = new Date();
        break;
      case 'last_week':
        const lastWeekEnd = new Date(now);
        lastWeekEnd.setDate(now.getDate() - now.getDay());
        const lastWeekStart = new Date(lastWeekEnd);
        lastWeekStart.setDate(lastWeekEnd.getDate() - 7);
        startDate = lastWeekStart;
        endDate = lastWeekEnd;
        break;
      case 'this_month':
        startDate = new Date(now.getFullYear(), now.getMonth(), 1);
        endDate = new Date();
        break;
      case 'last_month':
        startDate = new Date(now.getFullYear(), now.getMonth() - 1, 1);
        endDate = new Date(now.getFullYear(), now.getMonth(), 1);
        break;
      default: // recent
        startDate = new Date(now);
        startDate.setDate(now.getDate() - 7);
        endDate = new Date();
    }

    const query: HealthDataQuery = {
      startDate: startDate.toISOString().split('T')[0],
      endDate: endDate.toISOString().split('T')[0],
      category: intent.category,
      specificTypes: intent.specificMetric ? [intent.specificMetric] : undefined
    };

    // Add comparison timeframe if needed
    if (intent.comparison) {
      const comparisonPeriod = this.calculateComparisonPeriod(startDate, endDate);
      query.comparison = {
        startDate: comparisonPeriod.startDate.toISOString().split('T')[0],
        endDate: comparisonPeriod.endDate.toISOString().split('T')[0]
      };
    }

    console.log('📊 HEALTH DATA QUERY:', query);
    return query;
  }

  private calculateComparisonPeriod(startDate: Date, endDate: Date): { startDate: Date; endDate: Date } {
    const duration = endDate.getTime() - startDate.getTime();
    const comparisonEndDate = new Date(startDate);
    const comparisonStartDate = new Date(startDate.getTime() - duration);
    
    console.log('📊 COMPARISON PERIOD CALCULATION:');
    console.log('📊 Original period:', startDate.toISOString().split('T')[0], 'to', endDate.toISOString().split('T')[0]);
    console.log('📊 Comparison period:', comparisonStartDate.toISOString().split('T')[0], 'to', comparisonEndDate.toISOString().split('T')[0]);
    console.log('📊 Duration (days):', Math.round(duration / (1000 * 60 * 60 * 24)));
    
    return {
      startDate: comparisonStartDate,
      endDate: comparisonEndDate
    };
  }

  async fetchRelevantHealthData(query: HealthDataQuery): Promise<any> {
    try {
      await databaseService.initialize();
      
      // Fetch primary data
      const primaryData = await databaseService.getHealthDataByDateRange(
        query.startDate,
        query.endDate,
        query.category,
        query.specificTypes
      );

      let comparisonData = null;
      if (query.comparison) {
        console.log('🔄 FETCHING COMPARISON DATA for period:', query.comparison.startDate, 'to', query.comparison.endDate);
        comparisonData = await databaseService.getHealthDataByDateRange(
          query.comparison.startDate,
          query.comparison.endDate,
          query.category,
          query.specificTypes
        );
        console.log('🔄 COMPARISON DATA RESULTS:', comparisonData ? comparisonData.length : 0, 'records');
      }

      const result = {
        primary: this.aggregateHealthData(primaryData, query),
        comparison: comparisonData ? this.aggregateHealthData(comparisonData, query) : null,
        timeframe: {
          start: query.startDate,
          end: query.endDate
        }
      };

      console.log('📈 FETCHED HEALTH DATA:', result);
      return result;
    } catch (error) {
      console.error('Error fetching health data:', error);
      return null;
    }
  }

  private aggregateHealthData(data: any[], query: HealthDataQuery): any {
    if (!data || data.length === 0) {
      return { message: 'No data available for this timeframe' };
    }

    const aggregated: any = {
      recordCount: data.length,
      dateRange: {
        start: query.startDate,
        end: query.endDate
      }
    };

    // Aggregate by category
    switch (query.category) {
      case 'heart_rate':
        const hrData = data.filter(d => d.category === 'heart_rate');
        aggregated.heartRate = this.aggregateHeartRate(hrData);
        break;
      case 'sleep':
        const sleepData = data.filter(d => d.category === 'sleep');
        aggregated.sleep = this.aggregateSleep(sleepData);
        break;
      case 'activity':
        const activityData = data.filter(d => d.category === 'activity');
        aggregated.activity = this.aggregateActivity(activityData);
        break;
      case 'stress':
        const stressData = data.filter(d => d.category === 'stress');
        aggregated.stress = this.aggregateStress(stressData);
        break;
      default:
        aggregated.general = this.aggregateGeneral(data);
    }

    return aggregated;
  }

  private aggregateHeartRate(data: any[]): any {
    if (data.length === 0) return { message: 'No heart rate data available' };
    
    const values = data.map(d => parseFloat(d.value)).filter(v => !isNaN(v));
    return {
      average: Math.round(values.reduce((a, b) => a + b, 0) / values.length),
      min: Math.min(...values),
      max: Math.max(...values),
      recordCount: data.length,
      types: [...new Set(data.map(d => d.type))]
    };
  }

  private aggregateSleep(data: any[]): any {
    if (data.length === 0) return { message: 'No sleep data available' };
    
    // Match the actual types generated by EnhancedHealthDataManager
    const durationData = data.filter(d => d.type === 'duration');
    const qualityData = data.filter(d => d.type === 'score');
    const efficiencyData = data.filter(d => d.type === 'efficiency');
    
    console.log('🛏️ SLEEP AGGREGATION DEBUG:');
    console.log('🛏️ Total sleep records:', data.length);
    console.log('🛏️ Duration records:', durationData.length);
    console.log('🛏️ Quality records:', qualityData.length);
    console.log('🛏️ Efficiency records:', efficiencyData.length);
    console.log('🛏️ Sample data:', data.slice(0, 3));
    
    return {
      averageDuration: durationData.length ? 
        Math.round(durationData.reduce((a, d) => a + parseFloat(d.value), 0) / durationData.length) : 0,
      averageQuality: qualityData.length ?
        Math.round(qualityData.reduce((a, d) => a + parseFloat(d.value), 0) / qualityData.length) : 0,
      averageEfficiency: efficiencyData.length ?
        Math.round(efficiencyData.reduce((a, d) => a + parseFloat(d.value), 0) / efficiencyData.length) : 0,
      nightsTracked: Math.max(durationData.length, qualityData.length, efficiencyData.length),
      recordCount: data.length,
      types: [...new Set(data.map(d => d.type))] // Show what types we actually have
    };
  }

  private aggregateActivity(data: any[]): any {
    if (data.length === 0) return { message: 'No activity data available' };
    
    const stepsData = data.filter(d => d.type === 'steps');
    const caloriesData = data.filter(d => d.type === 'calories_burned');
    
    return {
      averageSteps: stepsData.length ?
        Math.round(stepsData.reduce((a, d) => a + parseFloat(d.value), 0) / stepsData.length) : 0,
      averageCalories: caloriesData.length ?
        Math.round(caloriesData.reduce((a, d) => a + parseFloat(d.value), 0) / caloriesData.length) : 0,
      daysTracked: Math.max(stepsData.length, caloriesData.length),
      recordCount: data.length
    };
  }

  private aggregateStress(data: any[]): any {
    if (data.length === 0) return { message: 'No stress data available' };
    
    const values = data.map(d => parseFloat(d.value)).filter(v => !isNaN(v));
    return {
      averageLevel: Math.round(values.reduce((a, b) => a + b, 0) / values.length),
      min: Math.min(...values),
      max: Math.max(...values),
      recordCount: data.length
    };
  }

  private aggregateGeneral(data: any[]): any {
    return {
      recordCount: data.length,
      categories: [...new Set(data.map(d => d.category))],
      types: [...new Set(data.map(d => d.type))]
    };
  }
}

export const enhancedIntentClassifier = new EnhancedIntentClassifier();
export default EnhancedIntentClassifier;
