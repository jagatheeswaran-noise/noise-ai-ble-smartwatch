import AsyncStorage from '@react-native-async-storage/async-storage';
import { Alert } from 'react-native';

// Types for daily assistant features
export interface Reminder {
  id: string;
  title: string;
  description?: string;
  dateTime: Date;
  isCompleted: boolean;
  type: 'reminder' | 'alarm' | 'timer';
  duration?: number; // for timers (in seconds)
  recurring?: 'daily' | 'weekly' | 'monthly' | 'none';
}

export interface CalorieEntry {
  id: string;
  food: string;
  calories: number;
  quantity: string;
  timestamp: Date;
  meal: 'breakfast' | 'lunch' | 'dinner' | 'snack';
  confidence?: number; // AI confidence in calorie estimation
}

export interface WeatherData {
  temperature: number;
  condition: string;
  humidity: number;
  windSpeed: number;
  location: string;
  timestamp: Date;
  recommendation?: string;
}

class DailyAssistantService {
  private reminders: Reminder[] = [];
  private calorieEntries: CalorieEntry[] = [];
  private activeTimers: Map<string, NodeJS.Timeout> = new Map();

  constructor() {
    this.loadStoredData();
  }

  // ========== TIME MANAGEMENT ==========

  // Create a reminder
  async createReminder(title: string, dateTime: Date, description?: string, recurring: 'daily' | 'weekly' | 'monthly' | 'none' = 'none'): Promise<string> {
    const reminder: Reminder = {
      id: Date.now().toString(),
      title,
      description,
      dateTime,
      isCompleted: false,
      type: 'reminder',
      recurring
    };

    this.reminders.push(reminder);
    await this.saveReminders();
    this.scheduleNotification(reminder);
    
    console.log('📅 Reminder created:', reminder);
    return reminder.id;
  }

  // Create an alarm
  async createAlarm(title: string, dateTime: Date, recurring: 'daily' | 'weekly' | 'monthly' | 'none' = 'none'): Promise<string> {
    const alarm: Reminder = {
      id: Date.now().toString(),
      title,
      dateTime,
      isCompleted: false,
      type: 'alarm',
      recurring
    };

    this.reminders.push(alarm);
    await this.saveReminders();
    this.scheduleNotification(alarm);
    
    console.log('⏰ Alarm created:', alarm);
    return alarm.id;
  }

  // Create a timer
  async createTimer(title: string, durationMinutes: number): Promise<string> {
    const timer: Reminder = {
      id: Date.now().toString(),
      title,
      dateTime: new Date(Date.now() + (durationMinutes * 60 * 1000)),
      isCompleted: false,
      type: 'timer',
      duration: durationMinutes * 60,
      recurring: 'none'
    };

    this.reminders.push(timer);
    await this.saveReminders();
    this.startTimer(timer);
    
    console.log('⏲️ Timer created:', timer);
    return timer.id;
  }

  // Start a timer
  private startTimer(timer: Reminder) {
    if (!timer.duration) return;

    const timeoutId = setTimeout(() => {
      console.log('⏲️ Timer finished:', timer.title);
      Alert.alert('Timer Finished!', timer.title, [
        { text: 'OK', onPress: () => this.completeReminder(timer.id) }
      ]);
      this.activeTimers.delete(timer.id);
    }, timer.duration * 1000);

    this.activeTimers.set(timer.id, timeoutId);
  }

  // Schedule notification for reminder/alarm
  private scheduleNotification(reminder: Reminder) {
    const now = new Date();
    const timeUntil = reminder.dateTime.getTime() - now.getTime();

    if (timeUntil > 0) {
      setTimeout(() => {
        const message = reminder.type === 'alarm' ? '⏰ Alarm' : '📅 Reminder';
        Alert.alert(message, reminder.title, [
          { text: 'Dismiss', onPress: () => this.completeReminder(reminder.id) },
          { text: 'Snooze 5min', onPress: () => this.snoozeReminder(reminder.id, 5) }
        ]);
      }, timeUntil);
    }
  }

