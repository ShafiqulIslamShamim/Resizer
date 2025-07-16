package com.resizer.imageeditor;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.TypedValue;
import android.view.View;
import android.widget.*;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.resizer.imageeditor.models.ImageInfo;
import com.resizer.imageeditor.ui.OptionsDialogCropActivity;
import com.resizer.imageeditor.utils.ImageUtils;
import com.yalantis.ucrop.UCrop;
import java.io.File;

public class CropFragment extends Fragment {

  private Button btnSelectImage, btnSaveImage;
  private ImageView ivBeforeImage, ivAfterImage;
  private TextView tvBeforeLabel, tvAfterLabel;
  private Uri sourceUri, resultUri;
  private Uri selectedFolderUri = null;
  private int quality = 75;
  private String outFormat = "JPEG";
  private boolean keepExif = true;

  private ActivityResultLauncher<Intent> pickImageLauncher;
  private ActivityResultLauncher<Intent> cropLauncher;
  private ActivityResultLauncher<Intent> folderPickerLauncher;

  public CropFragment() {
    super(R.layout.fragment_crop); // XML layout
  }

  private void setupImageView(ImageView imageView) {
    LinearLayout.LayoutParams params =
        new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

    // Set 24dp bottom margin
    params.setMargins(
        0,
        0,
        0,
        (int)
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 24, imageView.getResources().getDisplayMetrics()));

    imageView.setLayoutParams(params);
    imageView.setAdjustViewBounds(true);
    imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    btnSelectImage = view.findViewById(R.id.btnSelectImage_new);
    btnSaveImage = view.findViewById(R.id.btnSaveImage_new);
    ivBeforeImage = view.findViewById(R.id.ivBeforeImage_new);
    ivAfterImage = view.findViewById(R.id.ivAfterImage_new);
    tvBeforeLabel = view.findViewById(R.id.tvBeforeLabel_new);
    tvAfterLabel = view.findViewById(R.id.tvAfterLabel_new);

    pickImageLauncher =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            res -> {
              if (res.getResultCode() == Activity.RESULT_OK && res.getData() != null) {
                sourceUri = res.getData().getData();
                handlePickedImage();
              }
            });

    cropLauncher =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            res -> {
              if (res.getResultCode() == Activity.RESULT_OK && res.getData() != null) {
                resultUri = UCrop.getOutput(res.getData());
                if (resultUri != null) {
                  setupImageView(ivAfterImage);
                  ivAfterImage.setImageURI(resultUri);

                  ImageInfo info =
                      ImageUtils.resizeCompressCropActivity(
                          requireContext(),
                          resultUri,
                          quality,
                          outFormat,
                          keepExif,
                          false,
                          null,
                          null);

                  if (info != null) {
                    tvAfterLabel.setText(
                        "After: "
                            + info.width
                            + "x"
                            + info.height
                            + ", "
                            + info.sizeKb
                            + " KB, "
                            + info.format);
                  } else {
                    tvAfterLabel.setText("After: Failed to get info");
                  }
                }
              } else {
                Toast.makeText(requireContext(), "Crop canceled", Toast.LENGTH_SHORT).show();
              }
            });

    folderPickerLauncher =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
              if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Uri treeUri = result.getData().getData();
                if (treeUri != null) {
                  requireContext()
                      .getContentResolver()
                      .takePersistableUriPermission(
                          treeUri,
                          Intent.FLAG_GRANT_READ_URI_PERMISSION
                              | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                  selectedFolderUri = treeUri;

                  OptionsDialogCropActivity dialog =
                      (OptionsDialogCropActivity)
                          getParentFragmentManager().findFragmentByTag("OptionsDialogCropActivity");
                  if (dialog != null) {
                    dialog.setSelectedFolderName(getFolderName(treeUri));
                  }
                }
              }
            });

    btnSelectImage.setOnClickListener(
        v -> {
          Intent intent =
              new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
          pickImageLauncher.launch(intent);
        });

    btnSaveImage.setOnClickListener(
        v -> {
          if (!StoragePermissionHelper.isPermissionGranted(TabbedActivity.ActivityContext)) {

            StoragePermissionHelper.checkAndRequestStoragePermission(
                TabbedActivity.ActivityContext);

            return;
          }

          if (resultUri != null) {
            processImage();
          } else {
            Toast.makeText(requireContext(), "Please crop an image first", Toast.LENGTH_SHORT)
                .show();
          }
        });
  }

  private void handlePickedImage() {
    try {
      Bitmap bmp =
          MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), sourceUri);
      setupImageView(ivBeforeImage);
      ivBeforeImage.setImageBitmap(bmp);
      tvBeforeLabel.setText(
          "Before: "
              + bmp.getWidth()
              + "x"
              + bmp.getHeight()
              + ", "
              + ImageUtils.getFileSizeKb(requireContext(), sourceUri)
              + " KB");

      new OptionsDialogCropActivity(
              new OptionsDialogCropActivity.OptionsDialogListener() {
                @Override
                public void onOptionsSelected(String format, int q) {
                  quality = q;
                  outFormat = format;
                  keepExif = true;

                  Uri destUri =
                      Uri.fromFile(
                          new File(
                              requireContext().getCacheDir(),
                              "cropped" + ImageUtils.getExtension(format)));
                  UCrop uCrop = UCrop.of(sourceUri, destUri);

                  cropLauncher.launch(uCrop.getIntent(requireContext()));
                }

                @Override
                public void onPickFolderRequested() {
                  pickFolder();
                }
              })
          .show(getParentFragmentManager(), "OptionsDialogCropActivity");

    } catch (Exception e) {
      e.printStackTrace();
      Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
    }
  }

  private void pickFolder() {
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
    intent.addFlags(
        Intent.FLAG_GRANT_READ_URI_PERMISSION
            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
    folderPickerLauncher.launch(intent);
  }

  private String getFolderName(Uri uri) {
    String docId = DocumentsContract.getTreeDocumentId(uri);
    String[] parts = docId.split(":");
    return parts.length > 1 ? parts[1] : docId;
  }

  private void processImage() {
    String originalName = getFileNameFromUri(sourceUri);
    String baseName =
        (originalName != null) ? originalName.replaceAll("\\.[^.]+$", "") : "new_image";
    String fileName = "resized_" + baseName;

    ImageInfo info =
        ImageUtils.resizeCompressCropActivity(
            requireContext(),
            resultUri,
            quality,
            outFormat,
            keepExif,
            true,
            selectedFolderUri,
            fileName);

    if (info != null) {
      setupImageView(ivAfterImage);
      ivAfterImage.setImageURI(Uri.fromFile(new File(info.outputPath)));
      tvAfterLabel.setText(
          "Saved: "
              + info.outputPath
              + "\n"
              + info.width
              + "x"
              + info.height
              + ", "
              + info.sizeKb
              + " KB, "
              + info.format);
      Toast.makeText(requireContext(), "Image saved successfully!", Toast.LENGTH_SHORT).show();
    } else {
      Toast.makeText(requireContext(), "Failed to save image", Toast.LENGTH_SHORT).show();
    }
  }

  private String getFileNameFromUri(Uri uri) {
    String result = null;
    if ("content".equals(uri.getScheme())) {
      try (Cursor cursor =
          requireContext().getContentResolver().query(uri, null, null, null, null)) {
        if (cursor != null && cursor.moveToFirst()) {
          int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
          if (nameIndex >= 0) {
            result = cursor.getString(nameIndex);
          }
        }
      }
    }

    if (result == null || result.contains(":")) {
      result = "resized_" + System.currentTimeMillis();
    }

    return result;
  }
}
