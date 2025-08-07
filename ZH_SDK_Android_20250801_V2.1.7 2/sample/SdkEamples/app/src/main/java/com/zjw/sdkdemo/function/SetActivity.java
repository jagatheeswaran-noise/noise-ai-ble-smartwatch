package com.zjw.sdkdemo.function;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.LogUtils;
import com.zhapp.ble.ControlBleTools;
import com.zhapp.ble.bean.ClassicBluetoothStateBean;
import com.zhapp.ble.bean.ContactBean;
import com.zhapp.ble.bean.ContactLotBean;
import com.zhapp.ble.bean.ContinuousBloodOxygenSettingsBean;
import com.zhapp.ble.bean.DialStyleBean;
import com.zhapp.ble.bean.DoNotDisturbModeBean;
import com.zhapp.ble.bean.EmergencyContactBean;
import com.zhapp.ble.bean.FindWearSettingsBean;
import com.zhapp.ble.bean.HeartRateMonitorBean;
import com.zhapp.ble.bean.LanguageListBean;
import com.zhapp.ble.bean.NotificationSettingsBean;
import com.zhapp.ble.bean.PhysiologicalCycleBean;
import com.zhapp.ble.bean.PressureModeBean;
import com.zhapp.ble.bean.SchedulerBean;
import com.zhapp.ble.bean.SchoolBean;
import com.zhapp.ble.bean.ScreenDisplayBean;
import com.zhapp.ble.bean.ScreenSettingBean;
import com.zhapp.ble.bean.SettingTimeBean;
import com.zhapp.ble.bean.StockInfoBean;
import com.zhapp.ble.bean.StockSymbolBean;
import com.zhapp.ble.bean.UserInfo;
import com.zhapp.ble.bean.WeatherDayBean;
import com.zhapp.ble.bean.WeatherPerHourBean;
import com.zhapp.ble.bean.WorldClockBean;
import com.zhapp.ble.bean.WristScreenBean;
import com.zhapp.ble.callback.CallBackUtils;
import com.zhapp.ble.callback.ContactCallBack;
import com.zhapp.ble.callback.ContactLotCallBack;
import com.zhapp.ble.callback.EmergencyContactsCallBack;
import com.zhapp.ble.callback.LanguageCallBack;
import com.zhapp.ble.callback.PhysiologicalCycleCallBack;
import com.zhapp.ble.callback.StockCallBack;
import com.zhapp.ble.callback.UserInfoCallBack;
import com.zhapp.ble.parsing.ParsingStateManager;
import com.zhapp.ble.parsing.SendCmdState;
import com.zjw.sdkdemo.R;
import com.zjw.sdkdemo.app.MyApplication;
import com.zjw.sdkdemo.function.language.BaseActivity;
import com.zjw.sdkdemo.function.language.LanguageActivity;
import com.zjw.sdkdemo.livedata.DeviceSettingLiveData;
import com.zjw.sdkdemo.utils.DeviceTools;
import com.zjw.sdkdemo.utils.ToastDialog;
import com.zjw.sdkdemo.utils.Utils;

import androidx.annotation.NonNull;
import androidx.lifecycle.Observer;

import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * CTRL SHIFT -
 * CTRL SHIFT +
 */
