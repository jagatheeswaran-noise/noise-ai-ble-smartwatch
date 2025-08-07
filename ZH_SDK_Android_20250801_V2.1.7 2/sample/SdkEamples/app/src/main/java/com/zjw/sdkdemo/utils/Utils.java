package com.zjw.sdkdemo.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


public class    Utils {

    public static final String ASSETS_DIAL_DIR = "clock_dial_data" + File.separator;

    public static void MyLog(String tag, String msg) {
        System.out.println("MyLog:" + tag + "->" + msg);
    }

    public static String GetFormat(int length, float value) {

        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(length);
        df.setGroupingSize(0);
        df.setRoundingMode(RoundingMode.FLOOR);
        return df.format(value);
    }

    public static SharedPreferences getSharedPreferences(Context context) {
        if (context != null) {
            SharedPreferences sp = context.getSharedPreferences("DEMO_SP", Context.MODE_PRIVATE);
            return sp;
        }
        return null;
    }


    public static String GetDistanceFormatMetric(float value) {

        float result = (float) (int) (value * 100) / 100;

        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        String distanceString = decimalFormat.format(result);

        return distanceString;

    }

    public static String GetDistanceFormatBritish(float value) {

        float result = (float) (int) (value / 1.61f * 100) / 100;

        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        String distanceString = decimalFormat.format(result);

        return distanceString;

    }


