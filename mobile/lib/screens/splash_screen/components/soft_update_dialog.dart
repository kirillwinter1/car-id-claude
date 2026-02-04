import 'package:app/utils/utils.dart';
import 'package:flutter/material.dart';

class SoftUpdateDialog extends StatelessWidget {
  const SoftUpdateDialog({super.key});

  static Future<bool> open() async {
    return await Utils.showOptionsDialog(
      title: 'Обновление приложения',
      subtitle: 'Вышла новая версия приложения. Обновить?',
      buttonTextConfirm: 'Обновить',
      buttonTextCancel: 'Позже',
    );
  }

  // Not in use
  @override
  Widget build(BuildContext context) {
    return const Placeholder();
  }
}
