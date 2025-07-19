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
    void onOptionsSelected(String format, int quality, String aspect, int customW, int customH);

    void onPickFolderRequested();
  }

  private OptionsDialogListener listener;
  private int imageWidth = 0, imageHeight = 0;

  public OptionsDialogCropActivity(OptionsDialogListener l) {
    this.listener = l;
  }

  public void setImageDimensions(int w, int h) {
    this.imageWidth = w;
    this.imageHeight = h;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    View view =
        LayoutInflater.from(getContext()).inflate(R.layout.dialog_options_crop_activity, null);

    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
    builder.setView(view);

    // Views
    RadioGroup rgFormat = view.findViewById(R.id.rgCompressionFormat_new);
    RadioGroup rgSaveLocation = view.findViewById(R.id.rgSaveLocation_new);
    RadioGroup rgAspect = view.findViewById(R.id.rgAspectRatio);
    LinearLayout layoutCustomAspect = view.findViewById(R.id.layoutCustomAspectRatio);
    EditText etCustomWidth = view.findViewById(R.id.etCustomWidth);
    EditText etCustomHeight = view.findViewById(R.id.etCustomHeight);

    SeekBar seekQuality = view.findViewById(R.id.seekBarQuality_new);
    TextView tvQuality = view.findViewById(R.id.tvImageQuality_new);
    Button btnPick = view.findViewById(R.id.btnPickAndCrop_new);
    Button btnPickFolder = view.findViewById(R.id.btnPickFolder_new);
    TextView tvFolderPath = view.findViewById(R.id.tvSelectedFolderPath_new);

    // Quality change
    seekQuality.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            tvQuality.setText("Quality: " + i + "%");
          }

          public void onStartTrackingTouch(SeekBar sb) {}

          public void onStopTrackingTouch(SeekBar sb) {}
        });

    // Folder picker
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

    // Custom aspect visibility
    rgAspect.setOnCheckedChangeListener(
        (group, checkedId) -> {
          RadioButton selected = group.findViewById(checkedId);
          if (selected != null && "Custom".equalsIgnoreCase(selected.getText().toString())) {
            layoutCustomAspect.setVisibility(View.VISIBLE);
          } else {
            layoutCustomAspect.setVisibility(View.GONE);
          }
        });

    // Confirm button
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

          // Aspect ratio
          int aspectId = rgAspect.getCheckedRadioButtonId();
          String aspect = "default";
          int customW = 0, customH = 0;

          if (aspectId != -1) {
            RadioButton rb = rgAspect.findViewById(aspectId);
            String text = rb.getText().toString().trim();

            if ("Free".equalsIgnoreCase(text)) aspect = "free";
            else if ("Default".equalsIgnoreCase(text)) aspect = "default";
            else if ("Square".equalsIgnoreCase(text)) aspect = "square";
            else if ("Custom".equalsIgnoreCase(text)) {
              aspect = "custom";
              try {
                customW = Integer.parseInt(etCustomWidth.getText().toString());
                customH = Integer.parseInt(etCustomHeight.getText().toString());
                if (customW <= 0 || customH <= 0) throw new NumberFormatException();
              } catch (NumberFormatException e) {
                Toast.makeText(
                        getContext(), "Enter valid custom width and height", Toast.LENGTH_SHORT)
                    .show();
                return;
              }
            } else {
              aspect = text; // e.g., "4:3", "16:9"
            }
          }

          if (listener != null) {
            listener.onOptionsSelected(format, quality, aspect, customW, customH);
          }

          dismiss();
        });

    // Populate aspect ratios dynamically
    populateAspectRatioOptions(rgAspect);

    setCancelable(false);
    return builder.create();
  }

  private void populateAspectRatioOptions(RadioGroup group) {
    group.removeAllViews();

    addAspectOption(group, "Default");
    addAspectOption(group, "Free");
    addAspectOption(group, "Square");

    int[][] aspectRatios = {{4, 3}, {3, 2}, {5, 4}, {16, 9}, {9, 16}, {2, 1}, {21, 9}, {3, 1}};

    for (int[] ratio : aspectRatios) {
      if (imageWidth == 0
          || imageHeight == 0
          || isRatioApplicable(imageWidth, imageHeight, ratio[0], ratio[1])) {
        addAspectOption(group, ratio[0] + ":" + ratio[1]);
      }
    }

    addAspectOption(group, "Custom");
  }

  private void addAspectOption(RadioGroup group, String label) {
    RadioButton rb = new RadioButton(getContext());
    rb.setText(label);
    rb.setId(View.generateViewId());
    if (label.equals("Default")) {
      rb.setChecked(true); // Set only the "Default" option as checked
    }
    group.addView(rb);
  }

  private boolean isRatioApplicable(int imageW, int imageH, int arW, int arH) {
    float imageRatio = (float) imageW / imageH;
    float targetRatio = (float) arW / arH;
    float epsilon = 0.1f;
    return Math.abs(imageRatio - targetRatio) <= epsilon || (imageW >= arW && imageH >= arH);
  }

  public void setSelectedFolderName(String name) {
    if (getDialog() != null) {
      TextView tv = getDialog().findViewById(R.id.tvSelectedFolderPath_new);
      if (tv != null) tv.setText("Selected: " + name);
    }
  }
}
