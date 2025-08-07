import AsyncStorage from '@react-native-async-storage/async-storage';

// Sleep stages enum
export enum SleepStage {
  AWAKE = 'awake',
  LIGHT = 'light',
  DEEP = 'deep',
  REM = 'rem'
}

// Health data interfaces
export interface HeartRateData {
  timestamp: Date;
  bpm: number;
  context?: string; // 'resting', 'exercise', 'recovery'
}

export interface RespiratoryRateData {
  timestamp: Date;
  rpm: number; // respirations per minute
}

export interface HRVData {
  timestamp: Date;
  rmssd: number; // Root Mean Square of Successive Differences (ms)
  pnn50: number; // Percentage of adjacent NN intervals that differ by >50ms
  sdnn: number; // Standard Deviation of NN intervals (ms)
}

export interface SleepStageData {
  timestamp: Date;
  stage: SleepStage;
  duration: number; // in minutes
}

export interface SleepSessionData {
  date: Date;
  bedTime: Date;
  sleepTime: Date;
  wakeTime: Date;
  totalSleepTime: number; // in minutes
  sleepEfficiency: number; // percentage
  stages: SleepStageData[];
  sleepScore: number; // 0-100
}

export interface CalorieBurnData {
  timestamp: Date;
  activity: string; // 'walking', 'running', 'cycling', 'strength', 'rest'
  duration: number; // in minutes
  caloriesBurned: number;
  intensity: 'low' | 'moderate' | 'high';
  heartRateAvg?: number;
}

export interface HealthGoals {
  sleep: {
    targetDuration: number; // minutes
    targetBedtime: string; // "22:30"
    targetWakeTime: string; // "06:30"
    targetScore: number; // 0-100
  };
  fitness: {
    dailyCalorieTarget: number;
    weeklyExerciseMinutes: number;
    stepsTarget?: number;
  };
  wellness: {
    maxStressLevel: number; // 0-100
    minRecoveryScore: number; // 0-100
    hydrationTarget: number; // glasses per day
  };
}

export interface UserContext {
  preferences: {
    responseLength: 'brief' | 'moderate' | 'detailed';
    focusAreas: string[]; // ['sleep', 'fitness', 'nutrition', 'stress']
    timePreferences: {
      wakeTime: string;
      bedTime: string;
      workoutTime: string;
    };
    sleepGoal: number;
    exerciseGoal: number;
    calorieGoal: number;
    stressManagement: 'low' | 'moderate' | 'high';
  };
  behaviorPatterns: {
    commonQueries: string[];
    queryTimes: { [key: string]: string[] }; // time patterns for different query types
    engagementLevel: 'low' | 'moderate' | 'high';
    lastInteractionDate: Date;
  };
  healthHistory: {
    chronicConditions: string[];
    allergies: string[];
    medications: string[];
    injuries: string[];
  };
  lifestyle: {
    dietType: string;
    activityLevel: 'low' | 'moderate' | 'high';
    sleepSchedule: 'irregular' | 'regular' | 'shift';
    workSchedule: 'standard' | 'flexible' | 'shift';
  };
  goals: HealthGoals;
  achievements: {
    streaks: {
      sleepGoal: number; // consecutive days
      exerciseGoal: number;
      nutritionGoal: number;
    };
    milestones: Array<{
      type: string;
      achievement: string;
      date: Date;
    }>;
  };
}

export interface DailyHealthMetrics {
  date: Date;
  heartRate: {
    average: number;
    min: number;
    max: number;
    resting: number;
    data: HeartRateData[];
  };
  respiratoryRate: {
    average: number;
    min: number;
    max: number;
    data: RespiratoryRateData[];
  };
  hrv: {
    average: number;
    trend: 'improving' | 'stable' | 'declining';
    data: HRVData[];
  };
  sleep: SleepSessionData | null;
  calories: {
    burned: number;
    bmr: number; // basal metabolic rate
    activities: CalorieBurnData[];
  };
  stressLevel: number; // 0-100 derived from HRV and other metrics
  recoveryScore: number; // 0-100
}

export interface HealthTrends {
  period: 'week' | 'month' | 'quarter';
  heartRate: {
    average: number;
    trend: 'improving' | 'stable' | 'declining';
    changePercent: number;
  };
  sleep: {
    averageDuration: number;
    averageEfficiency: number;
    averageScore: number;
    trend: 'improving' | 'stable' | 'declining';
  };
  hrv: {
    average: number;
    trend: 'improving' | 'stable' | 'declining';
    changePercent: number;
  };
  recovery: {
    average: number;
    trend: 'improving' | 'stable' | 'declining';
  };
}

