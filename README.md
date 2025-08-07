# Noise AI - Intelligent Voice Health Assistant

A React Native app that combines voice recognition with local AI (Llama) to provide personalized health insights and recommendations based on your health data.

## 🚀 Features

### 🎤 Voice Recognition
- Real-time speech-to-text conversion
- Multi-language support
- Voice command processing

### 🤖 Local AI Integration
- Llama 3.2-1B-Instruct model running locally
- Streaming AI responses with typing effects
- No internet required for AI processing

### 📊 Intelligent Health Data Analysis
- **Heart Rate Analysis**: "What is my average heart rate last week?"
- **Sleep Comparison**: "Compare my sleep last week and this week"
- **Personalized Diet Plans**: "Give me a diet plan based on my health data"
- **Comprehensive Health Tracking**: HR, HRV, Sleep Stages, Recovery Scores

### 🎯 Smart Health Queries
The AI intelligently detects and responds to specific health questions:
- Fetches real data from local database
- Provides detailed analysis and trends
- Offers personalized recommendations
- Supports natural language queries

## 🏥 Health Data Features

### Tracked Metrics
- **Heart Rate (HR)**: Continuous monitoring with context (rest/activity/exercise)
- **Respiratory Rate (RR)**: Breathing pattern analysis
- **Heart Rate Variability (HRV)**: RMSSD, PNN50, SDNN for stress/recovery
- **Sleep Data**: Complete sleep stage tracking (Light, Deep, REM, Awake)
- **Recovery & Stress Scores**: AI-derived wellness indicators

### Sample Data
Automatically generates 30 days of realistic health data including:
- Daily heart rate patterns with activity contexts
- Complete sleep cycles with efficiency scores
- HRV trends for stress and recovery analysis
- Personalized health insights and recommendations

## 🎯 Example Queries

Try these natural language health queries:

```
"What is my average heart rate last week?"
"Compare my sleep last week and this week"
"Give me a diet plan based on my health data"
"How has my heart rate been trending?"
"What nutrition do I need based on my data?"
```

## 🔧 Technical Stack

- **React Native 0.80+**
- **@react-native-voice/voice**: Speech recognition
- **llama.rn**: Local AI inference
- **AsyncStorage**: Health data persistence
- **TypeScript**: Type-safe development

# Getting Started

> **Note**: Make sure you have completed the [Set Up Your Environment](https://reactnative.dev/docs/set-up-your-environment) guide before proceeding.

## Step 1: Start Metro

First, you will need to run **Metro**, the JavaScript build tool for React Native.

To start the Metro dev server, run the following command from the root of your React Native project:

```sh
# Using npm
npm start

# OR using Yarn
yarn start
```

## Step 2: Build and run your app

With Metro running, open a new terminal window/pane from the root of your React Native project, and use one of the following commands to build and run your Android or iOS app:

### Android

```sh
# Using npm
npm run android

# OR using Yarn
yarn android
```

### iOS

For iOS, remember to install CocoaPods dependencies (this only needs to be run on first clone or after updating native deps).

The first time you create a new project, run the Ruby bundler to install CocoaPods itself:

```sh
bundle install
```

Then, and every time you update your native dependencies, run:

```sh
bundle exec pod install
```

For more information, please visit [CocoaPods Getting Started guide](https://guides.cocoapods.org/using/getting-started.html).

```sh
# Using npm
npm run ios

# OR using Yarn
yarn ios
```

If everything is set up correctly, you should see your new app running in the Android Emulator, iOS Simulator, or your connected device.

This is one way to run your app — you can also build it directly from Android Studio or Xcode.

## Step 3: Modify your app

Now that you have successfully run the app, let's make changes!

Open `App.tsx` in your text editor of choice and make some changes. When you save, your app will automatically update and reflect these changes — this is powered by [Fast Refresh](https://reactnative.dev/docs/fast-refresh).

When you want to forcefully reload, for example to reset the state of your app, you can perform a full reload:

- **Android**: Press the <kbd>R</kbd> key twice or select **"Reload"** from the **Dev Menu**, accessed via <kbd>Ctrl</kbd> + <kbd>M</kbd> (Windows/Linux) or <kbd>Cmd ⌘</kbd> + <kbd>M</kbd> (macOS).
- **iOS**: Press <kbd>R</kbd> in iOS Simulator.

## Congratulations! :tada:

You've successfully run and modified your React Native App. :partying_face:

### Now what?

- If you want to add this new React Native code to an existing application, check out the [Integration guide](https://reactnative.dev/docs/integration-with-existing-apps).
- If you're curious to learn more about React Native, check out the [docs](https://reactnative.dev/docs/getting-started).

# Troubleshooting

If you're having issues getting the above steps to work, see the [Troubleshooting](https://reactnative.dev/docs/troubleshooting) page.

# Learn More

To learn more about React Native, take a look at the following resources:

- [React Native Website](https://reactnative.dev) - learn more about React Native.
- [Getting Started](https://reactnative.dev/docs/environment-setup) - an **overview** of React Native and how setup your environment.
- [Learn the Basics](https://reactnative.dev/docs/getting-started) - a **guided tour** of the React Native **basics**.
- [Blog](https://reactnative.dev/blog) - read the latest official React Native **Blog** posts.
- [`@facebook/react-native`](https://github.com/facebook/react-native) - the Open Source; GitHub **repository** for React Native.
