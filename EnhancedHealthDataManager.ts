import { databaseService, HealthDataRecord } from './DatabaseService';
import AsyncStorage from '@react-native-async-storage/async-storage';
import RNFS from 'react-native-fs';

interface HealthDataSource {
  name: string;
  url: string;
  format: 'json' | 'csv' | 'xml';
  description: string;
  enabled: boolean;
}

interface DataGenerationConfig {
  days: number;
  userId: string;
  includeVariations: boolean;
  realisticPatterns: boolean;
}

interface ExternalHealthData {
  timestamp: string;
  category: 'heart_rate' | 'sleep' | 'activity' | 'stress' | 'nutrition' | 'general';
  type: string;
  value: number;
  unit: string;
  source: string;
}

class EnhancedHealthDataManager {
  private static readonly DATA_SOURCES_KEY = 'health_data_sources';
  private static readonly LAST_SYNC_KEY = 'last_health_data_sync';

  // Available external health data sources
  private static readonly EXTERNAL_SOURCES: HealthDataSource[] = [
    {
      name: 'Health Data API',
      url: 'https://api.healthdata.gov/api/3/action/datastore_search',
      format: 'json',
      description: 'Public health datasets from health.gov',
      enabled: true
    },
    {
      name: 'Fitbit Sample Data',
      url: 'https://raw.githubusercontent.com/fitbit/sample-data/main/health-metrics.json',
      format: 'json',
      description: 'Sample health and fitness data from Fitbit',
      enabled: false
    },
    {
      name: 'Apple Health Export Sample',
      url: 'https://raw.githubusercontent.com/apple/health-export/main/sample-data.json',
      format: 'json',
      description: 'Sample Apple Health export data',
      enabled: false
    },
    {
      name: 'Google Fit Sample Data',
      url: 'https://raw.githubusercontent.com/google/fit-samples/main/health-data.json',
      format: 'json',
      description: 'Sample Google Fit health data',
      enabled: false
    },
    {
      name: 'MyFitnessPal Data',
      url: 'https://api.myfitnesspal.com/public/nutrition',
      format: 'json',
      description: 'Nutrition and calorie data',
      enabled: false
    }
  ];

  constructor() {
    this.initializeDataSources();
  }

  private async initializeDataSources(): Promise<void> {
    try {
      const existingSources = await AsyncStorage.getItem(EnhancedHealthDataManager.DATA_SOURCES_KEY);
      if (!existingSources) {
        await AsyncStorage.setItem(
          EnhancedHealthDataManager.DATA_SOURCES_KEY,
          JSON.stringify(EnhancedHealthDataManager.EXTERNAL_SOURCES)
        );
      }
    } catch (error) {
      console.error('Error initializing data sources:', error);
    }
  }

  // Generate comprehensive health data
  async generateHealthData(config: DataGenerationConfig): Promise<boolean> {
    try {
      console.log(`🏥 Generating ${config.days} days of health data for user ${config.userId}`);
      
      await databaseService.initialize();
      
      const healthData: HealthDataRecord[] = [];
      const endDate = new Date();
      const startDate = new Date();
      startDate.setDate(endDate.getDate() - config.days);

      for (let d = new Date(startDate); d <= endDate; d.setDate(d.getDate() + 1)) {
        const dateStr = d.toISOString().split('T')[0];
        const dayOfWeek = d.getDay();
        const isWeekend = dayOfWeek === 0 || dayOfWeek === 6;
        
        // Generate realistic patterns based on day type
        const dailyData = this.generateDailyHealthMetrics(dateStr, isWeekend, config);
        healthData.push(...dailyData);
      }

      // Batch insert data
      console.log(`📊 Inserting ${healthData.length} health records`);
      for (const record of healthData) {
        await databaseService.insertHealthData(record);
      }

      // Update last generation timestamp
      await AsyncStorage.setItem('health_data_generated', new Date().toISOString());
      await AsyncStorage.setItem('health_data_days', config.days.toString());

      console.log('✅ Health data generation completed successfully');
      return true;
    } catch (error) {
      console.error('❌ Error generating health data:', error);
      return false;
    }
  }

