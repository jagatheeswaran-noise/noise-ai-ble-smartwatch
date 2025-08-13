package com.dh.imagepick.crop;

import android.widget.ImageView;

import com.nostra13.universalimageloader.core.assist.ViewScaleType;
import com.nostra13.universalimageloader.core.imageaware.ImageViewAware;


public class MyImageWare extends ImageViewAware {
    public MyImageWare(ImageView imageView) {
        super(imageView);
    }

    public MyImageWare(ImageView imageView, boolean checkActualViewSize) {
        super(imageView, checkActualViewSize);
    }

    @Override
    public ViewScaleType getScaleType() {
        return ViewScaleType.CROP;
    }
}
