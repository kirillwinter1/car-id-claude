// ignore_for_file: file_names, avoid_print

import 'package:app/controllers/app_controller.dart';
import 'package:app/models/car_notification.dart';
import 'package:app/models/notification_settings.dart';
import 'package:app/utils/const.dart';
import 'package:get/get.dart';

class NotificationsRepository extends GetConnect {
  final appController = Get.find<AppController>();

  @override
  void onInit() {
    super.onInit();
    // allowAutoSignedCert = true;
  }

  ///
  /// Метод получает все уведомления юзера
  ///
  Future<CarNotificationResponse?> fetchAllNotifications(
      {int page = 0, int perPage = 100}) async {
    var body = {
      "method": "notification.get_all",
      "params": {
        "page": page,
        "size": perPage,
      },
    };

    print("notification.get_all request body: $body");

    try {
      final Response response = await post(MAIN_URL, body,
          headers: {"Authorization": "Bearer ${appController.token}"});
      print('notification.get_all response code: ${response.statusCode}');
      print('notification.get_all response body: ${response.body}');

      if (response.statusCode == 200) {
        final Map body = response.body;

        if (body.containsKey('result') &&
            body['result'] == 'true' &&
            body['params'] is Map) {
          return CarNotificationResponse.fromJson(body['params']);
        }
      }
    } catch (error) {
      print('notification.get_all error: $error');
    }

    return null;
  }

  ///
  /// Метод получает все непрочитанные уведомления юзера
  ///
  Future<CarNotificationResponse?> fetchUnreadNotifications(
      {int page = 0, int perPage = 100}) async {
    var body = {
      "method": "notification.get_all_unread",
      "params": {
        "page": page,
        "size": perPage,
      },
    };

    print("notification.get_all_unread request body: $body");

    try {
      final Response response = await post(MAIN_URL, body,
          headers: {"Authorization": "Bearer ${appController.token}"});
      print(
          'notification.get_all_unread response code: ${response.statusCode}');
      print('notification.get_all_unread response body: ${response.body}');

      if (response.statusCode == 200) {
        final Map body = response.body;

        if (body.containsKey('result') &&
            body['result'] == 'true' &&
            body['params'] is Map) {
          return CarNotificationResponse.fromJson(body['params']);
        }
      }
    } catch (error) {
      print('notification.get_all_unread error: $error');
    }

    return null;
  }

  ///
  /// Метод отмчает уведомление прочитанным
  ///
  Future<CarNotification?> markAsRead(String id) async {
    var body = {
      "method": "notification.mark_as_read",
      "params": {"notification_id": id},
    };

    print("notification.mark_as_read request body: $body");

    try {
      final Response response = await post(MAIN_URL, body,
          headers: {"Authorization": "Bearer ${appController.token}"});
      print('notification.mark_as_read response code: ${response.statusCode}');
      print('notification.mark_as_read response body: ${response.body}');

      if (response.statusCode == 200) {
        final Map body = response.body;

        if (body.containsKey('result') &&
            body['result'] == 'true' &&
            body['params'] is Map) {
          final CarNotification temp = CarNotification.fromJson(body['params']);

          return temp;
        }
      }
    } catch (error) {
      print('notification.mark_as_read error: $error');
    }

    return null;
  }

  ///
  /// Метод получает все ИСХОДЯЩИЕ уведомления юзера
  ///
  Future<CarNotificationResponse?> fetchAllOutgoingNotifications(
      {int page = 0, int perPage = 100}) async {
    var body = {
      "method": "report.get_all",
      "params": {
        "page": page,
        "size": perPage,
      },
    };

    print("report.get_all request body: $body");

    try {
      final Response response = await post(MAIN_URL, body,
          headers: {"Authorization": "Bearer ${appController.token}"});
      print('report.get_all response code: ${response.statusCode}');
      print('report.get_all response body: ${response.body}');

      if (response.statusCode == 200) {
        final Map body = response.body;

        if (body.containsKey('result') &&
            body['result'] == 'true' &&
            body['params'] is Map) {
          return CarNotificationResponse.fromJson(body['params']);
        }
      }
    } catch (error) {
      print('report.get_all error: $error');
    }

    return null;
  }

  ///
  /// Метод проверяет статус исходящего уведомления, было ли оно прочитано
  ///
  Future<bool> checkOutgoingStatus(String notificationId) async {
    var body = {
      "method": "report.get",
      "params": {"notification_id": notificationId},
    };

    print("report.get request body: $body");

    try {
      final Response response = await post(MAIN_URL, body,
          headers: {"Authorization": "Bearer ${appController.token}"});
      print('report.get response code: ${response.statusCode}');
      print('report.get response body: ${response.body}');

      if (response.statusCode == 200) {
        final Map body = response.body;

        if (body.containsKey('result') &&
            body['result'] == 'true' &&
            body['params'] is Map) {
          final CarNotification notification =
              CarNotification.fromJson(body['params']);
          return notification.isRead;
        }
      }
    } catch (error) {
      print('report.get error: $error');
    }

    return false;
  }

  ///
  /// Метод получает настройки юзера по уведомлениям
  ///
  Future<NotificationSettings?> fetchNotificationSettings() async {
    var body = {
      "method": "notification_settings.get",
      "params": {},
    };

    print("notification_settings.get request body: $body");

    try {
      final Response response = await post(MAIN_URL, body,
          headers: {"Authorization": "Bearer ${appController.token}"});
      print('notification_settings.get response code: ${response.statusCode}');
      print('notification_settings.get response body: ${response.body}');

      if (response.statusCode == 200) {
        final Map body = response.body;

        if (body.containsKey('result') &&
            body['result'] == 'true' &&
            body['params'] is Map) {
          return NotificationSettings.fromJson(body['params']);
        }
      }
    } catch (error) {
      print('notification_settings.get error: $error');
    }

    return null;
  }

  ///
  /// Метод изменяет настройки юзера по уведомлениям
  ///
  Future<NotificationSettings?> patchNotificationSettings(
      Map<String, bool> params) async {
    var body = {
      "method": "notification_settings.patch",
      "params": params,
    };

    print("notification_settings.patch request body: $body");

    try {
      final Response response = await post(MAIN_URL, body,
          headers: {"Authorization": "Bearer ${appController.token}"});
      print(
          'notification_settings.patch response code: ${response.statusCode}');
      print('notification_settings.patch response body: ${response.body}');

      if (response.statusCode == 200) {
        final Map body = response.body;

        if (body.containsKey('result') &&
            body['result'] == 'true' &&
            body['params'] is Map) {
          return NotificationSettings.fromJson(body['params']);
        }
      }
    } catch (error) {
      print('notification_settings.patch error: $error');
    }

    return null;
  }

  ///
  /// Метод изменяет настройки юзера по уведомлениям
  ///
  Future<bool?> updateToken(String token) async {
    var body = {
      "method": "notification_settings.update_token",
      "params": {'token': token},
    };

    print("update.token request body: $body");

    try {
      final Response response = await post(MAIN_URL, body,
          headers: {"Authorization": "Bearer ${appController.token}"});
      print("HEADERS: Authorization: Bearer ${appController.token}");
      print('update.token response code: ${response.statusCode}');
      print('update.token response body: ${response.body}');

      if (response.statusCode == 200) {
        final Map body = response.body;

        if (body.containsKey('result') &&
            body['result'] == 'true' &&
            body['params'] is Map) {
          // return NotificationSettings.fromJson(body['params']);
        }
      }
    } catch (error) {
      print('update.token error: $error');
    }

    return null;
  }
}
