package com.resizer.imageeditor;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class TabbedActivity extends AppCompatActivity {

  private static final String TAG = "TabbedActivity";

  public static TabbedActivity ActivityContext;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // ✅ Enable edge-to-edge
    WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
    applyLocalTheme();
    super.onCreate(savedInstanceState);
    ActivityContext = this;

    OTAUpdateHelper.checkForUpdatesIfDue(this);

    boolean logcat = SharedPrefValues.getValue("enable_logcat", false);
    if (logcat) {

      if (!StoragePermissionHelper.isPermissionGranted(this)) {
        StoragePermissionHelper.checkAndRequestStoragePermission(this);
        LogcatSaver.RunLog();
      }
    }

    setContentView(R.layout.activity_tabbed);

    // ✅ Apply insets to root view
    View rootView = findViewById(android.R.id.content);
    ViewCompat.setOnApplyWindowInsetsListener(
        rootView,
        (v, insets) -> {
          Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
          v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
          return insets;
        });

    MaterialToolbar toolbar = findViewById(R.id.topAppBar);
    setSupportActionBar(toolbar);

    TabLayout tabLayout = findViewById(R.id.tabLayout);
    ViewPager2 vp = findViewById(R.id.viewPager);
    vp.setAdapter(
        new FragmentStateAdapter(this) {
          @NonNull
          @Override
          public Fragment createFragment(int pos) {
            return pos == 0 ? new EditorFragment() : new CropFragment();
          }

          @Override
          public int getItemCount() {
            return 2;
          }
        });
    new TabLayoutMediator(tabLayout, vp, (tab, pos) -> tab.setText(pos == 0 ? "Editor" : "Crop"))
        .attach();
  }

  private void applyLocalTheme() {
    String themePref = SharedPrefValues.getValue("theme_preference", "0");
    switch (themePref) {
      case "2": // Dark
        setTheme(R.style.AppThemeDark);
        break;
      case "3": // Light
        setTheme(R.style.AppThemeLight);
        break;
      default: // System/default
        setTheme(R.style.AppTheme);
        break;
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    int menuId = getResources().getIdentifier("main_menu", "menu", getPackageName());
    if (menuId == 0) {
      Log.e(TAG, "Menu resource 'main_menu' not found");
      return false;
    }

    getMenuInflater().inflate(menuId, menu);

    // Settings button
    int settingsId = getResources().getIdentifier("settings", "id", getPackageName());
    int settingsIconId = getResources().getIdentifier("ic_settings", "drawable", getPackageName());
    if (settingsId != 0 && settingsIconId != 0) {
      menu.findItem(settingsId).setIcon(settingsIconId);
    } else {
      Log.e(TAG, "Menu item 'settings' or drawable 'ic_settings' not found");
    }

    // Reset button
    int ResetId = getResources().getIdentifier("action_reset", "id", getPackageName());
    int ResetIconId = getResources().getIdentifier("ic_reset", "drawable", getPackageName());
    if (ResetId != 0 && ResetIconId != 0) {
      menu.findItem(ResetId).setIcon(ResetIconId);
    } else {
      Log.e(TAG, "Menu item 'Reset' or drawable 'ic_reset' not found");
    }

    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {

    int id = item.getItemId();

    int settingsId = getResources().getIdentifier("settings", "id", getPackageName());

    if (id == settingsId) {
      startActivity(new Intent(this, SettingsActivity.class));
      return true;
    }

    if (id == R.id.action_reset) {
      // Restart the activity
      Intent intent = new Intent(this, TabbedActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
      finish(); // Close the current instance
      startActivity(intent); // Start a new instance
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == StoragePermissionHelper.REQUEST_CODE_MANAGE_STORAGE) {
      boolean logcat = SharedPrefValues.getValue("enable_logcat", false);
      if (logcat && StoragePermissionHelper.isPermissionGranted(this)) {
        LogcatSaver.RunLog();
      }
    }
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == StoragePermissionHelper.REQUEST_CODE_OLD_STORAGE) {
      boolean logcat = SharedPrefValues.getValue("enable_logcat", false);
      if (logcat && StoragePermissionHelper.isPermissionGranted(this)) {
        LogcatSaver.RunLog();
      }
    }
  }
}
