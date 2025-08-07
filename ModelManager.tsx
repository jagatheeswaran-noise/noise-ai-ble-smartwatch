import React, { Component } from 'react';
import {
  StyleSheet,
  Text,
  View,
  TouchableOpacity,
  Alert,
  Modal,
  ScrollView,
  Platform,
  Switch,
} from 'react-native';
import RNFS from 'react-native-fs';
import { llamaService } from './LlamaService';
import type { ModelInfo, ModelDownloadProgress } from './LlamaService';
import LlamaService from './LlamaService';
import { StorageManager, type StorageInfo } from './StorageManager';
import { HealthDataGeneratorSection } from './HealthDataGeneratorSection';

type Props = {
  visible: boolean;
  onClose: () => void;
  onModelStatusChange?: (isLoaded: boolean) => void;
};

type State = {
  models: ModelInfo[];
  downloadingModel: string | null;
  downloadProgress: number;
  downloadSpeed: string;
  modelStates: { [key: string]: 'not_downloaded' | 'downloading' | 'downloaded' | 'loaded' };
  autoOffloadEnabled: boolean;
  storageInfo: StorageInfo | null;
};

class ModelManager extends Component<Props, State> {
  private downloadStartTime: number = 0;
  private lastBytesWritten: number = 0;
  private speedUpdateTimer: NodeJS.Timeout | null = null;

  state: State = {
    models: [],
    downloadingModel: null,
    downloadProgress: 0,
    downloadSpeed: '0 MB/s',
    modelStates: {},
    autoOffloadEnabled: true,
    storageInfo: null,
  };

  async componentDidMount() {
    await this.loadModelInfo();
    await this.checkStorageInfo();
    this.setState({
      autoOffloadEnabled: llamaService.getAutoOffloadEnabled()
    });
  }

  componentWillUnmount() {
    if (this.speedUpdateTimer) {
      clearInterval(this.speedUpdateTimer);
    }
  }

  loadModelInfo = async () => {
    const models = LlamaService.getAvailableModels();
    const modelStates: { [key: string]: 'not_downloaded' | 'downloading' | 'downloaded' | 'loaded' } = {};

    for (const model of models) {
      const isDownloaded = await llamaService.isModelDownloaded(model);
      const isLoaded = llamaService.isReady();
      
      if (isLoaded && llamaService.getStatus().modelPath?.includes(model.filename)) {
        modelStates[model.filename] = 'loaded';
      } else if (isDownloaded) {
        modelStates[model.filename] = 'downloaded';
      } else {
        modelStates[model.filename] = 'not_downloaded';
      }
    }

    this.setState({ models, modelStates });
  };

  checkStorageInfo = async () => {
    try {
      const storageInfo = await StorageManager.getStorageInfo();
      this.setState({ storageInfo });
    } catch (error) {
      console.error('Error checking storage:', error);
    }
  };

  formatBytes = (bytes: number): string => {
    return StorageManager.formatBytes(bytes);
  };

  calculateDownloadSpeed = (bytesWritten: number) => {
    const now = Date.now();
    const timeDiff = (now - this.downloadStartTime) / 1000; // seconds
    const bytesDiff = bytesWritten - this.lastBytesWritten;
    
    if (timeDiff > 0) {
      const speed = bytesDiff / timeDiff; // bytes per second
      this.setState({ downloadSpeed: this.formatBytes(speed) + '/s' });
    }
    
    this.lastBytesWritten = bytesWritten;
  };

