import { deviceController } from './DeviceController';

export interface ConversationContext {
  previousCommands: string[];
  currentAlarms: any[];
  userPersonality: {
    wakeUpDifficulty: 'easy' | 'normal' | 'hard';
    preferredWakeUpTime: string;
    sleepPattern: string;
  };
  emotionalState: 'tired' | 'energetic' | 'stressed' | 'normal';
  recentEvents: string[];
}

export interface AIReasoning {
  intent: string;
  confidence: number;
  parameters: any;
  contextualUnderstanding: string;
  suggestedAction: string;
}

class ContextualAI {
  private conversationContext: ConversationContext;
  private reasoningHistory: AIReasoning[] = [];

  constructor() {
    this.conversationContext = {
      previousCommands: [],
      currentAlarms: [],
      userPersonality: {
        wakeUpDifficulty: 'normal',
        preferredWakeUpTime: '7:00 AM',
        sleepPattern: 'regular'
      },
      emotionalState: 'normal',
      recentEvents: []
    };
  }

  async processComplexCommand(userInput: string): Promise<string> {
    // Add to conversation history
    this.conversationContext.previousCommands.push(userInput);
    
    // Analyze the command with full context
    const reasoning = await this.analyzeWithContext(userInput);
    this.reasoningHistory.push(reasoning);
    
    // Execute the understood action
    return await this.executeContextualAction(reasoning, userInput);
  }

  private async analyzeWithContext(userInput: string): Promise<AIReasoning> {
    const input = userInput.toLowerCase();
    
    // Extract emotional indicators
    this.updateEmotionalState(input);
    
    // Complex pattern matching with context awareness
    if (this.isAlarmModificationWithReason(input)) {
      return this.analyzeAlarmModificationWithReason(input);
    } else if (this.isComplexTimeRequest(input)) {
      return this.analyzeComplexTimeRequest(input);
    } else if (this.isConditionalRequest(input)) {
      return this.analyzeConditionalRequest(input);
    } else if (this.isPersonalityBasedRequest(input)) {
      return this.analyzePersonalityBasedRequest(input);
    }
    
    // Default reasoning
    return {
      intent: 'simple_command',
      confidence: 0.5,
      parameters: { originalInput: userInput },
      contextualUnderstanding: 'Basic command without complex context',
      suggestedAction: 'process_as_normal'
    };
  }

  private updateEmotionalState(input: string): void {
    if (input.includes('tired') || input.includes('exhausted') || input.includes('can\'t wake up')) {
      this.conversationContext.emotionalState = 'tired';
    } else if (input.includes('stressed') || input.includes('busy') || input.includes('overwhelmed')) {
      this.conversationContext.emotionalState = 'stressed';
    } else if (input.includes('energetic') || input.includes('ready') || input.includes('motivated')) {
      this.conversationContext.emotionalState = 'energetic';
    }
  }

