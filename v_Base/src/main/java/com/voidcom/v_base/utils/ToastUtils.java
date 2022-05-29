package com.voidcom.v_base.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.annotation.UiThread;

import com.voidcom.v_base.BuildConfig;

public final class ToastUtils {
    private static final String TAG = ToastUtils.class.getSimpleName();
    private static final Handler handler = new Handler(Looper.getMainLooper());

    private ToastUtils() {
        throw new UnsupportedOperationException();
    }

    @UiThread
    public static void showShort(Context context, String message) {
        show(context, message, Toast.LENGTH_SHORT);
    }

    public static void showShortInMainThread(Context context, String message) {
        handler.post(() -> showShort(context, message));
    }

    public static void showLongInMainThread(Context context, String message) {
        handler.post(() -> showLong(context, message));
    }

    public static void debugToast(Context context, String msg) {
        if (BuildConfig.DEBUG) {
            handler.post(() -> showShort(context, msg));
        }
    }

    @UiThread
    public static void showShort(Context context, @StringRes int message) {
        show(context, message, Toast.LENGTH_SHORT);
    }

    @UiThread
    public static void showLong(Context context, String message) {
        show(context, message, Toast.LENGTH_LONG);
    }

    @UiThread
    public static void showLong(Context context, @StringRes int message) {
        show(context, message, Toast.LENGTH_LONG);
    }

    private static void show(Context context, String message, int duration) {
        Toast.makeText(context, message, duration).show();
        KLog.d(TAG, message);
    }

    private static void show(Context context, @StringRes int messageRes, int duration) {
        Toast.makeText(context, messageRes, duration).show();
        KLog.d(TAG, context.getString(messageRes));
    }
}