package com.zjw.sdkdemo.function;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.lifecycle.Observer;

import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.LogUtils;
import com.zhapp.ble.ControlBleTools;
import com.zhapp.ble.bean.WidgetBean;
import com.zhapp.ble.parsing.ParsingStateManager;
import com.zhapp.ble.parsing.SendCmdState;
import com.zjw.sdkdemo.R;
import com.zjw.sdkdemo.app.MyApplication;
import com.zjw.sdkdemo.function.language.BaseActivity;
import com.zjw.sdkdemo.livedata.DeviceSettingLiveData;
import com.zjw.sdkdemo.utils.ToastDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Android on 2022/1/4.
 */
public class MicroActivity extends BaseActivity {
    final private String TAG = MicroActivity.class.getSimpleName();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_micro);
        setTitle(getString(R.string.s217));
        initView();
        initData();
    }

    private void initView() {
        etWidget = findViewById(R.id.etWidget);
    }

    private void initData() {
        /**
         * 获取应用列表
         */
        DeviceSettingLiveData.getInstance().getApplicationList().observe(this, new Observer<List<WidgetBean>>() {
            @Override
            public void onChanged(List<WidgetBean> widgetBeans) {
                if (widgetBeans != null) {
                    Log.i(TAG, "getApplicationList == " + widgetBeans);
                    ToastDialog.showToast(MicroActivity.this, getString(R.string.s253) + "\n" + widgetBeans);
                }
            }
        });

        /**
         * 获取首页卡片
         */
        DeviceSettingLiveData.getInstance().getWidgetList().observe(this, new Observer<List<WidgetBean>>() {
            @Override
            public void onChanged(List<WidgetBean> widgetBeans) {
                if (widgetBeans != null) {
                    Log.i(TAG, "getWidgetList == " + widgetBeans);
                    ToastDialog.showToast(MicroActivity.this, getString(R.string.s218) + widgetBeans);
                }
            }
        });

        DeviceSettingLiveData.getInstance().getSportWidgetList().observe(this, new Observer<List<WidgetBean>>() {
            @Override
            public void onChanged(List<WidgetBean> widgetBeans) {
                if (widgetBeans != null) {
                    Log.i(TAG, "getSportWidgetList == " + widgetBeans);
                    ToastDialog.showToast(MicroActivity.this, getString(R.string.s297) + widgetBeans);
                }
            }
        });

        DeviceSettingLiveData.getInstance().getRingNFCSleepError().observe(this, new Observer<Integer>() {

            @Override
            public void onChanged(Integer integer) {
                ToastDialog.showToast(MicroActivity.this, getString(R.string.s748) + integer);
            }
        });
    }


    EditText etWidget;

    /*@OnClick({R.id.btnFindWear,                                 //找手环
            R.id.btnEnterPhotogragh, R.id.btnExitPhotogragh,    //摇摇拍照
            R.id.btnGetApplication, R.id.btnSetApplication,     //设备应用列表
            R.id.btnGetWidget, R.id.btnSetWidget,               //设备首页卡片
            R.id.btnGetSportWidget, R.id.btnSetSportWidget       //设备运动排序
    })*/
    public void click(View view) {
        int id = view.getId();
        if (id == R.id.btnFindWear) {
            ControlBleTools.getInstance().sendFindWear(new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
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
        } else if (id == R.id.btnEnterPhotogragh) {
            ControlBleTools.getInstance().sendPhonePhotogragh(0, new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
                @Override
                public void onState(SendCmdState state) {
                    switch (state) {
                        case SUCCEED:
                            Log.i(TAG, getString(R.string.s220));
                            break;
                        default:
                            Log.i(TAG, getString(R.string.s221));
                            break;
                    }
                }
            });
        } else if (id == R.id.btnExitPhotogragh) {
            ControlBleTools.getInstance().sendPhonePhotogragh(1, new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
                @Override
                public void onState(SendCmdState state) {
                    switch (state) {
                        case SUCCEED:
                            Log.i(TAG, getString(R.string.s220));
                            break;
                        default:
                            Log.i(TAG, getString(R.string.s221));
                            break;
                    }
                }
            });
        } else if (id == R.id.btnGetApplication) {
            ControlBleTools.getInstance().getApplicationList(new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
                @Override
                public void onState(SendCmdState state) {
                    switch (state) {
                        case SUCCEED:
                            Log.i(TAG, getString(R.string.s220));
                            break;
                        default:
                            Log.i(TAG, getString(R.string.s221));
                            break;
                    }
                }
            });
        } else if (id == R.id.btnSetApplication) {
            ArrayList<WidgetBean> apps = (ArrayList<WidgetBean>) DeviceSettingLiveData.getInstance().getApplicationList().getValue();
            if (apps == null) {
                Toast.makeText(MicroActivity.this, getString(R.string.s219), Toast.LENGTH_LONG).show();
                return;
            }
            //重新排序应用列表
            //3,4交换
            if (!apps.get(2).sortable || !apps.get(3).sortable) {
                Toast.makeText(MicroActivity.this, getString(R.string.s222), Toast.LENGTH_LONG).show();
                return;
            }
            Collections.swap(apps, 2, 3);
            //重新赋值排序字段
            for (int i = 0; i < apps.size(); i++) {
                apps.get(i).order = i + 1;
            }
            LogUtils.d("setApplicationList:" + GsonUtils.toJson(apps));
            ControlBleTools.getInstance().setApplicationList(apps, new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
                @Override
                public void onState(SendCmdState state) {
                    switch (state) {
                        case SUCCEED:
                            Log.i(TAG, getString(R.string.s220));
                            break;
                        default:
                            Log.i(TAG, getString(R.string.s221));
                            break;
                    }
                }
            });
        } else if (id == R.id.btnGetWidget) {
            ControlBleTools.getInstance().getWidgetList(new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
                @Override
                public void onState(SendCmdState state) {
                    switch (state) {
                        case SUCCEED:
                            Log.i(TAG, getString(R.string.s220));
                            break;
                        default:
                            Log.i(TAG, getString(R.string.s221));
                            break;
                    }
                }
            });
        } else if (id == R.id.btnSetWidget) {
            ArrayList<WidgetBean> widgets = (ArrayList<WidgetBean>) DeviceSettingLiveData.getInstance().getWidgetList().getValue();
            if (widgets == null) {
                Toast.makeText(MicroActivity.this, getString(R.string.s219), Toast.LENGTH_LONG).show();
                return;
            }
            //重新排序直达卡片顺序
            String ws = etWidget.getText().toString().trim();
            if (TextUtils.isEmpty(ws)) {
                //3,4交换
                if (!widgets.get(2).sortable || !widgets.get(3).sortable) {
                    Toast.makeText(MicroActivity.this, getString(R.string.s222), Toast.LENGTH_LONG).show();
                    return;
                }
                Collections.swap(widgets, 2, 3);
                //重新赋值排序字段
                for (int i = 0; i < widgets.size(); i++) {
                    widgets.get(i).order = i + 1;
                }
            } else {
                try {
                    if (ws.contains(",")) {
                        String[] ids = ws.split(",");
                        widgets = new ArrayList<>();
                        for (int i = 0; i < ids.length; i++) {
                            WidgetBean widgetBean = new WidgetBean();
                            widgetBean.functionId = Integer.parseInt(ids[i]);
                            widgetBean.order = i + 1;
                            widgetBean.isEnable = i != ids.length - 1;
                            widgetBean.haveHide = true;
                            widgetBean.sortable = true;
                            widgets.add(widgetBean);
                        }
                    } else {
                        Toast.makeText(MicroActivity.this, getString(R.string.s238), Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(MicroActivity.this, getString(R.string.s238), Toast.LENGTH_LONG).show();
                }
            }
            LogUtils.d("setWidgetList:" + GsonUtils.toJson(widgets));
            ControlBleTools.getInstance().setWidgetList(widgets, new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
                @Override
                public void onState(SendCmdState state) {
                    switch (state) {
                        case SUCCEED:
                            Log.i(TAG, getString(R.string.s220));
                            break;
                        default:
                            Log.i(TAG, getString(R.string.s221));
                            break;
                    }
                }
            });
        } else if (id == R.id.btnGetSportWidget) {
            ControlBleTools.getInstance().getSportWidgetSortList(new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
                @Override
                public void onState(SendCmdState state) {
                    switch (state) {
                        case SUCCEED:
                            Log.i(TAG, getString(R.string.s220));
                            break;
                        default:
                            Log.i(TAG, getString(R.string.s221));
                            break;
                    }
                }
            });
        } else if (id == R.id.btnSetSportWidget) {
            ArrayList<WidgetBean> widgets = (ArrayList<WidgetBean>) DeviceSettingLiveData.getInstance().getSportWidgetList().getValue();
            if (widgets == null) {
                Toast.makeText(MicroActivity.this, getString(R.string.s219), Toast.LENGTH_LONG).show();
                return;
            }
            LogUtils.d(TAG, "Get device Data: \n" + GsonUtils.toJson(widgets));

            //重新排序运动
            //3,4交换
            if (!widgets.get(2).sortable || !widgets.get(3).sortable) {
                Toast.makeText(MicroActivity.this, getString(R.string.s222), Toast.LENGTH_LONG).show();
                return;
            }
            Collections.swap(widgets, 2, 3);
            //重新赋值排序字段
            for (int i = 0; i < widgets.size(); i++) {
                widgets.get(i).order = i + 1;
                //widgets.get(i).isEnable = false (禁用 list启用最少1个最大10个) (disable list enable min 1 max 10)
            }
            LogUtils.d(TAG, "Set device Data: \n" + GsonUtils.toJson(widgets));
            ControlBleTools.getInstance().setSportWidgetSortList(widgets, new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
                @Override
                public void onState(SendCmdState state) {
                    switch (state) {
                        case SUCCEED:
                            Log.i(TAG, getString(R.string.s220));
                            break;
                        default:
                            Log.i(TAG, getString(R.string.s221));
                            break;
                    }
                }
            });
        } else if (id == R.id.btnGetRingSleepErr) {
            ControlBleTools.getInstance().getRingNFCSleepErr(new ParsingStateManager.SendCmdStateListener(getLifecycle()) {
                @Override
                public void onState(SendCmdState state) {
                    switch (state) {
                        case SUCCEED:
                            Log.i(TAG, getString(R.string.s220));
                            break;
                        default:
                            Log.i(TAG, getString(R.string.s221));
                            break;
                    }
                }
            });
        }
    }
}
