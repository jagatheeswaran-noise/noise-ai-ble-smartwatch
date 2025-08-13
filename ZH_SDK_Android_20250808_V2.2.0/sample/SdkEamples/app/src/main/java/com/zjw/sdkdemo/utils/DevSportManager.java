package com.zjw.sdkdemo.utils;

import android.util.Log;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ThreadUtils;
import com.zhapp.ble.ControlBleTools;
import com.zhapp.ble.bean.DevSportInfoBean;
import com.zhapp.ble.bean.PhoneSportDataBean;
import com.zhapp.ble.bean.SportRequestBean;
import com.zhapp.ble.bean.SportResponseBean;
import com.zhapp.ble.bean.SportStatusBean;
import com.zhapp.ble.callback.CallBackUtils;
import com.zhapp.ble.callback.SportCallBack;
import com.zhapp.ble.callback.SportParsingProgressCallBack;
import com.zhapp.ble.parsing.ParsingStateManager;
import com.zhapp.ble.parsing.SendCmdState;
import com.zhapp.ble.utils.SaveLog;
import com.zjw.sdkdemo.R;
import com.zjw.sdkdemo.app.MyApplication;

import java.util.Random;

/**
 * Created by Android on 2022/1/4.
 */
public class DevSportManager {
    private final static String TAG = DevSportManager.class.getSimpleName();

    private DevSportManager() {
    }

    private static class SingletonHolder {
        public static final DevSportManager INSTANCE = new DevSportManager();
    }

    public static DevSportManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public void initDevSportCallBack() {
        CallBackUtils.setSportCallBack(new SportCallBack() {
            @Override
            public void onDevSportInfo(DevSportInfoBean data) {
                LogUtils.d("运动数据回调线程：" + Thread.currentThread());
                saveData(data);
            }

            @Override
            public void onSportStatus(SportStatusBean statusBean) {
                if (statusBean != null) {
                    Log.d(TAG, MyApplication.context.getString(R.string.s163) + " -->" + statusBean);
                    ToastDialog.showToast(ActivityUtils.getTopActivity(), statusBean.toString());
                    deviceSportStatus(statusBean);
                }
            }

            @Override
            public void onSportRequest(SportRequestBean requestBean) {
                if (requestBean != null) {
                    Log.d(TAG, MyApplication.context.getString(R.string.s239) + "：--->" + requestBean);
                    MyApplication.showToast(MyApplication.context.getString(R.string.s239) + requestBean);
                    deviceRequest(requestBean);
                }
            }
        });

        CallBackUtils.setSportParsingProgressCallBack(new SportParsingProgressCallBack() {
            @Override
            public void onProgress(int progress, int total) {
                Log.d(TAG, MyApplication.context.getString(R.string.s243) + "：--->total = " + total + ", progress = " + progress);
                MyApplication.showToast("sport Progress --->total = " + total + ", progress = " + progress);
                //ToastDialog.showToast(ActivityUtils.getTopActivity(), "sport Progress --->total = " + total + ", progress = " + progress);
            }
        });
    }


    /**
     * 获取设备运动状态
     */
    public void getDevSportStatus() {
        ControlBleTools.getInstance().getSportStatus(new ParsingStateManager.SendCmdStateListener(null) {
            @Override
            public void onState(SendCmdState state) {
                switch (state) {
                    case SUCCEED:
                        Log.i(TAG, MyApplication.context.getString(R.string.s220));
                        break;
                    default:
                        Log.i(TAG, MyApplication.context.getString(R.string.s221));
                        break;
                }
            }
        });
    }

    /**
     * 获取多运动数据
     */
    public void getFitnessSportIdsData() {
        ControlBleTools.getInstance().getFitnessSportIdsData(new ParsingStateManager.SendCmdStateListener(null) {
            @Override
            public void onState(SendCmdState state) {
                switch (state) {
                    case SUCCEED:
                        Log.i(TAG, MyApplication.context.getString(R.string.s220));
                        break;
                    default:
                        Log.i(TAG, MyApplication.context.getString(R.string.s221));
                        break;
                }
            }
        });
    }

