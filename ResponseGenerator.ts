import { databaseService, HealthDataRecord, HealthResponse } from './DatabaseService';

export interface HealthMetrics {
  heartRate: {
    average: number;
    resting: number;
    max?: number;
    trend?: string;
  };
  sleep: {
    duration: number;
    score: number;
    efficiency: number;
    trend?: string;
  };
  activity: {
    steps: number;
    calories: number;
    activeMinutes: number;
  };
  stress: {
    level: number;
    hrv: number;
    recoveryScore: number;
    trend?: string;
  };
}

export interface ResponseData {
  [key: string]: string | number;
}

class ResponseGenerator {
  async generateResponse(userInput: string): Promise<string> {
    try {
      // Initialize database if not already done
      await databaseService.initialize();

      // Find the best matching response template
      const template = await databaseService.findBestResponseTemplate(userInput);
      
      if (!template) {
        return this.getGenericHealthResponse();
      }

      // If the template requires data, fetch and process it
      if (template.requires_data && template.data_requirements) {
        const requirements = JSON.parse(template.data_requirements);
        const data = await this.fetchRequiredData(requirements);
        return this.populateTemplate(template.response_template, data);
      }

      // Return template without data replacement
      return template.response_template;
    } catch (error) {
      console.error('Error generating response:', error);
      return this.getGenericHealthResponse();
    }
  }

  private async fetchRequiredData(requirements: any): Promise<ResponseData> {
    const data: ResponseData = {};
    const endDate = new Date().toISOString().split('T')[0];
    const startDate = new Date();
    startDate.setDate(startDate.getDate() - (requirements.period_days || 7));
    const startDateStr = startDate.toISOString().split('T')[0];

    try {
      // Fetch data for each required category and type
      for (const category of requirements.categories) {
        const types = requirements.types || [];
        
        for (const type of types) {
          const records = await databaseService.getHealthData(
            category,
            type,
            startDateStr,
            endDate
          );

          if (records.length > 0) {
            const values = records.map(r => parseFloat(r.value.toString()));
            const average = values.reduce((sum, val) => sum + val, 0) / values.length;
            const latest = parseFloat(records[0].value.toString());

            // Store different variations of the data
            data[`${category}_${type}`] = latest;
            data[`avg_${category}_${type}`] = Math.round(average * 100) / 100;
            data[`${type}`] = latest;
            data[`avg_${type}`] = Math.round(average * 100) / 100;

            // For specific naming conventions
            if (category === 'heart_rate' && type === 'average') {
              data['average'] = Math.round(average);
              data['avg_hr'] = Math.round(average);
            }
            if (category === 'heart_rate' && type === 'resting') {
              data['resting'] = Math.round(latest);
              data['resting_hr'] = Math.round(latest);
            }
            if (category === 'sleep' && type === 'duration') {
              const hours = Math.floor(latest / 60);
              const minutes = Math.round(latest % 60);
              data['hours'] = hours;
              data['minutes'] = minutes;
              data['avg_sleep'] = Math.round(average / 60 * 10) / 10;
            }
            if (category === 'sleep' && type === 'score') {
              data['score'] = Math.round(latest);
              data['sleep_score'] = Math.round(average);
            }
            if (category === 'activity' && type === 'steps') {
              data['avg_steps'] = Math.round(average);
            }
            if (category === 'stress' && type === 'level') {
              data['stress_level'] = Math.round(latest);
            }
            if (category === 'general' && type === 'recovery_score') {
              data['recovery_score'] = Math.round(latest);
            }
          }
        }
      }

      // Calculate period-specific data for comparisons
      if (requirements.period_days === 14) {
        await this.calculateWeeklyComparisons(data);
      }

      // Add derived calculations
      await this.addDerivedData(data);

      return data;
    } catch (error) {
      console.error('Error fetching required data:', error);
      return {};
    }
  }

