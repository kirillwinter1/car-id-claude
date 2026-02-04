import 'package:app/controllers/qrmarks_controller.dart';
import 'package:app/models/qr_mark.dart';
import 'package:app/screens/qr_scan_screen/components/add_qr_bottom_sheet.dart';
import 'package:app/utils/routes.dart';
import 'package:flutter/cupertino.dart';
import 'package:get/get.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:qr_code_scanner/qr_code_scanner.dart';

import '../utils/utils.dart';

class QRScanController extends GetxController {
  /// флаг что работаем с кодом чтобы не сканировать новые
  bool workWithCode = false;

  /// Контроллер для поля ввода названия автомобиля
  final nameTextController = TextEditingController();

  /// Контроллер фокуса поля ввода автомобиля
  final nameFocus = FocusNode();

  /// Флаг валидности поля названия автомобиля
  bool nameTextValid = true;

  /// Флаг активности кнопки сохрвнить
  bool buttonActive = false;

  /// Контроллер камеры/сканера QR кода
  QRViewController? scannerController;

  /// Флаг включения фонарика
  bool torchIsOn = false;

  /// Флаг открытия боттомщита ввода названия машины
  final showOverlay = false.obs;

  /// Стартовая переменаая показывающая откуда запущен
  /// этот экран, или с главного или с экрана меток
  final bool openFromQRMarkScreen;

  QRScanController({required this.openFromQRMarkScreen});

  /// Текущий сосканированный код
  String? currentCode;

  /// Флаг доступа к камере
  bool accessGranted = false;

  /// Инициализация, установка обработки валидности имени машины
  @override
  Future<void> onInit() async {
    super.onInit();
    nameTextController.addListener(() {
      nameTextValid =
          nameTextController.text.isEmpty || nameTextController.text.length > 1;
      buttonActive = nameTextController.text.isNotEmpty && nameTextValid;
      update();
    });
    Future.delayed(const Duration(milliseconds: 300), () => checkPermission());
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
              title: 'Добавить метку',
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

  /// Обработка события полчения кода от камеры, вызов боттомщита ввода имени
  Future<void> onCodeRecognized(String code) async {
    debugPrint('CODE RECEIVED: $code');

    // валидируем код по маске https://car-id.ru/qr/CODE
    if (code.startsWith('https://car-id.ru/qr/') == false) {
      debugPrint('error: NOT CAR ID CODE');
      return;
    }
    final String unmaskedCode = code.substring(21);

    if (workWithCode) return;
    workWithCode = true;
    currentCode = unmaskedCode;
    showOverlay.value = true;
    nameTextController.clear();
    Future.delayed(
        const Duration(milliseconds: 35), () => nameFocus.requestFocus());
    await Utils.showBottomSheet(
      title: 'Название автомобиля',
      contents: AddQRBottomSheet(
        controller: this,
      ),
    );
    showOverlay.value = false;
    workWithCode = false;
  }

  /// Запись удачного добаления автомобиля
  Future<void> onSaveButtonTap() async {
    if (torchIsOn) {
      torchIsOn = false;
      scannerController?.toggleFlash();
    }

    ///Закрытие боттомшита
    Get.back();
    Response? response = await Get.find<QRMarksController>().addQRMark(QRMark(
      qrId: currentCode ?? '',
      qrName: nameTextController.text,
    ));

    if (response!.body['error_code'] != null) {
      showOverlay.value = true;
      await Utils.showSimpleDialog(
          title: 'Ошибка',
          subtitle: response!.body['error_message'],
          buttonText: 'Понятно');
      showOverlay.value = false;
    } else {
      Get.find<QRMarksController>().loadQRMarks();
      if (openFromQRMarkScreen) {
        Get.back();
      } else {
        Get.offNamed(CustomRouter.PAGENAME_QRMARKS);
      }
    }
  }

  /// Обработка нажатия на кнопку фонарика на экране сканирования
  Future<void> onTorchTap() async {
    torchIsOn = !torchIsOn;
    scannerController?.toggleFlash();
    update();
  }
}
