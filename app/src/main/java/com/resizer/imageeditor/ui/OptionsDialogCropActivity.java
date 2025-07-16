package com.resizer.imageeditor.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.resizer.imageeditor.R;

public class OptionsDialogCropActivity extends DialogFragment {

  public interface OptionsDialogListener {
    void onOptionsSelected(String format, int quality);

    void onPickFolderRequested();
  }

  private OptionsDialogListener listener;

  public OptionsDialogCropActivity(OptionsDialogListener l) {
    this.listener = l;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    View view =
        LayoutInflater.from(getContext()).inflate(R.layout.dialog_options_crop_activity, null);

    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
    builder.setView(view);

    RadioGroup rgFormat = view.findViewById(R.id.rgCompressionFormat_new);
    RadioGroup rgSaveLocation = view.findViewById(R.id.rgSaveLocation_new);

    SeekBar seekQuality = view.findViewById(R.id.seekBarQuality_new);
    TextView tvQuality = view.findViewById(R.id.tvImageQuality_new);

    Button btnPick = view.findViewById(R.id.btnPickAndCrop_new);
    Button btnPickFolder = view.findViewById(R.id.btnPickFolder_new);
    TextView tvFolderPath = view.findViewById(R.id.tvSelectedFolderPath_new);

    // Update quality text as the user moves the SeekBar
    seekQuality.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            tvQuality.setText("Quality: " + i + "%");
          }

          public void onStartTrackingTouch(SeekBar sb) {}

          public void onStopTrackingTouch(SeekBar sb) {}
        });

    // Show/hide folder picker button
    rgSaveLocation.setOnCheckedChangeListener(
        (group, checkedId) -> {
          if (checkedId == R.id.rbSaveToFolder_new) {
            btnPickFolder.setVisibility(View.VISIBLE);
          } else {
            btnPickFolder.setVisibility(View.GONE);
            tvFolderPath.setText("No folder selected");
          }
        });

    btnPickFolder.setOnClickListener(
        v -> {
          if (listener != null) listener.onPickFolderRequested();
        });

    btnPick.setOnClickListener(
        v -> {
          int fmtId = rgFormat.getCheckedRadioButtonId();
          if (fmtId == -1) {
            Toast.makeText(getContext(), "Please select an image format", Toast.LENGTH_SHORT)
                .show();
            return;
          }

          String format = "JPEG";
          if (fmtId == R.id.rbFormatPng_new) format = "PNG";
          else if (fmtId == R.id.rbFormatWebp_new) format = "WEBP";

          int quality = seekQuality.getProgress();

          // Call listener
          if (listener != null) {
            listener.onOptionsSelected(format, quality);
          }
          dismiss();
        });

    // Make dialog uncancelable
    setCancelable(false);

    return builder.create();
  }

  public void setSelectedFolderName(String name) {
    if (getDialog() != null) {
      TextView tv = getDialog().findViewById(R.id.tvSelectedFolderPath_new);
      if (tv != null) tv.setText("Selected: " + name);
    }
  }
}
