# Send Button Text Wrapping Fix

## 🐛 **Problem**
- Send button text "Send" was wrapping with "Sen" on first line and "d" on second line
- Button was too small (32x32px) to accommodate the text properly

## ✅ **Solution Applied**

### **Button Size Adjustment**
```typescript
// Before (too small):
sendButton: {
  width: 32,        // Fixed width caused wrapping
  height: 32,
  borderRadius: 16,
  backgroundColor: '#00ff88',
  justifyContent: 'center',
  alignItems: 'center',
}

// After (proper sizing):
sendButton: {
  minWidth: 50,     // Flexible width to fit "Send" text
  height: 32,
  borderRadius: 16,
  backgroundColor: '#00ff88',
  justifyContent: 'center',
  alignItems: 'center',
  paddingHorizontal: 12,  // Added padding for better text spacing
}
```

### **Text Style Improvements**
```typescript
// Before:
sendButtonText: {
  color: '#ffffff',
  fontSize: 16,     // Too large for small button
  fontWeight: 'bold',
}

// After:
sendButtonText: {
  color: '#ffffff',
  fontSize: 14,     // Smaller font for better fit
  fontWeight: 'bold',
  textAlign: 'center',  // Ensure centered alignment
}
```

### **Button Text**
- Changed from arrow symbol "➤" to "Send" text
- Button now properly accommodates the text without wrapping

## 🎯 **Key Changes**
1. **Width**: Changed from fixed `width: 32` to flexible `minWidth: 50`
2. **Padding**: Added `paddingHorizontal: 12` for proper text spacing
3. **Font Size**: Reduced from 16px to 14px for better fit
4. **Text Alignment**: Added `textAlign: 'center'` for consistency
5. **Text Content**: Using "Send" instead of "➤" symbol

## 🚀 **Result**
- Send button now displays "Send" text properly on a single line
- Button maintains the same green color and rounded appearance
- Text is properly centered and readable
- No more text wrapping issues

The button will now expand horizontally as needed to fit the "Send" text while maintaining consistent height and styling.
