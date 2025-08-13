import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import { Language, LANGUAGES } from '../../types/chat';

interface LanguageSelectorProps {
  selectedLanguage: Language;
  onLanguageSelect: (language: Language) => void;
  style?: any;
}

const LanguageSelector: React.FC<LanguageSelectorProps> = ({
  selectedLanguage,
  onLanguageSelect,
  style,
}) => {
  return (
    <View style={[styles.container, style]}>
      <Text style={styles.label}>Language:</Text>
      <View style={styles.buttonsContainer}>
        {LANGUAGES.map((language) => (
          <TouchableOpacity
            key={language.code}
            style={[
              styles.languageButton,
              selectedLanguage.code === language.code && styles.selectedLanguageButton,
            ]}
            onPress={() => onLanguageSelect(language)}
          >
            <Text
              style={[
                styles.languageText,
                selectedLanguage.code === language.code && styles.selectedLanguageText,
              ]}
            >
              {language.name}
            </Text>
          </TouchableOpacity>
        ))}
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 8,
  },
  label: {
    color: '#cccccc',
    fontSize: 14,
    marginRight: 12,
  },
  buttonsContainer: {
    flexDirection: 'row',
    flex: 1,
  },
  languageButton: {
    backgroundColor: '#2a2a2a',
    paddingHorizontal: 20,
    paddingVertical: 10,
    borderRadius: 20,
    marginRight: 12,
    flexDirection: 'row',
    alignItems: 'center',
  },
  selectedLanguageButton: {
    backgroundColor: '#003d1f',
    borderWidth: 1,
    borderColor: '#00ff88',
  },
  languageText: {
    fontSize: 14,
    color: '#cccccc',
    fontWeight: '400',
  },
  selectedLanguageText: {
    color: '#00ff88',
  },
});

export default LanguageSelector;
