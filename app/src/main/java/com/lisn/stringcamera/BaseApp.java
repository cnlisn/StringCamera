package com.lisn.stringcamera;

import android.app.Application;

import com.lisn.stringcamera.Utils.CrashUtil;

/**
 * @author : lishan
 * @e-mail : cnlishan@163.com
 * @date : 2020/6/3 10:50 AM
 * @desc :
 */
public class BaseApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        CrashUtil.getInstance().init(this);
    }
}
