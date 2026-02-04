// ignore_for_file: avoid_print, constant_identifier_names

import 'dart:math';

import 'package:get/get.dart';
import 'dart:ui';

class SC {
  static const double SCREEN_320 = 320.0;
  static const double SCREEN_360 = 360.0;
  static const double SCREEN_375 = 375.0;
  static const double SCREEN_390 = 390.0;
  static const double SCREEN_414 = 414.0;
  static const double SCREEN_428 = 428.0;

  static late double s1;
  static late double s2;
  static late double s3;
  static late double s4;
  static late double s5;
  static late double s6;
  static late double s7;
  static late double s8;
  static late double s9;
  static late double s10;
  static late double s11;
  static late double s12;
  static late double s13;
  static late double s14;
  static late double s15;
  static late double s16;
  static late double s17;
  static late double s18;
  static late double s19;
  static late double s20;
  static late double s21;
  static late double s22;
  static late double s23;
  static late double s24;
  static late double s25;
  static late double s26;
  static late double s27;
  static late double s28;
  static late double s29;
  static late double s30;

  static double scale = 1.0;
  static double scaleHeight = 1.0;
  static double tileScale = 0.855;
  static bool isSmallHeight = false;

  static double s(double val) => val * scale;
  static double sh(double val) => val * scaleHeight;

  static late double bottomPadding;

  static late double initialBottomPadding;

  static late double textScaleFactor;

  static void initValues(double width) {
    if (width < SCREEN_360) {
      scale = 320.0 / 375.0;
    } else if (width < SCREEN_375) {
      scale = 360.0 / 375.0;
    } else {
      scale = 1.0;
    }

    s1 = 1.0 * scale;
    s2 = 2.0 * scale;
    s3 = 3.0 * scale;
    s4 = 4.0 * scale;
    s5 = 5.0 * scale;
    s6 = 6.0 * scale;
    s7 = 7.0 * scale;
    s8 = 8.0 * scale;
    s9 = 9.0 * scale;
    s10 = 10.0 * scale;
    s11 = 11.0 * scale;
    s12 = 12.0 * scale;
    s13 = 13.0 * scale;
    s14 = 14.0 * scale;
    s15 = 15.0 * scale;
    s16 = 16.0 * scale;
    s17 = 17.0 * scale;
    s18 = 18.0 * scale;
    s19 = 19.0 * scale;
    s20 = 20.0 * scale;
    s21 = 21.0 * scale;
    s22 = 22.0 * scale;
    s23 = 23.0 * scale;
    s24 = 24.0 * scale;
    s25 = 25.0 * scale;
    s26 = 26.0 * scale;
    s27 = 27.0 * scale;
    s28 = 28.0 * scale;
    s29 = 29.0 * scale;
    s30 = 30.0 * scale;

    if (width <= SCREEN_360) {
      scaleHeight = 0.85;
    } else if (width <= SCREEN_375) {
      scaleHeight = 0.96;
    } else if (width <= SCREEN_390) {
      scaleHeight = 1;
    } else if (width <= SCREEN_414) {
      scaleHeight = 1.01;
    } else if (width <= SCREEN_428) {
      scaleHeight = 1.03;
    } else if (width > SCREEN_428) {
      scaleHeight = 1.06;
    }

    initialBottomPadding = Get.mediaQuery.viewPadding.bottom;
    if (GetPlatform.isAndroid) {
      bottomPadding = initialBottomPadding + s12;
    } else {
      bottomPadding =
          initialBottomPadding > 0.0 ? initialBottomPadding + s12 : s20;
    }

    ///
    if (width >= SCREEN_428) {
      tileScale = 0.99;
    } else if (width >= SCREEN_414) {
      tileScale = 0.950;
    } else if (width >= SCREEN_390) {
      tileScale = 0.9;
    } else {
      tileScale = 0.855;
    }

    isSmallHeight = Get.size.height < 600;

    print("-----------------------------------------");
    print("f width: $width");
    print("scale height: $scaleHeight");
    print("top: ${Get.mediaQuery.viewPadding.top}");
    print("bottom: ${Get.mediaQuery.viewPadding.bottom}");
    print("window.physicalSize: ${window.physicalSize}");
    print("size: ${Get.size}");
    print("screen width: $width");
    print("scale: $scale");
    print("tileScale: $tileScale");
    print('system textScaleFactor: ${Get.mediaQuery.textScaleFactor}');
    print('device pixel ratio: ${Get.mediaQuery.devicePixelRatio}');
    textScaleFactor = Get.mediaQuery.textScaleFactor;
    if (/*width < SCREEN_375 &&*/ textScaleFactor > 1.0) {
      textScaleFactor = min(1.1, textScaleFactor);
    }
    print('our textScaleFactor: $textScaleFactor');
    print("-----------------------------------------");
  }

  static double getTileScale(int length) {
    if (length == 1) {
      return 0.415;
    } else if (length <= 4) {
      return 0.835;
    } else {
      return SC.tileScale;
    }
  }

  static double getScreenSizeBlock() {
    if (Get.size.width < SCREEN_360) {
      return SCREEN_320;
    } else if (Get.size.width < SCREEN_375) {
      return SCREEN_360;
    } else if (Get.size.width < SCREEN_390) {
      return SCREEN_375;
    } else if (Get.size.width < SCREEN_414) {
      return SCREEN_390;
    } else if (Get.size.width < SCREEN_428) {
      return SCREEN_414;
    } else {
      return SCREEN_428;
    }
  }
}