  // Complete a reminder/alarm/timer
  async completeReminder(id: string): Promise<void> {
    const reminder = this.reminders.find(r => r.id === id);
    if (reminder) {
      reminder.isCompleted = true;
      
      // Handle recurring reminders
      if (reminder.recurring !== 'none') {
        this.createRecurringReminder(reminder);
      }
      
      await this.saveReminders();
      
      // Clear timer if active
      if (this.activeTimers.has(id)) {
        clearTimeout(this.activeTimers.get(id)!);
        this.activeTimers.delete(id);
      }
    }
  }

  // Snooze a reminder
  async snoozeReminder(id: string, minutes: number): Promise<void> {
    const reminder = this.reminders.find(r => r.id === id);
    if (reminder) {
      reminder.dateTime = new Date(Date.now() + (minutes * 60 * 1000));
      await this.saveReminders();
      this.scheduleNotification(reminder);
    }
  }

  // Create recurring reminder
  private async createRecurringReminder(original: Reminder) {
    const nextDate = new Date(original.dateTime);
    
    switch (original.recurring) {
      case 'daily':
        nextDate.setDate(nextDate.getDate() + 1);
        break;
      case 'weekly':
        nextDate.setDate(nextDate.getDate() + 7);
        break;
      case 'monthly':
        nextDate.setMonth(nextDate.getMonth() + 1);
        break;
    }

    const newReminder: Reminder = {
      ...original,
      id: Date.now().toString(),
      dateTime: nextDate,
      isCompleted: false
    };

    this.reminders.push(newReminder);
    await this.saveReminders();
    this.scheduleNotification(newReminder);
  }

  // Get active reminders
  getActiveReminders(): Reminder[] {
    return this.reminders.filter(r => !r.isCompleted && r.dateTime > new Date());
  }

  // Get today's reminders
  getTodaysReminders(): Reminder[] {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const tomorrow = new Date(today);
    tomorrow.setDate(tomorrow.getDate() + 1);

    return this.reminders.filter(r => 
      r.dateTime >= today && r.dateTime < tomorrow
    );
  }

  // ========== WEATHER & ENVIRONMENT ==========

  // Get weather information (mock implementation - in real app, use weather API)
  async getWeatherInfo(location?: string): Promise<WeatherData> {
    try {
      // Mock weather data - replace with actual weather API call
      const mockWeather: WeatherData = {
        temperature: Math.round(Math.random() * 30 + 15), // 15-45°C
        condition: ['Sunny', 'Cloudy', 'Rainy', 'Partly Cloudy'][Math.floor(Math.random() * 4)],
        humidity: Math.round(Math.random() * 40 + 30), // 30-70%
        windSpeed: Math.round(Math.random() * 20 + 5), // 5-25 km/h
        location: location || 'Your Location',
        timestamp: new Date(),
        recommendation: this.getWeatherRecommendation()
      };

      console.log('🌤️ Weather data:', mockWeather);
      return mockWeather;
    } catch (error) {
      console.error('Error fetching weather:', error);
      throw new Error('Unable to fetch weather information');
    }
  }

  // Get weather-based recommendations
  private getWeatherRecommendation(): string {
    const recommendations = [
      'Perfect weather for outdoor activities!',
      'Stay hydrated in this warm weather.',
      'Great day for a walk or jog.',
      'Consider indoor workouts if it\'s too hot.',
      'Don\'t forget sunscreen if going outside.'
    ];
    return recommendations[Math.floor(Math.random() * recommendations.length)];
  }

  // ========== VOICE-BASED CALORIE TRACKING ==========

  // Process voice input for calorie tracking
  async processCalorieVoiceInput(voiceText: string): Promise<CalorieEntry | null> {
    try {
      const calorieInfo = this.parseCalorieFromVoice(voiceText);
      if (!calorieInfo) return null;

      const entry: CalorieEntry = {
        id: Date.now().toString(),
        food: calorieInfo.food,
        calories: calorieInfo.calories,
        quantity: calorieInfo.quantity,
        timestamp: new Date(),
        meal: this.detectMealTime(),
        confidence: calorieInfo.confidence
      };

      await this.addCalorieEntry(entry);
      console.log('🍎 Calorie entry added:', entry);
      return entry;
    } catch (error) {
      console.error('Error processing calorie voice input:', error);
      return null;
    }
  }

