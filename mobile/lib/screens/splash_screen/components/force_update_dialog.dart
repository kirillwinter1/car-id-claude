import 'package:app/controllers/app_controller.dart';
import 'package:app/utils/utils.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';

class ForceUpdateDialog extends StatelessWidget {
  const ForceUpdateDialog({super.key});

  static Future<void> open() async {
    await Utils.showSimpleDialog(
      isDismissible: false,
      title: 'Обновление приложения',
      subtitle:
          'Вы используете старую версию приложения. Для продолжения необходимо обновиться.',
      buttonText: 'Обновить',
      onButtonPressed: () {
        Get.find<AppController>().openStoreLink();
      },
    );
  }

  // Not in use
  @override
  Widget build(BuildContext context) {
    return const Placeholder();
  }
}
