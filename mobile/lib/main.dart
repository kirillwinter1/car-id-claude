import 'dart:io';
import 'package:app/controllers/app_controller.dart';
import 'package:app/controllers/app_event.dart';
import 'package:app/controllers/auth_controller.dart';
import 'package:app/controllers/lifecycle_controller.dart';
import 'package:app/controllers/notifications_controller.dart';
import 'package:app/controllers/qrmarks_controller.dart';
import 'package:app/utils/gms_hms.dart';
import 'package:app/utils/i18n/localization_service.dart';
import 'package:app/utils/routes.dart';
import 'package:app/utils/theme.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:get_storage/get_storage.dart';

import 'package:intl/date_symbol_data_local.dart';
import 'package:get/get.dart';

import 'bindings/default_binding.dart';
import 'controllers/push_notification_controller.dart';

class MyHttpOverrides extends HttpOverrides {
  @override
  HttpClient createHttpClient(SecurityContext? context) {
    var client = super.createHttpClient(context);
    client.badCertificateCallback =
        (X509Certificate cert, String host, int port) => true;
    client.connectionTimeout = const Duration(seconds: 10);
    return client;
  }
}

void main() async {
  HttpOverrides.global = MyHttpOverrides();
  await init();
  runApp(Application());
}

Future<void> init() async {
  initializeDateFormatting();
  WidgetsFlutterBinding.ensureInitialized();

  await SystemChrome.setPreferredOrientations([DeviceOrientation.portraitUp]);
  await GetStorage.init();
  await GmsHms.init();
}

class Application extends StatelessWidget {
  final appEvent = Get.put(AppEvent(
    appController: Get.put(AppController()),
    notificationsController: Get.put(NotificationsController()),
  ));
  final lifecycleController = Get.put(LifecycleController());
  final authController = Get.put(AuthController());
  final pushNotificationController = Get.put(PushNotificationController());
  final markController = Get.put(QRMarksController());

  Application({super.key});

  @override
  Widget build(BuildContext context) {
    SystemChrome.setSystemUIOverlayStyle(const SystemUiOverlayStyle(
      systemNavigationBarColor: Colors.white,
      systemNavigationBarIconBrightness: Brightness.dark,
      statusBarColor: Colors.white,
      statusBarIconBrightness: Brightness.dark,
    ));

    return GetMaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'Car Id',
      initialRoute: '/',
      theme: appThemeData,
      getPages: CustomRouter.route,
      defaultTransition: Transition.rightToLeft,
      locale: LocalizationService.locale,
      translations: LocalizationService(),
      initialBinding: DefaultBinding(),
      // navigatorObservers: GmsHms.getNavigatorObservers(),
    );
  }
}