  downloadModel = async (model: ModelInfo) => {
    if (this.state.downloadingModel) {
      Alert.alert('Download in Progress', 'Please wait for the current download to complete.');
      return;
    }

    // Check storage space
    if (this.state.storageInfo && this.state.storageInfo.availableSpace < model.size * 1.2) {
      Alert.alert(
        'Insufficient Storage',
        `You need at least ${this.formatBytes(model.size)} of free space to download this model.`
      );
      return;
    }

    Alert.alert(
      'Download Model',
      `Download ${model.name} (${this.formatBytes(model.size)})?\n\nThis may take several minutes depending on your internet connection.`,
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Download',
          onPress: async () => {
            this.setState({ 
              downloadingModel: model.filename,
              downloadProgress: 0,
              downloadSpeed: '0 MB/s',
              modelStates: {
                ...this.state.modelStates,
                [model.filename]: 'downloading'
              }
            });

            this.downloadStartTime = Date.now();
            this.lastBytesWritten = 0;

            // Start speed calculation timer
            this.speedUpdateTimer = setInterval(() => {
              // Speed calculation is updated in progress callback
            }, 1000);

            const success = await llamaService.downloadModel(
              (progress: ModelDownloadProgress) => {
                this.setState({ downloadProgress: progress.progress });
                this.calculateDownloadSpeed(progress.bytesWritten);
              },
              model
            );

            if (this.speedUpdateTimer) {
              clearInterval(this.speedUpdateTimer);
              this.speedUpdateTimer = null;
            }

            if (success) {
              this.setState({
                downloadingModel: null,
                modelStates: {
                  ...this.state.modelStates,
                  [model.filename]: 'downloaded'
                }
              });
              Alert.alert('Success', 'Model downloaded successfully!');
            } else {
              this.setState({
                downloadingModel: null,
                modelStates: {
                  ...this.state.modelStates,
                  [model.filename]: 'not_downloaded'
                }
              });
              Alert.alert('Download Failed', 'Failed to download the model. Please check your internet connection and try again.');
            }
          }
        }
      ]
    );
  };

  loadModel = async (model: ModelInfo) => {
    try {
      const modelPath = `${RNFS.DocumentDirectoryPath}/${model.filename}`;
      const success = await llamaService.initialize(modelPath);
      
      if (success) {
        this.setState({
          modelStates: {
            ...this.state.modelStates,
            [model.filename]: 'loaded'
          }
        });
        this.props.onModelStatusChange?.(true);
        Alert.alert('Success', 'Model loaded successfully!');
      } else {
        Alert.alert('Load Failed', 'Failed to load the model. Please try again.');
      }
    } catch (error) {
      console.error('Error loading model:', error);
      Alert.alert('Error', 'An error occurred while loading the model.');
    }
  };

  unloadModel = async (model: ModelInfo) => {
    await llamaService.offloadModel();
    this.setState({
      modelStates: {
        ...this.state.modelStates,
        [model.filename]: 'downloaded'
      }
    });
    this.props.onModelStatusChange?.(false);
    Alert.alert('Success', 'Model unloaded from memory.');
  };

  deleteModel = async (model: ModelInfo) => {
    Alert.alert(
      'Delete Model',
      `Are you sure you want to delete ${model.name}? You will need to download it again to use AI features.`,
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Delete',
          style: 'destructive',
          onPress: async () => {
            const success = await llamaService.deleteModel(model);
            if (success) {
              this.setState({
                modelStates: {
                  ...this.state.modelStates,
                  [model.filename]: 'not_downloaded'
                }
              });
              this.props.onModelStatusChange?.(false);
              Alert.alert('Success', 'Model deleted successfully.');
            } else {
              Alert.alert('Error', 'Failed to delete the model.');
            }
          }
        }
      ]
    );
  };

  toggleAutoOffload = (enabled: boolean) => {
    llamaService.setAutoOffloadEnabled(enabled);
    this.setState({ autoOffloadEnabled: enabled });
  };

  renderModelItem = (model: ModelInfo) => {
    const state = this.state.modelStates[model.filename] || 'not_downloaded';
    const isDownloading = this.state.downloadingModel === model.filename;

    return (
      <View key={model.filename} style={styles.modelItem}>
        <View style={styles.modelInfo}>
          <Text style={styles.modelName}>{model.name}</Text>
          <Text style={styles.modelDescription}>{model.description}</Text>
          <Text style={styles.modelSize}>Size: {this.formatBytes(model.size)}</Text>
          
          {isDownloading && (
            <View style={styles.downloadProgress}>
              <View style={styles.progressBarContainer}>
                <View 
                  style={[styles.progressBar, { width: `${this.state.downloadProgress * 100}%` }]} 
                />
              </View>
              <Text style={styles.progressText}>
                {Math.round(this.state.downloadProgress * 100)}% - {this.state.downloadSpeed}
              </Text>
            </View>
          )}
        </View>

        <View style={styles.modelActions}>
          {state === 'not_downloaded' && !isDownloading && (
            <TouchableOpacity
              style={[styles.actionButton, styles.downloadButton]}
              onPress={() => this.downloadModel(model)}
            >
              <Text style={styles.actionButtonText}>Download</Text>
            </TouchableOpacity>
          )}

          {state === 'downloaded' && (
            <>
              <TouchableOpacity
                style={[styles.actionButton, styles.loadButton]}
                onPress={() => this.loadModel(model)}
              >
                <Text style={styles.actionButtonText}>Load</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={[styles.actionButton, styles.deleteButton]}
                onPress={() => this.deleteModel(model)}
              >
                <Text style={styles.actionButtonText}>Delete</Text>
              </TouchableOpacity>
            </>
          )}

          {state === 'loaded' && (
            <TouchableOpacity
              style={[styles.actionButton, styles.unloadButton]}
              onPress={() => this.unloadModel(model)}
            >
              <Text style={styles.actionButtonText}>Unload</Text>
            </TouchableOpacity>
          )}
        </View>

        <View style={[styles.statusIndicator, styles[`status_${state}`]]} />
      </View>
    );
  };

  render() {
    return (
      <Modal
        visible={this.props.visible}
        animationType="slide"
        presentationStyle="pageSheet"
        onRequestClose={this.props.onClose}
      >
        <View style={styles.container}>
          <View style={styles.header}>
            <Text style={styles.title}>AI Model Manager</Text>
            <TouchableOpacity
              style={styles.closeButton}
              onPress={this.props.onClose}
            >
              <Text style={styles.closeButtonText}>Done</Text>
            </TouchableOpacity>
          </View>

          <ScrollView style={styles.content}>
            {/* Auto-offload setting */}
            <View style={styles.settingsSection}>
              <View style={styles.settingItem}>
                <View style={styles.settingInfo}>
                  <Text style={styles.settingTitle}>Auto Memory Management</Text>
                  <Text style={styles.settingDescription}>
                    Automatically unload models when app goes to background to save memory
                  </Text>
                </View>
                <Switch
                  value={this.state.autoOffloadEnabled}
                  onValueChange={this.toggleAutoOffload}
                  trackColor={{ false: '#3e3e3e', true: '#00ff88' }}
                  thumbColor={this.state.autoOffloadEnabled ? '#ffffff' : '#f4f3f4'}
                />
              </View>
            </View>

            {/* Storage info */}
            {this.state.storageInfo && (
              <View style={styles.storageSection}>
                <Text style={styles.sectionTitle}>Storage</Text>
                <View style={styles.storageCard}>
                  <View style={styles.storageBar}>
                    <View 
                      style={[
                        styles.storageUsed, 
                        { 
                          width: `${this.state.storageInfo.usedPercentage}%`,
                          backgroundColor: this.state.storageInfo.usedPercentage > 85 ? '#ff4757' : '#00ff88'
                        }
                      ]} 
                    />
                  </View>
                  <View style={styles.storageDetails}>
                    <Text style={styles.storageText}>
                      Available: {this.formatBytes(this.state.storageInfo.availableSpace)}
                    </Text>
                    <Text style={styles.storageText}>
                      Used: {this.formatBytes(this.state.storageInfo.usedSpace)} ({this.state.storageInfo.usedPercentage.toFixed(1)}%)
                    </Text>
                    <Text style={styles.storageText}>
                      Total: {this.formatBytes(this.state.storageInfo.totalSpace)}
                    </Text>
                  </View>
                </View>
              </View>
            )}

            {/* Models list */}
            <View style={styles.modelsSection}>
              <Text style={styles.sectionTitle}>Available Models</Text>
              {this.state.models.map(this.renderModelItem)}
            </View>

            {/* Help text */}
            <View style={styles.helpSection}>
              <Text style={styles.helpTitle}>About Models</Text>
              <Text style={styles.helpText}>
                • Downloaded models enable full AI capabilities{'\n'}
                • Models are stored locally for privacy{'\n'}
                • Larger models provide better responses but use more memory{'\n'}
                • Auto memory management helps optimize performance
              </Text>
            </View>

            {/* Health Data Generator Section */}
            <HealthDataGeneratorSection />
          </ScrollView>
        </View>
      </Modal>
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0a0a0a',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingTop: 60,
    paddingBottom: 20,
    borderBottomWidth: 1,
    borderBottomColor: '#2a2a2a',
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#ffffff',
  },
  closeButton: {
    paddingVertical: 8,
    paddingHorizontal: 16,
    backgroundColor: '#00ff88',
    borderRadius: 20,
  },
  closeButtonText: {
    color: '#000000',
    fontWeight: '600',
  },
  content: {
    flex: 1,
    paddingHorizontal: 20,
  },
  settingsSection: {
    marginTop: 20,
    marginBottom: 30,
  },
  settingItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    backgroundColor: '#1a1a1a',
    padding: 16,
    borderRadius: 12,
  },
  settingInfo: {
    flex: 1,
    marginRight: 16,
  },
  settingTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#ffffff',
    marginBottom: 4,
  },
  settingDescription: {
    fontSize: 14,
    color: '#cccccc',
  },
  storageSection: {
    marginBottom: 30,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#ffffff',
    marginBottom: 12,
  },
  storageText: {
    fontSize: 14,
    color: '#cccccc',
    marginBottom: 4,
  },
  storageCard: {
    backgroundColor: '#1a1a1a',
    padding: 16,
    borderRadius: 12,
  },
  storageBar: {
    height: 8,
    backgroundColor: '#2a2a2a',
    borderRadius: 4,
    marginBottom: 12,
    overflow: 'hidden',
  },
  storageUsed: {
    height: '100%',
    borderRadius: 4,
  },
  storageDetails: {
    gap: 4,
  },
  modelsSection: {
    marginBottom: 30,
  },
  modelItem: {
    backgroundColor: '#1a1a1a',
    borderRadius: 12,
    padding: 16,
    marginBottom: 12,
    position: 'relative',
  },
  modelInfo: {
    marginBottom: 12,
  },
  modelName: {
    fontSize: 16,
    fontWeight: '600',
    color: '#ffffff',
    marginBottom: 4,
  },
  modelDescription: {
    fontSize: 14,
    color: '#cccccc',
    marginBottom: 8,
  },
  modelSize: {
    fontSize: 12,
    color: '#999999',
  },
  downloadProgress: {
    marginTop: 12,
  },
  progressBarContainer: {
    height: 4,
    backgroundColor: '#2a2a2a',
    borderRadius: 2,
    marginBottom: 8,
  },
  progressBar: {
    height: '100%',
    backgroundColor: '#00ff88',
    borderRadius: 2,
  },
  progressText: {
    fontSize: 12,
    color: '#cccccc',
  },
  modelActions: {
    flexDirection: 'row',
    gap: 8,
  },
  actionButton: {
    paddingVertical: 8,
    paddingHorizontal: 16,
    borderRadius: 20,
    minWidth: 80,
    alignItems: 'center',
  },
  downloadButton: {
    backgroundColor: '#00ff88',
  },
  loadButton: {
    backgroundColor: '#007bff',
  },
  unloadButton: {
    backgroundColor: '#ffa500',
  },
  deleteButton: {
    backgroundColor: '#ff4757',
  },
  actionButtonText: {
    color: '#ffffff',
    fontWeight: '600',
    fontSize: 14,
  },
  statusIndicator: {
    position: 'absolute',
    top: 12,
    right: 12,
    width: 12,
    height: 12,
    borderRadius: 6,
  },
  status_not_downloaded: {
    backgroundColor: '#666666',
  },
  status_downloading: {
    backgroundColor: '#ffa500',
  },
  status_downloaded: {
    backgroundColor: '#007bff',
  },
  status_loaded: {
    backgroundColor: '#00ff88',
  },
  helpSection: {
    marginBottom: 40,
  },
  helpTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#ffffff',
    marginBottom: 8,
  },
  helpText: {
    fontSize: 14,
    color: '#cccccc',
    lineHeight: 20,
  },
});

export default ModelManager;
