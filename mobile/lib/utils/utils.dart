import 'package:app/utils/res/svg_assets.dart';
import 'package:app/utils/res/text_styles.dart';
import 'package:app/utils/res/ui_colors.dart';
import 'package:app/utils/scale_config.dart';
import 'package:app/widgets/buttons/light_button.dart';
import 'package:app/widgets/buttons/solid_button.dart';
import 'package:app/widgets/custom_loading_indicator.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:get/get.dart';
import 'package:url_launcher/url_launcher.dart';

import 'keyboard_pusher.dart';

class Utils {
  ///
  /// Метод запускает лоадер
  ///
  static void showLoader({String text = ""}) {
    SystemChrome.setSystemUIOverlayStyle(const SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
    ));

    Get.dialog(
      Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          if (text.isNotEmpty)
            Padding(
              padding: EdgeInsets.all(SC.s8),
              child: Text(text, style: TextStyles.defaultRegular),
            ),
          const CustomLoadingIndicator()
        ],
      ),
      barrierDismissible: false,
      barrierColor: UiColors.grey42,
    );
  }

  ///
  /// Метод останавливает лоадер
  ///
  static void hideLoader() async {
    if (Get.isOverlaysOpen) Navigator.of(Get.overlayContext!).pop();
  }

  ///
  /// Метод показывает снэкбар с желаемым текстом
  ///
  static void showSnackBar(
      {required String title, Duration duration = const Duration(seconds: 3)}) {
    Get.closeAllSnackbars();

    SystemChrome.setSystemUIOverlayStyle(const SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
    ));

    Get.rawSnackbar(
      snackPosition: SnackPosition.BOTTOM,
      padding: EdgeInsets.all(SC.s16),
      margin: EdgeInsets.fromLTRB(SC.s16, 0, SC.s16, SC.s30),
      // duration: const Duration(seconds: 5),
      borderRadius: SC.s12,
      backgroundColor: UiColors.greenDark,
      duration: duration,
      boxShadows: [
        BoxShadow(
            offset: Offset(0, SC.s4),
            blurRadius: SC.s8,
            color: UiColors.black.withOpacity(0.15)),
        BoxShadow(
            offset: Offset(0, SC.s1),
            blurRadius: SC.s3,
            color: UiColors.black.withOpacity(0.3)),
      ],
      messageText: Text(
        title,
        style: TextStyles.regular14.copyWith(
          height: SC.s20 / SC.s14,
          color: UiColors.white,
        ),
        textAlign: TextAlign.left,
      ),
    );
  }

  ///
  /// Метод показывает обычное диалоговое окно для уведомления о чем-либо
  /// (единственная кнопка скрывает окно), также в качестве подзаголовка
  /// можно передать виджет со специфическим поведением
  ///
  static Future<void> showSimpleDialog({
    /// Заголовок алерта
    required String title,

    /// Опционально виджет подзаголовка
    Widget? subtitleWidget,

    /// Опционально текст подзаголовка, если передан виджет игнорируется
    String? subtitle,

    /// Текст кнопки
    required String buttonText,

    /// Можно ли закрыть тапом вне алерта
    bool isDismissible = false,

    /// Коллбэк нажатия на кнопку
    VoidCallback? onButtonPressed,
  }) async {
    SystemChrome.setSystemUIOverlayStyle(const SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
    ));

    await Get.dialog(
      Dialog(
        insetPadding: EdgeInsets.zero,
        elevation: 0,
        backgroundColor: Colors.transparent,
        child: Container(
          width: double.infinity,
          padding: EdgeInsets.all(SC.s24),
          margin: EdgeInsets.symmetric(horizontal: SC.s(32)),
          decoration: BoxDecoration(
            color: UiColors.white,
            borderRadius: BorderRadius.circular(SC.s28),
            boxShadow: [
              BoxShadow(
                  offset: Offset(0, SC.s6),
                  blurRadius: SC.s10,
                  color: UiColors.black.withOpacity(0.15)),
              BoxShadow(
                offset: Offset(0, SC.s2),
                blurRadius: SC.s3,
                color: UiColors.black.withOpacity(0.3),
              ),
            ],
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                title,
                style: TextStyles.medium19.copyWith(
                  color: UiColors.blackA_94,
                  letterSpacing: 0.2,
                ),
              ),
              subtitleWidget ??
                  (subtitle != null && subtitle.isNotEmpty
                      ? Padding(
                          padding: EdgeInsets.only(top: SC.s4),
                          child: Text(
                            subtitle,
                            style: TextStyles.regular14.copyWith(
                              height: SC.s22 / SC.s14,
                              letterSpacing: 0.25,
                              color: UiColors.blackD_70,
                            ),
                          ),
                        )
                      : const SizedBox()),
              SolidButton(
                title: buttonText,
                onPressed: onButtonPressed ?? () => Get.back(),
                padding: EdgeInsets.only(top: SC.s20),
              )
            ],
          ),
        ),
      ),
      barrierColor: UiColors.black.withOpacity(0.4),
      barrierDismissible: isDismissible,
    );
  }

  ///
  /// Метод показывает обычное всплывающее окно для показа любой информации,
  /// нажатие на окно скрывает его
  ///
  static Future<void> showInfoDialog({
    /// Текст для показа
    required String infoText,
  }) async {
    SystemChrome.setSystemUIOverlayStyle(const SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
    ));

    await Get.dialog(
      GestureDetector(
        onTap: () => Get.back(),
        child: Dialog(
          insetPadding: EdgeInsets.zero,
          elevation: 0,
          backgroundColor: Colors.transparent,
          child: Container(
            width: double.infinity,
            padding: EdgeInsets.all(SC.s24),
            margin: EdgeInsets.symmetric(horizontal: SC.s(32)),
            decoration: BoxDecoration(
              color: UiColors.white,
              borderRadius: BorderRadius.circular(SC.s28),
              boxShadow: [
                BoxShadow(
                    offset: Offset(0, SC.s6),
                    blurRadius: SC.s10,
                    color: UiColors.black.withOpacity(0.15)),
                BoxShadow(
                  offset: Offset(0, SC.s2),
                  blurRadius: SC.s3,
                  color: UiColors.black.withOpacity(0.3),
                ),
              ],
            ),
            child: Text(
              infoText,
              style: TextStyles.regular16.copyWith(
                color: UiColors.blackA_94,
                // letterSpacing: 0.2,
              ),
            ),
          ),
        ),
      ),
      barrierColor: UiColors.black.withOpacity(0.4),
      barrierDismissible: true,
    );
  }

  ///
  /// Метод показывает диалоговое окно с двумя кнопками (принятие и отказ)
  /// и возращает соответственно true или false
  ///
  static Future<bool> showOptionsDialog({
    /// Заголовок алерта
    required String title,

    /// Опционально текст подзаголовка, если передан виджет игнорируется
    String? subtitle,

    /// Текст кнопки подтверждения
    required String buttonTextConfirm,

    /// Текст кнопки отмена
    required String buttonTextCancel,

    /// Опционально коллбэк кнопки подтверждения
    VoidCallback? onConfirmPressed,

    /// Опционально коллбэк кнопки отмена
    VoidCallback? onCancelPressed,
  }) async {
    SystemChrome.setSystemUIOverlayStyle(const SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
    ));

    return await Get.dialog(
      Dialog(
        insetPadding: EdgeInsets.zero,
        elevation: 0,
        backgroundColor: Colors.transparent,
        child: Container(
          width: double.infinity,
          padding: EdgeInsets.all(SC.s24),
          margin: EdgeInsets.symmetric(horizontal: SC.s(32)),
          decoration: BoxDecoration(
            color: UiColors.white,
            borderRadius: BorderRadius.circular(SC.s28),
            boxShadow: [
              BoxShadow(
                  offset: Offset(0, SC.s6),
                  blurRadius: SC.s10,
                  color: UiColors.black.withOpacity(0.15)),
              BoxShadow(
                offset: Offset(0, SC.s2),
                blurRadius: SC.s3,
                color: UiColors.black.withOpacity(0.3),
              ),
            ],
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                title,
                style: TextStyles.medium19.copyWith(
                  color: UiColors.blackA_94,
                  letterSpacing: 0.2,
                ),
              ),
              if (subtitle != null && subtitle.isNotEmpty)
                Padding(
                  padding: EdgeInsets.only(top: SC.s4),
                  child: Text(
                    subtitle,
                    style: TextStyles.regular14.copyWith(
                      height: SC.s22 / SC.s14,
                      letterSpacing: 0.25,
                      color: UiColors.blackD_70,
                    ),
                  ),
                ),
              LightButton(
                title: buttonTextConfirm,
                onPressed: onConfirmPressed ?? () => Get.back(result: true),
                padding: EdgeInsets.only(top: SC.s20),
              ),
              SolidButton(
                title: buttonTextCancel,
                onPressed: onCancelPressed ?? () => Get.back(result: false),
                padding: EdgeInsets.only(top: SC.s12),
              )
            ],
          ),
        ),
      ),
      barrierColor: UiColors.black.withOpacity(0.4),
      barrierDismissible: false,
    );
  }

  static Future<void> showBottomSheet({
    required String title,
    required Widget contents,
  }) async {
    SystemChrome.setSystemUIOverlayStyle(const SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
    ));
    return await showModalBottomSheet(
      context: Get.context!,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      barrierColor: Colors.black54,
      // builder: (context) => QuantityBottomSheet(initQuantity, maxQuantity),
      builder: (context) {
        return Container(
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.only(
              topLeft: Radius.circular(SC.s20),
              topRight: Radius.circular(SC.s20),
            ),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              /// Элемент закрытия, заголовок и кнопка закрыть

              /// элемент закрытия в самом вверху
              SizedBox(
                width: double.infinity,
                child: Center(
                  child: Container(
                    margin: EdgeInsets.only(top: SC.s12),
                    width: SC.s(32.0),
                    height: SC.s4,
                    decoration: BoxDecoration(
                      color: const Color(0x1A252523),
                      borderRadius: BorderRadius.all(Radius.circular(SC.s2)),
                    ),
                  ),
                ),
              ),
              Padding(
                padding: EdgeInsets.fromLTRB(SC.s16, 0.0, SC.s16, 0.0),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.center,
                  children: [
                    /// заголовок
                    Expanded(
                      child: Padding(
                        padding: EdgeInsets.only(bottom: SC.s4),
                        child: Text(
                          title,
                          style: TextStyle(
                              fontSize: SC.s19,
                              fontWeight: FontWeight.w500,
                              fontFamily: 'Roboto',
                              color: Colors.black,
                              height: 1.0),
                        ),
                      ),
                    ),

                    /// кнопка закрыть
                    GestureDetector(
                      behavior: HitTestBehavior.translucent,
                      onTap: () => Get.back(),
                      child: SizedBox(
                        width: SC.s(48.0),
                        height: SC.s(48.0),
                        child: Center(
                          child: SvgPicture.asset(SvgAssets.cross),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              contents,
              KeyboardPusher(),
            ],
          ),
        );
      },
    );
  }

  ///
  /// Метод проверяет возможность перехода по url и выполняет открытие ссылки.
  /// По умолчанию открывается во внешнем браузере.
  ///
  static void openUrl(String url,
      {LaunchMode launchMode = LaunchMode.externalApplication}) async {
    final Uri? uri = Uri.tryParse(url);
    if (uri != null && await canLaunchUrl(uri)) {
      launchUrl(uri, mode: launchMode);
    }
  }

  static double getValueFromNewRange({
    required double originalValue,
    required double maxNewRange,
    required double minNewRange,
    required double maxOriginalRange,
    required double minOriginalRange,
  }) {
    return ((maxNewRange - minNewRange) * (originalValue - minOriginalRange)) /
            (maxOriginalRange - minOriginalRange) +
        minNewRange;
  }
}
