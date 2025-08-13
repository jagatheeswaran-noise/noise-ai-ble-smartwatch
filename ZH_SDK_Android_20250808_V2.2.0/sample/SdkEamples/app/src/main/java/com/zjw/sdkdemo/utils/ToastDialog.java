package com.zjw.sdkdemo.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.zjw.sdkdemo.R;

/**
 * Created by Android on 2021/10/21.
 */
public class ToastDialog {
    public static Dialog showToast(Activity activity, String msg){
        Dialog dialog = new Dialog(activity);
        dialog.setCancelable(true);
        dialog.setContentView(activity.getLayoutInflater().inflate(R.layout.dialog_toast,null));
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = activity.getWindowManager().getDefaultDisplay().getWidth();
        params.height = activity.getWindowManager().getDefaultDisplay().getHeight();
        dialog.getWindow().setAttributes(params);
        TextView tvMsg = dialog.findViewById(R.id.tvMsg);
        tvMsg.setText(msg);
        dialog.findViewById(R.id.dialogLayout).setOnClickListener(v -> {
            dialog.dismiss();
        });
        if(!activity.isDestroyed()) {
            dialog.show();
        }
        return dialog;
    }
}
