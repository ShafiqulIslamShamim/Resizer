package com.resizer.imageeditor;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
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
import com.resizer.imageeditor.ui.CropOptionsDialog;
import com.resizer.imageeditor.utils.AspectRatioCalculator;
import com.resizer.imageeditor.utils.ImageUtils;
import com.yalantis.ucrop.UCrop;
import java.io.File;
import java.io.InputStream;

public class EditorFragment extends Fragment {

  private Button btnSelect;
  private Button btnSave;
  private ImageView ivBefore, ivAfter;
  private TextView tvInfoBefore, tvInfoAfter;
  private Uri sourceUri, resultUri;
  private Uri selectedFolderUri = null;
  private int targetW = 0, targetH = 0;
  private int quality = 80;
  private String maxFileSize = "";
  private String outFormat = "JPEG";
  private boolean keepExif = true;
  private boolean isSAFPath = false;

  private ActivityResultLauncher<Intent> pickImageLauncher;
  private ActivityResultLauncher<Intent> cropLauncher;
  private ActivityResultLauncher<Intent> folderPickerLauncher;

  public EditorFragment() {
    super(R.layout.fragment_editor); // XML layout
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

    btnSelect = view.findViewById(R.id.btnSelect);
    btnSave = view.findViewById(R.id.btnSave);
    ivBefore = view.findViewById(R.id.ivBefore);
    ivAfter = view.findViewById(R.id.ivAfter);
    tvInfoBefore = view.findViewById(R.id.tvInfoBefore);
    tvInfoAfter = view.findViewById(R.id.tvInfoAfter);

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
                  setupImageView(ivAfter);

                  ivAfter.setImageBitmap(loadScaledBitmap(resultUri, 720, 1280, new int[2]));

                  ImageInfo info =
                      ImageUtils.resizeCompress(
                          requireContext(),
                          resultUri,
                          targetW,
                          targetH,
                          quality,
                          outFormat,
                          keepExif,
                          false,
                          null,
                          null,
                          "",
                          isSAFPath);

                  if (info != null) {
                    tvInfoAfter.setText(
                        "After: "
                            + info.width
                            + "x"
                            + info.height
                            + ", "
                            + info.sizeKb
                            + " KB, "
                            + info.format);
                  } else {
                    tvInfoAfter.setText("After: Failed to get info");
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

                  CropOptionsDialog dialog =
                      (CropOptionsDialog)
                          getParentFragmentManager().findFragmentByTag("ResizeDialog");
                  if (dialog != null) {
                    dialog.setSelectedFolderName(getFolderName(treeUri));
                  }
                }
              }
            });

    btnSelect.setOnClickListener(
        v -> {
          Intent intent =
              new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
          pickImageLauncher.launch(intent);
        });

    btnSave.setOnClickListener(
        v -> {
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
      // Get ImageView dimensions (use default if not yet laid out)

      int reqWidth = 720;
      int reqHeight = 1280;

      // Load image dimensions without decoding the full bitmap
      BitmapFactory.Options options = new BitmapFactory.Options();
      options.inJustDecodeBounds = true;
      InputStream inputStream = requireContext().getContentResolver().openInputStream(sourceUri);
      BitmapFactory.decodeStream(inputStream, null, options);
      if (inputStream != null) {
        inputStream.close();
      }

      // Store original dimensions
      int originalWidth = options.outWidth;
      int originalHeight = options.outHeight;

      // Calculate inSampleSize for scaling
      options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
      options.inJustDecodeBounds = false;

      // Decode scaled bitmap
      inputStream = requireContext().getContentResolver().openInputStream(sourceUri);
      Bitmap bmp = BitmapFactory.decodeStream(inputStream, null, options);
      if (inputStream != null) {
        inputStream.close();
      }

      if (bmp != null) {
        setupImageView(ivBefore);
        ivBefore.setImageBitmap(bmp);

        // Use original dimensions for display
        tvInfoBefore.setText(
            "Before: "
                + originalWidth
                + "x"
                + originalHeight
                + ", "
                + ImageUtils.getFileSizeKb(requireContext(), sourceUri)
                + " KB");

        CropOptionsDialog dialog =
            new CropOptionsDialog(
                new CropOptionsDialog.CropOptionsListener() {
                  @Override
                  public void onOptionsSelected(
                      int w, int h, String format, int q, String maxSize, boolean isSAFEnabled) {
                    targetW = w;
                    targetH = h;
                    quality = q;
                    outFormat = format;
                    keepExif = true;
                    maxFileSize = maxSize;
                    isSAFPath = isSAFEnabled;

                    Uri destUri =
                        Uri.fromFile(
                            new File(
                                requireContext().getCacheDir(),
                                "resized" + ImageUtils.getExtension(format)));
                    UCrop uCrop = UCrop.of(sourceUri, destUri);

                    if (w > 0 && h > 0) {
                      float[] aspectRatio = AspectRatioCalculator.calculateAspectRatio(w, h);
                      uCrop = uCrop.withAspectRatio(aspectRatio[0], aspectRatio[1]);

                      if (w < originalWidth && h < originalHeight) {
                        uCrop = uCrop.withMaxResultSize(w, h);
                      }
                    }

                    cropLauncher.launch(uCrop.getIntent(requireContext()));
                  }

                  @Override
                  public void onPickFolderRequested() {
                    pickFolder();
                  }
                },
                originalWidth,
                originalHeight); // Pass original dimensions

        dialog.show(getParentFragmentManager(), "ResizeDialog");
      } else {
        throw new Exception("Failed to decode bitmap");
      }

    } catch (Exception e) {
      e.printStackTrace();
      Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
    }
  }

  private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
    final int height = options.outHeight;
    final int width = options.outWidth;
    int inSampleSize = 1;

    if (height > reqHeight || width > reqWidth) {
      final int halfHeight = height / 2;
      final int halfWidth = width / 2;

      // Calculate the largest inSampleSize that is a power of 2 and keeps both
      // height and width larger than the requested height and width.
      while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
        inSampleSize *= 2;
      }
    }

    return inSampleSize;
  }

  private int getMaterialColor(int attrResId) {
    TypedValue typedValue = new TypedValue();
    requireContext().getTheme().resolveAttribute(attrResId, typedValue, true);
    return typedValue.data;
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
        ImageUtils.resizeCompress(
            requireContext(),
            resultUri,
            targetW,
            targetH,
            quality,
            outFormat,
            keepExif,
            true,
            selectedFolderUri,
            fileName,
            maxFileSize,
            isSAFPath);

    if (info != null) {
      setupImageView(ivAfter);

      ivAfter.setImageBitmap(loadScaledBitmap(Uri.parse(info.outputUri), 720, 1280, new int[2]));
      tvInfoAfter.setText(
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

  private Bitmap loadScaledBitmap(Uri uri, int reqWidth, int reqHeight, int[] originalDimensions) {
    try {
      // Load image dimensions without decoding the full bitmap
      BitmapFactory.Options options = new BitmapFactory.Options();
      options.inJustDecodeBounds = true;
      InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
      BitmapFactory.decodeStream(inputStream, null, options);
      if (inputStream != null) {
        inputStream.close();
      }

      // Store original dimensions
      originalDimensions[0] = options.outWidth;
      originalDimensions[1] = options.outHeight;

      // Calculate inSampleSize
      options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
      options.inJustDecodeBounds = false;

      // Decode scaled bitmap
      inputStream = requireContext().getContentResolver().openInputStream(uri);
      Bitmap bmp = BitmapFactory.decodeStream(inputStream, null, options);
      if (inputStream != null) {
        inputStream.close();
      }
      return bmp;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  private int[] getScreenResolution() {
    Display defaultDisplay =
        ((WindowManager) requireContext().getSystemService(Context.WINDOW_SERVICE))
            .getDefaultDisplay();
    Point point = new Point();
    defaultDisplay.getRealSize(point);
    return new int[] {point.x, point.y};
  }
}
