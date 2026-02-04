import 'dart:async';

import 'package:app/models/car_notification.dart';
import 'package:app/repository/NotificationsRepository.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';

class StatusController extends GetxController with WidgetsBindingObserver {
  final CarNotification notification;
  StatusController(this.notification);

  final NotificationsRepository repository = Get.find();

  // Изначальный флаг прочитано ли уведомление
  late bool initialIsRead;

  // Актуальный флаг прочитано ли уведомление
  bool isRead = false;

  // Таймер, задающий периодичность запросов на бэк
  Timer? _timer;

  @override
  void onInit() {
    super.onInit();
    WidgetsFlutterBinding.ensureInitialized();
    WidgetsBinding.instance.addObserver(this);

    initialIsRead = notification.isRead;

    // Если уведомление уже было прочитано, то показываем прочитанный статус,
    // Иначе начинаем слать запросы на бэк пока не получим прочитанный статус
    if (initialIsRead) {
      isRead = true;
    } else {
      _checkStatusAndStartTimer();
    }
  }

  @override
  void onClose() {
    _stopTimer();
    WidgetsBinding.instance.removeObserver(this);
    super.onClose();
  }

  Future<void> _checkStatusAndStartTimer() async {
    await _checkStatus();
    if (isRead == false) _startTimer();
  }

  Future<void> _checkStatus() async {
    isRead = await repository.checkOutgoingStatus(notification.id).timeout(
          const Duration(seconds: 4),
          onTimeout: () => false,
        );
    update();
  }

  ///
  /// Начинает таймер, который раз в заданное время проверяет статус
  ///
  void _startTimer() {
    if (_timer != null && _timer!.isActive) {
      _timer!.cancel();
      _timer == null;
    }

    _timer = Timer.periodic(const Duration(seconds: 5), (timer) {
      if (isRead == false) _checkStatus();
    });
  }

  ///
  /// Остановка и отмена таймера
  ///
  void _stopTimer() {
    if (_timer != null && _timer!.isActive) {
      _timer!.cancel();
      _timer == null;
    }
  }

  ///
  /// Lifecycle protection
  ///
  AppLifecycleState appLifecycleState = AppLifecycleState.resumed;
  @override
  void didChangeAppLifecycleState(AppLifecycleState state) async {
    appLifecycleState = state;

    /// Если юзер свернул приложение, то останавливаем таймер
    if (state == AppLifecycleState.paused) {
      _stopTimer();
    }

    /// Когда приложение вновь активно и уведомление не прочитано,
    /// запускаем таймер опять
    if (state == AppLifecycleState.resumed) {
      if (isRead == false) _checkStatusAndStartTimer();
    }
  }
}
