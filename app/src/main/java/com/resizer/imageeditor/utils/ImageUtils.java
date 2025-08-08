package com.resizer.imageeditor.utils;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.documentfile.provider.DocumentFile;
import androidx.exifinterface.media.ExifInterface;
import com.resizer.imageeditor.models.ImageInfo;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

public class ImageUtils {

  public static int getFileSizeKb(Context context, Uri uri) {
    try (InputStream input = context.getContentResolver().openInputStream(uri)) {
      if (input != null) {
        return input.available() / 1024;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return 0;
  }

  public static ImageInfo resizeCompress(
      Context context,
      Uri uri,
      int targetW,
      int targetH,
      int quality,
      String format,
      boolean keepExif,
      boolean save,
      Uri folderUri,
      String fileName,
      String sizeLimitKbStr,
      boolean isSAFPathEnabled) {
    try {
      BitmapFactory.Options options = new BitmapFactory.Options();
      InputStream input = context.getContentResolver().openInputStream(uri);
      Bitmap bitmap = BitmapFactory.decodeStream(input, null, options);
      if (input != null) input.close();

      if (bitmap == null) throw new Exception("Failed to decode image.");

      if (targetW > 0 && targetH > 0) {
        bitmap = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true);
      }

      Bitmap.CompressFormat compressFormat = getCompressFormat(format);
      String extension = getExtension(format);
      String mimeType = getMimeType(format);

      if (!save) {
        if (sizeLimitKbStr != null && !sizeLimitKbStr.isEmpty()) {
          int targetSizeKb = Integer.parseInt(sizeLimitKbStr);
          quality = adjustQualityForSize(bitmap, compressFormat, targetSizeKb, quality);
        }
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(compressFormat, quality, stream);
        int sizeKb = stream.toByteArray().length / 1024;
        return new ImageInfo(bitmap.getWidth(), bitmap.getHeight(), sizeKb, format, "", "");
      }

      Uri savedUri = null;

      if (isSAFPathEnabled == true && folderUri != null) {
        DocumentFile pickedDir = DocumentFile.fromTreeUri(context, folderUri);
        if (pickedDir != null && pickedDir.canWrite()) {
          DocumentFile newFile = pickedDir.createFile(mimeType, fileName + extension);
          if (newFile != null) {
            savedUri = newFile.getUri();
            OutputStream out = context.getContentResolver().openOutputStream(savedUri);

            if (sizeLimitKbStr != null && !sizeLimitKbStr.isEmpty()) {
              int targetSizeKb = Integer.parseInt(sizeLimitKbStr);
              quality = adjustQualityForSize(bitmap, compressFormat, targetSizeKb, quality);
            }

            bitmap.compress(compressFormat, quality, out);
            if (out != null) out.close();
          }
        }
      }

      // ✅ Save to MediaStore if SAF not used
      if (savedUri == null) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName + extension);
        values.put(MediaStore.Images.Media.MIME_TYPE, mimeType);
        values.put(
            MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Resized");

        Uri imageUri =
            context
                .getContentResolver()
                .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (imageUri != null) {
          OutputStream out = context.getContentResolver().openOutputStream(imageUri);

          if (sizeLimitKbStr != null && !sizeLimitKbStr.isEmpty()) {
            int targetSizeKb = Integer.parseInt(sizeLimitKbStr);
            quality = adjustQualityForSize(bitmap, compressFormat, targetSizeKb, quality);
          }

          bitmap.compress(compressFormat, quality, out);
          if (out != null) out.close();
          savedUri = imageUri;
        }
      }

      if (keepExif && "JPEG".equalsIgnoreCase(format)) {
        copyExifData(context, uri, savedUri);
      }

      int savedSizeKb = getFileSizeKb(context, savedUri);
      return new ImageInfo(
          bitmap.getWidth(),
          bitmap.getHeight(),
          savedSizeKb,
          format,
          FileUtils.getPathFromUri(context, savedUri),
          savedUri.toString());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public static ImageInfo resizeCompressCropActivity(
      Context context,
      Uri uri,
      int quality,
      String format,
      boolean keepExif,
      boolean save,
      Uri folderUri,
      String fileName,
      boolean isSAFPathEnabled) {
    try {
      BitmapFactory.Options options = new BitmapFactory.Options();
      InputStream input = context.getContentResolver().openInputStream(uri);
      Bitmap bitmap = BitmapFactory.decodeStream(input, null, options);
      if (input != null) input.close();

      if (bitmap == null) throw new Exception("Failed to decode image.");

      Bitmap.CompressFormat compressFormat = getCompressFormat(format);
      String extension = getExtension(format);
      String mimeType = getMimeType(format);

      if (!save) {
        int sizeKb = bitmap.getByteCount() / 1024;
        return new ImageInfo(bitmap.getWidth(), bitmap.getHeight(), sizeKb, format, "", "");
      }

      Uri savedUri = null;

      if (isSAFPathEnabled == true && folderUri != null) {
        DocumentFile pickedDir = DocumentFile.fromTreeUri(context, folderUri);
        if (pickedDir != null && pickedDir.canWrite()) {
          DocumentFile newFile = pickedDir.createFile(mimeType, fileName + extension);
          if (newFile != null) {
            savedUri = newFile.getUri();
            OutputStream out = context.getContentResolver().openOutputStream(savedUri);
            bitmap.compress(compressFormat, quality, out);
            if (out != null) out.close();
          }
        }
      }

      // ✅ Save to MediaStore if SAF not used
      if (savedUri == null) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName + extension);
        values.put(MediaStore.Images.Media.MIME_TYPE, mimeType);
        values.put(
            MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Resized");

        Uri imageUri =
            context
                .getContentResolver()
                .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (imageUri != null) {
          OutputStream out = context.getContentResolver().openOutputStream(imageUri);
          bitmap.compress(compressFormat, quality, out);
          if (out != null) out.close();
          savedUri = imageUri;
        }
      }

      if (keepExif && "JPEG".equalsIgnoreCase(format)) {
        copyExifData(context, uri, savedUri);
      }

      int savedSizeKb = getFileSizeKb(context, savedUri);
      return new ImageInfo(
          bitmap.getWidth(),
          bitmap.getHeight(),
          savedSizeKb,
          format,
          FileUtils.getPathFromUri(context, savedUri),
          savedUri.toString());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  private static int adjustQualityForSize(
      Bitmap bitmap, Bitmap.CompressFormat format, int targetSizeKb, int initialQuality) {
    int minQuality = 10;
    int maxQuality = initialQuality;
    int bestQuality = minQuality;
    int low = minQuality;
    int high = maxQuality;

    while (low <= high) {
      int midQuality = (low + high) / 2;
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      bitmap.compress(format, midQuality, baos);
      int sizeKb = baos.toByteArray().length / 1024;

      if (sizeKb > targetSizeKb) {
        high = midQuality - 1;
      } else {
        bestQuality = midQuality;
        low = midQuality + 1;
      }
    }
    return bestQuality;
  }

  private static Bitmap.CompressFormat getCompressFormat(String format) {
    if ("PNG".equalsIgnoreCase(format)) {
      return Bitmap.CompressFormat.PNG;
    } else if ("WEBP".equalsIgnoreCase(format)) {
      return Bitmap.CompressFormat.WEBP;
    } else {
      return Bitmap.CompressFormat.JPEG;
    }
  }

  public static String getExtension(String format) {
    if ("PNG".equalsIgnoreCase(format)) return ".png";
    if ("WEBP".equalsIgnoreCase(format)) return ".webp";
    return ".jpg";
  }

  private static String getMimeType(String format) {
    if ("PNG".equalsIgnoreCase(format)) return "image/png";
    if ("WEBP".equalsIgnoreCase(format)) return "image/webp";
    return "image/jpeg";
  }

  private static void copyExifData(Context context, Uri srcUri, Uri destUri) {
    try {
      InputStream srcInput = context.getContentResolver().openInputStream(srcUri);
      if (srcInput == null) return;

      ExifInterface originalExif = new ExifInterface(srcInput);
      srcInput.close();

      ExifInterface newExif;
      if ("file".equals(destUri.getScheme())) {
        newExif = new ExifInterface(new File(destUri.getPath()));
      } else {
        newExif = new ExifInterface(context.getContentResolver().openInputStream(destUri));
      }

      String[] tags = {
        ExifInterface.TAG_MAKE,
        ExifInterface.TAG_MODEL,
        ExifInterface.TAG_DATETIME,
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.TAG_F_NUMBER,
        ExifInterface.TAG_ISO_SPEED_RATINGS,
        ExifInterface.TAG_EXPOSURE_TIME,
        ExifInterface.TAG_GPS_LATITUDE,
        ExifInterface.TAG_GPS_LONGITUDE
      };

      for (String tag : tags) {
        String value = originalExif.getAttribute(tag);
        if (value != null) {
          newExif.setAttribute(tag, value);
        }
      }
      newExif.saveAttributes();
    } catch (Exception ignored) {
    }
  }
}
