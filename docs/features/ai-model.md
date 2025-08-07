# AI Model Management & Features

This document covers AI model management, health features, and voice assistant capabilities.

## AI Model Management
A React Native app that combines voice recognition with local AI processing using llama.rn, featuring automatic model download and memory management.

## 🚀 Features

- 🎤 **Voice Recognition**: Support for English and Hindi
- 🤖 **Local AI**: Health assistant powered by Llama.cpp (completely offline)
- 💬 **Modern UI**: ChatGPT-style dark mode interface
- � **Cross Platform**: Android & iOS support
- 🔄 **Auto Model Management**: Download, load, and manage AI models in-app
- 🧠 **Smart Memory**: Auto-offload models when app goes to background
- 📊 **Storage Monitoring**: Real-time storage usage and health checks

## 🆕 New Model Management Features

### In-App Model Download
- **One-tap download** of Llama-3.2-1B-Instruct (Q8_0) model
- **Progress tracking** with real-time download speed
- **Storage validation** before download begins
- **Automatic retry** on failed downloads

### Auto Memory Management (Inspired by Pocket Pal)
- **Background Offload**: Automatically unloads models when app goes to background
- **Foreground Reload**: Automatically reloads models when app becomes active
- **Configurable**: Toggle auto-offload in Model Manager settings
- **Memory Optimization**: Reduces RAM usage when not actively using AI

### Smart Storage Management
- **Real-time monitoring** of device storage
- **Visual storage bar** showing usage percentage
- **Storage health checks** with warnings and recommendations
- **Model size tracking** and cleanup options

## 🎯 How to Use

### 1. Access Model Manager
- Tap the **AI button** in the top-right corner
- Or tap on the **model status bar** (shows current AI state)

### 2. Download AI Model
1. Open Model Manager
2. See "Llama-3.2-1B-Instruct" in the models list
3. Tap **Download** (ensures sufficient storage first)
4. Wait for download completion (~1.3GB)
5. Tap **Load** to activate the model

### 3. Automatic Features
- **Auto-offload**: Models unload after 5 minutes in background
- **Auto-reload**: Models reload when app becomes active
- **Storage monitoring**: Get warnings when storage is low

## 📱 UI Elements

### Model Status Indicators
- 🟢 **Green dot**: AI model loaded and ready
- 🔴 **Red dot**: Using fallback responses (no model)
- 🟠 **Orange dot**: Model downloading
- 🔵 **Blue dot**: Model downloaded but not loaded

### Model Manager Interface
- **Storage visualization**: Progress bar showing storage usage
- **Model cards**: Status, size, and action buttons for each model
- **Auto-offload toggle**: Enable/disable background memory management
- **Storage details**: Available, used, and total space information

## ⚙️ Technical Details

### Supported Models
- **Llama-3.2-1B-Instruct-Q8_0**: 1.3GB, optimized for health assistance
- Expandable architecture for future model additions

### Storage Requirements
- **Minimum**: 2GB free space for model download
- **Recommended**: 4GB+ for optimal performance
- **Warning thresholds**: 85% (warning), 95% (critical)

### Memory Management
- **CPU-only inference**: Better compatibility across devices
- **Context size**: 2048 tokens
- **Auto-cleanup**: Temporary files removed after 24 hours
- **Background timing**: 5-minute delay before auto-offload

### Download Features
- **Resume capability**: Partial downloads can be resumed
- **Integrity verification**: File size validation after download
- **Bandwidth monitoring**: Real-time download speed display
- **Error handling**: Automatic cleanup of failed downloads

## 🔧 Installation & Setup

```bash
# Install dependencies
npm install

# iOS setup (if targeting iOS)
cd ios && pod install

# Run on Android
npm run android

# Run on iOS
npm run ios
```

## 📦 Dependencies

### Core AI
- `llama.rn` - Local LLM inference
- `@react-native-async-storage/async-storage` - Persistent storage
- `react-native-fs` - File system operations

### Voice & UI
- `@react-native-voice/voice` - Speech recognition
- React Native core components

## 🎛️ Configuration Options

### Auto-Offload Settings
```typescript
llamaService.setAutoOffloadEnabled(true/false)
```

### Model Paths
- **Android**: `/data/data/com.noise_ai/files/`
- **iOS**: App Documents directory

### Storage Thresholds
- **Low warning**: 85% storage used
- **Critical warning**: 95% storage used
- **Download minimum**: 1.5x model size free space

## 📊 Storage Health Monitoring

The app provides intelligent storage monitoring:

### Health Checks
- **Storage percentage**: Visual progress bar
- **Available space**: Real-time calculation
- **Model usage**: Tracks space used by AI models
- **Recommendations**: Actionable advice for storage management

### Warnings
- **Low storage**: When >85% is used
- **Critical storage**: When >95% is used
- **Insufficient space**: When unable to download models
- **Large model usage**: When models use >10% of total storage

## 🔒 Privacy & Offline Features

- **100% offline AI**: No data sent to external servers
- **Local processing**: All voice and AI processing on-device
- **Privacy first**: Voice data never leaves your device
- **No internet required**: AI works completely offline once model is downloaded

## ⚡ Performance Tips

1. **Enable auto-offload** for optimal memory usage
2. **Download models on WiFi** to save cellular data
3. **Keep 2GB+ free space** for smooth operation
4. **Close other apps** during model download
5. **Use voice recognition in quiet environment** for best results

## 🆘 Troubleshooting

### Download Issues
- **Check internet connection** and retry
- **Ensure sufficient storage space** (2GB+ recommended)
- **Try downloading on WiFi** for stability

