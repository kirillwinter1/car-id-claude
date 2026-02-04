import 'dart:convert';
import 'dart:developer';

import 'package:app/controllers/notifications_controller.dart';
import 'package:app/repository/NotificationsRepository.dart';
import 'package:app/utils/const.dart';
import 'package:app/utils/gms_hms.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:get/get.dart';

/// Не используется, оставлено на будущеее
/*
Future<void> _firebaseMessagingBackgroundHandler(RemoteMessage message) async {
  await Firebase.initializeApp();
  debugPrint("Handling a background message: ${message.messageId}");
  debugPrint('message data: ${message.data}');
}
 */

/// Внутренняя настройка для вывода пушей
const AndroidNotificationChannel channel = AndroidNotificationChannel(
  'high_importance_channel', // id
  'High Importance Notifications', // title
  description: 'gm_channel_description',
  importance: Importance.high,
);

/// Контроллер, отвечающий за пуш-уведомления, содержит всю логику их
/// отображения и обработки
class PushNotificationController extends GetxController {
  /// Плагин для показа сообщений полученных при запущенном приложении
  late FlutterLocalNotificationsPlugin flutterLocalNotificationsPlugin;

  /// FirebaseCloudMessaging token
  String? fcmToken;

  /// Стартовое сообщение при старте, когда оно получено при неактивном
  /// приложении
  RemoteMessage? initialMessage;

  @override
  void onInit() async {
    super.onInit();

    /// Стартовые настройки flutterLocalNotificationsPlugin
    /// смотреть https://pub.dev/packages/flutter_local_notifications
    flutterLocalNotificationsPlugin = FlutterLocalNotificationsPlugin();

    const AndroidInitializationSettings initializationSettingsAndroid =
        AndroidInitializationSettings('launcher_icon');

    const DarwinInitializationSettings initializationSettingsDarwin =
        DarwinInitializationSettings(
      requestAlertPermission: false,
      requestBadgePermission: false,
      requestSoundPermission: false,
    );

    const InitializationSettings initializationSettings =
        InitializationSettings(
      android: initializationSettingsAndroid,
      iOS: initializationSettingsDarwin,
      macOS: initializationSettingsDarwin,
    );
    WidgetsFlutterBinding.ensureInitialized();

    await flutterLocalNotificationsPlugin.initialize(
      initializationSettings,
      onDidReceiveNotificationResponse:
          (NotificationResponse notificationResponse) {
        switch (notificationResponse.notificationResponseType) {
          case NotificationResponseType.selectedNotification:
            // selectNotificationStream.add(notificationResponse.payload);
            selectLocalNotification(notificationResponse.payload);
            break;
          case NotificationResponseType.selectedNotificationAction:
            // if (notificationResponse.actionId == navigationActionId) {
            //   selectNotificationStream.add(notificationResponse.payload);
            // }
            break;
        }
      },
    );

    await flutterLocalNotificationsPlugin
        .resolvePlatformSpecificImplementation<
            AndroidFlutterLocalNotificationsPlugin>()
        ?.createNotificationChannel(channel);

    final bool? result = await flutterLocalNotificationsPlugin
        .resolvePlatformSpecificImplementation<
            IOSFlutterLocalNotificationsPlugin>()
        ?.requestPermissions(
          alert: true,
          badge: true,
          sound: true,
        );
    debugPrint('IOSFlutterLocalNotificationsPlugin $result');

    if (GmsHms.isGmsAvailable) initFirebaseMessaging();
  }