  // Parse calorie information from voice text
  private parseCalorieFromVoice(text: string): { food: string; calories: number; quantity: string; confidence: number } | null {
    const lowerText = text.toLowerCase();
    
    // Common food items with approximate calories
    const foodDatabase = {
      'apple': { calories: 80, unit: 'medium' },
      'banana': { calories: 105, unit: 'medium' },
      'rice': { calories: 130, unit: 'half cup' },
      'bread': { calories: 80, unit: 'slice' },
      'chicken': { calories: 165, unit: '100g' },
      'fish': { calories: 140, unit: '100g' },
      'egg': { calories: 70, unit: 'one' },
      'milk': { calories: 150, unit: 'cup' },
      'pizza': { calories: 285, unit: 'slice' },
      'burger': { calories: 540, unit: 'one' },
      'salad': { calories: 150, unit: 'bowl' },
      'pasta': { calories: 220, unit: 'cup' },
      'coffee': { calories: 5, unit: 'cup' },
      'tea': { calories: 2, unit: 'cup' },
      'water': { calories: 0, unit: 'glass' }
    };

    // Try to extract food items
    for (const [food, data] of Object.entries(foodDatabase)) {
      if (lowerText.includes(food)) {
        // Extract quantity if mentioned
        const quantityMatch = lowerText.match(/(\d+|\bone\b|\btwo\b|\bthree\b|\bhalf\b|\bquarter\b)/);
        let multiplier = 1;
        
        if (quantityMatch) {
          const qty = quantityMatch[1];
          if (qty === 'one') multiplier = 1;
          else if (qty === 'two') multiplier = 2;
          else if (qty === 'three') multiplier = 3;
          else if (qty === 'half') multiplier = 0.5;
          else if (qty === 'quarter') multiplier = 0.25;
          else multiplier = parseFloat(qty) || 1;
        }

        return {
          food,
          calories: Math.round(data.calories * multiplier),
          quantity: quantityMatch ? `${quantityMatch[1]} ${data.unit}` : data.unit,
          confidence: 0.8
        };
      }
    }

    // If no specific food found, try to extract any mentioned calories
    const calorieMatch = lowerText.match(/(\d+)\s*(calorie|cal|kcal)/);
    if (calorieMatch) {
      return {
        food: 'Custom food item',
        calories: parseInt(calorieMatch[1]),
        quantity: 'as mentioned',
        confidence: 0.6
      };
    }

    return null;
  }

  // Detect meal time based on current time
  private detectMealTime(): 'breakfast' | 'lunch' | 'dinner' | 'snack' {
    const hour = new Date().getHours();
    
    if (hour >= 6 && hour < 11) return 'breakfast';
    if (hour >= 11 && hour < 16) return 'lunch';
    if (hour >= 16 && hour < 22) return 'dinner';
    return 'snack';
  }

  // Add calorie entry
  async addCalorieEntry(entry: CalorieEntry): Promise<void> {
    this.calorieEntries.push(entry);
    await this.saveCalorieEntries();
  }

  // Get today's calorie intake
  getTodaysCalories(): { entries: CalorieEntry[]; totalCalories: number } {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const tomorrow = new Date(today);
    tomorrow.setDate(tomorrow.getDate() + 1);

    const todaysEntries = this.calorieEntries.filter(entry => 
      entry.timestamp >= today && entry.timestamp < tomorrow
    );

    const totalCalories = todaysEntries.reduce((sum, entry) => sum + entry.calories, 0);

    return { entries: todaysEntries, totalCalories };
  }

  // Get calorie breakdown by meal
  getCalorieBreakdown(): { breakfast: number; lunch: number; dinner: number; snack: number } {
    const { entries } = this.getTodaysCalories();
    
    return {
      breakfast: entries.filter(e => e.meal === 'breakfast').reduce((sum, e) => sum + e.calories, 0),
      lunch: entries.filter(e => e.meal === 'lunch').reduce((sum, e) => sum + e.calories, 0),
      dinner: entries.filter(e => e.meal === 'dinner').reduce((sum, e) => sum + e.calories, 0),
      snack: entries.filter(e => e.meal === 'snack').reduce((sum, e) => sum + e.calories, 0)
    };
  }

