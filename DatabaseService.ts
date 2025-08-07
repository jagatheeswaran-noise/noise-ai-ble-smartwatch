import SQLite from 'react-native-sqlite-2';
import AsyncStorage from '@react-native-async-storage/async-storage';

export interface HealthDataRecord {
  id?: number;
  date: string;
  category: 'heart_rate' | 'sleep' | 'activity' | 'stress' | 'nutrition' | 'general';
  type: string; // Specific type within category
  value: number | string;
  unit?: string;
  metadata?: string; // JSON string for additional data
  created_at?: string;
  updated_at?: string;
}

export interface HealthResponse {
  id?: number;
  query_type: string;
  query_keywords: string;
  response_template: string;
  requires_data: boolean;
  data_requirements?: string; // JSON string
  created_at?: string;
  updated_at?: string;
}

export interface UserProfile {
  id?: number;
  user_id: string;
  preferences: string; // JSON string
  goals: string; // JSON string
  medical_history: string; // JSON string
  created_at?: string;
  updated_at?: string;
}

class DatabaseService {
  private db: any = null;
  private isInitialized: boolean = false;

  async initialize(): Promise<void> {
    if (this.isInitialized) return;

    try {
      this.db = SQLite.openDatabase('noise_ai.db', '1.0', '', 1);
      await this.createTables();
      await this.seedDefaultData();
      this.isInitialized = true;
      console.log('Database initialized successfully');
    } catch (error) {
      console.error('Error initializing database:', error);
      throw error;
    }
  }

