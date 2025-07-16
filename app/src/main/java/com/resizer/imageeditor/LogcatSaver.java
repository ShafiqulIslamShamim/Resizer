package com.resizer.imageeditor;

import android.os.Build;
import android.os.Environment;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogcatSaver {

  public static void RunLog() {

    File dir = new File(Environment.getExternalStorageDirectory(), "Resized/Logcat");
    if (dir.exists()) {
      dir.delete();
    }
    dir.mkdirs();

    String fileName =
        "Resized_" + Build.MANUFACTURER + "_" + Build.MODEL + "(" + Build.DEVICE + ")" + ".json";

    File file = new File(dir, fileName);
    String buildInfo = BuildPropHelper.getBuildPropInfo("android.os.Build");
    String versionInfo = BuildPropHelper.getBuildPropInfo("android.os.Build$VERSION");
    String info =
        "--------- beginning of build info\n"
            + buildInfo
            + "\n--------- beginning of version info\n"
            + versionInfo
            + "\n"
            + formatLogOutput();

    try {
      FileOutputStream fos = new FileOutputStream(file);
      OutputStreamWriter writer = new OutputStreamWriter(fos);
      writer.write(info);
      writer.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

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
}