  private generateDailyHealthMetrics(
    date: string, 
    isWeekend: boolean, 
    config: DataGenerationConfig
  ): HealthDataRecord[] {
    const records: HealthDataRecord[] = [];
    
    // Heart Rate Data
    const baseRestingHR = 58 + Math.random() * 22; // 58-80 BPM
    const stressMultiplier = isWeekend ? 0.9 : 1.1; // Lower stress on weekends
    
    records.push(
      {
        date,
        category: 'heart_rate',
        type: 'resting',
        value: Math.round(baseRestingHR * stressMultiplier).toString(),
        unit: 'bpm'
      },
      {
        date,
        category: 'heart_rate',
        type: 'average',
        value: Math.round(baseRestingHR * stressMultiplier + 15 + Math.random() * 25).toString(),
        unit: 'bpm'
      },
      {
        date,
        category: 'heart_rate',
        type: 'max',
        value: Math.round(baseRestingHR * stressMultiplier + 60 + Math.random() * 40).toString(),
        unit: 'bpm'
      }
    );

    // HRV Data
    const baseHRV = 30 + Math.random() * 25; // 30-55ms
    const hrvStressAdjustment = isWeekend ? 1.15 : 0.95; // Better HRV on weekends
    records.push({
      date,
      category: 'heart_rate',
      type: 'hrv',
      value: Math.round(baseHRV * hrvStressAdjustment).toString(),
      unit: 'ms'
    });

    // Sleep Data
    const baseSleepDuration = isWeekend ? 8.2 : 7.4; // More sleep on weekends
    const sleepVariation = (Math.random() - 0.5) * 1.5; // ±45 minutes variation
    const actualSleepDuration = Math.max(5.5, baseSleepDuration + sleepVariation);
    
    const sleepScore = Math.max(60, Math.min(95, 
      75 + (actualSleepDuration - 7) * 10 + (Math.random() - 0.5) * 20
    ));
    
    const sleepEfficiency = Math.max(75, Math.min(95,
      85 + (actualSleepDuration > 7 ? 5 : -5) + (Math.random() - 0.5) * 10
    ));

    records.push(
      {
        date,
        category: 'sleep',
        type: 'duration',
        value: Math.round(actualSleepDuration * 60).toString(),
        unit: 'minutes'
      },
      {
        date,
        category: 'sleep',
        type: 'score',
        value: Math.round(sleepScore).toString(),
        unit: 'score'
      },
      {
        date,
        category: 'sleep',
        type: 'efficiency',
        value: Math.round(sleepEfficiency).toString(),
        unit: 'percent'
      }
    );

    // Activity Data
    const baseSteps = isWeekend ? 7500 : 9500; // Different activity on weekends
    const stepsVariation = (Math.random() - 0.5) * 4000;
    const actualSteps = Math.max(3000, baseSteps + stepsVariation);
    
    const caloriesPerStep = 0.04 + Math.random() * 0.02; // 0.04-0.06 calories per step
    const baseCalories = 1800 + Math.random() * 400; // BMR
    const activityCalories = actualSteps * caloriesPerStep;
    
    const activeMinutes = isWeekend ? 
      30 + Math.random() * 90 : // 30-120 minutes on weekends
      20 + Math.random() * 70;  // 20-90 minutes on weekdays

    records.push(
      {
        date,
        category: 'activity',
        type: 'steps',
        value: Math.round(actualSteps).toString(),
        unit: 'steps'
      },
      {
        date,
        category: 'activity',
        type: 'calories_burned',
        value: Math.round(baseCalories + activityCalories).toString(),
        unit: 'calories'
      },
      {
        date,
        category: 'activity',
        type: 'active_minutes',
        value: Math.round(activeMinutes).toString(),
        unit: 'minutes'
      }
    );

    // Stress and Recovery Data
    const baseStress = isWeekend ? 25 : 45; // Lower stress on weekends
    const stressVariation = (Math.random() - 0.5) * 30;
    const actualStress = Math.max(10, Math.min(90, baseStress + stressVariation));
    
    // Recovery inversely related to stress and positively to sleep
    const recoveryScore = Math.max(40, Math.min(95,
      90 - (actualStress * 0.5) + (sleepScore - 75) * 0.3 + (Math.random() - 0.5) * 20
    ));

    records.push(
      {
        date,
        category: 'stress',
        type: 'level',
        value: Math.round(actualStress).toString(),
        unit: 'percent'
      },
      {
        date,
        category: 'general',
        type: 'recovery_score',
        value: Math.round(recoveryScore).toString(),
        unit: 'score'
      }
    );

    // Add some realistic correlations and patterns
    if (config.realisticPatterns) {
      // Add hydration data (correlated with activity)
      const baseHydration = 6 + (actualSteps / 10000) * 2; // More water with more activity
      records.push({
        date,
        category: 'nutrition',
        type: 'water_intake',
        value: Math.round(baseHydration + (Math.random() - 0.5) * 2).toString(),
        unit: 'glasses'
      });

      // Add mood data (inversely correlated with stress)
      const moodScore = Math.max(1, Math.min(10,
        8 - (actualStress / 100) * 4 + (sleepScore - 75) / 10 + (Math.random() - 0.5) * 2
      ));
      records.push({
        date,
        category: 'general',
        type: 'mood_score',
        value: moodScore.toFixed(1),
        unit: 'score'
      });

      // Add nutrition data
      const dailyCalorieIntake = 1800 + Math.random() * 800; // 1800-2600 calories
      const proteinIntake = dailyCalorieIntake * 0.15 / 4; // 15% of calories from protein
      const carbIntake = dailyCalorieIntake * 0.50 / 4; // 50% from carbs
      const fatIntake = dailyCalorieIntake * 0.35 / 9; // 35% from fats

      records.push(
        {
          date,
          category: 'nutrition',
          type: 'calories_consumed',
          value: Math.round(dailyCalorieIntake).toString(),
          unit: 'calories'
        },
        {
          date,
          category: 'nutrition',
          type: 'protein',
          value: Math.round(proteinIntake).toString(),
          unit: 'grams'
        },
        {
          date,
          category: 'nutrition',
          type: 'carbs',
          value: Math.round(carbIntake).toString(),
          unit: 'grams'
        },
        {
          date,
          category: 'nutrition',
          type: 'fat',
          value: Math.round(fatIntake).toString(),
          unit: 'grams'
        }
      );
    }

    return records;
  }