  private async createTables(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.db.transaction((tx: any) => {
        // Health data table
        tx.executeSql(`
          CREATE TABLE IF NOT EXISTS health_data (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            date TEXT NOT NULL,
            category TEXT NOT NULL,
            type TEXT NOT NULL,
            value TEXT NOT NULL,
            unit TEXT,
            metadata TEXT,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
          )
        `);

        // Health responses table for dynamic response generation
        tx.executeSql(`
          CREATE TABLE IF NOT EXISTS health_responses (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            query_type TEXT NOT NULL UNIQUE,
            query_keywords TEXT NOT NULL,
            response_template TEXT NOT NULL,
            requires_data BOOLEAN DEFAULT TRUE,
            data_requirements TEXT,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
          )
        `);

        // User profiles table
        tx.executeSql(`
          CREATE TABLE IF NOT EXISTS user_profiles (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id TEXT NOT NULL UNIQUE,
            preferences TEXT NOT NULL,
            goals TEXT,
            medical_history TEXT,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
          )
        `);

        // Indexes for better performance
        tx.executeSql(`CREATE INDEX IF NOT EXISTS idx_health_data_date ON health_data(date)`);
        tx.executeSql(`CREATE INDEX IF NOT EXISTS idx_health_data_category ON health_data(category)`);
        tx.executeSql(`CREATE INDEX IF NOT EXISTS idx_health_responses_type ON health_responses(query_type)`);
      }, reject, resolve);
    });
  }

  private async seedDefaultData(): Promise<void> {
    try {
      // Check if we've already seeded data
      const seeded = await AsyncStorage.getItem('database_seeded');
      if (seeded === 'true') return;

      await this.seedHealthData();
      await this.seedHealthResponses();
      await this.seedUserProfile();

      await AsyncStorage.setItem('database_seeded', 'true');
      console.log('Database seeded with default data');
    } catch (error) {
      console.error('Error seeding database:', error);
    }
  }

  private async seedHealthData(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.db.transaction((tx: any) => {
        const endDate = new Date();
        const startDate = new Date();
        startDate.setDate(endDate.getDate() - 30);

        // Generate 30 days of realistic health data
        for (let d = new Date(startDate); d <= endDate; d.setDate(d.getDate() + 1)) {
          const dateStr = d.toISOString().split('T')[0];
          
          // Heart rate data
          const restingHR = 60 + Math.random() * 20;
          const avgHR = restingHR + 15 + Math.random() * 25;
          const maxHR = avgHR + 40 + Math.random() * 30;
          
          tx.executeSql(
            'INSERT INTO health_data (date, category, type, value, unit) VALUES (?, ?, ?, ?, ?)',
            [dateStr, 'heart_rate', 'resting', Math.round(restingHR).toString(), 'bpm']
          );
          tx.executeSql(
            'INSERT INTO health_data (date, category, type, value, unit) VALUES (?, ?, ?, ?, ?)',
            [dateStr, 'heart_rate', 'average', Math.round(avgHR).toString(), 'bpm']
          );
          tx.executeSql(
            'INSERT INTO health_data (date, category, type, value, unit) VALUES (?, ?, ?, ?, ?)',
            [dateStr, 'heart_rate', 'max', Math.round(maxHR).toString(), 'bpm']
          );
          
          // Sleep data
          const sleepDuration = 6.5 + Math.random() * 2; // 6.5-8.5 hours
          const sleepScore = 70 + Math.random() * 25; // 70-95 score
          const sleepEfficiency = 80 + Math.random() * 15; // 80-95%
          
          tx.executeSql(
            'INSERT INTO health_data (date, category, type, value, unit) VALUES (?, ?, ?, ?, ?)',
            [dateStr, 'sleep', 'duration', (sleepDuration * 60).toString(), 'minutes']
          );
          tx.executeSql(
            'INSERT INTO health_data (date, category, type, value, unit) VALUES (?, ?, ?, ?, ?)',
            [dateStr, 'sleep', 'score', Math.round(sleepScore).toString(), 'score']
          );
          tx.executeSql(
            'INSERT INTO health_data (date, category, type, value, unit) VALUES (?, ?, ?, ?, ?)',
            [dateStr, 'sleep', 'efficiency', Math.round(sleepEfficiency).toString(), 'percent']
          );
          
          // Activity data
          const steps = 6000 + Math.random() * 6000; // 6000-12000 steps
          const calories = 1800 + Math.random() * 800; // 1800-2600 calories
          const activeMinutes = 30 + Math.random() * 60; // 30-90 minutes
          
          tx.executeSql(
            'INSERT INTO health_data (date, category, type, value, unit) VALUES (?, ?, ?, ?, ?)',
            [dateStr, 'activity', 'steps', Math.round(steps).toString(), 'steps']
          );
          tx.executeSql(
            'INSERT INTO health_data (date, category, type, value, unit) VALUES (?, ?, ?, ?, ?)',
            [dateStr, 'activity', 'calories_burned', Math.round(calories).toString(), 'calories']
          );
          tx.executeSql(
            'INSERT INTO health_data (date, category, type, value, unit) VALUES (?, ?, ?, ?, ?)',
            [dateStr, 'activity', 'active_minutes', Math.round(activeMinutes).toString(), 'minutes']
          );
          
          // Stress and HRV data
          const stressLevel = 20 + Math.random() * 60; // 20-80 stress level
          const hrv = 25 + Math.random() * 30; // 25-55 ms HRV
          const recoveryScore = 60 + Math.random() * 35; // 60-95 recovery
          
          tx.executeSql(
            'INSERT INTO health_data (date, category, type, value, unit) VALUES (?, ?, ?, ?, ?)',
            [dateStr, 'stress', 'level', Math.round(stressLevel).toString(), 'percent']
          );
          tx.executeSql(
            'INSERT INTO health_data (date, category, type, value, unit) VALUES (?, ?, ?, ?, ?)',
            [dateStr, 'heart_rate', 'hrv', Math.round(hrv).toString(), 'ms']
          );
          tx.executeSql(
            'INSERT INTO health_data (date, category, type, value, unit) VALUES (?, ?, ?, ?, ?)',
            [dateStr, 'general', 'recovery_score', Math.round(recoveryScore).toString(), 'score']
          );
        }
      }, reject, resolve);
    });
  }

  private async seedHealthResponses(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.db.transaction((tx: any) => {
        const responses = [
          {
            query_type: 'heart_rate_average',
            query_keywords: 'heart rate,average,bpm,last week,this week',
            response_template: 'Your average heart rate over the last {{period}} was {{average}} BPM. {{trend_analysis}} Your resting heart rate averaged {{resting}} BPM. {{health_advice}}',
            requires_data: true,
            data_requirements: JSON.stringify({
              categories: ['heart_rate'],
              types: ['average', 'resting'],
              period_days: 7
            })
          },
          {
            query_type: 'sleep_comparison',
            query_keywords: 'sleep,compare,last week,this week,duration,quality',
            response_template: 'Sleep Comparison:\n\nLast Week: {{last_week_duration}}h {{last_week_minutes}}m avg ({{last_week_score}}/100 quality)\nThis Week: {{this_week_duration}}h {{this_week_minutes}}m avg ({{this_week_score}}/100 quality)\nChange: {{duration_change}}h per night\n\n{{sleep_analysis}}',
            requires_data: true,
            data_requirements: JSON.stringify({
              categories: ['sleep'],
              types: ['duration', 'score', 'efficiency'],
              period_days: 14
            })
          },
          {
            query_type: 'sleep_last_night',
            query_keywords: 'sleep,last night,yesterday,duration,quality,deep sleep,rem',
            response_template: 'Last Night\'s Sleep:\n- Total Sleep: {{hours}}h {{minutes}}m\n- Sleep Quality: {{score}}/100\n- Sleep Efficiency: {{efficiency}}%\n- Deep Sleep: ~{{deep_sleep}}h\n- REM Sleep: ~{{rem_sleep}}h\n\n{{sleep_advice}}',
            requires_data: true,
            data_requirements: JSON.stringify({
              categories: ['sleep'],
              types: ['duration', 'score', 'efficiency'],
              period_days: 1
            })
          },
          {
            query_type: 'stress_level',
            query_keywords: 'stress,anxiety,stress level,hrv,heart rate variability',
            response_template: 'Your Stress & Recovery Data:\n- Stress Level: {{stress_level}}/100\n- Heart Rate Variability: {{hrv}}ms\n- Recovery Score: {{recovery_score}}/100\n\n{{stress_analysis}}',
            requires_data: true,
            data_requirements: JSON.stringify({
              categories: ['stress', 'heart_rate', 'general'],
              types: ['level', 'hrv', 'recovery_score'],
              period_days: 7
            })
          },
          {
            query_type: 'activity_summary',
            query_keywords: 'activity,steps,calories,exercise,workout,fitness',
            response_template: 'Your Activity Summary:\n- Average Steps: {{avg_steps}} per day\n- Calories Burned: {{avg_calories}} per day\n- Active Minutes: {{avg_active_minutes}} per day\n\n{{activity_analysis}}',
            requires_data: true,
            data_requirements: JSON.stringify({
              categories: ['activity'],
              types: ['steps', 'calories_burned', 'active_minutes'],
              period_days: 7
            })
          },
          {
            query_type: 'general_health',
            query_keywords: 'health,overall,summary,how am i doing',
            response_template: 'Your Health Overview:\n- Heart Rate: {{avg_hr}} BPM (Resting: {{resting_hr}} BPM)\n- Sleep: {{avg_sleep}}h avg ({{sleep_score}}/100 quality)\n- Activity: {{avg_steps}} steps daily\n- Stress Level: {{stress_level}}/100\n- Recovery: {{recovery_score}}/100\n\n{{general_advice}}',
            requires_data: true,
            data_requirements: JSON.stringify({
              categories: ['heart_rate', 'sleep', 'activity', 'stress', 'general'],
              types: ['average', 'resting', 'duration', 'score', 'steps', 'level', 'recovery_score'],
              period_days: 7
            })
          }
        ];

        responses.forEach(response => {
          tx.executeSql(
            'INSERT OR REPLACE INTO health_responses (query_type, query_keywords, response_template, requires_data, data_requirements) VALUES (?, ?, ?, ?, ?)',
            [response.query_type, response.query_keywords, response.response_template, response.requires_data, response.data_requirements]
          );
        });
      }, reject, resolve);
    });
  }

  private async seedUserProfile(): Promise<void> {
    return new Promise((resolve, reject) => {
      const defaultProfile = {
        preferences: {
          responseLength: 'moderate',
          focusAreas: ['sleep', 'fitness', 'stress'],
          timePreferences: {
            wakeTime: '07:00',
            bedTime: '23:00',
            workoutTime: '18:00'
          }
        },
        goals: {
          sleep: {
            targetDuration: 480, // 8 hours in minutes
            targetScore: 85
          },
          fitness: {
            dailySteps: 10000,
            weeklyExerciseMinutes: 150,
            dailyCalories: 2000
          },
          wellness: {
            maxStressLevel: 40,
            minRecoveryScore: 75
          }
        },
        medical_history: {
          conditions: [],
          allergies: [],
          medications: []
        }
      };

      this.db.transaction((tx: any) => {
        tx.executeSql(
          'INSERT OR REPLACE INTO user_profiles (user_id, preferences, goals, medical_history) VALUES (?, ?, ?, ?)',
          [
            'default_user',
            JSON.stringify(defaultProfile.preferences),
            JSON.stringify(defaultProfile.goals),
            JSON.stringify(defaultProfile.medical_history)
          ]
        );
      }, reject, resolve);
    });
  }

  // Get health data by category and date range
  async getHealthData(
    category: string,
    type?: string,
    startDate?: string,
    endDate?: string,
    limit?: number
  ): Promise<HealthDataRecord[]> {
    return new Promise((resolve, reject) => {
      let query = 'SELECT * FROM health_data WHERE category = ?';
      const params: any[] = [category];

      if (type) {
        query += ' AND type = ?';
        params.push(type);
      }

      if (startDate) {
        query += ' AND date >= ?';
        params.push(startDate);
      }

      if (endDate) {
        query += ' AND date <= ?';
        params.push(endDate);
      }

      query += ' ORDER BY date DESC';

      if (limit) {
        query += ' LIMIT ?';
        params.push(limit);
      }

      this.db.transaction((tx: any) => {
        tx.executeSql(
          query,
          params,
          (_: any, { rows }: any) => {
            const results: HealthDataRecord[] = [];
            for (let i = 0; i < rows.length; i++) {
              results.push(rows.item(i));
            }
            resolve(results);
          },
          (_: any, error: any) => reject(error)
        );
      });
    });
  }

  // Get health data by date range - enhanced version for intent classifier
  async getHealthDataByDateRange(
    startDate: string,
    endDate: string,
    category?: string,
    specificTypes?: string[]
  ): Promise<HealthDataRecord[]> {
    return new Promise((resolve, reject) => {
      let query = 'SELECT * FROM health_data WHERE date >= ? AND date <= ?';
      const params: any[] = [startDate, endDate];

      if (category) {
        query += ' AND category = ?';
        params.push(category);
      }

      if (specificTypes && specificTypes.length > 0) {
        const placeholders = specificTypes.map(() => '?').join(',');
        query += ` AND type IN (${placeholders})`;
        params.push(...specificTypes);
      }

      query += ' ORDER BY date DESC, created_at DESC';

      console.log('🔍 DB QUERY:', query);
      console.log('🔍 DB PARAMS:', params);

      this.db.transaction((tx: any) => {
        tx.executeSql(
          query,
          params,
          (_: any, { rows }: any) => {
            const results: HealthDataRecord[] = [];
            for (let i = 0; i < rows.length; i++) {
              results.push(rows.item(i));
            }
            console.log('🔍 DB RESULTS COUNT:', results.length);
            console.log('🔍 DB RESULTS SAMPLE:', results.slice(0, 2));
            resolve(results);
          },
          (_: any, error: any) => {
            console.error('🔍 DB ERROR:', error);
            reject(error);
          }
        );
      });
    });
  }

  // Get response template by query type
  async getResponseTemplate(queryType: string): Promise<HealthResponse | null> {
    return new Promise((resolve, reject) => {
      this.db.transaction((tx: any) => {
        tx.executeSql(
          'SELECT * FROM health_responses WHERE query_type = ?',
          [queryType],
          (_: any, { rows }: any) => {
            if (rows.length > 0) {
              resolve(rows.item(0));
            } else {
              resolve(null);
            }
          },
          (_: any, error: any) => reject(error)
        );
      });
    });
  }

  // Find best matching response template based on keywords
  async findBestResponseTemplate(userInput: string): Promise<HealthResponse | null> {
    return new Promise((resolve, reject) => {
      this.db.transaction((tx: any) => {
        tx.executeSql(
          'SELECT *, (LENGTH(query_keywords) - LENGTH(REPLACE(LOWER(query_keywords), LOWER(?), ""))) as relevance_score FROM health_responses ORDER BY relevance_score DESC LIMIT 1',
          [userInput.toLowerCase()],
          (_: any, { rows }: any) => {
            if (rows.length > 0 && rows.item(0).relevance_score > 0) {
              resolve(rows.item(0));
            } else {
              resolve(null);
            }
          },
          (_: any, error: any) => reject(error)
        );
      });
    });
  }

  // Get user profile
  async getUserProfile(userId: string = 'default_user'): Promise<UserProfile | null> {
    return new Promise((resolve, reject) => {
      this.db.transaction((tx: any) => {
        tx.executeSql(
          'SELECT * FROM user_profiles WHERE user_id = ?',
          [userId],
          (_: any, { rows }: any) => {
            if (rows.length > 0) {
              resolve(rows.item(0));
            } else {
              resolve(null);
            }
          },
          (_: any, error: any) => reject(error)
        );
      });
    });
  }

  // Insert new health data
  async insertHealthData(data: Omit<HealthDataRecord, 'id' | 'created_at' | 'updated_at'>): Promise<void> {
    return new Promise((resolve, reject) => {
      this.db.transaction((tx: any) => {
        tx.executeSql(
          'INSERT INTO health_data (date, category, type, value, unit, metadata) VALUES (?, ?, ?, ?, ?, ?)',
          [data.date, data.category, data.type, data.value, data.unit || null, data.metadata || null],
          () => {
            console.log('✅ INSERTED HEALTH DATA:', data.date, data.category, data.type);
            resolve();
          },
          (_: any, error: any) => {
            console.error('❌ INSERT ERROR:', error, data);
            reject(error);
          }
        );
      });
    });
  }

  // Calculate averages for health metrics
  async calculateAverages(
    category: string,
    type: string,
    days: number = 7
  ): Promise<{ average: number; count: number; latest: number | null }> {
    return new Promise((resolve, reject) => {
      const startDate = new Date();
      startDate.setDate(startDate.getDate() - days);
      const startDateStr = startDate.toISOString().split('T')[0];

      this.db.transaction((tx: any) => {
        tx.executeSql(
          'SELECT AVG(CAST(value AS REAL)) as average, COUNT(*) as count, MAX(CAST(value AS REAL)) as latest FROM health_data WHERE category = ? AND type = ? AND date >= ?',
          [category, type, startDateStr],
          (_: any, { rows }: any) => {
            const result = rows.item(0);
            resolve({
              average: result.average || 0,
              count: result.count || 0,
              latest: result.latest || null
            });
          },
          (_: any, error: any) => reject(error)
        );
      });
    });
  }

  // Clean up old data (keep only last 90 days)
  async cleanupOldData(): Promise<void> {
    return new Promise((resolve, reject) => {
      const cutoffDate = new Date();
      cutoffDate.setDate(cutoffDate.getDate() - 90);
      const cutoffDateStr = cutoffDate.toISOString().split('T')[0];

      this.db.transaction((tx: any) => {
        tx.executeSql(
          'DELETE FROM health_data WHERE date < ?',
          [cutoffDateStr],
          () => resolve(),
          (_: any, error: any) => reject(error)
        );
      });
    });
  }

  // Debug method to check database contents
  async checkDatabaseContents(): Promise<any> {
    return new Promise((resolve, reject) => {
      this.db.transaction((tx: any) => {
        tx.executeSql(
          'SELECT COUNT(*) as total_records FROM health_data',
          [],
          (_: any, { rows }: any) => {
            const totalCount = rows.item(0).total_records;
            
            tx.executeSql(
              'SELECT category, COUNT(*) as count FROM health_data GROUP BY category',
              [],
              (_: any, { rows: categoryRows }: any) => {
                const categories: any = {};
                for (let i = 0; i < categoryRows.length; i++) {
                  const row = categoryRows.item(i);
                  categories[row.category] = row.count;
                }
                
                tx.executeSql(
                  'SELECT date, category, type FROM health_data ORDER BY date DESC LIMIT 5',
                  [],
                  (_: any, { rows: sampleRows }: any) => {
                    const samples: any[] = [];
                    for (let i = 0; i < sampleRows.length; i++) {
                      samples.push(sampleRows.item(i));
                    }
                    
                    const result = {
                      totalRecords: totalCount,
                      categoryCounts: categories,
                      recentSamples: samples
                    };
                    console.log('📊 DATABASE CONTENTS:', result);
                    resolve(result);
                  },
                  (_: any, error: any) => reject(error)
                );
              },
              (_: any, error: any) => reject(error)
            );
          },
          (_: any, error: any) => reject(error)
        );
      });
    });
  }

  // Close database connection
  async close(): Promise<void> {
    if (this.db) {
      this.db.close();
      this.db = null;
      this.isInitialized = false;
    }
  }
}

export const databaseService = new DatabaseService();
export default DatabaseService;
