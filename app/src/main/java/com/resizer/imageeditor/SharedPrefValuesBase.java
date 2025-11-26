package com.resizer.imageeditor;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Simple SharedPreferences helper with listener support. Initialize once in Application.onCreate():
 * SharedPrefValues.init(appContext);
 */
public final class SharedPrefValuesBase {

  private static final String PREF_NAME = "app_prefs_v1";
  private static SharedPreferences prefs;
  private static final Set<OnPrefChangeListener> listeners =
      Collections.synchronizedSet(new HashSet<>());

  public interface OnPrefChangeListener {
    void onPrefChanged(@NonNull String key, @NonNull String newValue);
  }

  // init must be called once (e.g. from Application)
  public static void init(@NonNull Context context) {
    if (prefs == null) {
      prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
  }

  public static String getValue(@NonNull String key, @NonNull String defaultValue) {
    ensureInit();
    return prefs.getString(key, defaultValue);
  }

  public static void setValue(@NonNull String key, @NonNull String value) {
    ensureInit();
    String old = prefs.getString(key, null);
    if (value.equals(old)) return; // nothing changed

    prefs.edit().putString(key, value).apply();

    // notify listeners
    synchronized (listeners) {
      for (OnPrefChangeListener l : listeners) {
        try {
          l.onPrefChanged(key, value);
        } catch (Exception ignore) {
          // defensive: a misbehaving listener won't crash the loop
        }
      }
    }
  }

  public static void addListener(@NonNull OnPrefChangeListener l) {
    listeners.add(l);
  }

  public static void removeListener(@NonNull OnPrefChangeListener l) {
    listeners.remove(l);
  }

  private static void ensureInit() {
    if (prefs == null) {
      throw new IllegalStateException(
          "SharedPrefValues not initialized. Call SharedPrefValues.init(context) in"
              + " Application.onCreate()");
    }
  }
}
