class NotificationSettings {
  final bool pushEnabled;
  final bool callEnabled;
  final bool telegramEnabled;
  final bool showPhoneOnUnreachable;
  final String? telegramContact;
  final String? vkContact;
  final String? maxContact;
  final int? telegramDialogId;

  const NotificationSettings({
    required this.pushEnabled,
    required this.callEnabled,
    required this.telegramEnabled,
    required this.showPhoneOnUnreachable,
    this.telegramContact,
    this.vkContact,
    this.maxContact,
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
        showPhoneOnUnreachable: json['show_phone_on_unreachable'] is bool
            ? json['show_phone_on_unreachable']
            : false,
        telegramContact:
            json['telegram_contact'] is String ? json['telegram_contact'] : null,
        vkContact: json['vk_contact'] is String ? json['vk_contact'] : null,
        maxContact: json['max_contact'] is String ? json['max_contact'] : null,
        telegramDialogId: json['telegram_dialog_id'] is int
            ? json['telegram_dialog_id']
            : null);
  }

  @override
  String toString() {
    return 'NotificationSettings: {pushEnabled: $pushEnabled, callEnabled: $callEnabled, telegramEnabled: $telegramEnabled, showPhoneOnUnreachable: $showPhoneOnUnreachable}';
  }
}
