package com.resizer.imageeditor;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.lang.reflect.Method;

public class StoragePermissionHelper {

  public static final int REQUEST_CODE_OLD_STORAGE = 1001;
  public static final int REQUEST_CODE_MANAGE_STORAGE = 1002;

  public static void checkAndRequestStoragePermission(final AppCompatActivity activity) {
    if (Build.VERSION.SDK_INT < 23) {
      // No runtime permission needed
      return;
    }

    if (Build.VERSION.SDK_INT >= 30) { // Android 11+
      if (!isExternalStorageManagerCompat()) {
        showPermissionDialog(activity);
      }
    } else {
      // Android 6.0 to 10
      if (activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
              != PackageManager.PERMISSION_GRANTED
          || activity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
              != PackageManager.PERMISSION_GRANTED) {

        activity.requestPermissions(
            new String[] {
              Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
            },
            REQUEST_CODE_OLD_STORAGE);
      }
    }
  }

  private static void showPermissionDialog(final AppCompatActivity activity) {
    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
    //  builder.setTitle("Storage Permission Needed");
    builder.setCustomTitle(
        DialogUtils.createStyledDialogTitle(activity, "Storage Permission Needed"));

    builder.setMessage("This app needs permission to access all files. Please grant permission.");

    builder.setPositiveButton("OK", (dialog, which) -> openManageAllFilesPermission(activity));
    builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

    builder.show();
  }

  private static void openManageAllFilesPermission(AppCompatActivity activity) {
    Intent intent;
    try {
      intent = new Intent("android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION");
      intent.setData(Uri.parse("package:" + activity.getPackageName()));
      activity.startActivityForResult(intent, REQUEST_CODE_MANAGE_STORAGE);
    } catch (Exception e) {
      // Fallback for some devices
      try {
        intent = new Intent("android.settings.MANAGE_ALL_FILES_ACCESS_PERMISSION");
        activity.startActivityForResult(intent, REQUEST_CODE_MANAGE_STORAGE);
      } catch (Exception ex) {
        // Handle if both fail
      }
    }
  }

  public static boolean isExternalStorageManagerCompat() {
    if (Build.VERSION.SDK_INT >= 30) {
      try {
        Class envClass = Class.forName("android.os.Environment");
        Method method = envClass.getMethod("isExternalStorageManager");
        Object result = method.invoke(null);
        return result instanceof Boolean && (Boolean) result;
      } catch (Exception e) {
        return false;
      }
    } else {
      return false;
    }
  }

  public static boolean isPermissionGranted(AppCompatActivity activity) {
    if (Build.VERSION.SDK_INT >= 30) {
      return isExternalStorageManagerCompat();
    } else {
      return activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
              == PackageManager.PERMISSION_GRANTED
          && activity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
              == PackageManager.PERMISSION_GRANTED;
    }
  }
}
