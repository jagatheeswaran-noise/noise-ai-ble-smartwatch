/**
 * Script to generate clean health data for one month
 * This creates realistic health metrics for testing the AI assistant
 */

const fs = require('fs');
const path = require('path');

// Generate realistic health data for the past 30 days
function generateHealthData() {
  const healthData = {
    user_profile: {
      age: 28,
      gender: 'male',
      height: 175, // cm
      weight: 70, // kg
      activity_level: 'moderate',
      goals: {
        target_heart_rate: 75,
        target_sleep: 8,
        target_steps: 10000,
        target_calories: 2200
      }
    },
    daily_metrics: []
  };

  const today = new Date();
  
  for (let i = 29; i >= 0; i--) {
    const date = new Date(today);
    date.setDate(date.getDate() - i);
    
    // Generate realistic variations
    const baseHeartRate = 75;
    const baseHRV = 45;
    const baseSleep = 7.5;
    const baseSteps = 8500;
    
    // Add some realistic variation
    const heartRateVariation = (Math.random() - 0.5) * 10;
    const hrvVariation = (Math.random() - 0.5) * 15;
    const sleepVariation = (Math.random() - 0.5) * 2;
    const stepsVariation = (Math.random() - 0.5) * 3000;
    
    const dayMetrics = {
      date: date.toISOString().split('T')[0],
      timestamp: date.getTime(),
      heart_rate: {
        average: Math.round(baseHeartRate + heartRateVariation),
        resting: Math.round((baseHeartRate + heartRateVariation) * 0.85),
        max: Math.round((baseHeartRate + heartRateVariation) * 1.8),
        zones: {
          fat_burn: Math.round(Math.random() * 60) + 20,
          cardio: Math.round(Math.random() * 40) + 15,
          peak: Math.round(Math.random() * 20) + 5
        }
      },
      hrv: {
        average: Math.round(baseHRV + hrvVariation),
        trend: hrvVariation > 0 ? 'improving' : 'stable',
        stress_level: Math.round(50 + (hrvVariation * -2)), // Lower HRV = higher stress
        recovery_score: Math.round(70 + hrvVariation)
      },
      sleep: {
        total_hours: Math.round((baseSleep + sleepVariation) * 10) / 10,
        deep_sleep: Math.round((1.5 + sleepVariation * 0.3) * 10) / 10,
        rem_sleep: Math.round((1.8 + sleepVariation * 0.2) * 10) / 10,
        light_sleep: Math.round((4.2 + sleepVariation * 0.5) * 10) / 10,
        sleep_score: Math.round(75 + sleepVariation * 10),
        efficiency: Math.round(85 + sleepVariation * 5)
      },
      activity: {
        steps: Math.round(baseSteps + stepsVariation),
        calories_burned: Math.round(2000 + stepsVariation * 0.1),
        active_minutes: Math.round(45 + Math.random() * 30),
        distance_km: Math.round((baseSteps + stepsVariation) * 0.0008 * 10) / 10
      },
      mood: {
        energy_level: Math.round(Math.random() * 4) + 6, // 6-10 scale
        stress_level: Math.round(30 + Math.random() * 40), // 30-70 range
        motivation: Math.round(Math.random() * 4) + 6
      }
    };
    
    healthData.daily_metrics.push(dayMetrics);
  }
  
  return healthData;
}

// Generate the data
const healthData = generateHealthData();

// Save to file
const outputPath = path.join(__dirname, 'sample_health_data.json');
fs.writeFileSync(outputPath, JSON.stringify(healthData, null, 2));

console.log(`✅ Generated health data for ${healthData.daily_metrics.length} days`);
console.log(`📁 Saved to: ${outputPath}`);
console.log(`📊 Sample metrics:`);
console.log(`   - Average Heart Rate: ${Math.round(healthData.daily_metrics.reduce((sum, day) => sum + day.heart_rate.average, 0) / healthData.daily_metrics.length)} BPM`);
console.log(`   - Average HRV: ${Math.round(healthData.daily_metrics.reduce((sum, day) => sum + day.hrv.average, 0) / healthData.daily_metrics.length)}ms`);
console.log(`   - Average Sleep: ${Math.round(healthData.daily_metrics.reduce((sum, day) => sum + day.sleep.total_hours, 0) / healthData.daily_metrics.length * 10) / 10}h`);
console.log(`   - Average Steps: ${Math.round(healthData.daily_metrics.reduce((sum, day) => sum + day.activity.steps, 0) / healthData.daily_metrics.length)}`);
