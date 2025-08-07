#!/usr/bin/env node

/**
 * Database Initialization Script
 * 
 * This script initializes the health database with realistic sample data
 * Run this script when setting up the app for the first time or when
 * you want to reset the health data.
 */

const SQLite = require('react-native-sqlite-2');

// Mock AsyncStorage for Node.js environment
const mockAsyncStorage = {
  storage: new Map(),
  async getItem(key) {
    return this.storage.get(key) || null;
  },
  async setItem(key, value) {
    this.storage.set(key, value);
  },
  async removeItem(key) {
    this.storage.delete(key);
  }
};

// Set up global AsyncStorage mock
global.AsyncStorage = mockAsyncStorage;

// Import database service
const { databaseService } = require('./DatabaseService.ts');

async function initializeDatabase() {
  try {
    console.log('🚀 Initializing health database...');
    
    // Initialize the database service
    await databaseService.initialize();
    
    console.log('✅ Database initialization completed successfully!');
    console.log('📊 Sample health data has been generated for the last 30 days');
    console.log('💡 The database includes:');
    console.log('   - Heart rate data (resting, average, max)');
    console.log('   - Sleep data (duration, quality, efficiency)');
    console.log('   - Activity data (steps, calories, active minutes)');
    console.log('   - Stress and HRV data');
    console.log('   - Recovery scores');
    console.log('   - Response templates for common health queries');
    console.log('   - User profile with default preferences');
    
    // Close database connection
    await databaseService.close();
    console.log('🔒 Database connection closed');
    
  } catch (error) {
    console.error('❌ Error initializing database:', error);
    process.exit(1);
  }
}

// Run the initialization
if (require.main === module) {
  initializeDatabase();
}

module.exports = { initializeDatabase };
