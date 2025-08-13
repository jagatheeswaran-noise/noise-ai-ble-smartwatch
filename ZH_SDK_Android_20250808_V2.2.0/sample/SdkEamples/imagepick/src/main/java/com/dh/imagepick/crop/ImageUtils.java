package com.dh.imagepick.crop;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.cache.disc.naming.FileNameGenerator;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.WeakMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.core.imageaware.ImageViewAware;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.nostra13.universalimageloader.utils.L;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;


public class ImageUtils {

    public static final FileNameGenerator FILE_NAME_GENERATOR = new Md5FileNameGenerator();

    private static DisplayImageOptions options;

    /**
     * 加载网络上的图片
     */
    public static void displayImage(String uri, ImageView imageView) {
        ImageLoader.getInstance().displayImage(uri, imageView, options);
    }

    public static void clearMemoryCache() {
        ImageLoader.getInstance().getMemoryCache().clear();
    }

    public static void displayImageProgress(final String url, final ImageView iv, final ProgressBar bar) {
        ImageViewAware imageViewAware = new MyImageWare(iv, true);
        DisplayImageOptions options = new DisplayImageOptions.Builder().cacheOnDisk(true).cacheInMemory(true)
                .imageScaleType(url != null && url.startsWith("file://") ? ImageScaleType.IN_SAMPLE_POWER_OF_2 : ImageScaleType.NONE)
                .build();
        ImageLoader.getInstance().displayImage(url, imageViewAware, options, new SimpleImageLoadingListener() {
            @Override
            public void onLoadingStarted(String imageUri, View view) {
                bar.setVisibility(View.VISIBLE);

            }

            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                bar.setVisibility(View.GONE);
            }
        });
    }

    public static void displayImage(String uri, ImageView imageView, int defaultResId) {
        DisplayImageOptions options = new DisplayImageOptions.Builder().cacheOnDisk(true).cacheInMemory(true)
                .considerExifParams(true).bitmapConfig(Bitmap.Config.ALPHA_8).showImageOnLoading(defaultResId)
                .showImageForEmptyUri(defaultResId).showImageOnFail(defaultResId)
                .build();
        ImageLoader.getInstance().displayImage(uri, imageView, options);
    }

    public static void displayImageNoCache(String uri, ImageView imageView, int defaultResId) {
        DisplayImageOptions options = new DisplayImageOptions.Builder().cacheOnDisk(false).cacheInMemory(false)
                .considerExifParams(true).bitmapConfig(Bitmap.Config.ARGB_8888).showImageOnLoading(defaultResId)
                .showImageForEmptyUri(defaultResId).showImageOnFail(defaultResId)
                .build();
        ImageLoader.getInstance().displayImage(uri, imageView, options);
    }

    public static void downloadImage(String url) {
        if(TextUtils.isEmpty(url)){
            return;
        }
        if (ImageLoader.getInstance().getDiskCache().get(url) != null) {
            return;
        }
        ImageLoader.getInstance().loadImage(url, new ImageLoadingListener() {
            @Override
            public void onLoadingStarted(String imageUri, View view) {
            }

            @Override
            public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
            }

            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
            }

            @Override
            public void onLoadingCancelled(String imageUri, View view) {
            }
        });
    }

    public static void downloadImage(String url, ImageLoadingListener imageLoadingListener) {
        ImageLoader.getInstance().loadImage(url, imageLoadingListener);
    }

    public static void initImageLoader(Context context) {
        options = new DisplayImageOptions.Builder().cacheOnDisk(true)
                .considerExifParams(true).bitmapConfig(Bitmap.Config.ALPHA_8)
                .build();
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(context)
                .threadPriority(Thread.NORM_PRIORITY - 2)
                .diskCacheSize(60 * 1024 * 1024)
                .threadPoolSize(3)
                .memoryCache(new WeakMemoryCache())
                .diskCacheFileNameGenerator(FILE_NAME_GENERATOR)
                .defaultDisplayImageOptions(options)
                .tasksProcessingOrder(QueueProcessingType.LIFO).build();

        L.writeDebugLogs(false);
        L.writeLogs(false);

        ImageLoader il = ImageLoader.getInstance();
        il.init(config);
    }

    public static Bitmap decoderImage(String localPath, int targetWidth, int targetHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(localPath, options);

        int actualWidth = options.outWidth;
        int actualHeight = options.outHeight;

        int desiredWidth = PhotoHandler.getResizedDimension(targetWidth,
                targetHeight, actualWidth, actualHeight);
        int desiredHeight = PhotoHandler.getResizedDimension(targetHeight,
                targetWidth, actualHeight, actualWidth);
        options.inJustDecodeBounds = false;
        options.inSampleSize = PhotoHandler.findBestSampleSize(actualWidth, actualHeight,
                desiredWidth, desiredHeight);

        try {
            return BitmapFactory.decodeStream(new FileInputStream(localPath), null, options);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }



    public static Bitmap getDiscCacheImage(String url){

        try {
            File file = ImageLoader.getInstance().getDiskCache().get(url);
            String path= file.getPath();
            return BitmapFactory.decodeFile(path);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 加载本地图片
     *
     * @param path      图片的全路径
     * @param imageView
     */
    public static void loadLocalImage(String path, ImageView imageView) {
        String uri = "file://" + path;
        displayImage(uri, imageView);
    }

    /**
     * 把颜色替换成主题色，不改变图片的透明度
     */
    public static Bitmap replaceColorPix(int themeColor, Bitmap src) {
        int width = src.getWidth();
        int height = src.getHeight();
        int[] colors = new int[width * height];
        int tc = themeColor;
        int red = Color.red(tc);
        int green = Color.green(tc);
        int blue = Color.blue(tc);
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int color = src.getPixel(i, j);
                int alpha = Color.alpha(color);
                if (alpha > 0) {
                    colors[j * width + i] = Color.argb(alpha, red, green, blue);
                }
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(colors, width, height,
                src.getConfig());
        return bitmap;
    }

    public static Bitmap replaceColorPix(Context context, int resId, int color) {
        Bitmap src = BitmapFactory.decodeResource(context
                .getResources(), resId);
        return replaceColorPixBabyModel(src, color);
    }

    public static Bitmap replaceColor(Bitmap src, int sourceColor, int targetColor) {
        int width = src.getWidth();
        int height = src.getHeight();
        int sourceRgb = sourceColor & 0x00FFFFFF;
        int[] colors = new int[width * height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int color = src.getPixel(i, j);
                int rgb = color & 0x00FFFFFF;
                if (rgb != sourceRgb) {
                    colors[j * width + i] = color;
                    continue;
                }
                int alpha = color & 0xFF000000;
                if (alpha != 0)
                    colors[j * width + i] = alpha | targetColor;
            }
        }
        return Bitmap.createBitmap(colors, width, height, src.getConfig());
    }

    public static Bitmap replaceColorPixBabyModel(Bitmap src, int babyColor) {
        int width = src.getWidth();
        int height = src.getHeight();
        int[] colors = new int[width * height];
        int tc = babyColor;
        int red = Color.red(tc);
        int green = Color.green(tc);
        int blue = Color.blue(tc);
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int color = src.getPixel(i, j);
                int alpha = Color.alpha(color);
                if (alpha > 0) {
                    colors[j * width + i] = Color.argb(alpha, red, green, blue);
                }
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(colors, width, height,
                src.getConfig());
        return bitmap;
    }


    public static Bitmap replaceColorPix2(Bitmap src, int themeColor) {
        int width = src.getWidth();
        int height = src.getHeight();
        int[] colors = new int[width * height];
        int tc = themeColor & 0x00FFFFFF;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int color = src.getPixel(i, j);
                int alpha = 255 - Color.alpha(color);
                colors[j * width + i] = alpha << 24 | tc;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(colors, width, height,
                src.getConfig());
        return bitmap;
    }

    /**
     * 把非白色的其他颜色替换成主题色
     *
     * @param src
     * @return
     */
    public static Bitmap replaceColorPixExceptWhite(Bitmap src, int themeColor) {

        try {
            int width = src.getWidth();
            int height = src.getHeight();
            int[] colors = new int[width * height];
            int tc = themeColor;
            int red = Color.red(tc);
            int green = Color.green(tc);
            int blue = Color.blue(tc);
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    int color = src.getPixel(i, j);
                    if (color == Color.WHITE) {
                        colors[j * width + i] = color;
                        continue;
                    }
                    int alpha = Color.alpha(color);
                    colors[j * width + i] = Color.argb(alpha, red, green, blue);
                }
            }

            return Bitmap.createBitmap(colors, width, height, src.getConfig());
        }catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }

    public static Bitmap replaceColorPixExceptBlack(Bitmap src, int themeColor) {
        int width = src.getWidth();
        int height = src.getHeight();
        int[] colors = new int[width * height];
        int tc = themeColor;
        int red = Color.red(tc);
        int green = Color.green(tc);
        int blue = Color.blue(tc);
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int color = src.getPixel(i, j);
                if (color == Color.BLACK) {
                    colors[j * width + i] = color;
                    continue;
                }
                int alpha = Color.alpha(color);
                colors[j * width + i] = Color.argb(alpha, red, green, blue);
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(colors, width, height,
                src.getConfig());
        return bitmap;
    }

    public static Bitmap scaleBitmap(Bitmap src, float scale) {
        int destWidth = (int) (src.getWidth() * scale);
        int destHeight = (int) (src.getHeight() * scale);
        return Bitmap.createScaledBitmap(src, destWidth, destHeight, true);
    }

    public  static Bitmap scaleWithWH(Bitmap src, double w, double h) {
        if (w == 0 || h == 0 || src == null) {
            return src;
        } else {
            int width = src.getWidth();
            int height = src.getHeight();
            Matrix matrix = new Matrix();
            matrix.postScale((float) (w / width), (float) (h / height));
            return Bitmap.createBitmap(src, 0, 0, width, height, matrix, true);
        }
    }

    public static boolean isThisBitmapCanRead(String path) {
        if (TextUtils.isEmpty(path)) {
            return false;
        }
        File file = new File(path);

        if (!file.exists()) {
            return false;
        }

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        int width = options.outWidth;
        int height = options.outHeight;
        return !(width == -1 || height == -1);

    }

    public static Bitmap mergeBitmap(Bitmap backBitmap, Bitmap frontBitmap, int theme_color){
        if (backBitmap == null || backBitmap.isRecycled()
                || frontBitmap == null || frontBitmap.isRecycled()) {
            return null;
        }
        Bitmap backBitmap_change = ImageUtils.replaceColorPixExceptWhite(backBitmap, theme_color);
        Bitmap bitmap = backBitmap_change.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bitmap);
        Rect baseRect = new Rect(0, 0, backBitmap_change.getWidth(), backBitmap_change.getHeight());
        Rect frontRect = new Rect(0, 0, frontBitmap.getWidth(), frontBitmap.getHeight());
        canvas.drawBitmap(frontBitmap, frontRect, baseRect, null);
        return bitmap;
    }
}