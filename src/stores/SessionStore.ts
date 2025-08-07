import { makeAutoObservable, runInAction } from 'mobx';
import { makePersistable } from 'mobx-persist-store';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { format, isToday, isYesterday } from 'date-fns';

export interface MessageMetadata {
  contextId?: string;
  healthQuery?: boolean;
  goalQuery?: boolean;
  timings?: any;
  copyable?: boolean;
  multimodal?: boolean;
}

export interface ChatMessage {
  id: string;
  author: 'user' | 'assistant';
  text: string;
  createdAt: number;
  type: 'text';
  metadata?: MessageMetadata;
}

export interface SessionData {
  id: string;
  title: string;
  date: string;
  messages: ChatMessage[];
  userContext?: any;
  completionSettings?: any;
}

interface SessionGroup {
  [key: string]: SessionData[];
}

const DEFAULT_GROUP_NAMES = {
  today: 'Today',
  yesterday: 'Yesterday',
  thisWeek: 'This week',
  lastWeek: 'Last week',
  twoWeeksAgo: '2 weeks ago',
  threeWeeksAgo: '3 weeks ago',
  fourWeeksAgo: '4 weeks ago',
  lastMonth: 'Last month',
  older: 'Older',
};

class SessionStore {
  sessions: SessionData[] = [];
  activeSessionId: string | null = null;
  isGenerating: boolean = false;
  dateGroupNames: typeof DEFAULT_GROUP_NAMES = DEFAULT_GROUP_NAMES;

  constructor() {
    makeAutoObservable(this);
    
    makePersistable(this, {
      name: 'SessionStore',
      properties: ['sessions', 'activeSessionId'],
      storage: AsyncStorage,
    }).then(() => {
      this.loadSessions();
    });
  }

  async loadSessions() {
    // Sessions are automatically loaded by mobx-persist-store
    console.log('Sessions loaded:', this.sessions.length);
  }

  get currentSessionMessages(): ChatMessage[] {
    if (this.activeSessionId) {
      const session = this.sessions.find(s => s.id === this.activeSessionId);
      return session?.messages || [];
    }
    return [];
  }

  get activeSession(): SessionData | undefined {
    if (this.activeSessionId) {
      return this.sessions.find(s => s.id === this.activeSessionId);
    }
    return undefined;
  }

  get groupedSessions(): SessionGroup {
    const groups: SessionGroup = this.sessions.reduce(
      (acc: SessionGroup, session) => {
        const date = new Date(session.date);
        let dateKey: string = format(date, 'MMMM dd, yyyy');
        const today = new Date();
        const daysAgo = Math.ceil(
          (today.getTime() - date.getTime()) / (1000 * 3600 * 24),
        );

        if (isToday(date)) {
          dateKey = this.dateGroupNames.today;
        } else if (isYesterday(date)) {
          dateKey = this.dateGroupNames.yesterday;
        } else if (daysAgo <= 6) {
          dateKey = this.dateGroupNames.thisWeek;
        } else if (daysAgo <= 13) {
          dateKey = this.dateGroupNames.lastWeek;
        } else if (daysAgo <= 20) {
          dateKey = this.dateGroupNames.twoWeeksAgo;
        } else if (daysAgo <= 27) {
          dateKey = this.dateGroupNames.threeWeeksAgo;
        } else if (daysAgo <= 34) {
          dateKey = this.dateGroupNames.fourWeeksAgo;
        } else if (daysAgo <= 60) {
          dateKey = this.dateGroupNames.lastMonth;
        } else {
          dateKey = this.dateGroupNames.older;
        }

        if (!acc[dateKey]) {
          acc[dateKey] = [];
        }
        acc[dateKey].push(session);
        return acc;
      },
      {},
    );

    // Sort sessions within each group by date (newest first)
    Object.keys(groups).forEach(key => {
      groups[key].sort(
        (a, b) => new Date(b.date).getTime() - new Date(a.date).getTime(),
      );
    });

    return groups;
  }

  generateId(): string {
    return `session_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }

  generateMessageId(): string {
    return `msg_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }

  async createNewSession(title: string = 'New Chat'): Promise<string> {
    const sessionId = this.generateId();
    const newSession: SessionData = {
      id: sessionId,
      title,
      date: new Date().toISOString(),
      messages: [],
      userContext: {},
    };

    runInAction(() => {
      this.sessions.unshift(newSession);
      this.activeSessionId = sessionId;
    });

    return sessionId;
  }

