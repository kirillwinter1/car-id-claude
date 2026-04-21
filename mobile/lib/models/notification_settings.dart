class NotificationSettings {
  final bool pushEnabled;
  final bool callEnabled;
  final bool telegramEnabled;
  final int? telegramDialogId;

  const NotificationSettings({
    required this.pushEnabled,
    required this.callEnabled,
    required this.telegramEnabled,
    this.telegramDialogId,
  });

  factory NotificationSettings.fromJson(Map<String, dynamic> json) {
    return NotificationSettings(
        pushEnabled:
            json['push_enabled'] is bool ? json['push_enabled'] : false,
        callEnabled:
            json['call_enabled'] is bool ? json['call_enabled'] : false,
        telegramEnabled:
            json['telegram_enabled'] is bool ? json['telegram_enabled'] : false,
        telegramDialogId: json['telegram_dialog_id'] is int
            ? json['telegram_dialog_id']
            : null);
  }

  @override
  String toString() {
    return 'NotificationSettings: {pushEnabled: $pushEnabled, callEnabled: $callEnabled, telegramEnabled: $telegramEnabled}';
  }
}
