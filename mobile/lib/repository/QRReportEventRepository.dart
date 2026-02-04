import 'dart:convert';

import 'package:flutter/cupertino.dart';
import 'package:get/get.dart';

import '../controllers/app_controller.dart';
import '../models/report_event.dart';
import '../utils/const.dart';

class QRReportEventRepository extends GetConnect {
  final appController = Get.find<AppController>();

  @override
  void onInit() {
    super.onInit();
    // allowAutoSignedCert = true;
  }

  ///
  /// Получение списка причин отправки сообщения
  ///
  Future<List<ReportEvent>?> loadReportEvents() async {
    var body = {'method': 'report.get_all_reasons', 'params': {}};
    final Response response = await post(MAIN_URL, body,
        headers: {'Authorization': 'Bearer ${appController.token}'});

    debugPrint('loadReportEvents: ${jsonEncode(response.body)}');

    if (response.statusCode == 200) {
      List params = response.body['params'];
      return params.map((e) => ReportEvent.fromJson(e)).toList();
    }

    return null;
  }

  ///
  /// Отправка сообщения
  ///
  Future<Response> sendQRMessage(int id, String qr, String text) async {
    var body = {
      'method': 'report.send',
      'params': {'qr_id': qr, 'reason_id': id, 'text': text}
    };

    debugPrint('REQUEST: ${jsonEncode(body)}');

    final Response response = await post(MAIN_URL, body,
        headers: {'Authorization': 'Bearer ${appController.token}'});
    debugPrint('sendQRMessage statusCode: ${response.statusCode}');
    debugPrint('sendQRMessage: ${jsonEncode(response.body)}');

    return response;
    // if (response.statusCode == 200) {
    //   if (response.body['result'] == 'false') {
    //     return response.body['error_message'];
    //   } else {
    //     Map? result = response.body['params'];
    //   }
    // }
    // return null;
  }
}
