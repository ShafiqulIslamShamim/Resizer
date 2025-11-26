package com.resizer.imageeditor;

import android.content.*;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.*;
import android.os.Bundle;
import android.util.*;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

public class CrashActivity extends BaseActivity {

  private String crashLog;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    crashLog = getIntent().getStringExtra(Intent.EXTRA_TEXT);

    // Root Layout
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(getThemeColor(com.google.android.material.R.attr.colorSurface));

    // Toolbar
    MaterialToolbar toolbar = new MaterialToolbar(this);
    toolbar.setTitle("App Crash Report");
    toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
    toolbar.setNavigationOnClickListener(v -> onBackPressed());
    toolbar.inflateMenu(R.menu.crash_menu);
    toolbar.setBackgroundColor(getThemeColor(com.google.android.material.R.attr.colorSurface));
    toolbar.setNavigationOnClickListener(v -> restartApp());
    toolbar.setOnMenuItemClickListener(this::onMenuItemClick);

    // Crash log
    ScrollView scrollView = new ScrollView(this);
    scrollView.setFillViewport(true);
    HorizontalScrollView hScroll = new HorizontalScrollView(this);

    TextView textView = new TextView(this);
    int pad = dp2px(16);
    textView.setPadding(pad, pad, pad, pad);
    textView.setText(crashLog);
    textView.setTextIsSelectable(true);
    textView.setTextColor(getThemeColor(com.google.android.material.R.attr.colorOnSurface));

    hScroll.addView(textView);
    scrollView.addView(hScroll);

    // Bottom Buttons
    LinearLayout buttons = new LinearLayout(this);
    buttons.setOrientation(LinearLayout.HORIZONTAL);
    buttons.setPadding(pad, pad, pad, pad);
    buttons.setWeightSum(3);

    MaterialButton copyBtn =
        new MaterialButton(
            this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
    copyBtn.setText("Copy");
    copyBtn.setOnClickListener(v -> copyLog());

    MaterialButton shareBtn =
        new MaterialButton(
            this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
    shareBtn.setText("Share");
    shareBtn.setOnClickListener(v -> shareLog());

    MaterialButton restartBtn =
        new MaterialButton(
            this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
    restartBtn.setText("Restart");
    restartBtn.setOnClickListener(v -> restartApp());

    buttons.addView(
        copyBtn, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
    buttons.addView(
        shareBtn, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
    buttons.addView(
        restartBtn, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

    // Assemble
    root.addView(
        toolbar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp2px(56)));
    root.addView(
        scrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
    root.addView(
        buttons,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    setContentView(root);
  }

  private boolean onMenuItemClick(MenuItem item) {
    int id = item.getItemId();
    if (id == R.id.action_copy) {
      copyLog();
      return true;
    } else if (id == R.id.action_share) {
      shareLog();
      return true;
    } else if (id == R.id.action_restart) {
      restartApp();
      return true;
    }
    return false;
  }

  private void copyLog() {
    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
    cm.setPrimaryClip(ClipData.newPlainText(getPackageName(), crashLog));
    Snackbar.make(findViewById(android.R.id.content), "Crash log copied", Snackbar.LENGTH_SHORT)
        .show();
  }

  private void shareLog() {
    Intent shareIntent = new Intent(Intent.ACTION_SEND);
    shareIntent.setType("text/plain");
    shareIntent.putExtra(Intent.EXTRA_TEXT, crashLog);
    startActivity(Intent.createChooser(shareIntent, "Share Crash Log"));
  }

  private void restartApp() {
    Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
    if (intent != null) {
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
      startActivity(intent);
    }
    finish();
    android.os.Process.killProcess(android.os.Process.myPid());
    System.exit(0);
  }

  private int dp2px(float dpValue) {
    return (int) (dpValue * getResources().getDisplayMetrics().density + 0.5f);
  }

  private int getThemeColor(int attrResId) {
    TypedValue typedValue = new TypedValue();
    Resources.Theme theme = getTheme();
    if (theme.resolveAttribute(attrResId, typedValue, true)) {
      return typedValue.data;
    } else {
      Log.e("ThemeColors", "Attribute not found: " + attrResId);
      return 0xFF00FF; // fallback magenta
    }
  }
}
