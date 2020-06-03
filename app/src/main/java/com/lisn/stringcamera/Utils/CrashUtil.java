package com.lisn.stringcamera.Utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
/**
 * Author: LiShan 
 * Time: 2020/6/3  
 * Description: 
 */
public class CrashUtil implements Thread.UncaughtExceptionHandler {
    private static final String TAG = "CrashUtil";
    private String SD_CARD_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/crash";
    private Thread.UncaughtExceptionHandler mDefaultCrashHandler;
    private final Map<String, String> infos = new HashMap();
    private DateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault());
    private static CrashUtil mInstance = null;
    private Context mContext;

    public static CrashUtil getInstance() {
        Class var0 = CrashUtil.class;
        synchronized (CrashUtil.class) {
            if (null == mInstance) {
                mInstance = new CrashUtil();
            }

            return mInstance;
        }
    }

    private CrashUtil() {
    }

    public void init(Context context) {
        this.mDefaultCrashHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
        this.mContext = context;
        SD_CARD_PATH = mContext.getExternalFilesDir("").getAbsolutePath() + File.separator + "crash";
    }

    public void uncaughtException(Thread thread, Throwable ex) {
        this.handleException(ex);
        if (this.mDefaultCrashHandler != null) {
            SystemClock.sleep(500L);
            this.mDefaultCrashHandler.uncaughtException(thread, ex);
        }

    }

    private boolean handleException(Throwable ex) {
        if (ex == null) {
            return false;
        } else {
            this.collectDeviceInfo();
            this.saveCrashInfoToFile(ex);
            return true;
        }
    }

    public void collectDeviceInfo() {
        try {
            PackageManager pm = this.mContext.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(this.mContext.getPackageName(), 1);
            if (pi != null) {
                this.infos.put("App Version", pi.versionName + '_' + pi.versionCode + "\n");
                this.infos.put("OS Version", Build.VERSION.RELEASE + '_' + Build.VERSION.SDK_INT + "\n");
                this.infos.put("Device ID", Build.ID + "\n");
                this.infos.put("Device Serial", Build.SERIAL + "\n");
                this.infos.put("Manufacturer", Build.MANUFACTURER + "\n");
                this.infos.put("Model", Build.MODEL + "\n");
                this.infos.put("CPU ABI", Build.CPU_ABI + "\n");
                this.infos.put("Brand", Build.BRAND + "\n");
            }
        } catch (PackageManager.NameNotFoundException var3) {
            Log.e("CrashUtil", "an error occurred when collect package info");
            var3.printStackTrace();
        }

    }

    private String saveCrashInfoToFile(Throwable ex) {
        StringBuffer sb = new StringBuffer();
        Iterator var3 = this.infos.entrySet().iterator();

        String result;
        while (var3.hasNext()) {
            Map.Entry<String, String> entry = (Map.Entry) var3.next();
            String key = (String) entry.getKey();
            result = (String) entry.getValue();
            sb.append(key);
            sb.append(" : ");
            sb.append(result);
            sb.append("\n");
        }

        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);

        for (Throwable cause = ex.getCause(); cause != null; cause = cause.getCause()) {
            cause.printStackTrace(printWriter);
        }

        printWriter.close();
        result = writer.toString();
        sb.append("\n");
        sb.append(result);

        try {
            long currentTime = System.currentTimeMillis();
            String time = this.formatter.format(new Date(currentTime));
            String fileName = "crash_" + time + "_" + currentTime + ".txt";
            if (Environment.getExternalStorageState().equals("mounted")) {
                File dir = new File(SD_CARD_PATH);
                if (!dir.exists()) {
                    boolean s = dir.mkdirs();
                    System.out.println(s);
                }

                FileOutputStream fileOutputStream = new FileOutputStream(SD_CARD_PATH + "/" + fileName);
                fileOutputStream.write(sb.toString().getBytes());
                fileOutputStream.close();
            }

            return fileName;
        } catch (Exception var13) {
            Log.e("CrashUtil", "an error occurred while writing file...");
            var13.printStackTrace();
            return "";
        }
    }
}