  private async calculateWeeklyComparisons(data: ResponseData): Promise<void> {
    try {
      // Get data for last two weeks for comparison
      const endDate = new Date().toISOString().split('T')[0];
      const oneWeekAgo = new Date();
      oneWeekAgo.setDate(oneWeekAgo.getDate() - 7);
      const twoWeeksAgo = new Date();
      twoWeeksAgo.setDate(twoWeeksAgo.getDate() - 14);

      const thisWeekStart = oneWeekAgo.toISOString().split('T')[0];
      const lastWeekStart = twoWeeksAgo.toISOString().split('T')[0];
      const lastWeekEnd = oneWeekAgo.toISOString().split('T')[0];

      // This week's sleep data
      const thisWeekSleep = await databaseService.getHealthData(
        'sleep',
        'duration',
        thisWeekStart,
        endDate
      );
      const thisWeekScores = await databaseService.getHealthData(
        'sleep',
        'score',
        thisWeekStart,
        endDate
      );

      // Last week's sleep data
      const lastWeekSleep = await databaseService.getHealthData(
        'sleep',
        'duration',
        lastWeekStart,
        lastWeekEnd
      );
      const lastWeekScores = await databaseService.getHealthData(
        'sleep',
        'score',
        lastWeekStart,
        lastWeekEnd
      );

      if (thisWeekSleep.length > 0 && lastWeekSleep.length > 0) {
        // Calculate averages
        const thisWeekAvg = thisWeekSleep.reduce((sum, r) => sum + parseFloat(r.value.toString()), 0) / thisWeekSleep.length;
        const lastWeekAvg = lastWeekSleep.reduce((sum, r) => sum + parseFloat(r.value.toString()), 0) / lastWeekSleep.length;
        
        const thisWeekScoreAvg = thisWeekScores.reduce((sum, r) => sum + parseFloat(r.value.toString()), 0) / thisWeekScores.length;
        const lastWeekScoreAvg = lastWeekScores.reduce((sum, r) => sum + parseFloat(r.value.toString()), 0) / lastWeekScores.length;

        // This week data
        data['this_week_duration'] = Math.floor(thisWeekAvg / 60);
        data['this_week_minutes'] = Math.round(thisWeekAvg % 60);
        data['this_week_score'] = Math.round(thisWeekScoreAvg);

        // Last week data
        data['last_week_duration'] = Math.floor(lastWeekAvg / 60);
        data['last_week_minutes'] = Math.round(lastWeekAvg % 60);
        data['last_week_score'] = Math.round(lastWeekScoreAvg);

        // Change calculation
        const durationChange = (thisWeekAvg - lastWeekAvg) / 60;
        data['duration_change'] = (durationChange > 0 ? '+' : '') + durationChange.toFixed(1);
      }
    } catch (error) {
      console.error('Error calculating weekly comparisons:', error);
    }
  }

  private async addDerivedData(data: ResponseData): Promise<void> {
    // Calculate derived sleep metrics
    if (data['hours'] && data['minutes']) {
      const totalMinutes = (data['hours'] as number) * 60 + (data['minutes'] as number);
      data['deep_sleep'] = Math.round((totalMinutes * 0.25) / 60 * 10) / 10; // ~25% deep sleep
      data['rem_sleep'] = Math.round((totalMinutes * 0.22) / 60 * 10) / 10; // ~22% REM sleep
    }

    // Add trend analysis
    data['trend_analysis'] = this.generateTrendAnalysis(data);
    data['health_advice'] = this.generateHealthAdvice(data);
    data['sleep_analysis'] = this.generateSleepAnalysis(data);
    data['sleep_advice'] = this.generateSleepAdvice(data);
    data['stress_analysis'] = this.generateStressAnalysis(data);
    data['activity_analysis'] = this.generateActivityAnalysis(data);
    data['general_advice'] = this.generateGeneralAdvice(data);

    // Set period for response
    data['period'] = 'week';
  }

  private generateTrendAnalysis(data: ResponseData): string {
    const resting = data['resting'] as number;
    if (resting < 60) {
      return "Your heart rate trend shows excellent cardiovascular fitness, indicating effective training and good recovery.";
    } else if (resting > 80) {
      return "Your heart rate has been elevated, which could indicate stress, insufficient recovery, or the need for more cardiovascular training.";
    } else {
      return "Your heart rate has been stable within a healthy range, showing good cardiovascular health.";
    }
  }

