package com.zjw.sdkdemo.function;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.blankj.utilcode.util.GsonUtils;
import com.zh.ble.wear.protobuf.MusicProtos;
import com.zhapp.ble.ControlBleTools;
import com.zhapp.ble.bean.ClockInfoBean;
import com.zhapp.ble.bean.CommonReminderBean;
import com.zhapp.ble.bean.EventInfoBean;
import com.zhapp.ble.bean.MusicInfoBean;
import com.zhapp.ble.bean.SettingTimeBean;
import com.zhapp.ble.bean.SleepReminder;
import com.zhapp.ble.bean.TimeBean;
import com.zhapp.ble.callback.CallBackUtils;
import com.zhapp.ble.callback.CallStateCallBack;
import com.zhapp.ble.callback.MusicCallBack;
import com.zhapp.ble.parsing.ParsingStateManager;
import com.zhapp.ble.parsing.SendCmdState;
import com.zjw.sdkdemo.R;
import com.zjw.sdkdemo.app.MyApplication;
import com.zjw.sdkdemo.function.language.BaseActivity;
import com.zjw.sdkdemo.livedata.DeviceSettingLiveData;
import com.zjw.sdkdemo.utils.ToastDialog;

import androidx.annotation.NonNull;
import androidx.lifecycle.Observer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressLint("NonConstantResourceId")
/**
 * CTRL SHIFT -
 * CTRL SHIFT +
 * */
public class RemindActivity extends BaseActivity {
    final private String TAG = RemindActivity.class.getSimpleName();
    //region 音乐
    EditText etMusicTitle;

    EditText etMusicState;

    EditText etMusicNew;
    //endregion
    //region 系统通知

    EditText etPhone;

    EditText etContacts;

    EditText etMsg;
    //endregion
    //region 第三方通知

    Button btnAppApp;

    EditText etNTitle;

    EditText etAppName;

    EditText etPackName;

    EditText etNText;

    EditText etNTicker;
    //endregion
    //region 快速回复

    EditText etReply;
    //endregion
    //region 久坐提醒

    CheckBox cbSSwitch;

    CheckBox cbSSwitch2;

    EditText etSSH;

    EditText etSSM;

    EditText etSEH;

    EditText etSEM;

    EditText etSInterval;
    //endregion
    //region 事件提醒

    EditText etEventDescr;

    EditText etEventFinish;

    EditText etEy;

    EditText etEM;

    EditText etEd;

    EditText etEH;

    EditText etEm;

    EditText etEs;

    CheckBox cbSleepSwitch;

    EditText etSleepH;

    EditText etSleepM;
    //endregion
    //region 闹钟

    CheckBox cbCSwitch;