    /**
     * 处理设备运动状态
     *
     * @param statusBean
     */
    private void deviceSportStatus(SportStatusBean statusBean) {
        //{"duration":0,"isAppLaunched":false,"isPaused":false,"isStandalone":false,"selectVersion":0,"sportType":0,"timestamp":0}
        //独立运动不需要app发送定位数据
        if (statusBean.isStandalone) {
            return;
        }
        //设备在运动 && 不是暂停
        if (statusBean.duration != 0L && statusBean.sportType != 0 && statusBean.timestamp != 0L
                && !statusBean.isPaused
        ) {
            isDeviceSporting = true;
            isPause = false;
            //未定位，开启定位
            /*if(!LocationService.binder.service.isLocationDoing){
                LocationService.binder.service.startLocation()
            }*/
            startLocation();

        } else {
            //设备运动中
            if (isDeviceSporting) {
                isDeviceSporting = false;
                //也不是app运动
                /*if(!LocationService.binder.service.isAppSport){
                //关闭定位
                    LocationService.binder.service.stopLocation()
                }*/
                stopLocation();
            }
        }
    }

    //region 辅助运动
    //设备是否再运动中
    private boolean isDeviceSporting = false;
    //设备运动是否暂停
    private boolean isPause = false;
    //定位经纬度
    private double mLatitude = 0.0;
    private double mLongitude = 0.0;
    //上次发送辅助定位数据的时间戳,经纬度
    private long mLastTime = 0L;
    //上次发送的定位经纬度
    private double mLastLat = 0.0;
    private double mLastLon = 0.0;

    /**
     * 处理设备运动状态变化
     *
     * @param devSportRequest
     */
    private void deviceRequest(SportRequestBean devSportRequest) {
        //{"sportType":1,"state":0,"supportVersions":0,"timestamp":1637229490}
        //GPS开启定位，进行预定位
        if (devSportRequest.state == 0) {
            // 回复设备SportResponseBean
            sendSportResponseBean();
        }
        /*if (!PermissionUtils.isGranted(*com.zhapp.infowear.utils.PermissionUtils.PERMISSION_GROUP_LOCATION) ||
            !AppUtils.isGPSOpen(BaseApplication.mContext)) {
            return
        }*/
        switch (devSportRequest.state) {
            case 1: //运动开始
                isDeviceSporting = true;
                isPause = false;
                //未定位，开启定位
                /*if(!LocationService.binder.service.isLocationDoing){
                    LocationService.binder.service.startLocation()
                }*/
                startLocation();
                break;
            case 2: //运动暂停
                isPause = true;
                break;
            case 3: //运动重新开始
                isPause = false;
                isDeviceSporting = true;
                //未定位，开启定位
                /*if(!LocationService.binder.service.isLocationDoing){
                    LocationService.binder.service.startLocation()
                }*/
                startLocation();
                break;
            case 4: //运动结束
                isDeviceSporting = false;
                isPause = false;
                //不在app运动，关闭定位
                /*if(!LocationService.binder.service.isAppSport){
                    LocationService.binder.service.stopLocation()
                }*/
                stopLocation();
                mLastTime = 0L;
                mLastLat = 0.0;
                mLastLon = 0.0;
                break;
        }
    }

    /**
     * 回复设备当前状态
     */
    private void sendSportResponseBean() {
        SportResponseBean response = new SportResponseBean();
        response.code = getResponseCode();
        response.gpsAccuracy = getGpsAccuracy();
        ControlBleTools.getInstance().replyDevSportRequest(response, new ParsingStateManager.SendCmdStateListener(null) {
            @Override
            public void onState(SendCmdState state) {
                switch (state) {
                    case SUCCEED:
                        MyApplication.showToast(MyApplication.context.getString(R.string.s220));
                        break;
                    default:
                        MyApplication.showToast(MyApplication.context.getString(R.string.s221));
                        break;
                }
            }
        });
        //开始辅助运动
        if (response.code == 0) {
            /*LocationService.binder.service.startLocation()*/
            startLocation();
            isDeviceSporting = true;
        }
    }

    /**
     * GPS 信号码
     *
     * @return
     */
    private int getGpsAccuracy() {
        //GPS状态 低 0; 中 1; 高 2; 未知 10;
        return 1;
    }

    /**
     * 回复状态码
     *
     * @return
     */
    private int getResponseCode() {
        //状态回应 0 OK; 1 设备正忙; 2 恢复/暂停类型不匹配; 3 没有位置权限;
        // 4 运动不支持; 5 精确gps关闭或后台无gps许可; 6 充电中; 7 低电量 ; 10 未知
        if (isDeviceSporting) {
            return 2;
        }
//        if (!PermissionUtils.isGranted(*PermissionUtils.PERMISSION_GROUP_LOCATION)) {
//            return 3
//        }
//        if(!AppUtils.isGPSOpen(BaseApplication.mContext)){
//            return 5
//        }
        return 0;
    }