  private isAlarmModificationWithReason(input: string): boolean {
    const patterns = [
      /i am (very )?(tired|exhausted|sleepy).*can(not|'t) wake up.*(\d{1,2})/i,
      /i am.*tired.*can only wake up.*(\d{1,2})/i,
      /(change|modify|move).*alarm.*because.*i.*tired/i,
      /i (need|want) to (change|modify|move).*alarm.*(\d{1,2}).*to.*(\d{1,2})/i
    ];
    
    return patterns.some(pattern => pattern.test(input));
  }

  private analyzeAlarmModificationWithReason(input: string): AIReasoning {
    // Extract current and new times
    const timeMatches = input.match(/(\d{1,2})\s*(am|pm)?/gi);
    const reasonMatch = input.match(/(tired|exhausted|sleepy|can't wake up|cannot wake up|need more sleep)/i);
    
    let currentTime, newTime;
    if (timeMatches && timeMatches.length >= 2) {
      currentTime = timeMatches[0];
      newTime = timeMatches[1];
    } else if (timeMatches && timeMatches.length === 1) {
      // Look for "only" keyword to determine which time is new
      if (input.includes('only wake up') || input.includes('can only wake')) {
        newTime = timeMatches[0];
        // Find current alarm from context
        currentTime = this.findCurrentAlarmTime();
      } else {
        currentTime = timeMatches[0];
      }
    }

    const reasoning: AIReasoning = {
      intent: 'modify_alarm_with_emotional_reason',
      confidence: 0.9,
      parameters: {
        currentTime,
        newTime,
        reason: reasonMatch ? reasonMatch[1] : 'tired',
        emotionalState: this.conversationContext.emotionalState
      },
      contextualUnderstanding: `User is ${this.conversationContext.emotionalState} and needs to modify alarm from ${currentTime} to ${newTime} due to difficulty waking up`,
      suggestedAction: 'modify_alarm_with_empathy'
    };

    return reasoning;
  }

  private isComplexTimeRequest(input: string): boolean {
    const patterns = [
      /tomorrow morning/i,
      /next (monday|tuesday|wednesday|thursday|friday|saturday|sunday)/i,
      /in \d+ hours?/i,
      /at (dawn|sunrise|sunset)/i,
      /before (work|school|meeting)/i
    ];
    
    return patterns.some(pattern => pattern.test(input));
  }

  private analyzeComplexTimeRequest(input: string): AIReasoning {
    const timeContext = this.parseComplexTime(input);
    
    return {
      intent: 'set_alarm_complex_time',
      confidence: 0.8,
      parameters: timeContext,
      contextualUnderstanding: `User wants alarm set for ${timeContext.description}`,
      suggestedAction: 'set_alarm_with_time_calculation'
    };
  }

  private isConditionalRequest(input: string): boolean {
    const patterns = [
      /if.*then/i,
      /unless/i,
      /when.*wake me/i,
      /only if/i
    ];
    
    return patterns.some(pattern => pattern.test(input));
  }

  private analyzeConditionalRequest(input: string): AIReasoning {
    return {
      intent: 'conditional_alarm',
      confidence: 0.7,
      parameters: { condition: input },
      contextualUnderstanding: 'User has conditional requirements for alarm',
      suggestedAction: 'handle_conditional_logic'
    };
  }

  private isPersonalityBasedRequest(input: string): boolean {
    return input.includes('i\'m not a morning person') || 
           input.includes('i sleep heavy') ||
           input.includes('i snooze a lot') ||
           input.includes('i have trouble waking up');
  }

  private analyzePersonalityBasedRequest(input: string): AIReasoning {
    // Update user personality
    if (input.includes('not a morning person') || input.includes('trouble waking up')) {
      this.conversationContext.userPersonality.wakeUpDifficulty = 'hard';
    }
    
    return {
      intent: 'personality_aware_alarm',
      confidence: 0.85,
      parameters: { 
        personality: this.conversationContext.userPersonality,
        specialNeeds: true
      },
      contextualUnderstanding: 'User has specific waking up challenges that need accommodation',
      suggestedAction: 'set_alarm_with_personality_accommodation'
    };
  }

  private parseComplexTime(input: string): any {
    const now = new Date();
    
    if (input.includes('tomorrow morning')) {
      const tomorrow = new Date(now);
      tomorrow.setDate(tomorrow.getDate() + 1);
      tomorrow.setHours(7, 0, 0, 0); // Default morning time
      return {
        time: tomorrow,
        description: 'tomorrow morning at 7 AM',
        type: 'relative_day_time'
      };
    }
    
    if (input.includes('in')) {
      const hoursMatch = input.match(/in (\d+) hours?/i);
      if (hoursMatch) {
        const hours = parseInt(hoursMatch[1]);
        const futureTime = new Date(now.getTime() + hours * 60 * 60 * 1000);
        return {
          time: futureTime,
          description: `in ${hours} hours`,
          type: 'relative_time'
        };
      }
    }
    
    return {
      time: now,
      description: 'now',
      type: 'immediate'
    };
  }

  private findCurrentAlarmTime(): string | undefined {
    // This would typically check existing alarms
    // For now, return a placeholder
    if (this.conversationContext.previousCommands.length > 0) {
      const lastCommand = this.conversationContext.previousCommands[this.conversationContext.previousCommands.length - 1];
      const timeMatch = lastCommand.match(/(\d{1,2})\s*(am|pm)/i);
      return timeMatch ? timeMatch[0] : undefined;
    }
    return undefined;
  }

  private async executeContextualAction(reasoning: AIReasoning, originalInput: string): Promise<string> {
    try {
      switch (reasoning.suggestedAction) {
        case 'modify_alarm_with_empathy':
          return await this.handleEmpathicAlarmModification(reasoning, originalInput);
        
        case 'set_alarm_with_time_calculation':
          return await this.handleComplexTimeAlarm(reasoning, originalInput);
        
        case 'handle_conditional_logic':
          return await this.handleConditionalAlarm(reasoning, originalInput);
        
        case 'set_alarm_with_personality_accommodation':
          return await this.handlePersonalityBasedAlarm(reasoning, originalInput);
        
        default:
          return await deviceController.processNaturalLanguageCommand(originalInput);
      }
    } catch (error) {
      console.error('Error executing contextual action:', error);
      return "I understand what you're trying to do, but I had trouble executing it. Let me try a different approach.";
    }
  }

  private async handleEmpathicAlarmModification(reasoning: AIReasoning, originalInput: string): Promise<string> {
    const { currentTime, newTime, reason, emotionalState } = reasoning.parameters;
    
    // Process the alarm change through device controller
    const deviceResponse = await deviceController.processNaturalLanguageCommand(originalInput);
    
    // Add empathetic response
    let empathyResponse = "";
    
    if (emotionalState === 'tired') {
      empathyResponse = "I totally understand - being tired makes it so much harder to wake up early. ";
    }
    
    if (reason === 'tired' || reason === 'exhausted') {
      empathyResponse += "Getting enough rest is really important for your health. ";
    }
    
    empathyResponse += "I've updated your alarm to give you more time to rest. ";
    
    // Suggest additional help
    if (this.conversationContext.userPersonality.wakeUpDifficulty === 'hard') {
      empathyResponse += "Since you mentioned having trouble waking up, would you like me to set multiple gentle alarms or suggest some wake-up tips?";
    }
    
    return deviceResponse + "\n\n" + empathyResponse;
  }

  private async handleComplexTimeAlarm(reasoning: AIReasoning, originalInput: string): Promise<string> {
    const { time, description, type } = reasoning.parameters;
    
    // Create a modified input with the calculated time
    const timeString = time.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: true });
    const modifiedInput = `Set alarm for ${timeString}`;
    
    const deviceResponse = await deviceController.processNaturalLanguageCommand(modifiedInput);
    
    return `${deviceResponse}\n\nI understood you wanted an alarm ${description}, so I calculated that to be ${timeString}.`;
  }

  private async handleConditionalAlarm(reasoning: AIReasoning, originalInput: string): Promise<string> {
    // For now, acknowledge the conditional nature but set a basic alarm
    const basicAlarmResponse = await deviceController.processNaturalLanguageCommand(originalInput);
    
    return `${basicAlarmResponse}\n\nI've noted your conditional requirements. Currently I can set basic alarms, but I'm learning to handle more complex conditions. Is there a specific time you'd like me to set for now?`;
  }

  private async handlePersonalityBasedAlarm(reasoning: AIReasoning, originalInput: string): Promise<string> {
    const { personality, specialNeeds } = reasoning.parameters;
    
    const deviceResponse = await deviceController.processNaturalLanguageCommand(originalInput);
    
    let personalityResponse = "";
    
    if (personality.wakeUpDifficulty === 'hard') {
      personalityResponse = "\n\nSince you mentioned having trouble waking up, here are some tips:\n";
      personalityResponse += "• I can set multiple alarms 10-15 minutes apart\n";
      personalityResponse += "• Try placing your phone across the room\n";
      personalityResponse += "• Getting sunlight as soon as you wake up helps\n";
      personalityResponse += "• A consistent sleep schedule makes waking up easier";
    }
    
    return deviceResponse + personalityResponse;
  }

  // Public method to get conversation context
  public getContext(): ConversationContext {
    return this.conversationContext;
  }

  // Public method to get reasoning history
  public getReasoningHistory(): AIReasoning[] {
    return this.reasoningHistory;
  }
}

// Export singleton instance
export const contextualAI = new ContextualAI();
export default contextualAI;
