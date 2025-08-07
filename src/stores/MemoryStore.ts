import { makeAutoObservable, runInAction } from 'mobx';
import { makePersistable } from 'mobx-persist-store';
import AsyncStorage from '@react-native-async-storage/async-storage';

export interface UserMemory {
  id: string;
  type: 'health_preference' | 'goal_setting' | 'user_fact' | 'interaction_pattern';
  content: string;
  context: any;
  confidence: number; // 0-1 scale
  createdAt: number;
  lastUsed: number;
  useCount: number;
  tags: string[];
}

export interface ConversationContext {
  sessionId: string;
  userGoals: any[];
  healthPreferences: any;
  currentFocus: 'general' | 'health' | 'goals' | 'calories' | 'sleep';
  recentTopics: string[];
  adaptiveLength: 'short' | 'detailed';
}

class MemoryStore {
  memories: UserMemory[] = [];
  currentContext: ConversationContext | null = null;
  memoryLimit: number = 1000; // Maximum number of memories to store

  constructor() {
    makeAutoObservable(this);
    
    makePersistable(this, {
      name: 'MemoryStore',
      properties: ['memories'],
      storage: AsyncStorage,
    }).then(() => {
      this.loadMemories();
    });
  }

  async clearSystemPrompts(): Promise<void> {
    runInAction(() => {
      this.memories = this.memories.filter(memory => {
        const content = memory.content.toLowerCase();
        // Remove memories that contain system prompts
        return !content.includes('you are noise ai') && 
               !content.includes('expert health') &&
               !content.includes('diet plan for me') &&
               !content.includes('wellness coach') &&
               content.length > 10; // Keep only meaningful content
      });
    });
    console.log('🧹 CLEARED SYSTEM PROMPTS FROM MEMORY, remaining:', this.memories.length);
  }

  async loadMemories() {
    console.log('Memories loaded:', this.memories.length);
    this.cleanupOldMemories();
  }

