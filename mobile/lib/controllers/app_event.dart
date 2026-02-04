// ignore_for_file: avoid_print

import 'package:app/controllers/app_controller.dart';
import 'package:app/controllers/notifications_controller.dart';
import 'package:app/controllers/qrmarks_controller.dart';
import 'package:get/get.dart';
import 'package:get_storage/get_storage.dart';

///
/// Глобальная эвент модель приложения
/// Тут описываем события (хуки) приложения и возникающую логику
///
class AppEvent extends GetxController {
  AppEvent({
    required this.appController,
    required this.notificationsController,
  });

  final storage = GetStorage();
  final AppController appController;
  final NotificationsController notificationsController;

  ///
  /// Событие при старте приложения во время сплеша
  ///
  onAppStart() async {
    print("----- ON APP START ------");

    // пытаемся восстановить пользователя
    await appController.restoreUser();

    // загружаем непрочитанные уведомления
    if (appController.isAuthorized) {
      notificationsController.loadAllUnread();
      Get.find<QRMarksController>().loadQRMarks();
    }
  }

  ///
  /// Событие при логине пользователя
  ///
  onUserLogin() async {
    print("----- ON USER LOGIN ------");

    notificationsController.loadAllUnread();
    Get.find<QRMarksController>().loadQRMarks();
  }

  ///
  /// Событие при логауте пользователя
  ///
  onUserLogout() async {
    print("----- ON USER LOGOUT ------");

    notificationsController.clearDependencies();
    Get.find<QRMarksController>().onLogout();
  }

  ///
  /// Событие при сворачивании приложения
  ///
  onAppBackground() async {}

  ///
  /// Событие при восстановлении приложения
  ///
  onAppRestore() async {
    // загружаем непрочитанные уведомления
    if (appController.isAuthorized) {
      notificationsController.loadAllUnread();
    }
  }
}
