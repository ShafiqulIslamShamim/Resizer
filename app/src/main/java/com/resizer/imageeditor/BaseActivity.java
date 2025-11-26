package com.resizer.imageeditor;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public abstract class BaseActivity extends AppCompatActivity {

  private final SharedPrefValuesBase.OnPrefChangeListener themeListener =
      (key, newValue) -> {
        if ("theme_preference".equals(key)) {
          // Ensure this runs on UI thread
          runOnUiThread(this::onThemePreferenceChanged);
        }
      };

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    // Apply theme BEFORE super.onCreate()
    applyLocalTheme();
    super.onCreate(savedInstanceState);

    // Enable edge-to-edge layout always
    WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
  }

  @Override
  protected void onStart() {
    super.onStart();
    // Register listener so runtime changes propagate to activities
    SharedPrefValuesBase.addListener(themeListener);
  }

  @Override
  protected void onStop() {
    super.onStop();
    // Unregister to avoid leaks
    SharedPrefValuesBase.removeListener(themeListener);
  }

  @Override
  public void setContentView(int layoutResID) {
    super.setContentView(layoutResID);
    afterContentSet();
  }

  @Override
  public void setContentView(View view) {
    super.setContentView(view);
    afterContentSet();
  }

  private void afterContentSet() {
    setupEdgeToEdgePadding();

    /*   String themePref = SharedPrefValues.getValue("theme_preference", "0");
    if ("0".equals(themePref)) {
        // Follow system -> let EdgeToEdge handle appearance automatically
        EdgeToEdge.enable(this);
    } else {
    */
    // Manual: set status/nav bar icon colors based on chosen theme
    getWindow().getDecorView().post(this::applySystemBarAppearance);
  }

  // Apply theme from prefs (must be called before super.onCreate)
  protected void applyLocalTheme() {
    String themePref = SharedPrefValues.getValue("theme_preference", "0");
    switch (themePref) {
      case "2": // Dark
        setTheme(R.style.AppThemeDark);
        break;
      case "3": // Light
        setTheme(R.style.AppThemeLight);
        break;
      default: // Follow system
        setTheme(R.style.AppTheme);
        break;
    }
  }

  // Manual system bar appearance (for non-follow-system)
  protected void applySystemBarAppearance() {
    boolean isLight = isLightThemeActive();

    View decorView = getWindow().getDecorView();
    WindowInsetsControllerCompat controller =
        new WindowInsetsControllerCompat(getWindow(), decorView);

    controller.setAppearanceLightStatusBars(isLight);
    controller.setAppearanceLightNavigationBars(isLight);
  }

  private void setupEdgeToEdgePadding() {
    View root = findViewById(android.R.id.content);
    if (root == null) return;

    ViewCompat.setOnApplyWindowInsetsListener(
        root,
        (v, insets) -> {
          Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
          v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
          return insets;
        });
  }

  private boolean isLightThemeActive() {
    String themePref = SharedPrefValues.getValue("theme_preference", "0");
    switch (themePref) {
      case "2": // Dark
        return false;
      case "3": // Light
        return true;
      default:
        int mode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return mode != Configuration.UI_MODE_NIGHT_YES;
    }
  }

  /**
   * Called when theme_preference changes at runtime. Default: recreate() to apply theme cleanly.
   * Override in a concrete activity if you want different behavior.
   */
  protected void onThemePreferenceChanged() {
    recreate();
  }

  protected void showToast(String message) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
  }
}
