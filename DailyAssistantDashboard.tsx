import React, { Component } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  Alert,
  TextInput,
  Modal,
} from 'react-native';
import { dailyAssistant, Reminder, CalorieEntry, WeatherData } from './DailyAssistant';

interface Props {
  isVisible: boolean;
  onClose: () => void;
}

interface State {
  activeTab: 'reminders' | 'calories' | 'weather' | 'facts';
  reminders: Reminder[];
  todaysCalories: { entries: CalorieEntry[]; totalCalories: number };
  calorieBreakdown: { breakfast: number; lunch: number; dinner: number; snack: number };
  weather: WeatherData | null;
  newReminderTitle: string;
  newReminderTime: string;
  showAddReminder: boolean;
  factQuery: string;
  factResult: string;
}

export default class DailyAssistantDashboard extends Component<Props, State> {
  state: State = {
    activeTab: 'reminders',
    reminders: [],
    todaysCalories: { entries: [], totalCalories: 0 },
    calorieBreakdown: { breakfast: 0, lunch: 0, dinner: 0, snack: 0 },
    weather: null,
    newReminderTitle: '',
    newReminderTime: '',
    showAddReminder: false,
    factQuery: '',
    factResult: '',
  };

  componentDidMount() {
    if (this.props.isVisible) {
      this.loadData();
    }
  }

  componentDidUpdate(prevProps: Props) {
    if (this.props.isVisible && !prevProps.isVisible) {
      this.loadData();
    }
  }

  loadData = async () => {
    try {
      // Load reminders
      const reminders = dailyAssistant.getTodaysReminders();
      
      // Load calorie data
      const todaysCalories = dailyAssistant.getTodaysCalories();
      const calorieBreakdown = dailyAssistant.getCalorieBreakdown();
      
      // Load weather
      const weather = await dailyAssistant.getWeatherInfo();
      
      this.setState({
        reminders,
        todaysCalories,
        calorieBreakdown,
        weather,
      });
    } catch (error) {
      console.error('Error loading daily assistant data:', error);
    }
  };

  handleTabPress = (tab: 'reminders' | 'calories' | 'weather' | 'facts') => {
    this.setState({ activeTab: tab });
  };

  handleAddReminder = async () => {
    const { newReminderTitle, newReminderTime } = this.state;
    
    if (!newReminderTitle.trim()) {
      Alert.alert('Error', 'Please enter a reminder title');
      return;
    }

    try {
      // Parse time (simplified - in real app, use proper date picker)
      const [hours, minutes] = newReminderTime.split(':').map(Number);
      const reminderDate = new Date();
      reminderDate.setHours(hours || 12, minutes || 0, 0, 0);
      
      // If time is in the past, set for tomorrow
      if (reminderDate <= new Date()) {
        reminderDate.setDate(reminderDate.getDate() + 1);
      }

      await dailyAssistant.createReminder(newReminderTitle, reminderDate);
      
      this.setState({
        newReminderTitle: '',
        newReminderTime: '',
        showAddReminder: false,
      });
      
      this.loadData(); // Refresh data
      Alert.alert('Success', 'Reminder created successfully!');
    } catch (error) {
      Alert.alert('Error', 'Failed to create reminder');
    }
  };

  handleCompleteReminder = async (id: string) => {
    await dailyAssistant.completeReminder(id);
    this.loadData(); // Refresh data
  };

  handleFactQuery = () => {
    const { factQuery } = this.state;
    if (!factQuery.trim()) return;
    
    const result = dailyAssistant.getQuickFacts(factQuery);
    this.setState({ factResult: result, factQuery: '' });
  };

  renderTabButton = (tab: 'reminders' | 'calories' | 'weather' | 'facts', title: string, icon: string) => (
    <TouchableOpacity
      style={[styles.tabButton, this.state.activeTab === tab && styles.activeTab]}
      onPress={() => this.handleTabPress(tab)}
    >
      <Text style={styles.tabIcon}>{icon}</Text>
      <Text style={[styles.tabText, this.state.activeTab === tab && styles.activeTabText]}>
        {title}
      </Text>
    </TouchableOpacity>
  );

