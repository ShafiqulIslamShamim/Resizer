package com.resizer.imageeditor;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class IntentUtils {
  public static Intent openUrl(Context context, String url) {
    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    return intent;
  }
}
