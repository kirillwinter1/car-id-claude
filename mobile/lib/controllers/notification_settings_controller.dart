import 'package:app/models/notification_settings.dart';
import 'package:app/repository/NotificationsRepository.dart';
import 'package:app/utils/const.dart';
import 'package:app/utils/utils.dart';
import 'package:get/get.dart';

/// Контроллер настроек уведомлений
class NotificationSettingsController extends GetxController {
  final NotificationsRepository repository = Get.put(NotificationsRepository());

  // названия соответствующих полей в запросе на бэк
  static const Map<String, String> paramsNames = {
    'push': 'push_enabled',
    'call': 'call_enabled',
    'whatsapp': 'whatsapp_enabled',
    'telegram': 'telegram_enabled',
  };

  // текущие настройки уведомлений
  NotificationSettings? settings;

  bool isInitialLoading = true;

  // фактическое положение свитчеров
  late bool pushEnabled;
  late bool callEnabled;
  late bool whatsappEnabled;
  late bool telegramEnabled;

  @override
  void onInit() async {
    super.onInit();

    settings = await repository.fetchNotificationSettings();
    isInitialLoading = false;
    mapSettings();
  }

  /// Метод обновляет состояние свитчеров в UI
  void mapSettings() {
    if (settings != null) {
      pushEnabled = settings!.pushEnabled;
      callEnabled = settings!.callEnabled;
      whatsappEnabled = settings!.whatsappEnabled;
      telegramEnabled = settings!.telegramEnabled;
      update();
    }
  }

  /// Метод обновляет настройки на бэке и перерисовывает UI
  void changeSettings(String paramsName, bool newValue) async {
    if (paramsNames.containsKey(paramsName)) {
      // стучимся на бэк
      final NotificationSettings? newSettings = await repository
          .patchNotificationSettings({
        paramsNames[paramsName]!: newValue
      }).timeout(const Duration(seconds: TIMEOUT_DURATION_SECONDS),
              onTimeout: () => null);

      // если успешно, то обновляем настройки
      // иначе показываем снэкбар ошибки
      if (newSettings != null) {
        settings = newSettings;
      } else {
        Utils.showSnackBar(title: 'Произошла ошибка обновления настроек');
      }

      // мапим настройки для обновления UI
      // если запрос на бэк не прошел то замапятся старые настройки
      mapSettings();
    }
  }
}