    /**
     * 发送手机定位数据
     */
    private void sendPhoneLocationData() {
        if (!isDeviceSporting || isPause) return;
        if (mLatitude == 0.0 || mLongitude == 0.0) return;
        PhoneSportDataBean phoneSportDataBean = null;
        if (mLastTime == 0L || mLastLat == 0.0 || mLastLon == 0.0) {
            //初次定位发送
            phoneSportDataBean = new PhoneSportDataBean();
            phoneSportDataBean.gpsAccuracy = getGpsAccuracy();
            phoneSportDataBean.timestamp = (int) (System.currentTimeMillis() / 1000);
            phoneSportDataBean.latitude = mLatitude;
            phoneSportDataBean.longitude = mLongitude;
        } else {
            //5s | 定位有变化
            if (System.currentTimeMillis() - mLastTime >= 5000 ||
                    (mLatitude != mLastLat || mLongitude != mLastLon)) {
                phoneSportDataBean = new PhoneSportDataBean();
                phoneSportDataBean.gpsAccuracy = getGpsAccuracy();
                phoneSportDataBean.timestamp = (int) (System.currentTimeMillis() / 1000);
                phoneSportDataBean.latitude = mLatitude;
                phoneSportDataBean.longitude = mLongitude;
            }
        }
        if (phoneSportDataBean != null) {
            mLastTime = System.currentTimeMillis();
            mLastLat = mLatitude;
            mLastLon = mLongitude;

            ControlBleTools.getInstance().sendPhoneSportData(phoneSportDataBean, new ParsingStateManager.SendCmdStateListener(null) {
                @Override
                public void onState(SendCmdState state) {
                    switch (state) {
                        case SUCCEED:
                            //MyApplication.showToast("发送成功");
                            Log.d(TAG, MyApplication.context.getString(R.string.s244) + "：" + mLastLat + "," + mLongitude);
                            break;
                        default:
                            //MyApplication.showToast("发送超时");
                            break;
                    }
                }
            });
        }
    }

    /**
     * 设备连接上
     */
    public void testDevConnected() {
        if (ControlBleTools.getInstance().isConnect()) {
            //查询设备是否在运动
            getDevSportStatus();
        }
    }

    /**
     * 蓝牙连接断开
     */
    public void testBleClose() {
        //设备运动中
        if (isDeviceSporting) {
            isDeviceSporting = false;
            //关闭定位
            /*if (!LocationService.binder.service.isAppSport) {
                LocationService.binder.service.stopLocation()
            }*/
            stopLocation();
        }
    }


    //region TODO 定位
    private Thread testLocationThread;
    private final double[] TEST_LAT_DATA = new double[]{22.631818, 22.631812, 22.631823, 22.631834, 22.631845,
            22.631856, 22.631867, 22.631878, 22.631889, 22.631890, 22.631818};
    private final double[] TEST_LONG_DATA = new double[]{113.833136, 113.833112, 113.833123, 113.833134, 113.833145,
            113.833156, 113.833167, 113.833178, 113.833189, 113.833190, 113.833136};

