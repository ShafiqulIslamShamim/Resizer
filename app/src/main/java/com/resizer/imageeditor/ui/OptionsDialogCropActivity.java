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
import java.util.ArrayList;
import java.util.Arrays;

public class OptionsDialogCropActivity extends DialogFragment {

  public interface OptionsDialogListener {
    void onOptionsSelected(
        String format, int quality, String aspect, int customW, int customH, boolean isSAFEnabled);

    void onPickFolderRequested();
  }

  private OptionsDialogListener listener;
  private int imageWidth = 0, imageHeight = 0;
  private String selectedFolderPath = null;

  private boolean isSAF = false;

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
    Spinner spinnerFormat = view.findViewById(R.id.spinnerCompressionFormat);
    Spinner spinnerAspect = view.findViewById(R.id.spinnerAspectRatio);
    TextView tvAspectSummary = view.findViewById(R.id.tvAspectRatioSummary);
    TextView tvFormatSummary = view.findViewById(R.id.tvCompressionFormatSummary);
    LinearLayout layoutCustomAspect = view.findViewById(R.id.layoutCustomAspectRatio);
    EditText etCustomWidth = view.findViewById(R.id.etCustomWidth);
    EditText etCustomHeight = view.findViewById(R.id.etCustomHeight);
    SeekBar seekQuality = view.findViewById(R.id.seekBarQuality_new);
    TextView tvQuality = view.findViewById(R.id.tvImageQuality_new);
    Button btnPick = view.findViewById(R.id.btnPickAndCrop_new);
    Button btnPickFolder = view.findViewById(R.id.btnPickFolder_new);
    TextView tvFolderPath = view.findViewById(R.id.tvSelectedFolderPath_new);
    RadioGroup rgSaveLocation = view.findViewById(R.id.rgSaveLocation_new);

    // Setup compression format spinner
    ArrayList<String> formatOptions = new ArrayList<>(Arrays.asList("JPEG", "PNG", "WEBP"));
    ArrayAdapter<String> formatAdapter =
        new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, formatOptions);
    formatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spinnerFormat.setAdapter(formatAdapter);
    spinnerFormat.setSelection(0); // Default to JPEG
    tvFormatSummary.setText("Selected: JPEG");

    spinnerFormat.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          @Override
          public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            tvFormatSummary.setText("Selected: " + formatOptions.get(position));
          }

          @Override
          public void onNothingSelected(AdapterView<?> parent) {
            tvFormatSummary.setText("Select a compression format");
          }
        });

    // Setup aspect ratio spinner
    ArrayList<String> aspectOptions = new ArrayList<>();
    populateAspectRatioOptions(aspectOptions);
    ArrayAdapter<String> aspectAdapter =
        new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, aspectOptions);
    aspectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spinnerAspect.setAdapter(aspectAdapter);
    spinnerAspect.setSelection(aspectOptions.indexOf("Default")); // Default to "Default"
    tvAspectSummary.setText("Selected: Default");

    spinnerAspect.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          @Override
          public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            String selected = aspectOptions.get(position);
            tvAspectSummary.setText("Selected: " + selected);
            layoutCustomAspect.setVisibility(
                "Custom".equalsIgnoreCase(selected) ? View.VISIBLE : View.GONE);
          }

          @Override
          public void onNothingSelected(AdapterView<?> parent) {
            tvAspectSummary.setText("Select an aspect ratio");
            layoutCustomAspect.setVisibility(View.GONE);
          }
        });

    // Quality change
    seekQuality.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          @Override
          public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            tvQuality.setText("Quality: " + i + "%");
          }

          @Override
          public void onStartTrackingTouch(SeekBar sb) {}

          @Override
          public void onStopTrackingTouch(SeekBar sb) {}
        });

    // Folder picker
    rgSaveLocation.setOnCheckedChangeListener(
        (group, checkedId) -> {
          if (checkedId == R.id.rbSaveToFolder_new) {
            btnPickFolder.setVisibility(View.VISIBLE);
            isSAF = true;
          } else {
            btnPickFolder.setVisibility(View.GONE);
            tvFolderPath.setText("No folder selected");
            selectedFolderPath = null;
            isSAF = false;
          }
        });

    btnPickFolder.setOnClickListener(
        v -> {
          if (listener != null) listener.onPickFolderRequested();
        });

    // Confirm button
    btnPick.setOnClickListener(
        v -> {
          String format = spinnerFormat.getSelectedItem().toString();
          if (format.isEmpty()) {
            Toast.makeText(getContext(), "Please select an image format", Toast.LENGTH_SHORT)
                .show();
            return;
          }

          int quality = seekQuality.getProgress();
          String aspect = spinnerAspect.getSelectedItem().toString();
          int customW = 0, customH = 0;

          if ("Custom".equalsIgnoreCase(aspect)) {
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
          } else if ("Free".equalsIgnoreCase(aspect)) {
            aspect = "free";
          } else if ("Default".equalsIgnoreCase(aspect)) {
            aspect = "default";
          } else if ("Square".equalsIgnoreCase(aspect)) {
            aspect = "square";
          }

          // Check if "Save to Folder" selected but no folder picked
          int selectedSaveOption = rgSaveLocation.getCheckedRadioButtonId();
          if (selectedSaveOption == R.id.rbSaveToFolder_new) {
            if (selectedFolderPath == null || selectedFolderPath.trim().isEmpty()) {
              Toast.makeText(
                      getContext(), "Please pick a folder to save the image", Toast.LENGTH_SHORT)
                  .show();
              return;
            }
          }

          if (listener != null) {
            listener.onOptionsSelected(format, quality, aspect, customW, customH, isSAF);
          }

          dismiss();
        });

    setCancelable(false);
    return builder.create();
  }

  private void populateAspectRatioOptions(ArrayList<String> options) {
    options.clear();
    options.add("Default");
    options.add("Free");
    options.add("Square");

    int[][] aspectRatios = {{4, 3}, {3, 2}, {5, 4}, {16, 9}, {9, 16}, {2, 1}, {21, 9}, {3, 1}};
    for (int[] ratio : aspectRatios) {
      if (imageWidth == 0
          || imageHeight == 0
          || isRatioApplicable(imageWidth, imageHeight, ratio[0], ratio[1])) {
        options.add(ratio[0] + ":" + ratio[1]);
      }
    }

    options.add("Custom");
  }

  private boolean isRatioApplicable(int imageW, int imageH, int arW, int arH) {
    float imageRatio = (float) imageW / imageH;
    float targetRatio = (float) arW / arH;
    float epsilon = 0.1f;
    return Math.abs(imageRatio - targetRatio) <= epsilon || (imageW >= arW && imageH >= arH);
  }

  public void setSelectedFolderName(String name) {
    selectedFolderPath = name;
    if (getDialog() != null) {
      TextView tv = getDialog().findViewById(R.id.tvSelectedFolderPath_new);
      if (tv != null) tv.setText("Selected: " + name);
    }
  }
}