class HealthDataManager {
  private static readonly STORAGE_KEY = 'noise_ai_health_data';
  private static readonly SAMPLE_DATA_KEY = 'noise_ai_sample_health_data';

  // Generate sample health data for demonstration
  async generateSampleData(): Promise<void> {
    const endDate = new Date();
    const startDate = new Date();
    startDate.setDate(endDate.getDate() - 30); // 30 days of data

    const sampleData: DailyHealthMetrics[] = [];

    for (let d = new Date(startDate); d <= endDate; d.setDate(d.getDate() + 1)) {
      const currentDate = new Date(d);
      
      // Generate heart rate data (throughout the day)
      const heartRateData: HeartRateData[] = [];
      const baseRestingHR = 60 + Math.random() * 20; // 60-80 bpm base
      
      for (let hour = 0; hour < 24; hour++) {
        const timestamp = new Date(currentDate);
        timestamp.setHours(hour, Math.floor(Math.random() * 60));
        
        let bpm = baseRestingHR;
        let context = 'resting';
        
        // Simulate daily patterns
        if (hour >= 6 && hour <= 8) {
          bpm += 10 + Math.random() * 20; // Morning activity
          context = 'activity';
        } else if (hour >= 12 && hour <= 14) {
          bpm += 15 + Math.random() * 25; // Afternoon activity
          context = 'activity';
        } else if (hour >= 18 && hour <= 20) {
          bpm += 20 + Math.random() * 40; // Evening exercise
          context = 'exercise';
        } else if (hour >= 22 || hour <= 6) {
          bpm -= 5 + Math.random() * 10; // Sleep/rest
          context = 'resting';
        }
        
        heartRateData.push({
          timestamp,
          bpm: Math.round(bpm + (Math.random() - 0.5) * 10),
          context
        });
      }

      // Generate respiratory rate data
      const respiratoryData: RespiratoryRateData[] = [];
      const baseRR = 12 + Math.random() * 8; // 12-20 rpm
      
      for (let i = 0; i < 6; i++) { // 6 readings per day
        const timestamp = new Date(currentDate);
        timestamp.setHours(i * 4, Math.floor(Math.random() * 60));
        
        respiratoryData.push({
          timestamp,
          rpm: Math.round(baseRR + (Math.random() - 0.5) * 4)
        });
      }

      // Generate HRV data
      const hrvData: HRVData[] = [];
      const baseRMSSD = 25 + Math.random() * 30; // 25-55ms
      
      for (let i = 0; i < 3; i++) { // 3 readings per day
        const timestamp = new Date(currentDate);
        timestamp.setHours(i * 8, Math.floor(Math.random() * 60));
        
        const rmssd = baseRMSSD + (Math.random() - 0.5) * 10;
        hrvData.push({
          timestamp,
          rmssd: Math.round(rmssd),
          pnn50: Math.round(15 + Math.random() * 20), // 15-35%
          sdnn: Math.round(rmssd * 1.5 + Math.random() * 10) // Related to RMSSD
        });
      }

      // Generate sleep data
      const bedTime = new Date(currentDate);
      bedTime.setHours(22 + Math.random() * 2, Math.floor(Math.random() * 60)); // 22:00-23:59
      
      const sleepTime = new Date(bedTime);
      sleepTime.setMinutes(sleepTime.getMinutes() + 10 + Math.random() * 20); // 10-30 min to fall asleep
      
      const wakeTime = new Date(sleepTime);
      wakeTime.setHours(sleepTime.getHours() + 7 + Math.random() * 2); // 7-9 hours sleep
      
      const totalSleepMinutes = (wakeTime.getTime() - sleepTime.getTime()) / (1000 * 60);
      
      // Generate sleep stages
      const sleepStages: SleepStageData[] = [];
      let currentTime = new Date(sleepTime);
      let remainingTime = totalSleepMinutes;
      
      // Sleep cycle distribution (approximate)
      const stageDistribution = {
        [SleepStage.LIGHT]: 0.45, // 45%
        [SleepStage.DEEP]: 0.25,  // 25%
        [SleepStage.REM]: 0.25,   // 25%
        [SleepStage.AWAKE]: 0.05  // 5%
      };
      
      Object.entries(stageDistribution).forEach(([stage, percentage]) => {
        const duration = Math.round(totalSleepMinutes * percentage);
        if (duration > 0 && remainingTime > 0) {
          const actualDuration = Math.min(duration, remainingTime);
          sleepStages.push({
            timestamp: new Date(currentTime),
            stage: stage as SleepStage,
            duration: actualDuration
          });
          currentTime.setMinutes(currentTime.getMinutes() + actualDuration);
          remainingTime -= actualDuration;
        }
      });

      const sleepEfficiency = Math.round(75 + Math.random() * 20); // 75-95%
      const sleepScore = Math.round(60 + Math.random() * 35); // 60-95

      const sleepSession: SleepSessionData = {
        date: new Date(currentDate),
        bedTime,
        sleepTime,
        wakeTime,
        totalSleepTime: Math.round(totalSleepMinutes),
        sleepEfficiency,
        stages: sleepStages,
        sleepScore
      };

      // Calculate averages
      const avgHeartRate = Math.round(heartRateData.reduce((sum, d) => sum + d.bpm, 0) / heartRateData.length);
      const minHeartRate = Math.min(...heartRateData.map(d => d.bpm));
      const maxHeartRate = Math.max(...heartRateData.map(d => d.bpm));
      const restingHeartRate = Math.round(heartRateData.filter(d => d.context === 'resting').reduce((sum, d) => sum + d.bpm, 0) / heartRateData.filter(d => d.context === 'resting').length);
      
      const avgRespiratoryRate = Math.round(respiratoryData.reduce((sum, d) => sum + d.rpm, 0) / respiratoryData.length);
      const minRespiratoryRate = Math.min(...respiratoryData.map(d => d.rpm));
      const maxRespiratoryRate = Math.max(...respiratoryData.map(d => d.rpm));
      
      const avgHRV = Math.round(hrvData.reduce((sum, d) => sum + d.rmssd, 0) / hrvData.length);
      
      const stressLevel = Math.round(20 + Math.random() * 60); // 20-80
      const recoveryScore = Math.round(50 + Math.random() * 40); // 50-90

      // Generate calorie burn data
      const calorieActivities: CalorieBurnData[] = [];
      const baseBMR = 1800 + Math.random() * 400; // 1800-2200 BMR
      let totalCaloriesBurned = Math.round(baseBMR); // Start with BMR
      
      // Add various activities throughout the day
      const activities = [
        { name: 'walking', duration: 30 + Math.random() * 60, intensity: 'moderate' as const, caloriesPerMin: 5 },
        { name: 'running', duration: Math.random() * 45, intensity: 'high' as const, caloriesPerMin: 12 },
        { name: 'cycling', duration: Math.random() * 30, intensity: 'moderate' as const, caloriesPerMin: 8 },
        { name: 'strength', duration: Math.random() * 45, intensity: 'moderate' as const, caloriesPerMin: 6 },
      ];

      activities.forEach((activity, index) => {
        if (Math.random() > 0.3) { // 70% chance of doing each activity
          const duration = Math.round(activity.duration);
          const calories = Math.round(duration * activity.caloriesPerMin * (0.8 + Math.random() * 0.4));
          const timestamp = new Date(currentDate);
          timestamp.setHours(8 + index * 3, Math.floor(Math.random() * 60));
          
          calorieActivities.push({
            timestamp,
            activity: activity.name,
            duration,
            caloriesBurned: calories,
            intensity: activity.intensity,
            heartRateAvg: baseRestingHR + (activity.intensity === 'high' ? 60 : activity.intensity === 'moderate' ? 30 : 10)
          });
          
          totalCaloriesBurned += calories;
        }
      });

      sampleData.push({
        date: new Date(currentDate),
        heartRate: {
          average: avgHeartRate,
          min: minHeartRate,
          max: maxHeartRate,
          resting: restingHeartRate,
          data: heartRateData
        },
        respiratoryRate: {
          average: avgRespiratoryRate,
          min: minRespiratoryRate,
          max: maxRespiratoryRate,
          data: respiratoryData
        },
        hrv: {
          average: avgHRV,
          trend: Math.random() > 0.5 ? 'improving' : Math.random() > 0.5 ? 'stable' : 'declining',
          data: hrvData
        },
        sleep: sleepSession,
        calories: {
          burned: totalCaloriesBurned,
          bmr: Math.round(baseBMR),
          activities: calorieActivities
        },
        stressLevel,
        recoveryScore
      });
    }

    await AsyncStorage.setItem(HealthDataManager.STORAGE_KEY, JSON.stringify(sampleData));
    await AsyncStorage.setItem(HealthDataManager.SAMPLE_DATA_KEY, 'true');
  }