    /**
     * 开始定位 TODO 换真实定位数据
     */
    private void startLocation() {
        if (testLocationThread == null) {
            Random random = new Random();
            testLocationThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            Thread.sleep(1000);

                            mLatitude = TEST_LAT_DATA[random.nextInt(TEST_LAT_DATA.length - 1)];
                            mLongitude = TEST_LONG_DATA[random.nextInt(TEST_LONG_DATA.length - 1)];
                            ThreadUtils.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    sendPhoneLocationData();
                                }
                            });
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            testLocationThread.start();
        }
    }

    /**
     * 结束定位 TODO 结束定位
     */
    private void stopLocation() {
        if (testLocationThread != null) {
            testLocationThread.interrupt();
            testLocationThread = null;
        }
    }
    //endregion

    //endregion


    /**
     * 收到设备运动数据
     * 储存至本地
     *
     * @param data
     */
    private void saveData(DevSportInfoBean data) {
        if (data == null) return;
        Log.d(TAG, MyApplication.context.getString(R.string.s166) + "：" + data);
        SaveLog.writeFile("sport", GsonUtils.toJson(data));
        //{map_data='113.83312,22.631857;113.83312,22.631857;113.83319,22.63189;113.83315,22.631887;113.83319,22.631844;113.833145,22.631868;113.83319,22.63189;113.83317,22.63189;113.833176,22.631834;113.83314,22.63189;113.83317,22.631844;113.833176,22.631819;113.833115,22.631844;113.83312,22.631878;113.833145,22.631834;113.833145,22.631834;113.83317,22.631811;113.833115,22.631844;113.83317,22.631819;113.83312,22.631878;113.833115,22.631834;113.83314,22.631819;113.833115,22.631819;113.833176,22.631878;113.833176,22.631878;113.83319,22.63189;113.83314,22.631834;113.83314,22.631834;113.83319,22.631834;113.833145,22.631811;113.83315,22.631824;113.83315,22.631824;113.833176,22.631857;113.83314,22.631824;113.83314,22.631824;113.83317,22.631857;113.833176,22.631819;113.83312,22.631834;113.83314,22.631834;113.83315,22.631887;113.83314,22.631811;113.833176,22.631824;113.83312,22.631844;113.83317,22.631868;113.833145,22.631868;113.83319,22.631844;113.83314,22.631824;113.83319,22.631819;113.83314,22.63189;113.833176,22.631887;113.83319,22.631811;113.83319,22.631819;113.83319,22.631844;113.833115,22.63189;113.833145,22.631844;113.833115,22.631844;113.83319,22.631819;113.83312,22.631857;113.833115,22.631844;113.83319,22.631811', recordPointDataId='75-19-D5-61-20-02-84', recordPointIdTime=1641355637000, recordPointTimeZone=8, recordPointVersion=2, recordPointTypeDescription=1, recordPointSportType=1, reportSportStartTime=1641355637000, reportSportEndTime=1641355700000, reportDuration=61, reportDistance=87, reportCal=1, reportFastPace=697, reportSlowestPace=714, reportFastSpeed=5.16, reportTotalStep=0, reportMaxStepSpeed=0, reportAvgHeart=77, reportMaxHeart=91, reportMinHeart=57, reportCumulativeRise=0.0, reportCumulativeDecline=0.0, reportAvgHeight=0.0, reportMaxHeight=0.0, reportMinHeight=0.0, reportHeartLimitTime=0, reportHeartAnaerobic=0, reportHeartAerobic=0, reportHeartFatBurning=0, reportHeartWarmUp=0, recordPointSportDataServer=00-00-00-00-07-00-00-00-75-19-D5-61-00-00-41-00-00-78-00-00-00-4A-00-00-8C-00-00-00-50-00-00-A0-00-00-00-4F-00-00-64-00-01-00-5A-00-00-AA-00-00-00-59-00-00-A0-00-00-00-58-00-00-14-00, recordPointSportData=[RecordPointSportData{altitude=0, time=1641355637, cal=0, step=0, heart=65, isFullKilometer=false, heightType=0, height=0.0, distance=12.0}, RecordPointSportData{altitude=0, time=1641355647, cal=0, step=0, heart=74, isFullKilometer=false, heightType=0, height=0.0, distance=14.0}, RecordPointSportData{altitude=0, time=1641355657, cal=0, step=0, heart=80, isFullKilometer=false, heightType=0, height=0.0, distance=16.0}, RecordPointSportData{altitude=0, time=1641355667, cal=0, step=0, heart=79, isFullKilometer=false, heightType=0, height=0.0, distance=10.0}, RecordPointSportData{altitude=0, time=1641355677, cal=1, step=0, heart=90, isFullKilometer=false, heightType=0, height=0.0, distance=17.0}, RecordPointSportData{altitude=0, time=1641355687, cal=0, step=0, heart=89, isFullKilometer=false, heightType=0, height=0.0, distance=16.0}, RecordPointSportData{altitude=0, time=1641355697, cal=0, step=0, heart=88, isFullKilometer=false, heightType=0, height=0.0, distance=2.0}]}
        //TODO 二次封装或本地存储
        if (listener != null) {
            listener.onSportData(data);
        }
    }

    private TestSportDataListener listener;

    public void setListener(TestSportDataListener listener) {
        this.listener = listener;
    }

    public interface TestSportDataListener {
        void onSportData(DevSportInfoBean data);
    }

}
