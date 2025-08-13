// TypeScript definitions for smartwatch functionality

export interface DeviceInfo {
  name: string;
  address: string;
  protocol: string;
  rssi: number;
  isBind: boolean;
}

export interface ConnectionState {
  state: number;
  stateString: string;
}

export interface BindStateResult {
  isBound: boolean;
}

export interface BindDeviceResult {
  deviceVerify: boolean;
}

export interface UserIdVerificationResult {
  success: boolean;
  userId: string;
}

export interface BindingResult {
  success: boolean;
}

export interface BerryBindingResult {
  success: boolean;
  userId?: string;
  error?: string;
}

export interface DeviceInfoResult {
  equipmentNumber?: string;
  firmwareVersion?: string;
  serialNumber?: string;
  macAddress?: string;
}

export interface BatteryInfoResult {
  capacity: number;
  chargeStatus: number;
}

export interface TimeSetResult {
  success: boolean;
  timestamp?: number;
}

export interface TimeFormatResult {
  success: boolean;
  is24Hour?: boolean;
}

export interface WatchVoiceCommandResult {
  rawData: string;
  timestamp: string;
}

export interface AiCommandResult {
  success: boolean;
  command?: number;
  text?: string;
  errorCode?: number;
}

export type SmartwatchEventType = 
  | 'onDeviceFound'
  | 'onConnectionStateChanged'
  | 'onBindStateChecked'
  | 'onBindDeviceInfo'
  | 'onUserIdVerified'
  | 'onBindingResultSent'
  | 'onUserIdSetByBerry'
  | 'onDeviceBoundByBerry'
  | 'onDeviceBindSuccessByBerry'
  | 'onOneKeyBindCompleted'
  | 'onDeviceInfoReceived'
  | 'onBatteryInfoReceived'
  | 'onDeviceInfoRequestCompleted'
  | 'onTimeSetCompleted'
  | 'onTimeFormatSetCompleted'
  | 'onWatchAiVoiceCommand'
  | 'onAiVoiceCommandSent'
  | 'onAiTranslatedTextSent'
  | 'onAiAnswerTextSent'
  | 'onAiErrorCodeSent';

export interface SmartwatchEventData {
  onDeviceFound: DeviceInfo;
  onConnectionStateChanged: ConnectionState;
  onBindStateChecked: BindStateResult;
  onBindDeviceInfo: BindDeviceResult;
  onUserIdVerified: UserIdVerificationResult;
  onBindingResultSent: BindingResult;
  onUserIdSetByBerry: BerryBindingResult;
  onDeviceBoundByBerry: BerryBindingResult;
  onDeviceBindSuccessByBerry: BerryBindingResult;
  onOneKeyBindCompleted: BerryBindingResult;
  onDeviceInfoReceived: DeviceInfoResult;
  onBatteryInfoReceived: BatteryInfoResult;
  onDeviceInfoRequestCompleted: BindingResult;
  onTimeSetCompleted: TimeSetResult;
  onTimeFormatSetCompleted: TimeFormatResult;
  onWatchAiVoiceCommand: WatchVoiceCommandResult;
  onAiVoiceCommandSent: AiCommandResult;
  onAiTranslatedTextSent: AiCommandResult;
  onAiAnswerTextSent: AiCommandResult;
  onAiErrorCodeSent: AiCommandResult;
}

export enum ScanStatus {
  IDLE = 'idle',
  SCANNING = 'scanning',
  STOPPED = 'stopped'
}

export enum ConnectionStatus {
  DISCONNECTED = 'disconnected',
  CONNECTING = 'connecting', 
  CONNECTED = 'connected',
  BINDING = 'binding',
  BOUND = 'bound'
}