  // Get health data for a specific date range
  async getHealthData(startDate: Date, endDate: Date): Promise<DailyHealthMetrics[]> {
    try {
      const dataString = await AsyncStorage.getItem(HealthDataManager.STORAGE_KEY);
      if (!dataString) {
        await this.generateSampleData();
        return this.getHealthData(startDate, endDate);
      }

      const allData: DailyHealthMetrics[] = JSON.parse(dataString).map((item: any) => ({
        ...item,
        date: new Date(item.date),
        heartRate: {
          ...item.heartRate,
          data: item.heartRate.data.map((hr: any) => ({
            ...hr,
            timestamp: new Date(hr.timestamp)
          }))
        },
        respiratoryRate: {
          ...item.respiratoryRate,
          data: item.respiratoryRate.data.map((rr: any) => ({
            ...rr,
            timestamp: new Date(rr.timestamp)
          }))
        },
        hrv: {
          ...item.hrv,
          data: item.hrv.data.map((hrv: any) => ({
            ...hrv,
            timestamp: new Date(hrv.timestamp)
          }))
        },
        sleep: item.sleep ? {
          ...item.sleep,
          date: new Date(item.sleep.date),
          bedTime: new Date(item.sleep.bedTime),
          sleepTime: new Date(item.sleep.sleepTime),
          wakeTime: new Date(item.sleep.wakeTime),
          stages: item.sleep.stages.map((stage: any) => ({
            ...stage,
            timestamp: new Date(stage.timestamp)
          }))
        } : null,
        // Ensure calories data exists for backward compatibility
        calories: item.calories || {
          burned: Math.round(1800 + Math.random() * 600), // Default calorie burn if missing
          bmr: Math.round(1600 + Math.random() * 400),
          activities: []
        }
      }));

      return allData.filter(item => 
        item.date >= startDate && item.date <= endDate
      );
    } catch (error) {
      console.error('Error getting health data:', error);
      return [];
    }
  }

