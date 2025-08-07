package com.zjw.sdkdemo.utils;

import android.app.Activity;
import android.app.Dialog;
import android.widget.TextView;

import com.zjw.sdkdemo.R;

/**
 * Created by Android on 2022/5/26.
 */
public class LoadingDialog {
    public static Dialog show(Activity activity){
        Dialog dialog = new Dialog(activity);
        dialog.setCancelable(false);
        dialog.setContentView(activity.getLayoutInflater().inflate(R.layout.dialog_loading,null));
        /*WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = activity.getWindowManager().getDefaultDisplay().getWidth();
        params.height = activity.getWindowManager().getDefaultDisplay().getHeight();
        dialog.getWindow().setAttributes(params);*/
        //TextView tvMsg = dialog.findViewById(R.id.tvMsg);
        //tvMsg.setText(msg);
        /*dialog.findViewById(R.id.dialogLayout).setOnClickListener(v -> {
            dialog.dismiss();
        });*/
        if(!activity.isDestroyed()) {
            dialog.show();
        }
        return dialog;
    }
}
