import 'package:app/repository/QRMarksRepository.dart';
import 'package:app/utils/utils.dart';
import 'package:get/get.dart';

import '../models/qr_mark.dart';

class QRMarksController extends GetxController {
  List<QRMark> qrMarks = [];

  final repository = Get.put(QRMarksRepository());

  bool get hasAnyMark => qrMarks.isNotEmpty;

  @override
  void onInit() {
    super.onInit();

    /// этот метод теперь вызывает апп_контроллер когда извлекет токен
    // loadQRMarks();
  }

  Future<void> loadQRMarks() async {
    qrMarks.clear();
    var result = await repository.receiveQRMarks();
    if (result != null) {
      for (var m in result) {
        qrMarks.add(m);
      }
    }
    update();
  }

  Future<Response?> addQRMark(QRMark qrMark) async {
    // await repository.createQR();

    var response =
        await repository.linkQRToUser(qrMark.qrId, qrMark.qrName ?? '');

    if (response != null) {
      await loadQRMarks();
    }

    update();
    return response;
  }

  Future<void> deleteQRMark(QRMark qrMark) async {
    var result = await Utils.showOptionsDialog(
          title: 'Удаление метки',
          subtitle: 'После удаления метку можно восстановить повторным '
              'добавлением',
          buttonTextConfirm: 'Удалить',
          buttonTextCancel: 'Отмена',
        ) ||
        false;
    if (result) {
      await repository.deleteQR(qrMark.qrId);
      await loadQRMarks();
      update();
    }
  }

  Future<void> onLogout() async {
    qrMarks.clear();
  }
}