  // Get latest health metrics
  async getLatestMetrics(): Promise<DailyHealthMetrics | null> {
    const today = new Date();
    const data = await this.getHealthData(today, today);
    return data.length > 0 ? data[0] : null;
  }

  // Calculate health trends
  async calculateTrends(period: 'week' | 'month' | 'quarter'): Promise<HealthTrends> {
    const endDate = new Date();
    const startDate = new Date();
    
    switch (period) {
      case 'week':
        startDate.setDate(endDate.getDate() - 7);
        break;
      case 'month':
        startDate.setDate(endDate.getDate() - 30);
        break;
      case 'quarter':
        startDate.setDate(endDate.getDate() - 90);
        break;
    }

    const data = await this.getHealthData(startDate, endDate);
    
    if (data.length === 0) {
      return this.getDefaultTrends(period);
    }

    // Calculate averages
    const avgHeartRate = data.reduce((sum, d) => sum + d.heartRate.average, 0) / data.length;
    const avgSleepDuration = data.filter(d => d.sleep).reduce((sum, d) => sum + (d.sleep?.totalSleepTime || 0), 0) / data.filter(d => d.sleep).length;
    const avgSleepEfficiency = data.filter(d => d.sleep).reduce((sum, d) => sum + (d.sleep?.sleepEfficiency || 0), 0) / data.filter(d => d.sleep).length;
    const avgSleepScore = data.filter(d => d.sleep).reduce((sum, d) => sum + (d.sleep?.sleepScore || 0), 0) / data.filter(d => d.sleep).length;
    const avgHRV = data.reduce((sum, d) => sum + d.hrv.average, 0) / data.length;
    const avgRecovery = data.reduce((sum, d) => sum + d.recoveryScore, 0) / data.length;

    // Calculate trends (simplified - compare first half to second half)
    const midPoint = Math.floor(data.length / 2);
    const firstHalf = data.slice(0, midPoint);
    const secondHalf = data.slice(midPoint);

    const firstHalfHR = firstHalf.reduce((sum, d) => sum + d.heartRate.average, 0) / firstHalf.length;
    const secondHalfHR = secondHalf.reduce((sum, d) => sum + d.heartRate.average, 0) / secondHalf.length;
    const hrTrend = secondHalfHR < firstHalfHR ? 'improving' : secondHalfHR > firstHalfHR ? 'declining' : 'stable';
    const hrChange = ((secondHalfHR - firstHalfHR) / firstHalfHR) * 100;

    const firstHalfHRV = firstHalf.reduce((sum, d) => sum + d.hrv.average, 0) / firstHalf.length;
    const secondHalfHRV = secondHalf.reduce((sum, d) => sum + d.hrv.average, 0) / secondHalf.length;
    const hrvTrend = secondHalfHRV > firstHalfHRV ? 'improving' : secondHalfHRV < firstHalfHRV ? 'declining' : 'stable';
    const hrvChange = ((secondHalfHRV - firstHalfHRV) / firstHalfHRV) * 100;

    const sleepFirst = firstHalf.filter(d => d.sleep);
    const sleepSecond = secondHalf.filter(d => d.sleep);
    const firstHalfSleep = sleepFirst.reduce((sum, d) => sum + (d.sleep?.sleepScore || 0), 0) / sleepFirst.length;
    const secondHalfSleep = sleepSecond.reduce((sum, d) => sum + (d.sleep?.sleepScore || 0), 0) / sleepSecond.length;
    const sleepTrend = secondHalfSleep > firstHalfSleep ? 'improving' : secondHalfSleep < firstHalfSleep ? 'declining' : 'stable';

    const firstHalfRecovery = firstHalf.reduce((sum, d) => sum + d.recoveryScore, 0) / firstHalf.length;
    const secondHalfRecovery = secondHalf.reduce((sum, d) => sum + d.recoveryScore, 0) / secondHalf.length;
    const recoveryTrend = secondHalfRecovery > firstHalfRecovery ? 'improving' : secondHalfRecovery < firstHalfRecovery ? 'declining' : 'stable';

    return {
      period,
      heartRate: {
        average: Math.round(avgHeartRate),
        trend: hrTrend,
        changePercent: Math.round(hrChange * 100) / 100
      },
      sleep: {
        averageDuration: Math.round(avgSleepDuration),
        averageEfficiency: Math.round(avgSleepEfficiency),
        averageScore: Math.round(avgSleepScore),
        trend: sleepTrend
      },
      hrv: {
        average: Math.round(avgHRV),
        trend: hrvTrend,
        changePercent: Math.round(hrvChange * 100) / 100
      },
      recovery: {
        average: Math.round(avgRecovery),
        trend: recoveryTrend
      }
    };
  }