    EditText etCName;
    EditText etCSH;
    EditText etCSM;
    CheckBox cbC1;
    CheckBox cbC2;
    CheckBox cbC3;
    CheckBox cbC4;
    CheckBox cbC5;
    CheckBox cbC6;
    CheckBox cbC7;
    //endregion

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remind);
        setTitle(getString(R.string.s59));
        initView();
        initData();
    }

    void initView() {
        etMusicTitle = findViewById(R.id.etMusicTitle);
        cbC7 = findViewById(R.id.cbC7);
        cbC6 = findViewById(R.id.cbC6);
        cbC5 = findViewById(R.id.cbC5);
        cbC4 = findViewById(R.id.cbC4);
        cbC3 = findViewById(R.id.cbC3);
        cbC2 = findViewById(R.id.cbC2);
        cbC1 = findViewById(R.id.cbC1);
        etCSM = findViewById(R.id.etCSM);
        etMusicState = findViewById(R.id.etMusicState);
        etMusicNew = findViewById(R.id.etMusicNew);
        etPhone = findViewById(R.id.etPhone);
        etContacts = findViewById(R.id.etContacts);
        etMsg = findViewById(R.id.etMsg);
        btnAppApp = findViewById(R.id.btnAPP);
        etNTitle = findViewById(R.id.etNTitle);
        etAppName = findViewById(R.id.etAppName);
        etPackName = findViewById(R.id.etPackName);
        etNText = findViewById(R.id.etNText);
        etNTicker = findViewById(R.id.etNTicker);
        etReply = findViewById(R.id.etReply);
        cbSSwitch = findViewById(R.id.cbSSwitch);
        cbSSwitch2 = findViewById(R.id.cbSSwitch2);
        etSSH = findViewById(R.id.etSSH);
        etSSM = findViewById(R.id.etSSM);
        etSEH = findViewById(R.id.etSEH);
        etSEM = findViewById(R.id.etSEM);
        etSInterval = findViewById(R.id.etSInterval);
        etEventDescr = findViewById(R.id.etEventDescr);
        etEventFinish = findViewById(R.id.etEventFinish);
        etEy = findViewById(R.id.etEy);
        etEM = findViewById(R.id.etEM);
        etEd = findViewById(R.id.etEd);
        etEH = findViewById(R.id.etEH);
        etEm = findViewById(R.id.etEm);
        etEs = findViewById(R.id.etEs);
        cbSleepSwitch = findViewById(R.id.cbSleepSwitch);
        etSleepH = findViewById(R.id.etSleepH);
        etSleepM = findViewById(R.id.etSleepM);
        cbCSwitch = findViewById(R.id.cbCSwitch);
        etCName = findViewById(R.id.etCName);
        etCSH = findViewById(R.id.etCSH);


    }

    void initData() {
        /**
         * 处理音乐相关回调
         * */
        CallBackUtils.musicCallBack = new MusicCallBack() {
            @Override
            public void onRequestMusic() { //设备进入音乐界面，请求音乐信息，此时发送音乐信息设备才有反应
                Log.d(TAG, "RequestMusic");
                syncMusic();
            }

            @Override
            public void onSyncMusic(int errorCode) { //syncMusic结果
                Log.d(TAG, "onSyncMusic" + errorCode);
            }

            @Override
            public void onQuitMusic() {
                Log.d(TAG, getString(R.string.s240));
            }

            @Override
            public void onSendMusicCmd(int command) { //设备控制指令
                switch (command) {
                    case MusicProtos.SEPlayerControlCommand.PLAYING_VALUE:
                    case MusicProtos.SEPlayerControlCommand.PAUSE_VALUE:
                        controlMusic(RemindActivity.this, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                        break;
                    case MusicProtos.SEPlayerControlCommand.PREV_VALUE:
                        controlMusic(RemindActivity.this, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
                        break;
                    case MusicProtos.SEPlayerControlCommand.NEXT_VALUE:
                        controlMusic(RemindActivity.this, KeyEvent.KEYCODE_MEDIA_NEXT);
                        break;
                    case MusicProtos.SEPlayerControlCommand.ADJUST_VOLUME_UP_VALUE:
                        //调用系统提高音量
                        if (audioManager != null) {
                            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND);
                        }
                        //发送音乐信息
                        syncMusic();
                        break;
                    case MusicProtos.SEPlayerControlCommand.ADJUST_VOLUME_DOWN_VALUE:
                        if (audioManager != null) {
                            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND);
                        }
                        //发送音乐信息
                        syncMusic();
                        break;
                }
            }
        };

        /**
         * 设备端处理来电回调
         * */
        DeviceSettingLiveData.getInstance().getmCallState().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer state) {
                if (state == null) return;
                if (state == CallStateCallBack.CallState.ANSWER_PHONE.getState()) {
                    //0 接电话
                    ToastDialog.showToast(RemindActivity.this, getString(R.string.s227));
                } else if (state == CallStateCallBack.CallState.HANG_PHONE.getState()) {
                    //1 挂电话
                    ToastDialog.showToast(RemindActivity.this, getString(R.string.s226));
                } else if (state == CallStateCallBack.CallState.MUTE.getState()) {
                    //2 静音
                    ToastDialog.showToast(RemindActivity.this, getString(R.string.s228));
                }
            }
        });

        DeviceSettingLiveData.getInstance().getmShortReply().observe(this, new Observer<ArrayList<String>>() {
            @Override
            public void onChanged(ArrayList<String> strings) {
                if (strings == null) return;
                Log.i(TAG, "ShortReply == " + strings);
                ToastDialog.showToast(RemindActivity.this, getString(R.string.s229) + "\n" + strings);
            }
        });

        /**
         * 获取久坐提醒数据
         * */
        DeviceSettingLiveData.getInstance().getmSedentaryReminder().observe(this, new Observer<CommonReminderBean>() {
            @Override
            public void onChanged(CommonReminderBean commonReminderBean) {
                if (commonReminderBean == null) return;
                Log.i(TAG, "SedentaryReminder == " + commonReminderBean);
                ToastDialog.showToast(RemindActivity.this, getString(R.string.s230) + "\n" + GsonUtils.toJson(commonReminderBean));
            }
        });

        /**
         * 获取喝水提醒数据
         * */
        DeviceSettingLiveData.getInstance().getmDrinkWaterReminder().observe(this, new Observer<CommonReminderBean>() {
            @Override
            public void onChanged(CommonReminderBean commonReminderBean) {
                if (commonReminderBean == null) return;
                Log.i(TAG, "DrinkWaterReminder == " + commonReminderBean);
                ToastDialog.showToast(RemindActivity.this, getString(R.string.s231) + "\n" + GsonUtils.toJson(commonReminderBean));
            }
        });

        /**
         * 获取吃药提醒数据
         * */
        DeviceSettingLiveData.getInstance().getmMedicationReminder().observe(this, new Observer<CommonReminderBean>() {
            @Override
            public void onChanged(CommonReminderBean commonReminderBean) {
                if (commonReminderBean == null) return;
                Log.i(TAG, "MedicationReminder == " + commonReminderBean);
                ToastDialog.showToast(RemindActivity.this, getString(R.string.s61) + "\n" + GsonUtils.toJson(commonReminderBean));
            }
        });

        /**
         * 获取吃饭提醒数据
         */
        DeviceSettingLiveData.getInstance().getmHaveMealsReminder().observe(this, new Observer<CommonReminderBean>() {
            @Override
            public void onChanged(CommonReminderBean commonReminderBean) {
                if (commonReminderBean == null) return;
                Log.i(TAG, "HaveMealsReminder == " + commonReminderBean);
                ToastDialog.showToast(RemindActivity.this, getString(R.string.s262) + "\n" + GsonUtils.toJson(commonReminderBean));
            }
        });

        DeviceSettingLiveData.getInstance().getmWashHandReminder().observe(this, new Observer<CommonReminderBean>() {
            @Override
            public void onChanged(CommonReminderBean commonReminderBean) {
                if (commonReminderBean == null) return;
                Log.i(TAG, "WashHandReminder == " + commonReminderBean);
                ToastDialog.showToast(RemindActivity.this, getString(R.string.s263) + "\n" + GsonUtils.toJson(commonReminderBean));
            }
        });

        DeviceSettingLiveData.getInstance().getmSleepReminder().observe(this, new Observer<SleepReminder>() {
            @Override
            public void onChanged(SleepReminder sleepReminder) {
                if (sleepReminder == null) return;
                Log.i(TAG, "SleepReminder == " + sleepReminder);
                ToastDialog.showToast(RemindActivity.this, getString(R.string.s465) + "\n" + GsonUtils.toJson(sleepReminder));
            }
        });

        /**
         * 获取事件提醒数据
         * */
        DeviceSettingLiveData.getInstance().getmEventInfo().observe(this, new Observer<List<EventInfoBean>>() {
            @Override
            public void onChanged(List<EventInfoBean> eventInfoBeans) {
                if (eventInfoBeans == null) return;
                Log.i(TAG, "EventInfoBeans == " + eventInfoBeans);
                ToastDialog.showToast(RemindActivity.this, getString(R.string.s232) + "\n" + GsonUtils.toJson(eventInfoBeans));
            }
        });
        /**
         * 获取事件提醒支持设置的最大数量
         * */
        DeviceSettingLiveData.getInstance().getmEventMax().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer supportMax) {
                if (supportMax == null) return;
                Log.i(TAG, "EventInfo supportMax == " + supportMax);
                Toast.makeText(RemindActivity.this, getString(R.string.s233) + " = " + supportMax, Toast.LENGTH_LONG).show();
                eventMax = supportMax;
            }
        });

        /**
         * 获取闹钟提醒数据
         * */
        DeviceSettingLiveData.getInstance().getmClockInfo().observe(this, new Observer<List<ClockInfoBean>>() {
            @Override
            public void onChanged(List<ClockInfoBean> clockInfoBeans) {
                if (clockInfoBeans == null) return;
                Log.i(TAG, "ClockInfoBeans == " + clockInfoBeans);
                ToastDialog.showToast(RemindActivity.this, getString(R.string.s234) + "\n" + clockInfoBeans);
            }
        });
        /**
         * 获取闹钟提醒支持设置的最大数量
         * */
        DeviceSettingLiveData.getInstance().getmClockMax().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer supportMax) {
                if (supportMax == null) return;
                Log.i(TAG, "ClockInfo supportMax == " + supportMax);
                Toast.makeText(RemindActivity.this, getString(R.string.s235) + " = " + supportMax, Toast.LENGTH_LONG).show();
                clockMax = supportMax;
            }
        });
    }

    //region 音乐
    private AudioManager audioManager;

    /**
     * 同步音乐信息
     */
    private void syncMusic() {
        if (audioManager == null) {
            audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        }
        String mMusicTitle = etMusicTitle.getText().toString().trim();
        String strState = etMusicState.getText().toString().trim();
        String sMusicNew = etMusicNew.getText().toString().trim();

        int mMusicState = 0;
        try {
            mMusicState = Integer.parseInt(strState);
        } catch (Exception e) {
            e.printStackTrace();
        }

        int mCurrent = 0;
        if (audioManager != null) {
            mCurrent = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        }

        int mMaxVolume = 0;
        if (audioManager != null) {
            mMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        }

        boolean isNewPermissionType = sMusicNew.contains("1");

        //当应用通知访问权限未开启时，收到设备获取音乐信息请求MusicCallBack.onRequestMusic()后APP应下发音乐无权限指令通知设备，以便设备显示无权限页面。ControlBleTools.getInstance().syncMusicInfo(new MusicInfoBean(1, "", 0, 0), null)
        MusicInfoBean musiceInfoBean = new MusicInfoBean(mMusicState, mMusicTitle, mCurrent, mMaxVolume, isNewPermissionType);
        ControlBleTools.getInstance().syncMusicInfo(musiceInfoBean, new ParsingStateManager.SendCmdStateListener() {
            @Override
            public void onState(@NonNull SendCmdState state) {
                switch (state) {
                    case SUCCEED:
                        MyApplication.showToast("同步音乐成功");
                        break;
                    default:
                        MyApplication.showToast("同步音乐超时");
                        break;
                }
            }
        });
    }

    /**
     * 音乐控制指令
     *
     * @param context
     * @param keyCode
     */
    public static void controlMusic(Context context, int keyCode) {
        long eventTime = SystemClock.uptimeMillis();
        KeyEvent key = new KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0);
        dispatchMediaKeyToAudioService(context, key);
        dispatchMediaKeyToAudioService(context, KeyEvent.changeAction(key, KeyEvent.ACTION_UP));
    }

    private static void dispatchMediaKeyToAudioService(Context context, KeyEvent event) {
        AudioManager audioManager = (AudioManager) context.getSystemService(AUDIO_SERVICE);
        if (audioManager != null) {
            try {
                audioManager.dispatchMediaKeyEvent(event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    //endregion

    //region 设置快速回复
    private void sendShortReply() {
        ArrayList<String> replys = new ArrayList<>();
        String reply = etReply.getText().toString().trim();
        try {
            if (reply.contains(",")) {
                String[] rs = reply.split(",");
                replys.addAll(Arrays.asList(rs));
            } else {
                replys.add(reply);
            }
        } catch (Exception e) {
            e.printStackTrace();
            replys.add(reply);
        }
        //list可设置多个 / 修改 / 删除 ,TODO 最多不能超过5个
        ControlBleTools.getInstance().setDevShortReplyData(replys, new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
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

    //region 发送系统通知

    /**
     * 发送系统通知
     */
    private void sendSysNotice(int type) {
        String phone = etPhone.getText().toString().trim();
        String contancts = etContacts.getText().toString().trim();
        String msg = etMsg.getText().toString().trim();
        ControlBleTools.getInstance().sendSystemNotification(type, phone, contancts, msg, null);
    }

    //endregion

    //region 发送第三方app通知
    private String appPackName = "";
    private String appName = "";
    private LinearLayout appLayout;

    private void showAppDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(getLayoutInflater().inflate(R.layout.dialog_app, null));
        appLayout = dialog.findViewById(R.id.appLayout);
        dialog.setCancelable(false);
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = this.getWindowManager().getDefaultDisplay().getWidth();
        params.height = this.getWindowManager().getDefaultDisplay().getHeight();
        dialog.getWindow().setAttributes(params);
        PackageManager pm = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, 0);
        for (ResolveInfo r : resolveInfos) {
            if (r.activityInfo != null && r.activityInfo.packageName != null) {
                Button view = new Button(this);
                view.setText(r.loadLabel(pm).toString());
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        appPackName = r.activityInfo.packageName;
                        appName = r.loadLabel(pm).toString();
                        etAppName.setText(appName);
                        etPackName.setText(appPackName);
                        btnAppApp.setText(appName);
                        dialog.dismiss();
                    }
                });
                appLayout.addView(view);
            }
        }
        if (resolveInfos.size() > 0) {
            dialog.show();
        } else {
            ToastDialog.showToast(RemindActivity.this, getString(R.string.s236));
        }
    }

    private void sendAppNotice() {
        appPackName = etPackName.getText().toString().trim();
        appName = etAppName.getText().toString().trim();
        if (TextUtils.isEmpty(appPackName) || TextUtils.isEmpty(appName)) {
            Toast.makeText(RemindActivity.this, getString(R.string.s237), Toast.LENGTH_LONG).show();
            return;
        }
        String title = etNTitle.getText().toString().trim();
        String text = etNText.getText().toString().trim();
        String t = etNTicker.getText().toString().trim();
        ControlBleTools.getInstance().sendAppNotification(appName, appPackName, title, text, t, null);
    }
    //endregion

    //region 设置久坐提醒
    private void sendSedentaryReminder() {
        CommonReminderBean reminder = new CommonReminderBean();
        reminder.isOn = cbSSwitch.isChecked();
        reminder.noDisturbInLaunch = cbSSwitch2.isChecked();
        try {
            int stH = Integer.parseInt(etSSH.getText().toString().trim());
            int stM = Integer.parseInt(etSSM.getText().toString().trim());
            int etH = Integer.parseInt(etSEH.getText().toString().trim());
            int etM = Integer.parseInt(etSEM.getText().toString().trim());
            int frequency = Integer.parseInt(etSInterval.getText().toString().trim());
            reminder.startTime = new SettingTimeBean(stH, stM);
            reminder.endTime = new SettingTimeBean(etH, etM);
            reminder.frequency = frequency;
        } catch (Exception e) {
            e.printStackTrace();
            ToastDialog.showToast(RemindActivity.this, getString(R.string.s238));
            return;
        }
        ControlBleTools.getInstance().setSedentaryReminder(reminder, new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
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

    //region 设置喝水提醒
    private void sendDrinkWaterReminder() {
        CommonReminderBean reminder = new CommonReminderBean();
        reminder.isOn = cbSSwitch.isChecked();
        reminder.noDisturbInLaunch = cbSSwitch2.isChecked();
        try {
            int stH = Integer.parseInt(etSSH.getText().toString().trim());
            int stM = Integer.parseInt(etSSM.getText().toString().trim());
            int etH = Integer.parseInt(etSEH.getText().toString().trim());
            int etM = Integer.parseInt(etSEM.getText().toString().trim());
            int frequency = Integer.parseInt(etSInterval.getText().toString().trim());
            reminder.startTime = new SettingTimeBean(stH, stM);
            reminder.endTime = new SettingTimeBean(etH, etM);
            reminder.frequency = frequency;
        } catch (Exception e) {
            e.printStackTrace();
            ToastDialog.showToast(RemindActivity.this, getString(R.string.s238));
            return;
        }
        ControlBleTools.getInstance().setDrinkWaterReminder(reminder, new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
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

    //region 设置吃药提醒
    private void sendMedicationReminder() {
        CommonReminderBean reminder = new CommonReminderBean();
        reminder.isOn = cbSSwitch.isChecked();
        //TODO 吃药提醒无午休免打扰
        /*reminder.noDisturbInLaunch = cbSSwitch2.isChecked();*/
        try {
            int stH = Integer.parseInt(etSSH.getText().toString().trim());
            int stM = Integer.parseInt(etSSM.getText().toString().trim());
            int etH = Integer.parseInt(etSEH.getText().toString().trim());
            int etM = Integer.parseInt(etSEM.getText().toString().trim());
            int frequency = Integer.parseInt(etSInterval.getText().toString().trim());
            reminder.startTime = new SettingTimeBean(stH, stM);
            reminder.endTime = new SettingTimeBean(etH, etM);
            reminder.frequency = frequency;
        } catch (Exception e) {
            e.printStackTrace();
            ToastDialog.showToast(RemindActivity.this, getString(R.string.s238));
            return;
        }
        ControlBleTools.getInstance().setMedicationReminder(reminder, new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
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

    //region 设置吃饭提醒
    private void sendHaveMealsReminder() {
        CommonReminderBean reminder = new CommonReminderBean();
        reminder.isOn = cbSSwitch.isChecked();
        //TODO 吃饭提醒无午休免打扰
        /*reminder.noDisturbInLaunch = cbSSwitch2.isChecked();*/
        try {
            int stH = Integer.parseInt(etSSH.getText().toString().trim());
            int stM = Integer.parseInt(etSSM.getText().toString().trim());
            int etH = Integer.parseInt(etSEH.getText().toString().trim());
            int etM = Integer.parseInt(etSEM.getText().toString().trim());
            int frequency = Integer.parseInt(etSInterval.getText().toString().trim());
            reminder.startTime = new SettingTimeBean(stH, stM);
            reminder.endTime = new SettingTimeBean(etH, etM);
            reminder.frequency = frequency;
        } catch (Exception e) {
            e.printStackTrace();
            ToastDialog.showToast(RemindActivity.this, getString(R.string.s238));
            return;
        }
        ControlBleTools.getInstance().setHaveMealsWaterReminder(reminder, new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
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

    //region 设置洗手提醒
    private void sendWashHandReminder() {
        CommonReminderBean reminder = new CommonReminderBean();
        reminder.isOn = cbSSwitch.isChecked();
        //TODO 吃饭提醒无午休免打扰
        /*reminder.noDisturbInLaunch = cbSSwitch2.isChecked();*/
        try {
            int stH = Integer.parseInt(etSSH.getText().toString().trim());
            int stM = Integer.parseInt(etSSM.getText().toString().trim());
            int etH = Integer.parseInt(etSEH.getText().toString().trim());
            int etM = Integer.parseInt(etSEM.getText().toString().trim());
            int frequency = Integer.parseInt(etSInterval.getText().toString().trim());
            reminder.startTime = new SettingTimeBean(stH, stM);
            reminder.endTime = new SettingTimeBean(etH, etM);
            reminder.frequency = frequency;
        } catch (Exception e) {
            e.printStackTrace();
            ToastDialog.showToast(RemindActivity.this, getString(R.string.s238));
            return;
        }
        ControlBleTools.getInstance().setWashHandReminder(reminder, new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
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

    //region 设置睡眠提醒
    private void sendSleepReminder() {
        SleepReminder reminder = new SleepReminder();
        reminder.isOn = cbSleepSwitch.isChecked();
        try {
            int stH = Integer.parseInt(etSleepH.getText().toString().trim());
            int stM = Integer.parseInt(etSleepM.getText().toString().trim());
            reminder.reminderTime = new SettingTimeBean(stH, stM);
        } catch (Exception e) {
            e.printStackTrace();
            ToastDialog.showToast(RemindActivity.this, getString(R.string.s238));
            return;
        }
        ControlBleTools.getInstance().setSleepReminder(reminder, new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
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

    //region 设置事件提醒
    private int eventMax = 0; //获取设备最多支持设置数量

    private void sendEventInfo() {
        ArrayList<EventInfoBean> eventInfoBeans = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            EventInfoBean infoBean = new EventInfoBean();
            TimeBean timeBean;
            String des = etEventDescr.getText().toString().trim() + "" + i;
            try {
                int y = Integer.parseInt(etEy.getText().toString().trim());
                int M = Integer.parseInt(etEM.getText().toString().trim());
                int d = Integer.parseInt(etEd.getText().toString().trim());
                int H = Integer.parseInt(etEH.getText().toString().trim());
                int m = Integer.parseInt(etEm.getText().toString().trim());
                if (m < 55) {
                    m = m + i;
                } else {
                    m = m - i;
                }
                int s = Integer.parseInt(etEs.getText().toString().trim());
                //infoBean = new EventInfoBean(des,new TimeBean(y,M,d,H,m,s));
                timeBean = new TimeBean(y, M, d, H, m, s);
                infoBean.time = timeBean;
                infoBean.description = des;
                int f = Integer.parseInt(etEventFinish.getText().toString().trim());
                infoBean.isFinish = (f == 1);
            } catch (Exception e) {
                e.printStackTrace();
                ToastDialog.showToast(RemindActivity.this, getString(R.string.s238));
                return;
            }
            eventInfoBeans.add(infoBean);
        }

        //list 可设置多个 / 修改 / 删除 ,TODO 最多不能超过eventMax
        ControlBleTools.getInstance().setEventInfoList(eventInfoBeans, new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
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

    //region 设置闹钟提醒
    private int clockMax = 0; //获取设备最多支持设置数量

    private void sendClockInfo() {
        ArrayList<ClockInfoBean> clockInfoBeans = new ArrayList<>();
        ClockInfoBean clockInfo = new ClockInfoBean();
        ClockInfoBean.DataBean data = new ClockInfoBean.DataBean();
        try {
            int stH = Integer.parseInt(etCSH.getText().toString().trim());
            int stM = Integer.parseInt(etCSM.getText().toString().trim());
            data.time = new SettingTimeBean(stH, stM);
        } catch (Exception e) {
            e.printStackTrace();
            ToastDialog.showToast(RemindActivity.this, getString(R.string.s238));
            return;
        }
        data.clockName = etCName.getText().toString().trim();
        data.isEnable = cbCSwitch.isChecked();
        data.isMonday = cbC1.isChecked();
        data.isTuesday = cbC2.isChecked();
        data.isWednesday = cbC3.isChecked();
        data.isThursday = cbC4.isChecked();
        data.isFriday = cbC5.isChecked();
        data.isSaturday = cbC6.isChecked();
        data.isSunday = cbC7.isChecked();
        data.calculateWeekDays();
        clockInfo.id = 0; //闹钟数量下标
        clockInfo.data = data;
        clockInfoBeans.add(clockInfo);
        // 模拟增加多个
        clockInfoBeans.add(clockInfo);

        //list可设置多个 / 修改 / 删除 ,TODO 最多不能超过clockMax
        ControlBleTools.getInstance().setClockInfoList(clockInfoBeans, new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
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


    /*@OnClick({R.id.btnSyncMusic,                        //音乐
            R.id.btnSys0, R.id.btnSys1, R.id.btnSys2,     //系统通知
            R.id.btnSysPhoneHangup, R.id.btnSysPhoneAnswer, //通知设备挂断/接听电话
            R.id.btnAPP, R.id.btnSendAppN,               //第三方通知
            R.id.btnGetR, R.id.btnSetR,                  //快速回复
            R.id.btnGetS, R.id.btnSetS,                  //久坐提醒
            R.id.btnGetD, R.id.btnSetD,                  //喝水提醒
            R.id.btnGetM, R.id.btnSetM,                  //吃药提醒
            R.id.btnGetH, R.id.btnSetH,                  //吃药提醒
            R.id.btnGetW, R.id.btnSetW,                  //吃药提醒
            R.id.btnGetEvent, R.id.btnSetEvent,          //事件提醒
            R.id.btnGetSleep, R.id.btnSetSleep,                   //睡眠提醒
            R.id.btnGetC, R.id.btnSetC                   //闹钟提醒
    })*/
    public void click(View view) {
        int id = view.getId();
        if (id == R.id.btnSyncMusic) {
            syncMusic();
        } else if (id == R.id.btnGetR) {
            ControlBleTools.getInstance().getDevShortReplyData(new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
                @Override
                public void onState(SendCmdState state) {
                    switch (state) {
                        case SUCCEED:
                            Log.i(TAG, getString(R.string.s220));
                            //MyApplication.showToast(getString(R.string.s220));
                            break;
                        default:
                            Log.i(TAG, getString(R.string.s221));
                            //MyApplication.showToast(getString(R.string.s221));
                            break;
                    }
                }
            });
        } else if (id == R.id.btnSetR) {
            sendShortReply();
        } else if (id == R.id.btnSys0) {
            sendSysNotice(0);
        } else if (id == R.id.btnSys1) {
            sendSysNotice(1);
        } else if (id == R.id.btnSys2) {
            sendSysNotice(2);
        } else if (id == R.id.btnSysPhoneHangup) {
            ControlBleTools.getInstance().sendCallState(1, null);
        } else if (id == R.id.btnSysPhoneAnswer) {
            ControlBleTools.getInstance().sendCallState(0, null);
        } else if (id == R.id.btnAPP) {
            showAppDialog();
        } else if (id == R.id.btnSendAppN) {
            sendAppNotice();
        } else if (id == R.id.btnGetS) {
            ControlBleTools.getInstance().getSedentaryReminder(null);
        } else if (id == R.id.btnSetS) {
            sendSedentaryReminder();
        } else if (id == R.id.btnGetD) {
            ControlBleTools.getInstance().getDrinkWaterReminder(null);
        } else if (id == R.id.btnSetD) {
            sendDrinkWaterReminder();
        } else if (id == R.id.btnGetM) {
            ControlBleTools.getInstance().getMedicationReminder(null);
        } else if (id == R.id.btnSetM) {
            sendMedicationReminder();
        } else if (id == R.id.btnGetH) {
            ControlBleTools.getInstance().getHaveMealsReminder(null);
        } else if (id == R.id.btnSetH) {
            sendHaveMealsReminder();
        } else if (id == R.id.btnGetW) {
            ControlBleTools.getInstance().getWashHandReminder(null);
        } else if (id == R.id.btnSetW) {
            sendWashHandReminder();
        } else if (id == R.id.btnGetEvent) {
            ControlBleTools.getInstance().getEventInfoList(null);
        } else if (id == R.id.btnSetEvent) {
            sendEventInfo();
        } else if (id == R.id.btnGetSleep) {
            ControlBleTools.getInstance().getSleepReminder(null);
        } else if (id == R.id.btnSetSleep) {
            sendSleepReminder();
        } else if (id == R.id.btnGetC) {
            ControlBleTools.getInstance().getClockInfoList(null);
        } else if (id == R.id.btnSetC) {
            sendClockInfo();
        }
    }
}
