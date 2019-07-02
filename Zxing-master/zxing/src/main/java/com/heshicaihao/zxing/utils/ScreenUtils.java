package com.heshicaihao.zxing.utils;

import android.app.Activity;
import android.util.DisplayMetrics;

/**
 * ScreenUtils
 * <ul>
 * <strong>Convert between dp and sp</strong>
 * </ul>
 */
public class ScreenUtils {

    private static int widthPixels;
    private static int heightPixels;

    private ScreenUtils() {
        throw new AssertionError();
    }

    public static void init(Activity activity) {
        DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        widthPixels =  dm.widthPixels;
        heightPixels =  dm.heightPixels;
    }

    public static int getScreenWidth() {
        return widthPixels;
    }

    /**
     * 获取屏幕高度
     *
     * @return
     */
    public static int getScreenHeight() {
        return heightPixels;
    }
}
