package com.dh.imagepick.crop;

import android.graphics.Color;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.gyf.immersionbar.ImmersionBar;

public class ImageBaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ImmersionBar.with(this)
                .statusBarColorInt( Color.parseColor("#101016"))
                .statusBarDarkFont(false, 0.2f)
                .init();
        //解决没有toolbar时布局和状态栏重叠问题
        ImmersionBar.with(this).fitsSystemWindows(true).init();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        ImagePicker.getInstance().restoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        ImagePicker.getInstance().saveInstanceState(outState);
    }
}