  private getDefaultTrends(period: 'week' | 'month' | 'quarter'): HealthTrends {
    return {
      period,
      heartRate: {
        average: 72,
        trend: 'stable',
        changePercent: 0
      },
      sleep: {
        averageDuration: 450, // 7.5 hours
        averageEfficiency: 85,
        averageScore: 78,
        trend: 'stable'
      },
      hrv: {
        average: 35,
        trend: 'stable',
        changePercent: 0
      },
      recovery: {
        average: 75,
        trend: 'stable'
      }
    };
  }

  // Get health summary for voice assistant
  async getHealthSummary(): Promise<string> {
    const latest = await this.getLatestMetrics();
    const trends = await this.calculateTrends('week');

    if (!latest) {
      return "I don't have recent health data available. Please sync your health devices to get personalized insights.";
    }

    const parts = [];

    // Heart rate summary
    parts.push(`Your current resting heart rate is ${latest.heartRate.resting} BPM`);
    
    // Sleep summary
    if (latest.sleep) {
      const sleepHours = Math.floor(latest.sleep.totalSleepTime / 60);
      const sleepMinutes = latest.sleep.totalSleepTime % 60;
      parts.push(`Last night you slept ${sleepHours} hours and ${sleepMinutes} minutes with a sleep score of ${latest.sleep.sleepScore}`);
    }

    // HRV summary
    parts.push(`Your heart rate variability is ${latest.hrv.average}ms, indicating ${latest.stressLevel < 40 ? 'low' : latest.stressLevel < 70 ? 'moderate' : 'high'} stress levels`);

    // Recovery summary
    parts.push(`Your recovery score is ${latest.recoveryScore}%, suggesting you're ${latest.recoveryScore > 80 ? 'fully recovered' : latest.recoveryScore > 60 ? 'moderately recovered' : 'still recovering'}`);

    // Trends
    if (trends.heartRate.trend !== 'stable') {
      parts.push(`Your heart rate trend is ${trends.heartRate.trend} this week`);
    }

    return parts.join('. ') + '.';
  }

  // Specific query methods for AI to use

  // Get average heart rate for a specific period
  async getAverageHeartRate(period: 'week' | 'month'): Promise<{ average: number; trend: string; data: any[] }> {
    const endDate = new Date();
    const startDate = new Date();
    
    if (period === 'week') {
      startDate.setDate(endDate.getDate() - 7);
    } else {
      startDate.setDate(endDate.getDate() - 30);
    }

    const data = await this.getHealthData(startDate, endDate);
    
    if (data.length === 0) {
      return { average: 0, trend: 'no data', data: [] };
    }

    const averageHR = Math.round(data.reduce((sum, d) => sum + d.heartRate.average, 0) / data.length);
    const restingHR = Math.round(data.reduce((sum, d) => sum + d.heartRate.resting, 0) / data.length);
    
    // Calculate trend
    const midPoint = Math.floor(data.length / 2);
    const firstHalf = data.slice(0, midPoint);
    const secondHalf = data.slice(midPoint);
    
    const firstHalfAvg = firstHalf.reduce((sum, d) => sum + d.heartRate.average, 0) / firstHalf.length;
    const secondHalfAvg = secondHalf.reduce((sum, d) => sum + d.heartRate.average, 0) / secondHalf.length;
    
    let trend = 'stable';
    if (secondHalfAvg < firstHalfAvg - 2) trend = 'improving';
    else if (secondHalfAvg > firstHalfAvg + 2) trend = 'concerning';

    return {
      average: averageHR,
      trend,
      data: [
        { label: 'Average HR', value: averageHR },
        { label: 'Resting HR', value: restingHR },
        { label: 'Days tracked', value: data.length }
      ]
    };
  }

