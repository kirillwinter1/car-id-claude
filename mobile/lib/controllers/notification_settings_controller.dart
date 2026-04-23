import 'package:app/models/notification_settings.dart';
import 'package:app/repository/NotificationsRepository.dart';
import 'package:app/utils/const.dart';
import 'package:app/utils/utils.dart';
import 'package:flutter/widgets.dart';
import 'package:get/get.dart';

/// Контроллер настроек уведомлений
class NotificationSettingsController extends GetxController
    with WidgetsBindingObserver {
  final NotificationsRepository repository = Get.put(NotificationsRepository());

  // названия соответствующих полей в запросе на бэк
  static const Map<String, String> paramsNames = {
    'push': 'push_enabled',
    'call': 'call_enabled',
    'telegram': 'telegram_enabled',
  };

  // текущие настройки уведомлений
  NotificationSettings? settings;

  bool isInitialLoading = true;

  // фактическое положение свитчеров
  late bool pushEnabled;
  late bool callEnabled;
  late bool telegramEnabled;

  // Phase 2.4: признак, что пользователь ушёл в Telegram привязывать бота.
  // При возврате в приложение автоматически перезагружаем настройки,
  // чтобы увидеть обновлённое состояние привязки.
  bool _awaitingTelegramBind = false;

  @override
  void onInit() async {
    super.onInit();
    WidgetsBinding.instance.addObserver(this);

    settings = await repository.fetchNotificationSettings();
    isInitialLoading = false;
    mapSettings();
  }

  @override
  void onClose() {
    WidgetsBinding.instance.removeObserver(this);
    super.onClose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    super.didChangeAppLifecycleState(state);
    if (state == AppLifecycleState.resumed && _awaitingTelegramBind) {
      _awaitingTelegramBind = false;
      _refreshAfterTelegramBind();
    }
  }

  /// Метод обновляет состояние свитчеров в UI
  void mapSettings() {
    if (settings != null) {
      pushEnabled = settings!.pushEnabled;
      callEnabled = settings!.callEnabled;
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

  /// Phase 2.4: получить одноразовый deep-link и открыть Telegram.
  /// При ошибке API — фолбэк на [TELEGRAM_BOT_URL] (пользователь войдёт
  /// через shareContact, как раньше).
  Future<void> launchTelegramBot() async {
    final url = await repository.getTelegramStartUrl();
    _awaitingTelegramBind = true;
    await Utils.openUrl(url ?? TELEGRAM_BOT_URL);
  }

  Future<void> _refreshAfterTelegramBind() async {
    final previouslyLinked = settings?.telegramDialogId != null;
    final fresh = await repository.fetchNotificationSettings();
    if (fresh == null) return;
    settings = fresh;
    mapSettings();
    if (!previouslyLinked && fresh.telegramDialogId != null) {
      Utils.showSnackBar(title: '✅ Telegram подключён');
    }
  }
}
