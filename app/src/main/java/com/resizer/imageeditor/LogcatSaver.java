package com.resizer.imageeditor;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import androidx.documentfile.provider.DocumentFile;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogcatSaver {

  private static final String TAG = "LogcatSaver";

  /**
   * Start saving logcat data to file. Uses SAF on Android 11+, legacy storage for older versions.
   */
  public static void RunLog(Context context) {
    if (Build.VERSION.SDK_INT >= 30) {
      // Android 11+ (Scoped Storage)
      saveLogToScopedStorage(context);
    } else {
      // Android < 11 (Legacy external storage)
      saveLogToLegacyStorage();
    }
  }

  /** Scoped Storage: Write logs to user-selected folder */
  private static void saveLogToScopedStorage(Context context) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    String folderUriStr = prefs.getString(StoragePermissionHelper.PREF_LOG_FOLDER_URI, null);

    if (folderUriStr == null) {
      Log.e(TAG, "Log folder not selected. Please select a folder first.");
      return;
    }

    Uri folderUri = Uri.parse(folderUriStr);
    DocumentFile folder = DocumentFile.fromTreeUri(context, folderUri);

    if (folder == null || !folder.canWrite()) {
      Log.e(TAG, "Cannot write to the selected folder.");
      return;
    }

    String fileName =
        "Encryptor_Logcat_"
            + Build.MANUFACTURER
            + "_"
            + Build.MODEL
            + "("
            + Build.DEVICE
            + ")"
            + ".json";

    // Delete existing file if it exists
    DocumentFile existingFile = folder.findFile(fileName);
    if (existingFile != null) {
      existingFile.delete();
    }

    // Create a new file
    DocumentFile logFile = folder.createFile("application/json", fileName);
    if (logFile == null) {
      Log.e(TAG, "Failed to create log file.");
      return;
    }

    Uri fileUri = logFile.getUri();
    try {
      OutputStream outputStream =
          context.getContentResolver().openOutputStream(fileUri, "w"); // overwrite mode
      OutputStreamWriter writer = new OutputStreamWriter(outputStream);

      String buildInfo = BuildPropHelper.getBuildPropInfo("android.os.Build");
      String versionInfo = BuildPropHelper.getBuildPropInfo("android.os.Build$VERSION");
      String logs =
          "--------- beginning of build info\n"
              + buildInfo
              + "\n--------- beginning of version info\n"
              + versionInfo
              + "\n"
              + formatLogOutput();

      writer.write(logs);
      writer.flush();
      writer.close();

      Log.e(TAG, "Log file saved successfully in Scoped Storage.");
    } catch (Exception e) {
      Log.e(TAG, "Error writing log file: " + e.getMessage(), e);
    }
  }

  /** Legacy storage: Write logs to /storage/emulated/0 */
  private static void saveLogToLegacyStorage() {
    File dir = new File(Environment.getExternalStorageDirectory(), "Resizer/Logcat");

    if (dir.exists()) {
      deleteRecursive(dir);
    }
    if (!dir.mkdirs()) {
      Log.e(TAG, "Failed to create directory: " + dir.getAbsolutePath());
      return;
    }

    String fileName =
        "Resizer_" + Build.MANUFACTURER + "_" + Build.MODEL + "(" + Build.DEVICE + ")" + ".json";

    File file = new File(dir, fileName);
    String buildInfo = BuildPropHelper.getBuildPropInfo("android.os.Build");
    String versionInfo = BuildPropHelper.getBuildPropInfo("android.os.Build$VERSION");
    String logs =
        "--------- beginning of build info\n"
            + buildInfo
            + "\n--------- beginning of version info\n"
            + versionInfo
            + "\n"
            + formatLogOutput();

    try {
      FileOutputStream fos = new FileOutputStream(file, false); // overwrite mode
      OutputStreamWriter writer = new OutputStreamWriter(fos);
      writer.write(logs);
      writer.flush();
      writer.close();

      Log.e(TAG, "Log file saved successfully in Legacy Storage.");
    } catch (Exception e) {
      Log.e(TAG, "Error writing log file: " + e.getMessage(), e);
    }
  }

  /** Format logcat output into sections */
  public static String formatLogOutput() {
    Map<String, StringBuilder> logcatOutput = getLogcatOutput();
    StringBuilder sb = new StringBuilder();
    String[] logLevels = {"error", "warn", "debug", "info", "verbose", "assert", "unknown"};

    for (String buffer : new String[] {"MAIN", "SYSTEM", "CRASH", "RADIO", "EVENTS"}) {
      boolean hasContent = false;
      for (String level : logLevels) {
        String key = level.toUpperCase() + "_" + buffer;
        if (logcatOutput.get(key).length() > 0) {
          hasContent = true;
          break;
        }
      }
      if (hasContent) {
        sb.append("--------- beginning of ").append(buffer.toLowerCase()).append("\n");
        for (String level : logLevels) {
          String key = level.toUpperCase() + "_" + buffer;
          StringBuilder content = logcatOutput.get(key);
          sb.append("--------- ").append(level).append("\n");
          if (content.length() == 0) {
            sb.append("<empty>\n");
          } else {
            sb.append(content);
          }
        }
        sb.append("\n");
      }
    }
    return sb.toString();
  }

  public static Map<String, StringBuilder> getLogcatOutput() {
    Map<String, StringBuilder> logMap = new HashMap<>();
    String[] buffers = {"MAIN", "SYSTEM", "CRASH", "RADIO", "EVENTS"};
    String[] levels = {"ERROR", "WARN", "DEBUG", "INFO", "VERBOSE", "ASSERT", "UNKNOWN"};

    for (String level : levels) {
      for (String buffer : buffers) {
        logMap.put(level + "_" + buffer, new StringBuilder());
      }
    }

    Pattern logPattern =
        Pattern.compile(
            "^(\\d{2}-\\d{2}"
                + " \\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s+(\\d+)\\s+(\\d+)\\s+([VDIWEA])\\s+([^:]+):\\s*(.*)$");
    Pattern headerPattern =
        Pattern.compile(
            "^[-]+\\s*(debug|warn|info|verbose|error|assert|beginning of"
                + " (main|system|crash|radio|events))$",
            Pattern.CASE_INSENSITIVE);

    String currentLevel = null;
    String currentBuffer = null;

    try {
      BufferedReader reader =
          new BufferedReader(
              new InputStreamReader(Runtime.getRuntime().exec("logcat -d").getInputStream()));
      String line;

      while ((line = reader.readLine()) != null) {
        Matcher headerMatcher = headerPattern.matcher(line.trim());
        if (headerMatcher.matches()) {
          String header = headerMatcher.group(1).toUpperCase();
          if (header.startsWith("BEGINNING OF ")) {
            currentBuffer = header.substring(13).toUpperCase();
          } else {
            currentLevel = header;
          }
        } else {
          Matcher logMatcher = logPattern.matcher(line);
          String level;
          if (logMatcher.matches()) {
            String logChar = logMatcher.group(4);
            switch (logChar) {
              case "E":
                level = "ERROR";
                break;
              case "W":
                level = "WARN";
                break;
              case "D":
                level = "DEBUG";
                break;
              case "I":
                level = "INFO";
                break;
              case "V":
                level = "VERBOSE";
                break;
              case "A":
                level = "ASSERT";
                break;
              default:
                level = "UNKNOWN";
                break;
            }

            String finalLevel = currentLevel != null ? currentLevel : level;
            String finalBuffer = currentBuffer != null ? currentBuffer : "MAIN";
            String key = finalLevel + "_" + finalBuffer;
            StringBuilder sb = logMap.getOrDefault(key, logMap.get("UNKNOWN_MAIN"));
            sb.append(line).append("\n");
          } else {
            String fallbackKey = "UNKNOWN_" + (currentBuffer != null ? currentBuffer : "MAIN");
            logMap.get(fallbackKey).append(line).append("\n");
          }
        }
      }

      reader.close();
    } catch (Exception e) {
      logMap
          .get("UNKNOWN_MAIN")
          .append("Exception reading logcat: ")
          .append(e.getMessage())
          .append("\n");
    }

    return logMap;
  }

  /** Recursively delete directory contents */
  private static void deleteRecursive(File fileOrDirectory) {
    if (fileOrDirectory.isDirectory()) {
      File[] children = fileOrDirectory.listFiles();
      if (children != null) {
        for (File child : children) {
          deleteRecursive(child);
        }
      }
    }
    fileOrDirectory.delete();
  }
}
