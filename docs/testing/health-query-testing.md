# Health Data Query Testing Guide

## Test Queries for Enhanced Health AI

### Heart Rate Queries
Test these queries to see intelligent heart rate analysis:

1. **"What is my average heart rate last week?"**
   - Expected: Detailed heart rate analysis with trend information
   - Response includes: Average HR, resting HR, trend analysis, and health insights

2. **"Show me my heart rate for this month"**
   - Expected: Monthly heart rate analysis
   - Response includes: 30-day average, trend comparison, and fitness insights

### Sleep Comparison Queries
Test these queries for intelligent sleep analysis:

3. **"Compare my sleep last week and this week"**
   - Expected: Detailed comparison between two weeks
   - Response includes: Sleep duration, quality scores, efficiency, and improvement recommendations

4. **"How was my sleep this week compared to last week?"**
   - Expected: Similar sleep comparison with friendly analysis
   - Response includes: Trend analysis and actionable advice

### Diet Plan Queries
Test these queries for personalized diet recommendations:

5. **"Give me a diet plan based on my health data"**
   - Expected: Comprehensive diet plan based on current health metrics
   - Response includes: Health snapshot, personalized recommendations, and sample meal plan

6. **"What nutrition do I need based on my data?"**
   - Expected: Targeted nutritional advice based on health analysis
   - Response includes: Specific food recommendations based on heart rate, sleep, stress, and recovery

### General Health Queries
These should also work with enhanced responses:

7. **"How is my health this week?"**
   - Expected: Comprehensive health summary
   - Response includes: Overall health trends and insights

8. **"What does my health data say about my fitness?"**
   - Expected: Fitness analysis based on all available health metrics

## Expected Response Format Examples

### Heart Rate Query Response:
```
Your average heart rate over the last week was 72 BPM. That's excellent news! Your heart rate trend is improving, which indicates better cardiovascular fitness and overall health. This could be a result of regular exercise, better sleep, or reduced stress levels. Your resting heart rate averaged 65 BPM over this period. This falls within a healthy range for most adults. A resting heart rate between 60-80 BPM typically indicates good cardiovascular health and fitness. This analysis is based on 7 days of heart rate data, providing a reliable overview of your cardiovascular trends.
```

### Sleep Comparison Response:
```
Let me compare your sleep between last week and this week:

**Last Week:** You averaged 7h 32m of sleep with a sleep score of 76/100 and 87% efficiency.

**This Week:** You averaged 8h 5m of sleep with a sleep score of 82/100 and 91% efficiency.

**Analysis:** Your sleep has improved this week compared to last week. You're getting significantly more sleep this week, which is excellent for your physical recovery, mental clarity, and immune system. This extra rest should help improve your energy levels and overall well-being. Your sleep quality has notably improved! This suggests better sleep depth, fewer interruptions, and more restorative rest. Factors like consistent bedtime, comfortable room temperature, and reduced screen time before bed may be contributing to this improvement. Your sleep efficiency has improved, meaning you're spending more time actually sleeping versus lying awake in bed.

**Recommendations:** Continue tracking your sleep to identify patterns and maintain good sleep hygiene practices!
```

### Diet Plan Response:
```
Based on your recent health data analysis, here's a comprehensive personalized diet plan tailored to your current health status:

**Your Current Health Profile:**
• Resting Heart Rate: 68 BPM - Good (Healthy range)
• Heart Rate Variability: 42 ms - Good stress resilience
• Sleep Quality Score: 78/100 - Good quality
• Average Sleep Duration: 7h 45m - Optimal
• Stress Level: 35% - Moderate
• Recovery Score: 85% - Excellent recovery

**Personalized Nutritional Recommendations:**
1. Your health metrics look good! Maintain a balanced diet with plenty of fruits, vegetables, lean proteins, and whole grains.

**Detailed Sample Daily Meal Plan:**

**Breakfast (7:00-8:00 AM):**
• Steel-cut oatmeal with fresh berries and almonds
• Green tea or coffee (moderate caffeine)
• Whole grain toast with avocado

**Mid-Morning Snack (10:00 AM):**
• Mixed nuts (15-20 pieces) or apple with almond butter
• Herbal tea or water with lemon

**Lunch (12:00-1:00 PM):**
• Lean protein (chicken, fish, or legumes) with quinoa
• Large mixed salad with colorful vegetables
• Olive oil and lemon dressing

**Afternoon Snack (3:30 PM):**
• Hummus with carrot sticks or whole grain crackers
• Herbal tea or infused water

**Dinner (6:00-7:00 PM):**
• Grilled lean protein with roasted vegetables
• Brown rice or quinoa
• Herbal tea for evening relaxation

**Hydration Goal:** 8-10 glasses of water daily, more if you're active

**Important Notes:**
• This plan is specifically tailored to your current health metrics and trends
• Adjust portion sizes based on your activity level and hunger cues
• Consider tracking how different foods affect your sleep and energy levels
• Consult with a registered dietitian for more detailed nutritional guidance
• Monitor how dietary changes impact your health metrics over the next few weeks
```

## Testing Notes

1. **Data Availability**: The app automatically generates 30 days of sample health data on first use
2. **Real-time Analysis**: All responses are based on actual data calculations, not static responses
3. **Contextual Responses**: Each response is tailored to the user's specific health metrics
4. **Streaming Support**: All responses support the typing effect streaming interface
5. **Fallback Support**: Queries work even when the AI model is not loaded

## Advanced Testing

Try variations of these queries to test the natural language processing:

- "What was my average heart rate this past week?"
- "Can you compare my sleep from last week to this week?"
- "I need a diet plan based on my current health"
- "Show me a nutrition plan using my health data"
- "How has my heart rate been trending?"

The system should intelligently detect the intent and provide appropriate health data analysis.
