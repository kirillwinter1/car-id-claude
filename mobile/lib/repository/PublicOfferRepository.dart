// ignore_for_file: file_names, avoid_print

import 'package:app/utils/const.dart';
import 'package:get/get_connect/connect.dart';

class PublicOfferRepository extends GetConnect {
  @override
  void onInit() {
    super.onInit();
    // allowAutoSignedCert = true;
  }

  ///
  /// Метод возвращает текст публичной оферты
  ///
  Future<String?> fetchPublicOffer() async {
    var body = {
      "method": "public_offer.get",
      "params": {},
    };

    print("public_offer.get request body: $body");

    try {
      final Response response = await post(MAIN_URL, body);
      print('public_offer.get response code: ${response.statusCode}');
      print('public_offer.get response body: ${response.body}');

      if (response.statusCode == 200) {
        final Map body = response.body;

        if (body.containsKey('result') &&
            body['result'] == 'true' &&
            body.containsKey('params') &&
            body['params'] is Map &&
            (body['params'] as Map).containsKey('offer') &&
            body['params']['offer'] is String) {
          return body['params']['offer'];
        }
      }
    } catch (error) {
      print('public_offer.get error: $error');
    }

    return null;
  }
}
