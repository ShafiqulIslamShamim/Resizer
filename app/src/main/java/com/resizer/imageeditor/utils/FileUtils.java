package com.resizer.imageeditor.utils;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

public class FileUtils {

  @SuppressLint("NewApi")
  public static String getPathFromUri(final Context context, final Uri uri) {

    // DocumentProvider
    if (DocumentsContract.isDocumentUri(context, uri)) {

      // ExternalStorageProvider
      if (isExternalStorageDocument(uri)) {
        final String docId = DocumentsContract.getDocumentId(uri);
        final String[] split = docId.split(":");
        final String type = split[0];

        if ("primary".equalsIgnoreCase(type)) {
          return Environment.getExternalStorageDirectory() + "/" + split[1];
        }
        // TODO: handle non-primary volumes
      }

      // DownloadsProvider
      else if (isDownloadsDocument(uri)) {
        final String id = DocumentsContract.getDocumentId(uri);
        if (id != null && id.startsWith("raw:")) {
          return id.replaceFirst("raw:", "");
        }
        Uri contentUri =
            ContentUris.withAppendedId(
                Uri.parse("content://downloads/public_downloads"), Long.parseLong(id));

        return getDataColumn(context, contentUri, null, null);
      }

      // MediaProvider
      else if (isMediaDocument(uri)) {
        final String docId = DocumentsContract.getDocumentId(uri);
        final String[] split = docId.split(":");
        final String type = split[0];
        Uri contentUri = null;

        if ("image".equals(type)) {
          contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        } else if ("video".equals(type)) {
          contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        } else if ("audio".equals(type)) {
          contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }

        final String selection = "_id=?";
        final String[] selectionArgs = new String[] {split[1]};

        return getDataColumn(context, contentUri, selection, selectionArgs);
      }
    }
    // MediaStore (and general)
    else if ("content".equalsIgnoreCase(uri.getScheme())) {
      // Return the remote address
      return getDataColumn(context, uri, null, null);
    }
    // File
    else if ("file".equalsIgnoreCase(uri.getScheme())) {
      return uri.getPath();
    }

    return null;
  }

  private static String getDataColumn(
      Context context, Uri uri, String selection, String[] selectionArgs) {

    Cursor cursor = null;
    final String column = "_data";
    final String[] projection = {column};

    try {
      cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
      if (cursor != null && cursor.moveToFirst()) {
        final int column_index = cursor.getColumnIndexOrThrow(column);
        return cursor.getString(column_index);
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (cursor != null) cursor.close();
    }
    return null;
  }

  private static boolean isExternalStorageDocument(Uri uri) {
    return "com.android.externalstorage.documents".equals(uri.getAuthority());
  }

  private static boolean isDownloadsDocument(Uri uri) {
    return "com.android.providers.downloads.documents".equals(uri.getAuthority());
  }

  private static boolean isMediaDocument(Uri uri) {
    return "com.android.providers.media.documents".equals(uri.getAuthority());
  }
}