  // Compare sleep between two periods
  async compareSleepPeriods(): Promise<{ thisWeek: any; lastWeek: any; comparison: string }> {
    const today = new Date();
    
    // This week (last 7 days)
    const thisWeekStart = new Date();
    thisWeekStart.setDate(today.getDate() - 7);
    const thisWeekData = await this.getHealthData(thisWeekStart, today);
    
    // Last week (8-14 days ago)
    const lastWeekStart = new Date();
    lastWeekStart.setDate(today.getDate() - 14);
    const lastWeekEnd = new Date();
    lastWeekEnd.setDate(today.getDate() - 7);
    const lastWeekData = await this.getHealthData(lastWeekStart, lastWeekEnd);

    if (thisWeekData.length === 0 || lastWeekData.length === 0) {
      return {
        thisWeek: null,
        lastWeek: null,
        comparison: 'Insufficient data for comparison.'
      };
    }

    // Calculate averages for this week
    const thisWeekSleep = thisWeekData.filter(d => d.sleep);
    const thisWeekAvgDuration = thisWeekSleep.reduce((sum, d) => sum + (d.sleep?.totalSleepTime || 0), 0) / thisWeekSleep.length;
    const thisWeekAvgScore = thisWeekSleep.reduce((sum, d) => sum + (d.sleep?.sleepScore || 0), 0) / thisWeekSleep.length;
    const thisWeekAvgEfficiency = thisWeekSleep.reduce((sum, d) => sum + (d.sleep?.sleepEfficiency || 0), 0) / thisWeekSleep.length;

    // Calculate averages for last week
    const lastWeekSleep = lastWeekData.filter(d => d.sleep);
    const lastWeekAvgDuration = lastWeekSleep.reduce((sum, d) => sum + (d.sleep?.totalSleepTime || 0), 0) / lastWeekSleep.length;
    const lastWeekAvgScore = lastWeekSleep.reduce((sum, d) => sum + (d.sleep?.sleepScore || 0), 0) / lastWeekSleep.length;
    const lastWeekAvgEfficiency = lastWeekSleep.reduce((sum, d) => sum + (d.sleep?.sleepEfficiency || 0), 0) / lastWeekSleep.length;

    // Generate comparison analysis
    const durationDiff = thisWeekAvgDuration - lastWeekAvgDuration;
    const scoreDiff = thisWeekAvgScore - lastWeekAvgScore;
    const efficiencyDiff = thisWeekAvgEfficiency - lastWeekAvgEfficiency;

    let comparison = '';
    
    if (Math.abs(durationDiff) < 15 && Math.abs(scoreDiff) < 5 && Math.abs(efficiencyDiff) < 5) {
      comparison = 'Your sleep has been quite consistent between last week and this week.';
    } else if (durationDiff > 15 || scoreDiff > 5 || efficiencyDiff > 5) {
      comparison = 'Your sleep has improved this week compared to last week.';
    } else {
      comparison = 'Your sleep quality was better last week compared to this week.';
    }

    return {
      thisWeek: {
        avgDuration: Math.round(thisWeekAvgDuration),
        avgScore: Math.round(thisWeekAvgScore),
        avgEfficiency: Math.round(thisWeekAvgEfficiency),
        nights: thisWeekSleep.length
      },
      lastWeek: {
        avgDuration: Math.round(lastWeekAvgDuration),
        avgScore: Math.round(lastWeekAvgScore),
        avgEfficiency: Math.round(lastWeekAvgEfficiency),
        nights: lastWeekSleep.length
      },
      comparison
    };
  }