### Memory Issues
- **Enable auto-offload** in Model Manager
- **Manually unload** models when not needed
- **Restart app** if experiencing performance issues

### AI Not Responding
- **Check model status** - should show green indicator
- **Reload model** if showing blue indicator
- **Download model** if showing red indicator

## 📈 Future Roadmap

- [ ] Multiple model support (3B, 7B variants)
- [ ] Model quantization options (Q4, Q6, Q8)
- [ ] Custom training data integration
- [ ] Voice cloning capabilities
- [ ] Multi-language model support
- [ ] Cloud backup for model settings

---

**Noise AI** - Your privacy-focused, offline AI health assistant with intelligent model management.

## Health Features

## Overview
Noise AI now includes comprehensive health data tracking and analysis capabilities, providing personalized health insights and AI responses based on your health metrics.

## Features Added

### 📊 **Comprehensive Health Data Tracking**
- **Heart Rate (HR)**: Continuous monitoring with resting, active, and exercise contexts
- **Respiratory Rate (RR)**: Regular breathing pattern analysis
- **Heart Rate Variability (HRV)**: RMSSD, PNN50, and SDNN metrics for stress/recovery analysis
- **Sleep Data**: Complete sleep tracking with sleep stages (Light, Deep, REM, Awake)
- **Recovery & Stress Scores**: AI-derived wellness indicators

### 🏥 **Health Dashboard**
Access via the 📊 button in the app header:
- **Real-time Metrics**: Today's health data at a glance
- **Trends Analysis**: Week/Month/Quarter trend visualization with improvement indicators
- **Sleep Analysis**: Detailed sleep stages, efficiency, and quality scores
- **Personalized Insights**: Health score trends and recovery recommendations

### 🤖 **AI Health Integration**
- **Context-Aware Responses**: AI now considers your current health data when providing advice
- **Personalized Recommendations**: Responses tailored to your health metrics and trends
- **Health Data Queries**: Ask about your health data directly (e.g., "How did I sleep last night?")
- **No More Disclaimers**: Removed repetitive medical disclaimers for cleaner conversations

## Sample Health Data

### Generated Metrics (30 Days)
The app automatically generates realistic sample data including:

**Heart Rate Data:**
- Base resting: 60-80 BPM
- Daily patterns: Morning activity, afternoon peaks, evening exercise, nighttime rest
- Context tracking: Resting, activity, exercise periods

**Sleep Tracking:**
- Bedtime: 22:00-23:59
- Sleep duration: 7-9 hours
- Sleep stages: 45% Light, 25% Deep, 25% REM, 5% Awake
- Sleep efficiency: 75-95%
- Sleep scores: 60-95/100

**HRV Metrics:**
- RMSSD: 25-55ms (primary HRV metric)
- PNN50: 15-35%
- SDNN: Related to RMSSD with natural variation

**Recovery & Stress:**
- Stress levels: 20-80% (derived from HRV and other metrics)
- Recovery scores: 50-90%

## Health Data Structure

### Daily Metrics
```typescript
interface DailyHealthMetrics {
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
  stressLevel: number; // 0-100
  recoveryScore: number; // 0-100
}
```

### Sleep Stages
- **Light Sleep**: 45% of total sleep time
- **Deep Sleep**: 25% of total sleep time  
- **REM Sleep**: 25% of total sleep time
- **Awake**: 5% of total sleep time

### Trend Analysis
The system analyzes trends by comparing first half vs second half of the selected period:
- **Improving**: Positive trend in health metrics
- **Stable**: No significant change
- **Declining**: Negative trend requiring attention

## AI Integration Examples

### Health-Aware Responses
**Before:** Generic health advice
**After:** "Based on your recent sleep score of 78 and resting heart rate of 65 BPM, here's personalized advice..."

### Supported Queries
- "How did I sleep last night?"
- "What's my heart rate trend this week?"
- "Am I stressed based on my HRV?"
- "How's my recovery looking?"
- "What does my health data say about my fitness?"

## Technical Implementation

### Data Storage
- Uses AsyncStorage for local health data persistence
- Automatic sample data generation for demonstration
- Data structure supports real device integration

### Performance Optimizations
- Efficient data querying with date range filtering
- Minimal memory footprint with optimized data structures
- Background data processing for trend calculations

### Integration Points
- **LlamaService**: Enhanced with health context for AI responses
- **VoiceTest**: New health dashboard modal integration
- **HealthDataManager**: Core data management and analytics
- **HealthDashboard**: Rich UI for health data visualization

## Future Enhancements

### Real Device Integration
- Integration with Apple HealthKit (iOS)
- Integration with Google Fit (Android)
- Wearable device support (Apple Watch, Fitbit, etc.)

### Advanced Analytics
- Machine learning for health predictions
- Anomaly detection for health alerts
- Personalized health coaching recommendations

### Data Export
- PDF health reports
- Data sharing with healthcare providers
- Integration with medical record systems

## Data Privacy
- All health data stored locally on device
- No cloud synchronization by default
- User controls all data sharing and export

## Usage Tips

1. **Explore the Dashboard**: Tap the 📊 button to view your health metrics
2. **Ask Health Questions**: Use natural language to query your health data
3. **Monitor Trends**: Switch between week/month/quarter views for different perspectives
4. **Recovery Insights**: Pay attention to HRV and recovery scores for training guidance
5. **Sleep Optimization**: Use sleep stage data to improve sleep quality

The health data system provides a foundation for personalized health AI that grows smarter with your data, offering insights that generic health apps cannot provide.
