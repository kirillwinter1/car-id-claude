// ignore_for_file: file_names, avoid_print

import 'package:app/models/user.dart';
import 'package:app/utils/const.dart';
import 'package:app/utils/utils.dart';
import 'package:get/get.dart';

class UserRepository extends GetConnect {
  @override
  void onInit() {
    super.onInit();
    // allowAutoSignedCert = true;
  }

  ///
  /// Метод выполняет запрос регистрации телефона и возвращает int секунд
  /// до следующего запроса
  ///
  Future<int?> registerPhone(String phone) async {
    var body = {
      "method": "user.login_oauth_mobile",
      "params": {"phone_number": phone} // в формате "7##########"
    };

    print("user.login_oauth_mobile request body : $body");

    try {
      final Response response = await post(MAIN_URL, body);
      print('user.login_oauth_mobile response code: ${response.statusCode}');
      print('user.login_oauth_mobile response body: ${response.body}');

      if (response.statusCode == 200) {
        final Map body = response.body;

        if (body.containsKey('result') &&
            body['result'] == 'true' &&
            body.containsKey('params') &&
            body['params'] is Map &&
            (body['params'] as Map).containsKey('time_to_next_request_sec') &&
            body['params']['time_to_next_request_sec'] is int) {
          return body['params']['time_to_next_request_sec'];
        }
      } else {
        Utils.showSnackBar(title: 'Произошла ошибка, попробуйте позже');
      }
    } catch (error) {
      print('user.login_oauth_mobile error: $error');
    }

    return null;
  }

  ///
  /// Метод выполняет отправку смс-кода и возвращает токен авторизации
  ///
  Future<String?> confirmCode(String phone, String code) async {
    var body = {
      "method": "user.login_oauth_code",
      "params": {
        "phone_number": phone, // в формате "7##########"
        "code": code, // в формате "####"
      }
    };

    print("user.login_oauth_code request body : $body");

    try {
      final Response response = await post(MAIN_URL, body);
      print('user.login_oauth_code response code: ${response.statusCode}');
      print('user.login_oauth_code response body: ${response.body}');

      if (response.statusCode == 200) {
        final Map body = response.body;

        if (body.containsKey('result') &&
            body['result'] == 'true' &&
            body.containsKey('params') &&
            body['params'] is Map &&
            (body['params'] as Map).containsKey('token') &&
            body['params']['token'] is String) {
          return body['params']['token'];
        }
      }
    } catch (error) {
      print('user.login_oauth_code error: $error');
    }

    return null;
  }

  ///
  /// Метод выполняет отправку токена и возвращает пользователя
  ///
  Future<User?> getUserByToken(String token) async {
    var body = {
      "method": "user.get",
      "params": {},
    };

    print("user.get request body: $body");

    try {
      final Response response = await post(MAIN_URL, body,
          headers: {"Authorization": "Bearer $token"});
      print('user.get response code: ${response.statusCode}');
      print('user.get response body: ${response.body}');

      if (response.statusCode == 200) {
        final Map body = response.body;

        if (body.containsKey('result') &&
            body['result'] == 'true' &&
            body.containsKey('params') &&
            body['params'] is Map &&
            (body['params'] as Map).containsKey('phone_number') &&
            body['params']['phone_number'] is String) {
          return User.fromJson(response.body['params']);
        }
      }
    } catch (error) {
      print('user.get error: $error');
    }

    return null;
  }

  ///
  /// Метод удаляет пользователя на бэке
  ///
  Future<bool> deleteUser(String token) async {
    var body = {
      "method": "user.delete",
      "params": {},
    };

    print("user.delete request body: $body");

    try {
      final Response response = await post(MAIN_URL, body,
          headers: {"Authorization": "Bearer $token"});
      print('user.delete response code: ${response.statusCode}');
      print('user.delete response body: ${response.body}');

      if (response.statusCode == 200 &&
          response.body is Map &&
          (response.body as Map).containsKey('result') &&
          response.body['result'] == 'true') {
        return true;
      }
    } catch (error) {
      print('user.delete error: $error');
    }

    return false;
  }

  ///
  /// Метод деавторизует пользователя на бэке
  ///
  Future<bool> logout(String token) async {
    var body = {
      "method": "user.logout",
      "params": {},
    };

    print("user.logout request body: $body");

    try {
      final Response response = await post(MAIN_URL, body,
          headers: {"Authorization": "Bearer $token"});
      print('user.logout response code: ${response.statusCode}');
      print('user.logout response body: ${response.body}');

      if (response.statusCode == 200 &&
          response.body is Map &&
          (response.body as Map).containsKey('result') &&
          response.body['result'] == 'true') {
        return true;
      }
    } catch (error) {
      print('user.logout error: $error');
    }

    return false;
  }
}
