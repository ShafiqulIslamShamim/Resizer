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

public class CropOptionsDialog extends DialogFragment {

  public interface CropOptionsListener {
    void onOptionsSelected(
        int w, int h, String format, int quality, String maxFileSize, boolean isSAFEnabled);

    void onPickFolderRequested();
  }

  private CropOptionsListener listener;
  private int defaultWidth, defaultHeight;
  private String selectedFolderPath = null;
  private boolean isSAF = false;

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

    // Views
    Spinner spinnerFormat = view.findViewById(R.id.spinnerFormat);
    TextView tvFormatSummary = view.findViewById(R.id.tvFormatSummary);
    RadioGroup rgSaveLocation = view.findViewById(R.id.rgSaveLocation);
    EditText etWidth = view.findViewById(R.id.etWidth);
    EditText etHeight = view.findViewById(R.id.etHeight);
    SeekBar seekQuality = view.findViewById(R.id.seekQuality);
    TextView tvQuality = view.findViewById(R.id.tvQuality);
    EditText etMaxFileSize = view.findViewById(R.id.etMaxFileSize);
    Button btnPick = view.findViewById(R.id.btnPickCrop);
    Button btnPickFolder = view.findViewById(R.id.btnPickFolder);
    TextView tvFolderPath = view.findViewById(R.id.tvFolderPath);

    // Set default width and height
    etWidth.setText(String.valueOf(defaultWidth));
    etHeight.setText(String.valueOf(defaultHeight));

    // Setup compression format spinner
    ArrayList<String> formatOptions = new ArrayList<>(Arrays.asList("JPEG", "PNG", "WEBP"));
    ArrayAdapter<String> formatAdapter =
        new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, formatOptions);
    formatAdapter.setDropDownViewResource(R.layout.m3_spinner_dropdown_item);
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

    // Folder picker logic
    rgSaveLocation.setOnCheckedChangeListener(
        (group, checkedId) -> {
          if (checkedId == R.id.rbSaveToFolder) {
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

    // Confirm button logic
    btnPick.setOnClickListener(
        v -> {
          String widthStr = etWidth.getText().toString().trim();
          String heightStr = etHeight.getText().toString().trim();
          String sizeStr = etMaxFileSize.getText().toString().trim();

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

          String format = spinnerFormat.getSelectedItem().toString();
          int quality = seekQuality.getProgress();

          int selectedSaveOption = rgSaveLocation.getCheckedRadioButtonId();
          if (selectedSaveOption == R.id.rbSaveToFolder) {
            if (selectedFolderPath == null || selectedFolderPath.isEmpty()) {
              Toast.makeText(
                      getContext(), "Please pick a folder to save the image", Toast.LENGTH_SHORT)
                  .show();
              return;
            }
          }

          listener.onOptionsSelected(w, h, format, quality, sizeStr, isSAF);
          dismiss();
        });

    setCancelable(false);
    return builder.create();
  }

  public void setSelectedFolderName(String name) {
    this.selectedFolderPath = name;
    if (getDialog() != null) {
      TextView tv = getDialog().findViewById(R.id.tvFolderPath);
      if (tv != null) tv.setText("Selected: " + name);
    }
  }
}
