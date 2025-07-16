package com.resizer.imageeditor;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

public class AboutInfoPreference extends Preference {

  public AboutInfoPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    setLayoutResource(R.layout.activity_info); // তুমি যে layout শেয়ার করেছো
  }

  @Override
  public void onBindViewHolder(PreferenceViewHolder holder) {
    super.onBindViewHolder(holder);

    View facebook = holder.findViewById(R.id.facebook);
    View github = holder.findViewById(R.id.github);
    View cardUpdate = holder.findViewById(R.id.update_card);
    View card = holder.findViewById(R.id.about_card);
    if (cardUpdate != null) {
      cardUpdate.setOnClickListener(
          v -> {
            OTAUpdateHelper.hookPreference(getContext());
          });
    }

    if (card != null) {
      card.setOnClickListener(
          v -> {
            getContext()
                .startActivity(
                    IntentUtils.openUrl(
                        getContext(), "https://t.me/md_shamim12"));
          });
    }

    if (facebook != null) {
      facebook.setOnClickListener(
          v -> {
            // facebook link খোলার intent
            getContext()
                .startActivity(
                    IntentUtils.openUrl(
                        getContext(), "https://www.facebook.com/share/18wbmDDERe/"));
          });
    }

    if (github != null) {
      github.setOnClickListener(
          v -> {
            // github link খোলার intent
            getContext()
                .startActivity(IntentUtils.openUrl(getContext(), "https://github.com/ShafiqulIslamShamim/"));
          });
    }
  }
}
