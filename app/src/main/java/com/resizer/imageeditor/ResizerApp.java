package com.resizer.imageeditor;

import android.app.Application;
import android.content.Context;

public class ResizerApp extends Application {
  private static Context appContext;

  @Override
  public void onCreate() {
    super.onCreate();
    AppContext.init(this);
  }
}
