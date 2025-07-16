package com.resizer.imageeditor; // ← তোমার প্রকৃত প্যাকেজ অনুযায়ী রাখো

import android.content.Context;
import android.util.AttributeSet;
import androidx.preference.SwitchPreferenceCompat;

public class StringSwitchPreference extends SwitchPreferenceCompat {

  public StringSwitchPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected boolean persistBoolean(boolean value) {
    // Store "1" for true, "0" for false
    return persistString(value ? "1" : "0");
  }

  @Override
  public boolean getPersistedBoolean(boolean defaultReturnValue) {
    // Read string and convert back to boolean
    String stringValue = getPersistedString(defaultReturnValue ? "1" : "0");
    return "1".equals(stringValue);
  }
}
