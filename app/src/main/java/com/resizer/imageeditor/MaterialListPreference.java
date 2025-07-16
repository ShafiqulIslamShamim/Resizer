package com.resizer.imageeditor;

import android.content.Context;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class MaterialListPreference extends ListPreference {
  private int mClickedDialogEntryIndex;

  public MaterialListPreference(
      @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  public MaterialListPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  public MaterialListPreference(@NonNull Context context) {
    super(context);
  }

  @Override
  protected void onClick() {
    // If no entries or not enabled/persisted, don't show dialog
    if (getEntries() == null || getEntryValues() == null || !isEnabled() || !isPersistent()) {
      return;
    }

    // Find the index of current value
    mClickedDialogEntryIndex = findIndexOfValue(getValue());

    // Create Material dialog
    MaterialAlertDialogBuilder builder =
        new MaterialAlertDialogBuilder(getContext())

            // .setTitle(getDialogTitle())
            .setCustomTitle(DialogUtils.createStyledDialogTitle(getContext(), getDialogTitle()))
            .setSingleChoiceItems(
                getEntries(),
                mClickedDialogEntryIndex,
                (dialog, which) -> {
                  mClickedDialogEntryIndex = which;
                  // Update value when item is clicked
                  if (callChangeListener(getEntryValues()[which].toString())) {
                    setValueIndex(which);
                  }
                  dialog.dismiss();
                })
            .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss());

    // Optional: Add positive button if needed
    // builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
    //     if (mClickedDialogEntryIndex >= 0 && getEntryValues() != null) {
    //         String value = getEntryValues()[mClickedDialogEntryIndex].toString();
    //         if (callChangeListener(value)) {
    //             setValue(value);
    //         }
    //     }
    // });

    builder.show();
  }
}