  /// Настроки системы Фаербеййза
  Future<void> initFirebaseMessaging() async {
    // FirebaseMessaging.onBackgroundMessage(_firebaseMessagingBackgroundHandler);
    NotificationSettings settings =
        await FirebaseMessaging.instance.requestPermission(
      alert: true,
      announcement: false,
      badge: true,
      carPlay: false,
      criticalAlert: false,
      provisional: false,
      sound: true,
    );
    debugPrint('User granted permission: ${settings.authorizationStatus}');
    fcmToken = await FirebaseMessaging.instance.getToken();
    debugPrint('');
    debugPrint('                                                      '
        '                   Notification Token');
    debugPrint('$fcmToken');
    debugPrint('');
    debugPrint('');

    FirebaseMessaging.onMessage.listen(firebaseOnMessage);

    initialMessage = await FirebaseMessaging.instance.getInitialMessage();

    FirebaseMessaging.onMessageOpenedApp.listen((RemoteMessage message) {
      proceedPayload(message.data);
    });

    await FirebaseMessaging.instance
        .setForegroundNotificationPresentationOptions(
      alert: true,
      badge: true,
      sound: true,
    );

    FirebaseMessaging.instance.subscribeToTopic('ADS');
    if (BUILD_4_TEST) {
      FirebaseMessaging.instance.subscribeToTopic('ADS_TEST');
    }
  }

  /// Обработка ситуации тапа по локальному оповещению (proceedPayload)
  Future selectLocalNotification(String? payload) async {
    debugPrint('selectLocalNotification');
    if (payload != null) {
      debugPrint('notification payload: $payload');
      Map data = jsonDecode(payload);
      proceedPayload(data);
    }
  }

  /// Общая обработка смысловой нагрузки пуш уведомления
  Future<void> proceedPayload(Map payload) async {
    log('proceedPayload ${jsonEncode(payload)}');
    if (payload['notification_type'] == 'STATUS_CHANGE' ||
        payload['notification_type'] == 'MESSAGE') {
    } else if (payload['notification_type'] == 'ADS') {}
  }

  /// Обработка наличия начального сообщения после старта
  void loadFinished() {
    if (initialMessage != null) {
      Future.delayed(const Duration(milliseconds: 100), () async {
        await proceedPayload(initialMessage!.data);
        initialMessage = null;
      });
    }
  }

  /// При работе приложения при приходе уведомелния вызывается этот метод,
  /// делаем: запускаем локальное уведомление чтобы пользователь его видел
  /// по нажатию идет на стандартный proceedPayload
  void firebaseOnMessage(RemoteMessage message) async {
    // при получении пуша подгружаем уведомления
    Get.find<NotificationsController>().loadAllUnread();

    debugPrint('Current Route ${Get.currentRoute}');
    debugPrint('Current argument ${Get.arguments}');

    RemoteNotification? notification = message.notification;
    AndroidNotification? android = message.notification?.android;
    AppleNotification? apple = message.notification?.apple;

    debugPrint('Notification data: ${message.data}');

    if (notification != null &&
        (android != null || apple != null) &&
        !GetPlatform.isIOS) {
      flutterLocalNotificationsPlugin.show(
        notification.hashCode,
        notification.title,
        notification.body,
        NotificationDetails(
          android: AndroidNotificationDetails(
            channel.id,
            channel.name,
            channelDescription: channel.description,
            // icon: 'launcher_icon',
            importance: Importance.max,
            priority: Priority.high,
            ticker: 'ticker',
          ),
          iOS: const DarwinNotificationDetails(
            presentBadge: true,
            presentAlert: true,
            presentSound: true,
          ),
        ),
        payload: jsonEncode(message.data),
      );
    }
  }

  /// Для тестов
  void fakePush() {
    var payLoad = {
      'notification_type': 'STATUS_CHANGE',
      'status_description': 'onshelf',
      'delivery_type': 'pickup',
      'order_id': '00000000145',
      'status': 'onshelf'
    };
    flutterLocalNotificationsPlugin.show(
      435435345,
      'test',
      'test',
      NotificationDetails(
        android: AndroidNotificationDetails(
          channel.id,
          channel.name,
          channelDescription: channel.description,
          icon: 'launcher_icon',
          importance: Importance.max,
          priority: Priority.high,
          ticker: 'ticker',
        ),
        iOS: const DarwinNotificationDetails(
          presentBadge: true,
          presentAlert: true,
          presentSound: true,
        ),
      ),
      payload: jsonEncode(payLoad),
    );
  }

  /// Метод вызывается после авторизации для обновления FCMToken прользователя
  Future<void> updateFCMToken() async {
    var notificationsRepository = Get.put(NotificationsRepository());
    if (fcmToken != null) {
      notificationsRepository.updateToken(fcmToken ?? '');
    }
  }
}
