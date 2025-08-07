package com.zjw.sdkdemo.livedata;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Log;

import com.blankj.utilcode.util.ActivityUtils;
import com.zhapp.ble.ControlBleTools;
import com.zhapp.ble.bean.BodyTemperatureSettingBean;
import com.zhapp.ble.bean.BreathingLightSettingsBean;
import com.zhapp.ble.bean.ClassicBluetoothStateBean;
import com.zhapp.ble.bean.ClockInfoBean;
import com.zhapp.ble.bean.CommonReminderBean;
import com.zhapp.ble.bean.ContinuousBloodOxygenSettingsBean;
import com.zhapp.ble.bean.DeviceBatteryValueBean;
import com.zhapp.ble.bean.DoNotDisturbModeBean;
import com.zhapp.ble.bean.EvDataInfoBean;
import com.zhapp.ble.bean.EventInfoBean;
import com.zhapp.ble.bean.FindWearSettingsBean;
import com.zhapp.ble.bean.HeartRateMonitorBean;
import com.zhapp.ble.bean.NotificationSettingsBean;
import com.zhapp.ble.bean.PressureModeBean;
import com.zhapp.ble.bean.SchedulerBean;
import com.zhapp.ble.bean.SchoolBean;
import com.zhapp.ble.bean.ScreenDisplayBean;
import com.zhapp.ble.bean.ScreenSettingBean;
import com.zhapp.ble.bean.SimpleSettingSummaryBean;
import com.zhapp.ble.bean.SleepModeBean;
import com.zhapp.ble.bean.SleepReminder;
import com.zhapp.ble.bean.WidgetBean;
import com.zhapp.ble.bean.WorldClockBean;
import com.zhapp.ble.bean.WristScreenBean;
import com.zhapp.ble.callback.BehaviorLogCallBack;
import com.zhapp.ble.callback.CallBackUtils;
import com.zhapp.ble.callback.CallStateCallBack;
import com.zhapp.ble.callback.DeviceBatteryReportingCallBack;
import com.zhapp.ble.callback.DeviceLogCallBack;
import com.zhapp.ble.callback.DeviceOpenNotifyAppCallBack;
import com.zhapp.ble.callback.QuickReplyCallBack;
import com.zhapp.ble.callback.SettingMenuCallBack;
import com.zhapp.ble.callback.WhatsAppQuickReplyCallBack;
import com.zhapp.ble.utils.SaveLog;
import com.zjw.sdkdemo.R;
import com.zjw.sdkdemo.app.MyApplication;
import com.zjw.sdkdemo.utils.DevSportManager;
import com.zjw.sdkdemo.utils.MicroManager;
import com.zjw.sdkdemo.utils.ToastDialog;

import java.util.ArrayList;
import java.util.List;

import kotlin.Pair;

/**
 * Created by Android on 2021/10/21.
 */
public final class DeviceSettingLiveData {
    private final static String TAG = DeviceSettingLiveData.class.getSimpleName();

    private static class SingletonHolder {
        public static final DeviceSettingLiveData INSTANCE = new DeviceSettingLiveData();
    }

    public static DeviceSettingLiveData getInstance() {
        return SingletonHolder.INSTANCE;
    }