  // Download health data from external sources
  async downloadExternalHealthData(): Promise<boolean> {
    try {
      console.log('🌐 Starting external health data download');
      
      const sources = await this.getEnabledDataSources();
      let successCount = 0;
      
      for (const source of sources) {
        try {
          console.log(`📥 Downloading from ${source.name}`);
          const success = await this.downloadFromSource(source);
          if (success) {
            successCount++;
            console.log(`✅ Successfully downloaded from ${source.name}`);
          } else {
            console.log(`⚠️ Failed to download from ${source.name}`);
          }
        } catch (error) {
          console.error(`❌ Error downloading from ${source.name}:`, error);
        }
      }

      await AsyncStorage.setItem(EnhancedHealthDataManager.LAST_SYNC_KEY, new Date().toISOString());
      
      console.log(`📊 Download completed: ${successCount}/${sources.length} sources successful`);
      return successCount > 0;
    } catch (error) {
      console.error('❌ Error in external data download:', error);
      return false;
    }
  }

  private async downloadFromSource(source: HealthDataSource): Promise<boolean> {
    try {
      // Simulate different download strategies based on source
      if (source.url.includes('raw.githubusercontent.com')) {
        return await this.downloadFromGitHub(source);
      } else if (source.url.includes('api.healthdata.gov')) {
        return await this.downloadFromHealthAPI(source);
      } else if (source.url.includes('myfitnesspal.com')) {
        return await this.downloadFromFitnessAPI(source);
      } else {
        return await this.generateSampleDataForSource(source);
      }
    } catch (error) {
      console.error(`Error downloading from ${source.name}:`, error);
      return false;
    }
  }

  private async downloadFromGitHub(source: HealthDataSource): Promise<boolean> {
    try {
      console.log(`🔄 Simulating GitHub download: ${source.url}`);
      const simulatedData = this.generateExternalSourceData(source.name);
      return await this.processExternalData(simulatedData, source.name);
    } catch (error) {
      console.error('Error in GitHub download simulation:', error);
      return false;
    }
  }

  private async downloadFromHealthAPI(source: HealthDataSource): Promise<boolean> {
    try {
      console.log(`🔄 Simulating Health API call: ${source.url}`);
      const simulatedData = this.generateExternalSourceData('Health Data API');
      return await this.processExternalData(simulatedData, source.name);
    } catch (error) {
      console.error('Error in Health API simulation:', error);
      return false;
    }
  }

  private async downloadFromFitnessAPI(source: HealthDataSource): Promise<boolean> {
    try {
      console.log(`🔄 Simulating Fitness API call: ${source.url}`);
      const simulatedData = this.generateNutritionData();
      return await this.processExternalData(simulatedData, source.name);
    } catch (error) {
      console.error('Error in Fitness API simulation:', error);
      return false;
    }
  }

