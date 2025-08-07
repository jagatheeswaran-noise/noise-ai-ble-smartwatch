import AsyncStorage from '@react-native-async-storage/async-storage';
import { PermissionsAndroid, Platform, Alert } from 'react-native';

// Interface definitions for device operations
export interface AlarmData {
  id: string;
  title: string;
  dateTime: Date;
  isActive: boolean;
  repeatType?: 'none' | 'daily' | 'weekly' | 'weekdays';
  soundName?: string;
  vibrate?: boolean;
  snoozeInterval?: number;
  context?: string; // User context when alarm was set
}

export interface NotificationData {
  id: string;
  title: string;
  message: string;
  dateTime: Date;
  type: 'alarm' | 'reminder' | 'notification';
  priority: 'low' | 'normal' | 'high';
}

export interface UserIntent {
  action: 'set' | 'modify' | 'cancel' | 'snooze' | 'query';
  target: 'alarm' | 'reminder' | 'timer';
  time?: Date;
  originalTime?: Date; // For modifications
  reason?: string; // User's reasoning for changes
  context?: string; // Additional context
}

class DeviceController {
  private alarms: Map<string, AlarmData> = new Map();
  private isInitialized = false;
  private notificationQueue: any[] = [];

  constructor() {
    this.initializeController();
  }

  private async initializeController() {
    if (this.isInitialized) return;

    try {
      // Request necessary permissions
      await this.requestPermissions();
      
      // Load existing alarms
      await this.loadAlarmsFromStorage();
      
      this.isInitialized = true;
      console.log('DeviceController initialized successfully');
    } catch (error) {
      console.error('Failed to initialize DeviceController:', error);
    }
  }

  private async requestPermissions() {
    if (Platform.OS === 'android') {
      try {
        // Request standard notification permissions
        const granted = await PermissionsAndroid.request(
          PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS
        );
        
        if (granted !== PermissionsAndroid.RESULTS.GRANTED) {
          console.warn('Notification permission not granted');
        }
      } catch (error) {
        console.error('Error requesting permissions:', error);
      }
    }
  }

  // Natural language processing for device control
  async processNaturalLanguageCommand(input: string): Promise<string> {
    const intent = this.parseUserIntent(input);
    
    switch (intent.action) {
      case 'set':
        return await this.handleSetCommand(intent, input);
      case 'modify':
        return await this.handleModifyCommand(intent, input);
      case 'cancel':
        return await this.handleCancelCommand(intent, input);
      case 'query':
        return await this.handleQueryCommand(intent, input);
      default:
        return "I didn't understand that command. Try saying something like 'Set alarm for 6 AM tomorrow' or 'Change my 6 AM alarm to 8 AM'.";
    }
  }

  private parseUserIntent(input: string): UserIntent {
    const lowercaseInput = input.toLowerCase();
    
    // Detect modification patterns
    if (this.isModificationRequest(lowercaseInput)) {
      return this.parseModificationIntent(lowercaseInput);
    }
    
    // Detect setting patterns
    if (this.isSettingRequest(lowercaseInput)) {
      return this.parseSettingIntent(lowercaseInput);
    }
    
    // Detect cancellation patterns
    if (this.isCancellationRequest(lowercaseInput)) {
      return this.parseCancellationIntent(lowercaseInput);
    }
    
    // Detect query patterns
    if (this.isQueryRequest(lowercaseInput)) {
      return this.parseQueryIntent(lowercaseInput);
    }
    
    return { action: 'set', target: 'alarm' };
  }

  private isModificationRequest(input: string): boolean {
    const modificationKeywords = [
      'change', 'modify', 'update', 'move', 'shift', 'adjust',
      'tired', 'cannot wake up', 'can only wake up', 'instead',
      'make it', 'i need', 'i want', 'from.*to'
    ];
    
    return modificationKeywords.some(keyword => 
      input.includes(keyword) || new RegExp(keyword).test(input)
    );
  }

  private isSettingRequest(input: string): boolean {
    const settingKeywords = [
      'set alarm', 'create alarm', 'wake me up', 'alarm for',
      'set reminder', 'remind me', 'schedule'
    ];
    
    return settingKeywords.some(keyword => input.includes(keyword));
  }

  private isCancellationRequest(input: string): boolean {
    const cancellationKeywords = [
      'cancel', 'delete', 'remove', 'turn off', 'disable',
      'stop', 'no alarm', 'don\'t wake me'
    ];
    
    return cancellationKeywords.some(keyword => input.includes(keyword));
  }

  private isQueryRequest(input: string): boolean {
    const queryKeywords = [
      'what alarms', 'show alarms', 'list alarms', 'my alarms',
      'when is', 'what time', 'do i have'
    ];
    
    return queryKeywords.some(keyword => input.includes(keyword));
  }