  // ========== QUICK FACTUAL INFORMATION ==========

  // Process general information queries
  getQuickFacts(query: string): string {
    const lowerQuery = query.toLowerCase();
    
    // Time-related queries
    if (lowerQuery.includes('time') || lowerQuery.includes('clock')) {
      return `Current time: ${new Date().toLocaleTimeString()}`;
    }
    
    if (lowerQuery.includes('date') || lowerQuery.includes('today')) {
      return `Today's date: ${new Date().toLocaleDateString('en-US', { 
        weekday: 'long', 
        year: 'numeric', 
        month: 'long', 
        day: 'numeric' 
      })}`;
    }

    // Health-related quick facts
    if (lowerQuery.includes('water') && lowerQuery.includes('daily')) {
      return 'Recommended daily water intake: 8-10 glasses (about 2-2.5 liters) for adults.';
    }
    
    if (lowerQuery.includes('steps') && lowerQuery.includes('daily')) {
      return 'Recommended daily steps: 8,000-10,000 steps for general health benefits.';
    }
    
    if (lowerQuery.includes('sleep') && lowerQuery.includes('hours')) {
      return 'Recommended sleep: 7-9 hours per night for adults (18-64 years).';
    }

    // Calorie-related facts
    if (lowerQuery.includes('calorie') && (lowerQuery.includes('daily') || lowerQuery.includes('need'))) {
      return 'Average daily calorie needs: 2,000-2,500 for men, 1,600-2,000 for women (varies by age, activity level).';
    }

    return 'I can help with time, date, health facts, and more. Try asking about daily water intake, sleep hours, or calorie needs.';
  }

  // ========== DATA PERSISTENCE ==========

  // Save reminders to storage
  private async saveReminders(): Promise<void> {
    try {
      await AsyncStorage.setItem('daily_assistant_reminders', JSON.stringify(this.reminders));
    } catch (error) {
      console.error('Error saving reminders:', error);
    }
  }

  // Save calorie entries to storage
  private async saveCalorieEntries(): Promise<void> {
    try {
      await AsyncStorage.setItem('daily_assistant_calories', JSON.stringify(this.calorieEntries));
    } catch (error) {
      console.error('Error saving calorie entries:', error);
    }
  }

  // Load stored data
  private async loadStoredData(): Promise<void> {
    try {
      // Load reminders
      const remindersData = await AsyncStorage.getItem('daily_assistant_reminders');
      if (remindersData) {
        this.reminders = JSON.parse(remindersData).map((r: any) => ({
          ...r,
          dateTime: new Date(r.dateTime)
        }));
        
        // Reschedule active reminders
        this.reminders.filter(r => !r.isCompleted && r.dateTime > new Date()).forEach(r => {
          if (r.type === 'timer' && r.duration) {
            this.startTimer(r);
          } else {
            this.scheduleNotification(r);
          }
        });
      }

      // Load calorie entries
      const caloriesData = await AsyncStorage.getItem('daily_assistant_calories');
      if (caloriesData) {
        this.calorieEntries = JSON.parse(caloriesData).map((e: any) => ({
          ...e,
          timestamp: new Date(e.timestamp)
        }));
      }
    } catch (error) {
      console.error('Error loading stored data:', error);
    }
  }

  // Clear old data (run daily cleanup)
  async cleanupOldData(): Promise<void> {
    const thirtyDaysAgo = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000);
    
    // Remove old completed reminders
    this.reminders = this.reminders.filter(r => 
      !r.isCompleted || r.dateTime > thirtyDaysAgo
    );
    
    // Keep calorie entries for 90 days
    const ninetyDaysAgo = new Date(Date.now() - 90 * 24 * 60 * 60 * 1000);
    this.calorieEntries = this.calorieEntries.filter(e => 
      e.timestamp > ninetyDaysAgo
    );
    
    await this.saveReminders();
    await this.saveCalorieEntries();
  }
}

// Export singleton instance
export const dailyAssistant = new DailyAssistantService();
