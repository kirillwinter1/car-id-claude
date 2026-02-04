import 'package:app/controllers/notification_settings_controller.dart';
import 'package:app/utils/const.dart';
import 'package:app/utils/res/text_styles.dart';
import 'package:app/utils/res/ui_colors.dart';
import 'package:app/utils/scale_config.dart';
import 'package:app/utils/utils.dart';
import 'package:app/widgets/app_bars/title_app_bar.dart';
import 'package:app/widgets/buttons/light_button.dart';
import 'package:app/widgets/custom_progress_indicator.dart';
import 'package:app/widgets/custom_switch.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';

class NotificationSettingsScreen extends StatelessWidget {
  const NotificationSettingsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final TextStyle textStyle = TextStyles.regular17.copyWith(
      color: UiColors.blackC_80,
      height: SC.s18 / SC.s17,
      letterSpacing: 0.2,
    );

    return GetBuilder<NotificationSettingsController>(
        init: NotificationSettingsController(),
        builder: (ctrl) {
          return Scaffold(
            appBar: const TitleAppBar(
              title: 'Настройка уведомлений',
            ),
            body: ctrl.isInitialLoading
                ? const CustomProgressIndicator()
                : Padding(
                    padding: EdgeInsets.symmetric(horizontal: SC.s16),
                    child: Column(
                      children: [
                        SizedBox(height: SC.s20),

                        ///
                        /// Push
                        ///
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Text('Push - уведомления', style: textStyle),
                            CustomSwitch(
                              value: ctrl.pushEnabled,
                              onChanged: (value) {
                                // переключаем свитчер и обновляем UI
                                ctrl.pushEnabled = !ctrl.pushEnabled;
                                ctrl.update();

                                // пытаемся обновить настройки на бэке
                                // в случае неудачи откатываем свитчер
                                ctrl.changeSettings('push', ctrl.pushEnabled);
                              },
                            ),
                          ],
                        ),
                        SizedBox(height: SC.s(32)),

                        ///
                        /// Robot call
                        ///
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Text('Звонок робота', style: textStyle),
                            CustomSwitch(
                              value: ctrl.callEnabled,
                              onChanged: (value) {
                                // переключаем свитчер и обновляем UI
                                ctrl.callEnabled = !ctrl.callEnabled;
                                ctrl.update();

                                // пытаемся обновить настройки на бэке
                                // в случае неудачи откатываем свитчер
                                ctrl.changeSettings('call', !value);
                              },
                            ),
                          ],
                        ),
                        // SizedBox(height: SC.s(32)),

                        ///
                        /// Whatsapp
                        ///
                        // Row(
                        //   mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        //   children: [
                        //     Text('Уведомления в Whatsapp', style: textStyle),
                        //     CustomSwitch(
                        //       value: ctrl.whatsappEnabled,
                        //       onChanged: (value) {
                        //         // переключаем свитчер и обновляем UI
                        //         ctrl.whatsappEnabled = !ctrl.whatsappEnabled;
                        //         ctrl.update();

                        //         // пытаемся обновить настройки на бэке
                        //         // в случае неудачи откатываем свитчер
                        //         ctrl.changeSettings('whatsapp', !value);
                        //       },
                        //     ),
                        //   ],
                        // ),

                        ///
                        /// Telegram bot
                        ///
                        ctrl.settings?.telegramDialogId == null
                            ? Padding(
                                padding: EdgeInsets.only(top: SC.s23),
                                child: Row(
                                  mainAxisAlignment:
                                      MainAxisAlignment.spaceBetween,
                                  children: [
                                    Expanded(
                                        child:
                                            Text('Telegram', style: textStyle)),
                                    LightButton(
                                      title: 'Подключить бота',
                                      onPressed: () {
                                        Get.back();
                                        Utils.openUrl(TELEGRAM_BOT_URL);
                                      },
                                      height: SC.s(44),
                                      minWidth: null,
                                      padding: EdgeInsets.only(left: SC.s16),
                                      contentPadding: EdgeInsets.symmetric(
                                          horizontal: SC.s14),
                                      borderWidth: SC.s1,
                                      titleStyle: TextStyles.regular14
                                          .copyWith(height: SC.s18 / SC.s14),
                                      borderRadius: SC.s12,
                                    )
                                  ],
                                ),
                              )
                            : Padding(
                                padding: EdgeInsets.only(top: SC.s(32)),
                                child: Row(
                                  mainAxisAlignment:
                                      MainAxisAlignment.spaceBetween,
                                  children: [
                                    Text('Уведомления в Telegram',
                                        style: textStyle),
                                    CustomSwitch(
                                      value: ctrl.telegramEnabled,
                                      onChanged: (value) {
                                        // переключаем свитчер и обновляем UI
                                        ctrl.telegramEnabled =
                                            !ctrl.telegramEnabled;
                                        ctrl.update();

                                        // пытаемся обновить настройки на бэке
                                        // в случае неудачи откатываем свитчер
                                        ctrl.changeSettings('telegram', !value);
                                      },
                                    ),
                                  ],
                                ),
                              ),
                      ],
                    ),
                  ),
          );
        });
  }
}
