package com.resizer.imageeditor;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
    super.onCreate(savedInstanceState);
    ActivityContext = this;
    applyLocalTheme(); // 👈 Apply local theme

    OTAUpdateHelper.checkForUpdatesIfDue(this);

    boolean logcat = SharedPrefValues.getValue("enable_logcat", false);
    if (logcat) {

      if (!StoragePermissionHelper.isPermissionGranted(this)) {
        StoragePermissionHelper.checkAndRequestStoragePermission(this);
        LogcatSaver.RunLog();
      }
    }

    setContentView(R.layout.activity_tabbed);

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

    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {

    int settingsId = getResources().getIdentifier("settings", "id", getPackageName());

    if (item.getItemId() == settingsId) {
      startActivity(new Intent(this, SettingsActivity.class));
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