  renderRemindersTab = () => (
    <ScrollView style={styles.tabContent}>
      <View style={styles.section}>
        <View style={styles.sectionHeader}>
          <Text style={styles.sectionTitle}>Today's Reminders</Text>
          <TouchableOpacity
            style={styles.addButton}
            onPress={() => this.setState({ showAddReminder: true })}
          >
            <Text style={styles.addButtonText}>+ Add</Text>
          </TouchableOpacity>
        </View>
        
        {this.state.reminders.length === 0 ? (
          <Text style={styles.emptyText}>No reminders for today</Text>
        ) : (
          this.state.reminders.map((reminder) => (
            <View key={reminder.id} style={styles.reminderCard}>
              <View style={styles.reminderInfo}>
                <Text style={styles.reminderTitle}>{reminder.title}</Text>
                <Text style={styles.reminderTime}>
                  {reminder.dateTime.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                </Text>
                <Text style={styles.reminderType}>{reminder.type.toUpperCase()}</Text>
              </View>
              <TouchableOpacity
                style={styles.completeButton}
                onPress={() => this.handleCompleteReminder(reminder.id)}
              >
                <Text style={styles.completeButtonText}>✓</Text>
              </TouchableOpacity>
            </View>
          ))
        )}
      </View>

      {/* Add Reminder Modal */}
      <Modal visible={this.state.showAddReminder} transparent animationType="slide">
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <Text style={styles.modalTitle}>Add Reminder</Text>
            
            <TextInput
              style={styles.input}
              placeholder="Reminder title"
              value={this.state.newReminderTitle}
              onChangeText={(text) => this.setState({ newReminderTitle: text })}
            />
            
            <TextInput
              style={styles.input}
              placeholder="Time (HH:MM)"
              value={this.state.newReminderTime}
              onChangeText={(text) => this.setState({ newReminderTime: text })}
            />
            
            <View style={styles.modalButtons}>
              <TouchableOpacity
                style={styles.cancelButton}
                onPress={() => this.setState({ showAddReminder: false })}
              >
                <Text style={styles.cancelButtonText}>Cancel</Text>
              </TouchableOpacity>
              
              <TouchableOpacity
                style={styles.saveButton}
                onPress={this.handleAddReminder}
              >
                <Text style={styles.saveButtonText}>Save</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>
    </ScrollView>
  );

  renderCaloriesTab = () => (
    <ScrollView style={styles.tabContent}>
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Today's Calorie Intake</Text>
        
        <View style={styles.calorieOverview}>
          <Text style={styles.totalCalories}>{this.state.todaysCalories.totalCalories}</Text>
          <Text style={styles.caloriesLabel}>Total Calories</Text>
        </View>
        
        <View style={styles.calorieBreakdown}>
          <View style={styles.mealCard}>
            <Text style={styles.mealName}>Breakfast</Text>
            <Text style={styles.mealCalories}>{this.state.calorieBreakdown.breakfast}</Text>
          </View>
          <View style={styles.mealCard}>
            <Text style={styles.mealName}>Lunch</Text>
            <Text style={styles.mealCalories}>{this.state.calorieBreakdown.lunch}</Text>
          </View>
          <View style={styles.mealCard}>
            <Text style={styles.mealName}>Dinner</Text>
            <Text style={styles.mealCalories}>{this.state.calorieBreakdown.dinner}</Text>
          </View>
          <View style={styles.mealCard}>
            <Text style={styles.mealName}>Snacks</Text>
            <Text style={styles.mealCalories}>{this.state.calorieBreakdown.snack}</Text>
          </View>
        </View>
        
        <Text style={styles.subsectionTitle}>Recent Entries</Text>
        {this.state.todaysCalories.entries.length === 0 ? (
          <Text style={styles.emptyText}>No calorie entries today. Use voice to track your meals!</Text>
        ) : (
          this.state.todaysCalories.entries.map((entry) => (
            <View key={entry.id} style={styles.calorieEntry}>
              <View>
                <Text style={styles.foodName}>{entry.food}</Text>
                <Text style={styles.foodDetails}>
                  {entry.quantity} • {entry.meal} • {entry.timestamp.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                </Text>
              </View>
              <Text style={styles.entryCalories}>{entry.calories} cal</Text>
            </View>
          ))
        )}
      </View>
    </ScrollView>
  );

  renderWeatherTab = () => (
    <ScrollView style={styles.tabContent}>
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Weather & Environment</Text>
        
        {this.state.weather ? (
          <View>
            <View style={styles.weatherCard}>
              <Text style={styles.temperature}>{this.state.weather.temperature}°C</Text>
              <Text style={styles.condition}>{this.state.weather.condition}</Text>
              <Text style={styles.location}>{this.state.weather.location}</Text>
            </View>
            
            <View style={styles.weatherDetails}>
              <View style={styles.weatherDetailItem}>
                <Text style={styles.weatherDetailLabel}>Humidity</Text>
                <Text style={styles.weatherDetailValue}>{this.state.weather.humidity}%</Text>
              </View>
              <View style={styles.weatherDetailItem}>
                <Text style={styles.weatherDetailLabel}>Wind Speed</Text>
                <Text style={styles.weatherDetailValue}>{this.state.weather.windSpeed} km/h</Text>
              </View>
            </View>
            
            {this.state.weather.recommendation && (
              <View style={styles.recommendationCard}>
                <Text style={styles.recommendationTitle}>Health Recommendation</Text>
                <Text style={styles.recommendationText}>{this.state.weather.recommendation}</Text>
              </View>
            )}
          </View>
        ) : (
          <Text style={styles.emptyText}>Loading weather information...</Text>
        )}
      </View>
    </ScrollView>
  );

  renderFactsTab = () => (
    <ScrollView style={styles.tabContent}>
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Quick Facts & Information</Text>
        
        <View style={styles.querySection}>
          <TextInput
            style={styles.queryInput}
            placeholder="Ask me anything... (time, date, health facts)"
            value={this.state.factQuery}
            onChangeText={(text) => this.setState({ factQuery: text })}
            onSubmitEditing={this.handleFactQuery}
          />
          <TouchableOpacity style={styles.queryButton} onPress={this.handleFactQuery}>
            <Text style={styles.queryButtonText}>Ask</Text>
          </TouchableOpacity>
        </View>
        
        {this.state.factResult ? (
          <View style={styles.factResult}>
            <Text style={styles.factResultText}>{this.state.factResult}</Text>
          </View>
        ) : null}
        
        <View style={styles.quickFacts}>
          <Text style={styles.subsectionTitle}>Popular Queries</Text>
          <TouchableOpacity 
            style={styles.quickFactButton}
            onPress={() => this.setState({ factResult: dailyAssistant.getQuickFacts('what time is it') })}
          >
            <Text style={styles.quickFactText}>🕐 What time is it?</Text>
          </TouchableOpacity>
          <TouchableOpacity 
            style={styles.quickFactButton}
            onPress={() => this.setState({ factResult: dailyAssistant.getQuickFacts('what is todays date') })}
          >
            <Text style={styles.quickFactText}>📅 Today's date</Text>
          </TouchableOpacity>
          <TouchableOpacity 
            style={styles.quickFactButton}
            onPress={() => this.setState({ factResult: dailyAssistant.getQuickFacts('daily water intake') })}
          >
            <Text style={styles.quickFactText}>💧 Daily water needs</Text>
          </TouchableOpacity>
          <TouchableOpacity 
            style={styles.quickFactButton}
            onPress={() => this.setState({ factResult: dailyAssistant.getQuickFacts('sleep hours') })}
          >
            <Text style={styles.quickFactText}>😴 Sleep requirements</Text>
          </TouchableOpacity>
        </View>
      </View>
    </ScrollView>
  );

  render() {
    if (!this.props.isVisible) return null;

    return (
      <Modal visible={this.props.isVisible} animationType="slide">
        <View style={styles.container}>
          <View style={styles.header}>
            <Text style={styles.headerTitle}>Daily Assistant</Text>
            <TouchableOpacity style={styles.closeButton} onPress={this.props.onClose}>
              <Text style={styles.closeButtonText}>✕</Text>
            </TouchableOpacity>
          </View>
          
          <View style={styles.tabBar}>
            {this.renderTabButton('reminders', 'Reminders', '📅')}
            {this.renderTabButton('calories', 'Calories', '🍎')}
            {this.renderTabButton('weather', 'Weather', '🌤️')}
            {this.renderTabButton('facts', 'Facts', '💡')}
          </View>
          
          <View style={styles.content}>
            {this.state.activeTab === 'reminders' && this.renderRemindersTab()}
            {this.state.activeTab === 'calories' && this.renderCaloriesTab()}
            {this.state.activeTab === 'weather' && this.renderWeatherTab()}
            {this.state.activeTab === 'facts' && this.renderFactsTab()}
          </View>
        </View>
      </Modal>
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 20,
    backgroundColor: '#2196F3',
    paddingTop: 50,
  },
  headerTitle: {
    fontSize: 24,
    fontWeight: 'bold',
    color: 'white',
  },
  closeButton: {
    padding: 10,
  },
  closeButtonText: {
    fontSize: 18,
    color: 'white',
    fontWeight: 'bold',
  },
  tabBar: {
    flexDirection: 'row',
    backgroundColor: 'white',
    borderBottomWidth: 1,
    borderBottomColor: '#e0e0e0',
  },
  tabButton: {
    flex: 1,
    padding: 15,
    alignItems: 'center',
  },
  activeTab: {
    borderBottomWidth: 3,
    borderBottomColor: '#2196F3',
  },
  tabIcon: {
    fontSize: 20,
    marginBottom: 5,
  },
  tabText: {
    fontSize: 12,
    color: '#666',
  },
  activeTabText: {
    color: '#2196F3',
    fontWeight: 'bold',
  },
  content: {
    flex: 1,
  },
  tabContent: {
    flex: 1,
    padding: 20,
  },
  section: {
    marginBottom: 20,
  },
  sectionHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 15,
  },
  sectionTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#333',
  },
  subsectionTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#333',
    marginTop: 20,
    marginBottom: 10,
  },
  addButton: {
    backgroundColor: '#4CAF50',
    paddingHorizontal: 15,
    paddingVertical: 8,
    borderRadius: 20,
  },
  addButtonText: {
    color: 'white',
    fontWeight: 'bold',
  },
  emptyText: {
    textAlign: 'center',
    color: '#666',
    fontStyle: 'italic',
    padding: 20,
  },
  reminderCard: {
    backgroundColor: 'white',
    padding: 15,
    borderRadius: 10,
    marginBottom: 10,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  reminderInfo: {
    flex: 1,
  },
  reminderTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#333',
  },
  reminderTime: {
    fontSize: 14,
    color: '#666',
    marginTop: 2,
  },
  reminderType: {
    fontSize: 12,
    color: '#2196F3',
    marginTop: 2,
  },
  completeButton: {
    backgroundColor: '#4CAF50',
    width: 30,
    height: 30,
    borderRadius: 15,
    justifyContent: 'center',
    alignItems: 'center',
  },
  completeButtonText: {
    color: 'white',
    fontWeight: 'bold',
  },
  calorieOverview: {
    backgroundColor: 'white',
    padding: 20,
    borderRadius: 10,
    alignItems: 'center',
    marginBottom: 20,
  },
  totalCalories: {
    fontSize: 48,
    fontWeight: 'bold',
    color: '#FF5722',
  },
  caloriesLabel: {
    fontSize: 16,
    color: '#666',
  },
  calorieBreakdown: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 20,
  },
  mealCard: {
    backgroundColor: 'white',
    padding: 15,
    borderRadius: 10,
    alignItems: 'center',
    flex: 1,
    marginHorizontal: 2,
  },
  mealName: {
    fontSize: 12,
    color: '#666',
    marginBottom: 5,
  },
  mealCalories: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333',
  },
  calorieEntry: {
    backgroundColor: 'white',
    padding: 15,
    borderRadius: 10,
    marginBottom: 10,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  foodName: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#333',
  },
  foodDetails: {
    fontSize: 12,
    color: '#666',
    marginTop: 2,
  },
  entryCalories: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#FF5722',
  },
  weatherCard: {
    backgroundColor: 'white',
    padding: 30,
    borderRadius: 10,
    alignItems: 'center',
    marginBottom: 20,
  },
  temperature: {
    fontSize: 48,
    fontWeight: 'bold',
    color: '#FF9800',
  },
  condition: {
    fontSize: 18,
    color: '#666',
    marginTop: 5,
  },
  location: {
    fontSize: 14,
    color: '#999',
    marginTop: 5,
  },
  weatherDetails: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    marginBottom: 20,
  },
  weatherDetailItem: {
    backgroundColor: 'white',
    padding: 15,
    borderRadius: 10,
    alignItems: 'center',
    flex: 1,
    marginHorizontal: 5,
  },
  weatherDetailLabel: {
    fontSize: 12,
    color: '#666',
  },
  weatherDetailValue: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#333',
    marginTop: 5,
  },
  recommendationCard: {
    backgroundColor: '#E3F2FD',
    padding: 15,
    borderRadius: 10,
    borderLeftWidth: 4,
    borderLeftColor: '#2196F3',
  },
  recommendationTitle: {
    fontSize: 14,
    fontWeight: 'bold',
    color: '#1976D2',
    marginBottom: 5,
  },
  recommendationText: {
    fontSize: 14,
    color: '#333',
  },
  querySection: {
    flexDirection: 'row',
    marginBottom: 20,
  },
  queryInput: {
    flex: 1,
    backgroundColor: 'white',
    padding: 15,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: '#e0e0e0',
    marginRight: 10,
  },
  queryButton: {
    backgroundColor: '#2196F3',
    paddingHorizontal: 20,
    paddingVertical: 15,
    borderRadius: 10,
    justifyContent: 'center',
  },
  queryButtonText: {
    color: 'white',
    fontWeight: 'bold',
  },
  factResult: {
    backgroundColor: '#E8F5E8',
    padding: 15,
    borderRadius: 10,
    marginBottom: 20,
    borderLeftWidth: 4,
    borderLeftColor: '#4CAF50',
  },
  factResultText: {
    fontSize: 14,
    color: '#333',
  },
  quickFacts: {
    marginTop: 10,
  },
  quickFactButton: {
    backgroundColor: 'white',
    padding: 15,
    borderRadius: 10,
    marginBottom: 10,
    borderWidth: 1,
    borderColor: '#e0e0e0',
  },
  quickFactText: {
    fontSize: 14,
    color: '#333',
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  modalContent: {
    backgroundColor: 'white',
    padding: 20,
    borderRadius: 10,
    width: '80%',
  },
  modalTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    marginBottom: 20,
    textAlign: 'center',
  },
  input: {
    borderWidth: 1,
    borderColor: '#e0e0e0',
    padding: 12,
    borderRadius: 5,
    marginBottom: 15,
  },
  modalButtons: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  cancelButton: {
    backgroundColor: '#f0f0f0',
    padding: 12,
    borderRadius: 5,
    flex: 1,
    marginRight: 10,
  },
  cancelButtonText: {
    textAlign: 'center',
    color: '#333',
  },
  saveButton: {
    backgroundColor: '#2196F3',
    padding: 12,
    borderRadius: 5,
    flex: 1,
    marginLeft: 10,
  },
  saveButtonText: {
    textAlign: 'center',
    color: 'white',
    fontWeight: 'bold',
  },
});
