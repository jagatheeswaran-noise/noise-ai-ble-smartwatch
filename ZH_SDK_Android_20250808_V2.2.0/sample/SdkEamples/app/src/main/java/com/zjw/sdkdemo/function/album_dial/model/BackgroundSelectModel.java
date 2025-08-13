package com.zjw.sdkdemo.function.album_dial.model;

import android.graphics.Bitmap;

public class BackgroundSelectModel {
    private Bitmap bitmap;
    private int resId;
    private boolean isSelected;

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public int getResId() {
        return resId;
    }

    public void setResId(int resId) {
        this.resId = resId;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
