import React, { useState } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, Alert } from 'react-native';
import { enhancedHealthDataManager } from './EnhancedHealthDataManager';

export const DataGenerationComponent = () => {
  const [isGenerating, setIsGenerating] = useState(false);
  const [status, setStatus] = useState('');

  const generateLocalData = async () => {
    setIsGenerating(true);
    setStatus('Generating local health data...');
    
    try {
      const success = await enhancedHealthDataManager.generateHealthData({
        days: 30,
        userId: 'demo-user',
        includeVariations: true,
        realisticPatterns: true
      });
      
      if (success) {
        setStatus('✅ 30 days of health data generated!');
        Alert.alert('Success', 'Health data generated successfully!');
      } else {
        setStatus('❌ Data generation failed');
        Alert.alert('Error', 'Failed to generate health data');
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Unknown error';
      setStatus(`❌ Error: ${errorMessage}`);
      Alert.alert('Error', `Failed to generate data: ${errorMessage}`);
    } finally {
      setIsGenerating(false);
    }
  };

  const downloadExternalData = async () => {
    setIsGenerating(true);
    setStatus('Downloading external health data...');
    
    try {
      // Enable external sources first
      await enhancedHealthDataManager.toggleDataSource('github_health_datasets', true);
      
      const success = await enhancedHealthDataManager.downloadExternalHealthData();
      
      if (success) {
        setStatus('✅ External data downloaded!');
        Alert.alert('Success', 'External health data downloaded successfully!');
      } else {
        setStatus('❌ External download failed');
        Alert.alert('Error', 'Failed to download external data');
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Unknown error';
      setStatus(`❌ Error: ${errorMessage}`);
      Alert.alert('Error', `Failed to download data: ${errorMessage}`);
    } finally {
      setIsGenerating(false);
    }
  };

  const checkDataStatus = async () => {
    try {
      const dataStatus = await enhancedHealthDataManager.getDataStatus();
      Alert.alert(
        'Data Status',
        `Records: ${dataStatus.recordCount || 0}\nLast Generated: ${dataStatus.lastGenerated || 'Never'}\nLast Sync: ${dataStatus.lastSynced || 'Never'}`
      );
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Unknown error';
      Alert.alert('Error', `Failed to get status: ${errorMessage}`);
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Health Data Generation</Text>
      
      <TouchableOpacity 
        style={[styles.button, isGenerating && styles.disabled]} 
        onPress={generateLocalData}
        disabled={isGenerating}
      >
        <Text style={styles.buttonText}>Generate Local Data (30 days)</Text>
      </TouchableOpacity>

      <TouchableOpacity 
        style={[styles.button, isGenerating && styles.disabled]} 
        onPress={downloadExternalData}
        disabled={isGenerating}
      >
        <Text style={styles.buttonText}>Download External Data</Text>
      </TouchableOpacity>

      <TouchableOpacity 
        style={styles.button} 
        onPress={checkDataStatus}
      >
        <Text style={styles.buttonText}>Check Data Status</Text>
      </TouchableOpacity>

      {status ? <Text style={styles.status}>{status}</Text> : null}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    padding: 20,
    backgroundColor: '#f5f5f5',
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 20,
    textAlign: 'center',
  },
  button: {
    backgroundColor: '#007AFF',
    padding: 15,
    borderRadius: 8,
    marginBottom: 10,
    alignItems: 'center',
  },
  disabled: {
    backgroundColor: '#ccc',
  },
  buttonText: {
    color: 'white',
    fontSize: 16,
    fontWeight: '600',
  },
  status: {
    marginTop: 20,
    fontSize: 14,
    textAlign: 'center',
    color: '#333',
  },
});