  async setActiveSession(sessionId: string) {
    runInAction(() => {
      this.activeSessionId = sessionId;
    });
  }

  async addMessageToCurrentSession(message: Omit<ChatMessage, 'id'>): Promise<string> {
    const messageId = this.generateMessageId();
    const fullMessage: ChatMessage = {
      ...message,
      id: messageId,
    };

    if (!this.activeSessionId) {
      // Create new session if none exists
      await this.createNewSession();
    }

    if (this.activeSessionId) {
      const session = this.sessions.find(s => s.id === this.activeSessionId);
      if (session) {
        runInAction(() => {
          session.messages.unshift(fullMessage);
          
          // Update session title if it's the first user message
          if (message.author === 'user' && session.messages.length === 1) {
            session.title = message.text.slice(0, 40) + (message.text.length > 40 ? '...' : '');
          }
        });
      }
    }

    return messageId;
  }

  async updateMessage(messageId: string, update: Partial<ChatMessage>): Promise<void> {
    if (this.activeSessionId) {
      const session = this.sessions.find(s => s.id === this.activeSessionId);
      if (session) {
        const messageIndex = session.messages.findIndex(m => m.id === messageId);
        if (messageIndex >= 0) {
          runInAction(() => {
            session.messages[messageIndex] = {
              ...session.messages[messageIndex],
              ...update,
            };
          });
        }
      }
    }
  }

  async updateMessageToken(messageId: string, token: string): Promise<void> {
    if (this.activeSessionId) {
      const session = this.sessions.find(s => s.id === this.activeSessionId);
      if (session) {
        const messageIndex = session.messages.findIndex(m => m.id === messageId);
        if (messageIndex >= 0) {
          runInAction(() => {
            const message = session.messages[messageIndex];
            session.messages[messageIndex] = {
              ...message,
              text: (message.text + token).replace(/^\s+/, ''),
            };
          });
        }
      }
    }
  }

  async updateSessionUserContext(context: any): Promise<void> {
    if (this.activeSessionId) {
      const session = this.sessions.find(s => s.id === this.activeSessionId);
      if (session) {
        runInAction(() => {
          session.userContext = { ...session.userContext, ...context };
        });
      }
    }
  }

  async deleteSession(sessionId: string): Promise<void> {
    runInAction(() => {
      this.sessions = this.sessions.filter(s => s.id !== sessionId);
      
      if (this.activeSessionId === sessionId) {
        this.activeSessionId = this.sessions.length > 0 ? this.sessions[0].id : null;
      }
    });
  }

  async clearAllSessions(): Promise<void> {
    runInAction(() => {
      this.sessions = [];
      this.activeSessionId = null;
    });
  }

  resetActiveSession() {
    runInAction(() => {
      this.activeSessionId = null;
    });
  }

  setIsGenerating(value: boolean) {
    this.isGenerating = value;
  }

  // Context/Memory methods
  getSessionContext(): any {
    const session = this.activeSession;
    if (!session) return {};

    return {
      sessionId: session.id,
      messageCount: session.messages.length,
      userContext: session.userContext || {},
      recentMessages: session.messages.slice(-5).map(m => ({
        author: m.author,
        text: m.text.slice(0, 150), // Increased context length
        metadata: m.metadata,
      })),
    };
  }

  getHealthQueryHistory(): ChatMessage[] {
    const session = this.activeSession;
    if (!session) return [];

    return session.messages.filter(m => 
      m.metadata?.healthQuery || 
      m.text.toLowerCase().includes('health') ||
      m.text.toLowerCase().includes('heart') ||
      m.text.toLowerCase().includes('sleep') ||
      m.text.toLowerCase().includes('calories')
    );
  }

  getGoalQueryHistory(): ChatMessage[] {
    const session = this.activeSession;
    if (!session) return [];

    return session.messages.filter(m => 
      m.metadata?.goalQuery || 
      m.text.toLowerCase().includes('goal') ||
      m.text.toLowerCase().includes('target') ||
      m.text.toLowerCase().includes('progress')
    );
  }
}

export const sessionStore = new SessionStore();