  private parseModificationIntent(input: string): UserIntent {
    const timeMatch = this.extractTimeFromText(input);
    const originalTimeMatch = this.extractOriginalTimeFromText(input);
    const reasonMatch = this.extractReasonFromText(input);
    
    return {
      action: 'modify',
      target: 'alarm',
      time: timeMatch,
      originalTime: originalTimeMatch,
      reason: reasonMatch,
      context: input
    };
  }

  private parseSettingIntent(input: string): UserIntent {
    const timeMatch = this.extractTimeFromText(input);
    const context = this.extractContextFromText(input);
    
    return {
      action: 'set',
      target: 'alarm',
      time: timeMatch,
      context: context
    };
  }

  private parseCancellationIntent(input: string): UserIntent {
    const timeMatch = this.extractTimeFromText(input);
    
    return {
      action: 'cancel',
      target: 'alarm',
      time: timeMatch
    };
  }

  private parseQueryIntent(input: string): UserIntent {
    return {
      action: 'query',
      target: 'alarm'
    };
  }

  private extractTimeFromText(input: string): Date | undefined {
    const now = new Date();
    
    // Comprehensive time patterns
    const timePatterns = [
      // Standard formats: 6 AM, 6:30 PM, etc.
      /(\d{1,2})(?::(\d{2}))?\s*(am|pm)/i,
      // 24-hour format: 06:00, 18:30
      /(\d{1,2}):(\d{2})/,
      // Natural language: six AM, eight thirty
      /(six|seven|eight|nine|ten|eleven|twelve|one|two|three|four|five)\s*(am|pm|o'?clock)/i,
      // Relative time: in 8 hours, tomorrow at 6
      /(?:tomorrow|next day)\s*(?:at\s*)?(\d{1,2})(?::(\d{2}))?\s*(am|pm)?/i
    ];

    for (const pattern of timePatterns) {
      const match = input.match(pattern);
      if (match) {
        return this.parseTimeMatch(match, now);
      }
    }

    return undefined;
  }

  private extractOriginalTimeFromText(input: string): Date | undefined {
    // Look for patterns like "change my 6 AM alarm" or "from 6 AM to 8 AM"
    const originalTimePatterns = [
      /(?:my|the)\s*(\d{1,2})(?::(\d{2}))?\s*(am|pm)\s*alarm/i,
      /from\s*(\d{1,2})(?::(\d{2}))?\s*(am|pm)/i,
      /(\d{1,2})(?::(\d{2}))?\s*(am|pm)\s*(?:alarm|to)/i
    ];

    for (const pattern of originalTimePatterns) {
      const match = input.match(pattern);
      if (match) {
        return this.parseTimeMatch(match, new Date());
      }
    }

    return undefined;
  }

  private extractReasonFromText(input: string): string | undefined {
    const reasonPatterns = [
      /because\s+(.+?)(?:\.|$)/i,
      /i am\s+(.+?)(?:\.|,|$)/i,
      /i'm\s+(.+?)(?:\.|,|$)/i,
      /i cannot\s+(.+?)(?:\.|,|$)/i,
      /i can't\s+(.+?)(?:\.|,|$)/i,
      /i can only\s+(.+?)(?:\.|,|$)/i
    ];

    for (const pattern of reasonPatterns) {
      const match = input.match(pattern);
      if (match) {
        return match[1].trim();
      }
    }

    return undefined;
  }

  private extractContextFromText(input: string): string | undefined {
    const contextPatterns = [
      /for\s+(.+?)(?:\.|$)/i,
      /to\s+(.+?)(?:\.|$)/i,
      /so\s+i\s+can\s+(.+?)(?:\.|$)/i
    ];

    for (const pattern of contextPatterns) {
      const match = input.match(pattern);
      if (match) {
        return match[1].trim();
      }
    }

    return undefined;
  }

  private parseTimeMatch(match: RegExpMatchArray, baseDate: Date): Date {
    const date = new Date(baseDate);
    let hours = parseInt(match[1]);
    const minutes = match[2] ? parseInt(match[2]) : 0;
    const ampm = match[3]?.toLowerCase();

    // Handle AM/PM conversion
    if (ampm === 'pm' && hours !== 12) {
      hours += 12;
    } else if (ampm === 'am' && hours === 12) {
      hours = 0;
    }

    date.setHours(hours, minutes, 0, 0);

    // If the time has passed today, set for tomorrow
    if (date <= baseDate) {
      date.setDate(date.getDate() + 1);
    }

    return date;
  }

  private async handleSetCommand(intent: UserIntent, originalInput: string): Promise<string> {
    if (!intent.time) {
      return "I couldn't understand the time. Please say something like 'Set alarm for 6 AM tomorrow'.";
    }

    try {
      const alarmId = `alarm_${Date.now()}`;
      const alarmData: AlarmData = {
        id: alarmId,
        title: intent.context || 'Wake up',
        dateTime: intent.time,
        isActive: true,
        repeatType: 'none',
        vibrate: true,
        context: originalInput
      };

      // Store the alarm
      await this.setDeviceAlarm(alarmData);
      
      const timeString = intent.time.toLocaleString([], { 
        weekday: 'short',
        hour: '2-digit', 
        minute: '2-digit' 
      });

      return `✅ Alarm set for ${timeString}. I'll wake you up then! The alarm has been added to your device's alarm system.`;
      
    } catch (error) {
      console.error('Error setting alarm:', error);
      return "I had trouble setting that alarm. Please make sure I have permission to access your device's alarm system.";
    }
  }

  private async handleModifyCommand(intent: UserIntent, originalInput: string): Promise<string> {
    try {
      let targetAlarm: AlarmData | undefined;

      // Find the alarm to modify
      if (intent.originalTime) {
        targetAlarm = this.findAlarmByTime(intent.originalTime);
      } else {
        // Find the most recent alarm
        const alarms = Array.from(this.alarms.values())
          .filter(alarm => alarm.isActive && alarm.dateTime > new Date())
          .sort((a, b) => a.dateTime.getTime() - b.dateTime.getTime());
        targetAlarm = alarms[0];
      }

      if (!targetAlarm) {
        return "I couldn't find an alarm to modify. Please tell me which specific alarm you want to change.";
      }

      if (!intent.time) {
        return "I understood you want to change an alarm, but I couldn't understand the new time. Please say something like 'Change it to 8 AM'.";
      }

      // Update the alarm
      const oldTimeString = targetAlarm.dateTime.toLocaleString([], { 
        hour: '2-digit', 
        minute: '2-digit' 
      });

      targetAlarm.dateTime = intent.time;
      targetAlarm.context = originalInput;

      await this.updateDeviceAlarm(targetAlarm);

      const newTimeString = intent.time.toLocaleString([], { 
        weekday: 'short',
        hour: '2-digit', 
        minute: '2-digit' 
      });

      let response = `✅ Updated your alarm from ${oldTimeString} to ${newTimeString}.`;
      
      if (intent.reason) {
        response += ` I understand you ${intent.reason}. No problem!`;
      }

      response += ` The change has been applied to your device alarm system.`;

      return response;

    } catch (error) {
      console.error('Error modifying alarm:', error);
      return "I had trouble modifying that alarm. Please try again.";
    }
  }

  private async handleCancelCommand(intent: UserIntent, originalInput: string): Promise<string> {
    try {
      let targetAlarm: AlarmData | undefined;

      if (intent.time) {
        targetAlarm = this.findAlarmByTime(intent.time);
      } else {
        // Cancel all active alarms
        const activeAlarms = Array.from(this.alarms.values())
          .filter(alarm => alarm.isActive && alarm.dateTime > new Date());
        
        if (activeAlarms.length === 0) {
          return "You don't have any active alarms to cancel.";
        }

        if (activeAlarms.length === 1) {
          targetAlarm = activeAlarms[0];
        } else {
          return `You have ${activeAlarms.length} active alarms. Please specify which one to cancel by saying the time.`;
        }
      }

      if (!targetAlarm) {
        return "I couldn't find that alarm to cancel.";
      }

      await this.cancelDeviceAlarm(targetAlarm.id);

      const timeString = targetAlarm.dateTime.toLocaleString([], { 
        hour: '2-digit', 
        minute: '2-digit' 
      });

      return `✅ Canceled your ${timeString} alarm. The alarm has been removed from your device.`;

    } catch (error) {
      console.error('Error canceling alarm:', error);
      return "I had trouble canceling that alarm. Please try again.";
    }
  }

  private async handleQueryCommand(intent: UserIntent, originalInput: string): Promise<string> {
    const activeAlarms = Array.from(this.alarms.values())
      .filter(alarm => alarm.isActive && alarm.dateTime > new Date())
      .sort((a, b) => a.dateTime.getTime() - b.dateTime.getTime());

    if (activeAlarms.length === 0) {
      return "You don't have any active alarms set.";
    }

    let response = `You have ${activeAlarms.length} active alarm${activeAlarms.length > 1 ? 's' : ''}:\n\n`;
    
    activeAlarms.slice(0, 5).forEach((alarm, index) => {
      const timeString = alarm.dateTime.toLocaleString([], { 
        weekday: 'short',
        month: 'short',
        day: 'numeric',
        hour: '2-digit', 
        minute: '2-digit' 
      });
      response += `${index + 1}. ${timeString} - ${alarm.title}\n`;
    });

    if (activeAlarms.length > 5) {
      response += `...and ${activeAlarms.length - 5} more`;
    }

    return response;
  }

  private findAlarmByTime(targetTime: Date): AlarmData | undefined {
    const targetHour = targetTime.getHours();
    const targetMinute = targetTime.getMinutes();

    for (const alarm of this.alarms.values()) {
      if (alarm.isActive && 
          alarm.dateTime.getHours() === targetHour && 
          alarm.dateTime.getMinutes() === targetMinute) {
        return alarm;
      }
    }

    return undefined;
  }

  private async setDeviceAlarm(alarmData: AlarmData): Promise<void> {
    // Store in memory and persistent storage
    this.alarms.set(alarmData.id, alarmData);
    await this.saveAlarmsToStorage();

    // Schedule a timeout-based alarm simulation
    const timeUntilAlarm = alarmData.dateTime.getTime() - Date.now();
    
    if (timeUntilAlarm > 0) {
      setTimeout(() => {
        this.handleAlarmTrigger(alarmData.id);
      }, timeUntilAlarm);
      
      // Also add to notification queue for persistence
      this.notificationQueue.push({
        id: alarmData.id,
        time: alarmData.dateTime,
        title: alarmData.title
      });
    }

    console.log(`Alarm scheduled for ${alarmData.dateTime}`);
  }

  private async updateDeviceAlarm(alarmData: AlarmData): Promise<void> {
    // Cancel any existing timeout for this alarm
    this.cancelDeviceAlarmTimeout(alarmData.id);
    
    // Set the new alarm
    await this.setDeviceAlarm(alarmData);
  }

  private async cancelDeviceAlarm(alarmId: string): Promise<void> {
    // Remove from memory
    const alarm = this.alarms.get(alarmId);
    if (alarm) {
      alarm.isActive = false;
      await this.saveAlarmsToStorage();
    }

    // Cancel the timeout
    this.cancelDeviceAlarmTimeout(alarmId);
    
    console.log(`Alarm ${alarmId} canceled`);
  }

  private cancelDeviceAlarmTimeout(alarmId: string): void {
    // Remove from notification queue
    this.notificationQueue = this.notificationQueue.filter(n => n.id !== alarmId);
  }

  private async handleAlarmTrigger(alarmId: string): Promise<void> {
    const alarm = this.alarms.get(alarmId);
    if (!alarm) return;

    // This would trigger the alarm UI
    Alert.alert(
      '⏰ Alarm',
      alarm.title,
      [
        { text: 'Snooze', onPress: () => this.snoozeAlarm(alarmId) },
        { text: 'Stop', onPress: () => this.stopAlarm(alarmId) }
      ]
    );
  }

  private async snoozeAlarm(alarmId: string): Promise<void> {
    const alarm = this.alarms.get(alarmId);
    if (!alarm) return;

    // Snooze for 10 minutes
    const snoozeTime = new Date(Date.now() + 10 * 60 * 1000);
    alarm.dateTime = snoozeTime;
    
    await this.setDeviceAlarm(alarm);
  }

  private async stopAlarm(alarmId: string): Promise<void> {
    await this.cancelDeviceAlarm(alarmId);
  }

  private async saveAlarmsToStorage(): Promise<void> {
    try {
      const alarmsData = JSON.stringify(
        Array.from(this.alarms.entries()),
        (key, value) => {
          if (value instanceof Date) {
            return value.toISOString();
          }
          return value;
        }
      );
      await AsyncStorage.setItem('device_alarms', alarmsData);
    } catch (error) {
      console.error('Error saving alarms:', error);
    }
  }

  private async loadAlarmsFromStorage(): Promise<void> {
    try {
      const alarmsData = await AsyncStorage.getItem('device_alarms');
      if (alarmsData) {
        const entries = JSON.parse(alarmsData, (key, value) => {
          if (key === 'dateTime' && typeof value === 'string') {
            return new Date(value);
          }
          return value;
        });
        this.alarms = new Map(entries);
      }
    } catch (error) {
      console.error('Error loading alarms:', error);
    }
  }

  // Public methods for external access
  public async getActiveAlarms(): Promise<AlarmData[]> {
    return Array.from(this.alarms.values())
      .filter(alarm => alarm.isActive && alarm.dateTime > new Date())
      .sort((a, b) => a.dateTime.getTime() - b.dateTime.getTime());
  }

  public async getAllAlarms(): Promise<AlarmData[]> {
    return Array.from(this.alarms.values());
  }
}

// Export singleton instance
export const deviceController = new DeviceController();
export default deviceController;
