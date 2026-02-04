import 'package:app/models/car_notification.dart';
import 'package:app/screens/status_screen/components/animated_check.dart';
import 'package:app/screens/status_screen/status_controller.dart';
import 'package:app/utils/res/text_styles.dart';
import 'package:app/utils/routes.dart';
import 'package:app/utils/scale_config.dart';
import 'package:app/widgets/app_bars/title_app_bar.dart';
import 'package:app/widgets/custom_loading_indicator.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';

class StatusScreen extends StatelessWidget {
  final CarNotification notification;
  const StatusScreen(this.notification, {super.key});

  @override
  Widget build(BuildContext context) {
    return GetBuilder<StatusController>(
      init: StatusController(notification),
      builder: (ctrl) {
        return Scaffold(
          appBar: TitleAppBar(
            onBackTap: () {
              if (Navigator.of(context).canPop()) {
                Get.back();
              } else {
                Get.offAllNamed(CustomRouter.PAGENAME_NOTIFICATIONS);
              }
            },
          ),
          body: SafeArea(
            child: Padding(
              padding: EdgeInsets.only(bottom: SC.s(50)),
              child: Center(
                child: ctrl.initialIsRead
                    ? _readContent(alreadyChecked: true)
                    : ctrl.isRead
                        ? _readContent()
                        : _unreadContent(),
              ),
            ),
          ),
        );
      },
    );
  }

  Widget _readContent({bool alreadyChecked = false}) {
    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Text(
          'Сообщение владельцу авто доставлено\nОжидайте, когда он подойдёт',
          style: TextStyles.medium16,
          textAlign: TextAlign.center,
        ),
        SizedBox(height: SC.s20),
        AnimatedCheck(alreadyChecked: alreadyChecked),
      ],
    );
  }

  Widget _unreadContent() {
    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Padding(
          padding: EdgeInsets.fromLTRB(SC.s16, 0, SC.s16, SC.s16),
          child: Text(
            'Ожидаем ответа от владельца',
            style: TextStyles.medium16,
            textAlign: TextAlign.center,
          ),
        ),
        const CustomLoadingIndicator(),
      ],
    );
  }
}
