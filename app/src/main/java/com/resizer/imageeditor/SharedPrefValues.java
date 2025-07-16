package com.resizer.imageeditor;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import androidx.preference.PreferenceManager;

public class SharedPrefValues {

  // Gets the default SharedPreferences
  private static SharedPreferences getSharedPreferences() {

    Context context = AppContext.get();
    return PreferenceManager.getDefaultSharedPreferences(context);
  }

  // Unified method to get string from SharedPreferences
  public static String getValue(String key, String defaultValue) {
    SharedPreferences prefs = getSharedPreferences();
    if (prefs != null && prefs.contains(key)) {
      String value = prefs.getString(key, null);
      return !TextUtils.isEmpty(value) ? value : defaultValue;
    }
    return defaultValue;
  }

  // Unified method to get int from SharedPreferences
  public static int getValue(String key, int defaultValue) {
    String value = getValue(key, String.valueOf(defaultValue));
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  // Unified method to get float from SharedPreferences
  public static float getValue(String key, float defaultValue) {
    String value = getValue(key, String.valueOf(defaultValue));
    try {
      return Float.parseFloat(value);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  // Unified method to get double from SharedPreferences
  public static double getValue(String key, double defaultValue) {
    String value = getValue(key, String.valueOf(defaultValue));
    try {
      return Double.parseDouble(value);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  public static boolean getValue(String key, boolean defaultValue) {
    String value = getValue(key, defaultValue ? "1" : "0");

    try {
      return Integer.parseInt(value) != 0;
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  public static boolean parseFlexibleBoolean(String value) {
    if (value == null) return false;
    value = value.trim().toLowerCase();
    return value.equals("true") || value.equals("1");
  }

  public static int booleanToInt(boolean value) {
    return value ? 1 : 0;
  }

  public static void putValue(String key, String value) {
    SharedPreferences prefs = getSharedPreferences();
    SharedPreferences.Editor editor = prefs.edit();
    editor.putString(key, value);
    editor.apply(); // or editor.commit();
  }

  public static void putValueIfAbsent(String key, String defaultValue) {
    SharedPreferences prefs = getSharedPreferences();
    if (!prefs.contains(key)) {
      SharedPreferences.Editor editor = prefs.edit();
      editor.putString(key, defaultValue);
      editor.apply();
    }
  }
}
