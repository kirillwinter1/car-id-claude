import 'dart:async';

import 'package:app/controllers/app_controller.dart';
import 'package:app/controllers/notifications_controller.dart';
import 'package:app/models/car_notification.dart';
import 'package:app/repository/QRReportEventRepository.dart';
import 'package:app/screens/qr_report_event_screen/components/alert_timer_widget.dart';
import 'package:app/screens/status_screen/status_screen.dart';
import 'package:app/utils/routes.dart';
import 'package:flutter/cupertino.dart';
import 'package:get/get.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:qr_code_scanner/qr_code_scanner.dart';

import '../models/report_event.dart';
import '../utils/utils.dart';

class QRReportEventController extends GetxController {
  /// флаг что работаем с кодом чтобы не сканировать новые
  bool workWithCode = false;

  /// Текущей выбранный тип сообщения для события
  ReportEvent? selectedEvent;

  /// Контроллер управляющий камерой/сканированием QR кода
  QRViewController? scannerController;

  /// Основной контроллер приложения
  final appController = Get.find<AppController>();

  /// Флаг включения фонарика
  bool torchIsOn = false;

  /// Репозитарий для работы с отправкой сообщений
  /// и получений списка причин сообщений
  final repository = Get.put(QRReportEventRepository());

  /// Текущий отсканированный код
  String? currentCode;

  /// Геттер типов событий, они храняться в основном контроллере
  /// поскольку они нужны также для контроллера уведомлений
  List<ReportEvent> get events => appController.reportEvents;

  /// Таймер закрытия окна алерта ожидания следующей отправки сообщения
  Timer? closeAlertTimer;

  RxInt timerSeconds = (-1).obs;

  /// Флаг доступа к камере
  bool accessGranted = false;

  /// Инициализация - подгружаем типов событий
  @override
  void onInit() {
    super.onInit();
    Future.delayed(const Duration(milliseconds: 300), () => checkPermission());
    loadEvents();
  }

  /// Инизиализация разрешений
  Future<void> checkPermission() async {
    accessGranted = await Permission.camera.isGranted;
    if (!accessGranted) {
      await Permission.camera.request();
      accessGranted = await Permission.camera.isGranted;

      /// выводим запрос на разрешение через настройки
      if (!accessGranted) {
        var result = await Utils.showOptionsDialog(
              title: 'Сообщить о событии',
              subtitle: 'Разрешите доступ к камере',
              buttonTextConfirm: 'Настройки',
              buttonTextCancel: 'Отмена',
            ) ||
            false;
        if (result) {
          await openAppSettings();
        }
      }
    }

    await Future.delayed(const Duration(milliseconds: 300));

    accessGranted = await Permission.camera.isGranted;

    if (!accessGranted) {
      Get.back();
    }
    update();
  }

  /// Закрытие контроллера - проверяем включен ли фонарик, если да то выключаем
  @override
  void onClose() {
    super.onClose();
    if (torchIsOn) {
      scannerController?.toggleFlash();
    }
  }

  /// Загрузка типов сообщений
  Future<void> loadEvents() async {
    appController.reportEvents.clear();
    var result = await repository.loadReportEvents();
    if (result != null) {
      for (var event in result) {
        appController.reportEvents.add(event);
      }
    }
    update();
  }

  /// Выбор типа события из списка после удачного сканирования
  Future<void> onEventTap(ReportEvent event) async {
    selectedEvent = event;
    update();
  }

  /// Обработка нажатия на кнопку отправки сообщения
  Future<void> onReportButtonTap() async {
    /// Вкл экран загрузки
    Utils.showLoader();
    Response? response;
    if (selectedEvent != null) {
      response = await repository.sendQRMessage(
          selectedEvent!.id, currentCode ?? '', selectedEvent!.description);
    }

    /// Выкл экрана загрузки
    Utils.hideLoader();

    if (response != null) {
      if (response.body['error_code'] != null) {
        String error = response.body['error_message'] ?? '';

        Widget? subtitleWidget;

        if (response.body['error_code'] == 'SEND_TIMEOUT') {
          /// Если нужно подождать создаем таймер и виджет для окна алерта,
          /// по окончнию таймера окно автоматом закрывается, либо пользователь
          /// может раньше по кнопке закрыть
          int? leftSeconds =
              int.tryParse(error.replaceAll(RegExp(r'[\D]'), ''));

          if (leftSeconds != null && leftSeconds > 0) {
            timerSeconds.value = leftSeconds;
            closeAlertTimer = Timer.periodic(
              const Duration(seconds: 1),
              (timer) {
                int s = leftSeconds!;
                s--;
                leftSeconds = s;
                timerSeconds.value = s;
                if (s <= 0) onAlertDialogClose();
              },
            );

            subtitleWidget = AlertTimerWidget(leftSeconds: timerSeconds);
          }
        }
        await Utils.showSimpleDialog(
          title: 'Ошибка',
          subtitleWidget: subtitleWidget,
          subtitle: error,
          buttonText: 'Понятно',
          onButtonPressed: onAlertDialogClose,
        );
      } else {
        CarNotification? notification;

        try {
          notification = CarNotification.fromJson(response.body['params']);
        } catch (e) {
          debugPrint('Error parsing CarNotification');
        }

        if (notification != null) {
          /// Если удалось распарсить уведомление из ответа,
          /// то переходим на экран статуса через главный и уведомления
          Get.offAllNamed(CustomRouter.PAGENAME_HOME);
          Get.toNamed(CustomRouter.PAGENAME_NOTIFICATIONS);
          Get.find<NotificationsController>().setCurrentTab(1);
          Get.to(() => StatusScreen(notification!));
        } else {
          /// Иначе возвращаемся на главный экран, и показываем снэкбар
          /// удачной отправки сообщения
          Get.offAllNamed(CustomRouter.PAGENAME_HOME);
          Utils.showSnackBar(
              title: 'Уведомления отправлены владельцу автомобиля.');
        }
      }
    }
  }

  /// Метод реакции на закрытие окна алерта, если есть таймер - отключаем
  void onAlertDialogClose() {
    Get.back();
    closeAlertTimer?.cancel();
    closeAlertTimer = null;
    Future.delayed(const Duration(seconds: 1), () => timerSeconds.value = -1);
  }

  /// Обработка распознования QR кода
  Future<void> onCodeRecognized(String code) async {
    debugPrint('CODE RECEIVED: $code');

    // валидируем код по маске https://car-id.ru/qr/CODE
    if (code.startsWith('https://car-id.ru/qr/') == false) {
      debugPrint('error: NOT CAR ID CODE');
      return;
    }
    final String unmaskedCode = code.substring(21);

    currentCode = unmaskedCode;
    if (workWithCode) return;
    workWithCode = true;

    /// Если фонарик включен - выключаем
    if (torchIsOn) {
      torchIsOn = false;
      scannerController?.toggleFlash();
    }

    /// Переход на страницу выбора причины с последующей отправкой
    await Get.toNamed(CustomRouter.PAGENAME_SELECT_EVENT);
    workWithCode = false;
  }

  /// Обработка нажатия на кнопку фонарика на экране сканирования
  Future<void> onTorchTap() async {
    torchIsOn = !torchIsOn;
    scannerController?.toggleFlash();
    update();
  }
}
