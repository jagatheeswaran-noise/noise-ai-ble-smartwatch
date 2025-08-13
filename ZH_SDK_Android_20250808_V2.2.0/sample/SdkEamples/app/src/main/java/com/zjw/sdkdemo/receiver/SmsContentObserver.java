package com.zjw.sdkdemo.receiver;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.Telephony;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.blankj.utilcode.util.LogUtils;

/**
 * Created by android
 * on 2021/7/19
 */
public class SmsContentObserver extends ContentObserver {
    private static final String TAG = SmsContentObserver.class.getSimpleName();
    private final Context mContext;

    /**
     * Creates a content observer.
     *
     * @param handler The handler to run {@link #onChange} on, or null if none.
     */
    public SmsContentObserver(Context context, Handler handler) {
        super(handler);
        mContext = context;
    }

    public static final String[] PROJECT = new String[]{
            Telephony.Sms.BODY,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE,
            Telephony.Sms.STATUS,
            Telephony.Sms.SEEN
    };

    private static String lastDate = String.valueOf(System.currentTimeMillis());
    private static String lastContent = "";

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        LogUtils.d(TAG, "SmsContentObserver onChange");
        if (ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            LogUtils.d(TAG, "SmsContentObserver permission.READ_SMS != PackageManager.PERMISSION_GRANTED");
            return;
        }
        try {
            Cursor cursor = mContext.getContentResolver().query(Telephony.Sms.Inbox.CONTENT_URI, PROJECT,
                    null,
                    null, "_id desc");
            int index = 0;
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    index++;
                    String body = cursor.getString(0);
                    String address = cursor.getString(1);
                    String date = cursor.getString(2);
                    String type = cursor.getString(3);
                    String status = cursor.getString(4);
                    String seen = cursor.getString(5);
                    LogUtils.d(TAG, "接收到短信：" + index + " new sms body =" + body + " address = " + address + " date = " + date + " type = " + type + " uri = " + uri + " seen = " + seen + " status = " + status);
                } while (cursor.moveToNext());
            }
            cursor.close();
        } catch (Exception e) {
            LogUtils.d(TAG, "SmsContentObserver Exception");
            e.printStackTrace();
        }

        try {
            Cursor cur = mContext.getContentResolver().query(Telephony.Sms.Inbox.CONTENT_URI, null, null, null, null);
            if (cur.moveToFirst()) {
                do {
                    for (int j = 0; j < cur.getColumnCount(); j++) {
                        Log.i("====>", "name:" + cur.getColumnName(j) + "=" + cur.getString(j));
                    }
                } while (cur.moveToNext());
            }
            cur.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onChange(selfChange);
    }
}
