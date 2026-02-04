import 'package:app/repository/FeedbackRepository.dart';
import 'package:app/utils/const.dart';
import 'package:app/utils/utils.dart';
import 'package:email_validator/email_validator.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';

/// Контроллер обратной связи, хранит временную информацию текстовых полей
/// при заполнении обратной связи, а также все методы, связанные с ней
class FeedbackController extends GetxController {
  final FeedbackRepository repository = Get.put(FeedbackRepository());

  // текстовые контроллеры и фокус ноды
  final TextEditingController emailController = TextEditingController();
  final TextEditingController feedbackTextController = TextEditingController();
  final FocusNode emailFocus = FocusNode();
  final FocusNode feedbackTextFocus = FocusNode();

  bool isEmailValid = false;

  bool get showEmailError =>
      emailController.text.isNotEmpty &&
      emailFocus.hasFocus == false &&
      isEmailValid == false;

  bool get isFormReady =>
      isEmailValid && feedbackTextController.text.isNotEmpty;

  ///
  /// Метод инициализации FeedbackController
  ///
  @override
  void onInit() {
    super.onInit();
    _initializeInputControllersListeners();
  }

  ///
  /// Метод отправки обратной связи
  ///
  void sendFeedback() async {
    if (isFormReady == false) return;

    // Закрываем клавиатуру и показваем лоадер
    _unfocus();
    Utils.showLoader();

    // Отправляем сообщение
    final bool result = await repository
        .sendFeedback(
            email: emailController.text, text: feedbackTextController.text)
        .timeout(
          const Duration(seconds: TIMEOUT_DURATION_SECONDS),
          onTimeout: () => false,
        );

    // Убираем лоадер
    Utils.hideLoader();

    // Обрабатываем успешную и неуспешную отправки
    if (result) {
      Get.back();
      Utils.showSnackBar(title: 'Сообщение отправлено');
    } else {
      Utils.showSnackBar(
          title: 'Произошла ошибка при отправке, попробуйте позже');
    }
  }

  ///
  /// Метод инициализирует листенеры для контроллеров ввода имейла и отзыва
  ///
  void _initializeInputControllersListeners() {
    emailController.addListener(() {
      isEmailValid = EmailValidator.validate(emailController.text);
      update();
    });

    feedbackTextController.addListener(() {
      update();
    });

    emailFocus.addListener(() => update());
    feedbackTextFocus.addListener(() => update());
  }

  ///
  /// Метод снимает фокус со всех текстовых полей
  ///
  void _unfocus() {
    if (emailFocus.hasFocus) emailFocus.unfocus();
    if (feedbackTextFocus.hasFocus) feedbackTextFocus.unfocus();
  }
}
