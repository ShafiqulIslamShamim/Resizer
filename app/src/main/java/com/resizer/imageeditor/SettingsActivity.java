package com.resizer.imageeditor;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

public class SettingsActivity extends AppCompatActivity {

  private static final String EXTRA_PREF_KEY = "pref_key";
  private static final String EXTRA_PREF_TITLE = "pref_title";
  private static final String EXTRA_PARENT_KEY = "parent_key";
  private static final String EXTRA_PARENT_TITLE = "parent_title";
  private static final String PREF_CHANGE_FLAG = "preference_changed";

  private Toolbar toolbar;

  public static Intent createIntent(
      Context context, String prefKey, String prefTitle, String parentKey, String parentTitle) {
    Intent intent = new Intent(context, SettingsActivity.class);
    intent.putExtra(EXTRA_PREF_KEY, prefKey);
    intent.putExtra(EXTRA_PREF_TITLE, prefTitle);
    intent.putExtra(EXTRA_PARENT_KEY, parentKey);
    intent.putExtra(EXTRA_PARENT_TITLE, parentTitle);
    return intent;
  }

  public static Intent createRootIntent(Context context) {
    return createIntent(context, "main_settings", "Settings", null, null);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // ✅ Enable edge-to-edge
    WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
    applyLocalTheme();
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_settings);

    // ✅ Apply insets to root view
    View rootView = findViewById(android.R.id.content);
    ViewCompat.setOnApplyWindowInsetsListener(
        rootView,
        (v, insets) -> {
          Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
          v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
          return insets;
        });

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    prefs.edit().putBoolean(PREF_CHANGE_FLAG, false).apply();

    toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    if (getSupportActionBar() != null) {
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
      getSupportActionBar().setDisplayShowTitleEnabled(true);
    }

    String prefKey = getIntent().getStringExtra(EXTRA_PREF_KEY);
    String prefTitle = getIntent().getStringExtra(EXTRA_PREF_TITLE);

    if (getSupportActionBar() != null) {
      getSupportActionBar().setTitle(prefTitle != null ? prefTitle : "Settings");
    }

    if (savedInstanceState == null) {
      SettingsFragment fragment = new SettingsFragment();
      Bundle args = new Bundle();
      args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, prefKey);
      fragment.setArguments(args);
      getSupportFragmentManager()
          .beginTransaction()
          .replace(R.id.settings_container, fragment)
          .commit();
    }

    // Re-set title just in case
    if (getSupportActionBar() != null) {
      getSupportActionBar().setTitle(prefTitle != null ? prefTitle : "Settings");
    }
  }

  private void applyLocalTheme() {
    String themePref = SharedPrefValues.getValue("theme_preference", "0");
    switch (themePref) {
      case "2":
        setTheme(R.style.AppThemeDark);
        break;
      case "3":
        setTheme(R.style.AppThemeLight);
        break;
      default:
        setTheme(R.style.AppTheme);
        break;
    }
  }

  @Override
  public boolean onSupportNavigateUp() {
    onBackPressed();
    return true;
  }

  @Override
  public void onBackPressed() {
    String parentKey = getIntent().getStringExtra(EXTRA_PARENT_KEY);
    if (parentKey == null) {
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
      boolean prefChanged = prefs.getBoolean(PREF_CHANGE_FLAG, false);
      if (prefChanged) {
        prefs.edit().putBoolean(PREF_CHANGE_FLAG, false).apply();
        Intent intent = new Intent(this, TabbedActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
      } else {
        super.onBackPressed();
      }
    } else {
      super.onBackPressed();
    }
  }

  public static class SettingsFragment extends PreferenceFragmentCompat
      implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
      setPreferencesFromResource(R.xml.preferences, rootKey);

      PreferenceScreen root = getPreferenceScreen();
      setupPreferenceScreenListeners(root);

      Preference aboutInfoPreference = findPreference("pref_about_info_key");
      if (aboutInfoPreference != null) {
        aboutInfoPreference.setOnPreferenceClickListener(
            preference -> {
              // You can launch an AboutActivity or dialog here if you want
              return true;
            });
      }
    }

    @Override
    public void onResume() {
      super.onResume();
      PreferenceManager.getDefaultSharedPreferences(requireContext())
          .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
      super.onPause();
      PreferenceManager.getDefaultSharedPreferences(requireContext())
          .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
      sharedPreferences.edit().putBoolean(PREF_CHANGE_FLAG, true).apply();
    }

    private void setupPreferenceScreenListeners(PreferenceScreen preferenceScreen) {
      String parentKey = preferenceScreen.getKey();
      String parentTitle =
          preferenceScreen.getTitle() != null ? preferenceScreen.getTitle().toString() : "Settings";

      for (int i = 0; i < preferenceScreen.getPreferenceCount(); i++) {
        Preference preference = preferenceScreen.getPreference(i);
        if (preference instanceof PreferenceScreen) {
          PreferenceScreen subScreen = (PreferenceScreen) preference;
          subScreen.setOnPreferenceClickListener(
              p -> {
                Intent intent =
                    createIntent(
                        requireContext(),
                        p.getKey(),
                        p.getTitle().toString(),
                        parentKey,
                        parentTitle);
                requireActivity().startActivity(intent);
                return true;
              });
          setupPreferenceScreenListeners(subScreen);
        }
      }
    }
  }
}
