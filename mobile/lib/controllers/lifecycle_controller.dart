import 'package:app/controllers/app_event.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';

/// Контроллер жизненного цикла приложения, на случай если понадобиться
/// специальная логика на сворачивание/восстановление приложения
class LifecycleController extends GetxController with WidgetsBindingObserver {
  final AppEvent appEvent = Get.find();
  AppLifecycleState appLifecycleState = AppLifecycleState.resumed;

  ///
  /// App Lifecycles
  ///
  @override
  void didChangeAppLifecycleState(AppLifecycleState state) async {
    appLifecycleState = state;

    /// ВОССТАНОВЛЕНИЕ ПРИЛОЖЕНИЯ
    if (state == AppLifecycleState.resumed) {
      // махинации на восстановлении приложения
      await appEvent.onAppRestore();
    }

    /// СВОРАЧИВАНИЕ ПРИЛОЖЕНИЯ
    if (state == AppLifecycleState.paused) {
      // махинации на сворачивании приложения
      await appEvent.onAppBackground();
    }

    /// ЗАКРЫТИЕ ПРИЛОЖЕНИЯ
    if (state == AppLifecycleState.detached) {}

    // ignore: avoid_print
    print("---------------- LifecycleState: $state ----------------");
  }

  @override
  void onInit() async {
    super.onInit();
    WidgetsFlutterBinding.ensureInitialized();
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void onClose() {
    WidgetsBinding.instance.removeObserver(this);
    super.onClose();
  }
}
