package com.zjw.sdkdemo.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SaveLog {
    public static String FileName = "a01";
    private static final String LOG_TAG = "a_ble_heart";
    private static File dir = null;
    public static final String APP_NAME = "com.zjw.sdkdemo";
    public static final String HOME_DIR = Environment.getExternalStorageDirectory().getPath() + File.separator + APP_NAME;
    public static final String DEVICE_ERROR_LOG_FILE = HOME_DIR + File.separator;

    public static void setFileName(String fileName) {
        FileName = fileName;
    }


    public static void init(Context context) {
//        if (makeRootDirectory(HOME_DIR)){
//            boolean sdCardExist = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
            try {
//                if (sdCardExist) {
//                    dir = new File(DEVICE_ERROR_LOG_FILE
//                            + "/"
//                            + FileName + ".txt"
//                    );
                    dir = new File(context.getExternalFilesDir("log"),
                            FileName + ".txt"
                    );
//	            		+"ecg_test.log");
                    if (!dir.exists()) {
                        dir.createNewFile();
                    }
//                }
            } catch (Exception e) {
                e.printStackTrace();
            }
//        }
    }


    private static boolean makeRootDirectory(String filePath) {
        File file = null;
        try {
            file = new File(filePath);
            if (!file.exists()) {
                boolean isSuccess = file.mkdirs();
                Log.i("makeRootDirectory", "isSuccess = " + isSuccess);
                return isSuccess;
            } else if (!file.isDirectory()) {
                boolean isde = file.delete();
                boolean isSuccess = file.mkdirs();
                Log.i("makeRootDirectory ", "isSuccess = " + isSuccess);
                return isSuccess;
            }
        } catch (Exception e) {
            Log.i("makeRootDirectory:", e + "");
        }
        return false;
    }


    public static void writeFile(String msg) {
        String result = InputTime() + " -----> " + msg;
        try {
            if (dir != null) {
                FileOutputStream fos = new FileOutputStream(dir, true);
                result = result + "\r\n";
                byte[] bytes = result.getBytes();
                fos.write(bytes);
                fos.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String InputTime() {
        // TODO Auto-generated method stub
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss:SSSS");
//		SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm:ss:SSSS");
        return dateFormat.format(date);
    }

    public static String createFileName(){
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
//		SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm:ss:SSSS");
        return dateFormat.format(date);
    }
}