  private generateHealthAdvice(data: ResponseData): string {
    const resting = data['resting'] as number;
    if (resting < 60) {
      return "Your resting heart rate indicates excellent fitness levels. Continue your current training regimen and ensure adequate recovery.";
    } else if (resting > 80) {
      return "Consider incorporating more cardiovascular exercise and focus on stress reduction techniques. Ensure you're getting adequate sleep and recovery.";
    } else {
      return "Your heart rate is in a healthy range. Maintain your current activity level and continue monitoring your fitness progress.";
    }
  }

  private generateSleepAnalysis(data: ResponseData): string {
    const score = data['score'] as number;
    const efficiency = data['efficiency'] as number;
    
    let analysis = "";
    if (score >= 85) {
      analysis += "Excellent sleep quality! Your sleep score indicates restorative rest with good sleep architecture. ";
    } else if (score >= 70) {
      analysis += "Good sleep quality with room for improvement. Your sleep is generally restorative. ";
    } else {
      analysis += "Your sleep quality needs attention. Consider reviewing your sleep hygiene and environment. ";
    }
    
    if (efficiency >= 90) {
      analysis += "Your sleep efficiency is excellent, meaning you spend most of your time in bed actually sleeping.";
    } else if (efficiency >= 80) {
      analysis += "Good sleep efficiency, though you might benefit from optimizing your bedtime routine.";
    } else {
      analysis += "Your sleep efficiency could be improved. You may be spending too much time awake in bed.";
    }
    
    return analysis;
  }

  private generateSleepAdvice(data: ResponseData): string {
    const score = data['score'] as number;
    const duration = (data['hours'] as number) * 60 + (data['minutes'] as number);
    
    let advice = "";
    if (score < 70) {
      advice += "Focus on improving sleep quality by maintaining a consistent sleep schedule, creating a cool and dark sleeping environment, and avoiding caffeine 3 hours before bedtime. ";
    }
    if (duration < 420) { // Less than 7 hours
      advice += "Aim for at least 7-8 hours of sleep per night for optimal health and recovery. ";
    }
    advice += "Continue tracking your sleep patterns to identify what factors contribute to your best sleep quality.";
    
    return advice;
  }

  private generateStressAnalysis(data: ResponseData): string {
    const stressLevel = data['stress_level'] as number;
    const hrv = data['hrv'] as number;
    const recovery = data['recovery_score'] as number;
    
    let analysis = "";
    if (stressLevel < 30) {
      analysis += "Your stress levels are low, indicating good stress management. ";
    } else if (stressLevel < 60) {
      analysis += "Your stress levels are moderate. Consider incorporating stress-reduction techniques. ";
    } else {
      analysis += "Your stress levels are elevated. Focus on stress management through relaxation techniques, exercise, and adequate rest. ";
    }
    
    if (hrv > 40) {
      analysis += "Your HRV indicates good stress resilience and recovery capacity. ";
    } else if (hrv > 25) {
      analysis += "Your HRV shows moderate stress resilience. ";
    } else {
      analysis += "Your HRV suggests high stress or poor recovery. Prioritize rest and stress reduction. ";
    }
    
    if (recovery < 60) {
      analysis += "Your recovery score indicates you may need more rest or stress management.";
    } else if (recovery < 80) {
      analysis += "Your recovery is moderate. Ensure you're getting adequate sleep and managing stress.";
    } else {
      analysis += "Excellent recovery! Your body is adapting well to stress and recovering effectively.";
    }
    
    return analysis;
  }

