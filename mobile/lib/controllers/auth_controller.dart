import 'dart:async';

import 'package:app/controllers/app_controller.dart';
import 'package:app/controllers/app_event.dart';
import 'package:app/repository/UserRepository.dart';
import 'package:app/utils/const.dart';
import 'package:app/utils/routes.dart';
import 'package:app/utils/utils.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:get_storage/get_storage.dart';
import 'package:mask_text_input_formatter/mask_text_input_formatter.dart';

/// Контроллер авторизации, хранит временную информацию текстовых полей
/// при авторизации, а также все методы, связанные с ней
class AuthController extends GetxController {
  // маска телефонного номера
  final MaskTextInputFormatter phoneMaskFormatter = MaskTextInputFormatter(
      mask: '### ###-##-##', filter: {"#": RegExp(r'[0-9]')});

  // хранилище устройства
  final storage = GetStorage();

  final UserRepository userRepository = Get.put(UserRepository());
  final AppController appController = Get.find<AppController>();

  // чекбокс правил сервиса
  bool isAcceptedRules = false;
  // введены ли все цифры номера
  bool isPhoneEntered = false;
  // успешен ли первый запрос (отправка номера телефона)
  bool isPhoneValid = true;
  // успешен ли второй запрос (отправка смс-кода)
  bool isCodeValid = true;

  // текстовые контроллеры и фокус ноды
  final TextEditingController phoneController = TextEditingController();
  final TextEditingController codeController = TextEditingController();
  final FocusNode phoneInputFocusNode = FocusNode();
  final FocusNode codeInputFocusNode = FocusNode();

  // текущая страница (0 - phone login, 1 - sms code)
  int currentPage = 0;

  // booleans для проверки текущего состояния загрузки и показа лоадера
  bool phoneRegistrationInProgress = false;
  bool codeVerificationInProgress = false;

  // Таймер
  Timer? _timer;
  String timerForPhone = ''; // телефон для которого тикает счетчик
  DateTime? tickerStartDate; // время старта счетчика кода
  int secoundsLeft = 0;
  int secoundsCount = 0;

  bool get isPhonePageCompleted => isAcceptedRules && isPhoneEntered;
  bool get isTimerForCurrentPhone =>
      phoneMaskFormatter.getUnmaskedText() == timerForPhone;

  ///
  /// Метод инициализации AuthController
  ///
  @override
  void onInit() {
    super.onInit();
    _initializeInputControllersListeners();
  }

  ///
  /// Метод инициализирует листенеры для контроллеров ввода телефона и смс-кода
  ///
  void _initializeInputControllersListeners() {
    phoneController.addListener(() {
      isPhoneEntered = phoneMaskFormatter.isFill();

      // сбрасываем ошибку номера телефона при первом стирании цифры
      if (isPhoneValid == false &&
          phoneMaskFormatter.getUnmaskedText().length < 10) {
        isPhoneValid = true;
      }

      update();
    });

    codeController.addListener(() {
      // автоматически запускаем метод подтверждения смс-кода при условиях
      if (codeController.text.length == 4 &&
          isCodeValid &&
          codeVerificationInProgress == false) {
        _confirmCode();
      }

      // сбрасываем ошибку смс-кода при первом стирании цифры
      if (isCodeValid == false && codeController.text.length < 4) {
        isCodeValid = true;
      }

      update();
    });

    phoneInputFocusNode.addListener(() => update());
    codeController.addListener(() => update());
  }

  ///
  /// Метод переключает чекбокс согласия с правилами сервиса
  ///
  void toggleIsAcceptedRules() {
    isAcceptedRules = !isAcceptedRules;
    update();
  }

  ///
  /// Метод приводит все настройки контроллера с дефолтным значениям
  ///
  void _clearDependencies() {
    phoneMaskFormatter.clear();
    phoneController.clear();
    codeController.clear();
    isAcceptedRules = false;
    isPhoneValid = true;
    isCodeValid = true;
    currentPage = 0;
  }

