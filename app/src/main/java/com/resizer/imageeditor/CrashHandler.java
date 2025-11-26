package com.resizer.imageeditor;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Point;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import java.io.*;
import java.lang.reflect.Field;
import java.text.*;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class CrashHandler {

  private static CrashHandler sInstance;
  private static final Thread.UncaughtExceptionHandler DEFAULT_HANDLER =
      Thread.getDefaultUncaughtExceptionHandler();

  public static CrashHandler getInstance() {
    if (sInstance == null) sInstance = new CrashHandler();
    return sInstance;
  }

  public void registerGlobal(Context context) {
    registerGlobal(context, null);
  }

  public void registerGlobal(Context context, String crashDir) {
    Thread.setDefaultUncaughtExceptionHandler(
        new UncaughtExceptionHandlerImpl(context.getApplicationContext(), crashDir));
  }

  public void unregister() {
    Thread.setDefaultUncaughtExceptionHandler(DEFAULT_HANDLER);
  }

  private static class UncaughtExceptionHandlerImpl implements Thread.UncaughtExceptionHandler {

    private static final DateFormat DATE_FORMAT =
        new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss", Locale.US);
    private final Context context;
    private final File crashDir;

    UncaughtExceptionHandlerImpl(Context context, String crashDir) {
      this.context = context;
      this.crashDir =
          TextUtils.isEmpty(crashDir)
              ? new File(context.getExternalCacheDir(), "crash")
              : new File(crashDir);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
      try {
        String log = buildLog(throwable);
        writeLog(log);

        // Launch crash display activity
        Intent intent = new Intent(context, CrashActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(Intent.EXTRA_TEXT, log);
        context.startActivity(intent);

        throwable.printStackTrace();
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);

      } catch (Throwable e) {
        if (DEFAULT_HANDLER != null) DEFAULT_HANDLER.uncaughtException(thread, throwable);
      }
    }

    private String buildLog(Throwable throwable) {
      String time = GetTimeStamp(0, 0);

      // App info
      String versionName = "unknown";
      long versionCode = 0;
      try {
        PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        versionName = info.versionName;
        versionCode = Build.VERSION.SDK_INT >= 28 ? info.getLongVersionCode() : info.versionCode;
      } catch (Exception ignored) {
      }

      // Header info
      LinkedHashMap<String, String> head = new LinkedHashMap<>();
      head.put("Time Of Crash", time);
      head.put("Device", Build.MANUFACTURER + " " + Build.MODEL);
      head.put("Android Version", Build.VERSION.RELEASE + " (" + Build.VERSION.SDK_INT + ")");
      head.put("App Version", versionName + " (" + versionCode + ")");
      head.put("Resolution", getScreenResolution(context));
      head.put("System Language", Locale.getDefault().getDisplayLanguage());
      head.put("Total RAM", getTotalRAM());

      // Build log
      StringBuilder builder = new StringBuilder();
      builder.append("********** CRASH REPORT **********\n\n");
      for (Map.Entry<String, String> entry : head.entrySet()) {
        builder.append(entry.getKey()).append(" : ").append(entry.getValue()).append("\n");
      }

      builder.append("\n********** DEVICE INFO **********\n\n");

      builder
          .append(BuildPropHelper.getBuildPropInfo("android.os.Build"))
          .append(BuildPropHelper.getBuildPropInfo("android.os.Build$VERSION"))
          .append("\n");

      builder.append("\n********** STACK TRACE **********\n\n");
      builder.append(Log.getStackTraceString(throwable));
      builder.append("\n--------------------- THE END ---------------------\n");
      return builder.toString();
    }

    private void writeLog(String log) {
      try {
        if (!crashDir.exists()) crashDir.mkdirs();
        String time = DATE_FORMAT.format(new Date());
        File file = new File(crashDir, "crash_" + time + ".txt");
        try (FileOutputStream out = new FileOutputStream(file)) {
          out.write(log.getBytes("UTF-8"));
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    // ===== Extra Info Helpers =====

    private static String getScreenResolution(Context context) {
      try {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point point = new Point();
        display.getRealSize(point);
        return point.x + "x" + point.y;
      } catch (Exception e) {
        return "unknown";
      }
    }

    private static String getTotalRAM() {
      try {
        RandomAccessFile reader = new RandomAccessFile("/proc/meminfo", "r");
        String load = reader.readLine();
        reader.close();

        String[] parts = load.split("\\s+");
        double totalKb = Double.parseDouble(parts[1]);
        double totalMb = totalKb / 1024.0;
        double totalGb = totalMb / 1024.0;
        DecimalFormat df = new DecimalFormat("#.##");

        if (totalGb >= 1) return df.format(totalGb) + " GB";
        else if (totalMb >= 1) return df.format(totalMb) + " MB";
        else return df.format(totalKb) + " KB";
      } catch (Exception e) {
        return "unknown";
      }
    }

    private static String getKernel() {
      try (BufferedReader br = new BufferedReader(new FileReader("/proc/version"))) {
        return br.readLine();
      } catch (Exception e) {
        return e.getMessage();
      }
    }

    private static void collectDeviceInfo(Map<String, String> map, String className) {
      try {
        Class<?> cls = Class.forName(className);
        for (Field field : cls.getDeclaredFields()) {
          field.setAccessible(true);
          Object value = field.get(null);
          map.put(field.getName(), String.valueOf(value));
        }
      } catch (Exception e) {
        map.put("Error", "Failed to collect from " + className + ": " + e.getMessage());
      }
    }
  }

  public static String GetTimeStamp(int i, int i2) {
    return DateFormat.getDateTimeInstance(i, i2, Locale.ROOT).format(new Date());
  }
}
