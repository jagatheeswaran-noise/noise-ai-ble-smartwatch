# Keyboard Visibility Fix

## 🐛 **Problem**
When clicking to type, the keyboard pops up but **part of the message box gets hidden behind the keyboard**, making it difficult to see the input area.

## ✅ **Solution Applied**

### **1. Restored KeyboardAvoidingView**
```tsx
// Before: Regular View (no keyboard handling)
<View style={styles.container}>

// After: KeyboardAvoidingView with proper configuration
<KeyboardAvoidingView 
  style={styles.container}
  behavior={Platform.OS === 'ios' ? 'padding' : 'padding'}
  keyboardVerticalOffset={Platform.OS === 'ios' ? 0 : 10}
>
```

### **2. Updated Android Manifest**
```xml
<!-- Before: adjustPan (moves content but can hide elements) -->
<activity android:windowSoftInputMode="adjustPan" />

<!-- After: adjustResize (resizes layout to fit keyboard) -->
<activity android:windowSoftInputMode="adjustResize" />
```

### **3. Optimized Behavior Settings**
- **iOS**: `padding` behavior (standard iOS approach)
- **Android**: `padding` behavior (instead of `height` for better compatibility)
- **Vertical Offset**: 10px for Android to account for status bar

### **4. Maintained Bottom Padding**
```tsx
inputContainer: {
  paddingBottom: 40,  // Ensures adequate spacing above keyboard
}
```

## 🎯 **Expected Results**

✅ **Message box fully visible** when keyboard appears
✅ **Input area moves up** to stay above keyboard
✅ **Proper spacing** maintained with 40px bottom padding
✅ **Smooth keyboard animations** without content hiding
✅ **Cross-platform compatibility** (iOS and Android)

## 🔧 **Technical Implementation**

### **KeyboardAvoidingView Configuration**:
- **Behavior**: `padding` for both platforms (more reliable)
- **Offset**: 10px for Android to handle system UI
- **Manifest**: `adjustResize` for proper layout adjustment

### **Input Container**:
- **Bottom padding**: 40px for comfortable spacing
- **Background**: Dark theme consistency
- **Border**: Top border for visual separation

The keyboard should now properly push the message box up so it remains fully visible when typing.