    //设备回复来电
    private final UnFlawedLiveData<Integer> mCallState;
    //设备快捷回复信息
    private final UnFlawedLiveData<ArrayList<String>> mShortReply;
    //WhatsApp回复信息
    private final UnFlawedLiveData<ArrayList<String>> mWhatsAppReply;
    //震动强度
    private final UnFlawedLiveData<Integer> mVibrationMode;
    //震动强度
    private final UnFlawedLiveData<Integer> mVibrationDuration;
    //省电设置
    private final UnFlawedLiveData<Boolean> mPowerSaving;
    //覆盖息屏
    private final UnFlawedLiveData<Boolean> mOverlayScreen;
    //快速眼动
    private final UnFlawedLiveData<Boolean> mRapidEyeMovement;
    //抬腕亮屏
    private final UnFlawedLiveData<WristScreenBean> mWristScreen;
    //勿扰模式
    private final UnFlawedLiveData<DoNotDisturbModeBean> mDoNotDisturbMode;
    //心率检测
    private final UnFlawedLiveData<HeartRateMonitorBean> mHeartRateMonitor;
    //息屏设置
    private final UnFlawedLiveData<ScreenDisplayBean> mScreenDisplay;
    //屏幕设置
    private final UnFlawedLiveData<ScreenSettingBean> mScreenSetting;
    //学校模式
    private final UnFlawedLiveData<SchoolBean> mSchoolBean;
    //通话蓝牙状态
    private final UnFlawedLiveData<ClassicBluetoothStateBean> mClassicBluetoothStateBean;
    //调度器
    private final UnFlawedLiveData<SchedulerBean> mScheduler;
    //睡眠模式
    private final UnFlawedLiveData<SleepModeBean> mSleepMode;
    //压力模式设置
    private final UnFlawedLiveData<PressureModeBean> mPressureMode;
    //通知设置
    private final UnFlawedLiveData<NotificationSettingsBean> mNotificationSettings;
    //连续血氧设置
    private final UnFlawedLiveData<ContinuousBloodOxygenSettingsBean> mContinuousBloodOxygenSettings;
    //找手表设置
    private final UnFlawedLiveData<FindWearSettingsBean> mFindWearSettingsBean;
    //灯光设置
    private final UnFlawedLiveData<BreathingLightSettingsBean> mBreathingLightSettingsBean;
    //灯光设置
    private final UnFlawedLiveData<EvDataInfoBean> mEvDataInfoBean;
    //灯光设置
    private final UnFlawedLiveData<Integer> mRemindType;
    //左键功能自定义
    private final UnFlawedLiveData<Integer> mCustomizeSet;
    //久坐提醒
    private final UnFlawedLiveData<CommonReminderBean> mSedentaryReminder;
    //喝水提醒
    private final UnFlawedLiveData<CommonReminderBean> mDrinkWaterReminder;
    //吃药提醒
    private final UnFlawedLiveData<CommonReminderBean> mMedicationReminder;
    //吃饭提醒
    private final UnFlawedLiveData<CommonReminderBean> mHaveMealsReminder;
    //洗手提醒
    private final UnFlawedLiveData<CommonReminderBean> mWashHandReminder;
    //睡眠提醒
    private final UnFlawedLiveData<SleepReminder> mSleepReminder;
    //事件提醒
    private final UnFlawedLiveData<List<EventInfoBean>> mEventInfo;
    private final UnFlawedLiveData<Integer> mEventMax;
    //闹钟提醒
    private final UnFlawedLiveData<List<ClockInfoBean>> mClockInfo;
    private final UnFlawedLiveData<Integer> mClockMax;
    //设备应用列表
    private final UnFlawedLiveData<List<WidgetBean>> applicationList;
    //设备直达卡片列表
    private final UnFlawedLiveData<List<WidgetBean>> widgetList;
    //设备运动排序列表
    private final UnFlawedLiveData<List<WidgetBean>> sportWidgetList;
    //世界时钟列表
    private final UnFlawedLiveData<List<WorldClockBean>> worldClockList;
    //戒指设备电量主动上报
    private final UnFlawedLiveData<DeviceBatteryValueBean> mDeviceBatteryValueBean;
    //简单设置汇总
    private final UnFlawedLiveData<SimpleSettingSummaryBean> mSimpleSettingSummaryBean;
    //运动自失败
    private final UnFlawedLiveData<Pair<Boolean,Boolean>> mMotionRecognition;

    //region 获取LiveData方法
    public UnFlawedLiveData<Integer> getmCallState() {
        return mCallState;
    }

    public UnFlawedLiveData<ArrayList<String>> getmShortReply() {
        return mShortReply;
    }

    public UnFlawedLiveData<ArrayList<String>> getmWhatsAppReply() {
        return mWhatsAppReply;
    }

    public UnFlawedLiveData<Integer> getmVibrationMode() {
        return mVibrationMode;
    }

    public UnFlawedLiveData<Integer> getmVibrationDuration() {
        return mVibrationDuration;
    }

    public UnFlawedLiveData<Boolean> getmPowerSaving() {
        return mPowerSaving;
    }

