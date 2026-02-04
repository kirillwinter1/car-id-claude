// ignore_for_file: file_names, avoid_print

import 'package:app/models/app_version.dart';
import 'package:app/utils/const.dart';
import 'package:get/get.dart';

class VersionControlRepository extends GetConnect {
  @override
  void onInit() {
    super.onInit();
    // allowAutoSignedCert = true;
  }

  ///
  /// Метод получения номеров актуальных билдов приложения
  ///
  Future<AppVersion?> fetchAppVersion() async {
    var body = {
      "method": "version_control.get",
      "params": {},
    };

    print("version_control.get request body: $body");

    try {
      final Response response = await post(MAIN_URL, body);
      print('version_control.get response code: ${response.statusCode}');
      print('version_control.get response body: ${response.body}');

      if (response.statusCode == 200 &&
          response.body is Map &&
          (response.body as Map).containsKey('result') &&
          response.body['result'] == 'true' &&
          (response.body as Map).containsKey('params') &&
          response.body['params'] is Map) {
        return AppVersion.fromJson(response.body['params']);
      }
    } catch (error) {
      print('version_control.get error: $error');
    }

    return null;
  }
}
