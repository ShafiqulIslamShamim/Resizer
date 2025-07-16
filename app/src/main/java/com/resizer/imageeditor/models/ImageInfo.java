package com.resizer.imageeditor.models;

public class ImageInfo {
  public int width;
  public int height;
  public int sizeKb;
  public String format;
  public String outputPath;

  public ImageInfo() {}

  public ImageInfo(int width, int height, int sizeKb, String format, String outputPath) {
    this.width = width;
    this.height = height;
    this.sizeKb = sizeKb;
    this.format = format;
    this.outputPath = outputPath;
  }
}