  // Get comprehensive health analysis for diet planning
  async getHealthAnalysisForDiet(): Promise<{
    heartRate: any;
    sleep: any;
    stress: any;
    recovery: any;
    recommendations: string[];
  }> {
    const endDate = new Date();
    const startDate = new Date();
    startDate.setDate(endDate.getDate() - 14); // Last 2 weeks

    const data = await this.getHealthData(startDate, endDate);

    if (data.length === 0) {
      return {
        heartRate: null,
        sleep: null,
        stress: null,
        recovery: null,
        recommendations: ['Insufficient data for personalized diet recommendations.']
      };
    }

    // Analyze heart rate
    const avgRestingHR = Math.round(data.reduce((sum, d) => sum + d.heartRate.resting, 0) / data.length);
    const hrvAvg = Math.round(data.reduce((sum, d) => sum + d.hrv.average, 0) / data.length);

    // Analyze sleep
    const sleepData = data.filter(d => d.sleep);
    const avgSleepScore = Math.round(sleepData.reduce((sum, d) => sum + (d.sleep?.sleepScore || 0), 0) / sleepData.length);
    const avgSleepDuration = Math.round(sleepData.reduce((sum, d) => sum + (d.sleep?.totalSleepTime || 0), 0) / sleepData.length);

    // Analyze stress and recovery
    const avgStress = Math.round(data.reduce((sum, d) => sum + d.stressLevel, 0) / data.length);
    const avgRecovery = Math.round(data.reduce((sum, d) => sum + d.recoveryScore, 0) / data.length);

    // Generate diet recommendations based on health data
    const recommendations: string[] = [];

    if (avgRestingHR > 75) {
      recommendations.push('Focus on heart-healthy foods: omega-3 rich fish, leafy greens, and berries to support cardiovascular health.');
    }

    if (avgSleepScore < 70) {
      recommendations.push('Include magnesium-rich foods like almonds, spinach, and dark chocolate to improve sleep quality.');
      recommendations.push('Avoid caffeine after 2 PM and limit heavy meals 3 hours before bedtime.');
    }

    if (avgStress > 60) {
      recommendations.push('Add stress-reducing foods: chamomile tea, dark leafy greens, and foods rich in B vitamins.');
      recommendations.push('Consider anti-inflammatory foods like turmeric, ginger, and fatty fish.');
    }

    if (avgRecovery < 70) {
      recommendations.push('Boost recovery with protein-rich foods: lean meats, eggs, quinoa, and Greek yogurt.');
      recommendations.push('Include antioxidant-rich foods: blueberries, pomegranates, and green tea.');
    }

    if (hrvAvg < 30) {
      recommendations.push('Support your nervous system with foods rich in potassium: bananas, avocados, and sweet potatoes.');
    }

    if (recommendations.length === 0) {
      recommendations.push('Your health metrics look good! Maintain a balanced diet with plenty of fruits, vegetables, lean proteins, and whole grains.');
    }

    return {
      heartRate: { resting: avgRestingHR, hrv: hrvAvg },
      sleep: { score: avgSleepScore, duration: avgSleepDuration },
      stress: { level: avgStress },
      recovery: { score: avgRecovery },
      recommendations
    };
  }

  // Clear all health data
  async clearHealthData(): Promise<void> {
    await AsyncStorage.removeItem(HealthDataManager.STORAGE_KEY);
    await AsyncStorage.removeItem(HealthDataManager.SAMPLE_DATA_KEY);
  }

  // Check if sample data exists
  async hasSampleData(): Promise<boolean> {
    const sampleDataFlag = await AsyncStorage.getItem(HealthDataManager.SAMPLE_DATA_KEY);
    return sampleDataFlag === 'true';
  }

  // Context and memory management
  async getUserContext(): Promise<UserContext | null> {
    try {
      const contextData = await AsyncStorage.getItem('userHealthContext');
      return contextData ? JSON.parse(contextData) : null;
    } catch (error) {
      console.error('Error retrieving user context:', error);
      return null;
    }
  }