    /**
     * 获取年月日
     *
     * @return
     */
    public static String getToDayDate() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        return format.format(date);
    }


    /**
     * 获取时分
     *
     * @return
     */
    public static String getTime() {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm");
        Date date = new Date();
        return format.format(date);
    }


    /**
     * 获取完整时间
     *
     * @return
     */
    public static String getAllTime() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        return format.format(date);
    }


    public static int getColor(int r, int g, int b) {

        if (r < 0) {
            r = 0;
        }
        if (r > 255) {
            r = 255;
        }

        if (g < 0) {
            g = 0;
        }
        if (g > 255) {
            g = 255;
        }

        if (b < 0) {
            b = 0;
        }
        if (b > 255) {
            b = 255;
        }

        return Color.argb(255, r, g, b);
    }


    //Uri 转 Bitmap
    public static Bitmap decodeUriAsBitmap(Context context, Uri uri) {
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeStream(context.getContentResolver().openInputStream(uri));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        return bitmap;
    }

    public static String getProgress(float Progress) {

        String str = String.valueOf(Progress);

        if (Progress > 10) {
            if (str.length() >= 5) {
                str = str.substring(0, 5);
            }
        } else {
            if (str.length() >= 4) {
                str = str.substring(0, 4);
            }
        }

        return str;
    }

    public static Typeface modefyEscape(Context context) {
        Typeface face = Typeface.createFromAsset(context.getAssets(), "fonts/Escape.ttf");
        return face;
    }

    public static String item_language_str[] = {
            "values_en",
            "values_zh_rCN",
            "values_zh_rHK",
            "values_zh_rTW",
            "values_ja",
            "values_fr",
            "values_de",
            "values_it",
            "values_es",
            "values_ru",
            "values_pt",
            "values_ms",
            "values_ko",
            "values_pl",
            "values_th",
            "values_ro",
            "values_bg",
            "values_hu",
            "values_tr",
            "values_cs",
            "values_sk",
            "values_da",
            "values_nb",
            "values_sv",
            "values_tl",
            "values_uk"};


    public static String item_notiface_str[] = {
            "Phone",
            "QQ",
            "WeChat",
            "Msg",
            "Skype",
            "Whatsapp",
            "Facebook",
            "Link",
            "Twitter",
            "Viber",
            "Line",

            "Gmail",
            "OutLook",
            "Instagram",
            "Snapchat",
            "FacebookMsg",

            "TelegramMsg"
    };


    public static String item_sport_target[] = {
            "1000",
            "2000",
            "3000",
            "4000",
            "5000",
            "6000",
            "7000",
            "8000",
            "9000",
            "10000",
            "11000",
            "12000",
            "13000",
            "14000"};

    public static String getDate(int y, int m, int d) {

        String year = String.valueOf(y);
        String mon = String.valueOf(m);
        String day = String.valueOf(d);

        if (m < 10) {
            mon = "0" + mon;
        }

        if (d < 10) {
            day = "0" + day;
        }

        return year + "-" + mon + "-" + day;

    }

    public static String getNewTime(String s, int n) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

            Calendar cd = Calendar.getInstance();
            cd.setTime(sdf.parse(s));
            cd.add(Calendar.DATE, n);
            return sdf.format(cd.getTime());

        } catch (Exception e) {
            return null;
        }

    }


    public static byte[] getAssetBytes(Context context, String fileName) {
        byte[] buffer = null;
        try {
            InputStream is = context.getAssets().open(fileName);
            int size = is.available();
            buffer = new byte[size];
            is.read(buffer);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buffer;
    }

    public static String getPercentageStr(int Max, int Num) {
        float proportion = ((float) (Num) / (float) Max) * 100;
        DecimalFormat df = new DecimalFormat("######00.00");
        return df.format(proportion);

    }

    public static byte[] getBytesByAssets(Context context, String name) {
        byte[] buffer = null;
        try {
            InputStream fis = context.getAssets().open(name);
            ByteArrayOutputStream bos = new ByteArrayOutputStream(1000);
            byte[] b = new byte[1000];
            int n;
            while ((n = fis.read(b)) != -1) {
                bos.write(b, 0, n);
            }
            fis.close();
            bos.close();
            buffer = bos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return buffer;
    }

    public static String item_third_party_push_text_list[] = {
            "QQ",
            "WeChat",
            "Skype",
            "Whatsapp",
            "Facebook message",
            "Link",
            "Twitter",
            "Viber",
            "Line",
            "Gmail",
            "OutLook",
            "Instagram",
            "Snapchat",
            "Facebook",
            "Telegram",
            "zalo",
            "youtube",
            "kao kao talk",
            "talk vk",
            "ok",
            "icq",
            "Calendar",
            "other"
    };


    /**
     * 保留小数点后台两位 Keep the decimal point with two digits in the background
     *
     * @param number
     * @return
     */
    public static String bigDecimalFormat(float number) {
        return new BigDecimal(String.valueOf(number)).setScale(2, BigDecimal.ROUND_DOWN).toString();
    }

    /**
     * 平均配速 Average pace
     *
     * @param unit     单位 unit 0=Imperial，1=Metric
     * @param distance 距离-单位(m) Distance-unit (m)
     * @param duration 时长(s) Duration (s)
     * @return 00'00"
     */
    public static String avgPace(int unit, long distance, long duration) {
        String avgPaceString;
        float avgPace = 0.0f;
        if (distance != 0L) {
            avgPace = duration / ((distance / 1000.0f));
        }
        int minute = (int) (avgPace / 60);
        int second = (int) (avgPace % 60);
        if (isShow00Pace(minute, second)) {
            avgPaceString = String.format(Locale.ENGLISH, "%1$02d'%2$02d\"", 0, 0);
        } else {
            if (unit != 1) {
                avgPace = duration / ((distance / 1000.0f / 1.61f));
            }
            avgPaceString = String.format(Locale.ENGLISH, "%1$02d'%2$02d\"", (int) (avgPace / 60), (int) (avgPace % 60));
        }
        return avgPaceString;
    }


    /**
     * 配速是否合法 Is the pace legal
     *
     * @param minute 分钟 minute
     * @param second 秒 second
     * @return
     */
    public static boolean isShow00Pace(int minute, int second) {
        int totalSecond = minute * 60 + second;
        return totalSecond > (50 * 60 + 58) || totalSecond <= 0;
    }

    /**
     * 平均速度 Average speed
     *
     * @param unit     单位 unit 0=Imperial，1=Metric
     * @param distance 距离-单位(m) Distance-unit (m)
     * @param duration 时长(s) Duration (s)
     * @return 0.00 km/h
     */
    public static String averageSpeed(int unit, long distance, long duration) {
        if (unit == 1) {
            return bigDecimalFormat(((distance / 1000f) / (duration / 3600f)));
        } else {
            return bigDecimalFormat(((distance / 1.61f / 1000.0f) / (duration / 3600.0f)));
        }
    }

    /**
     * 米转换英尺 Meters to feet
     *
     * @param distance (m)
     * @return
     */
    public static float metersToFeet(long distance) {
        return distance * 3.28f;
    }


    /**
     * 米转换英寸 Meters to inches
     *
     * @param distance (m)
     * @return
     */
    public static float metersToInches(long distance) {
        return distance * 100 * 0.393f;
    }


    /**
     * 米转换千米 meters to Kilometer
     *
     * @param distance (m)
     * @return
     */
    public static float metersToKilometer(long distance) {
        return distance / 1000.0f;
    }

    /**
     * 米转换英里 meters to miles
     *
     * @param distance (m)
     * @return
     */
    public static float metersToMiles(long distance) {
        return distance / 1000.0f / 1.61f;
    }

    /**
     * 公里转换英里 Kilometers to miles
     *
     * @param distance (km)
     * @return
     */
    public static float kilometersToMiles(float distance) {
        return distance / 1.61f;
    }

    public static String item_notiface_str_n[] = {
            "QQ",
            "WeChat",
            "Skype",
            "Whatsapp",
            "Facebook",
            "Link",
            "Twitter",
            "Viber",
            "Line",
            "Gmail",
            "OutLook",
            "Instagram",
            "Snapchat",
            "other",
            "FacebookMessenger",
            "Telegram",
            "Calendar"
    };

    /**
     * 缩放图片
     *
     * @param bm
     * @param newWidth
     * @param newHeight
     * @return
     */
    public static Bitmap zoomImg(Bitmap bm, int newWidth, int newHeight) {
        return Bitmap.createScaledBitmap(bm, newWidth, newHeight, true);
    }


    /**
     * 合并两张bitmap为一张
     *
     * @param background
     * @param foreground
     * @return Bitmap
     */
    public static Bitmap combineBitmap(Bitmap background, Bitmap foreground, int x, int y) {
        if (background == null) {
            return null;
        }
        int bgWidth = background.getWidth();
        int bgHeight = background.getHeight();
        Bitmap newmap = Bitmap.createBitmap(bgWidth, bgHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(newmap);
        canvas.drawBitmap(background, 0, 0, null);
        canvas.drawBitmap(foreground, x, y, null);
        canvas.save();
        canvas.restore();
        return newmap;
    }
}
