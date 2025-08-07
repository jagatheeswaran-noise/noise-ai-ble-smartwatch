import React, { Component } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  Dimensions,
  Alert
} from 'react-native';
import healthDataManager, { DailyHealthMetrics, HealthTrends, SleepStage } from './HealthDataManager';

interface Props {}

interface State {
  metrics: DailyHealthMetrics | null;
  trends: HealthTrends | null;
  selectedPeriod: 'week' | 'month' | 'quarter';
  loading: boolean;
  error: string | null;
}

const { width } = Dimensions.get('window');

class HealthDashboard extends Component<Props, State> {
  state: State = {
    metrics: null,
    trends: null,
    selectedPeriod: 'week',
    loading: true,
    error: null,
  };

  async componentDidMount() {
    await this.loadHealthData();
  }

  loadHealthData = async () => {
    try {
      this.setState({ loading: true, error: null });
      
      // Check if we have sample data, if not generate it
      const hasSampleData = await healthDataManager.hasSampleData();
      if (!hasSampleData) {
        await healthDataManager.generateSampleData();
      }

      const [metrics, trends] = await Promise.all([
        healthDataManager.getLatestMetrics(),
        healthDataManager.calculateTrends(this.state.selectedPeriod)
      ]);

      this.setState({ 
        metrics, 
        trends, 
        loading: false 
      });
    } catch (error) {
      console.error('Error loading health data:', error);
      this.setState({ 
        error: 'Failed to load health data', 
        loading: false 
      });
    }
  };

  changePeriod = async (period: 'week' | 'month' | 'quarter') => {
    this.setState({ selectedPeriod: period, loading: true });
    
    try {
      const trends = await healthDataManager.calculateTrends(period);
      this.setState({ trends, loading: false });
    } catch (error) {
      console.error('Error loading trends:', error);
      this.setState({ 
        error: 'Failed to load trends', 
        loading: false 
      });
    }
  };

  getTrendIcon = (trend: 'improving' | 'stable' | 'declining') => {
    switch (trend) {
      case 'improving': return '↗️';
      case 'declining': return '↘️';
      default: return '➡️';
    }
  };

  getTrendColor = (trend: 'improving' | 'stable' | 'declining') => {
    switch (trend) {
      case 'improving': return '#00ff88';
      case 'declining': return '#ff6b6b';
      default: return '#cccccc';
    }
  };

  formatSleepStages = (stages: any[]) => {
    const stageLabels: { [key: string]: string } = {
      [SleepStage.LIGHT]: 'Light',
      [SleepStage.DEEP]: 'Deep',
      [SleepStage.REM]: 'REM',
      [SleepStage.AWAKE]: 'Awake'
    };

    return stages.map(stage => {
      const hours = Math.floor(stage.duration / 60);
      const minutes = stage.duration % 60;
      return `${stageLabels[stage.stage]}: ${hours}h ${minutes}m`;
    }).join(', ');
  };

  clearData = () => {
    Alert.alert(
      'Clear Health Data',
      'Are you sure you want to clear all health data? This action cannot be undone.',
      [
        { text: 'Cancel', style: 'cancel' },
        { 
          text: 'Clear', 
          style: 'destructive',
          onPress: async () => {
            await healthDataManager.clearHealthData();
            this.setState({ metrics: null, trends: null });
            await this.loadHealthData();
          }
        }
      ]
    );
  };