  async updateUserContext(context: Partial<UserContext>): Promise<void> {
    try {
      const existingContext = await this.getUserContext();
      const updatedContext: UserContext = {
        preferences: {
          responseLength: 'moderate',
          focusAreas: ['sleep', 'fitness', 'nutrition'],
          timePreferences: {
            wakeTime: '06:30',
            bedTime: '22:30',
            workoutTime: '18:00'
          },
          sleepGoal: 8,
          exerciseGoal: 30,
          calorieGoal: 2000,
          stressManagement: 'moderate',
          ...existingContext?.preferences,
          ...context.preferences
        },
        behaviorPatterns: {
          commonQueries: [],
          queryTimes: {},
          engagementLevel: 'moderate',
          lastInteractionDate: new Date(),
          ...existingContext?.behaviorPatterns,
          ...context.behaviorPatterns
        },
        healthHistory: {
          chronicConditions: [],
          allergies: [],
          medications: [],
          injuries: [],
          ...existingContext?.healthHistory,
          ...context.healthHistory
        },
        lifestyle: {
          dietType: 'balanced',
          activityLevel: 'moderate',
          sleepSchedule: 'regular',
          workSchedule: 'standard',
          ...existingContext?.lifestyle,
          ...context.lifestyle
        },
        goals: {
          sleep: {
            targetDuration: 480, // 8 hours in minutes
            targetBedtime: '22:30',
            targetWakeTime: '06:30',
            targetScore: 80
          },
          fitness: {
            dailyCalorieTarget: 2000,
            weeklyExerciseMinutes: 150,
            stepsTarget: 10000
          },
          wellness: {
            maxStressLevel: 30,
            minRecoveryScore: 70,
            hydrationTarget: 8
          },
          ...existingContext?.goals,
          ...context.goals
        },
        achievements: {
          streaks: {
            sleepGoal: 0,
            exerciseGoal: 0,
            nutritionGoal: 0
          },
          milestones: [],
          ...existingContext?.achievements,
          ...context.achievements
        }
      };
      
      await AsyncStorage.setItem('userHealthContext', JSON.stringify(updatedContext));
    } catch (error) {
      console.error('Error updating user context:', error);
    }
  }

  async getHealthGoals(): Promise<HealthGoals | null> {
    try {
      const context = await this.getUserContext();
      return context?.goals || null;
    } catch (error) {
      console.error('Error retrieving health goals:', error);
      return null;
    }
  }

  async updateHealthGoals(goals: Partial<HealthGoals>): Promise<void> {
    try {
      const context = await this.getUserContext();
      if (context) {
        const updatedGoals: HealthGoals = {
          ...context.goals,
          ...goals
        };
        await this.updateUserContext({ goals: updatedGoals });
      }
    } catch (error) {
      console.error('Error updating health goals:', error);
    }
  }

  // Enhanced analytics with goal tracking
  async getGoalProgress(days: number = 7): Promise<any> {
    const endDate = new Date();
    const startDate = new Date();
    startDate.setDate(endDate.getDate() - days);
    
    const metrics = await this.getHealthData(startDate, endDate);
    const goals = await this.getHealthGoals();
    
    if (!goals || metrics.length === 0) return null;

    const recentMetrics = metrics.slice(-days);
    
    return {
      sleep: {
        goal: goals.sleep.targetDuration / 60, // Convert to hours
        average: recentMetrics.reduce((sum: number, m: DailyHealthMetrics) => sum + (m.sleep?.totalSleepTime || 0), 0) / recentMetrics.length / 60,
        achievement: Math.round((recentMetrics.filter((m: DailyHealthMetrics) => (m.sleep?.totalSleepTime || 0) >= goals.sleep.targetDuration).length / recentMetrics.length) * 100)
      },
      calories: {
        goal: goals.fitness.dailyCalorieTarget,
        averageBurned: recentMetrics.reduce((sum: number, m: DailyHealthMetrics) => sum + (m.calories?.burned || 0), 0) / recentMetrics.length,
        totalBurned: recentMetrics.reduce((sum: number, m: DailyHealthMetrics) => sum + (m.calories?.burned || 0), 0)
      },
      heartRate: {
        goal: 180, // Default max heart rate
        averageResting: recentMetrics.reduce((sum: number, m: DailyHealthMetrics) => sum + m.heartRate.resting, 0) / recentMetrics.length,
        maxRecorded: Math.max(...recentMetrics.map((m: DailyHealthMetrics) => m.heartRate.max))
      },
      stress: {
        goal: goals.wellness.maxStressLevel,
        averageLevel: recentMetrics.reduce((sum: number, m: DailyHealthMetrics) => sum + m.stressLevel, 0) / recentMetrics.length,
        trend: this.getStressTrend(recentMetrics)
      }
    };
  }

  private getStressTrend(metrics: DailyHealthMetrics[]): 'improving' | 'stable' | 'worsening' {
    if (metrics.length < 3) return 'stable';
    
    const recent = metrics.slice(-3).map(m => m.stressLevel);
    const earlier = metrics.slice(-6, -3).map(m => m.stressLevel);
    
    const recentAvg = recent.reduce((sum, val) => sum + val, 0) / recent.length;
    const earlierAvg = earlier.reduce((sum, val) => sum + val, 0) / earlier.length;
    
    const diff = recentAvg - earlierAvg;
    if (diff < -5) return 'improving';
    if (diff > 5) return 'worsening';
    return 'stable';
  }
}

export default new HealthDataManager();