  ///
  /// Метод по нажатию на кнопку "Получить код"
  ///
  registerPhone() async {
    if (isPhonePageCompleted) {
      phoneRegistrationInProgress = true;
      phoneInputFocusNode.unfocus();
      update();

      // перекрываем интерфейс лоадером
      Utils.showLoader();

      try {
        // запрос на бэк
        final String unmaskedPhone = phoneMaskFormatter.getUnmaskedText();
        int? timeToNextRequestSec =
            await userRepository.registerPhone("7$unmaskedPhone");

        // если успешно, выставляем секунды для таймера
        if (timeToNextRequestSec != null) {
          secoundsLeft = timeToNextRequestSec;
          secoundsCount = timeToNextRequestSec;

          // на всякий случай защита от отрицательного значения
          if (secoundsLeft <= 0) {
            secoundsLeft = 120;
            secoundsCount = secoundsLeft;
          }

          // запоминаем для какого телефона тикает таймер и запускаем его
          timerForPhone = phoneMaskFormatter.getUnmaskedText();
          _initTimer();

          // переходим к окну ввода смс-кода
          _toNextPage();
        }
      } catch (error) {
        print('error on register phone: $error');
        // TODO error handling (snackbars, dialogues etc.)
        isPhoneValid = false;
      }

      // убираем лоадер
      Utils.hideLoader();
      phoneRegistrationInProgress = false;
      update();
    }
  }

  ///
  /// Метод подтверждения смс-кода
  ///
  _confirmCode() async {
    codeVerificationInProgress = true;
    update();

    Utils.showLoader();

    try {
      // запрос на бэк на получение токена
      final String unmaskedPhone = phoneMaskFormatter.getUnmaskedText();
      String? token = await userRepository.confirmCode(
          "7$unmaskedPhone", codeController.text);

      if (token != null) {
        // запрос на бэк на получение юзера по токену
        appController.user = await userRepository.getUserByToken(token);

        if (appController.user != null) {
          // сохраняем токен на клиенте
          appController.token = token;
          storage.write(KEY_USER_TOKEN, token);

          // хук на логин юзера
          Get.find<AppEvent>().onUserLogin();

          // переходим на главную
          Get.offAllNamed(CustomRouter.PAGENAME_HOME);
          _clearDependencies();
        }
      } else {
        isCodeValid = false;
      }
    } catch (error) {
      print('confirm code error: $error');
      isCodeValid = false;
    }

    Utils.hideLoader();
    codeVerificationInProgress = false;
    update();
  }

  ///
  /// Метод запускает таймер обратного отсчета
  ///
  _initTimer() {
    _timer?.cancel();
    tickerStartDate = DateTime.now();

    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      final int difference =
          DateTime.now().difference(tickerStartDate!).inSeconds;

      secoundsLeft = secoundsCount - difference;
      update();

      if (secoundsLeft <= 0) {
        _timer!.cancel();
        _timer = null;

        /// Подчищаем инпут ввода кода по окончанию таймера
        isCodeValid = true;
        codeController.clear();

        update();
      }
    });
  }

  ///
  /// Метод перехода к вводу смс-кода
  ///
  void _toNextPage() {
    if (currentPage == 0) currentPage = 1;
    update();
    Future.delayed(const Duration(milliseconds: 300),
        () => codeInputFocusNode.requestFocus());
  }

  ///
  /// Метод возврата к вводу номера телефона
  ///
  void toPreviousPage() {
    if (currentPage == 1) {
      currentPage = 0;
      codeController.clear();
      isCodeValid = true;
      update();
      Future.delayed(const Duration(milliseconds: 300),
          () => phoneInputFocusNode.requestFocus());
    }
  }

  ///
  /// Метод возвращает секунды в формате времени
  ///
  String getTime() {
    final Duration duration = Duration(seconds: secoundsLeft);

    final num minutes = duration.inMinutes.remainder(60);
    final String seconds =
        duration.inSeconds.remainder(60).toString().padLeft(2, '0');

    return "$minutes:$seconds";
  }
}