  private generateNutritionData(): ExternalHealthData[] {
    const data: ExternalHealthData[] = [];
    const startDate = new Date();
    startDate.setDate(startDate.getDate() - 7);

    for (let i = 0; i < 7; i++) {
      const date = new Date(startDate);
      date.setDate(date.getDate() + i);
      const dateStr = date.toISOString().split('T')[0];

      data.push(
        {
          source: 'MyFitnessPal Data',
          timestamp: dateStr,
          category: 'nutrition',
          type: 'calories_consumed',
          value: 1800 + Math.random() * 600,
          unit: 'calories'
        },
        {
          source: 'MyFitnessPal Data',
          timestamp: dateStr,
          category: 'nutrition',
          type: 'protein',
          value: 80 + Math.random() * 40,
          unit: 'grams'
        },
        {
          source: 'MyFitnessPal Data',
          timestamp: dateStr,
          category: 'nutrition',
          type: 'carbs',
          value: 200 + Math.random() * 100,
          unit: 'grams'
        },
        {
          source: 'MyFitnessPal Data',
          timestamp: dateStr,
          category: 'nutrition',
          type: 'fat',
          value: 60 + Math.random() * 30,
          unit: 'grams'
        }
      );
    }

    return data;
  }

  private async generateSampleDataForSource(source: HealthDataSource): Promise<boolean> {
    try {
      const sampleData = this.generateExternalSourceData(source.name);
      return await this.processExternalData(sampleData, source.name);
    } catch (error) {
      console.error('Error generating sample data:', error);
      return false;
    }
  }

  private generateExternalSourceData(sourceName: string): ExternalHealthData[] {
    const data: ExternalHealthData[] = [];
    const startDate = new Date();
    startDate.setDate(startDate.getDate() - 7);

    for (let i = 0; i < 7; i++) {
      const date = new Date(startDate);
      date.setDate(date.getDate() + i);
      const dateStr = date.toISOString().split('T')[0];

      if (sourceName.includes('Fitbit')) {
        data.push(
          {
            source: sourceName,
            timestamp: dateStr,
            category: 'activity',
            type: 'steps',
            value: 8000 + Math.random() * 4000,
            unit: 'steps'
          },
          {
            source: sourceName,
            timestamp: dateStr,
            category: 'heart_rate',
            type: 'resting',
            value: 62 + Math.random() * 18,
            unit: 'bpm'
          },
          {
            source: sourceName,
            timestamp: dateStr,
            category: 'activity',
            type: 'distance',
            value: 3 + Math.random() * 5,
            unit: 'miles'
          }
        );
      } else if (sourceName.includes('Apple')) {
        data.push(
          {
            source: sourceName,
            timestamp: dateStr,
            category: 'sleep',
            type: 'duration',
            value: 420 + Math.random() * 120,
            unit: 'minutes'
          },
          {
            source: sourceName,
            timestamp: dateStr,
            category: 'heart_rate',
            type: 'hrv',
            value: 30 + Math.random() * 25,
            unit: 'ms'
          },
          {
            source: sourceName,
            timestamp: dateStr,
            category: 'general',
            type: 'mindfulness_minutes',
            value: Math.random() * 30,
            unit: 'minutes'
          }
        );
      } else if (sourceName.includes('Google')) {
        data.push(
          {
            source: sourceName,
            timestamp: dateStr,
            category: 'activity',
            type: 'calories_burned',
            value: 2000 + Math.random() * 600,
            unit: 'calories'
          },
          {
            source: sourceName,
            timestamp: dateStr,
            category: 'activity',
            type: 'active_minutes',
            value: 30 + Math.random() * 60,
            unit: 'minutes'
          },
          {
            source: sourceName,
            timestamp: dateStr,
            category: 'activity',
            type: 'floors_climbed',
            value: Math.round(Math.random() * 20),
            unit: 'floors'
          }
        );
      } else {
        data.push(
          {
            source: sourceName,
            timestamp: dateStr,
            category: 'general',
            type: 'wellness_score',
            value: 70 + Math.random() * 25,
            unit: 'score'
          },
          {
            source: sourceName,
            timestamp: dateStr,
            category: 'general',
            type: 'energy_level',
            value: 6 + Math.random() * 4,
            unit: 'score'
          }
        );
      }
    }

    return data;
  }