    public UnFlawedLiveData<Boolean> getmOverlayScreen() {
        return mOverlayScreen;
    }

    public UnFlawedLiveData<Boolean> getmRapidEyeMovement() {
        return mRapidEyeMovement;
    }

    public UnFlawedLiveData<WristScreenBean> getmWristScreen() {
        return mWristScreen;
    }

    public UnFlawedLiveData<DoNotDisturbModeBean> getmDoNotDisturbMode() {
        return mDoNotDisturbMode;
    }

    public UnFlawedLiveData<HeartRateMonitorBean> getmHeartRateMonitor() {
        return mHeartRateMonitor;
    }

    public UnFlawedLiveData<ScreenDisplayBean> getmScreenDisplay() {
        return mScreenDisplay;
    }

    public UnFlawedLiveData<ScreenSettingBean> getmScreenSetting() {
        return mScreenSetting;
    }

    public UnFlawedLiveData<SchoolBean> getmSchoolBean() {
        return mSchoolBean;
    }

    public UnFlawedLiveData<ClassicBluetoothStateBean> getmClassicBluetoothStateBean() {
        return mClassicBluetoothStateBean;
    }

    public UnFlawedLiveData<SchedulerBean> getmScheduler() {
        return mScheduler;
    }

    public UnFlawedLiveData<SleepModeBean> getmSleepMode() {
        return mSleepMode;
    }

    public UnFlawedLiveData<PressureModeBean> getmPressureMode() {
        return mPressureMode;
    }

    public UnFlawedLiveData<NotificationSettingsBean> getmNotificationSettings() {
        return mNotificationSettings;
    }

    public UnFlawedLiveData<ContinuousBloodOxygenSettingsBean> getmContinuousBloodOxygenSettings() {
        return mContinuousBloodOxygenSettings;
    }

    public UnFlawedLiveData<FindWearSettingsBean> getmFindWearSettingsBean() {
        return mFindWearSettingsBean;
    }

    public UnFlawedLiveData<BreathingLightSettingsBean> getmBreathingLightSettingsBean() {
        return mBreathingLightSettingsBean;
    }

    public UnFlawedLiveData<EvDataInfoBean> getmEvDataInfoBean() {
        return mEvDataInfoBean;
    }

    public UnFlawedLiveData<Integer> getmRemindType() {
        return mRemindType;
    }

    public UnFlawedLiveData<Integer> getmCustomizeSet() {
        return mCustomizeSet;
    }

    public UnFlawedLiveData<CommonReminderBean> getmSedentaryReminder() {
        return mSedentaryReminder;
    }

    public UnFlawedLiveData<CommonReminderBean> getmDrinkWaterReminder() {
        return mDrinkWaterReminder;
    }

    public UnFlawedLiveData<CommonReminderBean> getmMedicationReminder() {
        return mMedicationReminder;
    }

    public UnFlawedLiveData<CommonReminderBean> getmHaveMealsReminder() {
        return mHaveMealsReminder;
    }

    public UnFlawedLiveData<CommonReminderBean> getmWashHandReminder() {
        return mWashHandReminder;
    }

    public UnFlawedLiveData<SleepReminder> getmSleepReminder() {
        return mSleepReminder;
    }

    public UnFlawedLiveData<List<EventInfoBean>> getmEventInfo() {
        return mEventInfo;
    }

    public UnFlawedLiveData<Integer> getmEventMax() {
        return mEventMax;
    }

    public UnFlawedLiveData<List<ClockInfoBean>> getmClockInfo() {
        return mClockInfo;
    }

    public UnFlawedLiveData<Integer> getmClockMax() {
        return mClockMax;
    }

    public UnFlawedLiveData<List<WidgetBean>> getApplicationList() {
        return applicationList;
    }

    public UnFlawedLiveData<List<WidgetBean>> getWidgetList() {
        return widgetList;
    }

    public UnFlawedLiveData<List<WidgetBean>> getSportWidgetList() {
        return sportWidgetList;
    }

    public UnFlawedLiveData<List<WorldClockBean>> getWorldClockList() {
        return worldClockList;
    }