  private generateActivityAnalysis(data: ResponseData): string {
    const steps = data['avg_steps'] as number;
    const calories = data['avg_calories'] as number;
    const activeMinutes = data['avg_active_minutes'] as number;
    
    let analysis = "";
    if (steps >= 10000) {
      analysis += "Excellent daily step count! You're meeting the recommended activity guidelines. ";
    } else if (steps >= 7500) {
      analysis += "Good activity level, though you could aim for 10,000 steps daily for optimal health benefits. ";
    } else {
      analysis += "Your step count is below recommended levels. Consider increasing daily movement and walking. ";
    }
    
    if (activeMinutes >= 30) {
      analysis += "Great job staying active with structured exercise or activities.";
    } else {
      analysis += "Try to include at least 30 minutes of moderate activity in your daily routine.";
    }
    
    return analysis;
  }

  private generateGeneralAdvice(data: ResponseData): string {
    const sleep = data['avg_sleep'] as number;
    const stress = data['stress_level'] as number;
    const recovery = data['recovery_score'] as number;
    
    let advice = "Based on your health data: ";
    
    if (sleep < 7) {
      advice += "Prioritize getting 7-8 hours of sleep nightly. ";
    }
    if (stress > 60) {
      advice += "Focus on stress management through meditation, exercise, or relaxation techniques. ";
    }
    if (recovery < 70) {
      advice += "Ensure adequate recovery time between intense activities. ";
    }
    
    advice += "Continue monitoring your health metrics to track progress and maintain optimal wellness.";
    
    return advice;
  }

  private populateTemplate(template: string, data: ResponseData): string {
    let populatedTemplate = template;
    
    // Replace all placeholders with actual data
    for (const [key, value] of Object.entries(data)) {
      const placeholder = `{{${key}}}`;
      populatedTemplate = populatedTemplate.replace(new RegExp(placeholder, 'g'), value.toString());
    }
    
    // Remove any unreplaced placeholders
    populatedTemplate = populatedTemplate.replace(/\{\{[^}]+\}\}/g, '[data not available]');
    
    return populatedTemplate;
  }

  private getGenericHealthResponse(): string {
    return "I'm here to help you with your health questions. I can provide information about your sleep patterns, heart rate, activity levels, and stress management based on your health data. What specific aspect of your health would you like to know about?";
  }

  // Method to get current health metrics summary
  async getHealthMetrics(): Promise<HealthMetrics | null> {
    try {
      await databaseService.initialize();
      
      const endDate = new Date().toISOString().split('T')[0];
      const startDate = new Date();
      startDate.setDate(startDate.getDate() - 7);
      const startDateStr = startDate.toISOString().split('T')[0];

      const heartRateAvg = await databaseService.calculateAverages('heart_rate', 'average', 7);
      const heartRateResting = await databaseService.calculateAverages('heart_rate', 'resting', 7);
      const sleepDuration = await databaseService.calculateAverages('sleep', 'duration', 7);
      const sleepScore = await databaseService.calculateAverages('sleep', 'score', 7);
      const sleepEfficiency = await databaseService.calculateAverages('sleep', 'efficiency', 7);
      const steps = await databaseService.calculateAverages('activity', 'steps', 7);
      const calories = await databaseService.calculateAverages('activity', 'calories_burned', 7);
      const activeMinutes = await databaseService.calculateAverages('activity', 'active_minutes', 7);
      const stressLevel = await databaseService.calculateAverages('stress', 'level', 7);
      const hrv = await databaseService.calculateAverages('heart_rate', 'hrv', 7);
      const recovery = await databaseService.calculateAverages('general', 'recovery_score', 7);

      return {
        heartRate: {
          average: Math.round(heartRateAvg.average),
          resting: Math.round(heartRateResting.average),
        },
        sleep: {
          duration: Math.round(sleepDuration.average),
          score: Math.round(sleepScore.average),
          efficiency: Math.round(sleepEfficiency.average),
        },
        activity: {
          steps: Math.round(steps.average),
          calories: Math.round(calories.average),
          activeMinutes: Math.round(activeMinutes.average),
        },
        stress: {
          level: Math.round(stressLevel.average),
          hrv: Math.round(hrv.average),
          recoveryScore: Math.round(recovery.average),
        },
      };
    } catch (error) {
      console.error('Error getting health metrics:', error);
      return null;
    }
  }
}

export const responseGenerator = new ResponseGenerator();
export default ResponseGenerator;
