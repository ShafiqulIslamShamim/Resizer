package com.resizer.imageeditor;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.text.Html;
import android.text.Spanned;
import android.util.Base64;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog; // Use AppCompat AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OTAUpdateHelper {

  public static boolean isInternetAvailable(@NonNull Context context) {
    ConnectivityManager cm =
        (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    if (cm != null) {
      NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
      return activeNetwork != null && activeNetwork.isConnected();
    }
    return false;
  }

  public static String getVersionName(Context context) {
    try {
      return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
    } catch (Exception e) {
      e.printStackTrace();
      return "Unknown";
    }
  }

  public static void hookPreference(Context context) {
    new UpdateCheckExecutor(context, false).run();
  }

  public static void checkForUpdatesIfDue(Context context) {
    if (isInternetAvailable(context)) {
      final String PREF_NAME = "update_pref";
      final String KEY_LAST_CHECK = "last_check_time";
      final long THREE_DAYS_MILLIS = 3L * 24 * 60 * 60 * 1000;

      SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
      long lastCheck = prefs.getLong(KEY_LAST_CHECK, 0);
      long currentTime = System.currentTimeMillis();

      if (currentTime - lastCheck >= THREE_DAYS_MILLIS) {
        prefs.edit().putLong(KEY_LAST_CHECK, currentTime).apply();
        new UpdateCheckExecutor(context, true).run();
      }
    }
  }

  private static class UpdateCheckExecutor {
    private final Context context;
    private final boolean autoCheck;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public UpdateCheckExecutor(Context context, boolean autoCheck) {
      this.context = context;
      this.autoCheck = autoCheck;
    }

    public void run() {
      if (!autoCheck && !isInternetAvailable(context)) {
        Toast.makeText(context, "No internet connection available.", Toast.LENGTH_LONG).show();
        return;
      }

      if (!autoCheck) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 40);
        CircularProgressIndicator progress = new CircularProgressIndicator(context);
        progress.setIndeterminate(true);
        TextView message = new TextView(context);
        message.setText("Checking for updates...");
        layout.addView(progress);
        layout.addView(message);

        AlertDialog progressDialog =
            new MaterialAlertDialogBuilder(context)
                //  .setTitle("Please wait")
                .setCustomTitle(DialogUtils.createStyledDialogTitle(context, "Please wait"))
                .setView(layout)
                .setCancelable(false)
                .create();
        progressDialog.show();
        executor.submit(() -> fetchUpdate(progressDialog));
      } else {
        executor.submit(() -> fetchUpdate(null));
      }
    }

    private void fetchUpdate(AlertDialog progressDialog) {
      try {
        String encodedUrl =
            "aHR0cHM6Ly9yYXcuZ2l0aHVidXNlcmNvbnRlbnQuY29tL1NoYWZpcXVsSXNsYW1TaGFtaW0vUmVzaXplci9tYWluL1VwZGF0ZUNoZWNrZXJJbmZvLnR4dA==";
        String textUrl =
            new String(Base64.decode(encodedUrl, Base64.DEFAULT), StandardCharsets.UTF_8);
        HttpURLConnection connection = (HttpURLConnection) new URL(textUrl).openConnection();
        connection.setRequestMethod("GET");

        BufferedReader reader =
            new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
          result.append(line).append("\n");
        }
        reader.close();

        String fetchedText = result.toString();
        ((android.app.Activity) context)
            .runOnUiThread(() -> handleUpdate(fetchedText, progressDialog));

      } catch (Exception e) {
        e.printStackTrace();
        ((android.app.Activity) context)
            .runOnUiThread(
                () -> {
                  if (!autoCheck)
                    Toast.makeText(context, "Failed to check for update", Toast.LENGTH_LONG).show();
                  if (progressDialog != null && progressDialog.isShowing())
                    progressDialog.dismiss();
                });
      }
    }

    private void handleUpdate(String text, AlertDialog progressDialog) {
      if (progressDialog != null && progressDialog.isShowing()) {
        progressDialog.dismiss();
      }

      if (text.isEmpty()) {
        if (!autoCheck) showToast("Failed to retrieve update info");
        return;
      }

      final ArrayList<String> parts = parseBrackets(text);
      if (parts.size() < 3) {
        if (!autoCheck) showToast("Update info is incomplete or malformed.");
        return;
      }

      String remoteMainVersion = parts.get(0);
      String remoteVersion = parts.get(1);
      String changelog = parts.get(2);
      String localVersion = getVersionName(context);

      if (compareVersions(localVersion, remoteVersion) >= 0) {
        if (!autoCheck) {
          new MaterialAlertDialogBuilder(context)
              //  .setTitle("No Update Available")
              .setCustomTitle(DialogUtils.createStyledDialogTitle(context, "No Update Available"))
              .setMessage("You are up to date.")
              .setPositiveButton("OK", null)
              .setCancelable(false)
              .show();
        }
      } else {
        MaterialAlertDialogBuilder changelogBuilder =
            new MaterialAlertDialogBuilder(context)
                //  .setTitle("Update Available: " + remoteVersion);
                .setCustomTitle(
                    DialogUtils.createStyledDialogTitle(
                        context, "Update Available: " + remoteVersion));

        if (HtmlDetector.isHtml(changelog)) {
          Spanned formatted;
          if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            formatted = Html.fromHtml(changelog, Html.FROM_HTML_MODE_LEGACY);
          } else {
            formatted = Html.fromHtml(changelog);
          }
          changelogBuilder.setMessage(formatted);
        } else {
          changelogBuilder.setMessage(changelog);
        }

        final ArrayList<String> downloadLinks = new ArrayList<>(parts.subList(3, parts.size()));
        ArrayList<String> labels = new ArrayList<>();
        int mirrorCount = 1;

        for (String link : downloadLinks) {
          String label;
          if (link.contains("t.me")) {
            label = "SGCam Official(TG) (Mirror " + mirrorCount + ") (Fast)";
          } else if (link.contains("celsoazevedo.com")) {
            label = "Celso (Mirror " + mirrorCount + ")";
          } else if (link.contains("gcambrasil.com")) {
            label = "GcamBrazil (Mirror " + mirrorCount + ")";
          } else if (link.contains("apkw.ru")) {
            label = "Apkw (Mirror " + mirrorCount + ")";
          } else if (link.contains("dropbox.com")) {
            label = "Dropbox";
          } else if (link.contains("github.com")) {
            label = "GitHub Release";
          } else {
            label = "Direct Download (Mirror " + mirrorCount + ")";
          }
          labels.add(label);
          mirrorCount++;
        }

        changelogBuilder.setPositiveButton(
            "Download now",
            (dialog, which) -> {
              new MaterialAlertDialogBuilder(context)
                  // .setTitle("Select source")
                  .setCustomTitle(DialogUtils.createStyledDialogTitle(context, "Select source"))
                  .setSingleChoiceItems(
                      labels.toArray(new String[0]),
                      -1,
                      (dialog1, index) -> {
                        openInBrowser(context, downloadLinks.get(index));
                        dialog1.dismiss();
                      })
                  .setNegativeButton("Cancel", null)
                  .show();
            });

        changelogBuilder.setNegativeButton("Cancel", null);
        changelogBuilder.setCancelable(false);
        changelogBuilder.show();
      }
    }

    private void showToast(String message) {
      Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
  }

  public static void openInBrowser(Context context, String url) {
    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    if (intent.resolveActivity(context.getPackageManager()) != null) {
      context.startActivity(intent);
    } else {
      Toast.makeText(context, "Unable to open the link.", Toast.LENGTH_LONG).show();
    }
  }

  private static ArrayList<String> parseBrackets(String text) {
    ArrayList<String> results = new ArrayList<>();
    int start = -1;
    for (int i = 0; i < text.length(); i++) {
      if (text.charAt(i) == '[') {
        start = i + 1;
      } else if (text.charAt(i) == ']' && start != -1) {
        String content = text.substring(start, i).trim();
        if (!content.isEmpty()) {
          results.add(content);
        }
        start = -1;
      }
    }
    return results;
  }

  public static int compareVersions(String v1, String v2) {
    String[] parts1 = v1.split("\\.");
    String[] parts2 = v2.split("\\.");

    int length = Math.max(parts1.length, parts2.length);
    for (int i = 0; i < length; i++) {
      int p1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
      int p2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;

      if (p1 < p2) return -1;
      if (p1 > p2) return 1;
    }
    return 0;
  }
}
