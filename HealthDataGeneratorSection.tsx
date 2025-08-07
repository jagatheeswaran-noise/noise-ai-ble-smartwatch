import React, { useState } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, Alert } from 'react-native';
import { enhancedHealthDataManager } from './EnhancedHealthDataManager';

interface DataStats {
  hasData: boolean;
  lastGenerated?: string;
  daysCovered?: number;
  lastSynced?: string;
  recordCount?: number;
}

interface HealthDataGeneratorSectionProps {
  style?: any;
}

export const HealthDataGeneratorSection: React.FC<HealthDataGeneratorSectionProps> = ({ style }) => {
  const [isGenerating, setIsGenerating] = useState(false);
  const [dataStats, setDataStats] = useState<DataStats | null>(null);
  const [lastAction, setLastAction] = useState<string>('');

  React.useEffect(() => {
    checkDataStatus();
  }, []);

  const checkDataStatus = async () => {
    try {
      const stats = await enhancedHealthDataManager.getDataStatus();
      setDataStats(stats);
    } catch (error) {
      // Silent fail for initial check
    }
  };

  const generateLocalData = async () => {
    setIsGenerating(true);
    setLastAction('Generating local health data...');
    
    try {
      const success = await enhancedHealthDataManager.generateHealthData({
        days: 30,
        userId: 'demo-user',
        includeVariations: true,
        realisticPatterns: true
      });
      
      if (success) {
        setLastAction('30 days of health data generated');
        await checkDataStatus();
        Alert.alert('Success', 'Health data generated successfully! Your AI assistant now has comprehensive health patterns for personalized advice.');
      } else {
        setLastAction('Data generation failed');
        Alert.alert('Error', 'Failed to generate health data');
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Unknown error';
      setLastAction(`Error: ${errorMessage}`);
      Alert.alert('Error', `Failed to generate data: ${errorMessage}`);
    } finally {
      setIsGenerating(false);
    }
  };

  const downloadExternalData = async () => {
    setIsGenerating(true);
    setLastAction('Downloading external health data...');
    
    try {
      await enhancedHealthDataManager.toggleDataSource('github_health_datasets', true);
      const success = await enhancedHealthDataManager.downloadExternalHealthData();
      
      if (success) {
        setLastAction('External data downloaded');
        await checkDataStatus();
        Alert.alert('Success', 'External health data downloaded successfully!');
      } else {
        setLastAction('External download failed');
        Alert.alert('Error', 'Failed to download external data');
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Unknown error';
      setLastAction(`Error: ${errorMessage}`);
      Alert.alert('Error', `Failed to download data: ${errorMessage}`);
    } finally {
      setIsGenerating(false);
    }
  };

  const showDataStatus = () => {
    if (dataStats?.hasData) {
      Alert.alert(
        'Health Data Status',
        `Records: ${dataStats.recordCount || 'Unknown'}\nDays Covered: ${dataStats.daysCovered || 'Unknown'}\nLast Generated: ${dataStats.lastGenerated ? new Date(dataStats.lastGenerated).toLocaleDateString() : 'Never'}`,
        [{ text: 'OK', style: 'default' }]
      );
    } else {
      Alert.alert(
        'No Health Data',
        'No health data found. Generate some data first to enable personalized AI responses.',
        [{ text: 'OK', style: 'default' }]
      );
    }
  };

  return (
    <View style={[styles.section, style]}>
      <Text style={styles.sectionTitle}>Health Data Management</Text>
      
      <View style={styles.dataCard}>
        <View style={styles.dataInfo}>
          <Text style={styles.dataTitle}>Training Data</Text>
          <Text style={styles.dataDescription}>
            Generate realistic health patterns for personalized AI responses
          </Text>
          {dataStats?.hasData && (
            <View style={styles.statusContainer}>
              <View style={[styles.statusIndicator, styles.statusActive]} />
              <Text style={styles.statusText}>
                {dataStats.recordCount || 0} records • {dataStats.daysCovered || 0} days
              </Text>
            </View>
          )}
          {!dataStats?.hasData && (
            <View style={styles.statusContainer}>
              <View style={[styles.statusIndicator, styles.statusInactive]} />
              <Text style={styles.statusText}>No data available</Text>
            </View>
          )}
        </View>
      </View>

      <View style={styles.actionButtons}>
        <TouchableOpacity 
          style={[styles.actionButton, styles.generateButton, isGenerating && styles.disabledButton]}
          onPress={generateLocalData}
          disabled={isGenerating}
        >
          <View style={styles.buttonContent}>
            <View style={[styles.buttonIcon, styles.generateIcon]} />
            <Text style={styles.actionButtonText}>
              {isGenerating && lastAction.includes('Generating') ? 'Generating...' : 'Generate'}
            </Text>
          </View>
        </TouchableOpacity>

        <TouchableOpacity 
          style={[styles.actionButton, styles.downloadButton, isGenerating && styles.disabledButton]}
          onPress={downloadExternalData}
          disabled={isGenerating}
        >
          <View style={styles.buttonContent}>
            <View style={[styles.buttonIcon, styles.downloadIcon]} />
            <Text style={styles.actionButtonText}>
              {isGenerating && lastAction.includes('Downloading') ? 'Downloading...' : 'Download'}
            </Text>
          </View>
        </TouchableOpacity>

        <TouchableOpacity 
          style={[styles.actionButton, styles.statusButton]}
          onPress={showDataStatus}
        >
          <View style={styles.buttonContent}>
            <View style={[styles.buttonIcon, styles.infoIcon]} />
            <Text style={styles.actionButtonText}>Info</Text>
          </View>
        </TouchableOpacity>
      </View>

      {lastAction ? (
        <Text style={styles.lastActionText}>{lastAction}</Text>
      ) : null}

      <View style={styles.helpSection}>
        <Text style={styles.helpTitle}>About Health Data</Text>
        <Text style={styles.helpText}>
          • Generate 30 days of realistic health patterns{'\n'}
          • Enables personalized AI health advice{'\n'}
          • All data stored locally for privacy{'\n'}
          • Removes hardcoded responses for better accuracy
        </Text>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  section: {
    marginBottom: 30,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#ffffff',
    marginBottom: 12,
  },
  dataCard: {
    backgroundColor: '#1a1a1a',
    padding: 16,
    borderRadius: 12,
    marginBottom: 12,
  },
  dataInfo: {
    flex: 1,
  },
  dataTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#ffffff',
    marginBottom: 4,
  },
  dataDescription: {
    fontSize: 14,
    color: '#cccccc',
    marginBottom: 8,
  },
  statusContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 4,
  },
  statusIndicator: {
    width: 8,
    height: 8,
    borderRadius: 4,
    marginRight: 8,
  },
  statusActive: {
    backgroundColor: '#00ff88',
  },
  statusInactive: {
    backgroundColor: '#666666',
  },
  statusText: {
    fontSize: 12,
    color: '#999999',
  },
  actionButtons: {
    flexDirection: 'row',
    gap: 8,
    marginBottom: 12,
  },
  actionButton: {
    flex: 1,
    paddingVertical: 8,
    paddingHorizontal: 12,
    borderRadius: 20,
    alignItems: 'center',
    minHeight: 36,
    justifyContent: 'center',
  },
  generateButton: {
    backgroundColor: '#00ff88',
  },
  downloadButton: {
    backgroundColor: '#007bff',
  },
  statusButton: {
    backgroundColor: '#666666',
  },
  disabledButton: {
    backgroundColor: '#3e3e3e',
  },
  actionButtonText: {
    color: '#ffffff',
    fontWeight: '600',
    fontSize: 14,
  },
  buttonContent: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
  },
  buttonIcon: {
    width: 12,
    height: 12,
    borderRadius: 6,
    marginRight: 6,
  },
  generateIcon: {
    backgroundColor: '#ffffff',
  },
  downloadIcon: {
    backgroundColor: '#ffffff',
  },
  infoIcon: {
    backgroundColor: '#ffffff',
  },
  lastActionText: {
    fontSize: 12,
    color: '#cccccc',
    textAlign: 'center',
    marginBottom: 12,
    fontStyle: 'italic',
  },
  helpSection: {
    marginTop: 8,
  },
  helpTitle: {
    fontSize: 14,
    fontWeight: '600',
    color: '#ffffff',
    marginBottom: 6,
  },
  helpText: {
    fontSize: 12,
    color: '#cccccc',
    lineHeight: 16,
  },
});
