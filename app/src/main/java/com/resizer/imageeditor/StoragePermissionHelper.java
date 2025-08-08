package com.resizer.imageeditor;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class StoragePermissionHelper {

  public static final int REQUEST_CODE_OLD_STORAGE = 1001;
  public static final String PREF_LOG_FOLDER_URI = "log_folder_uri";

  // Request storage permission or folder access
  public static void checkAndRequestStoragePermission(final AppCompatActivity activity) {
    if (Build.VERSION.SDK_INT < 23) {
      // No runtime permission needed
      return;
    }

    if (Build.VERSION.SDK_INT >= 30) {
      // Android 11+ - Check if user has selected a folder
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
      String folderUriStr = prefs.getString(PREF_LOG_FOLDER_URI, null);
      if (folderUriStr == null) {
        showFolderPermissionDialog(activity);
      }
    } else {
      // Android 6.0 to 10 - Request legacy storage permission
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

  // Show dialog asking user to select folder
  private static void showFolderPermissionDialog(final AppCompatActivity activity) {
    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
    builder.setCustomTitle(DialogUtils.createStyledDialogTitle(activity, "Folder Access Needed"));
    builder.setMessage(
        "This app needs permission to save log files. Please select a folder to save logs.");

    builder.setPositiveButton(
        "Select Folder",
        (dialog, which) -> {
          openFolderPicker(activity);
        });

    builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

    builder.show();
  }

  // Launch folder picker
  private static void openFolderPicker(AppCompatActivity activity) {
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
    intent.addFlags(
        Intent.FLAG_GRANT_READ_URI_PERMISSION
            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
    activity.startActivityForResult(
        intent, REQUEST_CODE_OLD_STORAGE); // reusing REQUEST_CODE_OLD_STORAGE
  }

  // Handle folder picker result
  public static void handleFolderPickerResult(
      Activity activity, int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_CODE_OLD_STORAGE
        && resultCode == Activity.RESULT_OK
        && data != null) {
      Uri treeUri = data.getData();

      if (treeUri != null) {
        // Persist permission
        activity
            .getContentResolver()
            .takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        // Save URI in preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        prefs.edit().putString(PREF_LOG_FOLDER_URI, treeUri.toString()).apply();
      }
    }
  }

  // Check if permission is granted
  public static boolean isPermissionGranted(AppCompatActivity activity) {
    if (Build.VERSION.SDK_INT >= 30) {
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
      String folderUriStr = prefs.getString(PREF_LOG_FOLDER_URI, null);
      return folderUriStr != null;
    } else {
      return activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
              == PackageManager.PERMISSION_GRANTED
          && activity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
              == PackageManager.PERMISSION_GRANTED;
    }
  }
}
