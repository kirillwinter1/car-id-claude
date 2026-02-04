// ignore_for_file: constant_identifier_names

import 'package:app/screens/auth/auth_screen.dart';
import 'package:app/screens/notifications_screen/notification_settings_screen.dart';
import 'package:app/screens/notifications_screen/notifications_screen.dart';
import 'package:app/screens/qr_marks_screen/qr_marks_screen.dart';
import 'package:app/screens/qr_report_event_screen/qr_report_event_screen.dart';
import 'package:app/screens/qr_report_event_screen/select_event_screen.dart';
import 'package:app/screens/qr_scan_screen/qr_scan_screen.dart';
import 'package:get/get.dart';

import '../bindings/default_binding.dart';
import '../screens/home/home_screen.dart';
import '../screens/splash_screen/splash_screen.dart';

class CustomRouter {
  static const PAGENAME_SPLASH = '/';
  static const PAGENAME_HOME = '/home';
  static const PAGENAME_AUTH = '/auth';
  static const PAGENAME_QRMARKS = '/qr_marks';
  static const PAGENAME_NOTIFICATIONS = '/notifications';
  static const PAGENAME_NOTIFICATION_SETTINGS = '/notification_settings';
  static const PAGENAME_QR_SCAN = '/qr_scan';
  static const PAGENAME_QR_ADD_EVENT = '/qr_add_event';
  static const PAGENAME_SELECT_EVENT = '/select_event';

  static final route = [
    GetPage(
      name: PAGENAME_SPLASH,
      page: () => const SplashScreen(),
      binding: DefaultBinding(),
    ),
    GetPage(
      name: PAGENAME_HOME,
      page: () => const HomeScreen(),
      binding: DefaultBinding(),
      transitionDuration: Duration.zero,
    ),
    GetPage(
      name: PAGENAME_AUTH,
      page: () => const AuthScreen(),
      binding: DefaultBinding(),
    ),
    GetPage(
      name: PAGENAME_QRMARKS,
      page: () => const QRMarksScreen(),
      binding: DefaultBinding(),
    ),
    GetPage(
      name: PAGENAME_NOTIFICATIONS,
      page: () => const NotificationsScreen(),
      binding: DefaultBinding(),
    ),
    GetPage(
      name: PAGENAME_NOTIFICATION_SETTINGS,
      page: () => const NotificationSettingsScreen(),
      binding: DefaultBinding(),
    ),
    GetPage(
      name: PAGENAME_QR_SCAN,
      page: () => const QrScanScreen(),
      binding: DefaultBinding(),
    ),
    GetPage(
      name: PAGENAME_QR_ADD_EVENT,
      page: () => const QRReportEventScreen(),
      binding: DefaultBinding(),
    ),
    GetPage(
      name: PAGENAME_SELECT_EVENT,
      page: () => const SelectEventScreen(),
      binding: DefaultBinding(),
    ),
  ];
}
