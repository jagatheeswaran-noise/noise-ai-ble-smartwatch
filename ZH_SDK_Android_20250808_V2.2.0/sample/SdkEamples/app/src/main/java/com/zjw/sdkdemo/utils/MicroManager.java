package com.zjw.sdkdemo.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.util.Log;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.LogUtils;
import com.zhapp.ble.ControlBleTools;
import com.zhapp.ble.bean.WidgetBean;
import com.zhapp.ble.callback.BerryDevReqContactCallBack;
import com.zhapp.ble.callback.CallBackUtils;
import com.zhapp.ble.callback.MicroCallBack;
import com.zjw.sdkdemo.R;
import com.zjw.sdkdemo.app.MyApplication;
import com.zjw.sdkdemo.livedata.DeviceSettingLiveData;

import java.util.List;

/**
 * Created by Android on 2022/1/4.
 */
public class MicroManager {
    private final static String TAG = MicroManager.class.getSimpleName();

    private MicroManager() {
    }

    private static class SingletonHolder {
        public static final MicroManager INSTANCE = new MicroManager();
    }

    public static MicroManager getInstance() {
        return MicroManager.SingletonHolder.INSTANCE;
    }

    public void initMicroCallBack() {
        /**
         * 小功能综合
         * */
        CallBackUtils.setMicroCallBack(new MicroCallBack() {
            @Override
            public void onWearSendFindPhone(int mode) {
                Log.i(TAG, MyApplication.context.getString(R.string.s245) + mode);
                findPhone(mode);
            }

            @Override
            public void onPhotograph(int status) {
                //0 开始摇一摇拍照 1 结束摇一摇拍  2 拍照
                //0 start shaking to take pictures 1 end shaking and taking pictures 2 take pictures
                if (status == PhotographMode.PHOTOGRAPH_START.getMode()) {
                    Log.i(TAG, MyApplication.context.getString(R.string.s29));
                    ToastDialog.showToast(ActivityUtils.getTopActivity(), MyApplication.context.getString(R.string.s29));
                } else if (status == PhotographMode.PHOTOGRAPH_STOP.getMode()) {
                    Log.i(TAG, MyApplication.context.getString(R.string.s30));
                    ToastDialog.showToast(ActivityUtils.getTopActivity(), MyApplication.context.getString(R.string.s30));
                } else if (status == PhotographMode.PHOTOGRAPHING.getMode()) {
                    Log.i(TAG, MyApplication.context.getString(R.string.s251));
                    ToastDialog.showToast(ActivityUtils.getTopActivity(), MyApplication.context.getString(R.string.s246));
                }
            }

            @Override
            public void onWidgetList(List<WidgetBean> list) {
                Log.i(TAG, MyApplication.context.getString(R.string.s247) + list.toString());
                DeviceSettingLiveData.getInstance().getWidgetList().postValue(list);
            }

            @Override
            public void onApplicationList(List<WidgetBean> list) {
                Log.i(TAG, MyApplication.context.getString(R.string.s248) + list.toString());
                DeviceSettingLiveData.getInstance().getApplicationList().postValue(list);
            }

            @Override
            public void onSportTypeIconList(List<WidgetBean> list) {

            }

            @Override
            public void onSportTypeOtherList(List<WidgetBean> list) {

            }

            @Override
            public void onQuickWidgetList(List<WidgetBean> list) {

            }

            @Override
            public void onSportWidgetSortList(List<WidgetBean> list) {
                Log.i(TAG, MyApplication.context.getString(R.string.s297) + list.toString());
                LogUtils.json(list);
                DeviceSettingLiveData.getInstance().getSportWidgetList().postValue(list);
            }

            @Override
            public void onNfcSleepErr(int error) {
                Log.i(TAG, "NFC Error :" + error);
                DeviceSettingLiveData.getInstance().getRingNFCSleepError().postValue(error);
            }
        });

        CallBackUtils.setBerryDevReqContactCallBack(new BerryDevReqContactCallBack() {
            @Override
            public void onDeviceRequestContact(String phoneNumber) {
                ControlBleTools.getInstance().updateBerryContactInfo("test", phoneNumber, null);
            }
        });
    }

    //region 设备查找手机
    public void findPhone(int mode) {
        if (mode == MicroCallBack.FindPhoneMode.FIND_START.getMode()) {
            startFindPhone();
        } else if (mode == MicroCallBack.FindPhoneMode.FIND_STOP.getMode()) {
            stopFindPhone();
        }
    }

    MediaPlayer mMediaPlayer;
    private AlertDialog findPhoneDialog;
    private AudioManager am;
    private Vibrator vibrator;
    private int volume;

    private void startFindPhone() {
        //播放铃声
        Uri mediaUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        if (mMediaPlayer == null) {
            mMediaPlayer = MediaPlayer.create(MyApplication.context, mediaUri);
            mMediaPlayer.setLooping(true);
            mMediaPlayer.start();
        }
        //音量飚大
        if (am == null) {
            am = (AudioManager) MyApplication.context.getSystemService(Context.AUDIO_SERVICE);
        }
        try {
            volume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
            am.setStreamVolume(AudioManager.STREAM_MUSIC,
                    am.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                    AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        //震动
        if (vibrator == null) {
            vibrator = (Vibrator) MyApplication.context.getSystemService(Service.VIBRATOR_SERVICE);
        }
        vibrator.vibrate(new long[]{2000, 1000, 2000, 1000}, 0);
        //弹窗
        findPhoneDialog = new AlertDialog.Builder(ActivityUtils.getTopActivity())
                .setTitle(MyApplication.context.getString(R.string.s245))
                .setMessage(MyApplication.context.getString(R.string.s249))
                .setPositiveButton(MyApplication.context.getString(R.string.s250), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (findPhoneDialog != null && findPhoneDialog.isShowing()) {
                            findPhoneDialog.dismiss();
                            stopFindPhone();
                            //发送给设备结束找手机
                            ControlBleTools.getInstance().sendCloseFindPhone(null);
                        }

                    }
                }).create();
        findPhoneDialog.show();

    }

    private void stopFindPhone() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        if (vibrator != null) {
            vibrator.cancel();
            vibrator = null;
        }
        if (volume != 0) {
            try {
                if (am == null) {
                    am = (AudioManager) MyApplication.context.getSystemService(Context.AUDIO_SERVICE);
                }
                am.setStreamVolume(AudioManager.STREAM_MUSIC,
                        volume,
                        AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
        if (findPhoneDialog != null && findPhoneDialog.isShowing()) {
            findPhoneDialog.cancel();
        }
    }
    //endregion

}