    public UnFlawedLiveData<DeviceBatteryValueBean> getmDeviceBatteryValueBean() {
        return mDeviceBatteryValueBean;
    }

    public UnFlawedLiveData<SimpleSettingSummaryBean> getmSimpleSettingSummaryBean() {
        return mSimpleSettingSummaryBean;
    }

    public UnFlawedLiveData<Pair<Boolean, Boolean>> getmMotionRecognition() {
        return mMotionRecognition;
    }

    //endregion

    private DeviceSettingLiveData() {
        mCallState = new UnFlawedLiveData();
        mShortReply = new UnFlawedLiveData();
        mWhatsAppReply = new UnFlawedLiveData();
        mVibrationMode = new UnFlawedLiveData();
        mVibrationDuration = new UnFlawedLiveData<>();
        mPowerSaving = new UnFlawedLiveData();
        mOverlayScreen = new UnFlawedLiveData();
        mRapidEyeMovement = new UnFlawedLiveData();
        mWristScreen = new UnFlawedLiveData();
        mDoNotDisturbMode = new UnFlawedLiveData();
        mHeartRateMonitor = new UnFlawedLiveData();
        mScreenDisplay = new UnFlawedLiveData();
        mScreenSetting = new UnFlawedLiveData();
        mSchoolBean = new UnFlawedLiveData();
        mClassicBluetoothStateBean = new UnFlawedLiveData<>();
        mScheduler = new UnFlawedLiveData();
        mSleepMode = new UnFlawedLiveData();
        mPressureMode = new UnFlawedLiveData<>();
        mNotificationSettings = new UnFlawedLiveData<>();
        mContinuousBloodOxygenSettings = new UnFlawedLiveData<>();
        mFindWearSettingsBean = new UnFlawedLiveData<>();
        mBreathingLightSettingsBean = new UnFlawedLiveData<>();
        mEvDataInfoBean = new UnFlawedLiveData<>();
        mRemindType = new UnFlawedLiveData<>();
        mCustomizeSet = new UnFlawedLiveData<>();
        mSedentaryReminder = new UnFlawedLiveData();
        mDrinkWaterReminder = new UnFlawedLiveData();
        mMedicationReminder = new UnFlawedLiveData();
        mHaveMealsReminder = new UnFlawedLiveData();
        mWashHandReminder = new UnFlawedLiveData();
        mSleepReminder = new UnFlawedLiveData();
        mEventInfo = new UnFlawedLiveData();
        mEventMax = new UnFlawedLiveData();
        mClockInfo = new UnFlawedLiveData();
        mClockMax = new UnFlawedLiveData();
        applicationList = new UnFlawedLiveData();
        widgetList = new UnFlawedLiveData();
        sportWidgetList = new UnFlawedLiveData<>();
        worldClockList = new UnFlawedLiveData<>();
        mDeviceBatteryValueBean = new UnFlawedLiveData<>();
        mSimpleSettingSummaryBean = new UnFlawedLiveData<>();
        mMotionRecognition = new UnFlawedLiveData<>();
    }

