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

public class CropOptionsDialog extends DialogFragment {

  public interface CropOptionsListener {
    void onOptionsSelected(int w, int h, String format, int quality, String maxFileSize);

    void onPickFolderRequested();
  }

  private CropOptionsListener listener;

  private int defaultWidth, defaultHeight;

  public CropOptionsDialog(CropOptionsListener l, int defaultW, int defaultH) {
    this.listener = l;
    this.defaultWidth = defaultW;
    this.defaultHeight = defaultH;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_crop_options, null);

    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
    builder.setView(view);

    RadioGroup rgFormat = view.findViewById(R.id.rgFormat);
    RadioGroup rgSaveLocation = view.findViewById(R.id.rgSaveLocation);

    EditText etWidth = view.findViewById(R.id.etWidth);
    EditText etHeight = view.findViewById(R.id.etHeight);

    etWidth.setText(String.valueOf(defaultWidth));
    etHeight.setText(String.valueOf(defaultHeight));

    SeekBar seekQuality = view.findViewById(R.id.seekQuality);
    TextView tvQuality = view.findViewById(R.id.tvQuality);

    EditText etMaxFileSize = view.findViewById(R.id.etMaxFileSize); // <-- Max file size EditText

    Button btnPick = view.findViewById(R.id.btnPickCrop);
    Button btnPickFolder = view.findViewById(R.id.btnPickFolder);
    TextView tvFolderPath = view.findViewById(R.id.tvFolderPath);

    seekQuality.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            tvQuality.setText("Quality: " + i + "%");
          }

          public void onStartTrackingTouch(SeekBar sb) {}

          public void onStopTrackingTouch(SeekBar sb) {}
        });

    rgSaveLocation.setOnCheckedChangeListener(
        (group, checkedId) -> {
          if (checkedId == R.id.rbSaveToFolder) {
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
          String widthStr = etWidth.getText().toString().trim();
          String heightStr = etHeight.getText().toString().trim();
          String sizeStr = etMaxFileSize.getText().toString().trim(); // <-- get as String

          if (widthStr.isEmpty() || heightStr.isEmpty()) {
            Toast.makeText(getContext(), "Please enter both width and height", Toast.LENGTH_SHORT)
                .show();
            return;
          }

          int w, h;
          try {
            w = Integer.parseInt(widthStr);
            h = Integer.parseInt(heightStr);
          } catch (NumberFormatException e) {
            Toast.makeText(
                    getContext(), "Width and height must be valid numbers", Toast.LENGTH_SHORT)
                .show();
            return;
          }

          if (w <= 0 || h <= 0) {
            Toast.makeText(
                    getContext(), "Width and height must be greater than 0", Toast.LENGTH_SHORT)
                .show();
            return;
          }

          int fmtId = rgFormat.getCheckedRadioButtonId();
          if (fmtId == -1) {
            Toast.makeText(getContext(), "Please select an image format", Toast.LENGTH_SHORT)
                .show();
            return;
          }

          String format = "JPEG";
          if (fmtId == R.id.rbPng) format = "PNG";
          else if (fmtId == R.id.rbWebp) format = "WEBP";

          int quality = seekQuality.getProgress();

          // Send maxFileSize string directly (can be empty)
          listener.onOptionsSelected(w, h, format, quality, sizeStr);
          dismiss();
        });

    // Make dialog uncancelable
    setCancelable(false);

    return builder.create();
  }

  public void setSelectedFolderName(String name) {
    if (getDialog() != null) {
      TextView tv = getDialog().findViewById(R.id.tvFolderPath);
      if (tv != null) tv.setText("Selected: " + name);
    }
  }
}
