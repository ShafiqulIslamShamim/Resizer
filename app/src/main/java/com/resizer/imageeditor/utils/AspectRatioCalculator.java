package com.resizer.imageeditor.utils;

public class AspectRatioCalculator {

  public static float[] calculateAspectRatio(int width, int height) {
    int gcd = findGCD(width, height);
    float ratioWidth = (float) width / gcd;
    float ratioHeight = (float) height / gcd;

    return new float[] {ratioWidth, ratioHeight};
  }

  // GCD নির্ণয়ের ফাংশন
  private static int findGCD(int a, int b) {
    if (b == 0) return a;
    return findGCD(b, a % b);
  }

  /*   // উদাহরণ ব্যবহার
  public static void main(String[] args) {
      int imageWidth = 1280;
      int imageHeight = 720;

      float[] aspectRatio = calculateAspectRatio(imageWidth, imageHeight);
      System.out.println("Aspect Ratio: " + aspectRatio[0] + ":" + aspectRatio[1]);
  }
  */
}
