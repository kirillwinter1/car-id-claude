// ignore_for_file: file_names, avoid_print

import 'package:app/controllers/app_controller.dart';
import 'package:app/utils/const.dart';
import 'package:get/get.dart';

class FeedbackRepository extends GetConnect {
  @override
  void onInit() {
    super.onInit();
    // allowAutoSignedCert = true;
  }

  ///
  /// Метод отправки обратной связи на бэк
  ///
  Future<bool> sendFeedback(
      {required String email, required String text}) async {
    var body = {
      "method": "feedback.send",
      "params": {
        "email": email,
        "text": text,
      },
    };

    print("feedback.send request body: $body");

    try {
      final Response response = await post(MAIN_URL, body, headers: {
        "Authorization": "Bearer ${Get.find<AppController>().token}"
      });
      print('feedback.send response code: ${response.statusCode}');
      print('feedback.send response body: ${response.body}');

      if (response.statusCode == 200 &&
          response.body is Map &&
          (response.body as Map).containsKey('result') &&
          response.body['result'] == 'true') {
        return true;
      }
    } catch (error) {
      print('feedback.send error: $error');
    }

    return false;
  }
}