    public void initCallBack() {

//        ControlBleTools.getInstance().setBleStateCallBack(new BleStateCallBack() {
//            @Override
//            public void onConnectState(int state) {
//                BleConnectState.getInstance().postValue(state);
//            }
//        });

        /**
         * 设备日志 Device log
         * */
        ControlBleTools.getInstance().setDeviceLogCallBack(new DeviceLogCallBack() {
            @Override
            public void onLogI(String mode, String tag, String msg) {
                if (TextUtils.isEmpty(mode)) {
                    Log.i(tag, msg);
                    //TODO save log
                    SaveLog.writeFile(tag, msg);
                } else {
                    StringBuilder str = new StringBuilder();
                    str.append("<").append(mode).append(">").append("  ").append(msg);
                    Log.i(tag, str.toString());
                    SaveLog.writeFile(tag, str.toString());
                }
            }

            @Override
            public void onLogV(String mode, String tag, String msg) {
                if (TextUtils.isEmpty(mode)) {
                    Log.v(tag, msg);
                    SaveLog.writeFile(tag, msg);
                } else {
                    StringBuilder str = new StringBuilder();
                    str.append("<").append(mode).append(">").append("  ").append(msg);
                    Log.v(tag, str.toString());
                    SaveLog.writeFile(tag, str.toString());
                }
            }

            @Override
            public void onLogE(String mode, String tag, String msg) {
                if (TextUtils.isEmpty(mode)) {
                    Log.e(tag, msg);
                    SaveLog.writeFile(tag, msg);
                } else {
                    StringBuilder str = new StringBuilder();
                    str.append("<").append(mode).append(">").append("  ").append(msg);
                    Log.e(tag, str.toString());
                    SaveLog.writeFile(tag, str.toString());
                }
            }

            @Override
            public void onLogD(String mode, String tag, String msg) {
                if (TextUtils.isEmpty(mode)) {
                    Log.w(tag, msg);
                    SaveLog.writeFile(tag, msg);
                } else {
                    StringBuilder str = new StringBuilder();
                    str.append("<").append(mode).append(">").append("  ").append(msg);
                    Log.w(tag, str.toString());
                    SaveLog.writeFile(tag, str.toString());
                }
            }

            @Override
            public void onLogW(String mode, String tag, String msg) {
                if (TextUtils.isEmpty(mode)) {
                    Log.w(tag, msg);
                    SaveLog.writeFile(tag, msg);
                } else {
                    StringBuilder str = new StringBuilder();
                    str.append("<").append(mode).append(">").append("  ").append(msg);
                    Log.w(tag, str.toString());
                    SaveLog.writeFile(tag, str.toString());
                }
            }
        });

        CallBackUtils.behaviorLogCallBack = new BehaviorLogCallBack() {
            @Override
            public void onLog(String module, String tag, String msg) {
                Log.d(module, tag + " ---> " + msg);
            }
        };


        /**
         * 设备端处理来电回复回调
         * */
        CallBackUtils.callStateCallBack = new CallStateCallBack() {
            @Override
            public void onState(int state) {
                Log.i(TAG, "callStateCallBack-->" + state);
                mCallState.postValue(state);
            }
        };

        /**
         * 设备快捷回复相关
         * */
        CallBackUtils.quickReplyCallBack = new QuickReplyCallBack() {
            @Override
            public void onQuickReplyResult(ArrayList<String> data) {
                Log.d(TAG, MyApplication.context.getString(R.string.s229) + " --》 " + data.toString());
                mShortReply.postValue(data);
            }

            @Override
            public void onMessage(String phone_number, String text) {
                Log.d(TAG, MyApplication.context.getString(R.string.s242) + " --》phone_number " + phone_number + ", text = " + text);
                ToastDialog.showToast(ActivityUtils.getTopActivity(), ActivityUtils.getTopActivity().getString(R.string.s229) + "\n" + MyApplication.context.getString(R.string.s242) + " --》phone_number " + phone_number + ", text = " + text);
            }
        };

        CallBackUtils.whatsAppQuickReplyCallBack = new WhatsAppQuickReplyCallBack() {
            @Override
            public void onQuickReplyResult(ArrayList<String> data) {
                Log.d(TAG, MyApplication.context.getString(R.string.s229) + " --》 " + data.toString());
                mWhatsAppReply.postValue(data);
            }

            @Override
            public void onMessage(String phone_number, String text) {
                Log.d(TAG, MyApplication.context.getString(R.string.s242) + " --》phone_number " + phone_number + ", text = " + text);
                ToastDialog.showToast(ActivityUtils.getTopActivity(), ActivityUtils.getTopActivity().getString(R.string.s229) + "\n" + MyApplication.context.getString(R.string.s242) + " --》phone_number " + phone_number + ", text = " + text);
            }
        };

        /**
         * 设备设置相关
         * */
        CallBackUtils.settingMenuCallBack = new SettingMenuCallBack() {

            @Override
            public void onVibrationResult(int model) {
                mVibrationMode.postValue(model);
            }

            @Override
            public void onVibrationDurationResult(int duration) {
                mVibrationDuration.postValue(duration);
            }

            @Override
            public void onPowerSavingResult(boolean isOpen) {
                mPowerSaving.postValue(isOpen);
            }

            @Override
            public void onOverlayScreenResult(boolean isOpen) {
                mOverlayScreen.postValue(isOpen);
            }

            @Override
            public void onRapidEyeMovementResult(boolean isOpen) {
                mRapidEyeMovement.postValue(isOpen);
            }

            @Override
            public void onWristScreenResult(WristScreenBean bean) {
                mWristScreen.postValue(bean);
            }

            @Override
            public void onDoNotDisturbModeResult(DoNotDisturbModeBean bean) {
                mDoNotDisturbMode.postValue(bean);
            }

            @Override
            public void onHeartRateMonitorResult(HeartRateMonitorBean bean) {
                mHeartRateMonitor.postValue(bean);
            }

            @Override
            public void onScreenDisplayResult(ScreenDisplayBean bean) {
                mScreenDisplay.postValue(bean);
            }

            @Override
            public void onScreenSettingResult(ScreenSettingBean bean) {
                mScreenSetting.postValue(bean);
            }

            @Override
            public void onSedentaryReminderResult(CommonReminderBean bean) {
                mSedentaryReminder.postValue(bean);
            }

            @Override
            public void onDrinkWaterReminderResult(CommonReminderBean bean) {
                mDrinkWaterReminder.postValue(bean);
            }

            @Override
            public void onMedicationReminderResult(CommonReminderBean bean) {
                mMedicationReminder.postValue(bean);
            }

            @Override
            public void onHaveMealsReminderResult(CommonReminderBean haveMealsReminder) {
                //Log.i(TAG,"onHaveMealsReminderResult-->"+haveMealsReminder);
                mHaveMealsReminder.postValue(haveMealsReminder);
            }

            @Override
            public void onWashHandReminderResult(CommonReminderBean washHandReminder) {
                Log.i(TAG, "onWashHandReminderResult-->" + washHandReminder);
                mWashHandReminder.postValue(washHandReminder);
            }

            @Override
            public void onSleepReminder(SleepReminder sleepReminder) {
                Log.i(TAG, "onSleepReminder-->" + sleepReminder);
                mSleepReminder.postValue(sleepReminder);
            }

            @Override
            public void onEventInfoResult(List<EventInfoBean> list, int max) {
                mEventInfo.postValue(list);
                mEventMax.postValue(max);
            }

            @Override
            public void onClockInfoResult(List<ClockInfoBean> list, int max) {
                mClockInfo.postValue(list);
                mClockMax.postValue(max);
            }

            @Override
            public void onSimpleSettingResult(SimpleSettingSummaryBean simpleSettingSummaryBean) {
                mSimpleSettingSummaryBean.postValue(simpleSettingSummaryBean);
            }

            @Override
            public void onMotionRecognitionResult(boolean isAutoRecognition, boolean isAutoPause) {
                mMotionRecognition.postValue(new Pair<>(isAutoRecognition, isAutoPause));
            }

            @Override
            public void onWorldClockResult(List<WorldClockBean> list) {
                worldClockList.postValue(list);
            }

            @Override
            public void onBodyTemperatureSettingResult(BodyTemperatureSettingBean bodyTemperatureSettingBean) {
                Log.i(TAG, "onBodyTemperatureSettingResult-->" + bodyTemperatureSettingBean);
            }

            @Override
            public void onClassicBleStateSetting(ClassicBluetoothStateBean classicBluetoothStateBean) {
                Log.i(TAG, "onClassicBleStateSetting-->" + classicBluetoothStateBean);
                mClassicBluetoothStateBean.postValue(classicBluetoothStateBean);
            }

            @Override
            public void onSchoolModeResult(SchoolBean schoolBean) {
                Log.i(TAG, "onSchoolModeResult-->" + schoolBean);
                mSchoolBean.postValue(schoolBean);
            }

            @Override
            public void onSchedulerResult(SchedulerBean schedulerBean) {
                Log.i(TAG, "onSchedulerResult-->" + schedulerBean);
                mScheduler.postValue(schedulerBean);
            }

            @Override
            public void onSleepModeResult(SleepModeBean sleepModeBean) {
                Log.i(TAG, "onSleepModeResult-->" + sleepModeBean);
                mSleepMode.postValue(sleepModeBean);
            }

            @Override
            public void onPressureModeResult(PressureModeBean pressureModeBean) {
                Log.i(TAG, "onPressureModeResult-->" + pressureModeBean);
                mPressureMode.postValue(pressureModeBean);
            }

            @Override
            public void onNotificationSetting(NotificationSettingsBean settingsBean) {
                Log.i(TAG, "onNotificationSetting-->" + settingsBean);
                mNotificationSettings.postValue(settingsBean);
            }

            @Override
            public void onContinuousBloodOxygenSetting(ContinuousBloodOxygenSettingsBean settingsBean) {
                Log.i(TAG, "onContinuousBloodOxygenSetting-->" + settingsBean);
                mContinuousBloodOxygenSettings.postValue(settingsBean);
            }

            @Override
            public void onFindWearSettings(FindWearSettingsBean settingsBean) {
                Log.i(TAG, "onFindWearSettings-->" + settingsBean);
                mFindWearSettingsBean.postValue(settingsBean);
            }

            @Override
            public void onBreathingLightSettings(BreathingLightSettingsBean settingsBean) {
                Log.i(TAG, "onBreathingLightSettings-->" + settingsBean);
                mBreathingLightSettingsBean.postValue(settingsBean);
            }

            @Override
            public void onEvDataInfo(EvDataInfoBean evDataInfoBean) {
                Log.i(TAG, "onEvDataInfo-->" + evDataInfoBean);
                mEvDataInfoBean.postValue(evDataInfoBean);
            }

            @Override
            public void onEvRemindType(int remindType) {
                Log.i(TAG, "onEvRemindType-->" + remindType);
                mRemindType.postValue(remindType);
            }

            @Override
            public void onCustomizeLeftClickSettings(int setType) {
                Log.i(TAG, "onCustomizeLeftClickSettings-->" + setType);
                mCustomizeSet.postValue(setType);
            }
            //endregion
        };

        /**
         * 戒指项目设备电量主动上报
         */
        CallBackUtils.deviceBatteryReportingCallBack = new DeviceBatteryReportingCallBack() {
            @Override
            public void onBatteryReporting(DeviceBatteryValueBean bean) {
                mDeviceBatteryValueBean.postValue(bean);
            }
        };

        /**
         * 设备请求开启通知app
         */
        CallBackUtils.deviceOpenNotifyAppCallBack = new DeviceOpenNotifyAppCallBack() {
            @Override
            public void onRequestOpen(String packageName) {
                ToastDialog.showToast(ActivityUtils.getTopActivity(), "开启APP --> " + packageName);
                PackageManager packageManager = MyApplication.context.getPackageManager();
                Intent launchIntent = packageManager.getLaunchIntentForPackage(packageName);
                if (launchIntent != null) {
                    launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    MyApplication.context.startActivity(launchIntent);
                } else {
                    ToastDialog.showToast(ActivityUtils.getTopActivity(), "无法开启APP --> " + packageName);
                }
            }
        };

        //小功能综合相关回调
        MicroManager.getInstance().initMicroCallBack();

        //设备多运动相关回调
        DevSportManager.getInstance().initDevSportCallBack();
    }

    /**
     * 解绑设备时 重置数据
     */
    public void resetData() {
        mCallState.setValue(null);
        mShortReply.setValue(null);
        mVibrationMode.setValue(null);
        mPowerSaving.setValue(null);
        mWristScreen.setValue(null);
        mDoNotDisturbMode.setValue(null);
        mHeartRateMonitor.setValue(null);
        mScreenDisplay.setValue(null);
        mScreenSetting.setValue(null);
        mSchoolBean.setValue(null);
        mScheduler.setValue(null);
        mSleepMode.setValue(null);
        mSedentaryReminder.setValue(null);
        mDrinkWaterReminder.setValue(null);
        mMedicationReminder.setValue(null);
        mHaveMealsReminder.setValue(null);
        mWashHandReminder.setValue(null);
        mEventInfo.setValue(null);
        mEventMax.setValue(null);
        mClockInfo.setValue(null);
        mClockMax.setValue(null);
    }
}
