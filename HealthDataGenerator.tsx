import React, { useState } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, ScrollView, Alert } from 'react-native';
import { enhancedHealthDataManager } from './EnhancedHealthDataManager';

interface DataStats {
  hasData: boolean;
  lastGenerated?: string;
  daysCovered?: number;
  lastSynced?: string;
  recordCount?: number;
}

export const HealthDataGenerator = () => {
  const [isGenerating, setIsGenerating] = useState(false);
  const [status, setStatus] = useState('Ready to generate health data');
  const [dataStats, setDataStats] = useState<DataStats | null>(null);

  const generateRealisticData = async () => {
    setIsGenerating(true);
    setStatus('🏥 Initializing health data generation...');

    try {
      // Configuration for 30 days of realistic data
      const config = {
        days: 30,
        userId: `user-${Date.now()}`,
        includeVariations: true,
        realisticPatterns: true
      };

      setStatus('⚙️ Configuration set: 30 days, realistic patterns enabled');
      setStatus('📊 Generating heart rate patterns...');
      
      // Start the generation process
      const success = await enhancedHealthDataManager.generateHealthData(config);

      if (success) {
        setStatus('✅ Successfully generated 30 days of realistic health data!');
        
        // Get data statistics
        const stats = await enhancedHealthDataManager.getDataStatus();
        setDataStats(stats);
        
        Alert.alert(
          'Success! 🎉',
          `Generated ${stats.daysCovered || 30} days of realistic health data with ${stats.recordCount || 'multiple'} records. Your AI assistant now has comprehensive health data to provide personalized advice!`,
          [{ text: 'Great!', style: 'default' }]
        );
        
      } else {
        setStatus('❌ Failed to generate health data');
        Alert.alert('Error', 'Failed to generate health data. Please try again.');
      }

    } catch (error) {
      const errorMsg = error instanceof Error ? error.message : 'Unknown error';
      setStatus(`❌ Error: ${errorMsg}`);
      Alert.alert('Error', `Generation failed: ${errorMsg}`);
    } finally {
      setIsGenerating(false);
    }
  };

  const checkDataStatus = async () => {
    try {
      const stats = await enhancedHealthDataManager.getDataStatus();
      setDataStats(stats);
      
      if (stats.hasData) {
        Alert.alert(
          'Data Status 📊',
          `Records: ${stats.recordCount || 'Unknown'}\nDays Covered: ${stats.daysCovered || 'Unknown'}\nLast Generated: ${stats.lastGenerated || 'Never'}`,
          [{ text: 'OK', style: 'default' }]
        );
      } else {
        Alert.alert(
          'No Data 📭',
          'No health data found. Generate some data first!',
          [{ text: 'OK', style: 'default' }]
        );
      }
    } catch (error) {
      Alert.alert('Error', 'Failed to check data status');
    }
  };

  return (
    <ScrollView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>🏥 Health Data Generator</Text>
        <Text style={styles.subtitle}>Generate 30 days of realistic health patterns</Text>
      </View>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>📊 Data Generation</Text>
        <Text style={styles.description}>
          This will create 30 days of realistic health data including:
        </Text>
        
        <View style={styles.featureList}>
          <Text style={styles.feature}>• Heart rate patterns (resting, active, recovery)</Text>
          <Text style={styles.feature}>• Sleep cycles (light, deep, REM phases)</Text>
          <Text style={styles.feature}>• Activity levels (steps, calories, exercise)</Text>
          <Text style={styles.feature}>• Stress indicators with daily variations</Text>
          <Text style={styles.feature}>• Nutrition data (calories, macros, hydration)</Text>
          <Text style={styles.feature}>• General health metrics (weight, mood)</Text>
        </View>

        <TouchableOpacity 
          style={[styles.generateButton, isGenerating && styles.disabledButton]}
          onPress={generateRealisticData}
          disabled={isGenerating}
        >
          <Text style={styles.generateButtonText}>
            {isGenerating ? '⏳ Generating...' : '🚀 Generate 30 Days of Data'}
          </Text>
        </TouchableOpacity>

        <TouchableOpacity 
          style={styles.statusButton}
          onPress={checkDataStatus}
        >
          <Text style={styles.statusButtonText}>📋 Check Data Status</Text>
        </TouchableOpacity>
      </View>

      <View style={styles.statusSection}>
        <Text style={styles.statusTitle}>Status</Text>
        <Text style={styles.statusText}>{status}</Text>
        
        {dataStats && (
          <View style={styles.statsContainer}>
            <Text style={styles.statsTitle}>📈 Current Data Statistics</Text>
            <Text style={styles.stat}>Has Data: {dataStats.hasData ? '✅ Yes' : '❌ No'}</Text>
            {dataStats.recordCount && (
              <Text style={styles.stat}>Records: {dataStats.recordCount}</Text>
            )}
            {dataStats.daysCovered && (
              <Text style={styles.stat}>Days Covered: {dataStats.daysCovered}</Text>
            )}
            {dataStats.lastGenerated && (
              <Text style={styles.stat}>Last Generated: {new Date(dataStats.lastGenerated).toLocaleDateString()}</Text>
            )}
          </View>
        )}
      </View>

      <View style={styles.infoSection}>
        <Text style={styles.infoTitle}>ℹ️ About This Data</Text>
        <Text style={styles.infoText}>
          The generated data follows realistic health patterns and will be stored in your local SQLite database. 
          Your AI health assistant will use this data to provide personalized, bias-free health advice without 
          any hardcoded responses.
        </Text>
      </View>
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f8f9fa',
  },
  header: {
    padding: 20,
    backgroundColor: '#fff',
    alignItems: 'center',
    borderBottomWidth: 1,
    borderBottomColor: '#e9ecef',
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#2c3e50',
    marginBottom: 5,
  },
  subtitle: {
    fontSize: 16,
    color: '#6c757d',
    textAlign: 'center',
  },
  section: {
    margin: 15,
    padding: 20,
    backgroundColor: '#fff',
    borderRadius: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#2c3e50',
    marginBottom: 10,
  },
  description: {
    fontSize: 14,
    color: '#6c757d',
    marginBottom: 15,
    lineHeight: 20,
  },
  featureList: {
    marginBottom: 20,
  },
  feature: {
    fontSize: 14,
    color: '#495057',
    marginBottom: 5,
    lineHeight: 20,
  },
  generateButton: {
    backgroundColor: '#28a745',
    padding: 15,
    borderRadius: 8,
    alignItems: 'center',
    marginBottom: 10,
  },
  disabledButton: {
    backgroundColor: '#6c757d',
  },
  generateButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
  statusButton: {
    backgroundColor: '#007bff',
    padding: 12,
    borderRadius: 8,
    alignItems: 'center',
  },
  statusButtonText: {
    color: '#fff',
    fontSize: 14,
    fontWeight: '500',
  },
  statusSection: {
    margin: 15,
    padding: 15,
    backgroundColor: '#fff',
    borderRadius: 12,
    borderLeftWidth: 4,
    borderLeftColor: '#17a2b8',
  },
  statusTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#2c3e50',
    marginBottom: 8,
  },
  statusText: {
    fontSize: 14,
    color: '#495057',
    fontFamily: 'monospace',
  },
  statsContainer: {
    marginTop: 15,
    padding: 12,
    backgroundColor: '#f8f9fa',
    borderRadius: 8,
  },
  statsTitle: {
    fontSize: 14,
    fontWeight: '600',
    color: '#2c3e50',
    marginBottom: 8,
  },
  stat: {
    fontSize: 13,
    color: '#495057',
    marginBottom: 3,
  },
  infoSection: {
    margin: 15,
    padding: 15,
    backgroundColor: '#e3f2fd',
    borderRadius: 12,
  },
  infoTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#1976d2',
    marginBottom: 8,
  },
  infoText: {
    fontSize: 14,
    color: '#1565c0',
    lineHeight: 20,
  },
});
