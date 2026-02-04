import 'dart:convert';

import 'package:app/controllers/app_controller.dart';
import 'package:app/models/qr_mark.dart';
import 'package:flutter/cupertino.dart';
import 'package:get/get.dart';

import '../utils/const.dart';

class QRMarksRepository extends GetConnect {
  @override
  void onInit() {
    super.onInit();
    // allowAutoSignedCert = true;
  }

  ///
  /// Получение сохраненных меток
  ///
  Future<List<QRMark>?> receiveQRMarks() async {
    var body = {'method': 'qr.get_all', 'params': {}};

    debugPrint('REQUEST: $body');

    var appController = Get.find<AppController>();
    final Response response = await post(MAIN_URL, body,
        headers: {'Authorization': 'Bearer ${appController.token}'});
    debugPrint('receiveQRMarks statusCode: ${response.statusCode}');
    debugPrint('receiveQRMarks: ${jsonEncode(response.body)}');
    if (response.statusCode == 200) {
      List result = response.body['params'];

      List<QRMark> answer = [];
      for (var qr in result) {
        var qrMark = QRMark.fromJson(qr);
        answer.add(qrMark);
      }
      return answer;
    }
    return null;
  }

  ///
  /// Создание метки
  ///
  Future<String> createQR() async {
    var body = {'method': 'qr.create', 'params': {}};

    debugPrint('qr.create REQUEST: $body');

    var appController = Get.find<AppController>();
    final Response response = await post(MAIN_URL, body,
        headers: {'Authorization': 'Bearer ${appController.token}'});
    debugPrint('qr.create statusCode: ${response.statusCode}');
    debugPrint('qr.create: ${jsonEncode(response.body)}');
    if (response.statusCode == 200 && response.body['result'] == "true") {
      Map result = response.body['params'];
      return result['qr_id'] ?? '';
    }
    return '';
  }

  ///
  /// Привязка метки к аккаунту
  ///
  Future<Response?> linkQRToUser(String qrId, String qrName) async {
    var body = {
      'method': 'qr.link_to_user',
      'params': {
        'qr_id': qrId,
        'qr_name': qrName,
      }
    };

    debugPrint('link_to_user REQUEST: $body');

    var appController = Get.find<AppController>();
    final Response response = await post(MAIN_URL, body,
        headers: {'Authorization': 'Bearer ${appController.token}'});
    debugPrint('link_to_user statusCode: ${response.statusCode}');
    debugPrint('link_to_user: ${jsonEncode(response.body)}');
    return response;
    // if (response.statusCode == 200) {
    //   if (response.body['result'] == 'false') {
    //     return response.body['error_message'];
    //   } else {
    //     return null;
    //   }
    // }
    // return 'Несуществующий QR код.';
  }

  ///
  /// Удаление метки
  ///
  Future<void> deleteQR(String qrId) async {
    var body = {
      'method': 'qr.delete',
      'params': {'qr_id': qrId}
    };

    debugPrint('qr.delete REQUEST: $body');

    var appController = Get.find<AppController>();
    final Response response = await post(MAIN_URL, body,
        headers: {'Authorization': 'Bearer ${appController.token}'});
    debugPrint('qr.delete statusCode: ${response.statusCode}');
    debugPrint('qr.delete: ${jsonEncode(response.body)}');
    if (response.statusCode == 200) {
      Map? result = response.body['params'];
    }
  }
}
