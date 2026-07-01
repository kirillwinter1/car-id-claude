import 'package:flutter_test/flutter_test.dart';
import 'package:app/models/notification_settings.dart';

void main() {
  test('fromJson parses contacts + show_phone', () {
    final s = NotificationSettings.fromJson({
      'push_enabled': true,
      'call_enabled': false,
      'telegram_enabled': false,
      'show_phone_on_unreachable': true,
      'telegram_contact': '@ivan',
      'vk_contact': 'ivan_vk',
      'max_contact': 'max.ru/u/abc',
    });
    expect(s.showPhoneOnUnreachable, true);
    expect(s.telegramContact, '@ivan');
    expect(s.vkContact, 'ivan_vk');
    expect(s.maxContact, 'max.ru/u/abc');
  });

  test('fromJson defaults missing fields', () {
    final s = NotificationSettings.fromJson({'push_enabled': true});
    expect(s.showPhoneOnUnreachable, false);
    expect(s.telegramContact, isNull);
    expect(s.vkContact, isNull);
    expect(s.maxContact, isNull);
  });
}