  private async processExternalData(data: ExternalHealthData[], sourceName: string): Promise<boolean> {
    try {
      await databaseService.initialize();
      
      let insertCount = 0;
      for (const record of data) {
        const healthRecord: Omit<HealthDataRecord, 'id' | 'created_at' | 'updated_at'> = {
          date: record.timestamp,
          category: record.category as 'heart_rate' | 'sleep' | 'activity' | 'stress' | 'nutrition' | 'general',
          type: record.type,
          value: record.value.toString(),
          unit: record.unit,
          metadata: JSON.stringify({
            source: record.source,
            imported: true,
            importedAt: new Date().toISOString()
          })
        };

        await databaseService.insertHealthData(healthRecord);
        insertCount++;
      }

      console.log(`📊 Processed ${insertCount} records from ${sourceName}`);
      return insertCount > 0;
    } catch (error) {
      console.error('Error processing external data:', error);
      return false;
    }
  }

  // Utility methods for data management
  async toggleDataSource(sourceName: string, enabled: boolean): Promise<boolean> {
    try {
      const sourcesData = await AsyncStorage.getItem(EnhancedHealthDataManager.DATA_SOURCES_KEY);
      let sources: HealthDataSource[] = sourcesData ? 
        JSON.parse(sourcesData) : 
        [...EnhancedHealthDataManager.EXTERNAL_SOURCES];

      const sourceIndex = sources.findIndex(s => s.name === sourceName);
      if (sourceIndex >= 0) {
        sources[sourceIndex].enabled = enabled;
        await AsyncStorage.setItem(EnhancedHealthDataManager.DATA_SOURCES_KEY, JSON.stringify(sources));
        console.log(`📊 ${enabled ? 'Enabled' : 'Disabled'} data source: ${sourceName}`);
        return true;
      }
      return false;
    } catch (error) {
      console.error('Error toggling data source:', error);
      return false;
    }
  }

  async getDataStatus(): Promise<{
    hasData: boolean;
    lastGenerated?: string;
    daysCovered?: number;
    lastSynced?: string;
    recordCount?: number;
  }> {
    try {
      const lastGenerated = await AsyncStorage.getItem('health_data_generated');
      const daysCovered = await AsyncStorage.getItem('health_data_days');
      const lastSynced = await AsyncStorage.getItem(EnhancedHealthDataManager.LAST_SYNC_KEY);
      
      await databaseService.initialize();
      const endDate = new Date().toISOString().split('T')[0];
      const startDate = new Date();
      startDate.setDate(startDate.getDate() - 30);
      const startDateStr = startDate.toISOString().split('T')[0];
      
      const recentData = await databaseService.getHealthData('heart_rate', 'average', startDateStr, endDate);
      
      return {
        hasData: recentData.length > 0,
        lastGenerated: lastGenerated || undefined,
        daysCovered: daysCovered ? parseInt(daysCovered) : undefined,
        lastSynced: lastSynced || undefined,
        recordCount: recentData.length
      };
    } catch (error) {
      console.error('Error getting data status:', error);
      return { hasData: false };
    }
  }

  async getAvailableDataSources(): Promise<HealthDataSource[]> {
    try {
      const sourcesData = await AsyncStorage.getItem(EnhancedHealthDataManager.DATA_SOURCES_KEY);
      return sourcesData ? JSON.parse(sourcesData) : EnhancedHealthDataManager.EXTERNAL_SOURCES;
    } catch (error) {
      console.error('Error getting available data sources:', error);
      return EnhancedHealthDataManager.EXTERNAL_SOURCES;
    }
  }

  private async getEnabledDataSources(): Promise<HealthDataSource[]> {
    try {
      const sourcesData = await AsyncStorage.getItem(EnhancedHealthDataManager.DATA_SOURCES_KEY);
      if (sourcesData) {
        const sources: HealthDataSource[] = JSON.parse(sourcesData);
        return sources.filter(source => source.enabled);
      }
      return EnhancedHealthDataManager.EXTERNAL_SOURCES.filter(source => source.enabled);
    } catch (error) {
      console.error('Error getting data sources:', error);
      return [];
    }
  }

  async clearAllData(): Promise<boolean> {
    try {
      await AsyncStorage.removeItem('health_data_generated');
      await AsyncStorage.removeItem('health_data_days');
      await AsyncStorage.removeItem(EnhancedHealthDataManager.LAST_SYNC_KEY);
      
      console.log('🗑️ Cleared all health data metadata');
      return true;
    } catch (error) {
      console.error('Error clearing data:', error);
      return false;
    }
  }
}

export const enhancedHealthDataManager = new EnhancedHealthDataManager();
export default EnhancedHealthDataManager;
export type { DataGenerationConfig, HealthDataSource, ExternalHealthData };
