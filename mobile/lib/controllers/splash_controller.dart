import 'dart:async';

import 'package:app/controllers/app_controller.dart';
import 'package:app/controllers/app_event.dart';
import 'package:app/models/app_version.dart';
import 'package:app/repository/VersionControlRepository.dart';
import 'package:app/screens/splash_screen/components/force_update_dialog.dart';
import 'package:app/screens/splash_screen/components/soft_update_dialog.dart';
import 'package:app/utils/scale_config.dart';
import 'package:get/get.dart';
import 'package:package_info_plus/package_info_plus.dart';

import '../utils/routes.dart';

/// Контроллер сплэш экрана, отвечающий за инициализацию необходимых
/// частей приложения на старте
class SplashController extends GetxController {
  final AppEvent appEvent = Get.find();
  int waiting = 0;

  // все, что связано с текущей версией приложения и контролем версий
  var currentAppVersion = ''.obs;
  int? currentBuild;
  final AppController appController = Get.find();
  final versionRepo = Get.put(VersionControlRepository());
  AppVersion? appVersion;

  // требуется ли принудительное обновление версии приложения
  bool get needsForceUpdate =>
      appVersion != null &&
      currentBuild != null &&
      currentBuild! < appVersion!.minimal;

  // рекомендуется ли необязательное обновление версии приложения
  bool get needsSoftUpdate =>
      appVersion != null &&
      currentBuild != null &&
      currentBuild! < appVersion!.current;

  @override
  void onInit() async {
    super.onInit();

    // инициализация класса, отвечающего за адаптивные величины в UI
    SC.initValues(Get.width);

    // определяем текущую версию приложения
    PackageInfo packageInfo = await PackageInfo.fromPlatform();
    currentBuild = int.tryParse(packageInfo.buildNumber);
    currentAppVersion.value = '${packageInfo.version} ($currentBuild)';

    // контроль версий с бэка
    appVersion = await versionRepo.fetchAppVersion();

    // если требуется принудительное обновление
    if (needsForceUpdate) {
      await ForceUpdateDialog.open();
    }

    // если вышла новая версия приложения
    if (needsSoftUpdate) {
      bool result = await SoftUpdateDialog.open();
      if (result) {
        appController.openStoreLink();
      }
    }

    // хук на старт прилы
    await appEvent.onAppStart();

    doLoad();
    setTimer();
  }

  Future<void> setTimer() async {
    waiting++;
    Timer(const Duration(seconds: 1), () => loadFinished());
  }

  Future<void> doLoad() async {}

  Future<void> loadFinished() async {
    if (--waiting == 0) {
      if (appController.isAuthorized) {
        Get.offNamed(CustomRouter.PAGENAME_HOME);
      } else {
        Get.offNamed(CustomRouter.PAGENAME_AUTH);
      }
    }
  }
}