  generateId(): string {
    return `memory_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }

  async addMemory(memory: Omit<UserMemory, 'id' | 'createdAt' | 'lastUsed' | 'useCount'>): Promise<string> {
    const memoryId = this.generateId();
    const fullMemory: UserMemory = {
      ...memory,
      id: memoryId,
      createdAt: Date.now(),
      lastUsed: Date.now(),
      useCount: 1,
    };

    runInAction(() => {
      this.memories.unshift(fullMemory);
      
      // Limit memory storage
      if (this.memories.length > this.memoryLimit) {
        this.memories = this.memories.slice(0, this.memoryLimit);
      }
    });

    return memoryId;
  }

  async updateMemory(memoryId: string, updates: Partial<UserMemory>): Promise<void> {
    const index = this.memories.findIndex(m => m.id === memoryId);
    if (index >= 0) {
      runInAction(() => {
        this.memories[index] = {
          ...this.memories[index],
          ...updates,
          lastUsed: Date.now(),
          useCount: this.memories[index].useCount + 1,
        };
      });
    }
  }

  async getRelevantMemories(context: string, type?: UserMemory['type'], limit: number = 10): Promise<UserMemory[]> {
    const contextLower = context.toLowerCase();
    
    let relevant = this.memories.filter(memory => {
      // Type filter
      if (type && memory.type !== type) return false;
      
      // Content relevance
      const contentMatch = memory.content.toLowerCase().includes(contextLower) ||
                          memory.tags.some(tag => contextLower.includes(tag.toLowerCase()));
      
      // Context relevance
      const contextMatch = memory.context && 
                          JSON.stringify(memory.context).toLowerCase().includes(contextLower);
      
      return contentMatch || contextMatch;
    });

    // Sort by relevance score (combination of confidence, recency, and usage)
    relevant.sort((a, b) => {
      const scoreA = this.calculateRelevanceScore(a, contextLower);
      const scoreB = this.calculateRelevanceScore(b, contextLower);
      return scoreB - scoreA;
    });

    // Update lastUsed for retrieved memories
    relevant.slice(0, limit).forEach(memory => {
      this.updateMemoryUsage(memory.id);
    });

    return relevant.slice(0, limit);
  }

  private calculateRelevanceScore(memory: UserMemory, context: string): number {
    const now = Date.now();
    const daysSinceCreated = (now - memory.createdAt) / (1000 * 60 * 60 * 24);
    const daysSinceUsed = (now - memory.lastUsed) / (1000 * 60 * 60 * 24);
    
    // Scoring factors
    const confidenceScore = memory.confidence * 0.4;
    const recencyScore = Math.max(0, (30 - daysSinceUsed) / 30) * 0.3;
    const usageScore = Math.min(memory.useCount / 10, 1) * 0.2;
    const relevanceScore = this.calculateTextRelevance(memory, context) * 0.1;
    
    return confidenceScore + recencyScore + usageScore + relevanceScore;
  }

  private calculateTextRelevance(memory: UserMemory, context: string): number {
    const memoryText = (memory.content + ' ' + memory.tags.join(' ')).toLowerCase();
    const contextWords = context.toLowerCase().split(' ');
    
    let matchCount = 0;
    contextWords.forEach(word => {
      if (word.length > 2 && memoryText.includes(word)) {
        matchCount++;
      }
    });
    
    return contextWords.length > 0 ? matchCount / contextWords.length : 0;
  }

  private updateMemoryUsage(memoryId: string): void {
    const index = this.memories.findIndex(m => m.id === memoryId);
    if (index >= 0) {
      runInAction(() => {
        this.memories[index].lastUsed = Date.now();
        this.memories[index].useCount++;
      });
    }
  }

  async setConversationContext(context: ConversationContext): Promise<void> {
    runInAction(() => {
      this.currentContext = context;
    });
  }

  async updateConversationContext(updates: Partial<ConversationContext>): Promise<void> {
    if (this.currentContext) {
      runInAction(() => {
        this.currentContext = { ...this.currentContext!, ...updates };
      });
    }
  }

  getConversationContext(): ConversationContext | null {
    return this.currentContext;
  }

  // Health-specific memory methods
  async addHealthPreference(preference: string, context: any): Promise<string> {
    return this.addMemory({
      type: 'health_preference',
      content: preference,
      context,
      confidence: 0.8,
      tags: ['health', 'preference'],
    });
  }

  async addGoalSetting(goal: string, context: any): Promise<string> {
    return this.addMemory({
      type: 'goal_setting',
      content: goal,
      context,
      confidence: 0.9,
      tags: ['goal', 'target', 'plan'],
    });
  }

  async addUserFact(fact: string, context: any): Promise<string> {
    return this.addMemory({
      type: 'user_fact',
      content: fact,
      context,
      confidence: 0.7,
      tags: ['user', 'fact', 'personal'],
    });
  }

  async addInteractionPattern(pattern: string, context: any): Promise<string> {
    return this.addMemory({
      type: 'interaction_pattern',
      content: pattern,
      context,
      confidence: 0.6,
      tags: ['interaction', 'pattern', 'behavior'],
    });
  }

  // Get memories by type
  async getHealthMemories(): Promise<UserMemory[]> {
    return this.memories.filter(m => 
      m.type === 'health_preference' || 
      m.tags.includes('health')
    ).slice(0, 20);
  }

  async getGoalMemories(): Promise<UserMemory[]> {
    return this.memories.filter(m => 
      m.type === 'goal_setting' || 
      m.tags.includes('goal')
    ).slice(0, 20);
  }

  // Cleanup old memories
  private cleanupOldMemories(): void {
    const thirtyDaysAgo = Date.now() - (30 * 24 * 60 * 60 * 1000);
    
    runInAction(() => {
      // Remove low-confidence, old, unused memories
      this.memories = this.memories.filter(memory => {
        if (memory.confidence < 0.3 && 
            memory.lastUsed < thirtyDaysAgo && 
            memory.useCount < 2) {
          return false;
        }
        return true;
      });
    });
  }

  async clearMemories(): Promise<void> {
    runInAction(() => {
      this.memories = [];
      this.currentContext = null;
    });
  }

  // Export/Import for debugging
  exportMemories(): any {
    return {
      memories: this.memories,
      currentContext: this.currentContext,
      timestamp: Date.now(),
    };
  }

  async importMemories(data: any): Promise<void> {
    if (data.memories && Array.isArray(data.memories)) {
      runInAction(() => {
        this.memories = data.memories;
        this.currentContext = data.currentContext || null;
      });
    }
  }
}

export const memoryStore = new MemoryStore();
