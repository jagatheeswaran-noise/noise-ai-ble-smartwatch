package com.zjw.sdkdemo.function;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.zhapp.ble.bean.DevSportInfoBean;
import com.zjw.sdkdemo.R;
import com.zjw.sdkdemo.app.MyApplication;
import com.zjw.sdkdemo.function.language.BaseActivity;
import com.zjw.sdkdemo.utils.DevSportManager;
import com.zjw.sdkdemo.utils.DeviceTools;


public class SportActivity extends BaseActivity {
    final private String TAG = SportActivity.class.getSimpleName();
    private DeviceTools mDeviceTools = MyApplication.getDeviceTools();

    TextView tvSportData;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sport);
        setTitle(getString(R.string.s14));
        initView();
        initData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initData();
    }

    void initView() {
        tvSportData = findViewById(R.id.tvSportData);
    }

    void initData() {
        DevSportManager.getInstance().setListener(new DevSportManager.TestSportDataListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onSportData(DevSportInfoBean data) {
                String old = tvSportData.getText().toString().trim();
                if (TextUtils.isEmpty(old)) {
                    tvSportData.setText(getString(R.string.s166) + "---------》\n" + data);
                } else {
                    tvSportData.setText(old + "\n---------》\n" + data);
                }
            }
        });
    }

    /*@SuppressLint("NonConstantResourceId")
    @OnClick({R.id.btnGetDevSportStatus, R.id.btnDevConnect, R.id.btnBleClose,            //辅助运动
            R.id.btnGetSportData,                                                      //获取多运动数据
            R.id.btnssSport                                                             //副屏运动
    })*/
    public void click(View view) {
        int id = view.getId();
        if (id == R.id.btnGetDevSportStatus) {
            DevSportManager.getInstance().getDevSportStatus();
        } else if (id == R.id.btnDevConnect) {
            DevSportManager.getInstance().testDevConnected();
        } else if (id == R.id.btnBleClose) {
            DevSportManager.getInstance().testBleClose();
        } else if (id == R.id.btnGetSportData) {
            DevSportManager.getInstance().getFitnessSportIdsData();
        } else if (id == R.id.btnssSport) {
            startActivity(new Intent(this, SecondaryScreenSportActivity.class));
        } else if (id == R.id.btnRingSport) {
            startActivity(new Intent(this, RingSportActivity.class));
        }
    }
}