@SuppressLint("NonConstantResourceId")
public class SetActivity extends BaseActivity implements CompoundButton.OnCheckedChangeListener {
    final private String TAG = SetActivity.class.getSimpleName();
    private DeviceTools mDeviceTools = MyApplication.getDeviceTools();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set);
        setTitle(getString(R.string.s116));
        initView();
        initData();
    }

    void initView() {
        ((CheckBox) findViewById(R.id.chbTimeFormat)).setOnCheckedChangeListener(this);
        ((CheckBox) findViewById(R.id.chbDistanceUnit)).setOnCheckedChangeListener(this);
        ((CheckBox) findViewById(R.id.chbTemperatureUnit)).setOnCheckedChangeListener(this);
        ((CheckBox) findViewById(R.id.cbWeatherSwitch)).setOnCheckedChangeListener(this);
        ((CheckBox) findViewById(R.id.chbSleep)).setOnCheckedChangeListener(this);
        cbPhSwitch = findViewById(R.id.cbPhSwitch);
        cbPhSwitch2 = findViewById(R.id.cbPhSwitch2);
        etY = findViewById(R.id.etY);
        etM = findViewById(R.id.etM);
        etD = findViewById(R.id.etD);
        etTip = findViewById(R.id.etTip);
        etAllDay = findViewById(R.id.etAllDay);
        etDay = findViewById(R.id.etDay);
        btnSyncStock = findViewById(R.id.btnSyncStock);
        cbNSwitch1 = findViewById(R.id.cbNSwitch1);
        cbSSwitch1 = findViewById(R.id.cbSSwitch1);
        etDMode = findViewById(R.id.etDMode);
        etVibration = findViewById(R.id.etVibration);
        cbRSwitch = findViewById(R.id.cbRSwitch);
        cbOSwitch = findViewById(R.id.cbOSwitch);
        cbPSwitch = findViewById(R.id.cbPSwitch);
        etWMode = findViewById(R.id.etWMode);
        etWSH = findViewById(R.id.etWSH);
        etWSM = findViewById(R.id.etWSM);
        etWEH = findViewById(R.id.etWEH);
        etWEM = findViewById(R.id.etWEM);
        etWSensitivity = findViewById(R.id.etWSensitivity);
        etDSH = findViewById(R.id.etDSH);
        etDSM = findViewById(R.id.etDSM);
        etDEH = findViewById(R.id.etDEH);
        etDEM = findViewById(R.id.etDEM);
        etDStyle = findViewById(R.id.etDStyle);
        cbSSwitch2 = findViewById(R.id.cbSSwitch2);
        etLevel = findViewById(R.id.etLevel);
        etDuration = findViewById(R.id.etDuration);
        cbNSwitch2 = findViewById(R.id.cbNSwitch2);
        etNSH = findViewById(R.id.etNSH);
        etNSM = findViewById(R.id.etNSM);
        etNEH = findViewById(R.id.etNEH);
        etNEM = findViewById(R.id.etNEM);
        cbBoModeSwitch = findViewById(R.id.cbBoModeSwitch);
        cbNoticeNotLightUp = findViewById(R.id.cbNoticeNotLightUp);
        cbPrModeSwitch = findViewById(R.id.cbPrModeSwitch);
        cbHrModeSwitch = findViewById(R.id.cbHrModeSwitch);
        cbBtSwitch = findViewById(R.id.cbBtSwitch);
        etSosContactPh = findViewById(R.id.etSosContactPh);
        etSosContactName = findViewById(R.id.etSosContactName);
        cbBtRemind = findViewById(R.id.cbBtRemind);
        cbHrIsWarningSwitch = findViewById(R.id.cbHrIsWarningSwitch);
        cbHeartRateMode = findViewById(R.id.cbHeartRateMode);
        etHeartRate = findViewById(R.id.etHeartRate);
        cbHrIsSport = findViewById(R.id.cbHrIsSport);
        etHeartSport = findViewById(R.id.etHeartSport);
        cbPrReminderSwitch = findViewById(R.id.cbPrReminderSwitch);
        cbNoticeDelay = findViewById(R.id.cbNoticeDelay);
        etPhoneRemindVibrationMode = findViewById(R.id.etPhoneRemindVibrationMode);
        etPhoneRemindRingMode = findViewById(R.id.etPhoneRemindRingMode);
        etBoReminderSwitch = findViewById(R.id.etBoReminderSwitch);
        etBoSH = findViewById(R.id.etBoSH);
        etBoSM = findViewById(R.id.etBoSM);
        etBoEH = findViewById(R.id.etBoEH);
        etBoEM = findViewById(R.id.etBoEM);
        etCyContactName = findViewById(R.id.etCyContactName);
        etFwRMode = findViewById(R.id.etFwRMode);
        etFwVMode = findViewById(R.id.etFwVMode);
        etCyContactPh = findViewById(R.id.etCyContactPh);
    }

    void initData() {
        initUserInfoData();
        initPhysiologicalCycleData();
        initWristScreenData();
        initPowerSavingData();
        initOverlayScreenData();
        initRapidEyeMovementData();
        initVibrationData();
        initScreenDisplayData();
        initScreenSettingData();
        initDoNotDisturbModeData();
        initStockData();
        initContactsCallBack();
        initClassicBluetoothStateCallBack();
        initWorldClockData();
        initHeartRateMonitorData();
        initSchoolModeData();
        initGetSchedulerData();
        initPressureModeData();
        initNoticeSettings();
        initContinuousBloodOxygenSettingsData();
        initFindWearSettingsData();
        initContactsLotData();
    }

    private void showTips(String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(SetActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    //region 设置事件 设置语言 用户设置
    public void setTime(View view) {
        if (ControlBleTools.getInstance().isConnect()) {
            ControlBleTools.getInstance().setTime(System.currentTimeMillis(), true, null);
        }
    }

    public void setLanguage(View view) {
        /*if (ControlBleTools.getInstance().isConnect()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this, 3);
            builder.setTitle(getString(R.string.s119));
            builder.setIcon(R.mipmap.ic_launcher);
            builder.setItems(getResources().getStringArray(R.array.language_name), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    ControlBleTools.getInstance().setLanguage(Integer.parseInt((getResources().getStringArray(R.array.language_value))[which]), null);
                }
            });
            builder.create().show();
        }*/
        startActivity(new Intent(this, LanguageActivity.class));
    }

    public void getLanguageList(View view) {
        CallBackUtils.setLanguageCallback(new LanguageCallBack() {
            @Override
            public void onResult(LanguageListBean bean) {
                showTips(bean.toString());
            }
        });
        if (ControlBleTools.getInstance().isConnect()) {
            ControlBleTools.getInstance().getLanguageList(null);
        }
    }

    //region setUserProfile
    public void setUserProfile(View view) {
        if (!ControlBleTools.getInstance().isConnect()) return;
        UserInfo bean = testUserInfo();
        ControlBleTools.getInstance().setUserProfile(bean, null);
    }

    public void getUserProfile(View view) {
        if (!ControlBleTools.getInstance().isConnect()) return;
        ControlBleTools.getInstance().getUserProfile(new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
            @Override
            public void onState(SendCmdState state) {
                switch (state) {
                    case SUCCEED:
                        MyApplication.showToast(R.string.s220);
                        break;
                    default:
                        MyApplication.showToast(R.string.s221);
                        break;
                }
            }
        });
    }

    private void initUserInfoData() {
        CallBackUtils.setUserInfoCallBack(new UserInfoCallBack() {
            @Override
            public void onUserInfo(UserInfo userInfo) {
                ToastDialog.showToast(SetActivity.this, "用户信息：" + userInfo.toString());
            }

            @Override
            public void onDayTimeSleep(boolean isDayTime) {
                LogUtils.d("是否白天睡眠：" + isDayTime);
            }

            @Override
            public void onAppWeatherSwitch(boolean isSwitch) {
                LogUtils.d("app天气开关：" + isSwitch);
            }
        });
    }

    private UserInfo testUserInfo() {
        Date date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        try {
            date = simpleDateFormat.parse("2021-10-01");
        } catch (ParseException e) {
            e.printStackTrace();
            date.setTime(0);
        }
        UserInfo bean = new UserInfo();
        bean.userName = "test_user_name";  // 最大支持90字节=30中文字符  Maximum support 90 bytes = 30 Chinese characters
        bean.age = 18;
        bean.height = 170;    //cm
        bean.weight = 60.0f;  //KG
        bean.birthday = (int) date.getTime();
        bean.sex = 2;//1=male，2=female
        bean.maxHr = 80; //最大心率（次/分）  Maximum heart rate (beats/minute)
        bean.calGoal = 180; //卡路里目标 千卡  Calorie goals kcal
        bean.stepGoal = 18000; //步数目标 步  step goal steps
        bean.distanceGoal = 18; // 距离目标 米  Meters from target
        bean.standingTimesGoal = 18;  //有效站立目标 次  Effective standing target times
        //天气开关是否打开  Whether the weather switch is on
        //bean.appWeatherSwitch = Utils.getSharedPreferences(this).getBoolean(SP_WEATHER_SWITCH,false);
        bean.appWeatherSwitch = false;
        return bean;
    }
    //endregion


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (ControlBleTools.getInstance().isConnect()) {

            int id = buttonView.getId();
            if (id == R.id.chbTimeFormat) {
                ControlBleTools.getInstance().setTimeFormat(buttonView.isChecked(), null);
            } else if (id == R.id.chbDistanceUnit) {
                if (buttonView.isChecked()) {
                    ControlBleTools.getInstance().setDistanceUnit(0, null);
                } else {
                    ControlBleTools.getInstance().setDistanceUnit(1, null);
                }
            } else if (id == R.id.chbTemperatureUnit) {
                if (buttonView.isChecked()) {
                    ControlBleTools.getInstance().setTemperatureUnit(0, null);
                } else {
                    ControlBleTools.getInstance().setTemperatureUnit(1, null);
                }
            } else if (id == R.id.chbSleep) {
                ControlBleTools.getInstance().setDaytimeSleep(buttonView.isChecked(), null);
            } else if (id == R.id.cbWeatherSwitch) {
                setWeatherSwitch(buttonView.isChecked());
            }
        }
    }
    //endregion

    //region 生理周期
    CheckBox cbPhSwitch;
    CheckBox cbPhSwitch2;
    EditText etY;
    EditText etM;
    EditText etD;
    EditText etTip;
    EditText etAllDay;
    EditText etDay;

    public void onPhysiologicalCycleClick(View v) {
        int id = v.getId();
        if (id == R.id.btnSetPh) {
            sendPhysiologicalCycle();
        } else if (id == R.id.btnGetPh) {
            ControlBleTools.getInstance().getPhysiologicalCycle(new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
                @Override
                public void onState(SendCmdState state) {
                    switch (state) {
                        case SUCCEED:
                            MyApplication.showToast(R.string.s220);
                            break;
                        default:
                            MyApplication.showToast(R.string.s221);
                            break;
                    }
                }
            });
        }
    }

    private void sendPhysiologicalCycle() {
        PhysiologicalCycleBean physiologicalCycle = new PhysiologicalCycleBean();
        try {
            int y = Integer.parseInt(etY.getText().toString().trim());
            int m = Integer.parseInt(etM.getText().toString().trim());
            int d = Integer.parseInt(etD.getText().toString().trim());
            int tip = Integer.parseInt(etTip.getText().toString().trim());
            int allDay = Integer.parseInt(etAllDay.getText().toString().trim());
            int day = Integer.parseInt(etDay.getText().toString().trim());
            physiologicalCycle.remindSwitch = cbPhSwitch.isChecked();
            physiologicalCycle.advanceDay = tip;
            physiologicalCycle.totalCycleDay = allDay;
            physiologicalCycle.physiologicalCycleDay = day;
            physiologicalCycle.physiologicalStartDate = new PhysiologicalCycleBean.DateBean(y, m, d);
            physiologicalCycle.physiologicalCycleSwitch = cbPhSwitch2.isChecked();
        } catch (Exception e) {
            e.printStackTrace();
            showTips(getString(R.string.s238));
            return;
        }
        ControlBleTools.getInstance().setPhysiologicalCycle(physiologicalCycle, new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
            @Override
            public void onState(SendCmdState state) {
                switch (state) {
                    case SUCCEED:
                        MyApplication.showToast(getString(R.string.s220));
                        break;
                    default:
                        MyApplication.showToast(getString(R.string.s221));
                        break;
                }
            }
        });
    }

    private void initPhysiologicalCycleData() {
        CallBackUtils.physiologicalCycleCallBack = new PhysiologicalCycleCallBack() {
            @Override
            public void onPhysiologicalCycleResult(PhysiologicalCycleBean bean) {
                Log.i(TAG, "physiologicalCycleBean == " + bean);
                ToastDialog.showToast(SetActivity.this, getString(R.string.s106) + "：\n" + bean);
            }

        };
    }
    //endregion

    //region 抬腕亮屏
    EditText etWMode;
    EditText etWSH;
    EditText etWSM;
    EditText etWEH;
    EditText etWEM;
    EditText etWSensitivity;

    public void onWristScreenClick(View v) {
        int id = v.getId();
        if (id == R.id.btnSetW) {
            sendWristScreen();
        } else if (id == R.id.btnGetW) {
            ControlBleTools.getInstance().getWristScreen(new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
                @Override
                public void onState(SendCmdState state) {
                    switch (state) {
                        case SUCCEED:
                            MyApplication.showToast(R.string.s220);
                            break;
                        default:
                            MyApplication.showToast(R.string.s221);
                            break;
                    }
                }
            });
        }
    }

    private void sendWristScreen() {
        WristScreenBean wristScreenBean = new WristScreenBean();
        try {
            int mode = Integer.parseInt(etWMode.getText().toString().trim());
            int sh = Integer.parseInt(etWSH.getText().toString().trim());
            int sm = Integer.parseInt(etWSM.getText().toString().trim());
            int eh = Integer.parseInt(etWEH.getText().toString().trim());
            int em = Integer.parseInt(etWEM.getText().toString().trim());
            int sensitivity = Integer.parseInt(etWSensitivity.getText().toString().trim());
            wristScreenBean.timingMode = mode;
            wristScreenBean.startTime = new SettingTimeBean(sh, sm);
            wristScreenBean.endTime = new SettingTimeBean(eh, em);
            wristScreenBean.sensitivityMode = sensitivity;
        } catch (Exception e) {
            e.printStackTrace();
            showTips(getString(R.string.s238));
            return;
        }
        ControlBleTools.getInstance().setWristScreen(wristScreenBean, new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
            @Override
            public void onState(SendCmdState state) {
                switch (state) {
                    case SUCCEED:
                        MyApplication.showToast(getString(R.string.s220));
                        break;
                    default:
                        MyApplication.showToast(getString(R.string.s221));
                        break;
                }
            }
        });
    }

    private void initWristScreenData() {
        DeviceSettingLiveData.getInstance().getmWristScreen().observe(this, new Observer<WristScreenBean>() {
            @Override
            public void onChanged(WristScreenBean wristScreenBean) {
                if (wristScreenBean == null) return;
                Log.i(TAG, "EventInfoBeans == " + wristScreenBean);
                ToastDialog.showToast(SetActivity.this, getString(R.string.s125) + "：\n" + wristScreenBean);
            }
        });
    }
    //endregion

    //region 省电设置
    CheckBox cbPSwitch;

    public void onPowerSavingCLick(View v) {
        int id = v.getId();
        if (id == R.id.btnSetP) {
            sendPowerSaving();
        } else if (id == R.id.btnGetP) {
            ControlBleTools.getInstance().getPowerSaving(new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
                @Override
                public void onState(SendCmdState state) {
                    switch (state) {
                        case SUCCEED:
                            MyApplication.showToast(R.string.s220);
                            break;
                        default:
                            MyApplication.showToast(R.string.s221);
                            break;
                    }
                }
            });
        }
    }

    private void sendPowerSaving() {
        ControlBleTools.getInstance().setPowerSaving(cbPSwitch.isChecked(), new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
            @Override
            public void onState(SendCmdState state) {
                switch (state) {
                    case SUCCEED:
                        MyApplication.showToast(getString(R.string.s220));
                        break;
                    default:
                        MyApplication.showToast(getString(R.string.s221));
                        break;
                }
            }
        });
    }

    private void initPowerSavingData() {
        DeviceSettingLiveData.getInstance().getmPowerSaving().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isOpen) {
                if (isOpen == null) return;
                Log.i(TAG, "isOpen == " + isOpen);
                ToastDialog.showToast(SetActivity.this, getString(R.string.s130) + "：\n" + isOpen);
            }
        });
    }
    //endregion

    //region 覆盖息屏
    CheckBox cbOSwitch;

    public void onOverlayScreenCLick(View v) {
        int id = v.getId();
        if (id == R.id.btnSetO) {
            sendOverlayScreen();
        } else if (id == R.id.btnGetO) {
            ControlBleTools.getInstance().getOverlayScreen(new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
                @Override
                public void onState(SendCmdState state) {
                    switch (state) {
                        case SUCCEED:
                            MyApplication.showToast(R.string.s220);
                            break;
                        default:
                            MyApplication.showToast(R.string.s221);
                            break;
                    }
                }
            });
        }
    }

    private void sendOverlayScreen() {
        ControlBleTools.getInstance().setOverlayScreen(cbOSwitch.isChecked(), new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
            @Override
            public void onState(SendCmdState state) {
                switch (state) {
                    case SUCCEED:
                        MyApplication.showToast(getString(R.string.s220));
                        break;
                    default:
                        MyApplication.showToast(getString(R.string.s221));
                        break;
                }
            }
        });
    }

    private void initOverlayScreenData() {
        DeviceSettingLiveData.getInstance().getmOverlayScreen().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isOpen) {
                if (isOpen == null) return;
                cbOSwitch.setChecked(isOpen);
                Log.i(TAG, "isOpen == " + isOpen);
                ToastDialog.showToast(SetActivity.this, getString(R.string.s133) + "：\n" + isOpen);
            }
        });
    }
    //endregion

    //region 快速眼动
    CheckBox cbRSwitch;

    public void onRapidEyeMovementCLick(View v) {
        int id = v.getId();
        if (id == R.id.btnSetR) {
            sendRapidEyeMovement();
        } else if (id == R.id.btnGetR) {
            ControlBleTools.getInstance().getRapidEyeMovement(new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
                @Override
                public void onState(SendCmdState state) {
                    switch (state) {
                        case SUCCEED:
                            MyApplication.showToast(R.string.s220);
                            break;
                        default:
                            MyApplication.showToast(R.string.s221);
                            break;
                    }
                }
            });
        }
    }

    private void sendRapidEyeMovement() {
        ControlBleTools.getInstance().setRapidEyeMovement(cbRSwitch.isChecked(), new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
            @Override
            public void onState(SendCmdState state) {
                switch (state) {
                    case SUCCEED:
                        MyApplication.showToast(getString(R.string.s220));
                        break;
                    default:
                        MyApplication.showToast(getString(R.string.s221));
                        break;
                }
            }
        });
    }

    private void initRapidEyeMovementData() {
        DeviceSettingLiveData.getInstance().getmRapidEyeMovement().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isOpen) {
                if (isOpen == null) return;
                cbRSwitch.setChecked(isOpen);
                Log.i(TAG, "isOpen == " + isOpen);
                ToastDialog.showToast(SetActivity.this, getString(R.string.s136) + "：\n" + isOpen);
            }
        });
    }
    //endregion

    //region 震动设置
    EditText etVibration;

    public void onVibrationCLick(View v) {
        int id = v.getId();
        if (id == R.id.btnSetV) {
            sendVibration();
        } else if (id == R.id.btnGetV) {
            ControlBleTools.getInstance().getDeviceVibrationIntensity(new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
                @Override
                public void onState(SendCmdState state) {
                    switch (state) {
                        case SUCCEED:
                            MyApplication.showToast(R.string.s220);
                            break;
                        default:
                            MyApplication.showToast(R.string.s221);
                            break;
                    }
                }
            });
        }
    }

    private void sendVibration() {
        try {
            int intensity = Integer.parseInt(etVibration.getText().toString().trim());
            ControlBleTools.getInstance().setDeviceVibrationIntensity(intensity, new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
                @Override
                public void onState(SendCmdState state) {
                    switch (state) {
                        case SUCCEED:
                            MyApplication.showToast(getString(R.string.s220));
                            break;
                        default:
                            MyApplication.showToast(getString(R.string.s221));
                            break;
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            showTips(getString(R.string.s238));
            return;
        }
    }

    private void initVibrationData() {
        DeviceSettingLiveData.getInstance().getmVibrationMode().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer intensity) {
                if (intensity == null) return;
                Log.i(TAG, "isOpen == " + intensity);
                ToastDialog.showToast(SetActivity.this, getString(R.string.s142) + "：\n" + intensity);
            }
        });
    }
    //endregion

    //region 息屏显示
    EditText etDMode;
    EditText etDSH;
    EditText etDSM;
    EditText etDEH;
    EditText etDEM;
    EditText etDStyle;

    public void onScreenDisplayCLick(View v) {
        int id = v.getId();
        if (id == R.id.btnSetD) {
            sendScreenDisplay();
        } else if (id == R.id.btnGetD) {
            ControlBleTools.getInstance().getScreenDisplay(new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
                @Override
                public void onState(SendCmdState state) {
                    switch (state) {
                        case SUCCEED:
                            MyApplication.showToast(R.string.s220);
                            break;
                        default:
                            MyApplication.showToast(R.string.s221);
                            break;
                    }
                }
            });
        }
    }

    private void sendScreenDisplay() {
        ScreenDisplayBean displayBean = new ScreenDisplayBean();
        try {
            int mode = Integer.parseInt(etDMode.getText().toString().trim());
            int sh = Integer.parseInt(etDSH.getText().toString().trim());
            int sm = Integer.parseInt(etDSM.getText().toString().trim());
            int eh = Integer.parseInt(etDEH.getText().toString().trim());
            int em = Integer.parseInt(etDEM.getText().toString().trim());
            int style = Integer.parseInt(etDStyle.getText().toString().trim());
            displayBean.timingMode = mode;
            displayBean.startTime = new SettingTimeBean(sh, sm);
            displayBean.endTime = new SettingTimeBean(eh, em);
            displayBean.dialStyle = new DialStyleBean(style);
        } catch (Exception e) {
            e.printStackTrace();
            showTips(getString(R.string.s238));
            return;
        }
        ControlBleTools.getInstance().setScreenDisplay(displayBean, new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
            @Override
            public void onState(SendCmdState state) {
                switch (state) {
                    case SUCCEED:
                        MyApplication.showToast(getString(R.string.s220));
                        break;
                    default:
                        MyApplication.showToast(getString(R.string.s221));
                        break;
                }
            }
        });
    }

    private void initScreenDisplayData() {
        DeviceSettingLiveData.getInstance().getmScreenDisplay().observe(this, new Observer<ScreenDisplayBean>() {
            @Override
            public void onChanged(ScreenDisplayBean screenDisplayBean) {
                if (screenDisplayBean == null) return;
                Log.i(TAG, "screenDisplayBean == " + screenDisplayBean);
                ToastDialog.showToast(SetActivity.this, getString(R.string.s143) + "：\n" + screenDisplayBean);
            }
        });
    }
    //endregion

    //region 亮屏设置


    CheckBox cbSSwitch1;
    CheckBox cbSSwitch2;
    EditText etLevel;
    EditText etDuration;

    public void onScreenSettingClick(View v) {
        int id = v.getId();
        if (id == R.id.btnSetS) {
            sendScreenSetting();
        } else if (id == R.id.btnGetS) {
            ControlBleTools.getInstance().getScreenSetting(new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
                @Override
                public void onState(SendCmdState state) {
                    switch (state) {
                        case SUCCEED:
                            MyApplication.showToast(R.string.s220);
                            break;
                        default:
                            MyApplication.showToast(R.string.s221);
                            break;
                    }
                }
            });
        }
    }

    private void sendScreenSetting() {
        ScreenSettingBean screenSetting = new ScreenSettingBean();
        try {
            int level = Integer.parseInt(etLevel.getText().toString().trim());
            int duration = Integer.parseInt(etDuration.getText().toString().trim());
            screenSetting.level = level;
            screenSetting.duration = duration;
            screenSetting.isSwitch = cbSSwitch1.isChecked();
            screenSetting.doubleClick = cbSSwitch2.isChecked();
        } catch (Exception e) {
            e.printStackTrace();
            showTips(getString(R.string.s238));
            return;
        }
        ControlBleTools.getInstance().setScreenSetting(screenSetting, new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
            @Override
            public void onState(SendCmdState state) {
                switch (state) {
                    case SUCCEED:
                        MyApplication.showToast(getString(R.string.s220));
                        break;
                    default:
                        MyApplication.showToast(getString(R.string.s221));
                        break;
                }
            }
        });
    }

    private void initScreenSettingData() {
        DeviceSettingLiveData.getInstance().getmScreenSetting().observe(this, new Observer<ScreenSettingBean>() {
            @Override
            public void onChanged(ScreenSettingBean screenSettingBean) {
                if (screenSettingBean == null) return;
                Log.i(TAG, "screenDisplayBean == " + screenSettingBean);
                ToastDialog.showToast(SetActivity.this, getString(R.string.s150) + "：\n" + screenSettingBean);
            }
        });
    }
    //endregion

    //region 勿扰模式

    CheckBox cbNSwitch1;
    CheckBox cbNSwitch2;
    EditText etNSH;
    EditText etNSM;
    EditText etNEH;
    EditText etNEM;

    public void onDoNotDisturbModeClick(View v) {
        int id = v.getId();
        if (id == R.id.btnSetN) {
            sendDoNotDisturbMode();
        } else if (id == R.id.btnGetN) {
            ControlBleTools.getInstance().getDoNotDisturbMode(new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
                @Override
                public void onState(SendCmdState state) {
                    switch (state) {
                        case SUCCEED:
                            MyApplication.showToast(R.string.s220);
                            break;
                        default:
                            MyApplication.showToast(R.string.s221);
                            break;
                    }
                }
            });
        }
    }

    private void sendDoNotDisturbMode() {
        DoNotDisturbModeBean doNotDisturbMode = new DoNotDisturbModeBean();
        try {
            int sh = Integer.parseInt(etNSH.getText().toString().trim());
            int sm = Integer.parseInt(etNSM.getText().toString().trim());
            int eh = Integer.parseInt(etNEH.getText().toString().trim());
            int em = Integer.parseInt(etNEM.getText().toString().trim());
            doNotDisturbMode.isSwitch = cbNSwitch1.isChecked();
            doNotDisturbMode.isSmartSwitch = cbNSwitch2.isChecked();
            doNotDisturbMode.startTime = new SettingTimeBean(sh, sm);
            doNotDisturbMode.endTime = new SettingTimeBean(eh, em);
        } catch (Exception e) {
            e.printStackTrace();
            showTips(getString(R.string.s238));
            return;
        }
        ControlBleTools.getInstance().setDoNotDisturbMode(doNotDisturbMode, new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
            @Override
            public void onState(SendCmdState state) {
                switch (state) {
                    case SUCCEED:
                        MyApplication.showToast(getString(R.string.s220));
                        break;
                    default:
                        MyApplication.showToast(getString(R.string.s221));
                        break;
                }
            }
        });
    }

    private void initDoNotDisturbModeData() {
        DeviceSettingLiveData.getInstance().getmDoNotDisturbMode().observe(this, new Observer<DoNotDisturbModeBean>() {
            @Override
            public void onChanged(DoNotDisturbModeBean doNotDisturbModeBean) {
                if (doNotDisturbModeBean == null) return;
                Log.i(TAG, "doNotDisturbModeBean == " + doNotDisturbModeBean);
                ToastDialog.showToast(SetActivity.this, getString(R.string.s154) + "：\n" + doNotDisturbModeBean);
            }
        });
    }
    //endregion

    //region 天气
    public static final String SP_WEATHER_SWITCH = "Weather_Switch";

    /**
     * 设置天气开关状态
     *
     * @param checked
     */
    private void setWeatherSwitch(boolean checked) {
        Utils.getSharedPreferences(this).edit().putBoolean(SP_WEATHER_SWITCH, checked).apply();
        //setUserProfile(null);
        ControlBleTools.getInstance().setAppWeatherSwitch(checked, new ParsingStateManager.SendCmdStateListener() {
            @Override
            public void onState(@NonNull SendCmdState state) {
                switch (state) {
                    case SUCCEED:
                        MyApplication.showToast(R.string.s220);
                        break;
                    default:
                        MyApplication.showToast(R.string.s221);
                        break;
                }
            }
        });
        //天气开关是否打开  Whether the weather switch is on
        //bean.appWeatherSwitch = Utils.getSharedPreferences(this).getBoolean(SP_WEATHER_SWITCH,false);
    }

    public void setDayWeather(View view) {
        if (!ControlBleTools.getInstance().isConnect()) return;
        WeatherDayBean bean = new WeatherDayBean();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        bean.year = cal.get(Calendar.YEAR);
        bean.month = cal.get(Calendar.MONTH) + 1;
        bean.day = cal.get(Calendar.DAY_OF_MONTH);
        bean.hour = cal.get(Calendar.HOUR_OF_DAY);
        bean.minute = cal.get(Calendar.MINUTE);
        bean.second = cal.get(Calendar.SECOND);
        bean.cityName = "ShenZhen";
        bean.locationName = "CN";
        for (int i = 0; i < 4; i++) {
            WeatherDayBean.Data listBean = new WeatherDayBean.Data();
            listBean.aqi = 80 + i;
            listBean.now_temperature = 30 + i;
            listBean.low_temperature = 20 + i;
            listBean.high_temperature = 30 + i;
            listBean.humidity = 70 + i;
            listBean.weather_id = 804;
            listBean.weather_name = "Clouds";
            listBean.Wind_speed = 2 + i;
            listBean.wind_info = 252 + i;
            listBean.Probability_of_rainfall = 4 + i;
            listBean.sun_rise = (1568958164 + i) + "";
            listBean.sun_set = (1569002733 + i) + "";
            listBean.wind_power = 10;
            listBean.visibility = 10;
            bean.list.add(listBean);
        }
        LogUtils.d("sendWeatherDailyForecast:" + GsonUtils.toJson(bean));
        ControlBleTools.getInstance().sendWeatherDailyForecast(bean, null);
        set4DayHourWeather();
    }

    //最大发送4*24，demo中只发送1*24
    private void set4DayHourWeather() {
        WeatherPerHourBean bean = new WeatherPerHourBean();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        bean.year = cal.get(Calendar.YEAR);
        bean.month = cal.get(Calendar.MONTH) + 1;
        bean.day = cal.get(Calendar.DAY_OF_MONTH);
        bean.hour = cal.get(Calendar.HOUR_OF_DAY);
        bean.minute = cal.get(Calendar.MINUTE);
        bean.second = cal.get(Calendar.SECOND);
        bean.cityName = "ShenZhen";
        bean.locationName = "CN";
        for (int i = 0; i < 4 * 24; i++) {
            WeatherPerHourBean.Data listBean = new WeatherPerHourBean.Data();
            listBean.now_temperature = 10 + i;
            listBean.humidity = 60 + i;
            listBean.weather_id = 804;
            listBean.Wind_speed = 2 + i;
            listBean.wind_info = 152 + i;
            listBean.Probability_of_rainfall = 4 + i;
            listBean.wind_power = 10;
            listBean.visibility = 10;
            bean.list.add(listBean);
        }
        LogUtils.d("sendWeatherPreHour:" + GsonUtils.toJson(bean));
        ControlBleTools.getInstance().sendWeatherPreHour(bean, new ParsingStateManager.SendCmdStateListener() {
            @Override
            public void onState(SendCmdState state) {
                switch (state) {
                    case SUCCEED: {
                        showTips(getString(R.string.s241) + "SUCCEED");
                    }
                    break;
                    case PARAM_ERROR:
                    case UNKNOWN:
                    case FAILED:
                    default:
                        showTips(getString(R.string.s221));
                        break;
                }
            }
        });
    }
    //endregion

    //region 股票

    Button btnSyncStock;

    List<StockInfoBean> stockInfos = new ArrayList<>();

    public void onStockClick(View v) {
        int id = v.getId();
        if (id == R.id.btnSyncStock) {
            ControlBleTools.getInstance().syncStockInfoList(stockInfos, new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
                @Override
                public void onState(@NonNull SendCmdState state) {
                    switch (state) {
                        case SUCCEED:
                            MyApplication.showToast(R.string.s220);
                            break;
                        default:
                            MyApplication.showToast(R.string.s221);
                            break;
                    }
                }
            });
        } else if (id == R.id.btnSetStockOrder) {
            List<StockSymbolBean> stockSymbolBeans = new ArrayList<>();
            for (int i = 0; i < stockInfos.size(); i++) {
                StockSymbolBean symbolBean = new StockSymbolBean();
                symbolBean.symbol = stockInfos.get(i).symbol;
                symbolBean.isWidget = false;
                symbolBean.order = i;
                stockSymbolBeans.add(symbolBean);
            }
            ControlBleTools.getInstance().setStockSymbolOrder(stockSymbolBeans, new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
                @Override
                public void onState(@NonNull SendCmdState state) {
                    switch (state) {
                        case SUCCEED:
                            MyApplication.showToast(R.string.s220);
                            break;
                        default:
                            MyApplication.showToast(R.string.s221);
                            break;
                    }
                }
            });
        } else if (id == R.id.btnGetStockOrder) {
            ControlBleTools.getInstance().getStockSymbolList(new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
                @Override
                public void onState(@NonNull SendCmdState state) {
                    switch (state) {
                        case SUCCEED:
                            MyApplication.showToast(R.string.s220);
                            break;
                        default:
                            MyApplication.showToast(R.string.s221);
                            break;
                    }
                }
            });
        } else if (id == R.id.btnDelStock) {
            ControlBleTools.getInstance().deleteStockBySymbol(stockInfos.get(0).symbol, new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
                @Override
                public void onState(@NonNull SendCmdState state) {
                    switch (state) {
                        case SUCCEED:
                            MyApplication.showToast(R.string.s220);
                            break;
                        default:
                            MyApplication.showToast(R.string.s221);
                            break;
                    }
                }
            });
        }
    }


    private void initStockData() {
        //region TODO 测试股票数据
        for (int i = 0; i < 10; i++) {
            /**
             *symbol;        //股票代码
             *market;        //股票市场编号
             *name;          //股票名称
             *latestPrice;   //最新价格
             *preClose;      //收盘价格
             */
            StockInfoBean stockInfoBean = new StockInfoBean("symbol_" + i, "market_" + i, "name" + i, 110.0f, 100.0f, 11, (int) (System.currentTimeMillis() / 1000), 22);
            stockInfos.add(stockInfoBean);
        }
        //endregion

        CallBackUtils.setStockCallBack(new MyStockCallBack(this));
    }

    /**
     * 股票相关回调
     */
    class MyStockCallBack implements StockCallBack {

        private WeakReference<SetActivity> weakReference;

        public MyStockCallBack(SetActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        @Override
        public void onStockInfoList(List<StockSymbolBean> list) {
            if (weakReference.get() != null) {
                ToastDialog.showToast(weakReference.get(), "" + GsonUtils.toJson(list));
            }
        }

        @Override
        public void onWearRequestStock() {
            //设备请求同步股票信息 调用ControlBleTools.getInstance().syncStockInfoList
            if (weakReference.get() != null) {
                weakReference.get().btnSyncStock.callOnClick();
            }
        }
    }
    //endregion

    //region 联系人 , 紧急联系人
    EditText etSosContactName;

    EditText etSosContactPh;

    //
    public void onContactsClick(View v) {
        int id = v.getId();
        if (id == R.id.btnSetContacts) {
            List<ContactBean> list = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                ContactBean contactBean = new ContactBean();
                contactBean.contacts_name = "name_" + i;
                contactBean.contacts_number = "1234567890" + i;
                list.add(contactBean);
            }
            ControlBleTools.getInstance().setContactList(list, new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
                @Override
                public void onState(@NonNull SendCmdState state) {
                    switch (state) {
                        case SUCCEED:
                            MyApplication.showToast(R.string.s220);
                            break;
                        default:
                            MyApplication.showToast(R.string.s221);
                            break;
                    }
                }
            });
        } else if (id == R.id.btnGetContacts) {
            ControlBleTools.getInstance().getContactList(new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
                @Override
                public void onState(@NonNull SendCmdState state) {
                    switch (state) {
                        case SUCCEED:
                            MyApplication.showToast(R.string.s220);
                            break;
                        default:
                            MyApplication.showToast(R.string.s221);
                            break;
                    }
                }
            });
        } else if (id == R.id.btnSetSosContacts) {
            EmergencyContactBean bean = new EmergencyContactBean();
            List<ContactBean> sosList = new ArrayList<>();
            ContactBean contactBean = new ContactBean();
            String name = "name";
            String phone = "12345678900";
            try {
                if (!etSosContactName.getText().toString().trim().isEmpty()) {
                    name = etSosContactName.getText().toString().trim();
                }
                if (!etSosContactPh.getText().toString().trim().isEmpty()) {
                    phone = etSosContactPh.getText().toString().trim();
                }
            } catch (Exception e) {
                e.printStackTrace();
                showTips(getString(R.string.s238));
                return;
            }
            contactBean.contacts_name = name;
            contactBean.contacts_number = phone;
            contactBean.contacts_sequence = 1;
            sosList.add(contactBean);
            bean.contactList = sosList;
            bean.sosSwitch = true;
            bean.max = 1;
            ControlBleTools.getInstance().setEmergencyContacts(bean, new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
                @Override
                public void onState(@NonNull SendCmdState state) {
                    switch (state) {
                        case SUCCEED:
                            MyApplication.showToast(R.string.s220);
                            break;
                        default:
                            MyApplication.showToast(R.string.s221);
                            break;
                    }
                }
            });
        } else if (id == R.id.btnGetSosContacts) {
            ControlBleTools.getInstance().getEmergencyContacts(new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
                @Override
                public void onState(@NonNull SendCmdState state) {
                    switch (state) {
                        case SUCCEED:
                            MyApplication.showToast(R.string.s220);
                            break;
                        default:
                            MyApplication.showToast(R.string.s221);
                            break;
                    }
                }
            });
        }
    }

    private void initContactsCallBack() {
        CallBackUtils.contactCallBack = new ContactCallBack() {
            @Override
            public void onContactResult(@NonNull ArrayList<ContactBean> data) {
                ToastDialog.showToast(SetActivity.this, "" + GsonUtils.toJson(data));
            }
        };

        CallBackUtils.setEmergencyContactsCallBack(new EmergencyContactsCallBack() {
            @Override
            public void onEmergencyContacts(EmergencyContactBean bean) {
                LogUtils.d("onEmergencyContacts:" + bean);
                ToastDialog.showToast(SetActivity.this, "" + GsonUtils.toJson(bean));
            }
        });
    }
    //endregion

    //region 通话蓝牙开关
    CheckBox cbBtSwitch;
    CheckBox cbBtRemind;

    public void onBtSwitchClick(View v) {
        int id = v.getId();
        if (id == R.id.btnSetBtSwitch) {
            ClassicBluetoothStateBean bean = new ClassicBluetoothStateBean(cbBtSwitch.isChecked(), cbBtRemind.isChecked());
            ControlBleTools.getInstance().setClassicBluetoothState(bean, new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
                @Override
                public void onState(@NonNull SendCmdState state) {
                    switch (state) {
                        case SUCCEED:
                            MyApplication.showToast(R.string.s220);
                            break;
                        default:
                            MyApplication.showToast(R.string.s221);
                            break;
                    }
                }
            });
        } else if (id == R.id.btnGetBtSwitch) {
            ControlBleTools.getInstance().getClassicBluetoothState(new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
                @Override
                public void onState(@NonNull SendCmdState state) {
                    switch (state) {
                        case SUCCEED:
                            MyApplication.showToast(R.string.s220);
                            break;
                        default:
                            MyApplication.showToast(R.string.s221);
                            break;
                    }
                }
            });
        }
    }

    private void initClassicBluetoothStateCallBack() {
        DeviceSettingLiveData.getInstance().getmClassicBluetoothStateBean().observe(this, new Observer<ClassicBluetoothStateBean>() {
            @Override
            public void onChanged(ClassicBluetoothStateBean classicBluetoothStateBean) {
                if (classicBluetoothStateBean == null) return;
                Log.i(TAG, "classicBluetoothStateBean == " + classicBluetoothStateBean);
                ToastDialog.showToast(SetActivity.this, getString(R.string.s357) + "：\n" + classicBluetoothStateBean);
            }
        });
    }
    //endregion

    //region 世界时钟
    public void onWorldClockClick(View v) {
        int id = v.getId();
        if (id == R.id.btnSetWorldClock) {
            List<WorldClockBean> list = new ArrayList<>();


            //本地获取
            /**
             * @see #toTimezoneInt
             */
            String[] stringArray = getResources().getStringArray(R.array.world_clock_zone_name);
            for (int i = 0; i < 5; i++) { // max = 5  最大值由设备或者产品决定 The maximum value is determined by the device or product
                String arr = stringArray[i];
                String[] split = arr.split("\\*");
                WorldClockBean bean = new WorldClockBean();
                bean.cityName = split[1];
                bean.offset = toTimezoneInt(split[0]) / 15;   //Time zone minutes divided by 15
                list.add(bean);
            }

            //当前时钟 ---------
            WorldClockBean worldClockBean = new WorldClockBean();
            worldClockBean.cityName = "Beijing";       //城市名
            worldClockBean.offset = TimeZone.getDefault().getRawOffset() / 60 / 1000 / 15;    //Time zone minutes divided by 15
            LogUtils.d("offset : " + worldClockBean.offset);
            list.add(worldClockBean);

            ControlBleTools.getInstance().setWorldClockList(list, new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
                @Override
                public void onState(@NonNull SendCmdState state) {
                    switch (state) {
                        case SUCCEED:
                            MyApplication.showToast(R.string.s220);
                            break;
                        default:
                            MyApplication.showToast(R.string.s221);
                            break;
                    }
                }
            });
        } else if (id == R.id.btnGetWorldClock) {
            ControlBleTools.getInstance().getWorldClockList(new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
                @Override
                public void onState(@NonNull SendCmdState state) {
                    switch (state) {
                        case SUCCEED:
                            MyApplication.showToast(R.string.s220);
                            break;
                        default:
                            MyApplication.showToast(R.string.s221);
                            break;
                    }
                }
            });
        }
    }

    private void initWorldClockData() {
        DeviceSettingLiveData.getInstance().getWorldClockList().observe(this, new Observer<List<WorldClockBean>>() {
            @Override
            public void onChanged(List<WorldClockBean> worldClockBeans) {
                if (worldClockBeans == null) return;
                Log.i(TAG, "worldClockBeans == " + worldClockBeans);
                ToastDialog.showToast(SetActivity.this, getString(R.string.s303) + "：\n" + worldClockBeans);
            }
        });
    }

    private int toTimezoneInt(String s) {
        int value = 0;
        if (s.contains("+")) {
            String replace = s.replace("+", "");
            Date date = str2Date(replace, "HH:mm");
            value = date.getHours() * 60 + date.getMinutes();
        } else if (s.contains("-")) {
            String replace = s.replace("-", "");
            Date date = str2Date(replace, "HH:mm");
            value = -(date.getHours() * 60 + date.getMinutes());
        }
        return value;
    }

    public Date str2Date(String time, String pattern) {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat mFormatter = new SimpleDateFormat(pattern);
        try {
            return mFormatter.parse(time);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
    //endregion

    //region 心率预警

    CheckBox cbHrModeSwitch;
    CheckBox cbHrIsWarningSwitch;
    CheckBox cbHeartRateMode;
    EditText etHeartRate;
    CheckBox cbHrIsSport;
    EditText etHeartSport;

    public void onHeartRateClick(View v) {
        int id = v.getId();
        if (id == R.id.btnGetHr) {
            ControlBleTools.getInstance().getHeartRateMonitor(new ParsingStateManager.SendCmdStateListener() {
                @Override
                public void onState(@NonNull SendCmdState state) {
                    switch (state) {
                        case SUCCEED:
                            MyApplication.showToast(R.string.s220);
                            break;
                        default:
                            MyApplication.showToast(R.string.s221);
                            break;
                    }
                }
            });
        } else if (id == R.id.btnSetHr) {
            sendHeartRate();
        }
    }

    private void sendHeartRate() {
        HeartRateMonitorBean bean = new HeartRateMonitorBean();
        try {
            int warningValue = Integer.parseInt(etHeartRate.getText().toString().trim());
            int sportWarningValue = Integer.parseInt(etHeartSport.getText().toString().trim());
            bean.mode = cbHrModeSwitch.isChecked() ? 0 : 1;
            bean.isWarning = cbHrIsWarningSwitch.isChecked();
            bean.warningValue = warningValue;
            bean.isSportWarning = cbHrIsSport.isChecked();
            bean.sportWarningValue = sportWarningValue;
            bean.frequency = 0; //设备内部写死5
            bean.continuousHeartRateMode = cbHeartRateMode.isChecked() ? 1 : 0;
        } catch (Exception e) {
            e.printStackTrace();
            showTips(getString(R.string.s238));
            return;
        }
        ControlBleTools.getInstance().setHeartRateMonitor(bean, new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
            @Override
            public void onState(SendCmdState state) {
                switch (state) {
                    case SUCCEED:
                        MyApplication.showToast(getString(R.string.s220));
                        break;
                    default:
                        MyApplication.showToast(getString(R.string.s221));
                        break;
                }
            }
        });
    }

    private void initHeartRateMonitorData() {
        DeviceSettingLiveData.getInstance().getmHeartRateMonitor().observe(this, new Observer<HeartRateMonitorBean>() {
            @Override
            public void onChanged(HeartRateMonitorBean bean) {
                if (bean == null) return;
                Log.i(TAG, "HeartRateMonitorBean == " + bean);
                ToastDialog.showToast(SetActivity.this, getString(R.string.s306) + "：\n" + bean);
            }
        });
    }
    //endregion

    //region 学校模式
    public void onSchooleMode(View v) {
        int id = v.getId();
        if (id == R.id.btnGetSchoolMode) {
            ControlBleTools.getInstance().getSchoolMode(new ParsingStateManager.SendCmdStateListener() {
                @Override
                public void onState(@NonNull SendCmdState state) {
                    switch (state) {
                        case SUCCEED:
                            MyApplication.showToast(R.string.s220);
                            break;
                        default:
                            MyApplication.showToast(R.string.s221);
                            break;
                    }
                }
            });
        } else if (id == R.id.btnSetSchoolMode) {
            sendSchoolMode();
        }
    }

    private int testSchoolModeTimeValue = 0;

    private void sendSchoolMode() {
        testSchoolModeTimeValue++;
        if (testSchoolModeTimeValue + 10 > 24) {
            testSchoolModeTimeValue = 0;
        }
        SchoolBean schoolBean = new SchoolBean(true, new SettingTimeBean(testSchoolModeTimeValue, 0), new SettingTimeBean(testSchoolModeTimeValue + 10, 30), true, true, true, true, true, true, true, testSchoolModeTimeValue, true, true);
        ControlBleTools.getInstance().setSchoolMode(schoolBean, new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
            @Override
            public void onState(SendCmdState state) {
                switch (state) {
                    case SUCCEED:
                        MyApplication.showToast(getString(R.string.s220));
                        break;
                    default:
                        MyApplication.showToast(getString(R.string.s221));
                        break;
                }
            }
        });
    }

    private void initSchoolModeData() {
        DeviceSettingLiveData.getInstance().getmSchoolBean().observe(this, new Observer<SchoolBean>() {
            @Override
            public void onChanged(SchoolBean bean) {
                if (bean == null) return;
                Log.i(TAG, "getmSchoolBean == " + bean);
                ToastDialog.showToast(SetActivity.this, getString(R.string.s320) + "：\n" + bean);
            }
        });
    }
    //endregion

    //region 调度器
    public void onScheduler(View v) {
        int id = v.getId();
        if (id == R.id.btnGetScheduler) {
            ControlBleTools.getInstance().getScheduleReminder(new ParsingStateManager.SendCmdStateListener() {
                @Override
                public void onState(@NonNull SendCmdState state) {
                    switch (state) {
                        case SUCCEED:
                            MyApplication.showToast(R.string.s220);
                            break;
                        default:
                            MyApplication.showToast(R.string.s221);
                            break;
                    }
                }
            });
        } else if (id == R.id.btnSetScheduler) {
            sendScheduler();
        }
    }

    private int testSchedulerValue = 0;

    private void sendScheduler() {
        if (DeviceSettingLiveData.getInstance().getmScheduler().getValue() == null) {
            Toast.makeText(SetActivity.this, getString(R.string.s219), Toast.LENGTH_LONG).show();
            return;
        }
        testSchedulerValue++;
        if (testSchedulerValue + 10 > 24) {
            testSchedulerValue = 0;
        }
        SchedulerBean schedulerBean = DeviceSettingLiveData.getInstance().getmScheduler().getValue();
        if (schedulerBean.alertList != null && schedulerBean.alertList.size() > 0) {
            schedulerBean.alertList.get(0).alertName = "alertName" + testSchedulerValue;
        } else {
            schedulerBean.alertList = new ArrayList<>();
            schedulerBean.alertList.add(new SchedulerBean.AlertBean("alertName" + testSchedulerValue, "alertName", new SettingTimeBean(testSchedulerValue, 0), false, false, false, false, false, false, false));
        }
        if (schedulerBean.habitBeanList != null && schedulerBean.habitBeanList.size() > 0) {
            schedulerBean.habitBeanList.get(0).habitName = "habitName" + testSchedulerValue;
        } else {
            schedulerBean.habitBeanList = new ArrayList<>();
            ArrayList<SettingTimeBean> test = new ArrayList<>();
            test.add(new SettingTimeBean(testSchedulerValue, 0));
            schedulerBean.habitBeanList.add(new SchedulerBean.HabitBean(0, "habitName" + testSchedulerValue, test, false, false, false, false, false, false, false));
        }
        if (schedulerBean.reminderBeanList != null && schedulerBean.reminderBeanList.size() > 0) {
            schedulerBean.reminderBeanList.get(0).reminderName = "reminderName" + testSchedulerValue;
        } else {
            schedulerBean.reminderBeanList = new ArrayList<>();
            schedulerBean.reminderBeanList.add(new SchedulerBean.ReminderBean(0, "reminderName" + testSchedulerValue, new SettingTimeBean(testSchedulerValue, 0), new SettingTimeBean(testSchedulerValue + 10, 0), testSchedulerValue, false, false, false, false, false, false, false));
        }
        ControlBleTools.getInstance().setScheduleReminder(schedulerBean, new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
            @Override
            public void onState(SendCmdState state) {
                switch (state) {
                    case SUCCEED:
                        MyApplication.showToast(getString(R.string.s220));
                        break;
                    default:
                        MyApplication.showToast(getString(R.string.s221));
                        break;
                }
            }
        });
    }

    private void initGetSchedulerData() {
        DeviceSettingLiveData.getInstance().getmScheduler().observe(this, new Observer<SchedulerBean>() {
            @Override
            public void onChanged(SchedulerBean bean) {
                if (bean == null) return;
                Log.i(TAG, "getmScheduler == " + bean);
                ToastDialog.showToast(SetActivity.this, getString(R.string.s323) + "：\n" + bean);
            }
        });
    }
    //endregion

    //region 压力模式
    CheckBox cbPrModeSwitch;
    CheckBox cbPrReminderSwitch;

    public void onPressureClick(View v) {
        int id = v.getId();
        if (id == R.id.btnGetPressure) {
            ControlBleTools.getInstance().getPressureMode(new ParsingStateManager.SendCmdStateListener() {
                @Override
                public void onState(@NonNull SendCmdState state) {
                    switch (state) {
                        case SUCCEED:
                            MyApplication.showToast(R.string.s220);
                            break;
                        default:
                            MyApplication.showToast(R.string.s221);
                            break;
                    }
                }
            });
        } else if (id == R.id.btnSetPressure) {
            setPressureMode();
        }
    }

    private void initPressureModeData() {
        DeviceSettingLiveData.getInstance().getmPressureMode().observe(this, new Observer<PressureModeBean>() {
            @Override
            public void onChanged(PressureModeBean pressureModeBean) {
                if (pressureModeBean != null) {
                    Log.i(TAG, "PressureModeBean == " + pressureModeBean);
                    ToastDialog.showToast(SetActivity.this, getString(R.string.s329) + "：\n" + pressureModeBean);
                    cbPrModeSwitch.setChecked(pressureModeBean.pressureMode);
                    cbPrReminderSwitch.setChecked(pressureModeBean.relaxationReminder);
                }
            }
        });
    }

    private void setPressureMode() {
        PressureModeBean bean = new PressureModeBean();
        try {
            bean.pressureMode = cbPrModeSwitch.isChecked();
            bean.relaxationReminder = cbPrReminderSwitch.isChecked();
        } catch (Exception e) {
            e.printStackTrace();
            showTips(getString(R.string.s238));
            return;
        }
        ControlBleTools.getInstance().setPressureMode(bean, new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
            @Override
            public void onState(SendCmdState state) {
                switch (state) {
                    case SUCCEED:
                        MyApplication.showToast(getString(R.string.s220));
                        break;
                    default:
                        MyApplication.showToast(getString(R.string.s221));
                        break;
                }
            }
        });
    }

    //endregion

    //region 通知设置
    CheckBox cbNoticeNotLightUp;
    CheckBox cbNoticeDelay;
    EditText etPhoneRemindVibrationMode;
    EditText etPhoneRemindRingMode;


    public void onNoticeSettingsClick(View v) {
        int id = v.getId();
        if (id == R.id.btnGetNoticeSettings) {
            ControlBleTools.getInstance().getNotificationSettings(new ParsingStateManager.SendCmdStateListener() {
                @Override
                public void onState(@NonNull SendCmdState state) {
                    switch (state) {
                        case SUCCEED:
                            MyApplication.showToast(R.string.s220);
                            break;
                        default:
                            MyApplication.showToast(R.string.s221);
                            break;
                    }
                }
            });
        } else if (id == R.id.btnSetNoticeSettings) {
            setNoticeSettings();
        }
    }

    private void initNoticeSettings() {
        DeviceSettingLiveData.getInstance().getmNotificationSettings().observe(this, new Observer<NotificationSettingsBean>() {
            @Override
            public void onChanged(NotificationSettingsBean settingsBean) {
                if (settingsBean != null) {
                    Log.i(TAG, "NotificationSettingsBean == " + settingsBean);
                    ToastDialog.showToast(SetActivity.this, getString(R.string.s334) + "：\n" + settingsBean);
                    cbNoticeNotLightUp.setChecked(settingsBean.noticeNotLightUp);
                    cbNoticeDelay.setChecked(settingsBean.delayReminderSwitch);
                    etPhoneRemindVibrationMode.setText("" + settingsBean.phoneRemindVibrationMode);
                    etPhoneRemindRingMode.setText("" + settingsBean.phoneRemindRingMode);
                }
            }
        });
    }

    private void setNoticeSettings() {
        NotificationSettingsBean bean = new NotificationSettingsBean();
        try {
            bean.noticeNotLightUp = cbNoticeNotLightUp.isChecked();
            bean.delayReminderSwitch = cbNoticeDelay.isChecked();
            int vMode = Integer.parseInt(etPhoneRemindVibrationMode.getText().toString().trim());
            int rMode = Integer.parseInt(etPhoneRemindRingMode.getText().toString().trim());
            bean.phoneRemindVibrationMode = vMode;
            bean.phoneRemindRingMode = rMode;
        } catch (Exception e) {
            e.printStackTrace();
            showTips(getString(R.string.s238));
            return;
        }
        ControlBleTools.getInstance().setNotificationSettings(bean, new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
            @Override
            public void onState(SendCmdState state) {
                switch (state) {
                    case SUCCEED:
                        MyApplication.showToast(getString(R.string.s220));
                        break;
                    default:
                        MyApplication.showToast(getString(R.string.s221));
                        break;
                }
            }
        });
    }
    //endregion

    //region 连续血氧

    CheckBox cbBoModeSwitch;
    EditText etBoReminderSwitch;
    EditText etBoSH;
    EditText etBoSM;
    EditText etBoEH;
    EditText etBoEM;

    public void onBloodOxygenSettingsClick(View v) {
        int id = v.getId();
        if (id == R.id.btnGetBoMode) {
            ControlBleTools.getInstance().getContinuousBloodOxygenSettings(new ParsingStateManager.SendCmdStateListener() {
                @Override
                public void onState(@NonNull SendCmdState state) {
                    switch (state) {
                        case SUCCEED:
                            MyApplication.showToast(R.string.s220);
                            break;
                        default:
                            MyApplication.showToast(R.string.s221);
                            break;
                    }
                }
            });
        } else if (id == R.id.btnSetBoMode) {
            setContinuousBloodOxygenSettings();
        }
    }

    private void initContinuousBloodOxygenSettingsData() {
        DeviceSettingLiveData.getInstance().getmContinuousBloodOxygenSettings().observe(this, new Observer<ContinuousBloodOxygenSettingsBean>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onChanged(ContinuousBloodOxygenSettingsBean settingsBean) {
                if (settingsBean != null) {
                    Log.i(TAG, "ContinuousBloodOxygenSettingsBean == " + settingsBean);
                    // 0 开启  1 关闭
                    cbBoModeSwitch.setChecked(settingsBean.mode == 0);
                    etBoReminderSwitch.setText(settingsBean.frequency + "");
                    ToastDialog.showToast(SetActivity.this, getString(R.string.s343) + "：\n" + settingsBean);
                }
            }
        });
    }

    private void setContinuousBloodOxygenSettings() {
        ContinuousBloodOxygenSettingsBean bean = new ContinuousBloodOxygenSettingsBean();
        try {
            bean.mode = cbBoModeSwitch.isChecked() ? 0 : 1;
            bean.frequency = Integer.parseInt(etBoReminderSwitch.getText().toString().trim());
            int sh = Integer.parseInt(etBoSH.getText().toString().trim());
            int sm = Integer.parseInt(etBoSM.getText().toString().trim());
            int eh = Integer.parseInt(etBoEH.getText().toString().trim());
            int em = Integer.parseInt(etBoEM.getText().toString().trim());
            bean.startTime = new SettingTimeBean(sh, sm);
            bean.endTime = new SettingTimeBean(eh, em);
        } catch (Exception e) {
            e.printStackTrace();
            showTips(getString(R.string.s238));
            return;
        }
        ControlBleTools.getInstance().setContinuousBloodOxygenSettings(bean, new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
            @Override
            public void onState(SendCmdState state) {
                switch (state) {
                    case SUCCEED:
                        MyApplication.showToast(getString(R.string.s220));
                        break;
                    default:
                        MyApplication.showToast(getString(R.string.s221));
                        break;
                }
            }
        });
    }
    //endregion

    //region 找手表设置

    EditText etFwRMode;
    EditText etFwVMode;

    public void onFindWearSettingsClick(View v) {
        int id = v.getId();
        if (id == R.id.btnGetFw) {
            ControlBleTools.getInstance().getFindWearSettings(new ParsingStateManager.SendCmdStateListener() {
                @Override
                public void onState(@NonNull SendCmdState state) {
                    switch (state) {
                        case SUCCEED:
                            MyApplication.showToast(R.string.s220);
                            break;
                        default:
                            MyApplication.showToast(R.string.s221);
                            break;
                    }
                }
            });
        } else if (id == R.id.btnSetFw) {
            setFindWearSetting();
        }
    }

    private void initFindWearSettingsData() {
        DeviceSettingLiveData.getInstance().getmFindWearSettingsBean().observe(this, new Observer<FindWearSettingsBean>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onChanged(FindWearSettingsBean settingsBean) {
                if (settingsBean != null) {
                    Log.i(TAG, "getmFindWearSettingsBean == " + settingsBean);
                    etFwRMode.setText(settingsBean.ringMode + "");
                    etFwVMode.setText(settingsBean.vibrationMode + "");
                    ToastDialog.showToast(SetActivity.this, getString(R.string.s348) + "：\n" + settingsBean);
                }
            }
        });
    }


    private void setFindWearSetting() {
        FindWearSettingsBean bean = new FindWearSettingsBean();
        try {
            bean.ringMode = Integer.parseInt(etFwRMode.getText().toString().trim());
            bean.vibrationMode = Integer.parseInt(etFwVMode.getText().toString().trim());
        } catch (Exception e) {
            e.printStackTrace();
            showTips(getString(R.string.s238));
            return;
        }
        ControlBleTools.getInstance().setFindWearSettings(bean, new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
            @Override
            public void onState(SendCmdState state) {
                switch (state) {
                    case SUCCEED:
                        MyApplication.showToast(getString(R.string.s220));
                        break;
                    default:
                        MyApplication.showToast(getString(R.string.s221));
                        break;
                }
            }
        });
    }
    //endregion

    //region CY100-联系人设置

    EditText etCyContactName;
    EditText etCyContactPh;

    public void onCYClick(View v) {
        List<ContactBean> list = new ArrayList<>();
        String name = "contact";
        String phone = "181000000";
        try {
            if (!etCyContactName.getText().toString().trim().isEmpty()) {
                name = etCyContactName.getText().toString().trim();
            }
            if (!etCyContactPh.getText().toString().trim().isEmpty()) {
                phone = etCyContactPh.getText().toString().trim();
            }
        } catch (Exception e) {
            e.printStackTrace();
            showTips(getString(R.string.s238));
            return;
        }
        for (int i = 0; i < 50; i++) {
            ContactBean contactBean = new ContactBean();
            contactBean.contacts_name = i + name;
            contactBean.contacts_number = phone + i;
            list.add(contactBean);
        }
        ContactLotBean contactLotBean = new ContactLotBean();
        contactLotBean.allCount = list.size();
        contactLotBean.data = list;
        ControlBleTools.getInstance().setContactLotList(contactLotBean, new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
            @Override
            public void onState(SendCmdState state) {
                switch (state) {
                    case SUCCEED:
                        MyApplication.showToast(getString(R.string.s220));
                        break;
                    default:
                        MyApplication.showToast(getString(R.string.s221));
                        break;
                }
            }
        });
    }

    public void onSetCYClick(View v) {
        ControlBleTools.getInstance().getContactLotList(new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
            @Override
            public void onState(SendCmdState state) {
                switch (state) {
                    case SUCCEED:
                        MyApplication.showToast(getString(R.string.s220));
                        break;
                    default:
                        MyApplication.showToast(getString(R.string.s221));
                        break;
                }
            }
        });
    }

    private void initContactsLotData() {
        CallBackUtils.contactLotCallBack = new ContactLotCallBack() {
            @Override
            public void onContactLot(ContactLotBean contactLotBean) {
                ToastDialog.showToast(SetActivity.this, "" + GsonUtils.toJson(contactLotBean));
            }
        };
    }

    //endregion

    //region 产测重启 产测重置 心率漏光测试
    public void onRestart(View v) {
        if (ControlBleTools.getInstance().isConnect()) {
            ControlBleTools.getInstance().restartByProduction();
        }
    }

    public void onReset(View v) {
        if (ControlBleTools.getInstance().isConnect()) {
            ControlBleTools.getInstance().resetByProduction();
        }
    }

    public void onHeartLightLeakTest(View v) {
        if (ControlBleTools.getInstance().isConnect()) {
            ControlBleTools.getInstance().heartLightLeakTestByProduction();
        }
    }
    //endregion

    //region 呼吸灯测试
    public void onLight(View view) {
        startActivity(new Intent(this, LightSetActivity.class));
    }
    //endregion

    //region EV电动摩托车
    public void onEv(View view) {
        startActivity(new Intent(this, EvActivity.class));
    }
    //endregion

    //region 描述
    public void onRingAir(View view) {
        if (ControlBleTools.getInstance().isConnect()) {
            ControlBleTools.getInstance().setRingAirplaneMode(new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
                @Override
                public void onState(SendCmdState state) {
                    switch (state) {
                        case SUCCEED:
                            MyApplication.showToast(getString(R.string.s220));
                            break;
                        default:
                            MyApplication.showToast(getString(R.string.s221));
                            break;
                    }
                }
            });
        }
    }

    //endregion


}