  render() {
    const { metrics, trends, selectedPeriod, loading, error } = this.state;

    if (loading) {
      return (
        <View style={styles.container}>
          <View style={styles.loadingContainer}>
            <Text style={styles.loadingText}>Loading health data...</Text>
          </View>
        </View>
      );
    }

    if (error) {
      return (
        <View style={styles.container}>
          <View style={styles.errorContainer}>
            <Text style={styles.errorText}>{error}</Text>
            <TouchableOpacity style={styles.retryButton} onPress={this.loadHealthData}>
              <Text style={styles.retryButtonText}>Retry</Text>
            </TouchableOpacity>
          </View>
        </View>
      );
    }

    return (
      <View style={styles.container}>
        <ScrollView style={styles.content} showsVerticalScrollIndicator={false}>
          {/* Header */}
          <View style={styles.header}>
            <Text style={styles.title}>Health Dashboard</Text>
            <TouchableOpacity style={styles.clearButton} onPress={this.clearData}>
              <Text style={styles.clearButtonText}>Clear Data</Text>
            </TouchableOpacity>
          </View>

          {/* Period Selector */}
          <View style={styles.periodSelector}>
            {(['week', 'month', 'quarter'] as const).map(period => (
              <TouchableOpacity
                key={period}
                style={[
                  styles.periodButton,
                  selectedPeriod === period && styles.selectedPeriodButton
                ]}
                onPress={() => this.changePeriod(period)}
              >
                <Text style={[
                  styles.periodButtonText,
                  selectedPeriod === period && styles.selectedPeriodButtonText
                ]}>
                  {period.charAt(0).toUpperCase() + period.slice(1)}
                </Text>
              </TouchableOpacity>
            ))}
          </View>

          {/* Current Metrics */}
          {metrics && (
            <View style={styles.section}>
              <Text style={styles.sectionTitle}>Today's Metrics</Text>
              
              {/* Heart Rate */}
              <View style={styles.metricCard}>
                <View style={styles.metricHeader}>
                  <Text style={styles.metricTitle}>Heart Rate</Text>
                  <Text style={styles.metricValue}>{metrics.heartRate.resting} BPM</Text>
                </View>
                <View style={styles.metricDetails}>
                  <Text style={styles.metricDetail}>Avg: {metrics.heartRate.average} BPM</Text>
                  <Text style={styles.metricDetail}>Range: {metrics.heartRate.min}-{metrics.heartRate.max} BPM</Text>
                </View>
              </View>

              {/* HRV */}
              <View style={styles.metricCard}>
                <View style={styles.metricHeader}>
                  <Text style={styles.metricTitle}>Heart Rate Variability</Text>
                  <Text style={styles.metricValue}>{metrics.hrv.average} ms</Text>
                </View>
                <View style={styles.metricDetails}>
                  <Text style={[styles.metricDetail, { color: this.getTrendColor(metrics.hrv.trend) }]}>
                    {this.getTrendIcon(metrics.hrv.trend)} {metrics.hrv.trend}
                  </Text>
                </View>
              </View>

              {/* Respiratory Rate */}
              <View style={styles.metricCard}>
                <View style={styles.metricHeader}>
                  <Text style={styles.metricTitle}>Respiratory Rate</Text>
                  <Text style={styles.metricValue}>{metrics.respiratoryRate.average} RPM</Text>
                </View>
                <View style={styles.metricDetails}>
                  <Text style={styles.metricDetail}>Range: {metrics.respiratoryRate.min}-{metrics.respiratoryRate.max} RPM</Text>
                </View>
              </View>

              {/* Sleep */}
              {metrics.sleep && (
                <View style={styles.metricCard}>
                  <View style={styles.metricHeader}>
                    <Text style={styles.metricTitle}>Sleep</Text>
                    <Text style={styles.metricValue}>{Math.floor(metrics.sleep.totalSleepTime / 60)}h {metrics.sleep.totalSleepTime % 60}m</Text>
                  </View>
                  <View style={styles.metricDetails}>
                    <Text style={styles.metricDetail}>Score: {metrics.sleep.sleepScore}/100</Text>
                    <Text style={styles.metricDetail}>Efficiency: {metrics.sleep.sleepEfficiency}%</Text>
                    <Text style={styles.metricDetailSmall}>
                      Bedtime: {metrics.sleep.bedTime.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </Text>
                    <Text style={styles.metricDetailSmall}>
                      Wake: {metrics.sleep.wakeTime.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </Text>
                  </View>
                </View>
              )}

              {/* Recovery & Stress */}
              <View style={styles.metricCard}>
                <View style={styles.metricHeader}>
                  <Text style={styles.metricTitle}>Recovery & Stress</Text>
                  <Text style={styles.metricValue}>{metrics.recoveryScore}%</Text>
                </View>
                <View style={styles.metricDetails}>
                  <Text style={styles.metricDetail}>
                    Stress Level: {metrics.stressLevel}% 
                    {metrics.stressLevel < 40 ? ' (Low)' : metrics.stressLevel < 70 ? ' (Moderate)' : ' (High)'}
                  </Text>
                </View>
              </View>
            </View>
          )}

          {/* Trends */}
          {trends && (
            <View style={styles.section}>
              <Text style={styles.sectionTitle}>
                {selectedPeriod.charAt(0).toUpperCase() + selectedPeriod.slice(1)} Trends
              </Text>

              <View style={styles.trendCard}>
                <View style={styles.trendHeader}>
                  <Text style={styles.trendTitle}>Heart Rate</Text>
                  <View style={styles.trendValue}>
                    <Text style={styles.trendNumber}>{trends.heartRate.average} BPM</Text>
                    <Text style={[styles.trendIndicator, { color: this.getTrendColor(trends.heartRate.trend) }]}>
                      {this.getTrendIcon(trends.heartRate.trend)} {trends.heartRate.changePercent > 0 ? '+' : ''}{trends.heartRate.changePercent}%
                    </Text>
                  </View>
                </View>
              </View>

              <View style={styles.trendCard}>
                <View style={styles.trendHeader}>
                  <Text style={styles.trendTitle}>Sleep Quality</Text>
                  <View style={styles.trendValue}>
                    <Text style={styles.trendNumber}>{trends.sleep.averageScore}/100</Text>
                    <Text style={[styles.trendIndicator, { color: this.getTrendColor(trends.sleep.trend) }]}>
                      {this.getTrendIcon(trends.sleep.trend)} {trends.sleep.trend}
                    </Text>
                  </View>
                </View>
                <View style={styles.trendDetails}>
                  <Text style={styles.trendDetail}>
                    Avg Duration: {Math.floor(trends.sleep.averageDuration / 60)}h {trends.sleep.averageDuration % 60}m
                  </Text>
                  <Text style={styles.trendDetail}>
                    Avg Efficiency: {trends.sleep.averageEfficiency}%
                  </Text>
                </View>
              </View>

              <View style={styles.trendCard}>
                <View style={styles.trendHeader}>
                  <Text style={styles.trendTitle}>HRV</Text>
                  <View style={styles.trendValue}>
                    <Text style={styles.trendNumber}>{trends.hrv.average} ms</Text>
                    <Text style={[styles.trendIndicator, { color: this.getTrendColor(trends.hrv.trend) }]}>
                      {this.getTrendIcon(trends.hrv.trend)} {trends.hrv.changePercent > 0 ? '+' : ''}{trends.hrv.changePercent}%
                    </Text>
                  </View>
                </View>
              </View>

              <View style={styles.trendCard}>
                <View style={styles.trendHeader}>
                  <Text style={styles.trendTitle}>Recovery</Text>
                  <View style={styles.trendValue}>
                    <Text style={styles.trendNumber}>{trends.recovery.average}%</Text>
                    <Text style={[styles.trendIndicator, { color: this.getTrendColor(trends.recovery.trend) }]}>
                      {this.getTrendIcon(trends.recovery.trend)} {trends.recovery.trend}
                    </Text>
                  </View>
                </View>
              </View>
            </View>
          )}
        </ScrollView>
      </View>
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0a0a0a',
  },
  content: {
    flex: 1,
    paddingHorizontal: 20,
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  loadingText: {
    color: '#cccccc',
    fontSize: 16,
  },
  errorContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 40,
  },
  errorText: {
    color: '#ff6b6b',
    fontSize: 16,
    textAlign: 'center',
    marginBottom: 20,
  },
  retryButton: {
    backgroundColor: '#00ff88',
    paddingHorizontal: 20,
    paddingVertical: 12,
    borderRadius: 8,
  },
  retryButtonText: {
    color: '#000000',
    fontSize: 16,
    fontWeight: '600',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingTop: 60,
    paddingBottom: 20,
  },
  title: {
    fontSize: 28,
    fontWeight: '700',
    color: '#ffffff',
  },
  clearButton: {
    backgroundColor: '#2a2a2a',
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 6,
  },
  clearButtonText: {
    color: '#ff6b6b',
    fontSize: 12,
    fontWeight: '500',
  },
  periodSelector: {
    flexDirection: 'row',
    backgroundColor: '#1a1a1a',
    borderRadius: 12,
    padding: 4,
    marginBottom: 24,
  },
  periodButton: {
    flex: 1,
    paddingVertical: 12,
    borderRadius: 8,
    alignItems: 'center',
  },
  selectedPeriodButton: {
    backgroundColor: '#00ff88',
  },
  periodButtonText: {
    fontSize: 14,
    fontWeight: '500',
    color: '#cccccc',
  },
  selectedPeriodButtonText: {
    color: '#000000',
  },
  section: {
    marginBottom: 32,
  },
  sectionTitle: {
    fontSize: 20,
    fontWeight: '600',
    color: '#ffffff',
    marginBottom: 16,
  },
  metricCard: {
    backgroundColor: '#1a1a1a',
    borderRadius: 16,
    padding: 20,
    marginBottom: 16,
    borderWidth: 1,
    borderColor: '#2a2a2a',
  },
  metricHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 12,
  },
  metricTitle: {
    fontSize: 16,
    fontWeight: '500',
    color: '#cccccc',
  },
  metricValue: {
    fontSize: 24,
    fontWeight: '700',
    color: '#00ff88',
  },
  metricDetails: {
    gap: 4,
  },
  metricDetail: {
    fontSize: 14,
    color: '#999999',
  },
  metricDetailSmall: {
    fontSize: 12,
    color: '#666666',
  },
  trendCard: {
    backgroundColor: '#1a1a1a',
    borderRadius: 16,
    padding: 20,
    marginBottom: 16,
    borderWidth: 1,
    borderColor: '#2a2a2a',
  },
  trendHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  trendTitle: {
    fontSize: 16,
    fontWeight: '500',
    color: '#cccccc',
  },
  trendValue: {
    alignItems: 'flex-end',
  },
  trendNumber: {
    fontSize: 20,
    fontWeight: '700',
    color: '#ffffff',
  },
  trendIndicator: {
    fontSize: 12,
    fontWeight: '500',
    marginTop: 2,
  },
  trendDetails: {
    gap: 4,
  },
  trendDetail: {
    fontSize: 14,
    color: '#999999',
  },
});

export default HealthDashboard;
