// ignore_for_file: file_names, avoid_print

import 'package:app/models/marketplaces.dart';
import 'package:app/utils/const.dart';
import 'package:get/get.dart';

class MarketplaceLinksRepository extends GetConnect {
  ///
  /// Метод актуальных ссылок на маркетплейсы
  ///
  Future<Marketplaces?> fetchMarketplaceLinks() async {
    var body = {
      "method": "marketplaces.get",
      "params": {},
    };

    print("marketplaces.get request body: $body");

    try {
      final Response response = await post(MAIN_URL, body);
      print('marketplaces.get response code: ${response.statusCode}');
      print('marketplaces.get response body: ${response.body}');

      if (response.statusCode == 200 &&
          response.body is Map &&
          (response.body as Map).containsKey('result') &&
          response.body['result'] == 'true' &&
          (response.body as Map).containsKey('params') &&
          response.body['params'] is Map) {
        return Marketplaces.fromJson(response.body['params']);
      }
    } catch (error) {
      print('marketplaces.get error: $error');
    }

    return null;
  }
}
